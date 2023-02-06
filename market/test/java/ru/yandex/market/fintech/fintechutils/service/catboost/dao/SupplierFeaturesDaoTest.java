package ru.yandex.market.fintech.fintechutils.service.catboost.dao;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.fintech.fintechutils.AbstractFunctionalTest;
import ru.yandex.market.fintech.fintechutils.service.catboost.model.SupplierFeatures;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SupplierFeaturesDaoTest extends AbstractFunctionalTest {

    @Autowired
    private SupplierFeaturesDao dao;

    @Test
    @DbUnitDataSet(before = "SupplierFeaturesDaoTest.before.csv")
    void testGetSupplierFeatures() {
        Optional<SupplierFeatures> optional = dao.getSupplierFeaturesBySupplierId(1000);
        assertTrue(optional.isEmpty());

        optional = dao.getSupplierFeaturesBySupplierId(3);
        assertTrue(optional.isPresent());

        SupplierFeatures features = optional.get();
        assertEquals(3, features.getSupplierId());
        assertEquals(0.1, features.getSupplierBadCancelGmvRatioPostpaid());
        assertEquals(0.1, features.getSupplierBadCancelGmvRatioPrepaid());
        assertEquals(0.1, features.getSupplierBadCancelOrdersRatioPostpaid());
        assertEquals(0.1, features.getSupplierBadCancelOrdersRatioPrepaid());
        assertEquals(0.1, features.getSupplierOrderCountPostpaid());
        assertEquals(0.1, features.getSupplierOrderCountPrepaid());
        assertEquals(0.3, features.getDaysChkpt130());
    }
}
