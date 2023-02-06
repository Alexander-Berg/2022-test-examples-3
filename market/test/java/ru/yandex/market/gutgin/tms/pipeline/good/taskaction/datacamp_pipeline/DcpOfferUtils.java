package ru.yandex.market.gutgin.tms.pipeline.good.taskaction.datacamp_pipeline;

import java.util.function.Consumer;

import Market.DataCamp.DataCampContentMarketParameterValue;
import Market.DataCamp.DataCampOffer;

import ru.yandex.market.partner.content.common.db.jooq.tables.pojos.DatacampOffer;

public class DcpOfferUtils {

    public static void initOffer(long categoryId, DatacampOffer offer, Consumer<DataCampOffer.Offer.Builder> build) {
        DataCampOffer.Offer.Builder builder = DataCampOffer.Offer.newBuilder();
        builder
                .getContentBuilder().getBindingBuilder().getApprovedBuilder()
                .setMarketCategoryId(Math.toIntExact(categoryId));
        build.accept(builder);
        offer.setData(builder.build());
    }

    public static void addNumericParam(int paramId, String paramName, String paramValue,
                                       DataCampOffer.Offer.Builder builder) {
        builder
                .getContentBuilder().getPartnerBuilder().getMarketSpecificContentBuilder()
                .getParameterValuesBuilder()
                .addParameterValues(numericParamValue(paramId, paramName, paramValue));
    }

    public static void addStrParam(int paramId, String paramName, String paramValue,
                                       DataCampOffer.Offer.Builder builder) {
        builder
                .getContentBuilder().getPartnerBuilder().getMarketSpecificContentBuilder()
                .getParameterValuesBuilder()
                .addParameterValues(strParamValue(paramId, paramName, paramValue));
    }

    public static void setMarketSkuId(Long marketSkuId, DataCampOffer.Offer.Builder builder) {
        builder
                .getContentBuilder().getBindingBuilder().getApprovedBuilder().setMarketSkuId(marketSkuId);
    }

    private static DataCampContentMarketParameterValue.MarketParameterValue.Builder numericParamValue(
            int paramId, String paramName, String value) {
        return DataCampContentMarketParameterValue.MarketParameterValue.newBuilder()
                .setParamId(paramId).setParamName(paramName)
                .setValue(DataCampContentMarketParameterValue.MarketValue.newBuilder()
                        .setStrValue(value)
                        .setNumericValue(value)
                        .setValueType(DataCampContentMarketParameterValue.MarketValueType.NUMERIC));
    }

    private static DataCampContentMarketParameterValue.MarketParameterValue.Builder strParamValue(
            int paramId, String paramName, String value) {
        return DataCampContentMarketParameterValue.MarketParameterValue.newBuilder()
                .setParamId(paramId).setParamName(paramName)
                .setValue(DataCampContentMarketParameterValue.MarketValue.newBuilder()
                        .setStrValue(value)
                        .setValueType(DataCampContentMarketParameterValue.MarketValueType.STRING));
    }
}
