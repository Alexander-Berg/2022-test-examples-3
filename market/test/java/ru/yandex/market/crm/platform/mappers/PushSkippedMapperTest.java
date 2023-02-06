package ru.yandex.market.crm.platform.mappers;

import java.nio.charset.StandardCharsets;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.junit.jupiter.api.Test;

import ru.yandex.market.crm.platform.common.Uids;
import ru.yandex.market.crm.platform.commons.CrmInfo;
import ru.yandex.market.crm.platform.commons.MobilePlatform;
import ru.yandex.market.crm.platform.commons.SendingPayload;
import ru.yandex.market.crm.platform.commons.SendingType;
import ru.yandex.market.crm.platform.commons.UidType;
import ru.yandex.market.crm.platform.models.PushSkipped;
import ru.yandex.market.crm.util.CrmCollections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * @author apershukov
 */
class PushSkippedMapperTest {

    private final PushSkippedMapper mapper = new PushSkippedMapper();

    @Test
    void testLocalControlLine() {
        var line = "tskv\t" +
                "status=SKIPPED_AS_CONTROL\t" +
                "control=true\t" +
                "appId=23104\t" +
                "platform=ANDROID\t" +
                "type=1\t" +
                "account=beru\t" +
                "sendingId=test_sending\t" +
                "uuid=1a7d3f5e38ad4a6af1\t" +
                "deviceId=a326df16-e1a7fa-79e3dc\t" +
                "deviceIdHash=2cdc2995f30ac\t" +
                "timestamp=12344656677546\t" +
                "application=market\t" +
                "segmentId=test_segment\t" +
                "variantId=test_sending_a\t" +
                "payloadJson=[" +
                "{\"name\":\"ORDER_ID\", \"value\":\"555\"}, " +
                "{\"name\":\"SKU_IDS\", \"value\":\"some_sku_id1,some_sku_id2\"}, " +
                "{\"name\":\"UNKNOWN\", \"value\":\"asd\"}" +
                "]";

        var fact = map(line);
        assertNotNull(fact, "No fact parsed");

        var expected = PushSkipped.newBuilder()
                .setFactId("beru;sending=test_sending")
                .setTimestamp(12344656677546L)
                .setStatus("SKIPPED_AS_CONTROL")
                .setSendingType(SendingType.PROMO)
                .setCrmInfo(
                        CrmInfo.newBuilder()
                                .setAccount("beru")
                                .setSendingId("test_sending")
                                .setVariantId("test_sending_a")
                                .setSegmentId("test_segment")
                )
                .setAppId("23104")
                .setApplication("market")
                .setIsControl(true)
                .setMobilePlatform(MobilePlatform.ANDROID)
                .setUid(Uids.create(UidType.UUID, "1a7d3f5e38ad4a6af1"))
                .addUids(Uids.create(UidType.MM_DEVICE_ID, "a326df16-e1a7fa-79e3dc"))
                .addUids(Uids.create(UidType.MM_DEVICE_ID_HASH, "2cdc2995f30ac"))
                .addPayload(
                        SendingPayload.newBuilder()
                                .setKey(SendingPayload.Key.ORDER_ID)
                                .setValue("555")
                )
                .addPayload(
                        SendingPayload.newBuilder()
                                .setKey(SendingPayload.Key.SKU_IDS)
                                .setValue("some_sku_id1,some_sku_id2")
                )
                .build();

        assertEquals(expected, fact);
    }

    /**
     * Выгруженное сообщение со статусом UPLOADED пропускается
     */
    @Test
    void testSkipSentPushWithStatus() {
        var line = "tskv\t" +
                "status=UPLOADED\t" +
                "control=false\t" +
                "appId=23107\t" +
                "platform=IOS\t" +
                "type=1\t" +
                "account=bringly\t" +
                "sendingId=sendingAbc\t" +
                "variantId=variantCde\t" +
                "segmentId=segmentFgh\t" +
                "uuid=3450ad9c67df2a10e4\t" +
                "deviceId=45adfc21-23facd-98afcd\t" +
                "deviceIdHash=3da4e23fa2058\t" +
                "title=Test\t" +
                "text=Текст пуш сообщения.\\nТело.\t" +
                "transferId=6736710\t" +
                "timestamp=13454637534413675";

        assertNull(map(line));
    }

    /**
     * Сообщение, не попавшее в контроль, без статуса UPLOADED пропускается.
     * Нужно для обратной совместимости. См. LILUCRM-5506
     */
    @Test
    void testSkipSentWithoutStatus() {
        var line = "tskv\t" +
                "control=false\t" +
                "appId=23107\t" +
                "platform=IOS\t" +
                "type=1\t" +
                "account=bringly\t" +
                "sendingId=sendingAbc\t" +
                "variantId=variantCde\t" +
                "segmentId=segmentFgh\t" +
                "uuid=3450ad9c67df2a10e4\t" +
                "deviceId=45adfc21-23facd-98afcd\t" +
                "deviceIdHash=3da4e23fa2058\t" +
                "title=Test\t" +
                "text=Текст пуш сообщения.\\nТело.\t" +
                "transferId=6736710\t" +
                "timestamp=13454637534413675";

        assertNull(map(line));
    }

    /**
     * Сообщение попавшее в контроль но не имеющее специального статуса не пропускается.
     * Нужно для обратной совместимости. См. LILUCRM-5506
     */
    @Test
    void testControlWithoutStatus() {
        var line = "tskv\t" +
                "control=true\t" +
                "appId=23104\t" +
                "platform=ANDROID\t" +
                "type=1\t" +
                "account=beru\t" +
                "sendingId=test_sending\t" +
                "uuid=1a7d3f5e38ad4a6af1\t" +
                "deviceId=a326df16-e1a7fa-79e3dc\t" +
                "deviceIdHash=2cdc2995f30ac\t" +
                "timestamp=12344656677546\t" +
                "application=market\t" +
                "segmentId=test_segment\t" +
                "variantId=test_sending_a";

        var fact = map(line);
        assertNotNull(fact, "No fact parsed");

        var expected = PushSkipped.newBuilder()
                .setFactId("beru;sending=test_sending")
                .setTimestamp(12344656677546L)
                .setStatus("SKIPPED_AS_CONTROL")
                .setSendingType(SendingType.PROMO)
                .setCrmInfo(
                        CrmInfo.newBuilder()
                                .setAccount("beru")
                                .setSendingId("test_sending")
                                .setVariantId("test_sending_a")
                                .setSegmentId("test_segment")
                )
                .setAppId("23104")
                .setApplication("market")
                .setIsControl(true)
                .setMobilePlatform(MobilePlatform.ANDROID)
                .setUid(Uids.create(UidType.UUID, "1a7d3f5e38ad4a6af1"))
                .addUids(Uids.create(UidType.MM_DEVICE_ID, "a326df16-e1a7fa-79e3dc"))
                .addUids(Uids.create(UidType.MM_DEVICE_ID_HASH, "2cdc2995f30ac"))
                .build();

        assertEquals(expected, fact);
    }

    @Nullable
    private PushSkipped map(@Nonnull String line) {
        var facts = mapper.apply(line.getBytes(StandardCharsets.UTF_8));
        return CrmCollections.isEmpty(facts) ? null : facts.get(0);
    }
}
