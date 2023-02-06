package ru.yandex.market.logshatter.parser.front.errorBooster.schema;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class CommonSchemaTest {

    @Test
    public void getOsVersionMajor() {
        CommonSchema commonSchema = new CommonSchema();

        assertEquals("5", commonSchema.getOsVersionMajor("Android", "5.6.7.8"));
        assertEquals("15", commonSchema.getOsVersionMajor("Android", "15.6.7.8"));
        assertEquals("4.5", commonSchema.getOsVersionMajor("Android", "4.5.6.7"));
        assertEquals("4.5", commonSchema.getOsVersionMajor("Android", "4.5"));
        assertEquals("4", commonSchema.getOsVersionMajor("Android", "4"));
        assertEquals("14.15", commonSchema.getOsVersionMajor("MacOS", "14.15.6.7"));
        assertEquals("14", commonSchema.getOsVersionMajor("MacOS", "14"));
        assertEquals("1.2", commonSchema.getOsVersionMajor("Tizen", "1.2"));
        assertEquals("10", commonSchema.getOsVersionMajor("Windows", "10.0"));
        assertEquals("12", commonSchema.getOsVersionMajor("iOS", "12.1.4"));
    }

    @Test
    public void getBrowserVersionMajor() {
        CommonSchema commonSchema = new CommonSchema();

        assertEquals("19.4", commonSchema.getBrowserVersionMajor("YandexBrowser", "19.4.1.454.00"));
        assertEquals("18.14", commonSchema.getBrowserVersionMajor("YandexBrowser", "18.14.1.454.00"));
        assertEquals("18.14", commonSchema.getBrowserVersionMajor("YandexBrowser", "18.14"));
        assertEquals("19", commonSchema.getBrowserVersionMajor("YandexBrowser", "19"));
        assertEquals("34", commonSchema.getBrowserVersionMajor("Chrome", "34.0.1847"));
        assertEquals("12", commonSchema.getBrowserVersionMajor("Opera", "12"));
        assertEquals("12", commonSchema.getBrowserVersionMajor("Opera", "12.3"));
        assertEquals("74", commonSchema.getBrowserVersionMajor("GoogleSearch", "74.0.248026584"));
        assertEquals("8.3", commonSchema.getBrowserVersionMajor("YandexSearch", "8.30"));
        assertEquals("7.1", commonSchema.getBrowserVersionMajor("YandexSearch", "7.16"));

    }
}
