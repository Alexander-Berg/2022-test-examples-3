package ru.yandex.market.markup2.tasks.fill_param_values;

import ru.yandex.market.markup2.utils.ModelTestUtils;
import ru.yandex.market.markup2.utils.offer.Offer;
import ru.yandex.market.mbo.export.MboParameters;
import ru.yandex.market.mbo.http.ModelStorage;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * @author V.Zaytsev (breezzo@yandex-team.ru)
 * @since 29.06.2017
 */
@SuppressWarnings("checkstyle:MagicNumber")
public class ModelsData {
    public static final long VENDOR_OPTION_ID = 146;
    public static final String STRING_VALUE = "Value";

    private static final Random RANDOM = new Random(System.currentTimeMillis());

    private static long nextModelId = 0;

    private ModelsData() {

    }

    static List<ModelStorage.Model> generateModels(
        int categoryId, List<MboParameters.Parameter> parameters, int count) {
        return generateModels(categoryId, parameters, count, null, true);
    }

    static List<ModelStorage.Model> generateModels(
        int categoryId, List<MboParameters.Parameter> parameters,
        int count, List<Offer> offers, boolean generateEmpty) {

        List<ModelStorage.Model> models = new ArrayList<>();
        List<MboParameters.Parameter> parametersForFill = new ArrayList<>(parameters);
        Collections.shuffle(parametersForFill);

        if (generateEmpty) {
            parametersForFill = parametersForFill.subList(0, parametersForFill.size() / 3);
        }

        for (int i = 0; i < count; i++) {
            ModelStorage.Model.Builder modelBuilder = ModelTestUtils.createModelBuilder(
                categoryId, VENDOR_OPTION_ID, nextModelId++,
                ModelStorage.ModelType.GURU.name(), true);

            if (offers != null) {
                modelBuilder.addClusterizerOfferIds(offers.get(i).getOfferId());
            }

            for (MboParameters.Parameter parameter : parametersForFill) {
                switch (parameter.getValueType()) {
                    case BOOLEAN:
                        modelBuilder.addParameterValues(booleanValue(parameter));
                        break;
                    case ENUM:
                    case NUMERIC_ENUM:
                        modelBuilder.addParameterValues(
                            ModelTestUtils.createOptionValue(parameter, randomOptionId(parameter.getOptionList())));
                        break;
                    case NUMERIC:
                        modelBuilder.addParameterValues(numericValue(parameter));
                        break;
                    case STRING:
                        modelBuilder.addParameterValues(stringValue(parameter));
                    default:
                }
            }

            models.add(modelBuilder.build());
        }

        return models;
    }

    private static ModelStorage.ParameterValue numericValue(MboParameters.Parameter param) {
        double value = RANDOM.nextInt((int) (param.getMaxValue() - param.getMinValue())) + param.getMinValue();
        return ModelTestUtils.createNumericValue(param, new BigDecimal(value).toPlainString()).build();
    }

    private static ModelStorage.ParameterValue stringValue(MboParameters.Parameter param) {
        return ModelTestUtils.createStringValue(param, STRING_VALUE).build();
    }

    private static ModelStorage.ParameterValue.Builder booleanValue(MboParameters.Parameter param) {
        MboParameters.Option option = param.getOption(0);
        return ModelTestUtils.createOptionValue(param, (int) option.getId())
            .setBoolValue(Boolean.valueOf(option.getName(0).getName()));
    }

    private static int randomOptionId(List<MboParameters.Option> options) {
        int index = RANDOM.nextInt(options.size());
        return (int) options.get(index).getId();
    }
}
