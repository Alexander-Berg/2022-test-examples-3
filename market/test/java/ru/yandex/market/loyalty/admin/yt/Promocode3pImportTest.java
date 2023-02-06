package ru.yandex.market.loyalty.admin.yt;

import Market.DataCamp.DataCampPromo;
import NMarketIndexer.Common.Common;
import com.google.protobuf.ByteString;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.loyalty.admin.controller.dto.PromoHistoryDto;
import ru.yandex.market.loyalty.admin.service.PromoHistoryService;
import ru.yandex.market.loyalty.admin.test.MarketLoyaltyAdminMockedDbTest;
import ru.yandex.market.loyalty.admin.yt.service.PromoStoragePromoImporter;
import ru.yandex.market.loyalty.admin.yt.service.PromoYtImporter;
import ru.yandex.market.loyalty.api.model.PromoStatus;
import ru.yandex.market.loyalty.core.model.ReportPromoType;
import ru.yandex.market.loyalty.core.model.promo.Promo;
import ru.yandex.market.loyalty.core.model.promo.PromoParameterName;
import ru.yandex.market.loyalty.core.service.PromoService;
import ru.yandex.market.loyalty.core.service.promocode.PromocodeService;

import Market.Promo.Promo.PromoDetails;

import java.nio.charset.Charset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

import static NMarket.Common.Promo.Promo.ESourceType.PARTNER_SOURCE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;


public class Promocode3pImportTest extends MarketLoyaltyAdminMockedDbTest {
    private static final String SOME_PROMO_KEY = "#1111";
    private static final long BUDGET = 10000L * 10000000;
    private static final long FEED_ID = 123123;
    public static final String UPDATED_PROMO_KEY = "PP8uzJankfi0gckoQuQFPA==";
    private static final long SHOP_ID = 151251;

    @Autowired
    private PromoStoragePromoImporter importer;
    @Autowired
    private PromoYtTestHelper promoYtTestHelper;
    @Autowired
    private PromocodeService promocodeService;
    @Autowired
    private PromoYtImporter promoYtImporter;
    @Autowired
    private PromoService promoService;
    @Autowired
    private PromoHistoryService promoHistoryService;

    private DataCampPromo.PromoDescription description;
    private PromoDetails promoDetailsDescription;

    private String promoCode;

    @Override
    @Before
    public void initMocks() {
        ZonedDateTime current = clock.dateTime().atZone(clock.getZone());

        promoCode = promocodeService.generateNewPromocode();

        description = DataCampPromo.PromoDescription.newBuilder()
                .setAdditionalInfo(
                        DataCampPromo.PromoAdditionalInfo.newBuilder()
                                .setCompensation(DataCampPromo.PromoAdditionalInfo.Compensation.MARKET)
                                .setComment("Comment")
                                .setCreatedAt(current.minusDays(2).toEpochSecond())
                                .setUpdatedAt(current.minusDays(1).toEpochSecond())
                                .setLendingUrl("landing url")
                                .setRulesUrl("url")
                                .setName("Promocode promo")
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
                        .setMoneyLimit(Common.PriceExpression.newBuilder().setPrice(BUDGET).build())
                        .setEndDate(current.plusDays(10).toEpochSecond())
                        .setStartDate(current.plusDays(1).toEpochSecond())
                        .setHidden(false)
                        .addOffersMatchingRules(DataCampPromo.PromoConstraints.OffersMatchingRule.newBuilder()
                                .setSupplierRestriction(
                                        DataCampPromo.PromoConstraints.OffersMatchingRule.SupplierRestriction.newBuilder()
                                                .setSuppliers(DataCampPromo.PromoConstraints.OffersMatchingRule.IntList.newBuilder()
                                                        .addId(SHOP_ID)
                                                        .build()).build())
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
                                        .setExcludedBrands(DataCampPromo.PromoConstraints.OffersMatchingRule.PromoBrands.newBuilder()
                                                .addBrands(DataCampPromo.PromoBrand.newBuilder()
                                                        .setId(123)
                                                        .setName("Brand")
                                                        .setIsRestriction(true)
                                                        .build()).build()).build()).build()))
                .setPrimaryKey(DataCampPromo.PromoDescriptionIdentifier.newBuilder()
                        .setBusinessId(1)
                        .setPromoId(SOME_PROMO_KEY)
                        .setSource(PARTNER_SOURCE)
                        .build())
                .setResponsible(DataCampPromo.PromoResponsible.newBuilder()
                        .setAuthor("author")
                        .build())
                .build();

        promoDetailsDescription = PromoDetails.newBuilder()
                .setPromoCode(promoCode)
                .setSourceType(PARTNER_SOURCE)
                .setShopPromoId(SOME_PROMO_KEY)
                .setType(ReportPromoType.PROMOCODE.getCode())
                .setBinaryPromoMd5(ByteString.copyFrom(UPDATED_PROMO_KEY, Charset.defaultCharset()))
                .setLandingUrl("idx landing url")
                .setUrl("idx url")
                .build();
    }

    private void preparePromoDetails(PromoYtTestHelper.YtreeDataBuilder dataBuilder) {
        promoYtTestHelper.addNullRecord(dataBuilder);
        dataBuilder
                .promo(FEED_ID, UPDATED_PROMO_KEY, promoDetailsDescription.toBuilder());
    }

    private void prepareData(PromoYtTestHelper.YtreeDataPromoStorageBuilder dataBuilder) {
        promoYtTestHelper.addNullPromoStorageRecord(dataBuilder);
        dataBuilder
                .promo(1, SOME_PROMO_KEY, PARTNER_SOURCE, description.toBuilder());
    }

    private void prepareDataEmptyUrl(PromoYtTestHelper.YtreeDataPromoStorageBuilder dataBuilder) {
        promoYtTestHelper.addNullPromoStorageRecord(dataBuilder);

        DataCampPromo.PromoDescription.Builder builder = description.toBuilder();

        DataCampPromo.PromoAdditionalInfo.Builder additionalBuilder = builder.getAdditionalInfo().toBuilder();
        additionalBuilder
                .setLendingUrl("")
                .setRulesUrl("")
                .setUpdatedAt(additionalBuilder.getUpdatedAt() + 1000L);

        builder.setAdditionalInfo(additionalBuilder.build());

        dataBuilder
                .promo(1, SOME_PROMO_KEY, PARTNER_SOURCE, builder);
    }

    private void prepareDataUpdate(PromoYtTestHelper.YtreeDataPromoStorageBuilder dataBuilder) {
        promoYtTestHelper.addNullPromoStorageRecord(dataBuilder);

        DataCampPromo.PromoDescription.Builder builder = description.toBuilder();

        DataCampPromo.PromoAdditionalInfo.Builder additionalBuilder = builder.getAdditionalInfo().toBuilder();
        additionalBuilder
                .setUpdatedAt(additionalBuilder.getUpdatedAt() + 2000L);

        builder.setAdditionalInfo(additionalBuilder.build());

        dataBuilder
                .promo(1, SOME_PROMO_KEY, PARTNER_SOURCE, builder);
    }

    @Test
    public void shouldUpdatePromoKeyFor3pPromocodes() {
        importFromDataCamp();
        updateFromIdx();

        clock.spendTime(2, ChronoUnit.DAYS);

        final Promo currentPromoByPromoCode = promocodeService.getActiveCurrentPromoByPromoCode(promoCode);
        assertThat(currentPromoByPromoCode.getPromoKey(), equalTo(UPDATED_PROMO_KEY));
        assertThat(currentPromoByPromoCode.getStatus(), equalTo(PromoStatus.ACTIVE));
    }

    @Test
    public void shouldNotUpdateStatusToPendingOnUpdateFromPromoStorage() {
        importFromDataCamp();

        final List<Promo> allPromos = promoService.getAllPromos();
        assertThat(allPromos, hasSize(1));
        Promo promo = allPromos.get(0);

        assertThat(promo.getPromoKey(), not(equalTo(UPDATED_PROMO_KEY)));
        assertThat(promo.getStatus(), equalTo(PromoStatus.PENDING));

        importFromDataCamp();
        promo = promoService.getPromo(promo.getPromoId().getId());

        assertThat(promo.getPromoKey(), not(equalTo(UPDATED_PROMO_KEY)));
        assertThat(promo.getStatus(), equalTo(PromoStatus.PENDING));

        updateFromIdx();
        promo = promoService.getPromo(promo.getPromoId().getId());
        assertThat(promo.getPromoKey(), equalTo(UPDATED_PROMO_KEY));
        assertThat(promo.getStatus(), equalTo(PromoStatus.ACTIVE));

        importFromDataCamp();
        promo = promoService.getPromo(promo.getPromoId().getId());

        assertThat(promo.getPromoKey(), equalTo(UPDATED_PROMO_KEY));
        assertThat(promo.getStatus(), equalTo(PromoStatus.ACTIVE));
    }

    @Test
    public void shouldUpdatePromoKeyWhenPromoActive() {
        importFromDataCamp();

        final List<Promo> allPromos = promoService.getAllPromos();
        assertThat(allPromos, hasSize(1));
        Promo promo = allPromos.get(0);

        assertThat(promo.getPromoKey(), not(equalTo(UPDATED_PROMO_KEY)));
        assertThat(promo.getStatus(), equalTo(PromoStatus.PENDING));

        promoService.updateStatusFromPromoStorage(promo, PromoStatus.ACTIVE);

        updateFromIdx();
        promo = promoService.getPromo(promo.getPromoId().getId());
        assertThat(promo.getPromoKey(), equalTo(UPDATED_PROMO_KEY));
        assertThat(promo.getStatus(), equalTo(PromoStatus.ACTIVE));
    }

    @Test
    public void shouldUpdatePromoKeyWhenPromoInactive() {
        importFromDataCamp();

        final List<Promo> allPromos = promoService.getAllPromos();
        assertThat(allPromos, hasSize(1));
        Promo promo = allPromos.get(0);

        assertThat(promo.getPromoKey(), not(equalTo(UPDATED_PROMO_KEY)));
        assertThat(promo.getStatus(), equalTo(PromoStatus.PENDING));

        promoService.updateStatusFromPromoStorage(promo, PromoStatus.INACTIVE);

        updateFromIdx();
        promo = promoService.getPromo(promo.getPromoId().getId());
        assertThat(promo.getPromoKey(), equalTo(UPDATED_PROMO_KEY));
        assertThat(promo.getStatus(), equalTo(PromoStatus.INACTIVE));
    }

    @Test
    public void shouldNotUpdateStatusToActiveAfterInactivated() {
        importFromDataCamp();
        updateFromIdx();
        clock.spendTime(2, ChronoUnit.DAYS);

        Promo promo = promocodeService.getActiveCurrentPromoByPromoCode(promoCode);
        assertThat(promo.getPromoKey(), equalTo(UPDATED_PROMO_KEY));
        assertThat(promo.getStatus(), equalTo(PromoStatus.ACTIVE));

        promoService.updateStatus(promo, PromoStatus.INACTIVE);
        promo = promoService.getPromo(promo.getPromoId().getId());
        assertThat(promo.getStatus(), equalTo(PromoStatus.INACTIVE));

        updateFromIdx();
        promo = promoService.getPromo(promo.getPromoId().getId());
        assertThat(promo.getStatus(), equalTo(PromoStatus.INACTIVE));
    }

    private void updateFromIdx() {
        final List<PromoYtImporter.ImportResult> importResults = promoYtTestHelper.withMock(
                this::preparePromoDetails, promoYtImporter::importPromos);

        assertThat(importResults.stream()
                .filter(PromoYtImporter.ImportResult::isImportFailed)
                .collect(Collectors.toSet()), empty());
    }

    private PromoStoragePromoImporter.PromoStorageImportResults importFromDataCamp() {
        final PromoStoragePromoImporter.PromoStorageImportResults promoStorageImportResults =
                promoYtTestHelper.withPromoStorageMock(
                        this::prepareData, importer::importPromos);

        assertThat(promoStorageImportResults.getImportResults().stream()
                .filter(PromoStoragePromoImporter.PromoStorageImportResult::isValid)
                .collect(Collectors.toSet()), not(empty()));
        return promoStorageImportResults;
    }

    private PromoStoragePromoImporter.PromoStorageImportResults importFromDataCampEmptyUrl() {
        final PromoStoragePromoImporter.PromoStorageImportResults promoStorageImportResults =
                promoYtTestHelper.withPromoStorageMock(
                        this::prepareDataEmptyUrl, importer::importPromos);

        assertThat(promoStorageImportResults.getImportResults().stream()
                .filter(PromoStoragePromoImporter.PromoStorageImportResult::isValid)
                .collect(Collectors.toSet()), not(empty()));
        return promoStorageImportResults;
    }

    private PromoStoragePromoImporter.PromoStorageImportResults importFromDataCampUpdate() {
        final PromoStoragePromoImporter.PromoStorageImportResults promoStorageImportResults =
                promoYtTestHelper.withPromoStorageMock(
                        this::prepareDataUpdate, importer::importPromos);

        assertThat(promoStorageImportResults.getImportResults().stream()
                .filter(PromoStoragePromoImporter.PromoStorageImportResult::isValid)
                .collect(Collectors.toSet()), not(empty()));
        return promoStorageImportResults;
    }

    @Test
    public void shouldSaveShopIdToPromoParams() {
        final PromoStoragePromoImporter.PromoStorageImportResults importResults = importFromDataCamp();

        Promo promo = promoService.getPromoByShopPromoId(
                importResults.getImportResults().stream().findFirst().get().getPromoStorageId()
        );

        assertThat(promo.getPromoParam(PromoParameterName.SHOP_ID).get(), equalTo(SHOP_ID));
    }

    @Test
    public void shouldSaveAndUpdateLandingUrlToPromoParams() {
        PromoStoragePromoImporter.PromoStorageImportResults importResults = importFromDataCamp();
        String promoStorageId = importResults.getImportResults().stream().findFirst().get().getPromoStorageId();
        Promo promo = promoService.getPromoByShopPromoId(promoStorageId);

        assertThat(promo.getPromoParam(PromoParameterName.LANDING_URL).get(), equalTo("landing url"));

        promoService.setPromoParam(promo.getPromoId().getId(), PromoParameterName.LANDING_URL, "another landing url");

        updateFromIdx();

        promo = promoService.getPromoByShopPromoId(promoStorageId);

        assertThat(promo.getPromoParam(PromoParameterName.LANDING_URL).get(), equalTo("idx landing url"));
    }

    @Test
    public void shouldNotDoubleUpdateLandingUrlToPromoParams() {
        PromoStoragePromoImporter.PromoStorageImportResults importResults = importFromDataCamp();
        String promoStorageId = importResults.getImportResults().stream().findFirst().get().getPromoStorageId();
        long promoId = promoService.getPromoByShopPromoId(promoStorageId).getPromoId().getId();

        updateFromIdx();
        PromoHistoryDto promoHistoryFirst = promoHistoryService.getPromoHistory(promoId);

        updateFromIdx();
        PromoHistoryDto promoHistorySecond = promoHistoryService.getPromoHistory(promoId);

        assertEquals(promoHistoryFirst.getVersions().size(), promoHistorySecond.getVersions().size());
    }

    @Test
    public void shouldSaveAndUpdateUrlToPromoParams() {
        PromoStoragePromoImporter.PromoStorageImportResults importResults = importFromDataCamp();
        String promoStorageId = importResults.getImportResults().stream().findFirst().get().getPromoStorageId();
        Promo promo = promoService.getPromoByShopPromoId(promoStorageId);

        assertThat(promo.getPromoParam(PromoParameterName.PROMO_OFFER_AND_ACCEPTANCE).get(), equalTo("url"));

        promoService.setPromoParam(
                promo.getPromoId().getId(), PromoParameterName.PROMO_OFFER_AND_ACCEPTANCE, "another url");

        updateFromIdx();

        promo = promoService.getPromoByShopPromoId(promoStorageId);

        assertThat(
                promo.getPromoParam(PromoParameterName.PROMO_OFFER_AND_ACCEPTANCE).get(), equalTo("idx url"));
    }

    @Test
    public void shouldNotSaveEmptyUrlUrlToPromoParams() {
        PromoStoragePromoImporter.PromoStorageImportResults importResults = importFromDataCamp();
        String promoStorageId = importResults.getImportResults().stream().findFirst().get().getPromoStorageId();
        Promo promo = promoService.getPromoByShopPromoId(promoStorageId);

        assertThat(promo.getPromoParam(PromoParameterName.LANDING_URL).get(), equalTo("landing url"));
        assertThat(promo.getPromoParam(PromoParameterName.PROMO_OFFER_AND_ACCEPTANCE).get(), equalTo("url"));

        updateFromIdx();

        promo = promoService.getPromoByShopPromoId(promoStorageId);

        assertThat(promo.getPromoParam(PromoParameterName.LANDING_URL).get(), equalTo("idx landing url"));
        assertThat(promo.getPromoParam(PromoParameterName.PROMO_OFFER_AND_ACCEPTANCE).get(), equalTo("idx url"));

        importFromDataCampEmptyUrl();

        promo = promoService.getPromoByShopPromoId(promoStorageId);

        assertThat(promo.getPromoParam(PromoParameterName.LANDING_URL).get(), equalTo("idx landing url"));
        assertThat(promo.getPromoParam(PromoParameterName.PROMO_OFFER_AND_ACCEPTANCE).get(), equalTo("idx url"));

        importFromDataCampUpdate();

        promo = promoService.getPromoByShopPromoId(promoStorageId);

        assertThat(promo.getPromoParam(PromoParameterName.LANDING_URL).get(), equalTo("landing url"));
        assertThat(promo.getPromoParam(PromoParameterName.PROMO_OFFER_AND_ACCEPTANCE).get(), equalTo("url"));

    }
}
