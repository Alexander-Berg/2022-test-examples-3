package ru.yandex.market.fintech.fintechutils.service.commonproperty;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.fintech.fintechutils.AbstractFunctionalTest;
import ru.yandex.market.fintech.fintechutils.service.commonproperty.dao.CommonPropertyDao;

import static org.junit.jupiter.api.Assertions.*;

class CommonPropertyServiceImplTest extends AbstractFunctionalTest {

    @Autowired
    private CommonPropertyDao dao;

    private CommonPropertyService service;

    @BeforeEach
    void setUp() {
        service = new CommonPropertyServiceImpl(dao);
    }

    @Test
    @DbUnitDataSet(after = "CommonPropertyServiceTest.insertOrUpdateProperty.after.csv")
    void insertOrUpdateProperty() {
        service.insertOrUpdateProperty("prop_1", "val1");
        service.insertOrUpdateProperty("prop_2", "val2");
        service.insertOrUpdateProperty("prop_2", "val10");
    }

    @Test
    @DbUnitDataSet(before = "CommonPropertyServiceTest.selectPropertyValueByName.before.csv")
    void selectPropertyValueByName() {
        assertEquals("val_1", service.selectPropertyValueByName("prop_1").get());
        assertEquals("true", service.selectPropertyValueByName("bool_prop").get());
        assertTrue(service.selectPropertyValueByName("non_existent").isEmpty());
    }


    @Test
    @DbUnitDataSet(before = "CommonPropertyServiceTest.selectPropertyValueByName.before.csv")
    void selectBooleanPropertyValueByName() {
        assertTrue(service.selectBooleanPropertyValueByName("bool_prop").get());
        assertTrue(service.selectBooleanPropertyValueByName("dsdas").isEmpty());
    }

    @Test
    @DbUnitDataSet(before = "CommonPropertyServiceTest.selectPropertyValueByName.before.csv")
    void selectBooleanPropertyValueByNameOrDefault() {
        assertTrue(service.selectBooleanPropertyValueByNameOrDefault("bool_prop", false));
        assertFalse(service.selectBooleanPropertyValueByNameOrDefault("nonexistent", false));
        assertTrue(service.selectBooleanPropertyValueByNameOrDefault("nonexistent", true));
    }
}
