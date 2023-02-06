package ru.yandex.market.loyalty.admin.audit;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.loyalty.admin.test.MarketLoyaltyAdminMockedDbTest;
import ru.yandex.market.loyalty.api.model.CouponCreationRequest;
import ru.yandex.market.loyalty.api.model.MarketLoyaltyErrorCode;
import ru.yandex.market.loyalty.api.model.PromoStatus;
import ru.yandex.market.loyalty.core.dao.accounting.AccountDao;
import ru.yandex.market.loyalty.core.dao.coin.BunchGenerationRequestParamName;
import ru.yandex.market.loyalty.core.dao.coin.GeneratorType;
import ru.yandex.market.loyalty.core.model.GenericParam;
import ru.yandex.market.loyalty.core.model.accounting.AccountMatter;
import ru.yandex.market.loyalty.core.model.accounting.AccountType;
import ru.yandex.market.loyalty.core.model.coin.BunchGenerationRequest;
import ru.yandex.market.loyalty.core.model.coupon.Coupon;
import ru.yandex.market.loyalty.core.model.promo.BudgetMode;
import ru.yandex.market.loyalty.core.model.promo.Promo;
import ru.yandex.market.loyalty.core.model.trigger.TriggerGroupType;
import ru.yandex.market.loyalty.core.rule.ParamsContainer;
import ru.yandex.market.loyalty.core.service.BudgetService;
import ru.yandex.market.loyalty.core.service.BunchGenerationService;
import ru.yandex.market.loyalty.core.service.PromoManager;
import ru.yandex.market.loyalty.core.service.PromoService;
import ru.yandex.market.loyalty.core.service.coupon.CouponService;
import ru.yandex.market.loyalty.core.utils.CoreCollectionUtils;
import ru.yandex.market.loyalty.core.utils.DiscountUtils;
import ru.yandex.market.loyalty.core.utils.PromoUtils;
import ru.yandex.market.loyalty.core.utils.TriggersFactory;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static ru.yandex.market.loyalty.core.service.coupon.CouponCode.of;

public class AuditTriggerTest extends MarketLoyaltyAdminMockedDbTest {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PromoService promoService;

    @Autowired
    private AccountDao accountDao;

    @Autowired
    private BudgetService budgetService;

    @Autowired
    private CouponService couponService;

    @Autowired
    private TriggersFactory triggersFactory;
    @Autowired
    private PromoManager promoManager;
    @Autowired
    private DiscountUtils discountUtils;
    @Autowired
    private BunchGenerationService bunchGenerationService;

    @Test
    public void testConfigs() {
        List<String> auditTables = Arrays.asList(
                "account",
                "bunch_generation_request",
                "coupon",
                "coupon_params",
                "promo",
                "promo_params",
                "promo_region",
                "promo_rule",
                "promo_rule_params",
                "promo_trigger",
                "trigger_action",
                "trigger_restriction",
                "coin_props",
                "coin_rule",
                "coin_rule_params",
                "coin_description"
        );

        for (String auditTable : auditTables) {
            Set<String> columnsFromBaseTable = jdbcTemplate.queryForList(
                    "select column_name from information_schema.columns where table_name = '" + auditTable + '\''
            ).stream().map(row -> (String) row.get("column_name")).collect(Collectors.toSet());
            Set<String> columnsFromAuditTable = jdbcTemplate.queryForList(
                    "select column_name from information_schema.columns where table_name = '" + auditTable + "_audit'"
            ).stream().map(row -> (String) row.get("column_name")).collect(Collectors.toSet());

            assertThat(auditTable, CoreCollectionUtils.minus(columnsFromBaseTable, columnsFromAuditTable), empty());
            assertTrue(auditTable, columnsFromAuditTable.containsAll(Arrays.asList("op_type", "app_user", "op_time")));

            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                    "/*validator=false*/select event_manipulation, event_object_table, action_statement " +
                            "from information_schema.triggers where trigger_name = '" + auditTable + "_audit_trigger'"
            );
            assertThat(
                    auditTable,
                    rows.stream().map(row -> (String) row.get("event_manipulation")).collect(Collectors.toList()),
                    containsInAnyOrder("INSERT", "UPDATE", "DELETE")
            );
            assertThat(
                    auditTable,
                    rows.stream().map(row -> (String) row.get("event_object_table")).distinct().collect(
                            Collectors.toList()),
                    contains(auditTable)
            );
            assertThat(
                    auditTable,
                    rows.stream().map(row -> (String) row.get("action_statement")).distinct().collect(Collectors.toList()),
                    contains("EXECUTE FUNCTION if_modified_table_func('" + auditTable + "', '" + auditTable + "_audit" +
                            "')")
            );
        }
    }

    @Test
    public void testPromoAudit() {
        int prev1, prev2;

        // проверяем вставку в промо
        prev1 = countAuditTable("PROMO_AUDIT", "I");

        Promo promo = promoManager.createCouponPromo(PromoUtils.Coupon.defaultSingleUse());

        assertEquals("insert trigger broken", prev1 + 1, (long) jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " +
                "promo_audit WHERE name=? AND op_type='I'", Integer.class, promo.getName()));

        // проверяем обновление промо
        prev1 = countAuditTable("PROMO_AUDIT", "UB");
        prev2 = countAuditTable("PROMO_AUDIT", "UA");

        promoService.updateStatus(promo, PromoStatus.INACTIVE);

        assertEquals("update trigger broken", prev1 + 1, (long) countAuditTable("PROMO_AUDIT", "UB"));
        assertEquals("update trigger broken", prev2 + 1, (long) countAuditTable("PROMO_AUDIT", "UA"));
    }

    @Test
    public void testAccountAudit() {
        int prev1, prev2;
        prev1 = countAuditTable("ACCOUNT_AUDIT", "I");
        long id = accountDao.createAccount(AccountType.ACTIVE, AccountMatter.MONEY, null, false);
        assertEquals("trigger broken", prev1 + 1, (long) countAuditTable("ACCOUNT_AUDIT", "I"));


        prev1 = countAuditTable("ACCOUNT_AUDIT", "UB");
        prev2 = countAuditTable("ACCOUNT_AUDIT", "UA");
        budgetService.performSingleTransaction(
                BigDecimal.TEN, accountDao.getTechnicalAccountId(AccountMatter.MONEY), id, BudgetMode.SYNC,
                MarketLoyaltyErrorCode.OTHER_ERROR
        );
        assertEquals("trigger broken", prev1 + 2, (long) countAuditTable("ACCOUNT_AUDIT", "UB"));
        assertEquals("trigger broken", prev2 + 2, (long) countAuditTable("ACCOUNT_AUDIT", "UA"));
    }

    @Test
    public void testCouponAudit() {
        Promo promo = promoManager.createCouponPromo(PromoUtils.Coupon.defaultSingleUse());

        int prev1, prev2;
        prev1 = countAuditTable("COUPON_AUDIT", "I");
        Coupon coupon = couponService.createOrGetCoupon(
                CouponCreationRequest.builder("test", promo.getId()).build(),
                discountUtils.getRulesPayload()
        );
        assertEquals("trigger broken", prev1 + 1, (long) countAuditTable("COUPON_AUDIT", "I"));

        prev1 = countAuditTable("COUPON_AUDIT", "UB");
        prev2 = countAuditTable("COUPON_AUDIT", "UA");
        couponService.activateCoupon(of(coupon.getCode()), "test");
        assertEquals("trigger broken", prev1 + 1, (long) countAuditTable("COUPON_AUDIT", "UB"));
        assertEquals("trigger broken", prev2 + 1, (long) countAuditTable("COUPON_AUDIT", "UA"));
    }

    @Test
    public void testBunchGenerationRequestAudit() {
        final String tbName = "bunch_generation_request_audit";
        final Promo promo = promoManager.createAccrualPromo(PromoUtils.WalletAccrual.defaultModelAccrual());

        var paramsContainer = new ParamsContainer<BunchGenerationRequestParamName<?>>();

        ParamsContainer.addParam(
                paramsContainer,
                BunchGenerationRequestParamName.INPUT_TABLE,
                GenericParam.of("//tmp/input_table")
        );

        final Integer insertsCount = countAuditTable(tbName, "I");
        final Integer ubCount = countAuditTable(tbName, "UB");
        final Integer uaCount = countAuditTable(tbName, "UA");

        final String bunchKey = bunchGenerationService.saveRequestAsScheduledReturningBunchId(
                BunchGenerationRequest.builder()
                        .setKey("DEFAULT_REQUEST_KEY")
                        .setPromoId(promo.getId())
                        .setSource("Some source")
                        .setCount(100)
                        .setGeneratorType(GeneratorType.YANDEX_WALLET)
                        .setEmail("email")
                        .setParamsContainer(paramsContainer)
                        .build()
        );

        assertThat(countAuditTable(tbName, "I"), equalTo(insertsCount + 1));

        bunchGenerationService.cancelRequest(bunchKey);

        assertThat(countAuditTable(tbName, "UB"), equalTo(ubCount + 1));
        assertThat(countAuditTable(tbName, "UA"), equalTo(uaCount + 1));

    }

    @Test
    public void testTriggerAudit() {
        Promo promo = promoManager.createCouponPromo(PromoUtils.Coupon.defaultSingleUse());

        long prev1 = countAuditTable("PROMO_TRIGGER_AUDIT", "I");
        long prev2 = countAuditTable("TRIGGER_ACTION_AUDIT", "I");
        long prev3 = countAuditTable("TRIGGER_RESTRICTION_AUDIT", "I");

        triggersFactory.createLoginTrigger(promo, 123L, TriggerGroupType.MANDATORY_TRIGGERS);

        assertEquals("trigger broken", prev1 + 1, (long) countAuditTable("PROMO_TRIGGER_AUDIT", "I"));
        assertEquals("trigger broken", prev2 + 1, (long) countAuditTable("TRIGGER_ACTION_AUDIT", "I"));
        assertEquals("trigger broken", prev3 + 1, (long) countAuditTable("TRIGGER_RESTRICTION_AUDIT", "I"));

    }

    private Integer countAuditTable(String tbName, String opType) {
        return jdbcTemplate.queryForObject(
                "SELECT count(*) FROM " + tbName + " WHERE op_type=?", Integer.class, opType);
    }
}
