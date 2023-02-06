package ru.yandex.direct.grid.processing.service.group.converter;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.AdGroupAdditionalTargetingJoinType;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.AdGroupAdditionalTargetingMode;
import ru.yandex.direct.grid.core.entity.group.model.additionaltargeting.GdiAdditionalTargeting;

@ParametersAreNonnullByDefault
public class AdditionalTargetingConverterFailTest {
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void testConvert_unknownTargetingType() {
        var unknownTargeting = new UnknownTargetingType()
                .withTargetingMode(AdGroupAdditionalTargetingMode.TARGETING)
                .withJoinType(AdGroupAdditionalTargetingJoinType.ANY);

        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Unknown targeting type: " + UnknownTargetingType.class);

        AdditionalTargetingConverter.convert(unknownTargeting);
    }

    private final static class UnknownTargetingType extends GdiAdditionalTargeting {}
}
