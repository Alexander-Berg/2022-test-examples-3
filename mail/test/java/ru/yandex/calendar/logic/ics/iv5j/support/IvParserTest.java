package ru.yandex.calendar.logic.ics.iv5j.support;

import org.junit.Test;

import ru.yandex.calendar.test.CalendarTestBase;
import ru.yandex.misc.test.Assert;

/**
 * @author dbrylev
 */
public class IvParserTest extends CalendarTestBase {

    @Test
    public void semicolonedParameter() {
        String property = "X-APPLE-STRUCTURED-LOCATION;X-TITLE=\"New;Jersey\":geo:40.110132,-74.655620";
        Assert.equals("New;Jersey", IvParser.parseIcsProperty(property).getParameter("X-TITLE").get().getValue());
    }
}
