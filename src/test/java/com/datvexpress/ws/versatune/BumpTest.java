package com.datvexpress.ws.versatune;

import lombok.Getter;
import lombok.Setter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;



public class BumpTest {

    @Setter
    @Getter
    static public class Prop{
        private int p1;
        private int p2;

        public int bumpP1(){
            return p1++;
        }
        public int bumpP2(){
            return ++p2;
        }
    }


    @Test
    public void bumpPropertyThenReturn(){
        Prop x = new Prop();
        x.setP1(1);
        x.setP2(1);
        int y = x.bumpP1();
        int z = x.bumpP2();
        Assertions.assertEquals(1, y, "y not equal 1 but should be.");
        Assertions.assertEquals(2, z, "Z not equal 2 but should be.");

    }

}
