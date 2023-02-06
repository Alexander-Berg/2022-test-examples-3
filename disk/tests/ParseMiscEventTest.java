package ru.yandex.chemodan.eventlog.log.tests;

import org.junit.Test;

import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.eventlog.events.CompoundResourceType;
import ru.yandex.chemodan.eventlog.events.misc.PublicVisitEvent;

/**
 * @author dbrylev
 */
public class ParseMiscEventTest extends AbstractParseEventTest {

    @Test
    public void testPublicVisit() {
        String url = "https://yadi.sk/i/n2K9HmSBg6zBJ";

        assertParseEquals(
                UID, "public-visit", "short_url=" + url
                        + "\tresource_type=file"
                        + "\tresource_media_type=image"
                        + "\tuid_is_owner=True"
                        + "\tuid_is_invited=False",
                new PublicVisitEvent(EVENT_METADATA, url,
                        Option.of(CompoundResourceType.file("image")), Option.of(true), Option.of(false)));
    }
}
