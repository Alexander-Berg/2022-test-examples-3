package ru.yandex.market.partner.content.common.csku;

import java.security.InvalidParameterException;
import java.util.Collections;
import java.util.List;

import ru.yandex.market.mbo.export.MboParameters;
import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.partner.content.common.csku.util.ParameterCreator;

public class ModelGenerator {

    public static ModelStorage.Model.Builder generateModelBuilder(
            List<SimplifiedOfferParameter> simplifiedOfferParameters,
            long supplierId,
            ModelStorage.ModificationSource modificationSource,
            int formalizationConfidence
    ) {
        ModelStorage.Model.Builder model = ModelStorage.Model.newBuilder().setId(1);
        simplifiedOfferParameters.forEach(parameter -> {
            ModelStorage.ParameterValue parameterValue = ParameterCreator.createStringParam(
                    parameter.getParamId(),
                    parameter.getParamName(),
                    Collections.singletonList(parameter.getStrValue()),
                    supplierId,
                    modificationSource,
                    formalizationConfidence
            );
            model.addParameterValues(parameterValue);
        });
        return model;
    }

    public static ModelStorage.ParameterValue generateStringParam(long id, String name, String value,
                                                                  long ownerId,
                                                                  ModelStorage.ModificationSource source) {
        if (source == ModelStorage.ModificationSource.FORMALIZATION) {
            throw new InvalidParameterException("must pass formalizationConfidence, check other overloaded method");
        }
        return generateStringParam(id, name, value, ownerId, source, 0);
    }

    public static ModelStorage.ParameterValue generateStringParam(long id, String name, String value,
                                                                  long ownerId,
                                                                  ModelStorage.ModificationSource source,
                                                                  int formalizationConfidence) {
        ModelStorage.ParameterValue pv = ParameterCreator.createStringParam(
                id,
                name,
                Collections.singletonList(value),
                ownerId,
                source,
                formalizationConfidence
        );
        return pv.toBuilder().setValueSource(source).build();
    }

    public static ModelStorage.Model.Builder generateModelBuilder(
            List<ModelStorage.ParameterValue> parameters
    ) {
        return ModelStorage.Model.newBuilder().addAllParameterValues(parameters)
                .setSourceType(ModelStorage.ModelType.PARTNER_SKU.name());
    }

}
