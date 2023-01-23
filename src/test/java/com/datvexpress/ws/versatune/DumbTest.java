package com.datvexpress.ws.versatune;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class DumbTest {
    @Test
    void test1(){

        byte[] byteArray = { 84, 69, 67, 72, 78, 79, 76, 79, 71, 89, 10, 65, 66, 67, 68, 69, 10, 70, 71, 72, 73, 10,0,0,0,0,0,0,};//array of ASCII values

        //converts byteArray to String
        Byte[] bytes1 = toObjects(byteArray);

        // convert byte array to LIST
        List<Byte> bList = Arrays.asList(bytes1);

        // filter out 0's
        List<Byte> list2 = bList.stream().filter(p -> p != 0).collect(Collectors.toList());

        byte[] filteredBuffer = toByteArray(list2);

        // now convert to an array of Strings
        // now we need to convert this to Array of Strings
        String inputString = new String(filteredBuffer, StandardCharsets.UTF_8);
        List<String> resultLines = Stream.of(inputString.split("\n", -1)).filter(p -> !p.isEmpty())
                .collect(Collectors.toList());

        String bob  = "bob";

    }





    private Byte[] toObjects(byte[] bytesPrim) {
        Byte[] bytes = new Byte[bytesPrim.length];
        Arrays.setAll(bytes, n -> bytesPrim[n]);
        return bytes;
    }

    private byte[] toByteArray(List<Byte> list){
        byte[] buffer = new byte[list.size()];

        for( int i=0; i < list.size(); i++){
            buffer[i] = list.get(i);
        }
        return buffer;
    }
}
