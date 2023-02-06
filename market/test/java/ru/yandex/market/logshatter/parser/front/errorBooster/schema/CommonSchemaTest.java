package ru.yandex.market.logshatter.parser.front.errorBooster.schema;

import java.util.Arrays;
import java.util.HashMap;

import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.Test;

import ru.yandex.market.health.configs.logshatter.useragent.UserAgentDetector;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CommonSchemaTest {

    @Test
    public void getOsVersionMajor() {
        CommonSchema commonSchema = new CommonSchema();
        assertEquals("5", commonSchema.getOsVersionMajor(
            ImmutableMap.of(UserAgentDetector.OS_FAMILY, "Android", UserAgentDetector.OS_VERSION, "5.6.7.8"))
        );
        assertEquals("15", commonSchema.getOsVersionMajor(
            ImmutableMap.of(UserAgentDetector.OS_FAMILY, "Android", UserAgentDetector.OS_VERSION, "15.6.7.8"))
        );
        assertEquals("4.5", commonSchema.getOsVersionMajor(
            ImmutableMap.of(UserAgentDetector.OS_FAMILY, "Android", UserAgentDetector.OS_VERSION, "4.5.6.7"))
        );
        assertEquals("4.5", commonSchema.getOsVersionMajor(
            ImmutableMap.of(UserAgentDetector.OS_FAMILY, "Android", UserAgentDetector.OS_VERSION, "4.5"))
        );
        assertEquals("4", commonSchema.getOsVersionMajor(
            ImmutableMap.of(UserAgentDetector.OS_FAMILY, "Android", UserAgentDetector.OS_VERSION, "4"))
        );
        assertEquals("14.15", commonSchema.getOsVersionMajor(
            ImmutableMap.of(UserAgentDetector.OS_FAMILY, "MacOS", UserAgentDetector.OS_VERSION, "14.15.6.7"))
        );
        assertEquals("14", commonSchema.getOsVersionMajor(
            ImmutableMap.of(UserAgentDetector.OS_FAMILY, "MacOS", UserAgentDetector.OS_VERSION, "14"))
        );
        assertEquals("1.2", commonSchema.getOsVersionMajor(
            ImmutableMap.of(UserAgentDetector.OS_FAMILY, "Tizen", UserAgentDetector.OS_VERSION, "1.2"))
        );
        assertEquals("10", commonSchema.getOsVersionMajor(
            ImmutableMap.of(UserAgentDetector.OS_FAMILY, "Windows", UserAgentDetector.OS_VERSION, "10.0"))
        );
        assertEquals("12", commonSchema.getOsVersionMajor(
            ImmutableMap.of(UserAgentDetector.OS_FAMILY, "iOS", UserAgentDetector.OS_VERSION, "12.1.4"))
        );
    }

    @Test
    public void getIsRobot() {
        CommonSchema commonSchema = new CommonSchema();
        assertEquals(true, commonSchema.getIsRobot(
            ImmutableMap.of(UserAgentDetector.IS_ROBOT, "true", UserAgentDetector.BROWSER_NAME, "YandexBrowser"))
        );
        assertEquals(false, commonSchema.getIsRobot(
            ImmutableMap.of(UserAgentDetector.IS_ROBOT, "false", UserAgentDetector.BROWSER_NAME, "YandexBrowser"))
        );
        assertEquals(true, commonSchema.getIsRobot(
            ImmutableMap.of(UserAgentDetector.IS_ROBOT, "false", UserAgentDetector.BROWSER_NAME, ""))
        );
        assertEquals(true, commonSchema.getIsRobot(
            ImmutableMap.of(UserAgentDetector.IS_ROBOT, "false", UserAgentDetector.BROWSER_NAME, "Unknown"))
        );
    }

    @Test
    public void getBrowserVersionMajor() {
        CommonSchema commonSchema = new CommonSchema();
        assertEquals("19.4", commonSchema.getBrowserVersionMajor(
            ImmutableMap.of(UserAgentDetector.BROWSER_NAME, "YandexBrowser", UserAgentDetector.BROWSER_VERSION, "19.4" +
                ".1.454.00"))
        );
        assertEquals("18.14", commonSchema.getBrowserVersionMajor(
            ImmutableMap.of(UserAgentDetector.BROWSER_NAME, "YandexBrowser", UserAgentDetector.BROWSER_VERSION, "18" +
                ".14.1.454.00"))
        );
        assertEquals("18.14", commonSchema.getBrowserVersionMajor(
            ImmutableMap.of(UserAgentDetector.BROWSER_NAME, "YandexBrowser", UserAgentDetector.BROWSER_VERSION, "18" +
                ".14"))
        );
        assertEquals("19", commonSchema.getBrowserVersionMajor(
            ImmutableMap.of(UserAgentDetector.BROWSER_NAME, "YandexBrowser", UserAgentDetector.BROWSER_VERSION, "19"))
        );
        assertEquals("34", commonSchema.getBrowserVersionMajor(
            ImmutableMap.of(UserAgentDetector.BROWSER_NAME, "Chrome", UserAgentDetector.BROWSER_VERSION, "34.0.1847"))
        );
        assertEquals("12", commonSchema.getBrowserVersionMajor(
            ImmutableMap.of(UserAgentDetector.BROWSER_NAME, "Opera", UserAgentDetector.BROWSER_VERSION, "12"))
        );
        assertEquals("12", commonSchema.getBrowserVersionMajor(
            ImmutableMap.of(UserAgentDetector.BROWSER_NAME, "Opera", UserAgentDetector.BROWSER_VERSION, "12.3"))
        );
        assertEquals("74", commonSchema.getBrowserVersionMajor(
            ImmutableMap.of(UserAgentDetector.BROWSER_NAME, "GoogleSearch", UserAgentDetector.BROWSER_VERSION, "74.0" +
                ".248026584"))
        );
        assertEquals("8.30", commonSchema.getBrowserVersionMajor(
            ImmutableMap.of(UserAgentDetector.BROWSER_NAME, "YandexSearch", UserAgentDetector.BROWSER_VERSION, "8.30"))
        );
        assertEquals("7.1", commonSchema.getBrowserVersionMajor(
            ImmutableMap.of(UserAgentDetector.BROWSER_NAME, "YandexSearch", UserAgentDetector.BROWSER_VERSION, "7.1"))
        );

        assertEquals("7.16", commonSchema.getBrowserVersionMajor(
            ImmutableMap.of(UserAgentDetector.BROWSER_NAME, "YandexSearch", UserAgentDetector.BROWSER_VERSION, "7.16" +
                ".1.454.00"))
        );
    }

    @Test
    public void getTestIds() {
        CommonSchema commonSchema = new CommonSchema();

        assertEquals(Arrays.asList(), commonSchema.getTestIds(""));
        assertEquals(Arrays.asList(118156, 129002), commonSchema.getTestIds("118156,0,97;129002,0,40"));
        assertEquals(Arrays.asList(118156, 129002), commonSchema.getTestIds("129002,0,97;118156,0,40"));
    }

    @Test
    public void isRobot() {
        CommonSchema commonSchema = new CommonSchema();

        assertFalse(commonSchema.isRobot(new HashMap<>()));
        assertFalse(commonSchema.isRobot(ImmutableMap.of("robotness", "0.0")));
        assertFalse(commonSchema.isRobot(ImmutableMap.of("robotness", "1")));

        assertTrue(commonSchema.isRobot(ImmutableMap.of("is_robot", "1")));
        assertTrue(commonSchema.isRobot(ImmutableMap.of("is_robot", "0")));
        assertTrue(commonSchema.isRobot(ImmutableMap.of("robotness", "1.0")));
        assertTrue(commonSchema.isRobot(ImmutableMap.of("is_robot", "1", "robotness", "1.0")));
    }
}
