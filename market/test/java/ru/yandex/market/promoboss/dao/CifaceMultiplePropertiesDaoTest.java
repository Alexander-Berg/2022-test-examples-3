package ru.yandex.market.promoboss.dao;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.promoboss.model.CifaceMultipleProperty;
import ru.yandex.market.promoboss.model.CifaceMultipleValue;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ContextConfiguration(classes = {
        CifaceMultiplePropertiesDao.class
})
class CifaceMultiplePropertiesDaoTest extends AbstractPromoTest {
    @Autowired
    private CifaceMultiplePropertiesDao multiplePropertiesDao;

    @Test
    void findEmptyList_ok() {
        List<CifaceMultipleValue> actual = multiplePropertiesDao.findByPromoId(PROMO_ID_1);

        assertNotNull(actual);
        assertTrue(actual.isEmpty());
    }

    @Test
    void findUnknownPromoId_ok() {
        List<CifaceMultipleValue> actual = multiplePropertiesDao.findByPromoId(-1L);

        assertNotNull(actual);
        assertTrue(actual.isEmpty());
    }

    @Test
    @DbUnitDataSet(
            before = "CifaceMultiplePropertiesDaoTest.findNotEmptyList_ok.before.csv"
    )
    void findNotEmptyList_ok() {
        List<CifaceMultipleValue> expected = List.of(
                CifaceMultipleValue.builder()
                        .property(CifaceMultipleProperty.CATEGORY_DEPARTMENT)
                        .stringValue("department1")
                        .build(),
                CifaceMultipleValue.builder()
                        .property(CifaceMultipleProperty.CATEGORY_DEPARTMENT)
                        .stringValue("department2")
                        .build(),
                CifaceMultipleValue.builder()
                        .property(CifaceMultipleProperty.CATEGORY_DEPARTMENT)
                        .stringValue("department3")
                        .build()
        );

        List<CifaceMultipleValue> actual = multiplePropertiesDao.findByPromoId(PROMO_ID_1);

        assertEquals(expected, actual);
    }

    @Test
    @DbUnitDataSet(
            after = "CifaceMultiplePropertiesDaoTest.insert_ok.after.csv"
    )
    void insert_ok() {
        List<CifaceMultipleValue> items = List.of(
                CifaceMultipleValue.builder()
                        .property(CifaceMultipleProperty.CATEGORY_DEPARTMENT)
                        .stringValue("department1")
                        .build(),
                CifaceMultipleValue.builder()
                        .property(CifaceMultipleProperty.CATEGORY_DEPARTMENT)
                        .stringValue("department2")
                        .build(),
                CifaceMultipleValue.builder()
                        .property(CifaceMultipleProperty.CATEGORY_DEPARTMENT)
                        .stringValue("department3")
                        .build()
        );

        for (CifaceMultipleValue item : items) {
            assertDoesNotThrow(() -> multiplePropertiesDao.insert(PROMO_ID_1, item));
        }
    }

    @Test
    @DbUnitDataSet(
            before = "CifaceMultiplePropertiesDaoTest.delete_ok.before.csv",
            after = "CifaceMultiplePropertiesDaoTest.delete_ok.after.csv"
    )
    void delete_ok() {
        assertDoesNotThrow(() -> multiplePropertiesDao.deleteAll(List.of(1L, 3L)));
    }

    @Test
    @DbUnitDataSet(
            before = "CifaceMultiplePropertiesDaoTest.deleteNotFound_throws.before.csv"
    )
    void deleteNotFound_throws() {
        RuntimeException exception = assertThrows(RuntimeException.class, () -> multiplePropertiesDao.deleteAll(List.of(123L, 124L)));
        assertEquals("Can't delete rows by ids: [123, 124]", exception.getMessage());
    }
}
