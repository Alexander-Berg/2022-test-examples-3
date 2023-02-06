package ru.yandex.calendar.logic.event.repetition;

import org.joda.time.Instant;
import org.junit.Test;

import ru.yandex.bolts.collection.Option;
import ru.yandex.misc.test.Assert;

/**
 * @author Stepan Koltsov
 */
public class InfiniteIntervalTest {

    @Test
    public void finite() {
        InfiniteInterval interval = new InfiniteInterval(new Instant(1000), Option.of(new Instant(2000)));
        Assert.A.isTrue(!interval.contains(new Instant(500)));
        Assert.A.isTrue(interval.contains(new Instant(1500)));
        Assert.A.isTrue(!interval.contains(new Instant(2500)));
    }

    @Test
    public void infinite() {
        InfiniteInterval interval = new InfiniteInterval(new Instant(1000), Option.<Instant>empty());
        Assert.A.isTrue(!interval.contains(new Instant(500)));
        Assert.A.isTrue(interval.contains(new Instant(1500)));
    }

} //~
