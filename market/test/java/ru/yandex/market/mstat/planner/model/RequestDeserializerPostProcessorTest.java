package ru.yandex.market.mstat.planner.model;

import org.junit.Test;
import ru.yandex.market.mstat.planner.util.RestUtil;

import static org.junit.Assert.*;
import static ru.yandex.market.mstat.planner.model.RequestDeserializerPostProcessor.calcDateEnd;

public class RequestDeserializerPostProcessorTest {

    @Test
    public void t() {
        assertEquals("07.08.2018",RestUtil.dateToStr(
            calcDateEnd(new java.sql.Date(118, 7, 1), "7d")));
        assertEquals("01.08.2018",RestUtil.dateToStr(
            calcDateEnd(new java.sql.Date(118, 7, 1), "1d")));
        assertEquals("31.10.2018",RestUtil.dateToStr(
            calcDateEnd(new java.sql.Date(118, 7, 1), "1q")));
    }

}
