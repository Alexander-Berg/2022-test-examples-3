package ru.yandex.direct.api.v5.entity.dynamictextadtargets.validation;

import com.yandex.direct.api.v5.dynamictextadtargets.SetBidsItem;
import com.yandex.direct.api.v5.dynamictextadtargets.SetBidsRequest;
import org.junit.BeforeClass;
import org.junit.Test;

import ru.yandex.direct.api.v5.validation.DefectType;
import ru.yandex.direct.validation.result.ValidationResult;

import static org.assertj.core.api.Assertions.assertThat;

public class DynamicTextAdTargetsSetBidsRequestValidatorPositiveTest {

    private static DynamicTextAdTargetsSetBidsRequestValidator validator;

    @BeforeClass
    public static void setUp() {
        validator = new DynamicTextAdTargetsSetBidsRequestValidator();
    }

    @Test
    public void test() {
        SetBidsRequest request = new SetBidsRequest()
                .withBids(new SetBidsItem()
                        .withId(1L)
                        .withBid(123L));

        ValidationResult<SetBidsRequest, DefectType> validationResult = validator.validate(request);

        assertThat(validationResult.hasAnyErrors()).isEqualTo(false);
    }

}
