package ru.yandex.market.supplier.act.exposed;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableMap;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.impl.EmptyMap;
import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.inside.yt.kosher.impl.ytree.YTreeEntityNodeImpl;
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;
import ru.yandex.inside.yt.kosher.ytree.YTreeNode;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.billing.matchers.SupplierAccountMatcher;
import ru.yandex.market.core.billing.matchers.SupplierExposedActMatcher;
import ru.yandex.market.core.partner.contract.PartnerContractDao;
import ru.yandex.market.core.supplier.PartnerContractType;
import ru.yandex.market.core.supplier.dao.SupplierExposedActDao;
import ru.yandex.market.core.supplier.model.ProductId;
import ru.yandex.market.core.supplier.model.SupplierAccount;
import ru.yandex.market.core.supplier.model.SupplierExposedAct;
import ru.yandex.market.core.supplier.model.SupplierExposedActStatus;
import ru.yandex.market.core.yt.YtHttpFactory;
import ru.yandex.market.mbi.util.MbiMatchers;
import ru.yandex.market.mbi.yt.YtCluster;
import ru.yandex.market.mbi.yt.YtTemplate;
import ru.yandex.market.mbi.yt.YtTestUtils;
import ru.yandex.market.shop.FunctionalTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

/**
 * Тесты для {@link SupplierExposedActImportService}
 */
class SupplierExposedActImportServiceTest extends FunctionalTest {

    private static final String HAHN = "hahn.yt.yandex.net";
    private final List<YTreeMapNode> actsYtData =
            YtTestUtils.readYtData(
                    getClass().getResourceAsStream("SupplierExposedActServiceTest.yt.actData.json"),
                    SupplierExposedActImportServiceTest::actJsonToYtNode
            );
    private final List<YTreeMapNode> accountsYtData =
            YtTestUtils.readYtData(
                    getClass().getResourceAsStream("SupplierExposedActServiceTest.yt.accountData.json"),
                    SupplierExposedActImportServiceTest::accountJsonToYtNode
            );
    private final String invoiceTablePath = "//home/market/testing/mbi/billing/overdraft_control/invoices/t_invoice";
    private final String salesDailyTablePath = "//home/market/testing/mstat/oebs/sales_daily_market";
    @Autowired
    private SupplierExposedActDao supplierExposedActDao;
    @Autowired
    private PartnerContractDao supplierContractDao;
    @Autowired
    private YtHttpFactory ytHttpFactory;
    @Autowired
    private OebsMetabaseDao oebsMetabaseDao;
    private SupplierExposedActImportService service;

    private static YTreeMapNode buildActTreeMapNode(
            long actId,
            String actDate,
            Long contractId,
            String contractEid,
            String externalId,
            double amtRur,
            double paidAmtRur,
            double amtRurWithNds,
            long productId,
            long invoiceId
    ) {
        return YtTestUtils.treeMapNode(ImmutableMap.<String, YTreeNode>builder()
                .put("act_id", YtTestUtils.intNode(actId))
                .put("dt", YtTestUtils.stringNode(actDate))
                .put("contract_id", contractId != null ? YtTestUtils.intNode(contractId)
                        : new YTreeEntityNodeImpl(new EmptyMap<>()))
                .put("contract_eid", YtTestUtils.stringNode(contractEid))
                .put("act_external_id", YtTestUtils.stringNode(externalId))
                .put("amt_rur", YtTestUtils.doubleNode(amtRur))
                .put("paid_amt_rur", YtTestUtils.doubleNode(paidAmtRur))
                .put("amt_rur_with_nds", YtTestUtils.doubleNode(amtRurWithNds))
                .put("product_id", YtTestUtils.intNode(productId))
                .put("invoice_id", YtTestUtils.intNode(invoiceId))
                .build());
    }

    private static YTreeMapNode buildAccountTreeMapNode(
            long id,
            String externalId,
            Long contractId,
            double totalActSum,
            String receiptDt,
            String receiptDt1c,
            double receiptSum,
            double receiptSum1c,
            String paymentDate,
            int hidden,
            int offerTypeId
    ) {
        return YtTestUtils.treeMapNode(ImmutableMap.<String, YTreeNode>builder()
                .put("id", YtTestUtils.intNode(id))
                .put("external_id", YtTestUtils.stringNode(externalId))
                .put("contract_id", contractId != null ? YtTestUtils.intNode(contractId)
                        : new YTreeEntityNodeImpl(new EmptyMap<>())
                ).put("total_act_sum", YtTestUtils.doubleNode(totalActSum))
                .put("receipt_dt", YtTestUtils.stringNode(receiptDt))
                .put("receipt_dt_1c", YtTestUtils.stringNode(receiptDt1c))
                .put("receipt_sum", YtTestUtils.doubleNode(receiptSum))
                .put("receipt_sum_1c", YtTestUtils.doubleNode(receiptSum1c))
                .put("payment_date", YtTestUtils.stringNode(paymentDate))
                .put("hidden", YtTestUtils.intNode(hidden))
                .put("offer_type_id", YtTestUtils.intNode(offerTypeId))
                .build()
        );
    }

    private static YTreeMapNode actJsonToYtNode(JsonNode jsonNode) {
        return buildActTreeMapNode(
                jsonNode.get("act_id").asLong(),
                jsonNode.get("dt").asText(),
                Optional.ofNullable(jsonNode.get("contract_id")).map(JsonNode::longValue).orElse(null),
                jsonNode.get("contract_eid").asText(),
                jsonNode.get("act_external_id").asText(),
                jsonNode.get("amt_rur").asDouble(),
                jsonNode.get("paid_amt_rur").asDouble(),
                jsonNode.get("amt_rur_with_nds").asDouble(),
                jsonNode.get("product_id").asLong(),
                jsonNode.get("invoice_id").asLong()
        );
    }

    private static YTreeMapNode accountJsonToYtNode(JsonNode jsonNode) {
        return buildAccountTreeMapNode(
                jsonNode.get("id").asLong(),
                jsonNode.get("external_id").asText(),
                Optional.ofNullable(jsonNode.get("contract_id")).map(JsonNode::longValue).orElse(null),
                jsonNode.get("total_act_sum").asDouble(),
                jsonNode.get("receipt_dt").asText(),
                jsonNode.get("receipt_dt_1c").asText(),
                jsonNode.get("receipt_sum").asDouble(),
                jsonNode.get("receipt_sum_1c").asDouble(),
                jsonNode.get("payment_date").asText(),
                jsonNode.get("hidden").asInt(),
                jsonNode.get("offer_type_id").asInt()
        );
    }

    void setUp(List<YTreeMapNode> ytData) {
        final Yt hahn = YtTestUtils.mockYt(ytData);
        when(ytHttpFactory.getYt(HAHN)).thenReturn(hahn);

        YtTemplate ytTemplate = new YtTemplate(new YtCluster(HAHN, hahn));

        service = new SupplierExposedActImportService(
                invoiceTablePath,
                salesDailyTablePath,
                supplierContractDao,
                ytTemplate,
                supplierExposedActDao,
                oebsMetabaseDao
        );
    }

    @DisplayName("Помечаем как устаревшие акты, которые есть в базе, но нет в переданном списке")
    @Test
    @DbUnitDataSet(
            before = "SupplierExposedActServiceTest.markObsolete.before.csv",
            after = "SupplierExposedActServiceTest.markObsolete.after.csv"
    )
    void checkObsoleteActs() {
        setUp(actsYtData);
        service.checkObsoleteActs(
                List.of(
                        new SupplierExposedAct.Builder()
                                .setSupplierId(1L)
                                .setContractId(1L)
                                .setContractEid("1")
                                .setContractType(PartnerContractType.INCOME)
                                .setActId(1L)
                                .setActDate(LocalDate.now())
                                .setStatus(SupplierExposedActStatus.PENDING)
                                .setProductId(ProductId.REWARD)
                                .build(),
                        new SupplierExposedAct.Builder()
                                .setSupplierId(2L)
                                .setContractId(2L)
                                .setContractEid("2")
                                .setContractType(PartnerContractType.INCOME)
                                .setActId(2L)
                                .setActDate(LocalDate.now())
                                .setStatus(SupplierExposedActStatus.PENDING)
                                .setProductId(ProductId.REWARD)
                                .build()
                ),
                PartnerContractType.INCOME
        );
    }

    @Test
    void importActsFromYt() {
        setUp(actsYtData);
        Map<Long, Long> supplierContractIds = ImmutableMap.of(344930L, 1L, 337142L, 1L, 364560L, 1L);
        List<SupplierExposedAct> acts = service.getActsToImport(supplierContractIds);

        assertEquals(3, acts.size());

        assertThat(
                acts,
                Matchers.containsInAnyOrder(
                        MbiMatchers.<SupplierExposedAct>newAllOfBuilder()
                                .add(allOf(
                                        SupplierExposedActMatcher.hasSupplierId(1),
                                        SupplierExposedActMatcher.hasContractId(344930),
                                        SupplierExposedActMatcher.hasContractEid("344930"),
                                        SupplierExposedActMatcher.hasContractType(PartnerContractType.INCOME),
                                        SupplierExposedActMatcher.hasActId(82199456),
                                        SupplierExposedActMatcher.hasExternalId("82199456"),
                                        SupplierExposedActMatcher.hasActDate(LocalDate.of(2018, 5, 31)),
                                        SupplierExposedActMatcher.hasDeadlineDate(LocalDate.of(2018, 6, 15)),
                                        SupplierExposedActMatcher.hasStatus(SupplierExposedActStatus.CLOSED),
                                        SupplierExposedActMatcher.hasProductId(ProductId.REWARD),
                                        SupplierExposedActMatcher.hasAmtRur(BigDecimal.valueOf(105.35)),
                                        SupplierExposedActMatcher.hasPaidAmtRur(BigDecimal.valueOf(105.35)),
                                        SupplierExposedActMatcher.hasAmtRurWithNds(BigDecimal.valueOf(84.28)),
                                        SupplierExposedActMatcher.hasAccountId(123L)
                                )).build(),
                        MbiMatchers.<SupplierExposedAct>newAllOfBuilder()
                                .add(allOf(
                                        SupplierExposedActMatcher.hasSupplierId(1),
                                        SupplierExposedActMatcher.hasContractId(337142),
                                        SupplierExposedActMatcher.hasContractEid("337142"),
                                        SupplierExposedActMatcher.hasContractType(PartnerContractType.INCOME),
                                        SupplierExposedActMatcher.hasActId(82198264),
                                        SupplierExposedActMatcher.hasExternalId("82198264"),
                                        SupplierExposedActMatcher.hasActDate(LocalDate.of(2018, 5, 31)),
                                        SupplierExposedActMatcher.hasDeadlineDate(LocalDate.of(2018, 6, 15)),
                                        SupplierExposedActMatcher.hasStatus(SupplierExposedActStatus.CLOSED),
                                        SupplierExposedActMatcher.hasProductId(ProductId.REWARD),
                                        SupplierExposedActMatcher.hasAmtRur(BigDecimal.valueOf(732.58)),
                                        SupplierExposedActMatcher.hasPaidAmtRur(BigDecimal.valueOf(732.58)),
                                        SupplierExposedActMatcher.hasAmtRurWithNds(BigDecimal.valueOf(580.07)),
                                        SupplierExposedActMatcher.hasAccountId(124L)
                                )).build(),
                        MbiMatchers.<SupplierExposedAct>newAllOfBuilder()
                                .add(allOf(
                                        SupplierExposedActMatcher.hasSupplierId(1),
                                        SupplierExposedActMatcher.hasContractId(364560),
                                        SupplierExposedActMatcher.hasContractEid("364560"),
                                        SupplierExposedActMatcher.hasContractType(PartnerContractType.INCOME),
                                        SupplierExposedActMatcher.hasActId(82202865),
                                        SupplierExposedActMatcher.hasExternalId("82202865"),
                                        SupplierExposedActMatcher.hasActDate(LocalDate.of(2018, 5, 31)),
                                        SupplierExposedActMatcher.hasDeadlineDate(LocalDate.of(2018, 6, 15)),
                                        SupplierExposedActMatcher.hasStatus(SupplierExposedActStatus.PENDING),
                                        SupplierExposedActMatcher.hasProductId(ProductId.REWARD),
                                        SupplierExposedActMatcher.hasAmtRur(BigDecimal.valueOf(732.58)),
                                        SupplierExposedActMatcher.hasPaidAmtRur(BigDecimal.valueOf(0.0)),
                                        SupplierExposedActMatcher.hasAmtRurWithNds(BigDecimal.valueOf(580.07)),
                                        SupplierExposedActMatcher.hasAccountId(124L)
                                )).build()
                )
        );
    }

    @Test
    void testImportAccountsFromYt() {
        setUp(accountsYtData);
        Map<Long, Long> supplierContractIds = ImmutableMap.of(10L, 1L, 20L, 1L);
        Set<SupplierAccount> accounts = service.getAccountsToImport(supplierContractIds);

        assertEquals(2, accounts.size());

        assertThat(
                accounts,
                Matchers.containsInAnyOrder(
                        MbiMatchers.<SupplierAccount>newAllOfBuilder()
                                .add(allOf(
                                        SupplierAccountMatcher.hasSupplierId(1),
                                        SupplierAccountMatcher.hasId(1),
                                        SupplierAccountMatcher.hasAccountEid("1"),
                                        SupplierAccountMatcher.hasTotalActSum(BigDecimal.valueOf(10.0)),
                                        SupplierAccountMatcher.hasReceiptSum(BigDecimal.valueOf(20.0)),
                                        SupplierAccountMatcher.hasPaymentDate(LocalDate.of(2019, 12, 6))
                                )).build(),
                        MbiMatchers.<SupplierAccount>newAllOfBuilder()
                                .add(allOf(
                                        SupplierAccountMatcher.hasSupplierId(1),
                                        SupplierAccountMatcher.hasId(2),
                                        SupplierAccountMatcher.hasAccountEid("2"),
                                        SupplierAccountMatcher.hasTotalActSum(BigDecimal.valueOf(10.0)),
                                        SupplierAccountMatcher.hasReceiptSum(BigDecimal.valueOf(0.0)),
                                        SupplierAccountMatcher.hasPaymentDate(LocalDate.of(2019, 12, 6))
                                )).build()
                )
        );
    }

    @Test
    void supplierExposedActConverter() {
        setUp(actsYtData);
        YTreeMapNode mapNode = buildActTreeMapNode(
                1,
                "2020-01-09",
                101L,
                "101",
                "1",
                100D,
                10D,
                8D,
                ProductId.REWARD.getId(),
                1
        );

        SupplierExposedAct exposedAct = service.convertToSupplierExposedAct(mapNode, 1L);

        assertThat(
                exposedAct,
                MbiMatchers.<SupplierExposedAct>newAllOfBuilder()
                        .add(allOf(
                                SupplierExposedActMatcher.hasActId(1L),
                                SupplierExposedActMatcher.hasActDate(LocalDate.parse("2020-01-09")),
                                SupplierExposedActMatcher.hasContractId(101L),
                                SupplierExposedActMatcher.hasContractEid("101"),
                                SupplierExposedActMatcher.hasContractType(PartnerContractType.INCOME),
                                SupplierExposedActMatcher.hasActDate(LocalDate.of(2020, 1, 9)),
                                SupplierExposedActMatcher.hasDeadlineDate(LocalDate.of(2020, 2, 15)),
                                SupplierExposedActMatcher.hasStatus(SupplierExposedActStatus.PENDING),
                                SupplierExposedActMatcher.hasPaidAmtRur(BigDecimal.valueOf(10.0)),
                                SupplierExposedActMatcher.hasAmtRur(BigDecimal.valueOf(100.0)),
                                SupplierExposedActMatcher.hasAmtRurWithNds(BigDecimal.valueOf(8.0)),
                                SupplierExposedActMatcher.hasProductId(ProductId.REWARD),
                                SupplierExposedActMatcher.hasAccountId(1L),
                                SupplierExposedActMatcher.hasExternalId("1")
                        )).build()
        );
    }

    @Test
    void supplierAccountConverter() {
        setUp(accountsYtData);
        YTreeMapNode mapNode = buildAccountTreeMapNode(
                1,
                "1",
                101L,
                50.5,
                "2021-10-02T04:25:12Z",
                "2021-10-02T04:25:12Z",
                50,
                50,
                "2020-01-30",
                0,
                0
        );

        assertThat(
                service.convertToSupplierAccount(mapNode, 1),
                MbiMatchers.<SupplierAccount>newAllOfBuilder()
                        .add(allOf(
                                SupplierAccountMatcher.hasId(1),
                                SupplierAccountMatcher.hasAccountEid("1"),
                                SupplierAccountMatcher.hasTotalActSum(BigDecimal.valueOf(50.5)),
                                SupplierAccountMatcher.hasReceiptSum(BigDecimal.valueOf(50.0)),
                                SupplierAccountMatcher.hasPaymentDate(LocalDate.of(2020, 1, 30))
                        )).build()
        );
    }
}
