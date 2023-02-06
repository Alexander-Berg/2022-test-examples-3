package ru.yandex.market.logshatter.reader.logbroker2.topic;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Alexander Kedrik <a href="mailto:alkedr@yandex-team.ru"></a>
 * @date 21.03.2019
 */
public class CompressionRatioCalculatorTest {
    @Test
    public void shouldReturnDefaultValue_whenThereIsNoDataYet() {
        CompressionRatioCalculator calculator = new CompressionRatioCalculator();
        assertFalse(calculator.canCalculatePreciseCompressionRatio());
        assertEquals(CompressionRatioCalculator.DEFAULT_COMPRESSION_RATIO, calculator.getCompressionRatio(), 0.1);
    }

    @Test
    public void shouldReturnDefaultValue_whenThereIsNotEnoughData() {
        CompressionRatioCalculator calculator = new CompressionRatioCalculator();
        calculator.add(100, 100);
        assertFalse(calculator.canCalculatePreciseCompressionRatio());
        assertEquals(CompressionRatioCalculator.DEFAULT_COMPRESSION_RATIO, calculator.getCompressionRatio(), 0.1);
    }

    @Test
    public void shouldReturnCompressionRatio_whenThereIsEnoughData() {
        CompressionRatioCalculator calculator = new CompressionRatioCalculator();
        calculator.add(512*1024*1024, 1024*1024*1024);
        assertTrue(calculator.canCalculatePreciseCompressionRatio());
        assertEquals(2, calculator.getCompressionRatio(), 0.1);
    }
}
