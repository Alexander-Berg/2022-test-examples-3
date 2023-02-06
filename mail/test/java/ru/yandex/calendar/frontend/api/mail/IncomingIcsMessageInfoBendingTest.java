package ru.yandex.calendar.frontend.api.mail;

import java.util.Optional;

import org.joda.time.Duration;
import org.joda.time.Instant;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.Option;
import ru.yandex.calendar.logic.beans.generated.Office;
import ru.yandex.calendar.logic.beans.generated.Resource;
import ru.yandex.calendar.logic.resource.ResourceInfo;
import ru.yandex.calendar.logic.user.Language;
import ru.yandex.misc.test.Assert;
import ru.yandex.misc.time.InstantInterval;
import ru.yandex.misc.time.MoscowTime;

/**
 * @author dbrylev
 */
public class IncomingIcsMessageInfoBendingTest {

    @Test
    public void serialize() {
        Instant start = MoscowTime.instant(2017, 2, 20, 22, 0);

        IncomingIcsMessageInfo info = new IncomingIcsMessageInfo(
                Optional.of(new XivaReminderSpecificData(1, "external", start)),
                "subject", "location", Cf.list(), new InstantInterval(start, start.plus(Duration.standardHours(1))),
                Optional.of(IncomingIcsMessageInfo.Type.UPDATE), Language.RUSSIAN);

        Assert.equals("{" +
                "\"service\":\"calendar\",\"operation\":\"meeting-reminder\"," +
                "\"eventId\":1,\"externalId\":\"external\",\"instanceStartTs\":\"2017-02-20T19:00:00\"," +
                "\"subject\":\"subject\",\"location\":\"location\"," +
                "\"start\":1487617200000,\"end\":1487620800000,\"type\":\"update\"}", info.serializeToJson());

        Resource resource = Mockito.mock(Resource.class);
        Mockito.when(resource.getName()).thenReturn(Option.of("Room"));
        Mockito.when(resource.getNameEn()).thenReturn(Option.empty());

        info = new IncomingIcsMessageInfo(
                Optional.empty(),
                "subject", "location", Cf.list(new ResourceInfo(resource, Mockito.mock(Office.class))),
                new InstantInterval(start, start.plus(Duration.standardHours(1))),
                Optional.empty(), Language.RUSSIAN);

        Assert.equals("{" +
                "\"subject\":\"subject\",\"location\":\"Room\"," +
                "\"start\":1487617200000,\"end\":1487620800000}", info.serializeToJson());
    }
}
