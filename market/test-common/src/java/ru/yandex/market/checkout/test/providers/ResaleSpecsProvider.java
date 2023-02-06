package ru.yandex.market.checkout.test.providers;

import java.util.Objects;

import ru.yandex.market.common.report.model.resale.ResaleSpecs;

public abstract class ResaleSpecsProvider {

    public static ResaleSpecs getResaleSpecs(Long id) {
        ResaleSpecs resaleSpecs = new ResaleSpecs();
        if (Objects.nonNull(id)) {
            resaleSpecs.setId(id);
        }
        resaleSpecs.setReasonValue("rv");
        resaleSpecs.setReasonText("rt");
        resaleSpecs.setConditionValue("cv");
        resaleSpecs.setConditionText("ct");
        return resaleSpecs;
    }
}
