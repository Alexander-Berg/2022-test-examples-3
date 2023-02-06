package ru.yandex.market.fintech.fintechutils.service.catboost.dao;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.fintech.fintechutils.AbstractFunctionalTest;
import ru.yandex.market.fintech.fintechutils.service.catboost.model.UserFeatures;

import static org.junit.jupiter.api.Assertions.assertTrue;

class UserFeaturesDaoTest extends AbstractFunctionalTest {

    @Autowired
    private UserFeaturesDao dao;

    @Test
    @DbUnitDataSet(before = "UserFeaturesDaoTest.before.csv")
    void testGetFeaturesByUid() {
        Optional<UserFeatures> optional = dao.getUserFeaturesByUid("asdfdsfa");
        assertTrue(optional.isEmpty());

        optional = dao.getUserFeaturesByUid("3a");
        assertTrue(optional.isPresent());

        UserFeatures features = optional.get();

    }

}
