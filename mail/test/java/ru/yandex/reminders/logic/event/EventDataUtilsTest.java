package ru.yandex.reminders.logic.event;

import org.junit.Test;
import ru.yandex.bolts.collection.Option;
import ru.yandex.commune.json.JsonObject;
import ru.yandex.misc.test.Assert;

/**
 * @author dbrylev
 */
public class EventDataUtilsTest {

    @Test
    public void extractMid() {
        Assert.none(EventDataUtils.extractMid(Option.none()));
        Assert.some("35", EventDataUtils.extractMid(Option.some(JsonObject.parseObject("{\"mid\":\"35\"}"))));
        Assert.some("35", EventDataUtils.extractMid(Option.some(JsonObject.parseObject("{\"data\":{\"mid\":\"35\"}}"))));
        Assert.none(EventDataUtils.extractMid(Option.some(JsonObject.parseObject("{\"mid\":28,\"data\":42}"))));
    }
}
