package ru.yandex.vendor;

import org.junit.Assert;
import ru.yandex.vendor.exception.BadParamException;
import ru.yandex.vendor.exception.VendorExceptionFactory;

public final class VendorTestUtils {

    private VendorTestUtils() {
    }

    public static void assertParamInvalid(BadParamException e, String code, String paramName) {
        BadParamException expected = VendorExceptionFactory.paramInvalid(code, paramName, "");
        Assert.assertEquals(expected.getCode(), e.getCode());
        Assert.assertEquals(expected.getDetails(), e.getDetails());
    }

    public static void assertParamInvalid(BadParamException e, String paramName) {
        BadParamException expected = VendorExceptionFactory.paramInvalid(paramName, "");
        Assert.assertEquals(expected.getCode(), e.getCode());
        Assert.assertEquals(expected.getDetails(), e.getDetails());
    }
}
