package ru.yandex.market.logistics.management.service.export.dynamic.validation.rule;

import java.util.Objects;

import javax.annotation.Nonnull;

import org.springframework.stereotype.Component;

import ru.yandex.market.logistics.management.domain.entity.PartnerRelationModel;
import ru.yandex.market.logistics.management.service.export.dynamic.validation.Result;
import ru.yandex.market.logistics.management.service.export.dynamic.validation.TypedValidationRule;

@Component
public class MockPartnerRelationRule extends TypedValidationRule<PartnerRelationModel> {

    @Nonnull
    @Override
    public Result validate(@Nonnull PartnerRelationModel partnerRelation) {
        String cause = "mock cause";

        return !Objects.equals(partnerRelation.getHandlingTime(), 666) ? Result.ok() :
            partnerRelation.getEnabled() ? Result.failed(cause) : Result.warn(cause);
    }

}
