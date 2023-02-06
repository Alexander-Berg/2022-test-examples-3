package ru.yandex.calendar;

import org.junit.Test;

import ru.yandex.calendar.logic.event.ActionSource;
import ru.yandex.misc.test.Assert;
import ru.yandex.misc.thread.WhatThreadDoes;

/**
 * @author gutman
 */
public class CalendarRequestTest {

    @Test
    public void getCurrent() {
        CalendarRequestHandle handle = CalendarRequest.push(ActionSource.WEB, "what");
        try {
            Assert.A.notNull(handle);
            Assert.A.isTrue(handle == CalendarRequest.getCurrent());
        } finally {
            handle.pop();
        }
    }

    @Test
    public void pushPop() {
        CalendarRequestHandle handle = CalendarRequest.push(ActionSource.WEB, "1");
        try {
            Assert.A.equals("1", WhatThreadDoes.current().getString());
            CalendarRequest.push(ActionSource.WEB, "2");
            Assert.A.equals("2", WhatThreadDoes.current().getString());
            CalendarRequest.getCurrent().pop();
            Assert.A.equals("1", WhatThreadDoes.current().getString());
        } finally {
            handle.pop();
        }
    }

}

