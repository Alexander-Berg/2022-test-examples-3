package ru.yandex.market.partner.content.common.csku.judge;

import ru.yandex.market.mbo.http.ModelStorage;

public class ParameterGenerator {

    public static ModelStorage.ParameterValue.Builder generateEmptyParam() {
        return ModelStorage.ParameterValue
                .newBuilder()
                .setParamId(123L)
                .setXslName("someParam");
    }
}
