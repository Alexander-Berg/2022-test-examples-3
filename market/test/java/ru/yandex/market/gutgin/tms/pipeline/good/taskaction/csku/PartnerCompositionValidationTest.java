package ru.yandex.market.gutgin.tms.pipeline.good.taskaction.csku;

import java.util.List;
import java.util.Set;

import it.unimi.dsi.fastutil.longs.LongArraySet;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.gutgin.tms.pipeline.good.taskaction.csku.PartnerCompositionValidation;
import ru.yandex.market.gutgin.tms.pipeline.good.taskaction.datacamp_pipeline.MessageReporter;
import ru.yandex.market.gutgin.tms.service.partnercomposition.PartnerCompositionParser;
import ru.yandex.market.ir.autogeneration.common.db.CategoryData;
import ru.yandex.market.ir.autogeneration.common.db.CategoryDataKnowledge;
import ru.yandex.market.mbo.export.MboParameters;
import ru.yandex.market.partner.content.common.DBDcpStateGenerator;
import ru.yandex.market.partner.content.common.csku.KnownParameters;
import ru.yandex.market.partner.content.common.csku.KnownTags;
import ru.yandex.market.partner.content.common.db.jooq.tables.pojos.GcSkuTicket;
import ru.yandex.market.partner.content.common.entity.goodcontent.FailData;
import ru.yandex.market.partner.content.common.entity.goodcontent.ParamInfo;
import ru.yandex.market.partner.content.common.message.MessageInfo;
import ru.yandex.market.partner.content.common.message.Messages;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static ru.yandex.market.gutgin.tms.pipeline.good.taskaction.datacamp_pipeline.DcpOfferUtils.addStrParam;
import static ru.yandex.market.gutgin.tms.pipeline.good.taskaction.datacamp_pipeline.DcpOfferUtils.initOffer;
import static ru.yandex.market.partner.content.common.csku.KnownParameters.PARTNER_COMPOSITION;

public class PartnerCompositionValidationTest extends DBDcpStateGenerator {

    public static final int PARAM_ID = PARTNER_COMPOSITION.getId().intValue();
    private static final String PARAM_NAME = PARTNER_COMPOSITION.getXslName();
    private static final String TEST_SKU_ID = "test_sku";
    public static final long MATERIAL_1 = 2L;
    public static final long MATERIAL_2 = 3L;

    private PartnerCompositionValidation validation;
    private MessageReporter messageReporter = new MessageReporter(TEST_SKU_ID);
    private PartnerCompositionParser parser;

    @Before
    public void setUp() {
        super.setUp();

        var categoryData = Mockito.mock(CategoryData.class);
        when(categoryData.containsParam(KnownParameters.PARTNER_COMPOSITION.getId()))
                .thenReturn(true);
        when(categoryData.getParamIdsByTag(KnownTags.MATERIAL.getName()))
                .thenReturn(new LongArraySet(Set.of(MATERIAL_1, MATERIAL_2)));
        when(categoryData.getParamById(MATERIAL_1))
                .thenReturn(
                        MboParameters.Parameter.newBuilder()
                                .setId(MATERIAL_1)
                                .addAlias(MboParameters.Word.newBuilder().setName("Хлопок").build())
                                .addAlias(MboParameters.Word.newBuilder().setName("хб").build())
                                .build()
                );
        when(categoryData.getParamById(MATERIAL_2))
                .thenReturn(
                        MboParameters.Parameter.newBuilder()
                                .setId(MATERIAL_2)
                                .addAlias(MboParameters.Word.newBuilder().setName("Синтетика").build())
                                .build()
                );

        CategoryDataKnowledge categoryDataKnowledge = Mockito.mock(CategoryDataKnowledge.class);
        when(categoryDataKnowledge.getCategoryData(CATEGORY_ID))
                .thenReturn(categoryData);

        this.parser = new PartnerCompositionParser(categoryDataKnowledge);
        this.validation = new PartnerCompositionValidation(gcSkuValidationDao, gcSkuTicketDao,
                categoryDataKnowledge, parser, true);
        this.messageReporter = new MessageReporter(TEST_SKU_ID);
    }

    @Test
    public void ticketsWithInvalidFormat() {
        List<GcSkuTicket> gcSkuTickets = generateDBDcpInitialState(2, offers -> {
            initOffer(CATEGORY_ID, offers.get(0), builder ->
                    addStrParam(PARAM_ID, PARAM_NAME, "50;хлопок:100;синтетика", builder));
            initOffer(CATEGORY_ID, offers.get(1), builder ->
                    addStrParam(PARAM_ID, PARAM_NAME, "хлопок 10 10", builder));
        });

        gcSkuTickets.forEach(gcSkuTicket -> validation.validateTicket(gcSkuTicket, messageReporter,
                Set.of()));

        List<MessageInfo> messages = messageReporter.getMessages();
        assertThat(messages).hasSize(2);
        assertEquals(
                Messages.get().partnerCompositionInvalidFormat(PARAM_ID, PARAM_NAME, Set.of("50", "синтетика")),
                messages.get(0)
        );
        assertEquals(
                Messages.get().partnerCompositionInvalidFormat(PARAM_ID, PARAM_NAME, Set.of("хлопок 10 10")),
                messages.get(1)
        );
        FailData failData = messageReporter.getFailData();
        assertThat(failData).isNotNull();
        assertThat(failData.getParams()).contains(new ParamInfo((long) PARAM_ID, PARAM_NAME,
                false));
    }

    @Test
    public void ticketsWithDuplicateParameter() {
        List<GcSkuTicket> gcSkuTickets = generateDBDcpInitialState(2, offers -> {
            initOffer(CATEGORY_ID, offers.get(0), builder ->
                    addStrParam(PARAM_ID, PARAM_NAME, "хлопок:50;синтетика:50;хб-50", builder));
        });

        gcSkuTickets.forEach(gcSkuTicket -> validation.validateTicket(gcSkuTicket, messageReporter,
                Set.of()));

        List<MessageInfo> messages = messageReporter.getMessages();
        assertThat(messages).hasSize(1);
        assertEquals(
                Messages.get().partnerCompositionDuplicateParameter(PARAM_ID, PARAM_NAME, Set.of("хлопок:50", "хб-50")),
                messages.get(0)
        );
        FailData failData = messageReporter.getFailData();
        assertThat(failData).isNotNull();
        assertThat(failData.getParams()).contains(new ParamInfo((long) PARAM_ID, PARAM_NAME,
                false));
    }

    @Test
    public void ticketsWithUnknownParameter() {
        List<GcSkuTicket> gcSkuTickets = generateDBDcpInitialState(2, offers -> {
            initOffer(CATEGORY_ID, offers.get(0), builder ->
                    addStrParam(PARAM_ID, PARAM_NAME, "шерсть:100;", builder));
        });

        gcSkuTickets.forEach(gcSkuTicket -> validation.validateTicket(gcSkuTicket, messageReporter,
                Set.of()));

        List<MessageInfo> messages = messageReporter.getMessages();
        assertThat(messages).hasSize(1);
        assertEquals(
                Messages.get().partnerCompositionUnknownParameter(PARAM_ID, PARAM_NAME, Set.of("шерсть:100")),
                messages.get(0)
        );
        FailData failData = messageReporter.getFailData();
        assertThat(failData).isNotNull();
        assertThat(failData.getParams()).contains(new ParamInfo((long) PARAM_ID, PARAM_NAME,
                false));
    }

    @Test
    public void ticketsWithInvalidSum() {
        List<GcSkuTicket> gcSkuTickets = generateDBDcpInitialState(2, offers -> {
            initOffer(CATEGORY_ID, offers.get(0), builder ->
                    addStrParam(PARAM_ID, PARAM_NAME, "хлопок:50;синтетика:40", builder));
        });

        gcSkuTickets.forEach(gcSkuTicket -> validation.validateTicket(gcSkuTicket, messageReporter,
                Set.of()));

        List<MessageInfo> messages = messageReporter.getMessages();
        assertThat(messages).hasSize(1);
        assertEquals(
                Messages.get().partnerCompositionInvalidSum(PARAM_ID, PARAM_NAME),
                messages.get(0)
        );
        FailData failData = messageReporter.getFailData();
        assertThat(failData).isNotNull();
        assertThat(failData.getParams()).contains(new ParamInfo((long) PARAM_ID, PARAM_NAME,
                false));
    }

    @Test
    public void ticketsWithMultipleErrors() {
        List<GcSkuTicket> gcSkuTickets = generateDBDcpInitialState(2, offers -> {
            initOffer(CATEGORY_ID, offers.get(0), builder ->
                    addStrParam(PARAM_ID, PARAM_NAME, "хлопок:50;шерсть:50;хб-50", builder));
        });

        gcSkuTickets.forEach(gcSkuTicket -> validation.validateTicket(gcSkuTicket, messageReporter,
                Set.of()));

        List<MessageInfo> messages = messageReporter.getMessages();
        assertThat(messages).hasSize(3);
        assertTrue(messages.containsAll(List.of(
                Messages.get().partnerCompositionDuplicateParameter(PARAM_ID, PARAM_NAME, Set.of("хлопок:50", "хб-50")),
                Messages.get().partnerCompositionUnknownParameter(PARAM_ID, PARAM_NAME, Set.of("шерсть:50")),
                Messages.get().partnerCompositionInvalidSum(PARAM_ID, PARAM_NAME)
        )));
        FailData failData = messageReporter.getFailData();
        assertThat(failData).isNotNull();
        assertThat(failData.getParams()).contains(new ParamInfo((long) PARAM_ID, PARAM_NAME,
                false));
    }

    @Test
    public void validTickets() {
        List<GcSkuTicket> gcSkuTickets = generateDBDcpInitialState(2, offers -> {
            initOffer(CATEGORY_ID, offers.get(0), builder ->
                    addStrParam(PARAM_ID, PARAM_NAME, "хлопок:100", builder));
            initOffer(CATEGORY_ID, offers.get(1), builder ->
                    addStrParam(PARAM_ID, PARAM_NAME, "хлопок:50,синтетика:50", builder));
        });
        gcSkuTickets.forEach(gcSkuTicket -> validation.validateTicket(gcSkuTicket, messageReporter,
                Set.of()));
        List<MessageInfo> messages = messageReporter.getMessages();
        assertThat(messages).isEmpty();
        FailData failData = messageReporter.getFailData();
        assertThat(failData).isNull();
    }
}
