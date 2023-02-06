package ru.yandex.crypta.graph;

import org.junit.Test;

import ru.yandex.crypta.lib.proto.identifiers.EIdType;

import static org.junit.Assert.assertEquals;

public class PhoneTest {
    @Test
    public void testIsValid() {
        Phone test1 = new Phone("");
        Phone test2 = new Phone("0");
        Phone test3 = new Phone("-1");
        Phone test4 = new Phone("+89168751594");
        Phone test5 = new Phone("+79168751593");
        Phone test6 = new Phone("+7 916 875 15 93");
        Phone test7 = new Phone("+7(916)-875-15-93");
        Phone test8 = new Phone("+7(916) 875 15 93");
        Phone test9 = new Phone("8 (916) 875 15 93");

        assertEquals(test1.getValue(), "");
        assertEquals(test1.isValid(), false);
        assertEquals(test2.getValue(), "0");
        assertEquals(test2.isValid(), false);
        assertEquals(test3.getValue(), "-1");
        assertEquals(test3.isValid(), false);
        assertEquals(test4.getValue(), "+89168751594");
        assertEquals(test4.isValid(), false);
        assertEquals(test5.getValue(), "+79168751593");
        assertEquals(test5.isValid(), true);
        assertEquals(test6.getValue(), "+7 916 875 15 93");
        assertEquals(test6.isValid(), true);
        assertEquals(test7.getValue(), "+7(916)-875-15-93");
        assertEquals(test7.isValid(), true);
        assertEquals(test8.getValue(), "+7(916) 875 15 93");
        assertEquals(test8.isValid(), true);
        assertEquals(test9.getValue(), "8 (916) 875 15 93");
        assertEquals(test9.isValid(), true);
    }

    @Test
    public void testNormalize() {
        Phone test1 = new Phone("+79168751593");      //+79168751593
        Phone test2 = new Phone("+7 916 875 15 93");  //+79168751593
        Phone test3 = new Phone("+7(916)-875-15-93"); //+79168751593
        Phone test4 = new Phone("+7(916) 875 15 93"); //+79168751593
        Phone test5 = new Phone("8(916) 875 15 93");  //+79168751593

        assertEquals(test1.getNormalizedValue(), "+79168751593");
        assertEquals(test2.getNormalizedValue(), "+79168751593");
        assertEquals(test3.getNormalizedValue(), "+79168751593");
        assertEquals(test4.getNormalizedValue(), "+79168751593");
        assertEquals(test5.getNormalizedValue(), "+79168751593");
    }

    @Test
    public void testHash() {
        Phone test1 = new Phone("+79183938034");
        Phone test2 = new Phone("+79183938035");
        Phone test3 = new Phone("+79183938036");

        assertEquals(test1.getMd5(), "3ac5b42a336f051a3295eb6d6266c32b");
        assertEquals(test2.getMd5(), "99e421d0f4183866e987eaacecee76b1");
        assertEquals(test3.getMd5(), "ed36c9dc68c8bc1a98b31937d27e0e59");

        assertEquals(
            test1.getSha256(),
            "5085bb1313fafdd2da5f16b22cb0e57d22e855223dc1d16f2de04bd7e4440c80");
        assertEquals(
            test2.getSha256(),
            "7f1cf9f0367b331623448be448976e408ce53a70a1b7fff8edeb4d724e315a66");
        assertEquals(
            test3.getSha256(),
            "1ac87020bab6dd6e82ddfb9227e4523ec009370d92f311af0d906b466adbdef7");
    }

    @Test
    public void testgetType() {
        Phone test = new Phone("");
        assertEquals(test.getType(), EIdType.PHONE);
    }
}
