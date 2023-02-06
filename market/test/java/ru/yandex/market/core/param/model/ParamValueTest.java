package ru.yandex.market.core.param.model;

import java.io.Serializable;
import java.util.Date;

import org.apache.commons.lang3.SerializationUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author wadim
 */
public class ParamValueTest {

    @Test
    void testSerialization() {
        final long entityId = 113;
        checkParamValueSerialization(new DateParamValue(ParamType.CPA_LATEST_ACTIVITY, entityId, new Date(2000, 3, 20)));
        checkParamValueSerialization(new StringParamValue(ParamType.DATASOURCE_DOMAIN, entityId, "ya.ru"));
        checkParamValueSerialization(new BooleanParamValue(ParamType.NEVER_PAID, entityId, true));
        checkParamValueSerialization(new NumberParamValue(ParamType.DAILY_BUDGET_LIMIT, entityId, 10));
    }

    private void checkParamValueSerialization(final Serializable expected) {
        final byte[] data = SerializationUtils.serialize(expected);
        final Object actual = SerializationUtils.deserialize(data);
        Assertions.assertEquals(expected, actual);
    }

}
