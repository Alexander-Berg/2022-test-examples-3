package ru.yandex.calendar.frontend.bender;

import org.junit.Test;

import ru.yandex.bolts.collection.Option;
import ru.yandex.calendar.test.CalendarTestBase;
import ru.yandex.misc.bender.Bender;
import ru.yandex.misc.bender.annotation.BenderBindAllFields;
import ru.yandex.misc.bender.config.BenderConfiguration;
import ru.yandex.misc.bender.serialize.BenderJsonSerializer;
import ru.yandex.misc.test.Assert;

/**
 * @author dbrylev
 */
public class ChangeTest extends CalendarTestBase {

    @BenderBindAllFields
    public static class Changes {
        public final Change<Integer> change;
        public final Option<Change<Integer>> someChange;
        public final Option<Change<Integer>> noneChange;
        public final Change<Option<Integer>> changeOptional;

        public Changes(
                Change<Integer> change,
                Option<Change<Integer>> someChange,
                Option<Change<Integer>> noneChange,
                Change<Option<Integer>> changeOptional)
        {
            this.change = change;
            this.someChange = someChange;
            this.noneChange = noneChange;
            this.changeOptional = changeOptional;
        }
    }

    @Test
    public void serialize() {
        Changes changes = new Changes(
                Change.change(0, 1),
                Change.changeO(0, 1),
                Change.changeO(0, 0),
                Change.change(Option.empty(), Option.of(1)));

        BenderJsonSerializer<Changes> serializer = Bender.jsonSerializer(
                Changes.class,
                ChangeBenderConfiguration.extend(BenderConfiguration.defaultConfiguration()));

        Assert.equals("{" +
                "\"change\":{\"before\":0,\"after\":1}," +
                "\"someChange\":{\"before\":0,\"after\":1}," +
                "\"changeOptional\":{\"after\":1}" +
                "}", new String(serializer.serializeJson(changes)));
    }
}
