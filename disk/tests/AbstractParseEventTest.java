package ru.yandex.chemodan.eventlog.log.tests;

import org.joda.time.Instant;

import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.eventlog.events.AbstractEvent;
import ru.yandex.chemodan.eventlog.events.EventMetadata;
import ru.yandex.chemodan.eventlog.events.EventType;
import ru.yandex.chemodan.eventlog.events.Resource;
import ru.yandex.chemodan.eventlog.events.YandexCloudRequestId;
import ru.yandex.chemodan.eventlog.log.TskvEventLogLine;
import ru.yandex.chemodan.mpfs.MpfsUid;
import ru.yandex.misc.test.Assert;

/**
 * @author Dmitriy Amelin (lemeh)
 */
public abstract class AbstractParseEventTest {
    protected static final long TIME = 1442015417L;

    protected static final YandexCloudRequestId YANDEX_CLOUD_REQUEST_ID =
            YandexCloudRequestId.parse("mpfs-d65d8cc3854f675d30eb0b7232567452-lemeh-precise");

    protected static final MpfsUid UID = new MpfsUid(4001210263L);

    protected static final EventMetadata EVENT_METADATA =
            new EventMetadata(UID, new Instant(TIME * 1000), YANDEX_CLOUD_REQUEST_ID);

    protected static final Resource FILE_RESOURCE = Resource.file(
            "text",
            "f062e2a85e207fa6808c131555687e513f6e8b400959b27e6d01905928379ace",
            UID);

    protected static final Resource OVERWRITTEN_FILE_RESOURCE = FILE_RESOURCE.withOverwritten(true);

    protected static final Resource PUBLIC_FILE_RESOURCE =
            FILE_RESOURCE.withKeyAndUrl("1234567890", "https://dummy.ya.ru/1234567890");

    protected static final String FILE_RESOURCE_LINE = "resource_type=file\tresource_media_type=text\t" +
            "resource_file_id=" + FILE_RESOURCE.fileId + "\towner_uid=" + UID;

    protected static final String OVERWRITTEN_FILE_RESOURCE_LINE = FILE_RESOURCE_LINE + "\toverwritten=true";

    protected static final Option<String> TYPE_TRASH_RESTORE = Option.of("trash_restore");

    protected static final Option<String> SUBTYPE_DISK = Option.of("disk");

    protected static final String PUBLIC_FILE_RESOURCE_LINE = FILE_RESOURCE_LINE +
            "\tpublic_key=" + PUBLIC_FILE_RESOURCE.publicKey.get() +
            "\tshort_url=" + PUBLIC_FILE_RESOURCE.shortUrl.get();

    protected void assertParseEquals(MpfsUid uid, String typeStr, String lineSuffix, AbstractEvent expectedEvent) {
        assertParseEquals(uid, typeStr, lineSuffix, expectedEvent, Option.empty());
    }

    protected void assertParseEquals(MpfsUid uid, String typeStr, String lineSuffix, AbstractEvent expectedEvent,
            EventType expectedEventType)
    {
        assertParseEquals(uid, typeStr, lineSuffix, expectedEvent, Option.of(expectedEventType));
    }

    private void assertParseEquals(MpfsUid uid, String typeStr, String lineSuffix, AbstractEvent expectedEvent,
            Option<EventType> expectedEventType)
    {
        String line = buildEventLine(uid, typeStr, lineSuffix);
        Assert.equals(expectedEvent, TskvEventLogLine.parse(line).toEvent().get());

        if (expectedEventType.isPresent()) {
            Assert.equals(expectedEventType.get(), expectedEvent.getEventType());
        } else {
            Assert.notNull(expectedEvent.getEventType());
        }
    }

    protected void assertSkipped(MpfsUid uid, String typeStr, String lineSuffix) {
        Assert.none(TskvEventLogLine.parse(buildEventLine(uid, typeStr, lineSuffix)).getEventTypeO());
    }

    private static String buildEventLine(MpfsUid uid, String typeStr, String lineSuffix) {
        return "tskv\t"
                + "unixtime=" + TIME + "\t"
                + "req_id=" + YANDEX_CLOUD_REQUEST_ID + "\t"
                + "tskv_format=ydisk-event-history\t"
                + "event_type=" + typeStr + "\t"
                + "uid=" + uid + "\t"
                + lineSuffix;
    }
}
