package ru.yandex.market.tsum.spok.validation.validator;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.market.tsum.core.registry.v2.model.spok.ServiceParams;

public class SpokPackageNameValidatorTest {
    private static final SpokPackageNameValidator VALIDATOR = new SpokPackageNameValidator();

    @Test
    public void nameIsNull() {
        Assert.assertFalse(VALIDATOR.validate(serviceParams(null)).isValid());
    }

    @Test
    public void nameContainsUnderlines() {
        Assert.assertFalse(VALIDATOR.validate(serviceParams("lcmp_test_service")).isValid());
    }

    @Test
    public void noJavaAppType() {
        ServiceParams params = serviceParams("lcmp_test_service");
        params.setApplicationType("CPP");
        Assert.assertTrue(VALIDATOR.validate(params).isValid());
    }

    @Test
    public void correctName() {
        Assert.assertTrue(VALIDATOR.validate(serviceParams("lcmptestservice")).isValid());
    }

    @Nonnull
    private ServiceParams serviceParams(@Nullable String name) {
        ServiceParams result = new ServiceParams();
        result.setJavaPackage(name);
        result.setApplicationType("JAVA");
        return result;
    }
}
