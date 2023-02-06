package ru.yandex.market.crm.platform.mappers;

import java.util.List;

import org.junit.Test;

import ru.yandex.market.crm.platform.common.Uids;
import ru.yandex.market.crm.platform.commons.CrmInfo;
import ru.yandex.market.crm.platform.commons.SendingPayload;
import ru.yandex.market.crm.platform.commons.SendingType;
import ru.yandex.market.crm.platform.commons.UidType;
import ru.yandex.market.crm.platform.models.Sms;

import static org.junit.Assert.*;

public class SmsSendingMapperTest {

    private static final String SENDING_LOG_LINE = "tskv\t" +
            "account=beru\t" +
            "type=2\t" +
            "phone=70001234569\t" +
            "timestamp=1543492050\t" +
            "triggerId=RDD\t" +
            "blockId=block345\t" +
            "processId=process345\t" +
            "text=Текст из смс\t" +
            "smsId=987789\t" +
            "payloadJson=[{\"name\":\"ORDER_ID\", \"value\":\"555\"}, {\"name\":\"UNKNOWN\", \"value\":\"asd\"}]\t" +
            "templateId=templateQWE";


    private SmsSendingMapper mapper = new SmsSendingMapper();

    @Test
    public void testMap() {
        List<Sms> result = mapper.apply(SENDING_LOG_LINE.getBytes());
        assertEquals(1, result.size());

        Sms expected = Sms.newBuilder()
                .setUid(Uids.create(UidType.PHONE, "70001234569"))
                .setFactId("smsId=987789")
                .setTimestamp(1543492050L)
                .setText("Текст из смс")
                .setSendingType(SendingType.TRIGGER)
                .setCrmInfo(CrmInfo.newBuilder()
                        .setAccount("beru")
                        .setTriggerId("RDD")
                        .setTemplateId("templateQWE")
                        .setBlockId("block345")
                        .setProcessId("process345"))
                .addPayload(SendingPayload.newBuilder().setKey(SendingPayload.Key.ORDER_ID).setValue("555"))
                .build();

        assertEquals(expected, result.get(0));
    }

    @Test
    public void testUsePuidAsIdIfPhoneIsNotSpecified() {
        String line = "tskv\t" +
                "account=beru\t" +
                "type=2\t" +
                "puid=123\t" +
                "timestamp=1543492050\t" +
                "triggerId=RDD\t" +
                "blockId=block345\t" +
                "processId=process345\t" +
                "text=Текст из смс\t" +
                "smsId=987789\t" +
                "payloadJson=[{\"name\":\"ORDER_ID\", \"value\":\"555\"}, {\"name\":\"UNKNOWN\", \"value\":\"asd\"}]\t" +
                "templateId=templateQWE";

        List<Sms> result = mapper.apply(line.getBytes());
        assertEquals(1, result.size());

        assertEquals(Uids.create(UidType.PUID, 123), result.get(0).getUid());
    }
}
