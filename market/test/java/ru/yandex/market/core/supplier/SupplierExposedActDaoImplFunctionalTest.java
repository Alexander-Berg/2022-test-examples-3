package ru.yandex.market.core.supplier;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.billing.matchers.SupplierAccountMatcher;
import ru.yandex.market.core.billing.matchers.SupplierExposedActMatcher;
import ru.yandex.market.core.supplier.dao.SupplierExposedActDaoImpl;
import ru.yandex.market.core.supplier.model.ProductId;
import ru.yandex.market.core.supplier.model.SupplierAccount;
import ru.yandex.market.core.supplier.model.SupplierExposedAct;
import ru.yandex.market.core.supplier.model.SupplierExposedActStatus;
import ru.yandex.market.mbi.util.MbiMatchers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SupplierExposedActDaoImplFunctionalTest extends FunctionalTest {
    private static final long IMPROBABILITY_SUPPLIER_ID = 7539514568L;
    private static final long IMPROBABILITY_SUPPLIER_ID_2 = 7539514567L;

    @Autowired
    NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    @Autowired
    SupplierExposedActDaoImpl supplierExposedActDao;

    @Test
    void mergeActsAndAccounts() {
        prepareDB();

        var acts = List.of(
                //Обновляем уже существующий акт
                new SupplierExposedAct.Builder()
                        .setSupplierId(IMPROBABILITY_SUPPLIER_ID)
                        .setContractId(1L)
                        .setContractEid("1")
                        .setContractType(PartnerContractType.INCOME)
                        .setActId(1L)
                        .setActDate(LocalDate.of(2019, 8, 12))
                        .setDeadlineDate(LocalDate.of(2019, 10, 15))
                        .setStatus(SupplierExposedActStatus.PENDING)
                        .setAccountId(1L)
                        .setExternalId("1")
                        .setPaidAmtRur(BigDecimal.ONE)
                        .setProductId(ProductId.REWARD)
                        .build(),

                //добавляем новый акт
                new SupplierExposedAct.Builder()
                        .setSupplierId(IMPROBABILITY_SUPPLIER_ID)
                        .setContractId(3L)
                        .setContractEid("3")
                        .setContractType(PartnerContractType.INCOME)
                        .setActId(3L)
                        .setActDate(LocalDate.of(2019, 10, 11))
                        .setDeadlineDate(LocalDate.of(2019, 11, 15))
                        .setStatus(SupplierExposedActStatus.PENDING)
                        .setAccountId(3L)
                        .setExternalId("3")
                        .setPaidAmtRur(BigDecimal.ONE)
                        .setProductId(ProductId.REWARD)
                        .build(),

                //добавляем новый акт
                new SupplierExposedAct.Builder()
                        .setSupplierId(IMPROBABILITY_SUPPLIER_ID_2)
                        .setContractId(3L)
                        .setContractEid("3")
                        .setContractType(PartnerContractType.INCOME)
                        .setActId(3L)
                        .setActDate(LocalDate.of(2019, 10, 11))
                        .setDeadlineDate(LocalDate.of(2019, 11, 15))
                        .setStatus(SupplierExposedActStatus.PENDING)
                        .setAccountId(4L)
                        .setExternalId("3")
                        .setPaidAmtRur(BigDecimal.ONE)
                        .setProductId(ProductId.REWARD)
                        .build()
        );

        var accounts = List.of(
                //Обновляем уже сущестсующий лицевой счет
                SupplierAccount.newBuilder()
                        .setId(1L)
                        .setSupplierId(IMPROBABILITY_SUPPLIER_ID)
                        .setAccountEid("ЛСМ-1817812094-1")
                        .setTotalActSum(BigDecimal.ONE)
                        .setReceiptSum(BigDecimal.ZERO)
                        .setPaymentDate(LocalDate.of(2019, 8, 12))
                        .build(),

                //добавляем новый лицевой счет
                SupplierAccount.newBuilder()
                        .setId(3L)
                        .setSupplierId(IMPROBABILITY_SUPPLIER_ID)
                        .setAccountEid("ЛСМ-1833052375-1")
                        .setTotalActSum(BigDecimal.ONE)
                        .setReceiptSum(BigDecimal.ZERO)
                        .setPaymentDate(LocalDate.of(2019, 10, 11))
                        .build(),

                //добавляем новый лицевой счет
                SupplierAccount.newBuilder()
                        .setId(4L)
                        .setSupplierId(IMPROBABILITY_SUPPLIER_ID_2)
                        .setAccountEid("ЛСМ-1833052375-2")
                        .setTotalActSum(BigDecimal.ONE)
                        .setReceiptSum(BigDecimal.ZERO)
                        .setPaymentDate(LocalDate.of(2019, 10, 11))
                        .build()
        );

        supplierExposedActDao.mergeAccounts(accounts);
        supplierExposedActDao.mergeActs(acts);

        var partnersLastIncomeActsWithAccounts =
                supplierExposedActDao.getPartnersLastIncomeActsWithAccounts(
                        List.of(IMPROBABILITY_SUPPLIER_ID,
                                IMPROBABILITY_SUPPLIER_ID_2));
        assertThat(partnersLastIncomeActsWithAccounts.get(IMPROBABILITY_SUPPLIER_ID).keySet(), hasSize(3));
        assertThat(partnersLastIncomeActsWithAccounts.get(IMPROBABILITY_SUPPLIER_ID_2).keySet(), hasSize(1));

        var actsFromDB = supplierExposedActDao.getActs(IMPROBABILITY_SUPPLIER_ID,
                PartnerContractType.INCOME);
        assertThat(actsFromDB, hasSize(3));
        assertThat(
                actsFromDB,
                Matchers.containsInAnyOrder(
                        MbiMatchers.<SupplierExposedAct>newAllOfBuilder()
                                .add(allOf(
                                        SupplierExposedActMatcher.hasSupplierId(IMPROBABILITY_SUPPLIER_ID),
                                        SupplierExposedActMatcher.hasContractId(1L),
                                        SupplierExposedActMatcher.hasContractType(PartnerContractType.INCOME),
                                        SupplierExposedActMatcher.hasActId(1L),
                                        SupplierExposedActMatcher.hasActDate(LocalDate.parse("2019-08-12")),
                                        SupplierExposedActMatcher.hasDeadlineDate(LocalDate.parse("2019-10-15")),
                                        SupplierExposedActMatcher.hasStatus(SupplierExposedActStatus.PENDING),
                                        SupplierExposedActMatcher.hasContractEid("1"),
                                        SupplierExposedActMatcher.hasAccountId(1L),
                                        SupplierExposedActMatcher.hasPaidAmtRur(BigDecimal.ONE)
                                )).build(),
                        MbiMatchers.<SupplierExposedAct>newAllOfBuilder()
                                .add(allOf(
                                        SupplierExposedActMatcher.hasSupplierId(IMPROBABILITY_SUPPLIER_ID),
                                        SupplierExposedActMatcher.hasContractId(2L),
                                        SupplierExposedActMatcher.hasContractType(PartnerContractType.INCOME),
                                        SupplierExposedActMatcher.hasActId(2L),
                                        SupplierExposedActMatcher.hasActDate(LocalDate.parse("2020-01-10")),
                                        SupplierExposedActMatcher.hasDeadlineDate(LocalDate.parse("2020-02-15")),
                                        SupplierExposedActMatcher.hasStatus(SupplierExposedActStatus.PENDING),
                                        SupplierExposedActMatcher.hasContractEid("2"),
                                        SupplierExposedActMatcher.hasAccountId(2L),
                                        SupplierExposedActMatcher.hasPaidAmtRur(BigDecimal.ZERO)
                                )).build(),
                        MbiMatchers.<SupplierExposedAct>newAllOfBuilder()
                                .add(allOf(
                                        SupplierExposedActMatcher.hasSupplierId(IMPROBABILITY_SUPPLIER_ID),
                                        SupplierExposedActMatcher.hasContractId(3L),
                                        SupplierExposedActMatcher.hasContractType(PartnerContractType.INCOME),
                                        SupplierExposedActMatcher.hasActId(3L),
                                        SupplierExposedActMatcher.hasActDate(LocalDate.parse("2019-10-11")),
                                        SupplierExposedActMatcher.hasDeadlineDate(LocalDate.parse("2019-11-15")),
                                        SupplierExposedActMatcher.hasStatus(SupplierExposedActStatus.PENDING),
                                        SupplierExposedActMatcher.hasContractEid("3"),
                                        SupplierExposedActMatcher.hasAccountId(3L),
                                        SupplierExposedActMatcher.hasPaidAmtRur(BigDecimal.ONE)
                                )).build()
                ));
        assertTrue(actsFromDB.containsAll(partnersLastIncomeActsWithAccounts.get(IMPROBABILITY_SUPPLIER_ID).keySet()));

        actsFromDB = supplierExposedActDao.getActs(IMPROBABILITY_SUPPLIER_ID_2, PartnerContractType.INCOME);
        assertThat(actsFromDB, hasSize(1));
        assertTrue(actsFromDB.containsAll(
                partnersLastIncomeActsWithAccounts.get(IMPROBABILITY_SUPPLIER_ID_2).keySet()));
        assertEquals(3, actsFromDB.get(0).getActId());

        var accountsFromDB = namedParameterJdbcTemplate.query(
                "select * from market_billing.supplier_account where supplier_id = :supplierId",
                new MapSqlParameterSource("supplierId", IMPROBABILITY_SUPPLIER_ID),
                (rs, rowNum) -> SupplierAccount.newBuilder()
                        .setId(rs.getLong("id"))
                        .setSupplierId(rs.getLong("supplier_id"))
                        .setAccountEid(rs.getString("account_eid"))
                        .setTotalActSum(rs.getBigDecimal("total_act_sum"))
                        .setReceiptSum(rs.getBigDecimal("receipt_sum"))
                        .build()
        );
        assertThat(accountsFromDB, hasSize(3));
        assertThat(
                accountsFromDB,
                Matchers.containsInAnyOrder(
                        MbiMatchers.<SupplierAccount>newAllOfBuilder().add(allOf(
                                SupplierAccountMatcher.hasId(1),
                                SupplierAccountMatcher.hasSupplierId(IMPROBABILITY_SUPPLIER_ID),
                                SupplierAccountMatcher.hasAccountEid("ЛСМ-1817812094-1"),
                                SupplierAccountMatcher.hasTotalActSum(BigDecimal.ONE),
                                SupplierAccountMatcher.hasReceiptSum(BigDecimal.ZERO)
                        )).build(),
                        MbiMatchers.<SupplierAccount>newAllOfBuilder().add(allOf(
                                SupplierAccountMatcher.hasId(2),
                                SupplierAccountMatcher.hasSupplierId(IMPROBABILITY_SUPPLIER_ID),
                                SupplierAccountMatcher.hasAccountEid("ЛСМ-1830941648-1"),
                                SupplierAccountMatcher.hasTotalActSum(BigDecimal.ZERO),
                                SupplierAccountMatcher.hasReceiptSum(BigDecimal.ZERO)
                        )).build(),
                        MbiMatchers.<SupplierAccount>newAllOfBuilder().add(allOf(
                                SupplierAccountMatcher.hasId(3),
                                SupplierAccountMatcher.hasSupplierId(IMPROBABILITY_SUPPLIER_ID),
                                SupplierAccountMatcher.hasAccountEid("ЛСМ-1833052375-1"),
                                SupplierAccountMatcher.hasTotalActSum(BigDecimal.ONE),
                                SupplierAccountMatcher.hasReceiptSum(BigDecimal.ZERO)
                        )).build()
                )
        );
        assertTrue(accountsFromDB.containsAll(
                partnersLastIncomeActsWithAccounts.get(IMPROBABILITY_SUPPLIER_ID).values()));
    }

    private void prepareDB() {
        namedParameterJdbcTemplate.update(
                "insert into shops_web.partner (id, type) values (:id, 'SUPPLIER')",
                new MapSqlParameterSource("id", IMPROBABILITY_SUPPLIER_ID));
        namedParameterJdbcTemplate.update(
                "insert into shops_web.partner (id, type) values (:id, 'SUPPLIER')",
                new MapSqlParameterSource("id", IMPROBABILITY_SUPPLIER_ID_2));

        namedParameterJdbcTemplate.update(
                "insert into shops_web.supplier (id, campaign_id, name, created_at, type, has_mapping) " +
                        "values (:id, :campId, 'supplier_exposed_act_test', :now, 3, 1)",
                new MapSqlParameterSource("id", IMPROBABILITY_SUPPLIER_ID)
                        .addValue("campId", IMPROBABILITY_SUPPLIER_ID)
                        .addValue("now", Timestamp.from(Instant.now())));
        namedParameterJdbcTemplate.update(
                "insert into shops_web.supplier (id, campaign_id, name, created_at, type, has_mapping) " +
                        "values (:id, :campId, 'supplier_exposed_act_test', :now, 3, 1)",
                new MapSqlParameterSource("id", IMPROBABILITY_SUPPLIER_ID_2)
                        .addValue("campId", IMPROBABILITY_SUPPLIER_ID_2)
                        .addValue("now", Timestamp.from(Instant.now())));

        insertAccounts(predefinedTestingAccounts());
        insertActs(predefinedTestingActs());
    }

    private void insertActs(Iterable<SupplierExposedAct> acts) {
        acts.forEach(act -> namedParameterJdbcTemplate.update("" +
                                "insert into market_billing.supplier_exposed_act " +
                                "(supplier_id, contract_id, contract_eid, contract_type, act_id, act_date, " +
                                "period_from, period_to, deadline_date, status, " +
                                "external_id, account_id, notification_sent, product_id, paid_amt_rur) " +
                                "values (:supplierId, :contractId, :contractEid, :contractType, :actId, :actDate, " +
                                ":periodFrom, :periodTo, :deadlineDate, :status, " +
                                ":externalId, :accountId, :notificationSent, :productId, :paidAmtRur)",
                        new MapSqlParameterSource("supplierId", act.getSupplierId())
                                .addValue("contractId", act.getContractId())
                                .addValue("contractEid", act.getContractEid())
                                .addValue("contractType", act.getContractType().getId())
                                .addValue("actId", act.getActId())
                                .addValue("actDate", Date.valueOf(act.getActDate()))
                                .addValue("periodFrom", act.getPeriodFrom())
                                .addValue("periodTo", act.getPeriodTo())
                                .addValue("deadlineDate", Date.valueOf(act.getDeadlineDate()))
                                .addValue("status", act.getStatus().getId())
                                .addValue("accountId", act.getAccountId())
                                .addValue("externalId", act.getExternalId())
                                .addValue("notificationSent", act.isNotificationSent())
                                .addValue("productId", act.getProductId().getId())
                                .addValue("paidAmtRur", act.getPaidAmtRur())
                )
        );
    }

    private void insertAccounts(Iterable<SupplierAccount> accounts) {
        accounts.forEach(account -> namedParameterJdbcTemplate.update("" +
                        "insert into market_billing.supplier_account\n" +
                        "(id, supplier_id, account_eid, total_act_sum, receipt_sum)\n" +
                        "values (:id, :supplierId, :accountEid, :totalActSum, :receiptSum)",
                new MapSqlParameterSource()
                        .addValue("id", account.getId())
                        .addValue("supplierId", account.getSupplierId())
                        .addValue("accountEid", account.getAccountEid())
                        .addValue("totalActSum", account.getTotalActSum())
                        .addValue("receiptSum", account.getReceiptSum())
        ));
    }

    private static Collection<SupplierExposedAct> predefinedTestingActs() {
        return List.of(
                new SupplierExposedAct.Builder()
                        .setSupplierId(IMPROBABILITY_SUPPLIER_ID)
                        .setContractId(1L)
                        .setContractEid("1")
                        .setContractType(PartnerContractType.INCOME)
                        .setActId(1L)
                        .setActDate(LocalDate.of(2019, 8, 31))
                        .setDeadlineDate(LocalDate.of(2019, 9, 15))
                        .setStatus(SupplierExposedActStatus.PENDING)
                        .setExternalId("1")
                        .setAccountId(1L)
                        .setPaidAmtRur(BigDecimal.ZERO)
                        .setProductId(ProductId.REWARD)
                        .build(),
                new SupplierExposedAct.Builder()
                        .setSupplierId(IMPROBABILITY_SUPPLIER_ID)
                        .setContractId(2L)
                        .setContractEid("2")
                        .setContractType(PartnerContractType.INCOME)
                        .setActId(2L)
                        .setActDate(LocalDate.of(2020, 1, 10))
                        .setDeadlineDate(LocalDate.of(2020, 2, 15))
                        .setStatus(SupplierExposedActStatus.PENDING)
                        .setExternalId("2")
                        .setAccountId(2L)
                        .setPaidAmtRur(BigDecimal.ZERO)
                        .setProductId(ProductId.REWARD)
                        .build()
        );
    }

    private static Collection<SupplierAccount> predefinedTestingAccounts() {
        return List.of(
                SupplierAccount.newBuilder()
                        .setId(1)
                        .setSupplierId(IMPROBABILITY_SUPPLIER_ID)
                        .setAccountEid("ЛСМ-1817812094-1")
                        .setTotalActSum(BigDecimal.ZERO)
                        .setReceiptSum(BigDecimal.ZERO)
                        .build(),
                SupplierAccount.newBuilder()
                        .setId(2)
                        .setSupplierId(IMPROBABILITY_SUPPLIER_ID)
                        .setAccountEid("ЛСМ-1830941648-1")
                        .setTotalActSum(BigDecimal.ZERO)
                        .setReceiptSum(BigDecimal.ZERO)
                        .build()
        );
    }
}
