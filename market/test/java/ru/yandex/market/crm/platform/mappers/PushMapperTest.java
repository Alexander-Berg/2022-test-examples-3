package ru.yandex.market.crm.platform.mappers;

import java.nio.charset.StandardCharsets;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.junit.jupiter.api.Test;

import ru.yandex.market.crm.platform.common.Uids;
import ru.yandex.market.crm.platform.commons.MobilePlatform;
import ru.yandex.market.crm.platform.models.Push;
import ru.yandex.market.crm.util.CrmCollections;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static ru.yandex.market.crm.platform.commons.UidType.MM_DEVICE_ID;
import static ru.yandex.market.crm.platform.commons.UidType.MM_DEVICE_ID_HASH;
import static ru.yandex.market.crm.platform.commons.UidType.UUID;

/**
 * @author apershukov
 */
class PushMapperTest {

    private final PushMapper mapper = new PushMapper(new String[]{"23107", "23104"}, value -> true);

    @Test
    void testTestingAppIdNotSkippedInTesting() {
        String line = "tskv_format=metrika-mobile-log\t" +
                "APIKey=2780002\t" +
                "UUID=24af01e3c4579a26f1a47dc29a811d7fd\t" +
                "DeviceID=75b685608991a824ffdf5191671227ee\t" +
                "DeviceIDHash=71326149106179661561\t" +
                "AppPlatform=iOS\t" +
                "PushActionType=3\t" +
                "PushCampaignID=36708\t" +
                "PushCorrelationID=6d264721-5793-4203-a5a5-ba7116311226\t" +
                "PushHypothesisID=41145\t" +
                "PushMessageID=183594\t" +
                "EventTimestamp=1529491013\t" +
                "EventValue={\"action\":{\"type\":\"dismiss\"}," +
                "\"notification_id\":\"m=183594&cor=6d264721-5793-4203-a5a5-ba7116311226\"}";

        assertNull(map(line));
    }

    @Test
    void testPushMapperDismissCampaignPush() {
        String line = "tskv_format=metrika-mobile-log\t" +
                "APIKey=23107\t" +
                "UUID=24af01e3c4579a26f1a47dc29a811d7fd\t" +
                "DeviceID=75b685608991a824ffdf5191671227ee\t" +
                "DeviceIDHash=71326149106179661561\t" +
                "AppPlatform=iOS\t" +
                "PushActionType=3\t" +
                "PushCampaignID=36708\t" +
                "PushCorrelationID=6d264721-5793-4203-a5a5-ba7116311226\t" +
                "PushHypothesisID=41145\t" +
                "PushMessageID=183594\t" +
                "EventTimestamp=1529491013\t" +
                "EventValue={\"action\":{\"type\":\"dismiss\"}," +
                "\"notification_id\":\"m=183594&cor=6d264721-5793-4203-a5a5-ba7116311226\"}";

        var fact = map(line);
        assertNotNull(fact);

        var expected = Push.newBuilder()
                .setUid(Uids.create(UUID, "24af01e3c4579a26f1a47dc29a811d7fd"))
                .addUids(Uids.create(MM_DEVICE_ID, "75b685608991a824ffdf5191671227ee"))
                .addUids(Uids.create(MM_DEVICE_ID_HASH, "71326149106179661561"))
                .setTimestamp(1529491013000L)
                .setReaction(Push.Action.DISMISS)
                .setReactionTimestamp(1529491013000L)
                .setMetrikaInfo(
                        Push.MetrikaInfo.newBuilder()
                                .setAppId("23107")
                                .setCampaignId("36708")
                                .setMessageId("183594")
                )
                .setMobilePlatform(MobilePlatform.IOS)
                .setFactId("campaignId=36708;messageId=183594")
                .build();

        assertEquals(expected, fact);
    }

    @Test
    void testPushMapperMissingEventType() {
        var line = "tskv_format=metrika-mobile-log\t" +
                "UUID=02a7929bce7aeb5e202d5beda700562d\t" +
                "APIKey=23107\t" +
                "EventValue={\"notification_id\":\"t=111\",\"action\":{\"type\":\"receive\"}}\t" +
                "EventTimestamp=1529491013";

        assertNull(map(line));
    }

    @Test
    void testPushMapperMissingRequiredField() {
        var missingUUID = "tskv_format=metrika-mobile-log\t" +
                "PushActionType=2\t" +
                "APIKey=23107\t" +
                "EventValue={\"notification_id\":\"t=111\",\"action\":{\"type\":\"receive\"}}\t" +
                "EventTimestamp=1";

        var ok = "tskv_format=metrika-mobile-log\t" +
                "PushActionType=2\t" +
                "UUID=4\t" +
                "APIKey=23104\t" +
                "EventValue={\"notification_id\":\"t=333\",\"action\":{\"type\":\"receive\"}}\t" +
                "EventTimestamp=4";

        String lines = String.join("\n", missingUUID, ok);

        var facts = mapper.apply(lines.getBytes(StandardCharsets.UTF_8));
        assertThat(facts, hasSize(1));

        var okPush = Push.newBuilder()
                .setUid(Uids.create(UUID, "4"))
                .setReceiveTimestamp(4000L)
                .setTimestamp(4000L)
                .setMetrikaInfo(
                        Push.MetrikaInfo.newBuilder()
                                .setAppId("23104")
                                .setTransferId("333")
                )
                .setFactId("transferId=333")
                .build();

        assertEquals(okPush, facts.get(0));
    }

    @Test
    void testPushMapperMissingFactIdFields() {
        var missingAll = "tskv_format=metrika-mobile-log\t" +
                "PushActionType=2\t" +
                "UUID=4\t" +
                "APIKey=23104\t" +
                "EventValue={\"action\":{\"type\":\"receive\"}}\t" +
                "EventTimestamp=4";

        var missingCampaignId1 = "tskv_format=metrika-mobile-log\t" +
                "PushActionType=2\t" +
                "UUID=4\t" +
                "APIKey=23104\t" +
                "EventValue={\"action\":{\"type\":\"receive\"}}\t" +
                "PushMessageID=183594\t" +
                "EventTimestamp=4";

        var missingCampaignId2 = "tskv_format=metrika-mobile-log\t" +
                "PushActionType=2\t" +
                "UUID=4\t" +
                "APIKey=23104\t" +
                "EventValue={\"notification_id\":\"m=111\",\"action\":{\"type\":\"receive\"}}\t" +
                "EventTimestamp=4";

        var missingMessageId = "tskv_format=metrika-mobile-log\t" +
                "PushActionType=2\t" +
                "UUID=4\t" +
                "APIKey=23104\t" +
                "EventValue={\"action\":{\"type\":\"receive\"}}\t" +
                "PushCampaignID=36708\t" +
                "EventTimestamp=4";

        var lines = String.join("\n", missingAll, missingCampaignId1, missingCampaignId2, missingMessageId);

        var facts = mapper.apply(lines.getBytes());
        assertThat(facts, empty());
    }

    @Test
    void testPushMapperDirectTransferIdAsFactId() {
        var line = "tskv_format=metrika-mobile-log\t" +
                "PushActionType=2\t" +
                "UUID=4\t" +
                "APIKey=23104\t" +
                "EventValue={\"notification_id\":\"t=1155678\",\"action\":{\"type\":\"receive\"}}\t" +
                "PushTransferID=1155677\t" +
                "XivaTransitID=193594\t" +
                "PushCampaignID=36708\t" +
                "PushMessageID=183594\t" +
                "EventTimestamp=4";

        var fact = map(line);
        assertNotNull(fact);
        assertEquals("1155677", fact.getMetrikaInfo().getTransferId());
        assertEquals("transferId=1155677", fact.getFactId());
    }

    @Test
    void testPushMapperIndirectTransferIdAsFactId() {
        String line = "tskv_format=metrika-mobile-log\t" +
                "PushActionType=2\t" +
                "UUID=4\t" +
                "APIKey=23104\t" +
                "EventValue={\"notification_id\":\"t=1155678\",\"action\":{\"type\":\"receive\"}}\t" +
                "XivaTransitID=193594\t" +
                "PushCampaignID=36708\t" +
                "PushMessageID=183594\t" +
                "EventTimestamp=4";

        var fact = map(line);
        assertNotNull(fact);
        assertEquals("1155678", fact.getMetrikaInfo().getTransferId());
        assertEquals("transferId=1155678", fact.getFactId());
    }

    @Test
    void testPushMapperIndirectCampaignAndMessageIdsAsFactId() {
        var line = "tskv_format=metrika-mobile-log\t" +
                "PushActionType=2\t" +
                "UUID=4\t" +
                "APIKey=23104\t" +
                "EventValue={\"notification_id\":\"m=183593\",\"action\":{\"type\":\"receive\"}}\t" +
                "PushCampaignID=36708\t" +
                "EventTimestamp=4";

        var fact = map(line);
        assertNotNull(fact);
        assertEquals("36708", fact.getMetrikaInfo().getCampaignId());
        assertEquals("183593", fact.getMetrikaInfo().getMessageId());
        assertEquals("campaignId=36708;messageId=183593", fact.getFactId());
    }

    @Test
    void testPushMapperOpenApiPush() {
        var line = "tskv_format=metrika-mobile-log\t" +
                "APIKey=23107\t" +
                "UUID=02a7929bce7aeb5e202d5beda700562d\t" +
                "DeviceID=4ffdf5191671227ee75b685608991a82\t" +
                "DeviceIDHash=17966156171326149106\t" +
                "AppPlatform=android\t" +
                "PushActionType=4\t" +
                "PushGroupID=3110\t" +
                "PushTag=skid070119_a\t" +
                "PushTransferID=1155677\t" +
                "EventTimestamp=1529491013\t" +
                "EventValue={\"notification_id\":\"t=1155677\",\"action\":{\"type\":\"open\"}}";

        var fact = map(line);
        assertNotNull(fact);

        var expected = Push.newBuilder()
                .setUid(Uids.create(UUID, "02a7929bce7aeb5e202d5beda700562d"))
                .addUids(Uids.create(MM_DEVICE_ID, "4ffdf5191671227ee75b685608991a82"))
                .addUids(Uids.create(MM_DEVICE_ID_HASH, "17966156171326149106"))
                .setTimestamp(1529491013L * 1000)
                .setReaction(Push.Action.OPEN)
                .setReactionTimestamp(1529491013L * 1000)
                .setMobilePlatform(MobilePlatform.ANDROID)
                .setMetrikaInfo(
                        Push.MetrikaInfo.newBuilder()
                                .setAppId("23107")
                                .setGroupId("3110")
                                .setTransferId("1155677")
                )
                .setFactId("transferId=1155677")
                .build();

        assertEquals(expected, fact);
    }

    @Test
    void testPushMapperReceiveApiPush() {
        var line = "tskv_format=metrika-mobile-log\t" +
                "APIKey=23104\t" +
                "UUID=202d5beda700562d02a7929bce7aeb5e\t" +
                "DeviceID=608991a824ffdf5191671227ee75b685\t" +
                "DeviceIDHash=56171326149106179661\t" +
                "AppPlatform=iOS\t" +
                "PushActionType=2\t" +
                "PushGroupID=3107\t" +
                "PushTag=skid0701_a\t" +
                "PushTransferID=1155686\t" +
                "EventTimestamp=1529491013\t" +
                "EventValue={\"notification_id\":\"t=1155686\",\"action\":{\"type\":\"receive\"}}";

        var fact = map(line);
        assertNotNull(fact);

        var expected = Push.newBuilder()
                .setUid(Uids.create(UUID, "202d5beda700562d02a7929bce7aeb5e"))
                .addUids(Uids.create(MM_DEVICE_ID, "608991a824ffdf5191671227ee75b685"))
                .addUids(Uids.create(MM_DEVICE_ID_HASH, "56171326149106179661"))
                .setTimestamp(1529491013L * 1000)
                .setReceiveTimestamp(1529491013L * 1000)
                .setMobilePlatform(MobilePlatform.IOS)
                .setMetrikaInfo(
                        Push.MetrikaInfo.newBuilder()
                                .setAppId("23104")
                                .setGroupId("3107")
                                .setTransferId("1155686")
                )
                .setFactId("transferId=1155686")
                .build();

        assertEquals(expected, fact);
    }

    @Nullable
    private Push map(@Nonnull String line) {
        var facts = mapper.apply(line.getBytes(StandardCharsets.UTF_8));
        return CrmCollections.isEmpty(facts) ? null : facts.get(0);
    }
}
