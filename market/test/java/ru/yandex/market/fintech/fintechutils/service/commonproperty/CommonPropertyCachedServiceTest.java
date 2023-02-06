package ru.yandex.market.fintech.fintechutils.service.commonproperty;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.fintech.fintechutils.AbstractFunctionalTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommonPropertyCachedServiceTest extends AbstractFunctionalTest {

    @Autowired
    private CommonPropertyService commonPropertyService;

    // helper link
    private CommonPropertyCachedService cached;

    @BeforeEach
    void setUp() {

        cached = (CommonPropertyCachedService) commonPropertyService;
        cached.refreshCache();
    }


    @Test
    void testInsert() {

        assertTrue(commonPropertyService.selectPropertyValueByName("insertName").isEmpty());

        commonPropertyService.insertOrUpdateProperty("insertName", "insertValue");
        cached.refreshCache();

        Optional<String> val = commonPropertyService.selectPropertyValueByName("insertName");
        assertTrue(val.isPresent());
        assertEquals("insertValue", val.get());

        commonPropertyService.insertOrUpdateProperty("insertName", "insertValueUpdated");
        cached.refreshCache();
        assertEquals(
                "insertValueUpdated",
                commonPropertyService.selectPropertyValueByName("insertName").get());
    }

    @Test
    void testPropertyGet() {
        assertTrue(commonPropertyService.selectPropertyValueByName("booleanProp").isEmpty());
        assertTrue(commonPropertyService.selectPropertyValueByName("floatProp").isEmpty());

        assertFalse(commonPropertyService.selectBooleanPropertyValueByNameOrDefault("booleanProp", false));
        assertTrue(commonPropertyService.selectBooleanPropertyValueByNameOrDefault("booleanProp", true));
        assertEquals(0.944f, commonPropertyService.selectFloatPropertyValueByNameOrDefault("floatProp", 0.944f));

        assertTrue(commonPropertyService.selectPropertyValueByName("booleanProp").isEmpty());
        assertTrue(commonPropertyService.selectPropertyValueByName("floatProp").isEmpty());

        commonPropertyService.insertOrUpdateProperty("booleanProp", "true");
        commonPropertyService.insertOrUpdateProperty("floatProp", "0.564");
        cached.refreshCache();

        assertTrue(commonPropertyService.selectBooleanPropertyValueByName("booleanProp").get());
        assertEquals(0.564f, commonPropertyService.selectFloatPropertyValueByName("floatProp").get());


    }
}
