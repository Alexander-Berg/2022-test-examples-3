package ru.yandex.market.gutgin.tms.pipeline.good.taskaction.databucket_validations.groupid;

import Market.DataCamp.DataCampContentMarketParameterValue;
import Market.DataCamp.DataCampOffer;
import ru.yandex.market.gutgin.tms.pipeline.good.taskaction.datacamp_pipeline.DcpSpecialParameters;
import ru.yandex.market.ir.excel.generator.param.MainParamCreator;
import ru.yandex.market.mbo.export.MboParameters;
import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.robot.db.ParameterValueComposer;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

abstract class TestParamFactory<OBJ> {
    static final TestParamFactory<DataCampOffer.Offer.Builder> TICKET_PARAM_FACTORY = new ForTicket();

    static final TestParamFactory<ModelStorage.Model.Builder> MODEL_STORAGE_PARAM_FACTORY = new ForModel();

    abstract void createStringParam(long id, String value, OBJ obj);

    abstract void createEnumParam(long id, EnumValue value, OBJ obj);

    abstract void createNumericParam(long id, int value, OBJ obj);

    abstract void createNumericEnumParam(long id, int value, OBJ obj);

    abstract void createBooleanParam(long id, boolean value, OBJ obj);

    abstract void createHypothesisParam(long id, String value, OBJ obj);

    interface EnumValue {
        int getCode();
    }

    private static class ForTicket extends TestParamFactory<DataCampOffer.Offer.Builder> {
        @Override
        void createStringParam(long id, String value, DataCampOffer.Offer.Builder obj) {
            createImpl(id, DataCampContentMarketParameterValue.MarketValueType.STRING, DataCampContentMarketParameterValue.MarketValue.Builder::setStrValue, value, obj, Function.identity());
        }

        @Override
        void createEnumParam(long id, EnumValue value, DataCampOffer.Offer.Builder obj) {
            createImpl(id, DataCampContentMarketParameterValue.MarketValueType.ENUM, (b, v) -> b.setOptionId(v.getCode()), value, obj, null);
        }

        @Override
        void createNumericParam(long id, int value, DataCampOffer.Offer.Builder obj) {
            createImpl(id, DataCampContentMarketParameterValue.MarketValueType.NUMERIC, (b, v) -> b.setNumericValue(v + ""), value, obj, null);
        }

        @Override
        void createNumericEnumParam(long id, int value, DataCampOffer.Offer.Builder obj) {
            createImpl(id, DataCampContentMarketParameterValue.MarketValueType.NUMERIC_ENUM, (b, v) -> b.setNumericValue(v + ""), value, obj, null);
        }

        @Override
        void createBooleanParam(long id, boolean value, DataCampOffer.Offer.Builder obj) {
            createImpl(id, DataCampContentMarketParameterValue.MarketValueType.BOOLEAN, DataCampContentMarketParameterValue.MarketValue.Builder::setBoolValue, value, obj, null);
        }

        @Override
        void createHypothesisParam(long id, String value, DataCampOffer.Offer.Builder obj) {
            createImpl(id,
                DataCampContentMarketParameterValue.MarketValueType.HYPOTHESIS,
                DataCampContentMarketParameterValue.MarketValue.Builder::setStrValue,
                value,
                obj,
                null
            );
        }

        private <T> void createImpl(
                long id,
                DataCampContentMarketParameterValue.MarketValueType type,
                BiConsumer<DataCampContentMarketParameterValue.MarketValue.Builder, T> valueSetter,
                T value,
                DataCampOffer.Offer.Builder objBuilder,
                Function<T, String> specialParamTypeHandler
        ) {
            boolean intercepted = DcpSpecialParamWriters.writeSpecial(id, value, specialParamTypeHandler, objBuilder);
            if (intercepted) {
                return;
            }

            BiConsumer<DataCampOffer.Offer.Builder, String> specialWriter = DcpSpecialParamWriters.STRING_WRITERS.get(id);
            if (specialWriter != null) {
                if (specialParamTypeHandler == null) {
                    throw new IllegalArgumentException("This type is not supported for special params");
                }
                specialWriter.accept(objBuilder, specialParamTypeHandler.apply(value));
                return;
            }

            DataCampContentMarketParameterValue.MarketParameterValue.Builder paramBuilder =
                    objBuilder.getContentBuilder().getPartnerBuilder().getMarketSpecificContentBuilder()
                            .getParameterValuesBuilder().addParameterValuesBuilder();
            paramBuilder.setParamId(id);
            DataCampContentMarketParameterValue.MarketValue.Builder valueBuilder = paramBuilder.getValueBuilder();
            valueSetter.accept(valueBuilder, value);
            valueBuilder.setValueType(type);
        }

        private static class DcpSpecialParamWriters {
            static final Map<Long, BiConsumer<DataCampOffer.Offer.Builder, String>> STRING_WRITERS;

            static {
                STRING_WRITERS = new HashMap<>();

                STRING_WRITERS.put(ParameterValueComposer.NAME_ID,
                        (builder, v) -> builder.getContentBuilder().getPartnerBuilder().getActualBuilder().getTitleBuilder().setValue(v));
                STRING_WRITERS.put(ParameterValueComposer.URL_ID,
                        (builder, v) -> builder.getContentBuilder().getPartnerBuilder().getActualBuilder().getUrlBuilder().setValue(v));
                STRING_WRITERS.put(ParameterValueComposer.VENDOR_CODE_ID,
                        (builder, v) -> builder.getContentBuilder().getPartnerBuilder().getActualBuilder().getVendorCodeBuilder().setValue(v));

                STRING_WRITERS.put(ParameterValueComposer.BARCODE_ID,
                        (builder, v) -> builder.getContentBuilder().getPartnerBuilder().getActualBuilder().getBarcodeBuilder().addValue(v));

                STRING_WRITERS.put(ParameterValueComposer.VENDOR_ID,
                    (builder, v) -> builder.getContentBuilder().getPartnerBuilder().getActualBuilder().getVendorBuilder().setValue(v));

                STRING_WRITERS.put(MainParamCreator.DESCRIPTION_ID,
                    (builder, v) -> builder.getContentBuilder().getPartnerBuilder().getActualBuilder().getDescriptionBuilder().setValue(v));

                if (!STRING_WRITERS.keySet().equals(DcpSpecialParameters.ALL_PARAM_MAP.keySet())) {
                    throw new IllegalArgumentException("Writer map doesn't not consistently pair the reader map");
                }
            }

            static <T> boolean writeSpecial(long id, T value, Function<T, String> specialParamTypeHandler, DataCampOffer.Offer.Builder objBuilder) {
                BiConsumer<DataCampOffer.Offer.Builder, String> specialWriter = STRING_WRITERS.get(id);
                if (specialWriter == null) {
                    return false;
                }
                if (specialParamTypeHandler == null) {
                    throw new IllegalArgumentException("This type is not supported for special params");
                }
                specialWriter.accept(objBuilder, specialParamTypeHandler.apply(value));
                return true;
            }
        }
    }

    private static class ForModel extends TestParamFactory<ModelStorage.Model.Builder> {
        @Override
        void createStringParam(long id, String value, ModelStorage.Model.Builder obj) {
            createImpl(id, MboParameters.ValueType.STRING, b -> b.addStrValue(ModelStorage.LocalizedString.newBuilder().setValue(value).build()), obj);
        }

        @Override
        void createEnumParam(long id, EnumValue value, ModelStorage.Model.Builder obj) {
            createImpl(id, MboParameters.ValueType.ENUM, b -> b.setOptionId(value.getCode()), obj);
        }

        @Override
        void createNumericParam(long id, int value, ModelStorage.Model.Builder obj) {
            createImpl(id, MboParameters.ValueType.NUMERIC, b -> b.setNumericValue(value + ""), obj);
        }

        @Override
        void createNumericEnumParam(long id, int value, ModelStorage.Model.Builder obj) {
            createImpl(id, MboParameters.ValueType.NUMERIC_ENUM, b -> b.setNumericValue(value + ""), obj);
        }

        @Override
        void createBooleanParam(long id, boolean value, ModelStorage.Model.Builder obj) {
            createImpl(id, MboParameters.ValueType.BOOLEAN, b -> b.setBoolValue(value), obj);
        }

        @Override
        void createHypothesisParam(long id, String value, ModelStorage.Model.Builder objBuilder) {
            ModelStorage.ParameterValueHypothesis.Builder paramBuilder = objBuilder.addParameterValueHypothesisBuilder();
            paramBuilder.addStrValue(MboParameters.Word.newBuilder().setName(value).build())
                    .setParamId(id)
                    .setValueType(MboParameters.ValueType.STRING);
        }

        private void createImpl(
                long id,
                MboParameters.ValueType type,
                Consumer<ModelStorage.ParameterValue.Builder> valueSetter,
                ModelStorage.Model.Builder objBuilder) {
            ModelStorage.ParameterValue.Builder valueBuilder = objBuilder.addParameterValuesBuilder();
            valueBuilder
                    .setParamId(id)
                    .setValueType(type);
            valueSetter.accept(valueBuilder);
        }

    }
}
