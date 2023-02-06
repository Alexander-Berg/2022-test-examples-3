package ru.yandex.calendar.frontend.bender;

import org.junit.Test;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.calendar.test.CalendarTestBase;
import ru.yandex.misc.test.Assert;

/**
 * @author dbrylev
 */
public class FilteringFieldsParserTest extends CalendarTestBase {

    @Test
    public void parse() {
        String input = "events(organizer(login),attendees/login,actions(edit,invite)),events/id";

        ListF<String> expected = Cf.list(
                "events/organizer/login",
                "events/attendees/login",
                "events/actions/edit",
                "events/actions/invite",
                "events/id");

        Assert.assertListsEqual(expected, FilteringFieldsParser.parseFields(input));
    }
}
