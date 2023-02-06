package ru.yandex.market.gutgin.tms.pipeline.good.taskaction.datacamp_pipeline;

import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.gutgin.tms.utils.CategoryDataHelperMock;
import ru.yandex.market.ir.autogeneration.common.helpers.CategoryDataHelper;
import ru.yandex.market.partner.content.common.DBDcpStateGenerator;
import ru.yandex.market.partner.content.common.db.jooq.tables.pojos.GcSkuTicket;
import ru.yandex.market.partner.content.common.entity.goodcontent.FailData;
import ru.yandex.market.partner.content.common.entity.goodcontent.ParamInfo;
import ru.yandex.market.partner.content.common.message.MessageInfo;
import ru.yandex.market.partner.content.common.message.Messages;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.gutgin.tms.pipeline.good.taskaction.datacamp_pipeline.DcpOfferUtils.addStrParam;
import static ru.yandex.market.gutgin.tms.pipeline.good.taskaction.datacamp_pipeline.DcpOfferUtils.initOffer;

public class HtmlTagsExistenceValidationTest extends DBDcpStateGenerator {

    private static final int PARAM_ID = 1;
    private static final String PARAM_NAME = "xxx";
    private static final String TEST_SKU_ID = "test_sku";

    private HtmlTagsExistenceValidation htmlTagsExistenceValidation;
    private MessageReporter messageReporter = new MessageReporter(TEST_SKU_ID);

    @Before
    public void setUp() {
        super.setUp();
        CategoryDataHelper categoryDataHelperMock = new CategoryDataHelperMock();
        this.htmlTagsExistenceValidation = new HtmlTagsExistenceValidation(gcSkuValidationDao, gcSkuTicketDao,
                categoryDataHelperMock);
        this.messageReporter = new MessageReporter(TEST_SKU_ID);
    }

    @Test
    public void ticketsWithTags() {
        List<GcSkuTicket> gcSkuTickets = generateDBDcpInitialState(2, offers -> {
            initOffer(CATEGORY_ID, offers.get(0), builder -> addStrParam(PARAM_ID, PARAM_NAME,
                "\u003c![CDATA[Хмели-сунели - это ароматная смесь]]\u003e", builder));
            initOffer(CATEGORY_ID, offers.get(1),
                builder -> addStrParam(PARAM_ID, PARAM_NAME, "Растворитель ржавчины  <html> с керамикой", builder));
        });

        gcSkuTickets.forEach(gcSkuTicket -> htmlTagsExistenceValidation.validateTicket(gcSkuTicket, messageReporter,
                Set.of()));

        MessageInfo expectedMessage = Messages.get().invalidHtmlTagInSkuDcp(TEST_SKU_ID, PARAM_NAME, (long) PARAM_ID);
        List<MessageInfo> messages = messageReporter.getMessages();
        assertThat(messages).hasSize(2);
        assertThat(messages.get(0)).isEqualTo(expectedMessage);
        assertThat(messages.get(1)).isEqualTo(expectedMessage);
        FailData failData = messageReporter.getFailData();
        assertThat(failData).isNotNull();
        assertThat(failData.getParams()).contains(new ParamInfo((long)PARAM_ID, PARAM_NAME,
                false));
    }

    @Test
    public void validTickets() {
        List<GcSkuTicket> gcSkuTickets = generateDBDcpInitialState(2, offers -> {
            initOffer(CATEGORY_ID, offers.get(0), builder ->
                addStrParam(PARAM_ID, PARAM_NAME, "Хмели-сунели - это ароматная смесь", builder));
            initOffer(CATEGORY_ID, offers.get(1), builder ->
                addStrParam(PARAM_ID, PARAM_NAME, "Растворитель ржавчины с керамикой", builder));
        });
        gcSkuTickets.forEach(gcSkuTicket -> htmlTagsExistenceValidation.validateTicket(gcSkuTicket, messageReporter,
                Set.of()));
        List<MessageInfo> messages = messageReporter.getMessages();
        assertThat(messages).isEmpty();
        FailData failData = messageReporter.getFailData();
        assertThat(failData).isNull();
    }
}
