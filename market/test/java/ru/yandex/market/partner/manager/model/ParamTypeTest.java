package ru.yandex.market.partner.manager.model;

import org.junit.jupiter.api.Test;

import ru.yandex.market.core.param.model.ParamType;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Nikolay Malevanny nmalevanny@yandex-team.ru
 */
class ParamTypeTest {
    @Test
    void testCheckString() {
        assertTrue(ParamType.isStringValue(ParamType.PHONE_NUMBER.getId()));
        assertFalse(ParamType.isStringValue(ParamType.LOCAL_DELIVERY_REGION.getId()));
    }

    @Test
    void testCheckNumber() {
        assertTrue(ParamType.isNumberValue(ParamType.LOCAL_DELIVERY_REGION.getId()));
        assertFalse(ParamType.isNumberValue(ParamType.PHONE_NUMBER.getId()));
    }
}
