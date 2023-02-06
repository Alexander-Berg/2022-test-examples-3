package ru.yandex.chemodan.app.eventloader.serializer.tests;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * @author Dmitriy Amelin (lemeh)
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({
        SerializeAlbumEventTest.class,
        SerializeBillingEventTest.class,
        SerializeFsEventTest.class,
        SerializeFsGroupEventTest.class,
        SerializeInviteEventTest.class,
        SerializeShareEventTest.class,
        SerializeSpaceEventTest.class,
        SerializeCommentEventTest.class,
})
public class SuiteOfSerializeEventTests {
}
