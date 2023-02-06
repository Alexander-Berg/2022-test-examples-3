package ru.yandex.market.crm.platform.mappers;

import java.util.List;

import org.junit.Test;

import ru.yandex.market.crm.platform.commons.CrmInfo;
import ru.yandex.market.crm.platform.commons.SendingPayload;
import ru.yandex.market.crm.platform.commons.Uid;
import ru.yandex.market.crm.platform.models.Email;
import ru.yandex.market.crm.platform.models.EmailControl;
import ru.yandex.market.crm.platform.reducers.utils.EmailFactUtils;

import static org.junit.Assert.assertEquals;
import static ru.yandex.market.crm.platform.commons.SendingType.PROMO;
import static ru.yandex.market.crm.platform.commons.SendingType.TRIGGER;
import static ru.yandex.market.crm.platform.commons.UidType.EMAIL;

public class EmailMappersTest {

    private static final String controlLogLine =
            "tskv\t" +
                    "account=beru\t" +
                    "type=1\t" +
                    "control=true\t" +
                    "email=email@yandex.ru\t" +
                    "originalEmail=email@ya.ru\t" +
                    "sendingId=sendingAbc\t" +
                    "segmentId=segmentDef\t" +
                    "timestamp=99999999999";

    private static final String sentEmailLogLine =
            "tskv\t" +
                    "account=bringly\t" +
                    "type=2\t" +
                    "control=false\t" +
                    "email=email@yandex.ru\t" +
                    "originalEmail=email@ya.ru\t" +
                    "subject=Привет! Как дела?\t" +
                    "triggerId=triggerGhi\t" +
                    "blockId=blockJkl\t" +
                    "processId=processJkl\t" +
                    "templateId=templateMno\t" +
                    "segmentId=seg_SEGID\t" +
                    "messageId=<email-message-id:123456>\t" +
                    "senderAccount=lot-a-lot\t" +
                    "campaignId=71981\t" +
                    "timestamp=99999999999\t" +
                    "payloadJson=[" +
                        "{\"name\":\"ORDER_ID\", \"value\":\"555\"}, " +
                        "{\"name\":\"SKU_IDS\", \"value\":\"some_sku_id1,some_sku_id2\"}, " +
                        "{\"name\":\"UNKNOWN\", \"value\":\"asd\"}" +
                    "]";

    private final EmailSendingMapper sendingMapper = new EmailSendingMapper();
    private final EmailControlMapper controlMapper = new EmailControlMapper();

    @Test
    public void testControlMapperControlLine() {
        List<EmailControl> actuals = controlMapper.apply(controlLogLine.getBytes());
        assertEquals(1, actuals.size());

        EmailControl expected = EmailControl.newBuilder()
                .setUid(Uid.newBuilder()
                        .setType(EMAIL)
                        .setStringValue("email@yandex.ru")
                )
                .setOriginalUid(Uid.newBuilder()
                        .setType(EMAIL)
                        .setStringValue("email@ya.ru")
                )
                .setSendingType(PROMO)
                .setTimestamp(99999999999L)
                .setCrmInfo(CrmInfo.newBuilder()
                        .setAccount("beru")
                        .setSendingId("sendingAbc")
                        .setSegmentId("segmentDef")
                )
                .setFactId("beru;sending=sendingAbc")
                .build();

        assertEquals(expected, actuals.get(0));
    }

    @Test
    public void testControlMapperSentLine() {
        List<EmailControl> res = controlMapper.apply(sentEmailLogLine.getBytes());
        assertEquals(0, res.size());
    }

    @Test
    public void testSendingMapperControlLine() {
        List<Email> res = sendingMapper.apply(controlLogLine.getBytes());
        assertEquals(0, res.size());
    }

    @Test
    public void testSendingMapperSentLine() {

        List<Email> actuals = sendingMapper.apply(sentEmailLogLine.getBytes());
        assertEquals(1, actuals.size());

        Email expected = Email.newBuilder()
                .setUid(Uid.newBuilder()
                        .setType(EMAIL)
                        .setStringValue("email@yandex.ru")
                )
                .setOriginalUid(Uid.newBuilder()
                        .setType(EMAIL)
                        .setStringValue("email@ya.ru")
                )
                .setFactId(EmailFactUtils.factId(71981, "<email-message-id:123456>"))
                .setSendingType(TRIGGER)
                .setTimestamp(99999999999L)
                .setSubject("Привет! Как дела?")
                .setCrmInfo(CrmInfo.newBuilder()
                        .setAccount("bringly")
                        .setTriggerId("triggerGhi")
                        .setBlockId("blockJkl")
                        .setProcessId("processJkl")
                        .setSegmentId("seg_SEGID")
                        .setTemplateId("templateMno")
                )
                .setSenderInfo(Email.SenderInfo.newBuilder()
                        .setAccount("lot-a-lot")
                        .setCampaignId(71981)
                )
                .setMessageId("<email-message-id:123456>")
                .setEventType(Email.EventType.SENDING)
                .setDeliveryStatus(Email.DeliveryStatus.UPLOADED)
                .addPayload(SendingPayload.newBuilder().setKey(SendingPayload.Key.ORDER_ID).setValue("555"))
                .addPayload(SendingPayload.newBuilder().setKey(SendingPayload.Key.SKU_IDS)
                        .setValue("some_sku_id1,some_sku_id2"))
                .build();

        assertEquals(expected, actuals.get(0));
    }
}
