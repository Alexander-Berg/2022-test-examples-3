package ru.yandex.market.loyalty.admin.yt.service;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableList;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.common.util.date.DateUtil;
import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.inside.yt.kosher.common.GUID;
import ru.yandex.inside.yt.kosher.tables.YTableEntryTypes;
import ru.yandex.market.loyalty.admin.test.MarketLoyaltyAdminMockedDbTest;
import ru.yandex.market.loyalty.admin.yt.dao.PromocodeCoinYtDao;
import ru.yandex.market.loyalty.admin.yt.model.FilteredPromoForYt;
import ru.yandex.market.loyalty.api.model.PromoStatus;
import ru.yandex.market.loyalty.core.config.YtHahn;
import ru.yandex.market.loyalty.core.dao.accounting.MetaTransactionDao;
import ru.yandex.market.loyalty.core.dao.accounting.OperationContextDao;
import ru.yandex.market.loyalty.core.model.promo.Promo;
import ru.yandex.market.loyalty.core.model.promo.PromoBuilder;
import ru.yandex.market.loyalty.core.model.promo.PromoParameterName;
import ru.yandex.market.loyalty.core.model.promo.RuleParameterName;
import ru.yandex.market.loyalty.core.rule.RuleContainer;
import ru.yandex.market.loyalty.core.rule.RuleType;
import ru.yandex.market.loyalty.core.service.BudgetService;
import ru.yandex.market.loyalty.core.service.ConfigurationService;
import ru.yandex.market.loyalty.core.service.PromoManager;
import ru.yandex.market.loyalty.core.service.PromoService;
import ru.yandex.market.loyalty.core.utils.PromoUtils;
import ru.yandex.market.loyalty.monitoring.PushMonitor;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;
import static ru.yandex.market.loyalty.admin.monitoring.AdminMonitorType.DUPLICATED_PROMOCODE_TO_YT;
import static ru.yandex.market.loyalty.admin.utils.FakePromoUtils.prepareFakePromocodePromoBuilder;
import static ru.yandex.market.loyalty.admin.utils.YtTestHelper.ytPathToName;
import static ru.yandex.market.loyalty.core.model.promo.PromoSubType.PROMOCODE;
import static ru.yandex.market.loyalty.core.utils.PromoUtils.SHOP_PROMO_ID;

@ActiveProfiles({"monitor-mock-test"})
public class PromocodeCoinPromoYtExporterTest extends MarketLoyaltyAdminMockedDbTest {
    @Autowired
    @YtHahn
    private Yt yt;
    @Autowired
    @YtHahn
    private PromocodeCoinPromoYtExporter promocodeCoinPromoYtExporter;
    @Autowired
    protected TransactionTemplate transactionTemplate;
    @Autowired
    protected MetaTransactionDao metaTransactionDao;
    @Autowired
    protected OperationContextDao operationContextDao;
    @Autowired
    protected BudgetService budgetService;
    @Autowired
    private PromoManager promoManager;
    @Autowired
    private PromoService promoService;
    @Autowired
    private PushMonitor pushMonitor;
    @Autowired
    private ConfigurationService configurationService;
    @Captor
    private ArgumentCaptor<Iterator<JsonNode>> argumentCaptor;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Before
    public void mockTransactionHandling() {
        when(yt.transactions().start(any(Optional.class), anyBoolean(), any(Duration.class), any(Map.class)))
                .thenReturn(GUID.valueOf("1-8e9c4f69-a3bc6964-3e99ab10"));
    }

    @Test
    public void shouldExportActivePromo() {
        Promo promo = promoManager.createSmartShoppingPromo(
                PromoUtils.SmartShopping.defaultFixedPromocode()
                        .addCoinRule(RuleType.SUPPLIER_FILTER_RULE, RuleParameterName.SUPPLIER_ID, Set.of(1234L))
                        .setActionCode("SAMSUNG500")
        );

        promocodeCoinPromoYtExporter.exportToYt();

        verifyHasPromo(promo);
        verifyHasNoFilteredPromo();
    }

    @Test
    public void shouldExportInactivePromo() {
        Promo promo = promoManager.createSmartShoppingPromo(
                PromoUtils.SmartShopping.defaultFixedPromocode()
                        .addCoinRule(RuleType.SUPPLIER_FILTER_RULE, RuleParameterName.SUPPLIER_ID, Set.of(1234L))
                        .setActionCode("SAMSUNG500")
                        .setStatus(PromoStatus.INACTIVE)
        );

        promocodeCoinPromoYtExporter.exportToYt();

        verifyHasPromo(promo);
        verifyHasNoFilteredPromo();
    }

    @Test
    public void shouldNotExportNotActiveAndNotInactivePromo() {
        promoManager.createSmartShoppingPromo(
                PromoUtils.SmartShopping.defaultFixedPromocode()
                        .addCoinRule(RuleType.SUPPLIER_FILTER_RULE, RuleParameterName.SUPPLIER_ID, Set.of(1234L))
                        .setActionCode("SAMSUNG500")
                        .setStatus(PromoStatus.PENDING)
        );

        promocodeCoinPromoYtExporter.exportToYt();

        verifyHasNoPromo();
        verifyHasNoFilteredPromo();
    }

    @Test
    public void shouldNotExportFakePromo() {
        Promo promo = promoManager.createFakeSmartShoppingPromo(
                PromoUtils.SmartShopping.defaultFixedPromocode()
                        .addCoinRule(RuleType.SUPPLIER_FILTER_RULE, RuleParameterName.SUPPLIER_ID, Set.of(1234L))
                        .addCoinRule(RuleType.FAKE_USER_CUTTING_RULE, RuleParameterName.APPLIED_TO_FAKE_USER,
                                Set.of(true))
                        .setAppliedToFakeUser(true)
                        .setActionCode("SAMSUNG500")
        );

        promocodeCoinPromoYtExporter.exportToYt();

        verifyHasNoPromo();
        verifyHasFilteredPromo(promo);
    }

    @Test
    public void shouldNotExportFakePromocode() {
        Promo promo = promoManager.createFakePromocodePromo(prepareFakePromocodePromoBuilder());

        promocodeCoinPromoYtExporter.exportToYt();

        verifyHasNoPromo();
        verifyHasFilteredPromo(promo);
    }

    @Test
    public void shouldNotExportGeneratedPromo() {
        Promo promo = promoManager.createSmartShoppingPromo(
                PromoUtils.SmartShopping.defaultFixedPromocode()
                        .addCoinRule(RuleType.SUPPLIER_FILTER_RULE, RuleParameterName.SUPPLIER_ID, Set.of(1234L))
                        .setActionCode("SAMSUNG500")
        );
        promoService.setPromoParam(promo.getId(), PromoParameterName.GENERATED_PROMOCODE, true);

        promocodeCoinPromoYtExporter.exportToYt();

        verifyHasNoPromo();
        verifyHasFilteredPromo(promo);
    }

    @Test
    public void shouldNotExportNotUploadedPromo() {
        Date startDate = Date.from(clock.instant());
        Date endDate = DateUtil.addDay(Date.from(clock.instant()), 1);
        Long supplierId = 1234L;

        Promo promo = promoManager.createSmartShoppingPromo(
                PromoUtils.SmartShopping.defaultFixedPromocode()
                        .setStartDate(startDate)
                        .setEndDate(endDate)
                        .addCoinRule(RuleType.SUPPLIER_FILTER_RULE, RuleParameterName.SUPPLIER_ID, Set.of(supplierId))
                        .setActionCode("SAMSUNG500")
        );
        promoService.setPromoParam(promo.getId(), PromoParameterName.DO_NOT_UPLOAD_TO_IDX, true);

        promocodeCoinPromoYtExporter.exportToYt();

        verifyHasNoPromo();
        verifyHasFilteredPromo(promo);
    }

    @Test
    public void shouldNotExportImportedPromo() {
        Promo promo = promoManager.createSmartShoppingPromo(
                PromoUtils.SmartShopping.defaultFixedPromocode()
                        .addCoinRule(RuleType.SUPPLIER_FILTER_RULE, RuleParameterName.SUPPLIER_ID, Set.of(1234L))
                        .setActionCode("SAMSUNG500")
        );
        promoService.setPromoParam(promo.getId(), PromoParameterName.IMPORTED, true);

        promocodeCoinPromoYtExporter.exportToYt();

        verifyHasNoPromo();
        verifyHasFilteredPromo(promo);
    }

    @Test
    public void shouldNotExportFirstOrderPromo() {
        Promo promo = promoManager.createSmartShoppingPromo(
                PromoUtils.SmartShopping.defaultFixedPromocode()
                        .addCoinRule(RuleType.SUPPLIER_FILTER_RULE, RuleParameterName.SUPPLIER_ID, Set.of(1234L))
                        .addCoinRule(RuleContainer.builder(RuleType.FIRST_ORDER_CUTTING_RULE))
                        .setActionCode("SAMSUNG500")
        );

        promocodeCoinPromoYtExporter.exportToYt();

        verifyHasNoPromo();
        verifyHasFilteredPromo(promo);
    }

    @Test
    public void shouldNotExportPromoWithoutAnyFilterRule() {
        Promo promo = promoManager.createSmartShoppingPromo(
                PromoUtils.SmartShopping.defaultFixedPromocode()
                        .setActionCode("SAMSUNG500")
        );

        promocodeCoinPromoYtExporter.exportToYt();

        verifyHasNoPromo();
        verifyHasFilteredPromo(promo);
    }

    @Test
    public void shouldFilterDuplicatePromocodesForIntersectingDate() throws Exception {
        List<Promo> promos = List.of(
                PromoBuilder.builder(PROMOCODE)
                        .setId(1L)
                        .setActionCode("THE_SAME_ACTION_CODE")
                        .setStartDate(getDate("2000-01-01 01-00-00"))
                        .setEndDate(getDate("2000-03-01 01-00-00"))
                        .basePromo(),
                PromoBuilder.builder(PROMOCODE)
                        .setId(2L)
                        .setActionCode("THE_SAME_ACTION_CODE")
                        .setStartDate(getDate("2000-02-01 01-00-00"))
                        .setEndDate(getDate("2000-04-01 01-00-00"))
                        .basePromo(),
                PromoBuilder.builder(PROMOCODE)
                        .setId(3L)
                        .setActionCode("THE_SAME_ACTION_CODE")
                        .setStartDate(getDate("2000-02-02 01-00-00"))
                        .setEndDate(getDate("2000-03-02 01-00-00"))
                        .basePromo());

        List<FilteredPromoForYt> filteredPromoForYts = new ArrayList<>();
        List<Promo> filteredPromos = promocodeCoinPromoYtExporter.filterDuplicatePromocodesWithOverlap(promos,
                filteredPromoForYts);
        assertThat(filteredPromos, hasSize(1));
        assertThat(filteredPromoForYts, hasSize(2));

        verify(pushMonitor).addTemporaryWarning(
                eq(DUPLICATED_PROMOCODE_TO_YT),
                eq("Promo (id 3, 2000-02-02 01:00:00 " +
                        "- 2000-03-02 01:00:00) with action code THE_SAME_ACTION_CODE was not loaded, it already " +
                        "exists " +
                        "duplicated promocode (id 2, 2000-02-01 01:00:00 - 2000-04-01 01:00:00) for the same period " +
                        "of time\n" +
                        "Promo (id 1, 2000-01-01 01:00:00 - 2000-03-01 01:00:00) with action code " +
                        "THE_SAME_ACTION_CODE was " +
                        "not loaded, it already exists duplicated promocode (id 2, 2000-02-01 01:00:00 - 2000-04-01 " +
                        "01:00:00)" + " for the same period of time\n"),
                eq(1L),
                eq(TimeUnit.HOURS));
    }

    @Test
    public void shouldFilterDuplicatePromocodesForIntersectingTime() throws Exception {
        List<Promo> promos = List.of(
                PromoBuilder.builder(PROMOCODE)
                        .setId(1L)
                        .setActionCode("THE_SAME_ACTION_CODE")
                        .setStartDate(getDate("2000-01-01 02-00-00"))
                        .setEndDate(getDate("2000-01-03 02-00-00"))
                        .basePromo(),
                PromoBuilder.builder(PROMOCODE)
                        .setId(2L)
                        .setActionCode("THE_SAME_ACTION_CODE")
                        .setStartDate(getDate("2000-01-03 01-00-00"))
                        .setEndDate(getDate("2000-01-04 01-00-00"))
                        .basePromo());

        List<FilteredPromoForYt> filteredPromoForYts = new ArrayList<>();
        List<Promo> filteredPromos = promocodeCoinPromoYtExporter.filterDuplicatePromocodesWithOverlap(promos,
                filteredPromoForYts);
        assertThat(filteredPromos, hasSize(1));
        assertThat(filteredPromoForYts, hasSize(1));

        verify(pushMonitor).addTemporaryWarning(
                eq(DUPLICATED_PROMOCODE_TO_YT),
                eq("Promo (id 1, 2000-01-01 02:00:00 - 2000-01-03 02:00:00) with action code " +
                        "THE_SAME_ACTION_CODE was not loaded, " +
                        "it already exists duplicated promocode (id 2, 2000-01-03 01:00:00 - 2000-01-04 01:00:00) for" +
                        " the same period of time\n"),
                eq(1L),
                eq(TimeUnit.HOURS));
    }

    @Test
    public void shouldNotFilterDuplicatePromocodesForNotIntersectingTime() throws Exception {
        List<Promo> promos = List.of(
                PromoBuilder.builder(PROMOCODE)
                        .setId(1L)
                        .setActionCode("THE_SAME_ACTION_CODE")
                        .setStartDate(getDate("2000-01-01 01-00-00"))
                        .setEndDate(getDate("2000-01-03 01-00-00"))
                        .basePromo(),
                PromoBuilder.builder(PROMOCODE)
                        .setId(2L)
                        .setActionCode("THE_SAME_ACTION_CODE")
                        .setStartDate(getDate("2000-01-03 01-00-01"))
                        .setEndDate(getDate("2000-01-04 01-00-00"))
                        .basePromo());

        List<FilteredPromoForYt> filteredPromoForYts = new ArrayList<>();
        List<Promo> filteredPromos = promocodeCoinPromoYtExporter.filterDuplicatePromocodesWithOverlap(promos,
                filteredPromoForYts);
        assertThat(filteredPromos, hasSize(2));
        assertThat(filteredPromoForYts, hasSize(0));

        verify(pushMonitor, never()).addTemporaryWarning(eq(DUPLICATED_PROMOCODE_TO_YT), any(), eq(1L), any());
    }

    @Test
    public void shouldFilterAllPromocodesIfExistsEmptyShopPromoId() {
        Promo promo = promoManager.createSmartShoppingPromo(
                PromoUtils.SmartShopping.defaultFixedPromocode()
                        .addCoinRule(RuleType.SUPPLIER_FILTER_RULE, RuleParameterName.SUPPLIER_ID, Set.of(1234L))
                        .setActionCode("SAMSUNG500")
                        .setShopPromoId(SHOP_PROMO_ID)
                        .setAnaplanId("")
                        .setPromoStorageId("")
        );

        // Устанавливаем shop_promo_id равным пустому, так как при создании будет задан L+promo.id
        jdbcTemplate.update("update promo set shop_promo_id = \'\' where id = " + promo.getPromoId().getId());

        promocodeCoinPromoYtExporter.exportToYt();

        verifyHasNoPromo();
        verifyHasFilteredPromo(promo);
    }

    @Test
    public void shouldNotFilterAllPromocodesIfExistsEmptyShopPromoIdWhenSetConfigurationParam() {
        Promo promo = promoManager.createSmartShoppingPromo(
                PromoUtils.SmartShopping.defaultFixedPromocode()
                        .addCoinRule(RuleType.SUPPLIER_FILTER_RULE, RuleParameterName.SUPPLIER_ID, Set.of(1234L))
                        .setActionCode("SAMSUNG500")
                        .setShopPromoId(SHOP_PROMO_ID)
                        .setAnaplanId("")
                        .setPromoStorageId("")
        );

        // Устанавливаем SHOP_PROMO_ID, так как при создании будет задан L+promo.id
        jdbcTemplate.update("update promo set shop_promo_id = \'\' where id = " + promo.getPromoId().getId());

        configurationService.set(ConfigurationService.IS_NOT_FILTER_ALL_PROMOCODE_COIN_IF_EMPTY_SHOPPROMOID_EXISTS,
                true);
        promocodeCoinPromoYtExporter.exportToYt();

        verifyHasPromo(promo);
        verifyHasNoFilteredPromo();
    }

    @Test
    public void shouldExportPromoWithMinOrderTotalAndConditions() {
        Promo promo = promoManager.createSmartShoppingPromo(
                PromoUtils.SmartShopping.defaultFixedPromocode()
                        .addCoinRule(RuleType.MIN_ORDER_TOTAL_CUTTING_RULE, RuleParameterName.MIN_ORDER_TOTAL,
                                BigDecimal.TEN)
                        .addCoinRule(RuleType.MSKU_FILTER_RULE, RuleParameterName.MSKU_ID, "1")
                        .setActionCode("SAMSUNG500")
        );

        promoService.setPromoParam(promo.getId(), PromoParameterName.ADDITIONAL_CONDITIONS_TEXT, "text");

        promocodeCoinPromoYtExporter.exportToYt();

        verifyHasPromo(promo);
        verifyHasNoFilteredPromo();
    }

    @Test
    public void shouldNotExportPromoWithMinOrderTotalAndWithoutConditions() throws IOException {
        Promo promo = promoManager.createSmartShoppingPromo(
                PromoUtils.SmartShopping.defaultFixedPromocode()
                        .addCoinRule(RuleType.MIN_ORDER_TOTAL_CUTTING_RULE, RuleParameterName.MIN_ORDER_TOTAL,
                                BigDecimal.TEN)
                        .setActionCode("SAMSUNG500")
        );

        promocodeCoinPromoYtExporter.exportToYt();

        verifyHasNoPromo();
        verifyHasFilteredPromo(promo);
    }

    @Test
    public void shouldFilterTwoPromocodesWithEmptyActionCode() throws Exception {
        List<Promo> promos = Arrays.asList(
                PromoBuilder.builder(PROMOCODE)
                        .setId(1L)
                        .setStartDate(getDate("2000-01-01 01-00-00"))
                        .setEndDate(getDate("2000-01-03 01-00-00"))
                        .setActionCode("")
                        .setShopPromoId("L" + 1L)
                        .basePromo(),
                PromoBuilder.builder(PROMOCODE)
                        .setId(2L)
                        .setStartDate(getDate("2000-01-01 01-00-00"))
                        .setEndDate(getDate("2000-01-03 01-00-00"))
                        .setActionCode("ACTION_CODE")
                        .setShopPromoId("L" + 2L)
                        .basePromo(),
                PromoBuilder.builder(PROMOCODE)
                        .setId(3L)
                        .setStartDate(getDate("2000-01-01 01-00-00"))
                        .setEndDate(getDate("2000-01-03 01-00-00"))
                        .setActionCode("")
                        .setShopPromoId("L" + 3L)
                        .basePromo());

        List<FilteredPromoForYt> filteredPromoForYts = new ArrayList<>();
        List<Promo> filteredPromos = promocodeCoinPromoYtExporter.filterOnlyPublicAndNotTicked(promos,
                filteredPromoForYts);

        assertThat(filteredPromos, hasSize(1));
        assertThat(filteredPromos, hasItem(allOf(
                hasProperty("id", equalTo(2L))
        )));
        assertThat(filteredPromoForYts, hasSize(2));
    }

    @Test
    public void shouldExportOrderPromoWithMsku() {
        Promo promo = promoManager.createSmartShoppingPromo(
                PromoUtils.SmartShopping.defaultFixedPromocode()
                        .addCoinRule(RuleType.MSKU_FILTER_RULE, RuleParameterName.MSKU_ID, "1")
                        .setActionCode("SAMSUNG500")
        );

        promocodeCoinPromoYtExporter.exportToYt();

        verifyHasPromo(promo);
        verifyHasNoFilteredPromo();
    }

    @Test
    public void shouldExportOrderPromoWithMskuAndNotLogicFalse() {
        Promo promo = promoManager.createSmartShoppingPromo(
                PromoUtils.SmartShopping.defaultFixedPromocode()
                        .addCoinRule(RuleContainer.builder(RuleType.MSKU_FILTER_RULE)
                                .withParams(RuleParameterName.NOT_LOGIC, Set.of(false))
                                .withParams(RuleParameterName.MSKU_ID, Set.of("1"))
                        )
                        .setActionCode("SAMSUNG500")
        );

        promocodeCoinPromoYtExporter.exportToYt();

        verifyHasPromo(promo);
        verifyHasNoFilteredPromo();
    }

    @Test
    public void shouldNoExportOrderPromoWithMskuAndNotLogicTrue() {
        Promo promo = promoManager.createSmartShoppingPromo(
                PromoUtils.SmartShopping.defaultFixedPromocode()
                        .addCoinRule(RuleContainer.builder(RuleType.MSKU_FILTER_RULE)
                                .withParams(RuleParameterName.NOT_LOGIC, Set.of(true))
                                .withParams(RuleParameterName.MSKU_ID, Set.of("1"))
                        )
                        .setActionCode("SAMSUNG500")
        );

        promocodeCoinPromoYtExporter.exportToYt();

        verifyHasNoPromo();
        verifyHasFilteredPromo(promo);
    }

    @Test
    public void shouldExportOrderPromoWithMskuAndCategory() {
        Promo promo = promoManager.createSmartShoppingPromo(
                PromoUtils.SmartShopping.defaultFixedPromocode()
                        .addCoinRule(RuleType.MSKU_FILTER_RULE, RuleParameterName.MSKU_ID, Set.of("1"))
                        .addCoinRule(RuleType.CATEGORY_FILTER_RULE, RuleParameterName.CATEGORY_ID, Set.of(1234))
                        .setActionCode("SAMSUNG500")
        );

        promocodeCoinPromoYtExporter.exportToYt();

        verifyHasPromo(promo);
        verifyHasNoFilteredPromo();
    }

    @Test
    public void shouldExportOrderPromoWithMskuAndCategoryWithNotLogicFalse() {
        Promo promo = promoManager.createSmartShoppingPromo(
                PromoUtils.SmartShopping.defaultFixedPromocode()
                        .addCoinRule(RuleContainer.builder(RuleType.MSKU_FILTER_RULE)
                                .withParams(RuleParameterName.NOT_LOGIC, Set.of(false))
                                .withParams(RuleParameterName.MSKU_ID, Set.of("1"))
                        )
                        .addCoinRule(RuleType.CATEGORY_FILTER_RULE, RuleParameterName.CATEGORY_ID, Set.of(1234))
                        .setActionCode("SAMSUNG500")
        );

        promocodeCoinPromoYtExporter.exportToYt();

        verifyHasPromo(promo);
        verifyHasNoFilteredPromo();
    }

    @Test
    public void shouldExportOrderPromoWithMskuAndCategoryWithNotLogicTrue() {
        Promo promo = promoManager.createSmartShoppingPromo(
                PromoUtils.SmartShopping.defaultFixedPromocode()
                        .addCoinRule(RuleContainer.builder(RuleType.MSKU_FILTER_RULE)
                                .withParams(RuleParameterName.NOT_LOGIC, Set.of(true))
                                .withParams(RuleParameterName.MSKU_ID, Set.of("1"))
                        )
                        .addCoinRule(RuleType.CATEGORY_FILTER_RULE, RuleParameterName.CATEGORY_ID, Set.of(1234))
                        .setActionCode("SAMSUNG500")
        );

        promocodeCoinPromoYtExporter.exportToYt();

        verifyHasPromo(promo);
        verifyHasNoFilteredPromo();
    }

    @Test
    public void shouldExportOrderPromoWithMskuAndCategoryWithNotLogicTrueFalse() {
        Promo promo = promoManager.createSmartShoppingPromo(
                PromoUtils.SmartShopping.defaultFixedPromocode()
                        .addCoinRule(RuleContainer.builder(RuleType.MSKU_FILTER_RULE)
                                .withParams(RuleParameterName.NOT_LOGIC, Set.of(true))
                                .withParams(RuleParameterName.MSKU_ID, Set.of("1"))
                        )
                        .addCoinRule(RuleContainer.builder(RuleType.CATEGORY_FILTER_RULE)
                                .withParams(RuleParameterName.NOT_LOGIC, Set.of(false))
                                .withParams(RuleParameterName.CATEGORY_ID, Set.of(1234))
                        )
                        .setActionCode("SAMSUNG500")
        );

        promocodeCoinPromoYtExporter.exportToYt();

        verifyHasPromo(promo);
        verifyHasNoFilteredPromo();
    }

    @Test
    public void shouldExportOrderPromoWithMskuAndCategoryWithAllNotLogicTrue() {
        Promo promo = promoManager.createSmartShoppingPromo(
                PromoUtils.SmartShopping.defaultFixedPromocode()
                        .addCoinRule(RuleContainer.builder(RuleType.MSKU_FILTER_RULE)
                                .withParams(RuleParameterName.NOT_LOGIC, Set.of(true))
                                .withParams(RuleParameterName.MSKU_ID, Set.of("1"))
                        )
                        .addCoinRule(RuleContainer.builder(RuleType.CATEGORY_FILTER_RULE)
                                .withParams(RuleParameterName.NOT_LOGIC, Set.of(true))
                                .withParams(RuleParameterName.CATEGORY_ID, Set.of(1234))
                        )
                        .setActionCode("SAMSUNG500")
        );

        promocodeCoinPromoYtExporter.exportToYt();

        verifyHasNoPromo();
        verifyHasFilteredPromo(promo);
    }

    private static Date getDate(String dateString) throws ParseException {
        SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH-mm-ss");
        return DATE_FORMAT.parse(dateString);
    }

    private void verifyHasPromo(Promo promo) {
        verify(yt.tables(), times(1)).write(
                any(Optional.class),
                anyBoolean(),
                argThat(ytPathToName(endsWith(PromocodeCoinYtDao.TMP_TABLE_NAME))),
                eq(YTableEntryTypes.JACKSON_UTF8),
                argumentCaptor.capture()
        );
        checkCapturedPromo(promo, argumentCaptor.getAllValues());
    }

    private void verifyHasNoPromo() {
        verify(yt.tables(), times(0)).write(
                any(Optional.class),
                anyBoolean(),
                argThat(ytPathToName(endsWith(PromocodeCoinYtDao.TMP_TABLE_NAME))),
                eq(YTableEntryTypes.JACKSON_UTF8),
                any()
        );
    }

    private void verifyHasFilteredPromo(Promo promo) {
        verify(yt.tables(), times(1)).write(
                any(Optional.class),
                anyBoolean(),
                argThat(ytPathToName(endsWith(PromocodeCoinYtDao.TABLE_NAME))),
                eq(YTableEntryTypes.JACKSON_UTF8),
                argumentCaptor.capture()
        );

        checkCapturedPromo(promo, argumentCaptor.getAllValues());
    }

    private void verifyHasNoFilteredPromo() {
        verify(yt.tables(), times(0)).write(
                any(Optional.class),
                anyBoolean(),
                argThat(ytPathToName(endsWith(PromocodeCoinYtDao.TABLE_NAME))),
                eq(YTableEntryTypes.JACKSON_UTF8),
                any()
        );
    }

    private void checkCapturedPromo(Promo promo, List<Iterator<JsonNode>> captured) {
        ImmutableList.Builder<JsonNode> nodesBuilder = ImmutableList.builder();
        for (Iterator<JsonNode> it : captured) {
            it.forEachRemaining(nodesBuilder::add);
        }
        List<JsonNode> nodes = nodesBuilder.build();
        assertEquals(nodes.size(), 1);
        assertEquals(nodes.get(0).get("promo_id").asText(), promo.getPromoKey());
    }

}
