package ru.yandex.market.crm.platform.mappers;

import java.nio.charset.StandardCharsets;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.junit.jupiter.api.Test;

import ru.yandex.market.crm.platform.common.Uids;
import ru.yandex.market.crm.platform.commons.CrmInfo;
import ru.yandex.market.crm.platform.commons.MobilePlatform;
import ru.yandex.market.crm.platform.models.Push;
import ru.yandex.market.crm.platform.models.Push.MetrikaInfo;
import ru.yandex.market.crm.util.CrmCollections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static ru.yandex.market.crm.platform.commons.SendingType.PROMO;
import static ru.yandex.market.crm.platform.commons.UidType.MM_DEVICE_ID;
import static ru.yandex.market.crm.platform.commons.UidType.MM_DEVICE_ID_HASH;
import static ru.yandex.market.crm.platform.commons.UidType.UUID;

/**
 * @author apershukov
 */
class PushSendingMapperTest {

    private static final Push SENT_PUSH = Push.newBuilder()
            .setUid(Uids.create(UUID, "3450ad9c67df2a10e4"))
            .addUids(Uids.create(MM_DEVICE_ID, "45adfc21-23facd-98afcd"))
            .addUids(Uids.create(MM_DEVICE_ID_HASH, "3da4e23fa2058"))
            .setSendingType(PROMO)
            .setMetrikaInfo(
                    MetrikaInfo.newBuilder()
                            .setAppId("23107")
                            .setTransferId("6736710")
            )
            .setTimestamp(13454637534413675L)
            .setTitle("Здравствуйте!!!")
            .setText("Текст пуш сообщения.\nТело.")
            .setCrmInfo(CrmInfo.newBuilder()
                    .setAccount("bringly")
                    .setSendingId("sendingAbc")
                    .setVariantId("variantCde")
                    .setSegmentId("segmentFgh")
            )
            .setMobilePlatform(MobilePlatform.IOS)
            .setFactId("transferId=6736710")
            .build();

    private final PushSendingMapper mapper = new PushSendingMapper();

    @Test
    void testSendingMapperControlLine() {
        var line = "tskv\t" +
                "control=true\t" +
                "appId=23104\t" +
                "platform=ANDROID\t" +
                "type=2\t" +
                "account=beru\t" +
                "triggerId=triggerIjk\t" +
                "blockId=blockLmn\t" +
                "processId=processLmn\t" +
                "templateId=templateRst\t" +
                "uuid=1a7d3f5e38ad4a6af1\t" +
                "deviceId=a326df16-e1a7fa-79e3dc\t" +
                "deviceIdHash=2cdc2995f30ac\t" +
                "timestamp=12344656677546";

        assertNull(map(line));
    }

    /**
     * Сообщение со статусом UPLOADED парсится в факт
     */
    @Test
    void testSentLineWithStatus() {
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
                "title=Здравствуйте!!!\t" +
                "text=Текст пуш сообщения.\\nТело.\t" +
                "transferId=6736710\t" +
                "timestamp=13454637534413675";

        var fact = map(line);
        assertEquals(SENT_PUSH, fact);
    }
    /**
     * Сообщение без статуса, не попавшее в контроль, парсится в факт.
     * Нужно для обратной совместимости. См. LILUCRM-5506
     */
    @Test
    void testSentLineWithoutStatus() {
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
                "title=Здравствуйте!!!\t" +
                "text=Текст пуш сообщения.\\nТело.\t" +
                "transferId=6736710\t" +
                "timestamp=13454637534413675";

        var fact = map(line);
        assertEquals(SENT_PUSH, fact);
    }

    /**
     * Сообщения не из контроля и с заполненным статусом, отличным от UPLOADED пропускается
     */
    @Test
    void testIgnoreSkippedPush() {
        var line = "tskv\t" +
                "status=SKIPPED_BY_FREQUENCY_FILTER\t" +
                "control=false\t" +
                "appId=23104\t" +
                "platform=ANDROID\t" +
                "type=2\t" +
                "account=beru\t" +
                "triggerId=triggerIjk\t" +
                "blockId=blockLmn\t" +
                "processId=processLmn\t" +
                "templateId=templateRst\t" +
                "uuid=1a7d3f5e38ad4a6af1\t" +
                "deviceId=a326df16-e1a7fa-79e3dc\t" +
                "deviceIdHash=2cdc2995f30ac\t" +
                "timestamp=12344656677546";

        assertNull(map(line));
    }

    @Nullable
    private Push map(@Nonnull String line) {
        var facts = mapper.apply(line.getBytes(StandardCharsets.UTF_8));
        return CrmCollections.isEmpty(facts) ? null : facts.get(0);
    }
}
