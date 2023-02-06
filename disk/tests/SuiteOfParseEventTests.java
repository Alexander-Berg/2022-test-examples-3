package ru.yandex.chemodan.eventlog.log.tests;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * @author Dmitriy Amelin (lemeh)
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({
        ParseAlbumEventTest.class,
        ParseBillingEventTest.class,
        ParseFsEventTest.class,
        ParseFsGroupEventTest.class,
        ParseInviteEventTest.class,
        ParseShareEventTest.class,
        ParseSpaceEventTest.class,
        ParseCommentEventTest.class,
        ParseMiscEventTest.class,
        ParseSharedFolderInviteEventTest.class,
})
public class SuiteOfParseEventTests {
}
