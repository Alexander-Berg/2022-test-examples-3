package ru.yandex.market.partner.security.checker;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.security.model.Authority;


public class NonServiceableAgencyCheckerTest extends FunctionalTest {

    @Autowired
    private NonServiceableAgencyChecker checker;

    @Test
    @DbUnitDataSet(before = "NonServiceableAgencyCheckerTest.csv")
    void notAgencyNotValidCampaignTest() {
        Assertions.assertTrue(checker.checkTyped(() -> 1, new Authority()));
        Assertions.assertFalse(checker.checkTyped(() -> 2, new Authority()));
        Assertions.assertTrue(checker.checkTyped(() -> 3, new Authority()));
    }
}
