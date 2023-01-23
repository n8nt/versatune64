package com.datvexpress.ws.versatune.service;

import com.datvexpress.ws.versatune.enums.IpcFifoStatus;
import com.datvexpress.ws.versatune.model.VersatuneContext;
import com.sun.jna.Library;
import com.sun.jna.Native;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class LinuxIoService {

    Logger logger = LoggerFactory.getLogger(getClass());

    int O_RDONLY = 0x0000;
    int O_WRONLY = 0x0001;
    int O_RDWR =   0x0002;
    int F_SETFL =  0x0003;
    int O_CREAT  = 0x0200;
    int O_NONBLOCK = 0x0004;

    public interface StdC extends Library {
        StdC INSTANCE = Native.load("c", StdC.class);
        int mkfifo(String path);
        int fcntl(int value, int cmd, long arg);
        int close(int fd);
        int open(String path, int value);
        int read(int fd, byte[] buffer, int count);
        int write(int fd, byte[] buffer, int count);

    }

    /*
            We'll open the ipcFifo for read only. Here are some notes from UNIX about the
            open command.

            See https://www.tutorialspoint.com/unix_system_calls/open.htm

    */
    public int linuxOpenPipeForRead(VersatuneContext context){
        String fifoPath = context.getIpcFifo();
        StdC libc = StdC.INSTANCE;

        if ( context.getIpcFifoStatus().equals(IpcFifoStatus.IPC_FIFO_OPEN.name())){
            logger.info("FD [" + context.getFd_ipcFifo() + "] for ipcFifo is already open.");
            context.setIpcFifoStatus(IpcFifoStatus.IPC_FIFO_OPEN.name());
            return context.getFd_ipcFifo(); // return open file descriptor.
        }else{
            try{
                context.setFd_ipcFifo( libc.open(fifoPath, O_RDONLY));
                if ( context.getFd_ipcFifo() < 0){
                    context.setIpcFifoStatus(IpcFifoStatus.IPC_FIFO_OPEN_ERROR.name());
                    logger.info("Error when opening FD: ["+ context.getFd_ipcFifo() + "]");
                    return context.getFd_ipcFifo();
                }
            }catch(Exception e){
                logger.info("Error when opening FD: ["+ context.getFd_ipcFifo() + "]");
                closeLinuxPipe(context);
            }

            try{
                context.setIpcErrorValue(libc.fcntl(context.getFd_ipcFifo(), F_SETFL, O_NONBLOCK));
                logger.info("libc.fcntl result is: ["+context.getIpcErrorValue()+"]");
                if (context.getIpcErrorValue()< 0){
                    context.setIpcFifoStatus(IpcFifoStatus.IPC_FIFO_FCNTL_ERROR.name());
                    return context.getIpcErrorValue();
                }
            }catch(Exception e){
                logger.error("ERROR trying to run FXNRL.",e);
                context.setIpcFifoStatus(IpcFifoStatus.IPC_FIFO_FCNTL_ERROR.name());
                closeLinuxPipe(context);
            }

            context.setIpcFifoStatus(IpcFifoStatus.IPC_FIFO_OPEN.name());
            return context.getFd_ipcFifo();
        }
    }

    public List<String> linuxGetLinesFromPipe(VersatuneContext context) throws ExecutionException, InterruptedException, TimeoutException {
        // we will put this on a separate thread
        ExecutorService executor = Executors.newFixedThreadPool(2);


        Supplier<List<String>> linesFetcher = () -> {
            StdC libc = StdC.INSTANCE;

            List<String> results = new ArrayList<>();
            // open the fifo for read
            int fd = linuxOpenPipeForRead(context);
            logger.info("linuxGetLinesFromPipe: OPEN returned: ["+fd+"]");
            if (fd > 0) {
                byte[] buffer = new byte[1024];

                // read up to 80 characters
                int num = libc.read(context.getFd_ipcFifo(), buffer, 1024);
                logger.info("linuxGetLinesFromPipe: returned ["+num+"] bytes from libc.read.");
                if (num > 0) {
                    // we are looking for a new-line character but, we want to
                    // ignore anything past a new-line character
                    // NTM: How do I know the buffer has any 0's in it?
                    results = convertFifoRawBufferToLines(buffer);
                    results.stream().forEach(p -> logger.info(p));
                    results.stream().forEach(p -> context.getFifoLines().add(p));
                }

            }else{
                logger.error("Could not open the file. fd: "+fd);
            }
            return results;

        };

        CompletableFuture<List<String>> cf1 = CompletableFuture.supplyAsync(linesFetcher, executor);
        logger.info("Waiting 20 seconds...");
        List<String> lines = cf1.get(20000L, TimeUnit.MILLISECONDS);
        executor.shutdown();
 //       logger.info(context.toString());
        return lines;

    }

    public int closeLinuxPipe (VersatuneContext context){
        String fifoPath = context.getIpcFifo();
        StdC libc = StdC.INSTANCE;
        int result = 0;
        if ( context.getIpcFifoStatus().equals(IpcFifoStatus.IPC_FIFO_OPEN.name())){
            try{
                result = libc.close(context.getFd_ipcFifo());
                context.setIpcFifoStatus(IpcFifoStatus.IPC_FIFO_CLOSED.name());
            }catch(Exception e){
                context.setIpcFifoStatus(IpcFifoStatus.IPC_FIFO_CLOSE_ERROR.name());
                result = -999;
            }
        }else{
            logger.info("FIFO with FD ["+context.getFd_ipcFifo()+"] was not open. State was: " + context.getIpcFifoStatus());
        }
        return result;
    }

    /*
        Converts the results of the FIFO data that was just read into a list of Strings.
    */
    private List<String> convertFifoRawBufferToLines(byte[] byteArray){

        // convert primitive bytes to Byte array.
        Byte[] bytes1 = toObjects(byteArray);
        int len2 = bytes1.length;

        // convert Byte array to LIST
        List<Byte> bList = Arrays.asList(bytes1);

        // filter out 0's which will probably be at the end of the array
        List<Byte> list2 = bList.stream().filter(p -> p != 0).collect(Collectors.toList());

        // Convert the Byte list to a primitive byte arrary
        byte[] filteredBuffer = toByteArray(list2);

        // now we need to convert this to Array of Strings
        String inputString = new String(filteredBuffer, StandardCharsets.UTF_8);
        List<String> resultLines = Stream.of(inputString.split("\n", -1)).filter(p -> !p.isEmpty())
                .collect(Collectors.toList());

        return resultLines;
    }

    /*
            Convert byte[] to Byte[]
     */
    private Byte[] toObjects(byte[] bytesPrim) {
        Byte[] bytes = new Byte[bytesPrim.length];
        Arrays.setAll(bytes, n -> bytesPrim[n]);
        return bytes;
    }

    /*
            Convert Byte[] to byte[]
     */
    private byte[] toByteArray(List<Byte> list){
        byte[] buffer = new byte[list.size()];

        for( int i=0; i < list.size(); i++){
            buffer[i] = list.get(i);
        }
        return buffer;
    }

//    public List<String> fetchLinesFromContext(VersatuneContext context){
//        return context.getFifoLines();
//    }

    public void clearFifoLinesInContext(VersatuneContext context){
        context.getFifoLines().clear();
    }

//    public void addFifoLinesToContext(VersatuneContext context, String line){
//        context.getFifoLines().add(line);
//    }
//
//    public void addFifoLinesToContext(VersatuneContext context, List<String> lines){
//        context.getFifoLines().addAll(lines);
//    }


}
