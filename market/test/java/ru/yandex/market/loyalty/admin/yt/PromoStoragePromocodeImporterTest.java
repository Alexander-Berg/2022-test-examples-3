package ru.yandex.market.loyalty.admin.yt;

import Market.DataCamp.DataCampOfferMeta;
import Market.DataCamp.DataCampPromo;
import NMarketIndexer.Common.Common;
import com.google.protobuf.Timestamp;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.loyalty.admin.test.MarketLoyaltyAdminMockedDbTest;
import ru.yandex.market.loyalty.admin.yt.service.PromoStoragePromoImporter;
import ru.yandex.market.loyalty.api.model.PromoStatus;
import ru.yandex.market.loyalty.core.dao.coin.CoinDao;
import ru.yandex.market.loyalty.core.dao.promocode.PromocodeEntryDao;
import ru.yandex.market.loyalty.core.model.coin.CoinProps;
import ru.yandex.market.loyalty.core.model.promo.Promo;
import ru.yandex.market.loyalty.core.model.promo.PromoParameterName;
import ru.yandex.market.loyalty.core.model.promocode.PromocodePromoDescription;
import ru.yandex.market.loyalty.core.rule.RulesContainer;
import ru.yandex.market.loyalty.core.service.ConfigurationService;
import ru.yandex.market.loyalty.core.service.PromoService;
import ru.yandex.market.loyalty.core.service.datacamp.DataCampStrollerClient;
import ru.yandex.market.loyalty.core.service.promocode.PromocodeService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Set;
import java.util.stream.Collectors;

import static Market.DataCamp.DataCampOfferMeta.DataSource.MARKET_MBI;
import static Market.DataCamp.DataCampOfferMeta.DataSource.UNKNOWN_SOURCE;
import static Market.DataCamp.DataCampPromo.BusinessMigrationInfo.Status.NEW_BUSINESS;
import static Market.DataCamp.DataCampPromo.BusinessMigrationInfo.Status.OLD_BUSINESS;
import static Market.DataCamp.DataCampPromo.PromoAdditionalInfo.Compensation.MERCHANT;
import static Market.DataCamp.DataCampPromo.PromoAdditionalInfo.PromoPurpose.GMV_GENERATION;
import static Market.DataCamp.DataCampPromo.PromoAdditionalInfo.PromoStatus.RUNNING;
import static Market.DataCamp.DataCampPromo.PromoAdditionalInfo.StrategyType.CATEGORY;
import static Market.DataCamp.DataCampPromo.PromoMechanics.MarketPromocode.ApplyingType.REUSABLE;
import static Market.DataCamp.DataCampPromo.PromoMechanics.MarketPromocode.DiscountType.PERCENTAGE;
import static Market.DataCamp.DataCampPromo.PromoType.BLUE_PROMOCODE;
import static Market.DataCamp.DataCampPromo.PromoType.MARKET_BONUS;
import static Market.DataCamp.DataCampPromo.PromoType.MARKET_PROMOCODE;
import static NMarket.Common.Promo.Promo.ESourceType.ANAPLAN;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.emptyIterable;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.iterableWithSize;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static ru.yandex.market.loyalty.core.model.promo.PromoParameterName.ANAPLAN_ID;
import static ru.yandex.market.loyalty.core.model.promo.PromoParameterName.BUSINESS_ID;
import static ru.yandex.market.loyalty.core.model.promo.PromoParameterName.BUSINESS_MIGRATION_INFO_STATUS;
import static ru.yandex.market.loyalty.core.rule.RuleType.FIRST_ORDER_CUTTING_RULE;
import static ru.yandex.market.loyalty.core.service.ConfigurationService.NOT_DOWNLOAD_BONUS_AND_PROMOCODE_FROM_ANAPLAN;

public class PromoStoragePromocodeImporterTest extends MarketLoyaltyAdminMockedDbTest {
    private static final String SOME_PROMO_ID = "#1111";
    private static final String SOME_PROMO_ID_2 = "#2222";
    private static final String SOME_PROMO_ID_KIDS = "#91885";
    private static final String NEW_PROMO_KEY = "new_promo_key";
    private static final long BUDGET = 10000;
    private static final long UPDATED_BUDGET = 15000;
    private static final long REGION = 1;
    private static final long EXCLUDED_REGION = 2;
    private static final long CLID = 3;
    private static final String PROMOCODE = "PROMO1-AF";
    private static final String PROMOCODE_KIDS = "PROMOTESTCODE_KIDS3";
    private static final DataCampPromo.PromoDescription PARENT_PROMO =
            DataCampPromo.PromoDescription.newBuilder()
                    .setPrimaryKey(DataCampPromo.PromoDescriptionIdentifier.newBuilder()
                            .setPromoId(SOME_PROMO_ID)
                            .setBusinessId(1)
                            .setSource(NMarket.Common.Promo.Promo.ESourceType.AFFILIATE)
                            .build()
                    )
                    .setPromoGeneralInfo(DataCampPromo.PromoGeneralInfo.newBuilder()
                            .setPromoType(DataCampPromo.PromoType.PARENT_PROMO)
                            .build()
                    )
                    .setConstraints(DataCampPromo.PromoConstraints.newBuilder()
                            .setStartDate(LocalDate.of(2021, 5, 1)
                                    .atStartOfDay(ZoneId.systemDefault()).toInstant().getEpochSecond())
                            .setEndDate(LocalDate.of(2021, 5, 31)
                                    .atStartOfDay(ZoneId.systemDefault()).toInstant().getEpochSecond())
                            .setMoneyLimit(NMarketIndexer.Common.Common.PriceExpression.newBuilder()
                                    .setPrice(BUDGET)
                                    .build()
                            )
                            .setEnabled(true)
                            .addOffersMatchingRules(DataCampPromo.PromoConstraints.OffersMatchingRule.newBuilder()
                                    .setOrigionalCategoryRestriction(
                                            DataCampPromo.PromoConstraints.OffersMatchingRule.OriginalCategoryRestriction.newBuilder()
                                                    .addIncludeCategegoryRestriction(DataCampPromo.PromoConstraints.OffersMatchingRule.PromoCategory.newBuilder()
                                                            .setId(111L))))
                    )
                    .build();

    private static final DataCampPromo.PromoDescription CHILD_AFFILIATE_PROMO =
            DataCampPromo.PromoDescription.newBuilder()
                    .setPrimaryKey(DataCampPromo.PromoDescriptionIdentifier.newBuilder()
                            .setPromoId(SOME_PROMO_ID_2)
                            .setBusinessId(1)
                            .setSource(NMarket.Common.Promo.Promo.ESourceType.AFFILIATE)
                            .build()
                    )
                    .setConstraints(PARENT_PROMO.getConstraints())
                    .setPromoGeneralInfo(DataCampPromo.PromoGeneralInfo.newBuilder()
                            .setPromoType(DataCampPromo.PromoType.BLUE_PROMOCODE)
                            .build()
                    )
                    .setAdditionalInfo(DataCampPromo.PromoAdditionalInfo.newBuilder()
                            .setParentPromoId(SOME_PROMO_ID)
                            .build())
                    .setMechanicsData(DataCampPromo.PromoMechanics.newBuilder().setBluePromocode(
                            DataCampPromo.PromoMechanics.BluePromocode.newBuilder()
                                    .setClid(CLID)
                                    .setPromoCode(PROMOCODE)
                                    .setBucketMinPrice(500)
                                    .setBucketMaxPrice(5000)
                                    .setOneOrderPromocode(false)
                                    .setWithoutUploadingToKM(true)
                                    .setAdditionalConditionsText("при стоимости корзины от 500 до 5000 р")
                                    .setTypePromocode(DataCampPromo.PromoMechanics.BluePromocode.PromocodeType.VALUE)
                                    .setPromocodeRatingRub(200)
                                    .setBudgetRub(BUDGET)
                                    .setBudgetThreshold(0)
                    ))
                    .build();

    @Autowired
    private PromoStoragePromoImporter importer;
    @Autowired
    private PromoYtTestHelper promoStorageTestHelper;
    @Autowired
    private PromoService promoService;
    @Autowired
    private PromocodeService promocodeService;
    @Autowired
    private PromocodeEntryDao promocodeEntryDao;
    @Autowired
    protected DataCampStrollerClient dataCampStrollerClient;
    @Autowired
    protected CoinDao coinDao;
    @Captor
    private ArgumentCaptor<DataCampPromo.PromoDescription> argumentCaptor;

    private DataCampPromo.PromoDescription description;
    private DataCampPromo.PromoDescription descriptionUpdate;
    private DataCampPromo.PromoDescription descriptionUpdateInactive;
    private DataCampPromo.PromoDescription descriptionKids;
    private DataCampPromo.PromoDescription descriptionWithEmptyPromoId;
    private DataCampPromo.PromoDescription descriptionWithMarketBonus;
    private DataCampPromo.PromoDescription descriptionWithBluePromocode;
    private String promoCode;

    @Override
    @Before
    public void initMocks() {
        MockitoAnnotations.initMocks(this);
        ZonedDateTime current = clock.dateTime().atZone(clock.getZone());

        promoCode = promocodeService.generateNewPromocode();

        description = DataCampPromo.PromoDescription.newBuilder()
                .setAdditionalInfo(DataCampPromo.PromoAdditionalInfo.newBuilder()
                        .setCompensation(DataCampPromo.PromoAdditionalInfo.Compensation.MARKET)
                        .setComment("Comment")
                        .setCreatedAt(current.minusDays(2).toEpochSecond())
                        .setStatus(RUNNING)
                        .setUpdatedAt(current.minusDays(1).toEpochSecond())
                        .setLendingUrl("landing url")
                        .setRulesUrl("url")
                        .setName("Promocode promo")
                        .setParentPromoId("Parent promo id")
                        .build())
                .setMechanicsData(DataCampPromo.PromoMechanics.newBuilder()
                        .setMarketPromocode(DataCampPromo.PromoMechanics.MarketPromocode.newBuilder()
                                .setDescription("description")
                                .setPromoCode(promoCode)
                                .setApplyingType(DataCampPromo.PromoMechanics.MarketPromocode.ApplyingType.ONE_TIME)
                                .setBudgetThreshold(5000)
                                .setTypeClient("IOS")
                                .setDiscountType(DataCampPromo.PromoMechanics.MarketPromocode.DiscountType.VALUE)
                                .setRatingRub(300)
                                .setWithoutDiscount(true)
                                .build())
                        .build())
                .setConstraints(DataCampPromo.PromoConstraints.newBuilder()
                        .setAllowBlueFlash(true)
                        .setAllowCheapestAsGift(false)
                        .setEnabled(true)
                        .setMoneyLimit(NMarketIndexer.Common.Common.PriceExpression.newBuilder().setPrice(BUDGET).build())
                        .setEndDate(current.plusDays(10).toEpochSecond())
                        .setStartDate(current.plusDays(1).toEpochSecond())
                        .setHidden(false)
                        .addOffersMatchingRules(DataCampPromo.PromoConstraints.OffersMatchingRule.newBuilder()
                                .setRegionRestriction(DataCampPromo.PromoConstraints.OffersMatchingRule.RegionRestriction
                                        .newBuilder()
                                        .setRegion(DataCampPromo.PromoConstraints.OffersMatchingRule.IntList
                                                .newBuilder()
                                                .addId(REGION)
                                                .build())
                                        .setExcludedRegion(DataCampPromo.PromoConstraints.OffersMatchingRule.IntList
                                                .newBuilder()
                                                .addId(EXCLUDED_REGION)
                                                .build())
                                        .build())
                                .setCategoryRestriction(DataCampPromo.PromoConstraints
                                        .OffersMatchingRule.CategoryRestriction.newBuilder()
                                        .addPromoCategory(DataCampPromo.PromoConstraints.OffersMatchingRule
                                                .PromoCategory.newBuilder()
                                                .setId(123)
                                                .build())
                                        .build())
                                .setMskuRestriction(DataCampPromo.PromoConstraints.OffersMatchingRule
                                        .MskuRestriction.newBuilder()
                                        .setExcludedMsku(DataCampPromo.PromoConstraints.OffersMatchingRule
                                                .IntList.newBuilder()
                                                .addId(123456)
                                                .build())
                                        .build())
                                .setBrandRestriction(DataCampPromo.PromoConstraints.OffersMatchingRule.BrandRestriction.newBuilder()
                                        .setExcludedBrands(
                                                DataCampPromo.PromoConstraints.OffersMatchingRule.PromoBrands.newBuilder()
                                                        .addBrands(DataCampPromo.PromoBrand.newBuilder()
                                                                .setId(123)
                                                                .setName("Brand")
                                                                .setIsRestriction(true)
                                                                .build())
                                                        .build())
                                        .build())
                                .build()))
                .setPrimaryKey(DataCampPromo.PromoDescriptionIdentifier.newBuilder()
                        .setBusinessId(1)
                        .setPromoId(SOME_PROMO_ID)
                        .setSource(ANAPLAN)
                        .build())
                .setResponsible(DataCampPromo.PromoResponsible.newBuilder()
                        .setAuthor("author")
                        .build())
                .build();

        descriptionUpdate = DataCampPromo.PromoDescription.newBuilder()
                .setAdditionalInfo(DataCampPromo.PromoAdditionalInfo.newBuilder()
                        .setCompensation(DataCampPromo.PromoAdditionalInfo.Compensation.MARKET)
                        .setComment("Comment")
                        .setCreatedAt(current.minusDays(2).toEpochSecond())
                        .setStatus(RUNNING)
                        .setUpdatedAt(current.toEpochSecond())
                        .setLendingUrl("landing url")
                        .setRulesUrl("url")
                        .setName("Promocode promo")
                        .setParentPromoId("Parent promo id")
                        .build())
                .setMechanicsData(DataCampPromo.PromoMechanics.newBuilder()
                        .setMarketPromocode(DataCampPromo.PromoMechanics.MarketPromocode.newBuilder()
                                .setDescription("description")
                                .setPromoCode("PROMOCODE")
                                .setApplyingType(DataCampPromo.PromoMechanics.MarketPromocode.ApplyingType.ONE_TIME)
                                .setBudgetThreshold(5000)
                                .setTypeClient("IOS")
                                .setDiscountType(DataCampPromo.PromoMechanics.MarketPromocode.DiscountType.VALUE)
                                .setRatingRub(300)
                                .setWithoutDiscount(true)
                                .build())
                        .build())
                .setConstraints(DataCampPromo.PromoConstraints.newBuilder()
                        .setAllowBlueFlash(true)
                        .setAllowCheapestAsGift(false)
                        .setEnabled(true)
                        .setMoneyLimit(NMarketIndexer.Common.Common.PriceExpression.newBuilder().setPrice(UPDATED_BUDGET)
                                .build())
                        .setEndDate(current.plusDays(10).toEpochSecond())
                        .setStartDate(current.plusDays(1).toEpochSecond())
                        .setHidden(false)
                        .addOffersMatchingRules(DataCampPromo.PromoConstraints.OffersMatchingRule.newBuilder()
                                .setCategoryRestriction(DataCampPromo.PromoConstraints
                                        .OffersMatchingRule.CategoryRestriction.newBuilder()
                                        .addPromoCategory(DataCampPromo.PromoConstraints.OffersMatchingRule
                                                .PromoCategory.newBuilder()
                                                .setId(123)
                                                .build())
                                        .build())
                                .setMskuRestriction(DataCampPromo.PromoConstraints.OffersMatchingRule
                                        .MskuRestriction.newBuilder()
                                        .setExcludedMsku(DataCampPromo.PromoConstraints.OffersMatchingRule
                                                .IntList.newBuilder()
                                                .addId(123456)
                                                .build())
                                        .build())
                                .setBrandRestriction(DataCampPromo.PromoConstraints.OffersMatchingRule.BrandRestriction.newBuilder()
                                        .setExcludedBrands(
                                                DataCampPromo.PromoConstraints.OffersMatchingRule.PromoBrands.newBuilder()
                                                        .addBrands(DataCampPromo.PromoBrand.newBuilder()
                                                                .setId(123)
                                                                .setName("Brand")
                                                                .setIsRestriction(false)
                                                                .build())
                                                        .build())
                                        .build())))
                .setPrimaryKey(DataCampPromo.PromoDescriptionIdentifier.newBuilder()
                        .setBusinessId(1)
                        .setPromoId(SOME_PROMO_ID)
                        .setSource(ANAPLAN)
                        .build())
                .setResponsible(DataCampPromo.PromoResponsible.newBuilder()
                        .setAuthor("author")
                        .build())
                .build();

        descriptionUpdateInactive = DataCampPromo.PromoDescription.newBuilder()
                .setAdditionalInfo(DataCampPromo.PromoAdditionalInfo.newBuilder()
                        .setCompensation(DataCampPromo.PromoAdditionalInfo.Compensation.MARKET)
                        .setComment("Comment")
                        .setCreatedAt(current.minusDays(2).toEpochSecond())
                        .setStatus(DataCampPromo.PromoAdditionalInfo.PromoStatus.FINISHED)
                        .setUpdatedAt(current.toEpochSecond())
                        .setLendingUrl("landing url")
                        .setRulesUrl("url")
                        .setName("Promocode promo")
                        .setParentPromoId("Parent promo id")
                        .build())
                .setMechanicsData(DataCampPromo.PromoMechanics.newBuilder()
                        .setMarketPromocode(DataCampPromo.PromoMechanics.MarketPromocode.newBuilder()
                                .setDescription("description")
                                .setPromoCode("PROMOCODE")
                                .setApplyingType(DataCampPromo.PromoMechanics.MarketPromocode.ApplyingType.ONE_TIME)
                                .setBudgetThreshold(5000)
                                .setTypeClient("IOS")
                                .setDiscountType(DataCampPromo.PromoMechanics.MarketPromocode.DiscountType.VALUE)
                                .setRatingRub(300)
                                .setWithoutDiscount(true)
                                .build())
                        .build())
                .setConstraints(DataCampPromo.PromoConstraints.newBuilder()
                        .setAllowBlueFlash(true)
                        .setAllowCheapestAsGift(false)
                        .setEnabled(true)
                        .setMoneyLimit(Common.PriceExpression.newBuilder().setPrice(UPDATED_BUDGET).build())
                        .setEndDate(current.plusDays(10).toEpochSecond())
                        .setStartDate(current.plusDays(1).toEpochSecond())
                        .setHidden(false)
                        .addOffersMatchingRules(DataCampPromo.PromoConstraints.OffersMatchingRule.newBuilder()
                                .setCategoryRestriction(DataCampPromo.PromoConstraints
                                        .OffersMatchingRule.CategoryRestriction.newBuilder()
                                        .addPromoCategory(DataCampPromo.PromoConstraints.OffersMatchingRule
                                                .PromoCategory.newBuilder()
                                                .setId(123)
                                                .build())
                                        .build())
                                .setMskuRestriction(DataCampPromo.PromoConstraints.OffersMatchingRule
                                        .MskuRestriction.newBuilder()
                                        .setExcludedMsku(DataCampPromo.PromoConstraints.OffersMatchingRule
                                                .IntList.newBuilder()
                                                .addId(123456)
                                                .build())
                                        .build())
                                .setBrandRestriction(DataCampPromo.PromoConstraints.OffersMatchingRule.BrandRestriction.newBuilder()
                                        .setExcludedBrands(
                                                DataCampPromo.PromoConstraints.OffersMatchingRule.PromoBrands.newBuilder()
                                                        .addBrands(DataCampPromo.PromoBrand.newBuilder()
                                                                .setId(123)
                                                                .setName("Brand")
                                                                .setIsRestriction(false)
                                                                .build())
                                                        .build())
                                        .build())))
                .setPrimaryKey(DataCampPromo.PromoDescriptionIdentifier.newBuilder()
                        .setBusinessId(1)
                        .setPromoId(SOME_PROMO_ID)
                        .setSource(ANAPLAN)
                        .build())
                .setResponsible(DataCampPromo.PromoResponsible.newBuilder()
                        .setAuthor("author")
                        .build())
                .build();

        descriptionKids = DataCampPromo.PromoDescription.newBuilder()
                .setAdditionalInfo(DataCampPromo.PromoAdditionalInfo.newBuilder()
                        .setAdhoc(true)
                        .setAssortmentAutopublication(false)
                        .setAssortmentLimit(6)
                        .setChecklist(DataCampPromo.PromoAdditionalInfo.CheckList.newBuilder()
                                .setApprovedByCd(false)
                                .setApprovedByRg(false)
                                .setAssortmentApproved(false)
                                .setAssortmentReady(false)
                                .setChannelsReady(false)
                                .setDesignReady(false)
                                .setLoyaltyPromoCreated(true)
                                .setMediaplanApproved(false)
                                .build())
                        .setCmLink("https://cm-testing.market.yandex-team.ru/#/promos/redirect-by-anaplan-id/%2391885")
                        .setCompensation(MERCHANT)
                        .setCreatedAt(1642626000L)
                        .setIsTestPromo(true)
                        .setMediaFormats("0")
                        .setMeta(DataCampOfferMeta.UpdateMeta.newBuilder()
                                .setSource(MARKET_MBI)
                                .setTimestamp(Timestamp.newBuilder()
                                        .setNanos(652525000)
                                        .setSeconds(1642838755)
                                        .build())
                                .build())
                        .setName("многразовый для детей 20.01-24.12")
                        .setPromoPurpose(GMV_GENERATION)
                        .setPromotionBudget(0L)
                        .setPublishDatePi(1642626000)
                        .setSendPromoPi(true)
                        .setStartrekRobotTicketLink("")
                        .setStatus(RUNNING)
                        .setStrategyType(CATEGORY)
                        .setUpdatedAt(1642838755)
                        .setWithoutAutolandingGeneration(false)
                        .build())
                .setConstraints(DataCampPromo.PromoConstraints.newBuilder()
                        .setAllowBlueFlash(true)
                        .setAllowBluePromocode(true)
                        .setAllowBlueSet(true)
                        .setAllowCheapestAsGift(true)
                        .setAllowGenericBundle(true)
                        .setAllowMarketBonus(true)
                        .setEnabled(true)
                        .setEndDate(1671915599)
                        .setExcludeDbsSupplier(false)
                        .setHidden(false)
                        .setMeta(DataCampOfferMeta.UpdateMeta.newBuilder()
                                .setSource(MARKET_MBI)
                                .setTimestamp(Timestamp.newBuilder()
                                        .setNanos(652406000)
                                        .setSeconds(1642838755)
                                        .build())
                                .build())
                        .setMoneyLimit(Common.PriceExpression.newBuilder()
                                .setId("RUR")
                                .setPlus(0)
                                .setPrice(10000000)
                                .setRate("1")
                                .setRefId("RUR")
                                .build())
                        .addOffersMatchingRules(DataCampPromo.PromoConstraints.OffersMatchingRule.newBuilder()
                                .setOrigionalCategoryRestriction(DataCampPromo.PromoConstraints.OffersMatchingRule.OriginalCategoryRestriction.newBuilder()
                                        .addIncludeCategegoryRestriction(DataCampPromo.PromoConstraints.OffersMatchingRule.PromoCategory.newBuilder()
                                                .setId(90401)
                                                .setMinDiscount(0))
                                        .setIncludeCategoryRestrictionsCount(1)
                                        .build()))
                        .setQuantityLimit(0)
                        .setStartDate(1642626000)
                        .build())
                .setCurrentBudget(DataCampPromo.CurrentBudget.newBuilder()
                        .setLastUpdatedAt(1642838401)
                        .setMeta(DataCampOfferMeta.UpdateMeta.newBuilder()
                                .setSource(UNKNOWN_SOURCE)
                                .setTimestamp(Timestamp.newBuilder()
                                        .setNanos(180722000)
                                        .setSeconds(1642838401)
                                        .build())
                                .build())
                        .setMoneyLimit(Common.PriceExpression.newBuilder()
                                .setId("RUR")
                                .setPlus(0)
                                .setPrice(10000000)
                                .setRate("1")
                                .setRefId("RUR")
                                .build())
                        .build())
                .setLoyaltyKey(DataCampPromo.LoyaltyKey.newBuilder()
                        .setLoyaltyPromoId(137804)
                        .setLoyaltyPromoKey("Srip9qVwEfTpIVoUoCVbBw")
                        .setMeta(DataCampOfferMeta.UpdateMeta.newBuilder()
                                .setSource(UNKNOWN_SOURCE)
                                .setTimestamp(Timestamp.newBuilder()
                                        .setNanos(180746000)
                                        .setSeconds(1642838401)
                                        .build())
                                .build())
                        .build())
                .setMechanicsData(DataCampPromo.PromoMechanics.newBuilder()
                        .setMarketPromocode(DataCampPromo.PromoMechanics.MarketPromocode.newBuilder()
                                .setApplyingType(REUSABLE)
                                .setBudgetThreshold(0)
                                .setDiscountType(PERCENTAGE)
                                .setEndSpending(1671915599)
                                .setGeneratedPomocode(false)
                                .setNumberOfDaysHours(0)
                                .setOneOrderPromocode(false)
                                .setPromoCode(PROMOCODE_KIDS)
                                .setReserveBudget(false)
                                .setSpentPromocode(false)
                                .setStartSpending(1642626000)
                                .setValue(69)
                                .setWithoutDiscount(false)
                                .setWithoutUploadingToKM(false)
                                .build())
                        .setMeta(DataCampOfferMeta.UpdateMeta.newBuilder()
                                .setSource(MARKET_MBI)
                                .setTimestamp(Timestamp.newBuilder()
                                        .setNanos(652482000)
                                        .setSeconds(1642838755)
                                        .build())
                                .build())
                        .build())
                .setPrimaryKey(DataCampPromo.PromoDescriptionIdentifier.newBuilder()
                        .setBusinessId(0)
                        .setPromoId("#91885")
                        .setSource(ANAPLAN)
                        .build())
                .setPromoGeneralInfo(DataCampPromo.PromoGeneralInfo.newBuilder()
                        .setMeta(DataCampOfferMeta.UpdateMeta.newBuilder()
                                .setSource(MARKET_MBI)
                                .setTimestamp(Timestamp.newBuilder()
                                        .setNanos(652402000)
                                        .setSeconds(1642838755)
                                        .build())
                                .build())
                        .setPromoType(MARKET_PROMOCODE)
                        .build())
                .setResponsible(DataCampPromo.PromoResponsible.newBuilder()
                        .setAuthor("lreshetilo")
                        .setMarcom("Екатерина Веселова")
                        .setMarcomLogin("veselovakate")
                        .setMeta(DataCampOfferMeta.UpdateMeta.newBuilder()
                                .setSource(MARKET_MBI)
                                .setTimestamp(Timestamp.newBuilder()
                                        .setNanos(652384000)
                                        .setSeconds(1642838755)
                                        .build())
                                .build())
                        .setTm("Лейла Решетило")
                        .setTmLogin("lreshetilo")
                        .build())
                .setUpdateInfo(DataCampPromo.UpdateInfo.newBuilder()
                        .setCreatedAt(1642758970)
                        .setMeta(DataCampOfferMeta.UpdateMeta.newBuilder()
                                .setSource(MARKET_MBI)
                                .setTimestamp(Timestamp.newBuilder()
                                        .setNanos(652575000)
                                        .setSeconds(1642838755)
                                        .build())
                                .build())
                        .setUpdatedAt(1642838755)
                        .build())
                .build();

        descriptionWithEmptyPromoId =
                description.toBuilder().setPrimaryKey(DataCampPromo.PromoDescriptionIdentifier.newBuilder()
                .setBusinessId(1)
                .setPromoId("")
                .setSource(ANAPLAN)
                .build()).build();

        descriptionWithMarketBonus = DataCampPromo.PromoDescription.newBuilder()
                .setAdditionalInfo(DataCampPromo.PromoAdditionalInfo.newBuilder().build())
                .setPrimaryKey(DataCampPromo.PromoDescriptionIdentifier.newBuilder()
                        .setBusinessId(1)
                        .setPromoId(SOME_PROMO_ID)
                        .setSource(ANAPLAN)
                        .build())
                .setPromoGeneralInfo(DataCampPromo.PromoGeneralInfo.newBuilder()
                        .setPromoType(MARKET_BONUS)
                        .build())
                .setMechanicsData(DataCampPromo.PromoMechanics.newBuilder()
                        .setMarketBonus(DataCampPromo.PromoMechanics.MarketBonus.newBuilder()
                                .setEndDataCPC(current.plusDays(10).toEpochSecond())
                                .setStartDataCPC(current.plusDays(1).toEpochSecond())
                                .setRatingRub(10)
                                .setLifetime("ДО ОКОНЧАНИЯ СРОКА ДЕЙСТВИЯ АКЦИИ")
                                .build())
                        .build())
                .setConstraints(DataCampPromo.PromoConstraints.newBuilder()
                        .setEndDate(current.plusDays(10).toEpochSecond())
                        .setStartDate(current.plusDays(1).toEpochSecond())
                        .build())
                .build();

        descriptionWithBluePromocode = DataCampPromo.PromoDescription.newBuilder()
                .setAdditionalInfo(DataCampPromo.PromoAdditionalInfo.newBuilder().build())
                .setPrimaryKey(DataCampPromo.PromoDescriptionIdentifier.newBuilder()
                        .setBusinessId(1)
                        .setPromoId(SOME_PROMO_ID)
                        .setSource(ANAPLAN)
                        .build())
                .setPromoGeneralInfo(DataCampPromo.PromoGeneralInfo.newBuilder()
                        .setPromoType(BLUE_PROMOCODE)
                        .build())
                .setMechanicsData(DataCampPromo.PromoMechanics.newBuilder()
                        .setBluePromocode(DataCampPromo.PromoMechanics.BluePromocode.newBuilder()
                                .setPromoCode(PROMOCODE)
                                .setPromocodeRating(10)
                                .build())
                        .build())
                .setConstraints(DataCampPromo.PromoConstraints.newBuilder()
                        .setEndDate(current.plusDays(10).toEpochSecond())
                        .setStartDate(current.plusDays(1).toEpochSecond())
                        .build())
                .build();
    }

    private void prepareData(PromoYtTestHelper.YtreeDataPromoStorageBuilder dataBuilder) {
        promoStorageTestHelper.addNullPromoStorageRecord(dataBuilder);
        dataBuilder
                .promo(1, SOME_PROMO_ID, ANAPLAN, description.toBuilder());
    }

    private void prepareDataKids(PromoYtTestHelper.YtreeDataPromoStorageBuilder dataBuilder) {
        promoStorageTestHelper.addNullPromoStorageRecord(dataBuilder);
        dataBuilder
                .promo(0, SOME_PROMO_ID_KIDS, ANAPLAN, descriptionKids.toBuilder());
    }

    private void prepareDataKidsUpdate(PromoYtTestHelper.YtreeDataPromoStorageBuilder dataBuilder) {
        promoStorageTestHelper.addNullPromoStorageRecord(dataBuilder);

        DataCampPromo.PromoDescription.Builder builder = descriptionKids.toBuilder();

        DataCampPromo.PromoAdditionalInfo.Builder additionalBuilder = builder.getAdditionalInfo().toBuilder();
        additionalBuilder.setUpdatedAt(additionalBuilder.getUpdatedAt() + 1000L);

        builder.setAdditionalInfo(additionalBuilder);

        dataBuilder
                .promo(0, SOME_PROMO_ID_KIDS, ANAPLAN, builder);
    }

    private void prepareDataKidsUpdateWithOneOrderTrue(PromoYtTestHelper.YtreeDataPromoStorageBuilder dataBuilder) {
        promoStorageTestHelper.addNullPromoStorageRecord(dataBuilder);

        DataCampPromo.PromoDescription.Builder builder = descriptionKids.toBuilder();

        DataCampPromo.PromoAdditionalInfo.Builder additionalBuilder = builder.getAdditionalInfo().toBuilder();
        additionalBuilder.setUpdatedAt(additionalBuilder.getUpdatedAt() + 1000L);

        builder.setAdditionalInfo(additionalBuilder);

        DataCampPromo.PromoMechanics.Builder mechanicsData = builder.getMechanicsData().toBuilder();
        DataCampPromo.PromoMechanics.MarketPromocode.Builder marketPromocode =
                builder.getMechanicsData().getMarketPromocode().toBuilder();
        marketPromocode.setOneOrderPromocode(true);
        mechanicsData.setMarketPromocode(marketPromocode);
        builder.setMechanicsData(mechanicsData);

        dataBuilder
                .promo(0, SOME_PROMO_ID_KIDS, ANAPLAN, builder);
    }

    private void prepareDataUpdate(PromoYtTestHelper.YtreeDataPromoStorageBuilder dataBuilder) {
        promoStorageTestHelper.addNullPromoStorageRecord(dataBuilder);
        dataBuilder
                .promo(1, SOME_PROMO_ID, ANAPLAN, descriptionUpdate.toBuilder());
    }

    private void prepareDataUpdateNow(PromoYtTestHelper.YtreeDataPromoStorageBuilder dataBuilder) {
        ZonedDateTime current = clock.dateTime().atZone(clock.getZone());
        long updatedAt = current.plusMinutes(30).toEpochSecond();
        promoStorageTestHelper.addNullPromoStorageRecord(dataBuilder);
        dataBuilder
                .promo(1, SOME_PROMO_ID, ANAPLAN,
                        descriptionUpdate.toBuilder().setAdditionalInfo(descriptionUpdate.getAdditionalInfo().toBuilder()
                                .setUpdatedAt(updatedAt)
                                .build()));
    }

    private void prepareDataUpdateInactive(PromoYtTestHelper.YtreeDataPromoStorageBuilder dataBuilder) {
        promoStorageTestHelper.addNullPromoStorageRecord(dataBuilder);
        dataBuilder
                .promo(1, SOME_PROMO_ID, ANAPLAN,
                        descriptionUpdateInactive.toBuilder());
    }

    private void prepareDataUpdateNewBusinessId(PromoYtTestHelper.YtreeDataPromoStorageBuilder dataBuilder) {
        promoStorageTestHelper.addNullPromoStorageRecord(dataBuilder);
        DataCampPromo.PromoDescription.Builder builder = description.toBuilder()
                .setBusinessMigrationInfo(
                        DataCampPromo.BusinessMigrationInfo.newBuilder()
                                .setStatus(NEW_BUSINESS)
                );
        dataBuilder
                .promo(2, SOME_PROMO_ID, ANAPLAN, builder);
    }

    private void prepareDataUpdateStatusBusinessMigration(PromoYtTestHelper.YtreeDataPromoStorageBuilder dataBuilder) {
        promoStorageTestHelper.addNullPromoStorageRecord(dataBuilder);
        DataCampPromo.PromoDescription.Builder builder = description.toBuilder()
                .setBusinessMigrationInfo(
                        DataCampPromo.BusinessMigrationInfo.newBuilder()
                                .setStatus(OLD_BUSINESS)
                );
        dataBuilder
                .promo(2, SOME_PROMO_ID, ANAPLAN, builder);
    }

    private void prepareDataWithEmptyPromoid(PromoYtTestHelper.YtreeDataPromoStorageBuilder dataBuilder) {
        promoStorageTestHelper.addNullPromoStorageRecord(dataBuilder);
        dataBuilder
                .promo(1, "", ANAPLAN, descriptionWithEmptyPromoId.toBuilder());
    }

    private void prepareDataWithMarketBonus(PromoYtTestHelper.YtreeDataPromoStorageBuilder dataBuilder) {
        dataBuilder.promo(descriptionWithMarketBonus);
    }

    private void prepareDataWithBluePromocode(PromoYtTestHelper.YtreeDataPromoStorageBuilder dataBuilder) {
        dataBuilder.promo(descriptionWithBluePromocode);
    }

    @Test
    public void shouldImportPromo() {
        PromoStoragePromoImporter.PromoStorageImportResults importResults = promoStorageTestHelper
                .withPromoStorageMock(this::prepareData, importer::importPromos);

        assertThat(importResults.getImportResults().stream()
                .filter(PromoStoragePromoImporter.PromoStorageImportResult::isValid)
                .collect(Collectors.toSet()), not(empty()));

        assertThat(importResults.getImportResults().stream().findFirst().get().getPromoStorageId(), is(SOME_PROMO_ID));
    }

    @Test
    public void shouldImportPromoWithAnaplanId() {
        PromoStoragePromoImporter.PromoStorageImportResults importResults = promoStorageTestHelper
                .withPromoStorageMock(this::prepareData, importer::importPromos);

        Promo promo = promoService.getPromoByShopPromoId(
                importResults.getImportResults().stream().findFirst().get().getPromoStorageId()
        );

        assertThat(promo.getPromoParam(ANAPLAN_ID).orElseThrow(), is(SOME_PROMO_ID));
    }

    @Test
    public void shouldUpdateBudgetFromPromoStorage() {
        PromoStoragePromoImporter.PromoStorageImportResults importResults = promoStorageTestHelper
                .withPromoStorageMock(this::prepareData, importer::importPromos);

        Promo promo = promoService.getPromoByShopPromoId(
                importResults.getImportResults().stream().findFirst().get().getPromoStorageId()
        );

        promoStorageTestHelper.withPromoStorageMock(this::prepareDataUpdate, importer::importPromos);

        promo = promoService.getPromo(promo.getId());

        assertThat(promo.getCurrentBudget().compareTo(BigDecimal.valueOf(UPDATED_BUDGET)), is(0));
    }

    @Test
    public void shouldNotUpdateStatusFromPromoStorageIfStatusUpdatedInLoyalty() {
        PromoStoragePromoImporter.PromoStorageImportResults importResults = promoStorageTestHelper
                .withPromoStorageMock(this::prepareData, importer::importPromos);

        Promo promo = promoService.getPromoByShopPromoId(
                importResults.getImportResults().stream().findFirst().get().getPromoStorageId()
        );

        promoService.updateStatus(promo, PromoStatus.INACTIVE);

        promo = promoService.getPromo(promo.getId());

        promoStorageTestHelper.withPromoStorageMock(this::prepareDataUpdate, importer::importPromos);

        promo = promoService.getPromo(promo.getId());

        assertThat(promo.getStatus(), is(PromoStatus.INACTIVE));
        assertThat(promo.getPromoParam(PromoParameterName.STATUS_SET_IN_LOYALTY).isPresent(), is(true));
    }

    @Test
    public void shouldUpdateStatusFromPromoStorageIfStatusUpdatedInLoyaltyAndChangedInPromoStorage() {
        PromoStoragePromoImporter.PromoStorageImportResults importResults = promoStorageTestHelper
                .withPromoStorageMock(this::prepareData, importer::importPromos);

        Promo promo = promoService.getPromoByShopPromoId(
                importResults.getImportResults().stream().findFirst().get().getPromoStorageId()
        );

        promoService.updateStatus(promo, PromoStatus.INACTIVE);

        promo = promoService.getPromo(promo.getId());

        assertThat(promo.getStatus(), is(PromoStatus.INACTIVE));
        assertThat(promo.getPromoParam(PromoParameterName.STATUS_SET_IN_LOYALTY).isPresent(), is(true));

        promoStorageTestHelper.withPromoStorageMock(this::prepareDataUpdateInactive, importer::importPromos);

        promo = promoService.getPromo(promo.getId());

        assertThat(promo.getPromoParam(PromoParameterName.STATUS_SET_IN_LOYALTY).isPresent(), is(false));

        promoStorageTestHelper.withPromoStorageMock(this::prepareDataUpdateNow, importer::importPromos);

        promo = promoService.getPromo(promo.getId());

        assertThat(promo.getStatus(), is(PromoStatus.ACTIVE));

    }


    @Test
    public void shouldStoreParentPromoBudget() {
        PromoStoragePromoImporter.PromoStorageImportResults importResults = promoStorageTestHelper
                .withPromoStorageMock(ytreeDataPromoStorageBuilder -> {
                    ytreeDataPromoStorageBuilder.promo(PARENT_PROMO);
                }, importer::importPromos);

        Promo promo = promoService.getPromoByShopPromoId(
                importResults.getImportResults().stream().findFirst().get().getPromoStorageId()
        );
        assertThat(promo.getCurrentBudget().compareTo(BigDecimal.valueOf(BUDGET)), is(0));
        assertThat(promo.getPromoParam(BUSINESS_ID).orElseThrow(), is(1L));
        assertThat(promo.getPromoParam(PromoParameterName.PROMO_SOURCE).orElseThrow(),
                is(NMarket.Common.Promo.Promo.ESourceType.AFFILIATE_VALUE));
    }


    @Test
    public void shouldImportOnlyAffiliateParentPromo() {
        PromoStoragePromoImporter.PromoStorageImportResults importResults = promoStorageTestHelper
                .withPromoStorageMock(ytreeDataPromoStorageBuilder -> {
                    ytreeDataPromoStorageBuilder.promo(
                            PARENT_PROMO.newBuilderForType().setPrimaryKey(
                                    PARENT_PROMO.getPrimaryKey().newBuilderForType()
                                            .setSource(ANAPLAN)
                            )
                                    .build()
                    );

                }, importer::importPromos);

        assertThat(importResults.getImportResults(), is(empty()));
    }

    @Test
    public void shouldImportParentPromo() {
        PromoStoragePromoImporter.PromoStorageImportResults importResults = promoStorageTestHelper
                .withPromoStorageMock(ytreeDataPromoStorageBuilder -> {
                    ytreeDataPromoStorageBuilder.promo(PARENT_PROMO);
                }, importer::importPromos);

        assertThat(importResults.getImportResults().stream()
                .filter(PromoStoragePromoImporter.PromoStorageImportResult::isValid)
                .collect(Collectors.toSet()), not(empty()));

        assertThat(importResults.getImportResults().stream().findFirst().get().getPromoStorageId(), is(SOME_PROMO_ID));
    }

    @Test
    public void testUpdateParentBudget() {
        var importResults = promoStorageTestHelper
                .withPromoStorageMock(ytreeDataPromoStorageBuilder -> {
                    ytreeDataPromoStorageBuilder.promo(PARENT_PROMO);
                    ytreeDataPromoStorageBuilder.promo(CHILD_AFFILIATE_PROMO);
                }, importer::importPromos);
        assertFalse(importResults.isImportFailed());

        // try importing the same
        importResults = promoStorageTestHelper
                .withPromoStorageMock(ytreeDataPromoStorageBuilder -> {
                    ytreeDataPromoStorageBuilder.promo(PARENT_PROMO);
                }, importer::importPromos);
        assertFalse(importResults.isImportFailed());
        assertThat(importResults.getImportResults(), emptyIterable());

        var updatedMoneyLimit =
                PARENT_PROMO.getConstraints().getMoneyLimit().toBuilder().setPrice(UPDATED_BUDGET);
        var updatePromo = PARENT_PROMO.toBuilder()
                .setConstraints(PARENT_PROMO.getConstraints().toBuilder()
                        .setMoneyLimit(updatedMoneyLimit)).build();
        importResults = promoStorageTestHelper
                .withPromoStorageMock(ytreeDataPromoStorageBuilder -> {
                    ytreeDataPromoStorageBuilder.promo(updatePromo);
                }, importer::importPromos);
        assertFalse(importResults.isImportFailed());
        assertThat(importResults.getImportResults(), iterableWithSize(1));

        var inDb = promoService.getPromoByShopPromoId(PARENT_PROMO.getPrimaryKey().getPromoId());
        assertNotNull(inDb);
        assertEquals(UPDATED_BUDGET, inDb.getCurrentBudget().longValue());
    }

    @Test
    public void testImportChildPromo() {
        var importResult = promoStorageTestHelper
                .withPromoStorageMock(ytreeDataPromoStorageBuilder -> {
                    ytreeDataPromoStorageBuilder.promo(PARENT_PROMO);
                    ytreeDataPromoStorageBuilder.promo(CHILD_AFFILIATE_PROMO);
                }, importer::importPromos);
        assertThat(importResult.getImportResults(), iterableWithSize(2));
        var parent = promoService.getPromoByShopPromoId(PARENT_PROMO.getPrimaryKey().getPromoId());
        var child = promoService.getPromoByShopPromoId(CHILD_AFFILIATE_PROMO.getPrimaryKey().getPromoId());
        assertNotNull(parent);
        assertNotNull(child);
        assertEquals(child.getBudgetSources(), parent.getBudgetSources());
        assertEquals(child.getBudgetAccountId(), parent.getBudgetAccountId());
        assertEquals(child.getSpendingAccountId(), parent.getSpendingAccountId());
        assertEquals(child.getBudgetEmissionAccountId(), parent.getBudgetEmissionAccountId());
        assertEquals(child.getSpendingEmissionAccountId(), parent.getSpendingEmissionAccountId());
        assertEquals(BUDGET, parent.getCurrentBudget().longValue());
        assertEquals(BUDGET, child.getCurrentBudget().longValue());
    }

    @Test
    public void shouldUseGeneratedPromoCode() {
        var countByCode = promocodeEntryDao.countByCode(promoCode);
        assertThat(countByCode, equalTo(1));

        PromoStoragePromoImporter.PromoStorageImportResults importResults = promoStorageTestHelper
                .withPromoStorageMock(this::prepareData, importer::importPromos);


        Promo promo = promoService.getPromoByShopPromoId(
                importResults.getImportResults().stream().findFirst().get().getPromoStorageId()
        );

        countByCode = promocodeEntryDao.countByCode(promoCode);
        assertThat(countByCode, equalTo(1));
        assertThat(promo.getActionCode(), equalTo(promoCode));
    }

    @Test
    public void shouldSetNewBusinessIdImportPromo() {
        PromoStoragePromoImporter.PromoStorageImportResults importResults = promoStorageTestHelper
                .withPromoStorageMock(this::prepareData, importer::importPromos);

        Promo promo = promoService.getPromoByShopPromoId(
                importResults.getImportResults().stream().findFirst().get().getPromoStorageId()
        );

        configurationService.set(ConfigurationService.PROMO_STORAGE_UPDATE_BUSINESS_MIGRATION, true);
        promoStorageTestHelper.withPromoStorageMock(this::prepareDataUpdateNewBusinessId, importer::importPromos);

        Promo promoWithNewBusinessId = promoService.getPromo(promo.getPromoId().getId());

        assertNotEquals(promo.getPromoParam(BUSINESS_ID), promoWithNewBusinessId.getPromoParam(BUSINESS_ID));
        assertTrue(promoWithNewBusinessId.getPromoParam(BUSINESS_MIGRATION_INFO_STATUS).isPresent());


        DataCampPromo.BusinessMigrationInfo.Status status =
                DataCampPromo.BusinessMigrationInfo.Status.forNumber(
                        promoWithNewBusinessId.getPromoParam(BUSINESS_MIGRATION_INFO_STATUS).get()
                );
        assertEquals(status, NEW_BUSINESS);
    }

    @Test
    public void shouldSetOldBusinessMigrationStatusImportPromo() {
        PromoStoragePromoImporter.PromoStorageImportResults importResults = promoStorageTestHelper
                .withPromoStorageMock(this::prepareData, importer::importPromos);

        Promo promo = promoService.getPromoByShopPromoId(
                importResults.getImportResults().stream().findFirst().get().getPromoStorageId()
        );

        configurationService.set(ConfigurationService.PROMO_STORAGE_UPDATE_BUSINESS_MIGRATION, true);
        promoStorageTestHelper.withPromoStorageMock(this::prepareDataUpdateNewBusinessId, importer::importPromos);
        Promo promoWithNewBusinessId = promoService.getPromo(promo.getPromoId().getId());

        promoStorageTestHelper.withPromoStorageMock(
                this::prepareDataUpdateStatusBusinessMigration,
                importer::importPromos
        );
        Promo promoWithOldBusinessMigrationStatus = promoService.getPromo(promo.getPromoId().getId());

        assertEquals(
                promoWithNewBusinessId.getPromoParam(BUSINESS_ID),
                promoWithOldBusinessMigrationStatus.getPromoParam(BUSINESS_ID)
        );
        assertTrue(promoWithNewBusinessId.getPromoParam(BUSINESS_MIGRATION_INFO_STATUS).isPresent());

        DataCampPromo.BusinessMigrationInfo.Status status =
                DataCampPromo.BusinessMigrationInfo.Status.forNumber(
                        promoWithOldBusinessMigrationStatus.getPromoParam(BUSINESS_MIGRATION_INFO_STATUS).get()
                );
        assertEquals(status, OLD_BUSINESS);
    }

    @Test
    public void shouldNotUpdateNewBusinessIdWhenUpdateAtImportPromo() {
        PromoStoragePromoImporter.PromoStorageImportResults importResults = promoStorageTestHelper
                .withPromoStorageMock(this::prepareData, importer::importPromos);

        Promo promo = promoService.getPromoByShopPromoId(
                importResults.getImportResults().stream().findFirst().get().getPromoStorageId()
        );

        configurationService.set(ConfigurationService.PROMO_STORAGE_UPDATE_BUSINESS_MIGRATION, true);
        promoStorageTestHelper.withPromoStorageMock(this::prepareDataUpdateNewBusinessId, importer::importPromos);
        Promo promoWithNewBusinessId = promoService.getPromo(promo.getPromoId().getId());

        promoStorageTestHelper.withPromoStorageMock(this::prepareDataUpdateNow, importer::importPromos);
        Promo promoWithNewBusinessIdToo = promoService.getPromo(promo.getPromoId().getId());

        assertEquals(
                promoWithNewBusinessId.getPromoParam(BUSINESS_ID),
                promoWithNewBusinessIdToo.getPromoParam(BUSINESS_ID)
        );
    }

    @Test
    public void shouldImportPromoForKids() {
        PromoStoragePromoImporter.PromoStorageImportResults importResults = promoStorageTestHelper
                .withPromoStorageMock(this::prepareDataKids, importer::importPromos);

        Promo promo = promoService.getPromoByShopPromoId(
                importResults.getImportResults().stream().findFirst().get().getPromoStorageId()
        );

        assertThat(promo.getStatus(), is(PromoStatus.PENDING));
    }

    @Test
    public void shouldImportPromoForKidsAndUpdatePromoKey() {
        PromoStoragePromoImporter.PromoStorageImportResults importResults = promoStorageTestHelper
                .withPromoStorageMock(this::prepareDataKids, importer::importPromos);

        String promoStorageId = importResults.getImportResults().stream().findFirst().get().getPromoStorageId();

        Promo promo = promoService.getPromoByShopPromoId(promoStorageId);

        final PromocodePromoDescription promoDescription = PromocodePromoDescription.builder()
                .promoCode(PROMOCODE_KIDS)
                .promoKey(NEW_PROMO_KEY)
                .shopPromoId(promoStorageId)
                .source(ANAPLAN.name())
                .feedId(0L)
                .startTime(LocalDateTime.now())
                .build();
        promocodeService.updatePendingPromoPromocode(promoDescription);

        Promo promoWithNewPromoKey = promoService.getPromoByShopPromoId(promoStorageId);

        assertThat(promoWithNewPromoKey.getStatus(), is(PromoStatus.ACTIVE));
        assertThat(promoWithNewPromoKey.getPromoKeys(), hasSize(2));
        assertThat(promoWithNewPromoKey.getPromoKeys(), is(Set.of(promo.getPromoKey(), NEW_PROMO_KEY)));
    }

    @Test
    public void shouldReImportPromoForKidsAndUpdatePromoKey() {
        PromoStoragePromoImporter.PromoStorageImportResults importResults = promoStorageTestHelper
                .withPromoStorageMock(this::prepareDataKids, importer::importPromos);

        String promoStorageId = importResults.getImportResults().stream().findFirst().get().getPromoStorageId();

        Promo promo = promoService.getPromoByShopPromoId(promoStorageId);

        promoStorageTestHelper
                .withPromoStorageMock(this::prepareDataKidsUpdate, importer::importPromos);

        final PromocodePromoDescription promoDescription = PromocodePromoDescription.builder()
                .promoCode(PROMOCODE_KIDS)
                .promoKey(NEW_PROMO_KEY)
                .shopPromoId(promoStorageId)
                .source(ANAPLAN.name())
                .feedId(0L)
                .startTime(LocalDateTime.now())
                .build();
        promocodeService.updatePendingPromoPromocode(promoDescription);

        Promo promoWithNewPromoKey = promoService.getPromoByShopPromoId(promoStorageId);

        assertThat(promoWithNewPromoKey.getStatus(), is(PromoStatus.ACTIVE));
        assertThat(promoWithNewPromoKey.getPromoKeys(), hasSize(2));
        assertThat(promoWithNewPromoKey.getPromoKeys(), is(Set.of(promo.getPromoKey(), NEW_PROMO_KEY)));
    }

    @Test
    public void shouldNotImportPromoWithEmptyPromoId() {
        PromoStoragePromoImporter.PromoStorageImportResults importResults = promoStorageTestHelper
                .withPromoStorageMock(this::prepareDataWithEmptyPromoid, importer::importPromos);

        assertThat(importResults.getImportResults(), empty());

        verify(dataCampStrollerClient, atLeastOnce()).updatePromo(
                argumentCaptor.capture()
        );

        String creationErrors = argumentCaptor.getValue().getLoyaltyKey().getCreationError();
        assertThat(creationErrors, is("Не указан идентификатор акции в АХ (promoStorageId)"));
    }

    @Test
    public void shouldUpdateOneOrder() {
        PromoStoragePromoImporter.PromoStorageImportResults importResults = promoStorageTestHelper
                .withPromoStorageMock(this::prepareDataKids, importer::importPromos);

        String promoStorageId = importResults.getImportResults().stream().findFirst().get().getPromoStorageId();

        Promo promo = promoService.getPromoByShopPromoId(promoStorageId);
        Set<CoinProps> allCoinPropsPrototypesByPromoId =
                coinDao.getAllCoinPropsPrototypesByPromoId(promo.getPromoId().getId());

        assertThat(allCoinPropsPrototypesByPromoId, hasSize(1));

        RulesContainer rulesContainer = allCoinPropsPrototypesByPromoId.stream().findFirst().get().getRulesContainer();

        assertThat(rulesContainer.getAllRules(), hasSize(4));
        assertNull(rulesContainer.get(FIRST_ORDER_CUTTING_RULE));

        promoStorageTestHelper
                .withPromoStorageMock(this::prepareDataKidsUpdateWithOneOrderTrue, importer::importPromos);

        Promo promoWithUpdateOneOrder = promoService.getPromoByShopPromoId(promoStorageId);
        Set<CoinProps> allCoinPropsPrototypesByPromoIdWithUpdateOneOrder =
                coinDao.getAllCoinPropsPrototypesByPromoId(promoWithUpdateOneOrder.getPromoId().getId());

        assertThat(allCoinPropsPrototypesByPromoIdWithUpdateOneOrder, hasSize(2));

        RulesContainer rulesContainerWithUpdateOneOrder = allCoinPropsPrototypesByPromoIdWithUpdateOneOrder.stream()
                .filter(props -> props.getId().equals(promoWithUpdateOneOrder.getCoinPropsId()))
                .findFirst().get()
                .getRulesContainer();

        assertThat(rulesContainerWithUpdateOneOrder.getAllRules(), hasSize(5));
        assertNotNull(rulesContainerWithUpdateOneOrder.get(FIRST_ORDER_CUTTING_RULE));
    }

    @Test
    public void shouldImportPromoWithMarketBonusFromAnaplan() {
        configurationService.set(NOT_DOWNLOAD_BONUS_AND_PROMOCODE_FROM_ANAPLAN, false);

        PromoStoragePromoImporter.PromoStorageImportResults importResultsWithMarketBonus = promoStorageTestHelper
                .withPromoStorageMock(this::prepareDataWithMarketBonus, importer::importPromos);

        assertThat(importResultsWithMarketBonus.getImportResults(), not(empty()));
    }

    @Test
    public void shouldImportPromoWithBluePromocodeFromAnaplan() {
        configurationService.set(NOT_DOWNLOAD_BONUS_AND_PROMOCODE_FROM_ANAPLAN, false);

        PromoStoragePromoImporter.PromoStorageImportResults importResultsWithBluePromocode = promoStorageTestHelper
                .withPromoStorageMock(this::prepareDataWithBluePromocode, importer::importPromos);

        assertThat(importResultsWithBluePromocode.getImportResults(), not(empty()));
    }

    @Test
    public void shouldNotImportPromoWithMarketBonusFromAnaplan() {
        configurationService.set(NOT_DOWNLOAD_BONUS_AND_PROMOCODE_FROM_ANAPLAN, true);

        PromoStoragePromoImporter.PromoStorageImportResults importResultsWithMarketBonus = promoStorageTestHelper
                .withPromoStorageMock(this::prepareDataWithMarketBonus, importer::importPromos);

        assertThat(importResultsWithMarketBonus.getImportResults(), empty());
    }

    @Test
    public void shouldNotImportPromoWithBluePromocodeFromAnaplan() {
        configurationService.set(NOT_DOWNLOAD_BONUS_AND_PROMOCODE_FROM_ANAPLAN, true);

        PromoStoragePromoImporter.PromoStorageImportResults importResultsWithBluePromocode = promoStorageTestHelper
                .withPromoStorageMock(this::prepareDataWithBluePromocode, importer::importPromos);

        assertThat(importResultsWithBluePromocode.getImportResults(), empty());
    }
}
