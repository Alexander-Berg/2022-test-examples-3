package ru.yandex.market.gutgin.tms.pipeline.good.taskaction.datacamp_pipeline;

import Market.DataCamp.DataCampOfferMeta;
import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.partner.content.common.DBDcpStateGenerator;
import ru.yandex.market.partner.content.common.csku.KnownParameters;
import ru.yandex.market.partner.content.common.db.jooq.tables.pojos.DatacampOffer;
import ru.yandex.market.partner.content.common.db.jooq.tables.pojos.GcSkuTicket;
import ru.yandex.market.partner.content.common.entity.goodcontent.FailData;
import ru.yandex.market.partner.content.common.entity.goodcontent.ParamInfo;
import ru.yandex.market.partner.content.common.message.MessageInfo;
import ru.yandex.market.partner.content.common.message.Messages;
import ru.yandex.market.robot.db.ParameterValueComposer;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static ru.yandex.market.gutgin.tms.pipeline.good.taskaction.datacamp_pipeline.DcpOfferUtils.initOffer;
import static ru.yandex.market.gutgin.tms.pipeline.good.taskaction.datacamp_pipeline.TitleUpperCaseValidation.UPPER_CASE_THRESHOLD_PERCENTS;

public class TitleUpperCaseValidationTest extends DBDcpStateGenerator {

    private static final String TEST_SKU_ID = "test_sku";

    private TitleUpperCaseValidation titleUpperCaseValidation;
    private MessageReporter messageReporter;

    @Before
    public void setUp() {
        super.setUp();
        this.messageReporter = new MessageReporter(TEST_SKU_ID);
        this.titleUpperCaseValidation = new TitleUpperCaseValidation(gcSkuValidationDao, gcSkuTicketDao);
    }

    @Test
    public void validTitle() {
        List<GcSkuTicket> gcSkuTickets = generateDBDcpInitialState(3, offers -> {
            offerWithTitle(offers, "Valid name", 0);
            offerWithTitle(offers, "Норм имя", 1);
            offerWithTitle(offers, "Лак DALI-DECOR Лессирующий перламутровый полиакриловый бесцветный 1 кг", 2);
        });

        gcSkuTickets.forEach(gcSkuTicket -> titleUpperCaseValidation.validateTicket(gcSkuTicket, messageReporter, Set.of()));
        List<MessageInfo> messages = messageReporter.getMessages();
        assertThat(messages).isEmpty();
        FailData failData = messageReporter.getFailData();
        assertThat(failData).isNull();
    }

    @Test
    public void tooMuchUpperCaseInTitle() {
        List<GcSkuTicket> gcSkuTickets = generateDBDcpInitialState(3, offers -> {
            offerWithTitle(offers, "ЛАК ЛЕССИРУЮЩИЙ \"DALI-DECOR\" ПЕРЛАМУТР. БЕСЦВЕТНЫЙ 1 КГ (6) \"РОГНЕДА\"", 0);
            offerWithTitle(offers, "ALL UPPER CASE", 1);
            offerWithTitle(offers, "ABCDEFHIJk", 2);
        });
        gcSkuTickets.forEach(gcSkuTicket -> titleUpperCaseValidation.validateTicket(gcSkuTicket, messageReporter, Set.of()));
        List<MessageInfo> messages = messageReporter.getMessages();
        assertThat(messages).extracting(MessageInfo::getCode)
            .containsOnly("ir.partner_content.error.upper_case_in_title")
            .hasSize(3);

        MessageInfo expectedMessage100 = Messages.get().upperCaseInTitle(TEST_SKU_ID, 100.0, UPPER_CASE_THRESHOLD_PERCENTS, ParameterValueComposer.NAME_ID);
        MessageInfo expectedMessage90 = Messages.get().upperCaseInTitle(TEST_SKU_ID, 90.0, UPPER_CASE_THRESHOLD_PERCENTS, ParameterValueComposer.NAME_ID);

        assertEquals(expectedMessage100, messages.get(0));
        assertEquals(expectedMessage100, messages.get(1));
        assertEquals(expectedMessage90, messages.get(2));
        FailData failData = messageReporter.getFailData();
        assertThat(failData).isNotNull();
        assertThat(failData.getParams()).contains(new ParamInfo(ParameterValueComposer.NAME_ID, ParameterValueComposer.NAME, false));
    }

    private void offerWithTitle(List<DatacampOffer> offers, String title, int index) {
        initOffer(CATEGORY_ID, offers.get(index), builder -> builder.getContentBuilder().getPartnerBuilder().getActualBuilder()
            .setTitle(DataCampOfferMeta.StringValue.newBuilder().setValue(title).build()).build());
    }
}
