package com.datvexpress.ws.versatune.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class FifoService {

    Logger myLog = LoggerFactory.getLogger(getClass());
    ExecutorService executor = Executors.newFixedThreadPool(8);
    public List<String> getLinesFromFifo(InputStream is) throws ExecutionException, InterruptedException, TimeoutException {

        //ExecutorService executor = Executors.newFixedThreadPool(4);

        Supplier<List<String>> linesFetcher2 = () -> {
            ByteArrayOutputStream res = new ByteArrayOutputStream();

            List<String> resultLines = new ArrayList<>();
            byte[] bytes = new byte[8192]; // so we can buffer a number of lines...
            int numRead = 0;

            try{
                while ((numRead = is.read(bytes, 0, bytes.length)) >= 0) {
                    res.write(bytes, 0, numRead);
                    break;
                }
            }catch(Exception e){
                myLog.error("StreamReader error. ",e);
                throw new RuntimeException("Error in reading the FIFO.",e);
            }
            if (numRead > 0){
                try {
                    // we have an array of bytes in
                    // now we need to convert this to Array of Strings
                    String inputString = res.toString(StandardCharsets.UTF_8);
                    resultLines = Stream.of(inputString.split("\n", -1)).filter(p -> !p.isEmpty())
                            .collect(Collectors.toList());
                    resultLines.stream().forEach(p -> myLog.info(p));
                }catch(Exception e){
                    myLog.error("Could not convert bytes to lines. ", e);
                    throw new RuntimeException("Could not convert bytes to lines in the FIFO READER", e);
                }

            }
            return resultLines;
        };

        CompletableFuture<List<String>> cf1 = CompletableFuture.supplyAsync(linesFetcher2, executor);
        List<String> myLines = cf1.get(15000L,TimeUnit.MILLISECONDS);
        executor.shutdown();
        return myLines;
    }

    public FileInputStream makeNewFIO(String fifoPath) throws ExecutionException, InterruptedException, TimeoutException {



        Supplier <FileInputStream> makeStreamFrom = () -> {
            FileInputStream fio = null;
            try {
                myLog.info("*** CREATING new FIO for reading status while in tuner mode. ***");
                fio = new FileInputStream(fifoPath);
                myLog.info("*** created new FIO for reading status while in tuner mode. ***");
            } catch (FileNotFoundException e) {
                myLog.error("Cannot continue. Need to define path for FIFO.", e);
                throw new RuntimeException("FILE NOT FOUND when trying to open FIFO", e);
            }
            return fio;
        };

        CompletableFuture<FileInputStream> cf1 = CompletableFuture.supplyAsync(makeStreamFrom, executor);
        FileInputStream fio = cf1.get(10000L,TimeUnit.MILLISECONDS);
        executor.shutdown();
        return fio;

    }



    /*
            import org.javaync.io.AsyncFiles;
            import reactor.core.publisher.Flux;

            import java.nio.file.Path;

            public class AsyncFileRead {
                public Flux<String> lines() {
                    return Flux.from(AsyncFiles.lines(Path.of("test/sample.txt")));
                }
            }

     */

    public List<String> getDataFromNamedPipe(String pipePath){

        // open fifo for READ

        // get available data

        // convert to list of strings

        // close fifo

        // return the list
        return null;
    }

}
