package ru.yandex.market.loyalty.admin.it;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.web.WebAppConfiguration;

import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.market.loyalty.admin.archivation.ArchivationStateDao;
import ru.yandex.market.loyalty.admin.config.ITConfig;
import ru.yandex.market.loyalty.admin.config.TestAuthorizationContext;
import ru.yandex.market.loyalty.admin.service.bunch.BunchRequestException;
import ru.yandex.market.loyalty.admin.service.bunch.BunchRequestService;
import ru.yandex.market.loyalty.admin.service.bunch.export.YtExporter;
import ru.yandex.market.loyalty.admin.tms.BunchRequestProcessor;
import ru.yandex.market.loyalty.admin.tms.PromoYtUpdateProcessor;
import ru.yandex.market.loyalty.admin.tms.YandexWalletTopUpProcessor;
import ru.yandex.market.loyalty.admin.tms.YandexWalletTransactionReplicationHahnProcessor;
import ru.yandex.market.loyalty.admin.utils.YtTestHelper;
import ru.yandex.market.loyalty.admin.yt.YtClient;
import ru.yandex.market.loyalty.admin.yt.model.DataTransferYtTable;
import ru.yandex.market.loyalty.admin.yt.service.AccountTableYtExporter;
import ru.yandex.market.loyalty.admin.yt.service.ActiveAuthCoinsYtExporter;
import ru.yandex.market.loyalty.admin.yt.service.AllPromosYtExporter;
import ru.yandex.market.loyalty.admin.yt.service.CategoryTypeGroupReferenceYtExporter;
import ru.yandex.market.loyalty.admin.yt.service.CategoryTypeGroupTariffReferenceYtExporter;
import ru.yandex.market.loyalty.admin.yt.service.CategoryTypeReferenceYtExporter;
import ru.yandex.market.loyalty.admin.yt.service.CoinDescriptionTableYtExporter;
import ru.yandex.market.loyalty.admin.yt.service.CoinHistoryExternalYtExporter;
import ru.yandex.market.loyalty.admin.yt.service.CoinHistoryTableYtExporter;
import ru.yandex.market.loyalty.admin.yt.service.CoinHistoryWithOrdersYtExporter;
import ru.yandex.market.loyalty.admin.yt.service.CoinHistoryXTableYtExporter;
import ru.yandex.market.loyalty.admin.yt.service.CoinNoAuthTableYtExporter;
import ru.yandex.market.loyalty.admin.yt.service.CoinPropsTableYtExporter;
import ru.yandex.market.loyalty.admin.yt.service.CoinsTableYtExporter;
import ru.yandex.market.loyalty.admin.yt.service.CouponHistoryExternalYtExporter;
import ru.yandex.market.loyalty.admin.yt.service.CouponHistoryTableYtExporter;
import ru.yandex.market.loyalty.admin.yt.service.CouponHistoryXTableYtExporter;
import ru.yandex.market.loyalty.admin.yt.service.CouponTableYtExporter;
import ru.yandex.market.loyalty.admin.yt.service.CouponValueYtExporter;
import ru.yandex.market.loyalty.admin.yt.service.CreatedCoinsYtExporter;
import ru.yandex.market.loyalty.admin.yt.service.CreatedCouponsYtExporter;
import ru.yandex.market.loyalty.admin.yt.service.CreatedPromocodesYtExporter;
import ru.yandex.market.loyalty.admin.yt.service.DiscountHistoryTableYtExporter;
import ru.yandex.market.loyalty.admin.yt.service.DiscountHistoryWithOrdersYtExporter;
import ru.yandex.market.loyalty.admin.yt.service.DiscountHistoryXTableYtExporter;
import ru.yandex.market.loyalty.admin.yt.service.DiscountNoAuthTableYtExporter;
import ru.yandex.market.loyalty.admin.yt.service.DiscountTableYtExporter;
import ru.yandex.market.loyalty.admin.yt.service.FraudCoinDisposeQueueYtExporter;
import ru.yandex.market.loyalty.admin.yt.service.MetaTransactionTableYtExporter;
import ru.yandex.market.loyalty.admin.yt.service.OperationContextTableYtExporter;
import ru.yandex.market.loyalty.admin.yt.service.PartnerCashbackPropertiesTableExternalYtExporter;
import ru.yandex.market.loyalty.admin.yt.service.PartnerCashbackStandardPromoHidsTableExternalYtExporter;
import ru.yandex.market.loyalty.admin.yt.service.PartnerCashbackStandardPromoPropertiesTableExternalYtExporter;
import ru.yandex.market.loyalty.admin.yt.service.PartnerCashbackStandardPromoYtExporter;
import ru.yandex.market.loyalty.admin.yt.service.PartnerCashbackVersionYtExporter;
import ru.yandex.market.loyalty.admin.yt.service.ProhibitedTriggerEventDryRunCoinsTableYtExporter;
import ru.yandex.market.loyalty.admin.yt.service.PromoAuditTableYtExporter;
import ru.yandex.market.loyalty.admin.yt.service.PromoMergeTagYtExporter;
import ru.yandex.market.loyalty.admin.yt.service.PromoParamsTableYtExporter;
import ru.yandex.market.loyalty.admin.yt.service.PromoTableYtExporter;
import ru.yandex.market.loyalty.admin.yt.service.PromoYtExporter;
import ru.yandex.market.loyalty.admin.yt.service.PromocodeCoinPromoYtExporter;
import ru.yandex.market.loyalty.admin.yt.service.TransactionTableYtExporter;
import ru.yandex.market.loyalty.admin.yt.service.UsedCoinsYtExporter;
import ru.yandex.market.loyalty.admin.yt.service.UsedCouponsYtExporter;
import ru.yandex.market.loyalty.admin.yt.service.UsedPromocodesYtExporter;
import ru.yandex.market.loyalty.admin.yt.service.YandexWalletTransactionExternalExtendedYtExporter;
import ru.yandex.market.loyalty.admin.yt.service.YandexWalletTransactionExternalYtExporter;
import ru.yandex.market.loyalty.admin.yt.service.YandexWalletTransactionsYtExporter;
import ru.yandex.market.loyalty.admin.yt.service.YtTableDescription;
import ru.yandex.market.loyalty.api.model.CoinGeneratorType;
import ru.yandex.market.loyalty.api.model.CouponCreationRequest;
import ru.yandex.market.loyalty.api.model.TableFormat;
import ru.yandex.market.loyalty.api.model.discount.MultiCartWithBundlesDiscountResponse;
import ru.yandex.market.loyalty.api.model.perk.PerkType;
import ru.yandex.market.loyalty.api.model.promocode.PromocodeActivationResultCode;
import ru.yandex.market.loyalty.core.config.YtHahn;
import ru.yandex.market.loyalty.core.dao.CategoryTypeGroupReferenceDao;
import ru.yandex.market.loyalty.core.dao.CategoryTypeGroupTariffReferenceDao;
import ru.yandex.market.loyalty.core.dao.CategoryTypeReferenceDao;
import ru.yandex.market.loyalty.core.dao.PartnerCashbackStandardPromoDao;
import ru.yandex.market.loyalty.core.dao.PartnerCashbackVersionDao;
import ru.yandex.market.loyalty.core.dao.YandexWalletTransactionDao;
import ru.yandex.market.loyalty.core.dao.antifraud.FraudCoinDisposeQueueDao;
import ru.yandex.market.loyalty.core.dao.antifraud.FraudCoinDisposeQueueRecord;
import ru.yandex.market.loyalty.core.dao.antifraud.FraudCoinDisposeStatus;
import ru.yandex.market.loyalty.core.dao.coin.BunchGenerationRequestDao;
import ru.yandex.market.loyalty.core.dao.coin.BunchGenerationRequestParamName;
import ru.yandex.market.loyalty.core.dao.coin.GeneratorType;
import ru.yandex.market.loyalty.core.dao.trigger.DryRunCoin;
import ru.yandex.market.loyalty.core.dao.trigger.DryRunCoinsRecord;
import ru.yandex.market.loyalty.core.dao.trigger.ProhibitedTriggerEventDryRunCoinsDao;
import ru.yandex.market.loyalty.core.mock.ClockForTests;
import ru.yandex.market.loyalty.core.model.cashback.partner.entity.CategoryTypeEntry;
import ru.yandex.market.loyalty.core.model.cashback.partner.entity.CategoryTypeGroupEntry;
import ru.yandex.market.loyalty.core.model.cashback.partner.entity.CategoryTypeGroupTariffEntry;
import ru.yandex.market.loyalty.core.model.cashback.partner.entity.StandardPromoEntry;
import ru.yandex.market.loyalty.core.model.cashback.partner.entity.VersionEntry;
import ru.yandex.market.loyalty.core.model.coin.BunchGenerationRequest;
import ru.yandex.market.loyalty.core.model.coin.CoinKey;
import ru.yandex.market.loyalty.core.model.coin.CoreCoinType;
import ru.yandex.market.loyalty.core.model.promo.Promo;
import ru.yandex.market.loyalty.core.model.wallet.YandexWalletNewTransaction;
import ru.yandex.market.loyalty.core.model.wallet.YandexWalletTransaction;
import ru.yandex.market.loyalty.core.model.wallet.YandexWalletTransactionPriority;
import ru.yandex.market.loyalty.core.model.wallet.YandexWalletTransactionStatus;
import ru.yandex.market.loyalty.core.service.ClidCacheService;
import ru.yandex.market.loyalty.core.service.ConfigurationService;
import ru.yandex.market.loyalty.core.service.budgeting.DeferredMetaTransactionService;
import ru.yandex.market.loyalty.core.service.PromoManager;
import ru.yandex.market.loyalty.core.service.budgeting.BudgetModeService;
import ru.yandex.market.loyalty.core.service.cashback.CashbackCacheService;
import ru.yandex.market.loyalty.core.service.coin.CoinService;
import ru.yandex.market.loyalty.core.service.coupon.CouponService;
import ru.yandex.market.loyalty.core.service.discount.DiscountService;
import ru.yandex.market.loyalty.core.service.promocode.PromocodeActivationRequest;
import ru.yandex.market.loyalty.core.service.promocode.PromocodeActivationResult;
import ru.yandex.market.loyalty.core.service.promocode.PromocodeService;
import ru.yandex.market.loyalty.core.service.promocode.PromocodesActivationResult;
import ru.yandex.market.loyalty.core.test.DbCleaner;
import ru.yandex.market.loyalty.core.test.LoyaltySpringTestRunner;
import ru.yandex.market.loyalty.core.test.SupplementaryDataLoader;
import ru.yandex.market.loyalty.core.utils.DiscountRequestWithBundlesBuilder;
import ru.yandex.market.loyalty.core.utils.DiscountUtils;
import ru.yandex.market.loyalty.core.utils.OperationContextFactory;
import ru.yandex.market.loyalty.core.utils.PromoUtils;
import ru.yandex.market.loyalty.core.utils.RulePayloads;

import static java.time.temporal.ChronoUnit.MINUTES;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertTrue;
import static ru.yandex.market.loyalty.admin.tms.CheckouterEventProcessorMultiEventsTest.DEFAULT_MULTI_ORDER_ID;
import static ru.yandex.market.loyalty.admin.tms.PreparedBunchGenerationRequestProcessorTest.createTestScheduledRequest;
import static ru.yandex.market.loyalty.admin.utils.YtTestHelper.createWalletTopUps;
import static ru.yandex.market.loyalty.core.dao.coin.BunchGenerationRequestParamName.CAMPAIGN_NAME;
import static ru.yandex.market.loyalty.core.dao.coin.BunchGenerationRequestParamName.COIN_GENERATOR_TYPE;
import static ru.yandex.market.loyalty.core.dao.coin.BunchGenerationRequestParamName.ERROR_OUTPUT_TABLE;
import static ru.yandex.market.loyalty.core.dao.coin.BunchGenerationRequestParamName.INPUT_TABLE;
import static ru.yandex.market.loyalty.core.dao.coin.BunchGenerationRequestParamName.INPUT_TABLE_CLUSTER;
import static ru.yandex.market.loyalty.core.dao.coin.BunchGenerationRequestParamName.OUTPUT_TABLE;
import static ru.yandex.market.loyalty.core.dao.coin.BunchGenerationRequestParamName.OUTPUT_TABLE_CLUSTER;
import static ru.yandex.market.loyalty.core.model.cashback.partner.entity.VersionStatus.ACTIVE;
import static ru.yandex.market.loyalty.core.model.coin.CoreCoinCreationReason.ORDER;
import static ru.yandex.market.loyalty.core.model.coin.CoreCoinCreationReason.PARTNER;
import static ru.yandex.market.loyalty.core.model.promo.RuleParameterName.CATEGORY_ID;
import static ru.yandex.market.loyalty.core.model.promo.RuleParameterName.MAX_CASHBACK;
import static ru.yandex.market.loyalty.core.model.promo.RuleParameterName.MSKU_ID;
import static ru.yandex.market.loyalty.core.model.promo.RuleParameterName.PERK_TYPE;
import static ru.yandex.market.loyalty.core.rule.RuleType.CATEGORY_FILTER_RULE;
import static ru.yandex.market.loyalty.core.rule.RuleType.DONT_USE_WITH_BUNDLES_FILTER_RULE;
import static ru.yandex.market.loyalty.core.rule.RuleType.DONT_USE_WITH_CHEAPEST_AS_GIFT_FILTER_RULE;
import static ru.yandex.market.loyalty.core.rule.RuleType.MAX_CASHBACK_FILTER_RULE;
import static ru.yandex.market.loyalty.core.rule.RuleType.MSKU_FILTER_RULE;
import static ru.yandex.market.loyalty.core.rule.RuleType.PERKS_ALLOWED_CUTTING_RULE;
import static ru.yandex.market.loyalty.core.test.SupplementaryDataLoader.PARENT_CATEGORY_ID;
import static ru.yandex.market.loyalty.core.utils.CheckouterUtils.DEFAULT_ORDER_ID;
import static ru.yandex.market.loyalty.core.utils.CoinRequestUtils.defaultAuth;
import static ru.yandex.market.loyalty.core.utils.CoinRequestUtils.defaultNoAuth;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.orderRequestWithBundlesBuilder;
import static ru.yandex.market.loyalty.core.utils.PromoUtils.DEFAULT_COUPON_VALUE;
import static ru.yandex.market.loyalty.core.utils.PromoUtils.SmartShopping.DEFAULT_COIN_FIXED_NOMINAL;
import static ru.yandex.market.loyalty.core.utils.PromoUtils.SmartShopping.defaultFreeDelivery;
import static ru.yandex.market.loyalty.core.utils.UserDataFactory.DEFAULT_UID;

@Ignore("this test suite should be run manually because it uses real YT")
@ContextConfiguration(classes = ITConfig.class)
@RunWith(LoyaltySpringTestRunner.class)
@WebAppConfiguration
//token можно получить https://yql.yandex-team.ru/?settings_mode=token
@TestPropertySource(
        locations = {"/it.secret.local.properties"}
)
public class YtExporterUtilForTesting {

    private static final Map<DataTransferYtTable, YtTableDescription> DATA_TRANSFER_TABLE_DESCRIPTIONS = Map.of(
            DataTransferYtTable.DISCOUNT, DiscountTableYtExporter.TABLE_DESCRIPTION,
            DataTransferYtTable.DISCOUNT_ARCHIVE, DiscountTableYtExporter.TABLE_DESCRIPTION,
            DataTransferYtTable.DISCOUNT_HISTORY, DiscountHistoryTableYtExporter.TABLE_DESCRIPTION,
            DataTransferYtTable.DISCOUNT_HISTORY_ARCHIVE, DiscountHistoryTableYtExporter.TABLE_DESCRIPTION,
            DataTransferYtTable.DISCOUNT_NO_AUTH, DiscountNoAuthTableYtExporter.TABLE_DESCRIPTION,
            DataTransferYtTable.DISCOUNT_NO_AUTH_ARCHIVE, DiscountNoAuthTableYtExporter.TABLE_DESCRIPTION
    );

    @Autowired
    @YtHahn
    private YtClient ytClient;
    @Autowired
    private PromoManager promoManager;
    @Autowired
    private PromoUtils promoUtils;
    @Autowired
    private CouponService couponService;
    @Autowired
    private DbCleaner dbCleaner;
    @Autowired
    private SupplementaryDataLoader supplementaryDataLoader;
    @Autowired
    private ClockForTests clock;
    @Autowired
    private DiscountService discountService;
    @Autowired
    private FraudCoinDisposeQueueDao fraudCoinDisposeQueueDao;
    @Autowired
    private CoinService coinService;
    @Autowired
    private PromocodeService promocodeService;
    @Autowired
    private BunchRequestService bunchRequestService;
    @Autowired
    private BunchRequestProcessor bunchRequestProcessor;
    @Autowired
    private BunchGenerationRequestDao bunchGenerationRequestDao;
    @Autowired
    @YtHahn
    private PartnerCashbackVersionYtExporter partnerCashbackVersionYtExporter;
    @Autowired
    @YtHahn
    private CategoryTypeReferenceYtExporter categoryTypeReferenceYtExporter;
    @Autowired
    @YtHahn
    private CategoryTypeGroupReferenceYtExporter categoryTypeGroupReferenceYtExporter;
    @Autowired
    @YtHahn
    private CategoryTypeGroupTariffReferenceYtExporter categoryTypeGroupTariffReferenceYtExporter;
    @Autowired
    @YtHahn
    private PartnerCashbackStandardPromoYtExporter partnerCashbackStandardPromoYtExporter;
    @Autowired
    private PartnerCashbackVersionDao partnerCashbackVersionDao;
    @Autowired
    private CategoryTypeReferenceDao categoryTypeReferenceDao;
    @Autowired
    private CategoryTypeGroupReferenceDao categoryTypeGroupReferenceDao;
    @Autowired
    private CategoryTypeGroupTariffReferenceDao categoryTypeGroupTariffReferenceDao;
    @Autowired
    private PartnerCashbackStandardPromoDao partnerCashbackStandardPromoDao;
    @Autowired
    private BudgetModeService budgetModeService;
    @Autowired
    private ClidCacheService clidCacheService;
    @Autowired
    private CashbackCacheService cashbackCacheService;

    @Before
    public void prepareDatabase() {
        dbCleaner.clearDb();
        supplementaryDataLoader.createTechnicalIfNotExists();
        supplementaryDataLoader.createEmptyOperationContext();
        supplementaryDataLoader.populateCategoryTree();
        cashbackCacheService.reloadCashbackPromos();
    }

    @Autowired
    @YtHahn
    private CouponTableYtExporter couponTableYtExporter;
    @Autowired
    @YtHahn
    private CoinsTableYtExporter coinsTableYtExporter;
    @Autowired
    @YtHahn
    private DiscountTableYtExporter discountTableYtExporter;
    @Autowired
    @YtHahn
    private DiscountNoAuthTableYtExporter discountNoAuthTableYtExporter;
    @Autowired
    @YtHahn
    private CoinPropsTableYtExporter coinPropsTableYtExporter;
    @Autowired
    @YtHahn
    private PromoTableYtExporter promoTableYtExporter;
    @Autowired
    @YtHahn
    private ActiveAuthCoinsYtExporter activeAuthCoinsYtExporter;
    @Autowired
    @YtHahn
    private PromoMergeTagYtExporter promoMergeTagYtExporter;
    @Autowired
    @YtHahn
    private CoinNoAuthTableYtExporter coinNoAuthTableYtExporter;
    @Autowired
    @YtHahn
    private CreatedCoinsYtExporter createdCoinsYtExporter;
    @Autowired
    @YtHahn
    private CreatedPromocodesYtExporter createdPromocodesYtExporter;
    @Autowired
    @YtHahn
    private CoinHistoryTableYtExporter coinHistoryTableYtExporter;
    @Autowired
    @YtHahn
    private DiscountHistoryTableYtExporter discountHistoryTableYtExporter;
    @Autowired
    @YtHahn
    private CoinHistoryExternalYtExporter coinHistoryExternalYtExporter;
    @Autowired
    @YtHahn
    private CoinHistoryXTableYtExporter coinHistoryXTableYtExporter;
    @Autowired
    @YtHahn
    private DiscountHistoryXTableYtExporter discountHistoryXTableYtExporter;
    @Autowired
    @YtHahn
    private CouponHistoryTableYtExporter couponHistoryTableYtExporter;
    @Autowired
    @YtHahn
    private CouponHistoryExternalYtExporter couponHistoryExternalYtExporter;
    @Autowired
    @YtHahn
    private CouponHistoryXTableYtExporter couponHistoryXTableYtExporter;
    @Autowired
    @YtHahn
    private PromoParamsTableYtExporter promoParamsTableYtExporter;
    @Autowired
    @YtHahn
    private OperationContextTableYtExporter operationContextTableYtExporter;
    @Autowired
    @YtHahn
    private MetaTransactionTableYtExporter metaTransactionTableYtExporter;
    @Autowired
    @YtHahn
    private TransactionTableYtExporter transactionTableYtExporter;
    @Autowired
    @YtHahn
    private UsedCoinsYtExporter usedCoinsYtExporter;
    @Autowired
    @YtHahn
    private UsedCouponsYtExporter usedCouponsYtExporter;
    @Autowired
    @YtHahn
    private UsedPromocodesYtExporter usedPromocodesYtExporter;
    @Autowired
    @YtHahn
    private CreatedCouponsYtExporter createdCouponsYtExporter;
    @Autowired
    @YtHahn
    private AllPromosYtExporter allPromosYtExporter;
    @Autowired
    @YtHahn
    private CoinHistoryWithOrdersYtExporter coinHistoryWithOrdersYtExporter;
    @Autowired
    @YtHahn
    private DiscountHistoryWithOrdersYtExporter discountHistoryWithOrdersYtExporter;
    @Autowired
    @YtHahn
    private PromoAuditTableYtExporter promoAuditTableYtExporter;
    @Autowired
    @YtHahn
    private CoinDescriptionTableYtExporter coinDescriptionTableYtExporter;
    @Autowired
    @YtHahn
    private AccountTableYtExporter accountTableYtExporter;
    @Autowired
    @YtHahn
    private FraudCoinDisposeQueueYtExporter fraudCoinDisposeQueueYtExporter;
    @Autowired
    @YtHahn
    private CouponValueYtExporter couponValueYtExporter;
    @Autowired
    private ArchivationStateDao archivationStateDao;
    @Autowired
    private YPath basePath;
    @Autowired
    @YtHahn
    private PromoYtExporter promoYtExporter;
    @Autowired
    @YtHahn
    private PromocodeCoinPromoYtExporter promocodeCoinPromoYtExporter;

    @Autowired
    private ProhibitedTriggerEventDryRunCoinsDao prohibitedTriggerEventDryRunCoinsDao;
    @Autowired
    @YtHahn
    private ProhibitedTriggerEventDryRunCoinsTableYtExporter prohibitedTriggerEventDryRunCoinsTableYtExporter;
    @Autowired
    @YtHahn
    private YandexWalletTransactionsYtExporter yandexWalletTransactionsYtExporter;
    @Autowired
    @YtHahn
    private YandexWalletTransactionExternalYtExporter yandexWalletTransactionsExternalYtExporter;
    @Autowired
    @YtHahn
    private YandexWalletTransactionExternalExtendedYtExporter yandexWalletTransactionsExternalExtendedYtExporter;
    @Autowired
    protected TestAuthorizationContext authorizationContext;
    @Autowired
    private YtTestHelper ytTestHelper;
    @Autowired
    private YtExporter exporter;
    @Autowired
    private DiscountUtils discountUtils;
    @Autowired
    private ConfigurationService configurationService;
    @Autowired
    private YandexWalletTransactionDao yandexWalletTransactionDao;
    @Autowired
    private YandexWalletTopUpProcessor yandexWalletTopUpProcessor;
    @Autowired
    @YtHahn
    private PartnerCashbackStandardPromoPropertiesTableExternalYtExporter partnerCashbackStandardPromoPropertiesTableExternalYtExporter;
    @Autowired
    @YtHahn
    private PartnerCashbackStandardPromoHidsTableExternalYtExporter partnerCashbackStandardPromoHidsTableExternalYtExporter;
    @Autowired
    @YtHahn
    private PartnerCashbackPropertiesTableExternalYtExporter partnerCashbackPropertiesTableExternalYtExporter;
    @Autowired
    private PromoYtUpdateProcessor promoYtUpdateProcessor;
    @Autowired
    private YandexWalletTransactionReplicationHahnProcessor yandexWalletTransactionReplicationHahnProcessor;
    @Autowired
    private DeferredMetaTransactionService deferredMetaTransactionService;

    @Value("${market.loyalty.yt.promo.url}")
    String ytPathString;

    @Before
    public void initAuthorizationContext() {
        authorizationContext.setUserName(TestAuthorizationContext.DEFAULT_USER_NAME);
    }

    /**
     * для валидности запросов в выгрузках нужно, чтобы таблицы data transfer (созданные извне) тоже существовали
     */
    @Before
    public void createDataTransferTables() {
        Arrays.stream(DataTransferYtTable.values()).forEach(dtYtTable -> {
            YPath path = basePath.child(dtYtTable.getTablePath());
            if (!ytClient.exists(path)) {
                ytClient.createTable(
                        path,
                        DATA_TRANSFER_TABLE_DESCRIPTIONS.get(dtYtTable).toSchema(true)
                );
            }
        });
    }

    @Before
    public void setUp() {
        ytClient.remove(basePath);
        configurationService.set(ConfigurationService.DISABLE_DEPRECATED_YT_EXPORTERS, "false");
    }

    @Test
    public void testNewPromoFieldExportToYt() {
        YPath ytPath = YPath.simple(ytPathString);
        promoManager.createSmartShoppingPromo(
                PromoUtils.SmartShopping.defaultFixedPromocode()
                        .setCode("test_code_okkon811")
                        .addPromoRule(DONT_USE_WITH_BUNDLES_FILTER_RULE)
                        .addPromoRule(DONT_USE_WITH_CHEAPEST_AS_GIFT_FILTER_RULE)
                        .addCoinRule(MSKU_FILTER_RULE, MSKU_ID, ImmutableSet.of("1", "2", "3"))
                        .setAdditionalConditionsText("Проверка_поля_conditions")
                        .setEmissionBudget(BigDecimal.valueOf(200))
                        .setBudget(BigDecimal.valueOf(200).multiply(DEFAULT_COIN_FIXED_NOMINAL))
        );
        promocodeCoinPromoYtExporter.exportToYt();
        assertTrue(ytClient.exists(ytPath));
    }

    @Test
    public void testCashbackAndSecretSalesExportToYt() {
        YPath ytPath = YPath.simple(ytPathString);
        promoManager.createCashbackPromo(
                PromoUtils.Cashback.defaultPercent(BigDecimal.TEN)
                        .setDescription("cashback promo description")
                        .addCashbackRule(MAX_CASHBACK_FILTER_RULE, MAX_CASHBACK, BigDecimal.valueOf(1234))
                        .addCashbackRule(PERKS_ALLOWED_CUTTING_RULE, PERK_TYPE, PerkType.YANDEX_CASHBACK)
                        .addCashbackRule(CATEGORY_FILTER_RULE, CATEGORY_ID, ImmutableSet.of(PARENT_CATEGORY_ID))
        );
        promoYtExporter.exportToYt();
        assertTrue(ytClient.exists(ytPath));
    }

    @Test
    public void testDumpCoinTableData() {

        final Promo promoForMergeTag = promoManager.createSmartShoppingPromo(PromoUtils.SmartShopping.defaultFixed());
        final Promo freeDeliveryPromo = promoManager.createSmartShoppingPromo(
                defaultFreeDelivery()
                        .setPromoKey("testKey")
        );
        configurationService.configureFreeDeliveryPromo(freeDeliveryPromo);
        promoUtils.reloadFreeDeliveryPromosCache();
        ytTestHelper.setupMergeTagForPromo(promoForMergeTag, "test_merge_tag");
        ytTestHelper.setupMergeTagForPromo(freeDeliveryPromo, "free_delivery_merge_tag");

        Promo promoForOrder = promoManager.createSmartShoppingPromo(
                PromoUtils.SmartShopping.defaultFixed()
                        .setEmissionBudget(BigDecimal.valueOf(200))
                        .setBudget(BigDecimal.valueOf(200).multiply(DEFAULT_COIN_FIXED_NOMINAL))
                        .setCoinCreationReason(ORDER)
        );

        Promo partnerPromo = promoManager.createSmartShoppingPromo(
                PromoUtils.SmartShopping.defaultFixed()
                        .setEmissionBudget(BigDecimal.valueOf(200))
                        .setBudget(BigDecimal.valueOf(200).multiply(DEFAULT_COIN_FIXED_NOMINAL))
                        .setCoinCreationReason(PARTNER)
                        .setAnalyticName("Специальная акция")
                        .setPartnerName("Сбер")
        );
        budgetModeService.reloadCache();
        clidCacheService.reloadCache();
        deferredMetaTransactionService.consumeBatchOfTransactions(10); // to update account balances in db

        coinService.create.createCoin(
                promoForMergeTag, defaultAuth(123).setReasonParam("merge_tag_reason").setReason(ORDER).build());
        coinService.create.createCoin(promoForOrder, defaultNoAuth().setReasonParam("1234").setReason(ORDER).build());
        coinService.create.createCoin(
                promoForOrder, defaultNoAuth().setReasonParam("1234-1234").setReason(ORDER).build());

        for (int i = 0; i < 147; i++) {
            CoinKey coinKey = coinService.create.createCoin(partnerPromo, defaultAuth().setReason(PARTNER).build());
            var discountResponse = discountService.spendDiscount(
                    DiscountRequestWithBundlesBuilder.builder(
                            orderRequestWithBundlesBuilder().withOrderItem().build(),
                            orderRequestWithBundlesBuilder().withOrderItem().build()
                    ).withCoins(coinKey).build(),
                    configurationService.currentPromoApplicabilityPolicy(),
                    null
            );

            assertThat(discountResponse.getCoinErrors(), empty());
        }

        clock.spendTime(Duration.ofSeconds(2));

        coinsTableYtExporter.exportToYt();
        coinNoAuthTableYtExporter.exportToYt();
        coinHistoryTableYtExporter.exportToYt();
        discountTableYtExporter.exportToYt();
        discountNoAuthTableYtExporter.exportToYt();
        coinPropsTableYtExporter.exportToYt();
        coinDescriptionTableYtExporter.exportToYt();
        promoTableYtExporter.exportToYt();
        promoParamsTableYtExporter.exportToYt();
        promoMergeTagYtExporter.exportToYt();
        activeAuthCoinsYtExporter.exportToYt();
        discountHistoryTableYtExporter.exportToYt();
        coinHistoryExternalYtExporter.exportToYt();
        coinHistoryXTableYtExporter.exportToYt();
        discountHistoryXTableYtExporter.exportToYt();
        metaTransactionTableYtExporter.exportToYt();
        transactionTableYtExporter.exportToYt();

        clock.spendTime(Duration.ofMinutes(1));

        coinHistoryWithOrdersYtExporter.exportToYt();
        discountHistoryWithOrdersYtExporter.exportToYt();

        clock.spendTime(Duration.ofMinutes(1));

        createdCoinsYtExporter.exportToYt();
        usedCoinsYtExporter.exportToYt();
    }

    @Test
    public void testDumpPromocodeTableData() {

        final Promo promoForMergeTag = promoManager.createPromocodePromo(
                PromoUtils.SmartShopping.defaultFixedPromocode()
                        .setBudget(BigDecimal.valueOf(10000000))
                        .setEmissionBudget(BigDecimal.valueOf(100000))
                        .setCode("PROMOCODE"));
        ytTestHelper.setupMergeTagForPromo(promoForMergeTag, "test_merge_tag");
        deferredMetaTransactionService.consumeBatchOfTransactions(10); // to update account balances in db

        for (int i = 0; i < 1; i++) {
            long uid = 100L + i;

            PromocodesActivationResult activationResults = promocodeService.activatePromocodes(
                    PromocodeActivationRequest.builder()
                            .externalPromocodes(Set.of("PROMOCODE"))
                            .userId(uid)
                            .build());

            PromocodeActivationResult activationResult = activationResults.getActivationResults().get(0);

            assertThat(
                    activationResult.getActivationResultCode(),
                    is(PromocodeActivationResultCode.SUCCESS)
            );

            MultiCartWithBundlesDiscountResponse discountResponse = discountService.spendDiscount(
                    DiscountRequestWithBundlesBuilder.builder(
                            orderRequestWithBundlesBuilder().withOrderItem().build(),
                            orderRequestWithBundlesBuilder().withOrderItem().build())
                            .withCoupon("PROMOCODE")
                            .withOperationContext(OperationContextFactory.uidOperationContextDto(uid))
                            .build(),
                    configurationService.currentPromoApplicabilityPolicy(),
                    null
            );

            assertThat(discountResponse.getPromocodeErrors(), empty());
            assertThat(discountResponse.getCouponError(), nullValue());
        }


        clock.spendTime(Duration.ofSeconds(2));

        coinsTableYtExporter.exportToYt();
        coinNoAuthTableYtExporter.exportToYt();
        discountTableYtExporter.exportToYt();
        discountNoAuthTableYtExporter.exportToYt();
        coinPropsTableYtExporter.exportToYt();
        coinDescriptionTableYtExporter.exportToYt();
        promoTableYtExporter.exportToYt();
        promoParamsTableYtExporter.exportToYt();
        promoMergeTagYtExporter.exportToYt();
        activeAuthCoinsYtExporter.exportToYt();
        coinHistoryTableYtExporter.exportToYt();
        discountHistoryTableYtExporter.exportToYt();
        coinHistoryExternalYtExporter.exportToYt();
        coinHistoryXTableYtExporter.exportToYt();
        discountHistoryXTableYtExporter.exportToYt();
        metaTransactionTableYtExporter.exportToYt();
        transactionTableYtExporter.exportToYt();

        clock.spendTime(Duration.ofMinutes(1));

        coinHistoryWithOrdersYtExporter.exportToYt();
        discountHistoryWithOrdersYtExporter.exportToYt();

        clock.spendTime(Duration.ofMinutes(1));

        createdPromocodesYtExporter.exportToYt();
        usedPromocodesYtExporter.exportToYt();
    }

    @Test
    public void testDumpCouponTableData() {
        Promo promo = promoManager.createCouponPromo(
                PromoUtils.Coupon.defaultSingleUse()
                        .setEmissionBudget(BigDecimal.valueOf(200))
                        .setBudget(BigDecimal.valueOf(200).multiply(DEFAULT_COUPON_VALUE))
        );
        deferredMetaTransactionService.consumeBatchOfTransactions(10); // to update account balances in db

        RulePayloads<?> rulePayloads = discountUtils.getRulesPayload();

        for (int i = 0; i < 147; i++) {
            String coupon = couponService.createOrGetCoupon(
                    CouponCreationRequest.builder(UUID.randomUUID().toString(), promo.getId())
                            .forceActivation(true)
                            .build(), rulePayloads
            ).getCode();
            discountService.spendDiscount(
                    DiscountRequestWithBundlesBuilder.builder(
                            orderRequestWithBundlesBuilder().withOrderItem().build(),
                            orderRequestWithBundlesBuilder().withOrderItem().build()
                    ).withCoupon(coupon).build(),
                    configurationService.currentPromoApplicabilityPolicy(),
                    null
            );
        }

        clock.spendTime(Duration.ofSeconds(2));

        couponTableYtExporter.exportToYt();
        promoTableYtExporter.exportToYt();
        promoParamsTableYtExporter.exportToYt();
        couponHistoryTableYtExporter.exportToYt();
        couponHistoryExternalYtExporter.exportToYt();
        couponHistoryXTableYtExporter.exportToYt();
        metaTransactionTableYtExporter.exportToYt();
        transactionTableYtExporter.exportToYt();
        operationContextTableYtExporter.exportToYt();

        clock.spendTime(Duration.ofMinutes(1));

        couponValueYtExporter.exportToYt();

        clock.spendTime(Duration.ofMinutes(1));

        createdCouponsYtExporter.exportToYt();
        usedCouponsYtExporter.exportToYt();
    }

    @Test
    public void testDumpPromoTableData() {
        promoManager.createCouponPromo(
                PromoUtils.Coupon.defaultSingleUse()
                        .setEmissionBudget(BigDecimal.valueOf(200))
                        .setBudget(BigDecimal.valueOf(200).multiply(DEFAULT_COUPON_VALUE))
        );

        promoManager.createSmartShoppingPromo(
                PromoUtils.SmartShopping.defaultFixed()
                        .setEmissionBudget(BigDecimal.valueOf(200))
                        .setBudget(BigDecimal.valueOf(200).multiply(DEFAULT_COIN_FIXED_NOMINAL))
        );


        clock.spendTime(Duration.ofSeconds(2));

        promoTableYtExporter.exportToYt();
        promoAuditTableYtExporter.exportToYt();
        promoParamsTableYtExporter.exportToYt();
        coinPropsTableYtExporter.exportToYt();
        coinDescriptionTableYtExporter.exportToYt();
        accountTableYtExporter.exportToYt();

        clock.spendTime(Duration.ofMinutes(1));

        couponValueYtExporter.exportToYt();

        clock.spendTime(Duration.ofMinutes(1));

        allPromosYtExporter.exportToYt();
    }

    @Test
    public void testDumpFraudCoinDisposeQueueData() {
        Promo promoForOrder = promoManager.createSmartShoppingPromo(
                PromoUtils.SmartShopping.defaultFixed()
                        .setEmissionBudget(BigDecimal.valueOf(200))
                        .setBudget(BigDecimal.valueOf(200).multiply(DEFAULT_COIN_FIXED_NOMINAL))
                        .setCoinCreationReason(ORDER)
        );


        final long uid1 = 100000000L;
        final long uid2 = 100000001L;

        final CoinKey coin1 = coinService.create.createCoin(
                promoForOrder, defaultAuth(uid1).setReasonParam("1234").setReason(ORDER).build());

        final CoinKey coin2 = coinService.create.createCoin(
                promoForOrder, defaultAuth(uid2).setReasonParam("1234").setReason(ORDER).build());


        fraudCoinDisposeQueueDao.addCoinToQueue(
                null, Collections.singletonList(
                        new FraudCoinDisposeQueueRecord(coin1, Arrays.asList(new CoinKey(10001), new CoinKey(10002)),
                                uid1)),
                FraudCoinDisposeStatus.TEST
        );
        fraudCoinDisposeQueueDao.addCoinToQueue(
                null, Collections.singletonList(
                        new FraudCoinDisposeQueueRecord(coin2, Arrays.asList(new CoinKey(10004), new CoinKey(10005)),
                                uid2)),
                FraudCoinDisposeStatus.SUCCESS
        );
        fraudCoinDisposeQueueYtExporter.exportToYt();
    }

    @Test
    public void testDumpPreparedCoinsData() {
        setupCoinDataCreatedByPreparedRequest();

        bunchRequestProcessor.processPreparedRequests(Duration.of(1, MINUTES));
    }

    @Test
    public void testCreateEmptyTableWithoutValidTransactions() throws BunchRequestException {
        final String testAccrualKey1 = "testAccrualKey1";
        final String testAccrualKey2 = "testAccrualKey2";
        long requestId1 = setupYandexWalletBunchGenerationRequest(testAccrualKey1, true);
        long requestId2 = setupYandexWalletBunchGenerationRequest(testAccrualKey2, false);
        final BunchGenerationRequest request1 = bunchRequestService.getRequest(requestId1);
        final BunchGenerationRequest request2 = bunchRequestService.getRequest(requestId2);

        exporter.export(request1);
        exporter.export(request2);

        final YPath firstTablePath = YPath.simple(
                request1.getParam(OUTPUT_TABLE).get()
        );
        final YPath secondTablePath = YPath.simple(
                request2.getParam(OUTPUT_TABLE).get()
        );

        assertThat(ytClient.exists(firstTablePath), equalTo(true));
        assertThat(ytClient.exists(secondTablePath), equalTo(true));
    }

    private long setupYandexWalletBunchGenerationRequest(String requestKey, boolean createTransactions) {
        final Promo promo = promoManager.createAccrualPromo(
                PromoUtils.WalletAccrual.defaultModelAccrual()
        );
        final long requestId = bunchRequestService.scheduleRequest(BunchGenerationRequest.scheduled(
                promo.getId(),
                requestKey,
                6,
                null,
                TableFormat.YT,
                null, GeneratorType.YANDEX_WALLET,
                ImmutableMap.<BunchGenerationRequestParamName<?>, String>builder()
                        .put(
                                INPUT_TABLE,
                                "//home/market/testing/market-promo/bunch_request/input" +
                                        "/LILUCRM3280_smoke4_issue_cashback_64f5d03f"
                        )
                        .put(INPUT_TABLE_CLUSTER, "hahn")
                        .put(CAMPAIGN_NAME, "test_marketdiscount_4621")
                        .put(
                                OUTPUT_TABLE,
                                "//home/market/testing/market-promo/bunch_request/output/MARKETDISCOUNT-4621-test" +
                                        "-telerman-" + requestKey
                        )
                        .put(OUTPUT_TABLE_CLUSTER, "hahn")
                        .put(ERROR_OUTPUT_TABLE, "//home/market/testing/market-promo/bunch_request/output" +
                                "/MARKETDISCOUNT-4621-test-telerman-error-" + requestKey)
                        .build()
        ));

        if (createTransactions) {
            final List<YandexWalletNewTransaction> transactions = LongStream.range(0, 6)
                    .mapToObj(id -> new YandexWalletNewTransaction(id, BigDecimal.ONE, String.valueOf(
                            100 + id), "", "productId", null))
                    .collect(Collectors.toList());
            yandexWalletTransactionDao.enqueueTransactions(
                    requestId, "test", transactions, YandexWalletTransactionPriority.LOW);
            yandexWalletTransactionDao.queryAll()
                    .forEach(transaction -> {
                        yandexWalletTransactionDao.updateStatus(
                                transaction.getId(), transaction.getStatus(), YandexWalletTransactionStatus.CONFIRMED,
                                clock.instant()
                        );
                    });
        }
        bunchGenerationRequestDao.markRequestPrepared(requestId);
        return requestId;
    }

    private void setupCoinDataCreatedByPreparedRequest() {
        final Promo promo = promoManager.createSmartShoppingPromo(
                PromoUtils.SmartShopping.defaultFixed()
        );
        final String requestKey = "coin";

        bunchRequestService.scheduleRequest(BunchGenerationRequest.scheduled(
                promo.getId(),
                requestKey,
                10,
                null,
                TableFormat.YT,
                null,
                GeneratorType.COIN,
                ImmutableMap.<BunchGenerationRequestParamName<?>, String>builder()
                        .put(COIN_GENERATOR_TYPE, CoinGeneratorType.NO_AUTH.getCode())
                        .put(OUTPUT_TABLE, "//home/market/testing/market-promo/bunch_request/output" +
                                "/fonar101_test_output_3")
                        .put(OUTPUT_TABLE_CLUSTER, "hahn")
                        .build()
                )
        );

        bunchRequestService.processScheduledRequests(
                500,
                Duration.of(1, MINUTES),
                GeneratorType.COIN
        );
    }

    @Test
    public void testDryRunCoinsExport() {
        for (int i = 0; i < 100; i++) {
            prohibitedTriggerEventDryRunCoinsDao.saveDryRunCoins(
                    new DryRunCoinsRecord(
                            i,
                            DEFAULT_ORDER_ID,
                            DEFAULT_MULTI_ORDER_ID,
                            DEFAULT_UID,
                            null,
                            null,
                            null,
                            Collections.singletonList(
                                    new DryRunCoin(
                                            10_000L,
                                            CoreCoinType.FIXED,
                                            BigDecimal.valueOf(100)
                                    )
                            )
                    ));
        }
        prohibitedTriggerEventDryRunCoinsTableYtExporter.exportToYt();
    }

    @Test
    public void testYandexWalletExportToYt() throws InterruptedException {
        Promo promo = promoManager.createSmartShoppingPromo(
                PromoUtils.SmartShopping.defaultFixed()
        );
        final long requestId = bunchRequestService.scheduleRequest(
                createTestScheduledRequest(
                        promo.getId(),
                        "test_key",
                        GeneratorType.YANDEX_WALLET,
                        null,
                        100
                )
        );
        yandexWalletTransactionDao.enqueueTransactions(requestId, "test_stock",
                createWalletTopUps(1), YandexWalletTransactionPriority.LOW);
        yandexWalletTopUpProcessor.yandexWalletTransactionsProcess(Duration.ofMinutes(1), 10);
        clock.spendTime(10, MINUTES);
        yandexWalletTransactionsYtExporter.exportToYt();
        yandexWalletTransactionsExternalYtExporter.exportToYt();
        yandexWalletTransactionsExternalExtendedYtExporter.exportToYt();
    }

    @Test
    public void testPartnerCashbackExportToYt() {
        partnerCashbackVersionDao.save(
                VersionEntry.builder()
                        .setId(0)
                        .setStatus(ACTIVE)
                        .setComment("initial")
                        .setCustomCashbackPromoBucketName("test")
                        .setStandardCashbackPromoBucketName("test")
                        .setCustomCashbackPromoPriorityHigh(10000)
                        .setCustomCashbackPromoPriorityLow(0)
                        .build()
        );
        categoryTypeGroupReferenceDao.save(
                CategoryTypeGroupEntry.builder()
                        .setName("default")
                        .build()
        );
        categoryTypeReferenceDao.save(
                CategoryTypeEntry.builder()
                        .setHid(1000)
                        .setCategoryTypeGroupName("default")
                        .build()
        );
        categoryTypeGroupTariffReferenceDao.save(
                CategoryTypeGroupTariffEntry.builder()
                        .setCategoryTypeGroupName("default")
                        .setDeleted(false)
                        .setMinCashbackNominal(BigDecimal.valueOf(1))
                        .setMaxCashbackNominal(BigDecimal.valueOf(10))
                        .setPartnerCashbackVersionId(10000)
                        .setMarketTariff(BigDecimal.valueOf(1.3))
                        .build()
        );
        partnerCashbackStandardPromoDao.save(StandardPromoEntry.builder()
                .setCodeName("default")
                .setCategoryTypeReferenceGroupName("default")
                .setDefaultCashbackNominal(BigDecimal.valueOf(5))
                .setDescription("")
                .setPartnerCashbackVersionId(10000)
                .setPriority(10)
                .build());

        partnerCashbackVersionYtExporter.exportToYt();
        categoryTypeGroupReferenceYtExporter.exportToYt();
        categoryTypeReferenceYtExporter.exportToYt();
        categoryTypeGroupTariffReferenceYtExporter.exportToYt();
        partnerCashbackStandardPromoYtExporter.exportToYt();
        partnerCashbackPropertiesTableExternalYtExporter.exportToYt();
        partnerCashbackStandardPromoHidsTableExternalYtExporter.exportToYt();
        partnerCashbackStandardPromoPropertiesTableExternalYtExporter.exportToYt();
    }

    @Test
    public void testImportPromos() {
        promoYtUpdateProcessor.updatePromoFromYt();
        // поставить брейкпоинт и посмотреть что в базе
    }

    @Test
    public void testCreateDynamicTable() throws Exception {
        Promo promo = promoManager.createSmartShoppingPromo(
                PromoUtils.SmartShopping.defaultFixed()
        );
        yandexWalletTransactionDao.enqueueTransactions(null, "test_stock",
                createWalletTopUps(1), null, null, promo.getId(), YandexWalletTransactionPriority.LOW);

        yandexWalletTransactionReplicationHahnProcessor.replication(Duration.ofMinutes(5));

        List<YandexWalletTransaction> all = yandexWalletTransactionDao.findAll();


        for (YandexWalletTransaction t : all) {
            yandexWalletTransactionDao.updateStatus(t.getId(), t.getStatus(), YandexWalletTransactionStatus.CANCELLED, null);

        }

        yandexWalletTransactionReplicationHahnProcessor.replication(Duration.ofMinutes(5));
    }
}
