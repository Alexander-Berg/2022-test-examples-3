package ru.yandex.market.psku.postprocessor.service.migration;

import ru.yandex.market.ir.autogeneration.common.helpers.MigrationUtils;
import ru.yandex.market.mbo.export.MboParameters;
import ru.yandex.market.mbo.http.ModelStorage;

/**
 * @author Nur-Magomed Dzhamiev <a href="mailto:n-mago@yandex-team.ru"></a>
 */
public class MigrationTestUtils {

    private MigrationTestUtils() {

    }

    public static ModelStorage.ParameterValue createShopSkuParam(String shopSku) {
        return ModelStorage.ParameterValue.newBuilder()
            .setParamId(MigrationUtils.SHOP_SKU_PARAM_ID)
            .setValueType(MboParameters.ValueType.STRING)
            .setValueSource(ModelStorage.ModificationSource.OPERATOR_CONFIRMED)
            .addStrValue(ModelStorage.LocalizedString.newBuilder()
                .setValue(shopSku)
                .build())
            .build();
    }
}
