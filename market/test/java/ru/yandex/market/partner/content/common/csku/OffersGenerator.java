package ru.yandex.market.partner.content.common.csku;

import java.util.ArrayList;
import java.util.List;

import Market.DataCamp.DataCampContentMarketParameterValue;
import Market.DataCamp.DataCampOffer;
import Market.DataCamp.DataCampOfferContent;
import Market.DataCamp.DataCampOfferMarketContent;

import ru.yandex.market.partner.content.common.csku.judge.MarketParameterValueWrapper;

import static ru.yandex.market.partner.content.common.csku.util.OfferSpecialParameterCreator.createBooleanParameter;
import static ru.yandex.market.partner.content.common.csku.util.OfferSpecialParameterCreator.createEnumParameter;
import static ru.yandex.market.partner.content.common.csku.util.OfferSpecialParameterCreator.createStringParameter;

public class OffersGenerator {

    public static final Long PARAM_ID_1 = 1L;
    public static final String VALUE_1 = "124";

    public static DataCampOffer.Offer generate(List<SimplifiedOfferParameter> simplifiedOfferParameters) {
        return generateOfferBuilder(simplifiedOfferParameters).build();
    }

    public static DataCampOffer.Offer.Builder generateOfferBuilder(List<SimplifiedOfferParameter> simplifiedOfferParameters) {
        DataCampOfferMarketContent.MarketParameterValues.Builder paramValueBuilder =
                DataCampOfferMarketContent.MarketParameterValues.newBuilder();

        List<MarketParameterValueWrapper> parameterValueWrapperList = new ArrayList();

        simplifiedOfferParameters.forEach(parameter ->
        {
            switch (parameter.getType()) {
                case STRING:
                    createStringParameter(
                            parameter.getParamId(),
                            parameter.getParamName(),
                            parameter.getStrValue(),
                            parameterValueWrapperList::add
                    );
                    break;
                case OPTION:
                    createEnumParameter(parameter.getParamId(),
                            parameter.getParamName(),
                            parameter.getOptionId(),
                            parameterValueWrapperList::add
                    );
                    break;
                case BOOLEAN:
                    createBooleanParameter(parameter.getParamId(),
                            parameter.getParamName(),
                            parameter.getBooleanValue(),
                            parameterValueWrapperList::add
                    );
            }
        });

        parameterValueWrapperList.forEach(marketParameterValueWrapper ->
                paramValueBuilder.addParameterValues(marketParameterValueWrapper.toMarketParameterValue()));

        return DataCampOffer.Offer.newBuilder()
                .setContent(DataCampOfferContent.OfferContent.newBuilder()
                        .setPartner(DataCampOfferContent.PartnerContent.newBuilder()
                                .setMarketSpecificContent(DataCampOfferMarketContent.MarketSpecificContent
                                        .newBuilder()
                                        .setParameterValues(paramValueBuilder
                                                .build())
                                        .build()
                                ).build())
                        .build());
    }


    public static DataCampOffer.Offer generateEmptyOffer() {
        DataCampOfferMarketContent.MarketParameterValues.Builder paramValueBuilder =
                DataCampOfferMarketContent.MarketParameterValues.newBuilder();
        return DataCampOffer.Offer.newBuilder()
                .setContent(DataCampOfferContent.OfferContent.newBuilder()
                        .setPartner(DataCampOfferContent.PartnerContent.newBuilder()
                                .setMarketSpecificContent(DataCampOfferMarketContent.MarketSpecificContent
                                        .newBuilder()
                                        .setParameterValues(paramValueBuilder
                                                .build())
                                        .build()
                                ).build())
                        .build())
                .build();
    }

    public static MarketParameterValueWrapper buildOfferParam() {
        return new MarketParameterValueWrapper(DataCampContentMarketParameterValue
                .MarketParameterValue.newBuilder()
                .setParamId(PARAM_ID_1)
                .setValue(DataCampContentMarketParameterValue.MarketValue
                        .newBuilder().setStrValue(VALUE_1).build())
                .build());
    }
}
