package ru.yandex.calendar.logic.ics.iv5j.ical;

import net.fortuna.ical4j.model.component.VTimeZone;
import org.joda.time.DateTimeZone;
import org.junit.Test;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.Option;
import ru.yandex.misc.test.Assert;

/**
 * @author shinderuk
 */
public class TimeZoneRegistry2Test {

    @Test
    public void getAllAvailableTimeZones() {
        for (String id : Cf.x(DateTimeZone.getAvailableIDs()).filterNot(i -> i.startsWith("tz20"))) {
            Assert.some(TimeZoneRegistry2.fullTimeZones.getVTimeZone(id), id);
            Assert.some(TimeZoneRegistry2.outlookTimeZones.getVTimeZone(id), id);
        }
    }

    /**
     * @see net/fortuna/ical4j/model/tz.alias
     */
    @Test
    public void timeZoneAliases() {
        timeZoneAliases(TimeZoneRegistry2.fullTimeZones);
        timeZoneAliases(TimeZoneRegistry2.outlookTimeZones);
    }

    private void timeZoneAliases(TimeZoneRegistry2 tzr) {
        Option<VTimeZone> vtz1 = tzr.getVTimeZone("America/Los_Angeles");
        Assert.some(vtz1);
        Option<VTimeZone> vtz2 = tzr.getVTimeZone("US/Pacific");  // alias for America/Los_Angeles
        Assert.equals(vtz1, vtz2);
    }

    /**
     * {@link net.fortuna.ical4j.model.TimeZoneRegistryImpl} fails this test
     */
    @Test
    public void multipleIndependentInstances() {
        Option<VTimeZone> fullTz = TimeZoneRegistry2.fullTimeZones.getVTimeZone("Europe/Moscow");
        Assert.some(fullTz);
        Assert.isTrue(fullTz.get().getObservances().size() > 10);

        Option<VTimeZone> outlookTz = TimeZoneRegistry2.outlookTimeZones.getVTimeZone("Europe/Moscow");
        Assert.some(outlookTz);
        Assert.isTrue(outlookTz.get().getObservances().size() <= 2);
    }

    // CAL-6221
    @Test
    public void fixedTimeZone() {
        Assert.some(TimeZoneRegistry2.fullTimeZones.getVTimeZone(DateTimeZone.forOffsetHours(-13).getID()));
        Assert.some(TimeZoneRegistry2.outlookTimeZones.getVTimeZone(DateTimeZone.forOffsetHours(-13).getID()));
    }
}
