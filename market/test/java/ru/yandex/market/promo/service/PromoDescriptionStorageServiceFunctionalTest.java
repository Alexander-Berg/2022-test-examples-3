package ru.yandex.market.promo.service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import Market.DataCamp.DataCampOfferMeta;
import Market.DataCamp.DataCampPromo;
import Market.DataCamp.SyncAPI.SyncGetPromo;
import NMarket.Common.Promo.Promo;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import ru.yandex.market.core.supplier.promo.model.PromoType;
import ru.yandex.market.core.util.DateTimes;
import ru.yandex.market.mbi.datacamp.stroller.DataCampClient;
import ru.yandex.market.promo.model.AdditionalInfoYt;
import ru.yandex.market.promo.model.AnaplanExports;
import ru.yandex.market.promo.model.AnaplanPromoStatus;
import ru.yandex.market.promo.model.BluePromocodeYt;
import ru.yandex.market.promo.model.CheapestAsGiftYt;
import ru.yandex.market.promo.model.ExportMskuPromoYtTable;
import ru.yandex.market.promo.model.ExportOperationalPromoYtTable;
import ru.yandex.market.promo.model.ExportParentPromoYtTable;
import ru.yandex.market.promo.model.ExportPromoChannelYtTable;
import ru.yandex.market.promo.model.ExportRestrictionYtTable;
import ru.yandex.market.promo.model.PromoCheckInfo;
import ru.yandex.market.promo.model.PromoConstraintsYt;
import ru.yandex.market.promo.model.PromoResponsibleYt;
import ru.yandex.market.shop.FunctionalTest;

import static Market.DataCamp.DataCampPromo.PromoPromotion.Channel.MAIN_BOTTOM_BANNER;
import static org.mockito.Mockito.doNothing;
import static ru.yandex.market.core.supplier.promo.service.PromoService.getDateInSeconds;

public class PromoDescriptionStorageServiceFunctionalTest extends FunctionalTest {

    @Autowired
    private PromoDescriptionStorageService promoDescriptionStorageService;

    @Autowired
    @Qualifier("dataCampShopClient")
    private DataCampClient dataCampShopClient;

    @Test
    public void testNPE() {
        ArgumentCaptor<SyncGetPromo.UpdatePromoBatchRequest> promosSetCaptor =
                ArgumentCaptor.forClass(SyncGetPromo.UpdatePromoBatchRequest.class);
        ArgumentCaptor<Long> businessCapture = ArgumentCaptor.forClass(Long.class);
        doNothing().when(dataCampShopClient).addPromo(promosSetCaptor.capture(), businessCapture.capture());
        AnaplanExports anaplanExports = new AnaplanExports.Builder()
                .withPromoRestriction(Collections.emptyMap())
                .withPromosChannels(Collections.emptyMap())
                .withPromosParent(Collections.emptyMap())
                .withRestrictionMsku(Collections.emptyMap())
                .build();
        promoDescriptionStorageService.sendPromoToPromoStorage(
                Collections.emptyList(), anaplanExports, 0L);
        List<SyncGetPromo.UpdatePromoBatchRequest> capturedPromosSets = promosSetCaptor.getAllValues();
        Assertions.assertEquals(0, capturedPromosSets.size());
    }

    @Test
    public void test() {
        String promoId = "#promo_id";
        long businessId = 0L;
        String restrictionCnt = "1";
        ExportOperationalPromoYtTable promo = new ExportOperationalPromoYtTable.Builder()
                .withId(promoId)
                .withName("promo_name")
                .withType(PromoType.CHEAPEST_AS_GIFT)
                .withStatus(AnaplanPromoStatus.NEW)
                .withPromoCheckInfo(createCheckInfo(promoId, restrictionCnt))
                .withPromoResponsible(
                        new PromoResponsibleYt.Builder()
                                .withAuthor("author")
                                .withMarcom("marcom")
                                .withTm("tm")
                                .withTmLogin("tm_login")
                                .withMarcomLogin("marcom_login")
                                .withApprovingManager("approving_manager")
                                .build())
                .withAdditionalInfo(
                        new AdditionalInfoYt.Builder()
                                .withPromoUrl("promo_url")
                                .withLinkText("link_text")
                                .withAdhoc("true")
                                .withCmLink("cm_link")
                                .withAssortmentAutopublication(true)
                                .withAssortmentLimit(1000)
                                .withStartrekRobotTicketLink("link_to_robot")
                                .withSupplierFlagRestriction("Express")
                                .build()
                )
                .withPromoConstraints(
                        new PromoConstraintsYt.Builder()
                                .withStartDate("2020-12-22T00:00:00")
                                .withEndDate("2021-01-01T00:00:00")
                                .build())
                .withCheapestAsGift(new CheapestAsGiftYt("3"))
                .withBluePromocode(new BluePromocodeYt.Builder().withMultipleUsage(true).build())
                .build();
        List<ExportOperationalPromoYtTable> promos = List.of(promo);

        ExportRestrictionYtTable exportRestrictionYtTable =
                new ExportRestrictionYtTable.Builder(promoId)
                        .withPromosHidRestrictionCategory(
                                Map.of(
                                        2L, new ExportRestrictionYtTable.HidCategory.Builder()
                                                .withHid("2")
                                                .withDiscount("")
                                                .build()
                                ))
                        .withOriginalRestrictionCategory(
                                new ExportRestrictionYtTable.OriginalCategoryRestriction("false")
                                        .addCategory("2", "")
                        )
                        .build();

        Map<String, ExportRestrictionYtTable> promosRestriction = Map.of(promoId, exportRestrictionYtTable);
        ExportPromoChannelYtTable exportPromoChannelYtTable = new ExportPromoChannelYtTable(promoId);
        exportPromoChannelYtTable.addChannel(3);
        Map<String, ExportPromoChannelYtTable> promosChannels = Map.of(promoId, exportPromoChannelYtTable);
        Map<String, ExportMskuPromoYtTable> restrictionMsku = Collections.emptyMap();
        List<ExportParentPromoYtTable> promosParent = Collections.emptyList();

        ArgumentCaptor<SyncGetPromo.UpdatePromoBatchRequest> promosSetCaptor =
                ArgumentCaptor.forClass(SyncGetPromo.UpdatePromoBatchRequest.class);
        ArgumentCaptor<Long> businessCapture = ArgumentCaptor.forClass(Long.class);
        doNothing().when(dataCampShopClient).addPromo(promosSetCaptor.capture(), businessCapture.capture());
        AnaplanExports anaplanExports = new AnaplanExports.Builder()
                .withPromoRestriction(promosRestriction)
                .withPromosChannels(promosChannels)
                .withPromosParent(promosParent.stream()
                        .collect(Collectors.toMap(ExportParentPromoYtTable::getId, p -> p)))
                .withRestrictionMsku(restrictionMsku)
                .build();
        promoDescriptionStorageService.sendPromoToPromoStorage(
                promos, anaplanExports, businessId);

        List<SyncGetPromo.UpdatePromoBatchRequest> capturedPromosSets = promosSetCaptor.getAllValues();
        Assertions.assertEquals(1, capturedPromosSets.size());
        SyncGetPromo.UpdatePromoBatchRequest logbrokerEvent = capturedPromosSets.get(0);
        Assertions.assertEquals(1, logbrokerEvent.getPromos().getPromoCount());
        DataCampPromo.PromoDescription actualPromoDescription = logbrokerEvent.getPromos().getPromo(0);

        Assertions.assertTrue(actualPromoDescription.hasUpdateInfo());
        Assertions.assertTrue(actualPromoDescription.getUpdateInfo().hasCreatedAt());
        Assertions.assertTrue(actualPromoDescription.getUpdateInfo().hasUpdatedAt());
        Assertions.assertEquals(
                actualPromoDescription.getUpdateInfo().getCreatedAt(),
                actualPromoDescription.getUpdateInfo().getUpdatedAt()
        );

        var processedPromoDescription = extractedWithTestMetaTime(actualPromoDescription);

        DataCampPromo.PromoDescription promoDescriptionExpected =
                DataCampPromo.PromoDescription.newBuilder()
                        .setPrimaryKey(
                                DataCampPromo.PromoDescriptionIdentifier.newBuilder()
                                        .setPromoId(promoId)
                                        .setBusinessId(Math.toIntExact(businessId))
                                        .setSource(Promo.ESourceType.ANAPLAN)
                                        .build())
                        .setPromoGeneralInfo(
                                DataCampPromo.PromoGeneralInfo.newBuilder()
                                        .setMeta(createTestMeta())
                                        .setPromoType(DataCampPromo.PromoType.CHEAPEST_AS_GIFT)
                                        .build())
                        .setConstraints(DataCampPromo.PromoConstraints.newBuilder()
                                .setMeta(createTestMeta())
                                .setStartDate(getDateInSeconds(promo.getPromoConstraints().getStartDate()))
                                .setEndDate(getDateInSeconds(promo.getPromoConstraints().getEndDate()))
                                .addOffersMatchingRules(
                                        DataCampPromo.PromoConstraints.OffersMatchingRule.newBuilder()
                                                .setCategoryRestriction(DataCampPromo.PromoConstraints.OffersMatchingRule
                                                        .CategoryRestriction.newBuilder()
                                                        .addPromoCategory(DataCampPromo.PromoConstraints
                                                                .OffersMatchingRule.PromoCategory.newBuilder()
                                                                .setId(2)
                                                                .setMinDiscount(0))
                                                        .build())
                                                .setOrigionalCategoryRestriction(DataCampPromo.PromoConstraints.OffersMatchingRule.OriginalCategoryRestriction.newBuilder()
                                                        .addIncludeCategegoryRestriction(DataCampPromo.PromoConstraints.OffersMatchingRule.PromoCategory.newBuilder()
                                                                .setId(2)
                                                                .build())
                                                        .setIncludeCategoryRestrictionsCount(Integer.parseInt(restrictionCnt))
                                                        .build())
                                                .build())
                                .setAllowMarketBonus(false)
                                .setAllowBluePromocode(false)
                                .setAllowCheapestAsGift(false)
                                .setAllowGenericBundle(false)
                                .setAllowBlueFlash(false)
                                .setAllowBlueSet(false)
                                .setHidden(false)
                                .setEnabled(true)
                                .setExcludeDbsSupplier(false)
                                .build())
                        .setResponsible(
                                DataCampPromo.PromoResponsible.newBuilder()
                                        .setMeta(createTestMeta())
                                        .setAuthor("author")
                                        .setTm("tm")
                                        .setMarcom("marcom")
                                        .setTmLogin("tm_login")
                                        .setMarcomLogin("marcom_login")
                                        .setApprovingManager("approving_manager")
                                        .build())
                        .setPromotion(DataCampPromo.PromoPromotion.newBuilder()
                                .setMeta(createTestMeta())
                                .addChannel(MAIN_BOTTOM_BANNER)
                                .build())
                        .setMechanicsData(DataCampPromo.PromoMechanics.newBuilder()
                                .setMeta(createTestMeta())
                                .setCheapestAsGift(DataCampPromo.PromoMechanics.CheapestAsGift.newBuilder()
                                        .setCount(promo.getCheapestAsGift().getCount())
                                        .build())
                                .build())
                        .setAdditionalInfo(DataCampPromo.PromoAdditionalInfo.newBuilder()
                                .setMeta(createTestMeta())
                                .setName("promo_name")
                                .setIsTestPromo(false)
                                .setAdhoc(true)
                                .setLendingUrl("promo_url")
                                .setLendingUrlText("link_text")
                                .setSendPromoPi(false)
                                .setCmLink("cm_link")
                                .setAssortmentAutopublication(true)
                                .setAssortmentLimit(1000)
                                .setStartrekRobotTicketLink("link_to_robot")
                                .setSupplierFlagRestriction("Express")
                                .setUpdatedAt(getDateInSeconds(LocalDateTime.now()))
                                .setStatus(DataCampPromo.PromoAdditionalInfo.PromoStatus.NEW)
                                .setChecklist(DataCampPromo.PromoAdditionalInfo.CheckList.newBuilder()
                                        .setApprovedByRg(false)
                                        .setApprovedByCd(false)
                                        .setChannelsReady(false)
                                        .setDesignReady(false)
                                        .setMediaplanApproved(false)
                                        .setLoyaltyPromoCreated(false)
                                        .setAssortmentApproved(false)
                                        .setAssortmentReady(false)
                                        .build())
                                .setWithoutAutolandingGeneration(false)
                                .build())
                        .build();

        Assertions.assertEquals(promoDescriptionExpected, processedPromoDescription);
    }

    @Test
    public void testPromoForCategory() {
        String promoId = "#promo_id";
        long businessId = 0L;
        String restrictionCnt = "1";
        ExportOperationalPromoYtTable promo = new ExportOperationalPromoYtTable.Builder()
                .withId(promoId)
                .withName("promo_name")
                .withType(PromoType.CHEAPEST_AS_GIFT)
                .withStatus(AnaplanPromoStatus.NEW)
                .withPromoCheckInfo(createCheckInfo(promoId, restrictionCnt))
                .withPromoConstraints(
                        new PromoConstraintsYt.Builder()
                                .withStartDate("2020-12-22T00:00:00")
                                .withEndDate("2021-01-01T00:00:00")
                                .build())
                .withPromoResponsible(new PromoResponsibleYt.Builder()
                        .withAuthor("author")
                        .build())
                .withCheapestAsGift(new CheapestAsGiftYt("3"))
                .build();
        List<ExportOperationalPromoYtTable> promos = List.of(promo);

        ExportRestrictionYtTable exportRestrictionYtTable =
                new ExportRestrictionYtTable.Builder(promoId)
                        .withPromosHidRestrictionCategory(
                                Map.of(
                                        2L, new ExportRestrictionYtTable.HidCategory.Builder()
                                                .withHid("2")
                                                .withDiscount("")
                                                .build()
                                ))
                        .withOriginalRestrictionCategory(
                                new ExportRestrictionYtTable.OriginalCategoryRestriction("false")
                                        .addCategory("2", "")
                        )
                        .build();

        Map<String, ExportRestrictionYtTable> promosRestriction = Map.of(promoId, exportRestrictionYtTable);
        ExportPromoChannelYtTable exportPromoChannelYtTable = new ExportPromoChannelYtTable(promoId);
        exportPromoChannelYtTable.addChannel(3);
        Map<String, ExportPromoChannelYtTable> promosChannels = Map.of(promoId, exportPromoChannelYtTable);
        Map<String, ExportMskuPromoYtTable> restrictionMsku = Collections.emptyMap();
        List<ExportParentPromoYtTable> promosParent = Collections.emptyList();

        ArgumentCaptor<SyncGetPromo.UpdatePromoBatchRequest> promosSetCaptor =
                ArgumentCaptor.forClass(SyncGetPromo.UpdatePromoBatchRequest.class);
        ArgumentCaptor<Long> businessCapture = ArgumentCaptor.forClass(Long.class);
        doNothing().when(dataCampShopClient).addPromo(promosSetCaptor.capture(), businessCapture.capture());
        AnaplanExports anaplanExports = new AnaplanExports.Builder()
                .withPromoRestriction(promosRestriction)
                .withPromosChannels(promosChannels)
                .withPromosParent(promosParent.stream()
                        .collect(Collectors.toMap(ExportParentPromoYtTable::getId, p -> p)))
                .withRestrictionMsku(restrictionMsku)
                .build();
        promoDescriptionStorageService.sendPromoToPromoStorage(
                promos, anaplanExports, businessId);

        List<SyncGetPromo.UpdatePromoBatchRequest> capturedPromosSets = promosSetCaptor.getAllValues();
        Assertions.assertEquals(1, capturedPromosSets.size());
        SyncGetPromo.UpdatePromoBatchRequest logbrokerEvent = capturedPromosSets.get(0);
        Assertions.assertEquals(1, logbrokerEvent.getPromos().getPromoCount());
        DataCampPromo.PromoDescription actualPromoDescription = logbrokerEvent.getPromos().getPromo(0);

        Assertions.assertTrue(actualPromoDescription.hasUpdateInfo());
        Assertions.assertTrue(actualPromoDescription.getUpdateInfo().hasCreatedAt());
        Assertions.assertTrue(actualPromoDescription.getUpdateInfo().hasUpdatedAt());
        Assertions.assertEquals(
                actualPromoDescription.getUpdateInfo().getCreatedAt(),
                actualPromoDescription.getUpdateInfo().getUpdatedAt()
        );

        var processedPromoDescription = extractedWithTestMetaTime(actualPromoDescription);

        DataCampPromo.PromoDescription promoDescriptionExpected =
                DataCampPromo.PromoDescription.newBuilder()
                        .setPrimaryKey(
                                DataCampPromo.PromoDescriptionIdentifier.newBuilder()
                                        .setPromoId(promoId)
                                        .setBusinessId(Math.toIntExact(businessId))
                                        .setSource(Promo.ESourceType.ANAPLAN)
                                        .build())
                        .setPromoGeneralInfo(
                                DataCampPromo.PromoGeneralInfo.newBuilder()
                                        .setMeta(createTestMeta())
                                        .setPromoType(DataCampPromo.PromoType.CHEAPEST_AS_GIFT)
                                        .build())
                        .setResponsible(DataCampPromo.PromoResponsible.newBuilder()
                                .setMeta(createTestMeta())
                                .setAuthor("author")
                                .build())
                        .setConstraints(DataCampPromo.PromoConstraints.newBuilder()
                                .setMeta(createTestMeta())
                                .setStartDate(getDateInSeconds(promo.getPromoConstraints().getStartDate()))
                                .setEndDate(getDateInSeconds(promo.getPromoConstraints().getEndDate()))
                                .addOffersMatchingRules(
                                        DataCampPromo.PromoConstraints.OffersMatchingRule.newBuilder()
                                                .setCategoryRestriction(DataCampPromo.PromoConstraints.OffersMatchingRule
                                                        .CategoryRestriction.newBuilder()
                                                        .addPromoCategory(DataCampPromo.PromoConstraints
                                                                .OffersMatchingRule.PromoCategory.newBuilder()
                                                                .setId(2)
                                                                .setMinDiscount(0))
                                                        .build())
                                                .setOrigionalCategoryRestriction(DataCampPromo.PromoConstraints.OffersMatchingRule.OriginalCategoryRestriction.newBuilder()
                                                        .addIncludeCategegoryRestriction(DataCampPromo.PromoConstraints.OffersMatchingRule.PromoCategory.newBuilder()
                                                                .setId(2)
                                                                .build())
                                                        .setIncludeCategoryRestrictionsCount(Integer.parseInt(restrictionCnt))
                                                        .build())
                                                .build())
                                .setAllowMarketBonus(false)
                                .setAllowBluePromocode(false)
                                .setAllowCheapestAsGift(false)
                                .setAllowGenericBundle(false)
                                .setAllowBlueFlash(false)
                                .setAllowBlueSet(false)
                                .setHidden(false)
                                .setEnabled(true)
                                .setExcludeDbsSupplier(false)
                                .build())
                        .setPromotion(DataCampPromo.PromoPromotion.newBuilder()
                                .setMeta(createTestMeta())
                                .addChannel(MAIN_BOTTOM_BANNER)
                                .build())
                        .setMechanicsData(DataCampPromo.PromoMechanics.newBuilder()
                                .setMeta(createTestMeta())
                                .setCheapestAsGift(DataCampPromo.PromoMechanics.CheapestAsGift.newBuilder()
                                        .setCount(promo.getCheapestAsGift().getCount())
                                        .build())
                                .build())
                        .setAdditionalInfo(DataCampPromo.PromoAdditionalInfo.newBuilder()
                                .setMeta(createTestMeta())
                                .setName("promo_name")
                                .setStatus(DataCampPromo.PromoAdditionalInfo.PromoStatus.NEW)
                                .build())
                        .build();

        Assertions.assertEquals(promoDescriptionExpected, processedPromoDescription);
    }

    /**
     * Проверяем, что бренды, добавленные для описания промо добавляются верно в ограничения
     */
    @Test
    public void testBrandsForPi() {
        String promoId = "#12";
        long businessId = 0L;
        String restrictionCnt = "1";
        ExportOperationalPromoYtTable promo = new ExportOperationalPromoYtTable.Builder()
                .withId(promoId)
                .withName("promo_name")
                .withType(PromoType.CHEAPEST_AS_GIFT)
                .withCheapestAsGift(new CheapestAsGiftYt("3"))
                .withPromoCheckInfo(createCheckInfo(promoId, restrictionCnt))
                .build();
        List<ExportOperationalPromoYtTable> promos = List.of(promo);
        ExportRestrictionYtTable exportRestrictionYtTable =
                new ExportRestrictionYtTable.Builder(promoId)
                        .withOriginalRestrictionBrands(new ExportRestrictionYtTable.Brands("1", "true"))
                        .withPromosHidRestrictionCategory(
                                Map.of(2L, new ExportRestrictionYtTable.HidCategory.Builder()
                                        .withHid("2")
                                        .withDiscount("")
                                        .build()))
                        .withOriginalRestrictionCategory(
                                new ExportRestrictionYtTable.OriginalCategoryRestriction("false")
                                        .addCategory("2", ""))
                        .build();
        Map<String, ExportRestrictionYtTable> promosRestriction = Map.of(promoId, exportRestrictionYtTable);
        Map<String, ExportPromoChannelYtTable> promosChannels = Collections.emptyMap();
        Map<String, ExportMskuPromoYtTable> restrictionMsku = Collections.emptyMap();
        List<ExportParentPromoYtTable> promosParent = Collections.emptyList();

        ArgumentCaptor<SyncGetPromo.UpdatePromoBatchRequest> promosSetCaptor =
                ArgumentCaptor.forClass(SyncGetPromo.UpdatePromoBatchRequest.class);
        ArgumentCaptor<Long> businessCapture = ArgumentCaptor.forClass(Long.class);
        doNothing().when(dataCampShopClient).addPromo(promosSetCaptor.capture(), businessCapture.capture());
        AnaplanExports anaplanExports = new AnaplanExports.Builder()
                .withPromoRestriction(promosRestriction)
                .withPromosChannels(promosChannels)
                .withPromosParent(promosParent.stream()
                        .collect(Collectors.toMap(ExportParentPromoYtTable::getId, p -> p)))
                .withRestrictionMsku(restrictionMsku)
                .build();
        promoDescriptionStorageService.sendPromoToPromoStorage(promos, anaplanExports, businessId);

        List<SyncGetPromo.UpdatePromoBatchRequest> capturedPromosSets = promosSetCaptor.getAllValues();
        Assertions.assertEquals(1, capturedPromosSets.size());
        SyncGetPromo.UpdatePromoBatchRequest logbrokerEvent = capturedPromosSets.get(0);
        Assertions.assertEquals(1, logbrokerEvent.getPromos().getPromoCount());
        DataCampPromo.PromoDescription actualPromoDescription = logbrokerEvent.getPromos().getPromo(0);

        Assertions.assertTrue(actualPromoDescription.hasUpdateInfo());
        Assertions.assertTrue(actualPromoDescription.getUpdateInfo().hasCreatedAt());
        Assertions.assertTrue(actualPromoDescription.getUpdateInfo().hasUpdatedAt());
        Assertions.assertEquals(
                actualPromoDescription.getUpdateInfo().getCreatedAt(),
                actualPromoDescription.getUpdateInfo().getUpdatedAt()
        );

        var processedPromoDescription = extractedWithTestMetaTime(actualPromoDescription);

        DataCampPromo.PromoDescription promoDescriptionExpected =
                DataCampPromo.PromoDescription.newBuilder()
                        .setPrimaryKey(
                                DataCampPromo.PromoDescriptionIdentifier.newBuilder()
                                        .setPromoId(promoId)
                                        .setBusinessId(Math.toIntExact(businessId))
                                        .setSource(Promo.ESourceType.ANAPLAN)
                                        .build())
                        .setPromoGeneralInfo(
                                DataCampPromo.PromoGeneralInfo.newBuilder()
                                        .setMeta(createTestMeta())
                                        .setPromoType(DataCampPromo.PromoType.CHEAPEST_AS_GIFT)
                                        .build())
                        .setResponsible(
                                DataCampPromo.PromoResponsible.newBuilder()
                                        .setMeta(createTestMeta())
                                        .build())
                        .setPromotion(
                                DataCampPromo.PromoPromotion.newBuilder()
                                        .setMeta(createTestMeta())
                                        .build())
                        .setConstraints(DataCampPromo.PromoConstraints.newBuilder()
                                .setMeta(createTestMeta())
                                .addOffersMatchingRules(
                                        DataCampPromo.PromoConstraints.OffersMatchingRule.newBuilder()
                                                .setCategoryRestriction(DataCampPromo.PromoConstraints.OffersMatchingRule
                                                        .CategoryRestriction.newBuilder()
                                                        .addPromoCategory(DataCampPromo.PromoConstraints
                                                                .OffersMatchingRule.PromoCategory.newBuilder()
                                                                .setId(2)
                                                                .setMinDiscount(0))
                                                        .build())
                                                .setOrigionalCategoryRestriction(DataCampPromo.PromoConstraints.OffersMatchingRule.OriginalCategoryRestriction.newBuilder()
                                                        .addAllIncludeCategegoryRestriction(List.of(
                                                                DataCampPromo.PromoConstraints.OffersMatchingRule.PromoCategory.newBuilder()
                                                                        .setId(2)
                                                                        .build()))
                                                        .setIncludeCategoryRestrictionsCount(Integer.parseInt(restrictionCnt))
                                                        .build())
                                                .setOriginalBrandRestriction(DataCampPromo.PromoConstraints.OffersMatchingRule.OriginalBrandRestriction.newBuilder()
                                                        .setExcludeBrands(DataCampPromo.PromoConstraints.OffersMatchingRule.PromoBrands.newBuilder()
                                                                .addBrands(DataCampPromo.PromoBrand.newBuilder()
                                                                        .setId(1)
                                                                        .build())
                                                                .build())
                                                        .setBrandRestrictionCount(Integer.parseInt(restrictionCnt))
                                                        .build())
                                                .build())
                                .build())
                        .setMechanicsData(DataCampPromo.PromoMechanics.newBuilder()
                                .setMeta(createTestMeta())
                                .setCheapestAsGift(DataCampPromo.PromoMechanics.CheapestAsGift.newBuilder()
                                        .setCount(promo.getCheapestAsGift().getCount())
                                        .build())
                                .build())
                        .setAdditionalInfo(DataCampPromo.PromoAdditionalInfo.newBuilder()
                                .setMeta(createTestMeta())
                                .setName("promo_name")
                                .build())
                        .build();

        Assertions.assertEquals(promoDescriptionExpected, processedPromoDescription);
    }

    private DataCampPromo.PromoDescription extractedWithTestMetaTime(DataCampPromo.PromoDescription promoDescription) {
        return DataCampPromo.PromoDescription.newBuilder(promoDescription)
                .setPromoGeneralInfo(
                        DataCampPromo.PromoGeneralInfo.newBuilder(promoDescription.getPromoGeneralInfo())
                                .setMeta(createTestMeta())
                                .build())
                .setConstraints(
                        DataCampPromo.PromoConstraints.newBuilder(promoDescription.getConstraints())
                                .setMeta(createTestMeta())
                                .build())
                .setResponsible(
                        DataCampPromo.PromoResponsible.newBuilder(promoDescription.getResponsible())
                                .setMeta(createTestMeta())
                                .build())
                .setPromotion(
                        DataCampPromo.PromoPromotion.newBuilder(promoDescription.getPromotion())
                                .setMeta(createTestMeta())
                                .build())
                .setMechanicsData(
                        DataCampPromo.PromoMechanics.newBuilder(promoDescription.getMechanicsData())
                                .setMeta(createTestMeta())
                                .build())
                .setAdditionalInfo(
                        DataCampPromo.PromoAdditionalInfo.newBuilder(promoDescription.getAdditionalInfo())
                                .setMeta(createTestMeta())
                                .clearPriority()
                                .build())
                .clearUpdateInfo()
                .build();
    }

    private DataCampOfferMeta.UpdateMeta createTestMeta() {
        Instant instant = Instant.ofEpochSecond(1608165983L, 274462000L);
        return DataCampOfferMeta.UpdateMeta.newBuilder()
                .setSource(DataCampOfferMeta.DataSource.MARKET_MBI)
                .setTimestamp(DateTimes.toTimestamp(instant))
                .build();
    }

    private PromoCheckInfo createCheckInfo(String promoId, String cnt) {
        return new PromoCheckInfo.Builder()
                .withPromoId(promoId)
                .withChannelsCount(cnt)
                .withHidTriggerCount(cnt)
                .withHidTriggerExcludeCount(cnt)
                .withMskuCount(cnt)
                .withMskuExcludeCount(cnt)
                .withOriginalBrandCount(cnt)
                .withOriginalExcludeRestrictionCategoryCount(cnt)
                .withOriginalExcludeBrandCount(cnt)
                .withOriginalRestrictionCategoryCount(cnt)
                .withRegionCount(cnt)
                .withRegionExcludeCount(cnt)
                .withRegionTriggerCount(cnt)
                .withRegionTriggerExcludeCount(cnt)
                .withSupplierCount(cnt)
                .withSupplierExcludeCount(cnt)
                .withVendorTriggerCount(cnt)
                .withVendorTriggerExcludeCount(cnt)
                .withWarehouseCount(cnt)
                .withWarehouseExcludeCount(cnt)
                .build();
    }
}
