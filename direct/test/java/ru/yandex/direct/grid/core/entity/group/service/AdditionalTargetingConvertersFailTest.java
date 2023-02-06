package ru.yandex.direct.grid.core.entity.group.service;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.AdGroupAdditionalTargeting;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.AdGroupAdditionalTargetingJoinType;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.AdGroupAdditionalTargetingMode;

@ParametersAreNonnullByDefault
public class AdditionalTargetingConvertersFailTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void testConvert_unknownTargetingType() {
        var unknownTargeting = new UnknownTargetingType()
                .withTargetingMode(AdGroupAdditionalTargetingMode.TARGETING)
                .withJoinType(AdGroupAdditionalTargetingJoinType.ANY)
                .withAdGroupId(1L);

        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Unknown targeting type: " + UnknownTargetingType.class);

        AdditionalTargetingConverters.convert(unknownTargeting);
    }

    private final static class UnknownTargetingType extends AdGroupAdditionalTargeting {}
}
