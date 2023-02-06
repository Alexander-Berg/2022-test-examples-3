package ru.yandex.market.supplier.act.exposed;

import java.time.LocalDate;
import java.util.List;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.billing.matchers.SupplierExposedActMatcher;
import ru.yandex.market.core.supplier.PartnerContractType;
import ru.yandex.market.core.supplier.model.ProductId;
import ru.yandex.market.core.supplier.model.SupplierExposedAct;
import ru.yandex.market.core.supplier.model.SupplierExposedActStatus;
import ru.yandex.market.mbi.util.MbiMatchers;
import ru.yandex.market.shop.FunctionalTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;

/**
 * Тесты для {@link OebsMetabaseDao}
 */
class OebsMetabaseDaoTest extends FunctionalTest {

    @Autowired
    private OebsMetabaseDao oebsMetabaseDao;

    @Test
    @DbUnitDataSet(before = "OebsMetabaseDaoTest.before.csv")
    void testGetActs() {
        List<SupplierExposedAct> acts = oebsMetabaseDao.getOutcomeActs();
        Assertions.assertEquals(4, acts.size());

        assertThat(
                acts,
                Matchers.containsInAnyOrder(
                        MbiMatchers.<SupplierExposedAct>newAllOfBuilder()
                                .add(allOf(
                                        SupplierExposedActMatcher.hasSupplierId(1L),
                                        SupplierExposedActMatcher.hasContractId(1L),
                                        SupplierExposedActMatcher.hasContractEid("1"),
                                        SupplierExposedActMatcher.hasContractType(PartnerContractType.OUTCOME),
                                        SupplierExposedActMatcher.hasActId(1L),
                                        SupplierExposedActMatcher.hasActDate(LocalDate.parse("2019-02-01")),
                                        SupplierExposedActMatcher.hasDeadlineDate(LocalDate.parse("2019-03-01")),
                                        SupplierExposedActMatcher.hasStatus(SupplierExposedActStatus.CLOSED),
                                        SupplierExposedActMatcher.hasProductId(ProductId.OUTCOME_ACT_PRODUCT)
                                )).build(),
                        MbiMatchers.<SupplierExposedAct>newAllOfBuilder()
                                .add(allOf(
                                        SupplierExposedActMatcher.hasSupplierId(1L),
                                        SupplierExposedActMatcher.hasContractId(1L),
                                        SupplierExposedActMatcher.hasContractEid("1"),
                                        SupplierExposedActMatcher.hasContractType(PartnerContractType.OUTCOME),
                                        SupplierExposedActMatcher.hasActId(2L),
                                        SupplierExposedActMatcher.hasActDate(LocalDate.parse("2019-03-01")),
                                        SupplierExposedActMatcher.hasDeadlineDate(LocalDate.parse("2019-04-01")),
                                        SupplierExposedActMatcher.hasStatus(SupplierExposedActStatus.PENDING),
                                        SupplierExposedActMatcher.hasProductId(ProductId.OUTCOME_ACT_PRODUCT)
                                )).build(),
                        MbiMatchers.<SupplierExposedAct>newAllOfBuilder()
                                .add(allOf(
                                        SupplierExposedActMatcher.hasSupplierId(2L),
                                        SupplierExposedActMatcher.hasContractId(1L),
                                        SupplierExposedActMatcher.hasContractEid("1"),
                                        SupplierExposedActMatcher.hasContractType(PartnerContractType.OUTCOME),
                                        SupplierExposedActMatcher.hasActId(1L),
                                        SupplierExposedActMatcher.hasActDate(LocalDate.parse("2019-02-01")),
                                        SupplierExposedActMatcher.hasDeadlineDate(LocalDate.parse("2019-03-01")),
                                        SupplierExposedActMatcher.hasStatus(SupplierExposedActStatus.CLOSED),
                                        SupplierExposedActMatcher.hasProductId(ProductId.OUTCOME_ACT_PRODUCT)
                                )).build(),
                        MbiMatchers.<SupplierExposedAct>newAllOfBuilder()
                                .add(allOf(
                                        SupplierExposedActMatcher.hasSupplierId(2L),
                                        SupplierExposedActMatcher.hasContractId(1L),
                                        SupplierExposedActMatcher.hasContractEid("1"),
                                        SupplierExposedActMatcher.hasContractType(PartnerContractType.OUTCOME),
                                        SupplierExposedActMatcher.hasActId(2L),
                                        SupplierExposedActMatcher.hasActDate(LocalDate.parse("2019-03-01")),
                                        SupplierExposedActMatcher.hasDeadlineDate(LocalDate.parse("2019-04-01")),
                                        SupplierExposedActMatcher.hasStatus(SupplierExposedActStatus.PENDING),
                                        SupplierExposedActMatcher.hasProductId(ProductId.OUTCOME_ACT_PRODUCT)
                                )).build()
                ));
    }
}
