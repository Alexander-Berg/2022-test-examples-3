package ru.yandex.chemodan.app.tcm.utils;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.joda.time.Instant;
import org.junit.Assert;

import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.app.tcm.db.ConferenceMeta;

/**
 * @author friendlyevil
 */
public final class ConferenceTestUtils {
    public static void assertConferenceMetaEquals(ConferenceMeta first, ConferenceMeta second) {
        Assert.assertEquals(first.getConferenceId(), second.getConferenceId());
        Assert.assertEquals(first.getShortUrlId(), second.getShortUrlId());
        Assert.assertEquals(first.getShardId(), second.getShardId());
    }

    public static ConferenceMeta generateConferenceMeta() {
        return new ConferenceMeta(null,
                generateShorUrlId(),
                generateConferenceId(),
                RandomUtils.nextInt(),
                Option.empty(),
                Instant.now());
    }

    public static String generateConferenceId() {
        return RandomStringUtils.random(10);
    }

    public static String generateShorUrlId() {
        return RandomStringUtils.random(10);
    }
}
