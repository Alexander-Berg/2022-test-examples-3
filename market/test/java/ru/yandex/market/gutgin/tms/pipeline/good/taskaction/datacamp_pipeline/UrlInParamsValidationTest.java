package ru.yandex.market.gutgin.tms.pipeline.good.taskaction.datacamp_pipeline;

import Market.DataCamp.DataCampContentMarketParameterValue;
import Market.DataCamp.DataCampOffer;
import Market.DataCamp.DataCampOfferContent;
import Market.DataCamp.DataCampOfferMarketContent;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.Test;
import org.mockito.Mockito;
import ru.yandex.market.ir.autogeneration.common.helpers.CategoryDataHelper;
import ru.yandex.market.partner.content.common.db.dao.goodcontent.GcSkuTicketDao;
import ru.yandex.market.partner.content.common.db.dao.goodcontent.GcSkuValidationDao;
import ru.yandex.market.partner.content.common.db.jooq.tables.pojos.GcSkuTicket;
import ru.yandex.market.partner.content.common.entity.goodcontent.FailData;
import ru.yandex.market.partner.content.common.entity.goodcontent.ParamInfo;
import ru.yandex.market.partner.content.common.message.MessageInfo;
import ru.yandex.market.partner.content.common.message.Messages;


public class UrlInParamsValidationTest {

    public static final String SHOP_SKU = "first";
    public static final long CONTAINS_URL_PARAM_ID = 49256781232L;
    private final UrlInParamsValidation urlInParamsValidation;

    public static final long SMART_HOUSE_CATEGORY_ID = 90627L;
    public static final long SMART_HOUSE_ECOSYSTEM_PARAMETER_ID = 16395492L;
    public static final long SMART_HOUSE_ECOSYSTEM_MAIL_RU_OPTION_ID = 17509196L;

    public UrlInParamsValidationTest() {
        urlInParamsValidation = new UrlInParamsValidation(
                Mockito.mock(GcSkuValidationDao.class),
                Mockito.mock(GcSkuTicketDao.class),
                Mockito.mock(CategoryDataHelper.class)
        );
    }

    @Test
    public void validateTicket() {
        GcSkuTicket gcSkuTicket = new GcSkuTicket();

        gcSkuTicket.setDatacampOffer(DataCampOffer.Offer.newBuilder()
            .setContent(DataCampOfferContent.OfferContent.newBuilder()
                .setPartner(DataCampOfferContent.PartnerContent.newBuilder()
                    .setMarketSpecificContent(DataCampOfferMarketContent.MarketSpecificContent.newBuilder()
                        .setParameterValues(DataCampOfferMarketContent.MarketParameterValues.newBuilder()
                            .addParameterValues(DataCampContentMarketParameterValue.MarketParameterValue.newBuilder()
                                .setParamId(4925678)
                                .setValueSource(DataCampContentMarketParameterValue.MarketValueSource.CONTENT_EXCEL)
                                .setValue(DataCampContentMarketParameterValue.MarketValue.newBuilder()
                                    .setValueType(DataCampContentMarketParameterValue.MarketValueType.BOOLEAN)
                                    .setBoolValue(true)
                                )
                            )
                            .addParameterValues(DataCampContentMarketParameterValue.MarketParameterValue.newBuilder()
                                .setParamId(49256781231L)
                                .setValueSource(DataCampContentMarketParameterValue.MarketValueSource.CONTENT_EXCEL)
                                .setValue(DataCampContentMarketParameterValue.MarketValue.newBuilder()
                                    .setValueType(DataCampContentMarketParameterValue.MarketValueType.STRING)
                                    .setStrValue("hello world")
                                )
                            )
                            .addParameterValues(DataCampContentMarketParameterValue.MarketParameterValue.newBuilder()
                                .setParamId(CONTAINS_URL_PARAM_ID)
                                .setValueSource(DataCampContentMarketParameterValue.MarketValueSource.CONTENT_EXCEL)
                                .setValue(DataCampContentMarketParameterValue.MarketValue.newBuilder()
                                    .setValueType(DataCampContentMarketParameterValue.MarketValueType.STRING)
                                    .setStrValue("http://ya.ru")
                                )
                            )
                            .addParameterValues(
                                    DataCampContentMarketParameterValue.MarketParameterValue
                                        .newBuilder()
                                        .setParamId(SMART_HOUSE_ECOSYSTEM_PARAMETER_ID)
                                        .setValueSource(DataCampContentMarketParameterValue.MarketValueSource.CONTENT_EXCEL)
                                        .setValue(
                                                DataCampContentMarketParameterValue.MarketValue
                                                    .newBuilder()
                                                    .setOptionId(SMART_HOUSE_ECOSYSTEM_MAIL_RU_OPTION_ID)
                                                    .setValueType(DataCampContentMarketParameterValue.MarketValueType.ENUM)
                                                    .setStrValue("Умный дом Mail.ru")
                                        )
                            )
                            .addParameterValues(
                                    DataCampContentMarketParameterValue.MarketParameterValue
                                            .newBuilder()
                                            .setParamId(SMART_HOUSE_ECOSYSTEM_PARAMETER_ID)
                                            .setValueSource(DataCampContentMarketParameterValue.MarketValueSource.CONTENT_EXCEL)
                                            .setValue(
                                                    DataCampContentMarketParameterValue.MarketValue
                                                            .newBuilder()
                                                            .setValueType(DataCampContentMarketParameterValue.MarketValueType.ENUM)
                                                            .setStrValue("Умный дом Mail.ru")
                                            )
                            )
                            .build()
                        )
                    )
                )
            )
            .build());

        gcSkuTicket.setCategoryId(SMART_HOUSE_CATEGORY_ID);

        MessageReporter messageReporter = new MessageReporter(SHOP_SKU);
        urlInParamsValidation.validateTicket(gcSkuTicket, messageReporter, Set.of());

        MessageInfo expectedMessageForParameterWithURL = Messages.get().gcParamContainsUrl(
                SHOP_SKU, "", CONTAINS_URL_PARAM_ID
        );
        MessageInfo expectedMessageForSmartHouseParameterWithoutOptionId = Messages.get().gcParamContainsUrl(
                SHOP_SKU, "", SMART_HOUSE_ECOSYSTEM_PARAMETER_ID
        );

        List<MessageInfo> messageInfoList = messageReporter.getMessages();

        System.out.println(messageInfoList.get(0).toString());
        System.out.println(expectedMessageForParameterWithURL.toString());
        System.out.println(messageInfoList.get(0).toString().equals(expectedMessageForParameterWithURL.toString()));

        assertThat(messageInfoList.stream().map(MessageInfo::toString))
                .hasSize(2)
                .containsOnly(
                        expectedMessageForParameterWithURL.toString(),
                        expectedMessageForSmartHouseParameterWithoutOptionId.toString()
                );

        FailData failData = messageReporter.getFailData();
        assertThat(failData).isNotNull();
        assertThat(failData.getParams()).containsExactlyInAnyOrder(
                new ParamInfo(CONTAINS_URL_PARAM_ID, null, false),
                new ParamInfo(SMART_HOUSE_ECOSYSTEM_PARAMETER_ID, null, false)
        );
    }
}
