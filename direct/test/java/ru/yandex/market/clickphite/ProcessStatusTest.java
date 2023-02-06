package ru.yandex.market.clickphite;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Anton Sukhonosenko <a href="mailto:algebraic@yandex-team.ru"></a>
 * @date 28.12.16
 */
public class ProcessStatusTest {
    @Test
    public void firstErrorTimeIsNull() throws Exception {
        ProcessStatus sut = new ProcessStatus();
        assertEquals(-1, sut.getFirstErrorTimeInARowMillis());
    }

    @Test
    public void firstErrorTimeIsNotNullOnError() throws Exception {
        ProcessStatus sut = new ProcessStatus();
        sut.setError(true);
        assertNotNull(sut.getFirstErrorTimeInARowMillis());
    }

    @Test
    public void firstErrorTimeIsNullAfterSuccess() throws Exception {
        ProcessStatus sut = new ProcessStatus();
        sut.setError(true);
        sut.setError(false);
        assertEquals(-1, sut.getFirstErrorTimeInARowMillis());
    }

    @Test
    public void firstErrorTimeValue() throws Exception {
        ProcessStatus sut = new ProcessStatus();
        sut.setError(true);
        long firstErrorTimeInARowMillis = sut.getFirstErrorTimeInARowMillis();
        sut.setError(true);
        assertEquals(firstErrorTimeInARowMillis, sut.getFirstErrorTimeInARowMillis());
    }
}