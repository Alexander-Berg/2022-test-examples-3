package ru.yandex.market.crm.platform.mappers;

import java.util.List;

import org.junit.Test;

import ru.yandex.market.crm.platform.common.Uids;
import ru.yandex.market.crm.platform.commons.CrmInfo;
import ru.yandex.market.crm.platform.commons.UidType;
import ru.yandex.market.crm.platform.models.Gnc;
import ru.yandex.market.crm.platform.models.GncControl;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static ru.yandex.market.crm.platform.commons.SendingType.PROMO;
import static ru.yandex.market.crm.platform.commons.SendingType.TRIGGER;

public class GncMappersTest {

    private static final String SENDING_LOG_LINE = "tskv\t" +
            "account=market\t" +
            "type=2\t" +
            "control=false\t" +
            "gncType=new-answer\t" +
            "service=market\t" +
            "puid=3043813210\t" +
            "timestamp=1543492050\t" +
            "actor=1122334455\t" +
            "templateVars=model:iphone\t" +
            "triggerId=triggerAbc\t" +
            "segmentId=segmentDef\t" +
            "blockId=blockGhi\t" +
            "processId=processGhi\t" +
            "templateId=templateJkl";

    private static final String CONTROL_LOG_LINE = "tskv\t" +
            "account=beru\t" +
            "type=1\t" +
            "control=true\t" +
            "gncType=some-type\t" +
            "service=beru\t" +
            "puid=3043813210\t" +
            "timestamp=1543492050\t" +
            "sendingId=sendingMno\t" +
            "variantId=variantPqr\t" +
            "segmentId=segmentStu";

    private static final String SENDING_LOG_LINE_JSON_TEMPLATE_VARS = "tskv\t" +
            "account=market\t" +
            "type=2\t" +
            "control=false\t" +
            "gncType=new-answer\t" +
            "service=market\t" +
            "puid=3043813210\t" +
            "timestamp=1543492050\t" +
            "actor=1122334455\t" +
            "templateVarsJson=[{\"name\":\"model\",\"value\":\"iphone,iphone\"},{\"name\":\"var2\"," +
            "\"value\":\"value29=1!2-/3\"}]\t" +
            "triggerId=triggerAbc\t" +
            "segmentId=segmentDef\t" +
            "blockId=blockGhi\t" +
            "processId=processGhi\t" +
            "templateId=templateJkl";

    private final GncSendingMapper sendingMapper = new GncSendingMapper();
    private final GncControlMapper controlMapper = new GncControlMapper();

    @Test
    public void testControlMapperControlLine() {

        List<GncControl> res = controlMapper.apply(CONTROL_LOG_LINE.getBytes());

        assertEquals(1, res.size());

        GncControl expected = GncControl.newBuilder()
                .setUid(Uids.create(UidType.PUID, 3043813210L))
                .setTimestamp(1543492050)
                .setSendingType(PROMO)
                .setGncType("some-type")
                .setService("beru")
                .setCrmInfo(CrmInfo.newBuilder()
                        .setAccount("beru")
                        .setSendingId("sendingMno")
                        .setVariantId("variantPqr")
                        .setSegmentId("segmentStu")
                        .build()
                )
                .setFactId("beru;sending=sendingMno")
                .build();

        GncControl actual = res.get(0);
        assertEquals(expected, actual);
    }

    @Test
    public void testControlMapperSentLine() {

        List<GncControl> res = controlMapper.apply(SENDING_LOG_LINE.getBytes());
        assertEquals(0, res.size());
    }

    @Test
    public void testSendingMapperControlLine() {

        List<Gnc> res = sendingMapper.apply(CONTROL_LOG_LINE.getBytes());
        assertEquals(0, res.size());
    }

    @Test
    public void testJsonTemplateVarsLine() {

        List<Gnc> res = sendingMapper.apply(SENDING_LOG_LINE_JSON_TEMPLATE_VARS.getBytes());
        assertEquals(1, res.size());

        Gnc expected = Gnc.newBuilder()
                .setUid(Uids.create(UidType.PUID, 3043813210L))
                .setTimestamp(1543492050)
                .setSendingType(TRIGGER)
                .setGncType("new-answer")
                .setService("market")
                .setCrmInfo(CrmInfo.newBuilder()
                        .setAccount("market")
                        .setTriggerId("triggerAbc")
                        .setBlockId("blockGhi")
                        .setProcessId("processGhi")
                        .setSegmentId("segmentDef")
                        .setTemplateId("templateJkl")
                )
                .setFactId("market;trigger=triggerAbc;block=blockGhi")
                .setActor("1122334455")
                .addAllTemplateVars(asList(
                        Gnc.TemplateVar.newBuilder().setName("model").setValue("iphone,iphone").build(),
                        Gnc.TemplateVar.newBuilder().setName("var2").setValue("value29=1!2-/3").build()
                )).build();

        Gnc actual = res.get(0);
        assertEquals(expected, actual);
    }

    @Test
    public void testSendingMapperSentLine() {

        List<Gnc> res = sendingMapper.apply(SENDING_LOG_LINE.getBytes());

        assertEquals(1, res.size());

        Gnc expected = Gnc.newBuilder()
                .setUid(Uids.create(UidType.PUID, 3043813210L))
                .setTimestamp(1543492050)
                .setSendingType(TRIGGER)
                .setGncType("new-answer")
                .setService("market")
                .setCrmInfo(CrmInfo.newBuilder()
                        .setAccount("market")
                        .setTriggerId("triggerAbc")
                        .setBlockId("blockGhi")
                        .setProcessId("processGhi")
                        .setSegmentId("segmentDef")
                        .setTemplateId("templateJkl")
                )
                .setFactId("market;trigger=triggerAbc;block=blockGhi")
                .setActor("1122334455")
                .addAllTemplateVars(singletonList(
                        Gnc.TemplateVar.newBuilder()
                                .setName("model")
                                .setValue("iphone")
                                .build()
                )).build();

        Gnc actual = res.get(0);
        assertEquals(expected, actual);
    }
}
