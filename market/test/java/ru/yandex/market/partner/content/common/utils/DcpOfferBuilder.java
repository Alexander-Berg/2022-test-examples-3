package ru.yandex.market.partner.content.common.utils;

import Market.DataCamp.DataCampContentMarketParameterValue;
import Market.DataCamp.DataCampOffer;
import Market.DataCamp.DataCampOfferContent;
import Market.DataCamp.DataCampOfferMarketContent;
import Market.DataCamp.DataCampOfferMeta;
import Market.DataCamp.DataCampOfferPictures;
import com.google.protobuf.Timestamp;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

public class DcpOfferBuilder {
    private final DataCampOffer.Offer.Builder builder = DataCampOffer.Offer.newBuilder();
    private final Timestamp updatedTimestamp;

    public DcpOfferBuilder(int businessId, String offerId) {
        this(new java.sql.Timestamp(System.currentTimeMillis()), businessId, offerId);
    }

    public DcpOfferBuilder(java.sql.Timestamp createTime, int businessId, String offerId) {
        updatedTimestamp = Timestamp.newBuilder()
            .setSeconds(TimeUnit.MILLISECONDS.toSeconds(createTime.getTime()))
            .setNanos(createTime.getNanos())
            .build();

        builder.getIdentifiersBuilder()
            .setBusinessId(businessId)
            .setOfferId(offerId);
    }

    public DcpOfferBuilder withCategory(int categoryId) {
        builder.getContentBuilder().getBindingBuilder().getApprovedBuilder()
            .setMeta(updateMeta())
            .setMarketCategoryId(categoryId);
        return this;
    }

    public DcpOfferBuilder withTitle(String title) {
        actualBuilder()
            .setTitle(
                stringValue(title).setMeta(updateMeta())
            );
        return this;
    }

    public DcpOfferBuilder withUrl(String url) {
        actualBuilder()
            .setUrl(
                stringValue(url).setMeta(updateMeta())
            );
        return this;
    }

    public DcpOfferBuilder withName(String name) {
        actualBuilder()
            .setTitle(
                stringValue(name).setMeta(updateMeta())
            );
        return this;
    }

    public DcpOfferBuilder withDescription(String description) {
        actualBuilder()
            .setDescription(
                stringValue(description).setMeta(updateMeta())
            );
        return this;
    }

    public DcpOfferBuilder withVendor(String vendor) {
        actualBuilder()
            .setVendor(
                stringValue(vendor).setMeta(updateMeta())
            );
        return this;
    }

    public DcpOfferBuilder withVendorCode(String vendorCode) {
        actualBuilder()
            .setVendorCode(
                stringValue(vendorCode).setMeta(updateMeta())
            );
        return this;
    }

    public DcpOfferBuilder withBarCodes(String... barcodes) {
        actualBuilder()
            .setBarcode(DataCampOfferMeta.StringListValue.newBuilder()
                .setMeta(updateMeta())
                .addAllValue(Arrays.asList(barcodes))
            );
        return this;
    }

    public DcpOfferBuilder withGroup(int id, String name) {
        return withGroupId(id)
            .withGroupName(name);
    }

    public DcpOfferBuilder withGroupId(int id) {
        originalBuilder().getGroupIdBuilder()
            .setMeta(updateMeta())
            .setValue(id);
        return this;
    }

    public DcpOfferBuilder withGroupName(String name) {
        originalBuilder().getGroupNameBuilder()
            .setMeta(updateMeta())
            .setValue(name);
        return this;
    }

    public DcpOfferBuilder withPicture(String url) {
        return withPictures(Collections.singletonList(url));
    }

    public DcpOfferBuilder withPictures(String... urls) {
        return withPictures(Arrays.asList(urls));
    }

    public DcpOfferBuilder withPictures(Collection<String> urls) {
        for (String url : urls) {
            builder.getPicturesBuilder().getPartnerBuilder().getOriginalBuilder()
                .setMeta(updateMeta())
                .addSourceBuilder()
                .setUrl(url)
                .setSource(DataCampOfferPictures.PictureSource.UNDEFINED);
            builder.getPicturesBuilder()
                .getPartnerBuilder()
                .putActual(
                    url,
                    DataCampOfferPictures.MarketPicture.newBuilder()
                        .setMeta(updateMeta())
                        .setOriginal(DataCampOfferPictures.MarketPicture.Picture.newBuilder()
                            .setUrl("idx_" + url)
                        ).build()
                );
        }
        return this;
    }

    public DcpOfferBuilder withStringParam(long paramId, String value) {
        return withStringParam(paramId, value, (DataCampContentMarketParameterValue.MarketValueSource) null);
    }

    public DcpOfferBuilder withStringParam(long paramId, String paramName, String value) {
        return withStringParam(paramId, paramName, value, null);
    }

    public DcpOfferBuilder withStringParam(long paramId, String value,
                                           DataCampContentMarketParameterValue.MarketValueSource valueSource) {
        return withStringParam(paramId, null, value, valueSource);
    }

    public DcpOfferBuilder withStringParam(long paramId, String paramName, String value,
                                           DataCampContentMarketParameterValue.MarketValueSource valueSource) {
        DataCampContentMarketParameterValue.MarketParameterValue.Builder paramValueBuilder = createParamValue(paramId, paramName);
        paramValueBuilder.setValue(DataCampContentMarketParameterValue.MarketValue.newBuilder()
            .setStrValue(value)
            .setValueType(DataCampContentMarketParameterValue.MarketValueType.STRING)
        );
        if (valueSource != null) {
            paramValueBuilder.setValueSource(valueSource);
        }
        parameterValues()
            .setMeta(updateMeta())
            .addParameterValues(paramValueBuilder);
        return this;
    }

    public DcpOfferBuilder withEnumParam(long paramId, long optionId, String value) {
        return withEnumParam(paramId, optionId, value, null);
    }

    public DcpOfferBuilder withEnumParam(long paramId, long optionId, String value,
                                         DataCampContentMarketParameterValue.MarketValueSource valueSource) {
        DataCampContentMarketParameterValue.MarketParameterValue.Builder paramValueBuilder = createParamValue(paramId);
        paramValueBuilder.setValue(DataCampContentMarketParameterValue.MarketValue.newBuilder()
            .setStrValue(value)
            .setOptionId(optionId)
            .setValueType(DataCampContentMarketParameterValue.MarketValueType.ENUM)
        );
        if (valueSource != null) {
            paramValueBuilder.setValueSource(DataCampContentMarketParameterValue.MarketValueSource.PARTNER_CONTENT_MAPPING);
        }


        parameterValues()
            .setMeta(updateMeta())
            .addParameterValues(paramValueBuilder);

        return this;
    }

    public DcpOfferBuilder withBooleanParam(long paramId, boolean value) {
        DataCampContentMarketParameterValue.MarketParameterValue.Builder paramValueBuilder = createParamValue(paramId);
        paramValueBuilder.setValue(DataCampContentMarketParameterValue.MarketValue.newBuilder()
            .setBoolValue(value)
            .setValueType(DataCampContentMarketParameterValue.MarketValueType.BOOLEAN)
        );

        parameterValues()
            .setMeta(updateMeta())
            .addParameterValues(paramValueBuilder);
        return this;
    }

    public DcpOfferBuilder withNumericParam(long paramId, String value) {
        return withNumericParam(paramId, null, value);
    }

    public DcpOfferBuilder withNumericParam(long paramId, String paramName, String value) {
        DataCampContentMarketParameterValue.MarketParameterValue.Builder paramValueBuilder = createParamValue(paramId, paramName);
        paramValueBuilder.setValue(DataCampContentMarketParameterValue.MarketValue.newBuilder()
            .setNumericValue(value)
            .setValueType(DataCampContentMarketParameterValue.MarketValueType.NUMERIC)
        );

        parameterValues()
            .setMeta(updateMeta())
            .addParameterValues(paramValueBuilder);
        return this;
    }

    public DcpOfferBuilder withNumericEnumParam(long paramId, long optionId, String value) {
        DataCampContentMarketParameterValue.MarketParameterValue.Builder paramValueBuilder = createParamValue(paramId);
        paramValueBuilder.setValue(DataCampContentMarketParameterValue.MarketValue.newBuilder()
            .setOptionId(optionId)
            .setNumericValue(value)
            .setValueType(DataCampContentMarketParameterValue.MarketValueType.NUMERIC_ENUM)
        );

        parameterValues()
            .setMeta(updateMeta())
            .addParameterValues(paramValueBuilder);
        return this;
    }

    public DcpOfferBuilder withHypothesisParam(long paramId, String value) {
        DataCampContentMarketParameterValue.MarketParameterValue.Builder paramValueBuilder = createParamValue(paramId);
        paramValueBuilder.setValue(DataCampContentMarketParameterValue.MarketValue.newBuilder()
            .setStrValue(value)
            .setValueType(DataCampContentMarketParameterValue.MarketValueType.HYPOTHESIS)
        );

        parameterValues()
            .setMeta(updateMeta())
            .addParameterValues(paramValueBuilder);
        return this;
    }

    public DataCampOffer.Offer build() {
        return builder.build();
    }

    private DataCampContentMarketParameterValue.MarketParameterValue.Builder createParamValue(long paramId) {
        return createParamValue(paramId, null);
    }

    private DataCampContentMarketParameterValue.MarketParameterValue.Builder createParamValue(long paramId,
                                                                                              String paramName) {
        DataCampContentMarketParameterValue.MarketParameterValue.Builder builder =
            DataCampContentMarketParameterValue.MarketParameterValue.newBuilder()
            .setParamId(paramId)
            .setValueSource(DataCampContentMarketParameterValue.MarketValueSource.CONTENT_EXCEL);
        if (paramName != null) {
            builder.setParamName(paramName);
        }
        return builder;
    }

    private DataCampOfferMarketContent.MarketParameterValues.Builder parameterValues() {
        return builder.getContentBuilder()
            .getPartnerBuilder()
            .getMarketSpecificContentBuilder()
            .getParameterValuesBuilder();
    }

    private DataCampOfferMeta.StringValue.Builder stringValue(String value) {
        return DataCampOfferMeta.StringValue.newBuilder().setValue(value);
    }

    private DataCampOfferMeta.UpdateMeta updateMeta() {
        return DataCampOfferMeta.UpdateMeta.newBuilder()
            .setTimestamp(updatedTimestamp)
            .setSource(DataCampOfferMeta.DataSource.MARKET_DATACAMP)
            .build();
    }

    private DataCampOfferContent.OriginalSpecification.Builder originalBuilder() {
        return builder.getContentBuilder().getPartnerBuilder().getOriginalBuilder();
    }

    private DataCampOfferContent.ProcessedSpecification.Builder actualBuilder() {
        return builder.getContentBuilder().getPartnerBuilder().getActualBuilder();
    }
}
