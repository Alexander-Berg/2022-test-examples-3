package ru.yandex.direct.core.entity.pricepackage.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import org.jooq.DSLContext;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.CompareStrategy;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies;
import ru.yandex.direct.core.entity.adgroup.model.AdGroupType;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierInventory;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierInventoryAdjustment;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierMobile;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierType;
import ru.yandex.direct.core.entity.bidmodifier.InventoryType;
import ru.yandex.direct.core.entity.pricepackage.model.PricePackage;
import ru.yandex.direct.core.entity.pricepackage.model.PricePackageClient;
import ru.yandex.direct.core.entity.pricepackage.model.PricePackageForClient;
import ru.yandex.direct.core.entity.pricepackage.model.PricePackagePlatform;
import ru.yandex.direct.core.entity.pricepackage.model.PricePackagesFilter;
import ru.yandex.direct.core.entity.pricepackage.model.StatusApprove;
import ru.yandex.direct.core.entity.pricepackage.model.TargetingsCustom;
import ru.yandex.direct.core.entity.pricepackage.repository.PricePackageRepository.PricePackagesWithTotalCount;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.PricePackageInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.model.AppliedChanges;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.multitype.entity.LimitOffset;
import ru.yandex.direct.test.utils.differ.BigDecimalDiffer;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;
import static ru.yandex.direct.core.testing.data.TestPricePackages.allowedPricePackageClient;
import static ru.yandex.direct.core.testing.data.TestPricePackages.anotherPricePackage;
import static ru.yandex.direct.core.testing.data.TestPricePackages.approvedPricePackage;
import static ru.yandex.direct.core.testing.data.TestPricePackages.defaultPricePackage;
import static ru.yandex.direct.core.testing.data.TestPricePackages.disallowedPricePackageClient;
import static ru.yandex.direct.regions.Region.BY_REGION_ID;
import static ru.yandex.direct.regions.Region.REGION_TYPE_DISTRICT;
import static ru.yandex.direct.regions.Region.SAINT_PETERSBURG_AND_LENINGRAD_OBLAST_REGION_ID;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

@CoreTest
@RunWith(SpringRunner.class)
public class PricePackageRepositoryTest {

    private static final PricePackage PRICE_PACKAGE_1 = defaultPricePackage();
    private static final PricePackage PRICE_PACKAGE_2 = anotherPricePackage();
    private static final PricePackage APPROVED_PRICE_PACKAGED = approvedPricePackage();

    private static final CompareStrategy PRICE_PACKAGES_COMPARE_STRATEGY = DefaultCompareStrategies
            .allFieldsExcept(newPath("id"))
            .forFields(newPath("price")).useDiffer(new BigDecimalDiffer())
            .forFields(newPath("eshow")).useDiffer(new BigDecimalDiffer());

    @Autowired
    private Steps steps;
    @Autowired
    private PricePackageRepository pricePackageRepository;
    @Autowired
    private DslContextProvider dslContextProvider;

    private DSLContext dslContext;

    @Before
    public void before() {
        dslContext = dslContextProvider.ppcdict();
        steps.pricePackageSteps().clearPricePackages();
    }

    @Test
    public void getExistent() {
        List<Long> ids = pricePackageRepository.addPricePackages(List.of(PRICE_PACKAGE_1, PRICE_PACKAGE_2));

        Long id1 = ids.get(0);
        Long id2 = ids.get(1);
        var actual = pricePackageRepository.getPricePackages(ids);

        assertThat(actual.size()).isEqualTo(2);
        assertThat(actual.get(id1))
                .is(matchedBy(beanDiffer(PRICE_PACKAGE_1).useCompareStrategy(PRICE_PACKAGES_COMPARE_STRATEGY)));
        assertThat(actual.get(id2))
                .is(matchedBy(beanDiffer(PRICE_PACKAGE_2).useCompareStrategy(PRICE_PACKAGES_COMPARE_STRATEGY)));
    }

    @Test
    public void getMixed() {
        List<Long> ids = pricePackageRepository.addPricePackages(List.of(PRICE_PACKAGE_1));

        Long id1 = ids.get(0);
        ids.add(333444L);

        var actual = pricePackageRepository.getPricePackages(ids);
        assertThat(actual.size()).isEqualTo(1);
        assertThat(actual.get(id1))
                .is(matchedBy(beanDiffer(PRICE_PACKAGE_1).useCompareStrategy(PRICE_PACKAGES_COMPARE_STRATEGY)));
    }

    @Test
    public void getNonexistent() {
        var actual = pricePackageRepository.getPricePackages(List.of(12321L));
        assertThat(actual.size()).isEqualTo(0);
    }

    @Test
    public void getPricePackagesOrderById_WithLimit() {
        pricePackageRepository.addPricePackages(List.of(PRICE_PACKAGE_1, PRICE_PACKAGE_2, APPROVED_PRICE_PACKAGED));

        PricePackagesWithTotalCount actual = pricePackageRepository.getPricePackages(
                new PricePackagesFilter(), null, LimitOffset.limited(1, 1));

        assertThat(actual.getTotalCount()).isEqualTo(3);
        assertThat(actual.getPricePackages().size()).isEqualTo(1);
        assertThat(actual.getPricePackages().get(0))
                .is(matchedBy(beanDiffer(PRICE_PACKAGE_2).useCompareStrategy(PRICE_PACKAGES_COMPARE_STRATEGY)));
    }

    @Test
    public void getPricePackagesOrderById_WithLimitAndIdFilter() {
        List<Long> ids = pricePackageRepository.addPricePackages(List.of(PRICE_PACKAGE_1, PRICE_PACKAGE_2,
                APPROVED_PRICE_PACKAGED));

        PricePackagesWithTotalCount actual = pricePackageRepository.getPricePackages(
                new PricePackagesFilter().withPackageIdIn(Set.of(ids.get(0), ids.get(2))),
                null,
                LimitOffset.limited(1, 1));

        assertThat(actual.getTotalCount()).isEqualTo(2);
        assertThat(actual.getPricePackages().size()).isEqualTo(1);
        assertThat(actual.getPricePackages().get(0))
                .is(matchedBy(beanDiffer(APPROVED_PRICE_PACKAGED).useCompareStrategy(PRICE_PACKAGES_COMPARE_STRATEGY)));
    }

    @Test
    public void addPricePackage() {
        var pricePackage = PRICE_PACKAGE_1
                .withClients(List.of(allowedPricePackageClient(1L), disallowedPricePackageClient(2L)));
        var pricePackageId = pricePackageRepository.addPricePackages(List.of(pricePackage)).get(0);
        var pricePackageFromDb = pricePackageRepository.getPricePackages(List.of(pricePackageId)).get(pricePackageId);
        assertThat(pricePackageFromDb)
                .is(matchedBy(beanDiffer(pricePackage).useCompareStrategy(PRICE_PACKAGES_COMPARE_STRATEGY)));
    }

    @Test
    public void addPricePackage_WithNoClients() {
        var pricePackage = PRICE_PACKAGE_1.withClients(List.of());
        var pricePackageId = pricePackageRepository.addPricePackages(List.of(pricePackage)).get(0);
        var pricePackageFromDb = getPricePackage(pricePackageId);
        assertThat(pricePackageFromDb)
                .is(matchedBy(beanDiffer(pricePackage).useCompareStrategy(PRICE_PACKAGES_COMPARE_STRATEGY)));
    }

    @Test
    public void updatePricePackage_ChangesInClients() {
        PricePackageInfo pricePackage = steps.pricePackageSteps().createPricePackage(defaultPricePackage()
                .withClients(List.of(
                        allowedPricePackageClient(1L),
                        allowedPricePackageClient(2L),
                        allowedPricePackageClient(3L)
                )));

        AppliedChanges<PricePackage> changes = appliedChangesWithClients(pricePackage, List.of(
                allowedPricePackageClient(2L),
                disallowedPricePackageClient(3L),
                disallowedPricePackageClient(4L)
        ));
        pricePackageRepository.updatePricePackages(dslContext, List.of(changes));

        PricePackage expectedPricePackage = pricePackage.getPricePackage()
                .withClients(List.of(
                        allowedPricePackageClient(2L),
                        disallowedPricePackageClient(3L),
                        disallowedPricePackageClient(4L)
                ));
        PricePackage pricePackageFromDb = getPricePackage(pricePackage.getPricePackageId());
        assertThat(pricePackageFromDb)
                .is(matchedBy(beanDiffer(expectedPricePackage).useCompareStrategy(PRICE_PACKAGES_COMPARE_STRATEGY)));
    }

    @Test
    public void updatePricePackage_NoChangesInClients() {
        PricePackageInfo pricePackage = steps.pricePackageSteps().createPricePackage(defaultPricePackage()
                .withClients(List.of(
                        allowedPricePackageClient(1L),
                        disallowedPricePackageClient(2L)
                )));

        AppliedChanges<PricePackage> changes = appliedChangesWithClients(pricePackage, List.of(
                allowedPricePackageClient(1L),
                disallowedPricePackageClient(2L)
        ));
        pricePackageRepository.updatePricePackages(dslContext, List.of(changes));

        PricePackage expectedPricePackage = pricePackage.getPricePackage()
                .withClients(List.of(
                        allowedPricePackageClient(1L),
                        disallowedPricePackageClient(2L)
                ));
        PricePackage pricePackageFromDb = getPricePackage(pricePackage.getPricePackageId());
        assertThat(pricePackageFromDb)
                .is(matchedBy(beanDiffer(expectedPricePackage).useCompareStrategy(PRICE_PACKAGES_COMPARE_STRATEGY)));
    }

    @Test
    public void updatePricePackage_ToEmptyClients() {
        PricePackageInfo pricePackage = steps.pricePackageSteps().createPricePackage(defaultPricePackage()
                .withClients(List.of(
                        allowedPricePackageClient(1L),
                        disallowedPricePackageClient(2L)
                )));

        AppliedChanges<PricePackage> changes = appliedChangesWithClients(pricePackage, List.of());
        pricePackageRepository.updatePricePackages(dslContext, List.of(changes));

        PricePackage expectedPricePackage = pricePackage.getPricePackage()
                .withClients(List.of());
        PricePackage pricePackageFromDb = getPricePackage(pricePackage.getPricePackageId());
        assertThat(pricePackageFromDb)
                .is(matchedBy(beanDiffer(expectedPricePackage).useCompareStrategy(PRICE_PACKAGES_COMPARE_STRATEGY)));
    }

    @Test
    public void getActivePricePackages() {
        long currentClientId = 1L;
        PricePackageClient currentPackageClient = allowedPricePackageClient(currentClientId);
        long anotherClientId = 2L;
        PricePackageClient anotherClient = allowedPricePackageClient(anotherClientId);
        CurrencyCode currency = CurrencyCode.RUB;

        steps.pricePackageSteps().clearPricePackages();
        PricePackageInfo pricePackageInPast = steps.pricePackageSteps().createPricePackage(approvedPricePackage()
                .withDateStart(LocalDate.of(2017, 12, 30))
                .withDateEnd(LocalDate.of(2017, 12, 31))
                .withCurrency(currency)
                .withClients(List.of(currentPackageClient)));
        PricePackageInfo pricePackageInPresent = steps.pricePackageSteps().createPricePackage(approvedPricePackage()
                .withDateStart(LocalDate.now().minusDays(1))
                .withDateEnd(LocalDate.now().plusDays(1))
                .withCurrency(currency)
                .withClients(List.of(currentPackageClient)));
        PricePackageInfo pricePackageInFuture = steps.pricePackageSteps().createPricePackage(approvedPricePackage()
                .withDateStart(LocalDate.now().plusMonths(3))
                .withDateEnd(LocalDate.now().plusMonths(4))
                .withCurrency(currency)
                .withClients(List.of(currentPackageClient)));
        PricePackageInfo pricePackageForAnotherClient =
                steps.pricePackageSteps().createPricePackage(approvedPricePackage()
                        .withDateStart(LocalDate.now().minusDays(1))
                        .withDateEnd(LocalDate.now().plusDays(1))
                        .withCurrency(currency)
                        .withClients(List.of(anotherClient)));
        PricePackageInfo pricePackageNotApproved = steps.pricePackageSteps().createPricePackage(defaultPricePackage()
                .withDateStart(LocalDate.now().plusMonths(3))
                .withDateEnd(LocalDate.now().plusMonths(4))
                .withStatusApprove(StatusApprove.NO)
                .withCurrency(currency)
                .withClients(List.of(currentPackageClient)));
        PricePackageInfo pricePackageArchived = steps.pricePackageSteps().createPricePackage(approvedPricePackage()
                .withDateStart(LocalDate.now().minusDays(1))
                .withDateEnd(LocalDate.now().plusDays(1))
                .withIsArchived(true)
                .withCurrency(currency)
                .withClients(List.of(currentPackageClient)));

        List<PricePackageForClient> availablePricePackages =
                pricePackageRepository.getActivePricePackagesForClient(ClientId.fromLong(currentClientId), currency,
                        new PricePackagesFilter());
        List<Long> availablePricePackageIds =
                mapList(availablePricePackages, PricePackageForClient::getId);

        assertThat(availablePricePackageIds).containsExactlyInAnyOrder(
                pricePackageInPresent.getPricePackageId(), pricePackageInFuture.getPricePackageId());
    }

    @Test
    public void getPricePackagesForClientWithFilters() {
        long currentClientId = 1L;
        PricePackageClient currentPackageClient = allowedPricePackageClient(currentClientId);
        long anotherClientId = 2L;
        PricePackageClient anotherClient = allowedPricePackageClient(anotherClientId);
        CurrencyCode currency = CurrencyCode.RUB;

        steps.pricePackageSteps().clearPricePackages();
        PricePackageInfo pricePackageCpmBanner = steps.pricePackageSteps().createPricePackage(approvedPricePackage()
                .withCurrency(currency)
                .withClients(List.of(currentPackageClient))
                .withAvailableAdGroupTypes(Set.of(AdGroupType.CPM_BANNER)));
        PricePackageInfo pricePackageCpmBannerMobile = steps.pricePackageSteps().createPricePackage(approvedPricePackage()
                .withCurrency(currency)
                .withClients(List.of(currentPackageClient))
                .withAvailableAdGroupTypes(Set.of(AdGroupType.CPM_BANNER))
                .withBidModifiers(List.of(new BidModifierMobile().withType(BidModifierType.MOBILE_MULTIPLIER))));
        PricePackageInfo pricePackageCpmBannerMobileGeo = steps.pricePackageSteps().createPricePackage(approvedPricePackage()
                .withCurrency(currency)
                .withClients(List.of(currentPackageClient))
                .withAvailableAdGroupTypes(Set.of(AdGroupType.CPM_BANNER))
                .withBidModifiers(List.of(new BidModifierMobile().withType(BidModifierType.MOBILE_MULTIPLIER)))
                .withTargetingsCustom(new TargetingsCustom()
                        .withGeo(List.of(BY_REGION_ID))
                        .withGeoType(REGION_TYPE_DISTRICT)
                        .withGeoExpanded(List.of(BY_REGION_ID))));
        PricePackageInfo pricePackageCpmBannerMobileGeoSpecial = steps.pricePackageSteps().createPricePackage(
                approvedPricePackage()
                        .withCurrency(currency)
                        .withClients(List.of(currentPackageClient))
                        .withAvailableAdGroupTypes(Set.of(AdGroupType.CPM_BANNER))
                        .withIsSpecial(true)
                        .withBidModifiers(List.of(new BidModifierMobile().withType(BidModifierType.MOBILE_MULTIPLIER)))
                        .withTargetingsCustom(new TargetingsCustom()
                                .withGeo(List.of(BY_REGION_ID))
                                .withGeoType(REGION_TYPE_DISTRICT)
                                .withGeoExpanded(List.of(BY_REGION_ID))));
        PricePackageInfo pricePackageCpmBannerInventoryGeoSpecial = steps.pricePackageSteps().createPricePackage(
                approvedPricePackage()
                        .withCurrency(currency)
                        .withClients(List.of(currentPackageClient))
                        .withAvailableAdGroupTypes(Set.of(AdGroupType.CPM_BANNER))
                        .withIsSpecial(true)
                        .withBidModifiers(List.of(new BidModifierInventory()
                                .withType(BidModifierType.INVENTORY_MULTIPLIER)
                                .withInventoryAdjustments(List.of(new BidModifierInventoryAdjustment()
                                        .withInventoryType(InventoryType.INAPP)))))
                        .withTargetingsCustom(new TargetingsCustom()
                                .withGeo(List.of(BY_REGION_ID))
                                .withGeoType(REGION_TYPE_DISTRICT)
                                .withGeoExpanded(List.of(BY_REGION_ID))));
        PricePackageInfo pricePackageCpmFrontpage = steps.pricePackageSteps().createPricePackage(approvedPricePackage()
                .withCurrency(currency)
                .withClients(List.of(currentPackageClient)));
        PricePackageInfo pricePackageArchived = steps.pricePackageSteps().createPricePackage(approvedPricePackage()
                .withIsArchived(true)
                .withCurrency(currency)
                .withClients(List.of(currentPackageClient)));

        List<PricePackageForClient> availablePricePackages =
                pricePackageRepository.getActivePricePackagesForClient(ClientId.fromLong(currentClientId), currency,
                        new PricePackagesFilter()
                                .withIsSpecial(true)
                                .withFormats(Set.of(AdGroupType.CPM_BANNER))
                                .withPlatforms(Set.of(PricePackagePlatform.MOBILE))
                                .withRegionIds(Set.of(102154L)));
        List<Long> availablePricePackageIds =
                mapList(availablePricePackages, PricePackageForClient::getId);

        assertThat(availablePricePackageIds).containsExactlyInAnyOrder(
                pricePackageCpmBannerMobileGeoSpecial.getPricePackageId(),
                pricePackageCpmBannerInventoryGeoSpecial.getPricePackageId());

        List<PricePackageForClient> availablePricePackagesPlatformAll =
                pricePackageRepository.getActivePricePackagesForClient(ClientId.fromLong(currentClientId), currency,
                        new PricePackagesFilter()
                                .withFormats(Set.of(AdGroupType.CPM_YNDX_FRONTPAGE))
                                .withPlatforms(Set.of(PricePackagePlatform.ALL))
                                .withRegionIds(Set.of(SAINT_PETERSBURG_AND_LENINGRAD_OBLAST_REGION_ID)));
        List<Long> availablePricePackagesPlatformAllIds =
                mapList(availablePricePackagesPlatformAll, PricePackageForClient::getId);

        assertThat(availablePricePackagesPlatformAllIds).containsExactlyInAnyOrder(
                pricePackageCpmFrontpage.getPricePackageId());
    }

    @Test
    public void frontpageBrandSafety() {
        PricePackage pricePackage = defaultPricePackage();
        pricePackage.getCampaignOptions().setAllowBrandSafety(null);
        Long id = pricePackageRepository.addPricePackages(List.of(pricePackage)).get(0);

        var actual = pricePackageRepository.getPricePackages(List.of(id));

        assertThat(actual.size()).isEqualTo(1);
        assertThat(actual.get(id).getCampaignOptions().getAllowBrandSafety()).isFalse();
    }

    private AppliedChanges<PricePackage> appliedChangesWithClients(PricePackageInfo pricePackage,
                                                                   List<PricePackageClient> clients) {
        ModelChanges<PricePackage> changes =
                ModelChanges.build(pricePackage.getPricePackageId(), PricePackage.class, PricePackage.CLIENTS, clients);
        return changes.applyTo(pricePackage.getPricePackage());
    }

    private PricePackage getPricePackage(Long pricePackageId) {
        return pricePackageRepository.getPricePackages(List.of(pricePackageId)).get(pricePackageId);
    }

}
