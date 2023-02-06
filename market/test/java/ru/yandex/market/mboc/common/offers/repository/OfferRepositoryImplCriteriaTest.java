package ru.yandex.market.mboc.common.offers.repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableList;
import org.apache.commons.lang.SystemUtils;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mbo.http.SkuBDApi;
import ru.yandex.market.mbo.lightmapper.criteria.Criteria;
import ru.yandex.market.mboc.common.db.jooq.generated.mbo_category.tables.pojos.BuyPromoPrice;
import ru.yandex.market.mboc.common.dict.SupplierRepository;
import ru.yandex.market.mboc.common.infrastructure.sql.TransactionHelper;
import ru.yandex.market.mboc.common.msku.BuyPromoPriceRepository;
import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.offers.model.OfferContent;
import ru.yandex.market.mboc.common.offers.repository.search.FmcgSuggestMappingBarcodeSkutchCriteria;
import ru.yandex.market.mboc.common.offers.repository.search.HasDeadlineOrOldProcessingStatusCriteria;
import ru.yandex.market.mboc.common.offers.repository.search.OfferCriterias;
import ru.yandex.market.mboc.common.offers.repository.search.OffersFilter;
import ru.yandex.market.mboc.common.offers.repository.search.StampCriteria;
import ru.yandex.market.mboc.common.test.YamlTestUtil;
import ru.yandex.market.mboc.common.utils.BaseDbTestClass;
import ru.yandex.market.mboc.common.utils.DateTimeUtils;
import ru.yandex.market.mboc.common.utils.OfferTestUtils;
import ru.yandex.market.mboc.http.MboMappings;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.mboc.common.offers.model.Offer.MappingConfidence.CONTENT;
import static ru.yandex.market.mboc.common.utils.MbocConstants.MBO_MAPPINGS_SERVICE_DEFAULT_USER;
import static ru.yandex.market.mboc.common.utils.MbocConstants.PROTO_API_USER;

/**
 * @author yuramalinov
 * @created 16.04.18
 */
@SuppressWarnings("checkstyle:MagicNumber")
public class OfferRepositoryImplCriteriaTest extends BaseDbTestClass {
    private int skuNum = 1;

    @Autowired
    private OfferRepositoryImpl repository;
    @Autowired
    private SupplierRepository supplierRepository;
    @Autowired
    private TransactionHelper transactionHelper;
    @Autowired
    private BuyPromoPriceRepository buyPromoPriceRepository;

    @Before
    public void loadData() {
        supplierRepository.insertBatch(YamlTestUtil.readSuppliersFromResource("suppliers/test-suppliers.yml"));
    }

    @Test
    public void testPromoSearch() {
        Offer matchedOffer = Offer.MappingType.APPROVED.set(
            baseOffer()
                .setCategoryIdForTests(99L, Offer.BindingKind.APPROVED)
                .setApprovedSkuMappingConfidence(CONTENT),
            OfferTestUtils.mapping(123)
        );
        Offer notMatchedOffer = Offer.MappingType.APPROVED.set(
            baseOffer()
                .setCategoryIdForTests(99L, Offer.BindingKind.APPROVED)
                .setApprovedSkuMappingConfidence(CONTENT),
            OfferTestUtils.mapping(321)
        );

        buyPromoPriceRepository.save(new BuyPromoPrice()
            .setMarketSkuId(matchedOffer.getApprovedSkuMapping().getMappingId())
            .setSupplierId(matchedOffer.getBusinessId())
        );

        runThenRollback(() -> {
            repository.insertOffers(matchedOffer, notMatchedOffer);
            List<Offer> found = repository.findOffers(
                new OffersFilter().addCriteria(OfferCriterias.isPromo())
            );
            Assertions.assertThat(found).containsOnly(matchedOffer);
        });
    }

    @Test
    public void testTextSearch() {
        validateCriteriaMatches(OfferCriterias.externalTextSearch("test"), true,
            baseOffer().setTitle("Test offer").setShopSku("some"),
            baseOffer().setTitle("Some offer").setShopSku("test-sku1"),
            baseOffer().setTitle("Test offer").setShopSku("test-sku2"));

        validateCriteriaMatches(OfferCriterias.externalTextSearch("test"), false,
            baseOffer().setTitle("Some offer").setShopSku("some"));
    }

    @Test
    public void testTextSearchBySkuId() {
        validateCriteriaMatches(OfferCriterias.externalTextSearch("123"), true,
            baseOffer().setTitle("title 123").setShopSku("sku-123")
                .setCategoryIdForTests(99L, Offer.BindingKind.APPROVED)
                .updateApprovedSkuMapping(OfferTestUtils.mapping(123L), CONTENT), // match by all fields
            baseOffer().setTitle("Test offer").setShopSku("some")
                .setCategoryIdForTests(99L, Offer.BindingKind.APPROVED)
                .updateApprovedSkuMapping(OfferTestUtils.mapping(123L), CONTENT), // by skuId only
            baseOffer().setTitle("offer 123").setShopSku("another-123")
                .setCategoryIdForTests(99L, Offer.BindingKind.APPROVED)
                .updateApprovedSkuMapping(OfferTestUtils.mapping(1L), CONTENT), // by title and shopSku
            baseOffer().setTitle("Test offer").setShopSku("123-is-shop-sku")
                .setCategoryIdForTests(99L, Offer.BindingKind.APPROVED)
                .updateApprovedSkuMapping(OfferTestUtils.mapping(1L), CONTENT), // by shopSku only
            baseOffer().setTitle("123").setShopSku("other")
                .setCategoryIdForTests(99L, Offer.BindingKind.APPROVED)
                .updateApprovedSkuMapping(OfferTestUtils.mapping(1L), CONTENT) // by title only
        );

        validateCriteriaMatches(OfferCriterias.externalTextSearch("123abc"), false,
            baseOffer()
                .setCategoryIdForTests(99L, Offer.BindingKind.APPROVED)
                .updateApprovedSkuMapping(OfferTestUtils.mapping(123L), CONTENT));

        validateCriteriaMatches(OfferCriterias.externalTextSearch("123"), false,
            baseOffer()
                .setCategoryIdForTests(99L, Offer.BindingKind.APPROVED)
                .updateApprovedSkuMapping(OfferTestUtils.mapping(1L), CONTENT),
            baseOffer()
                .setCategoryIdForTests(99L, Offer.BindingKind.APPROVED)
                .updateApprovedSkuMapping(OfferTestUtils.mapping(2L), CONTENT));
    }

    @Test
    public void testTextSearchRus() {
        if (SystemUtils.IS_OS_WINDOWS) {
            // Encoding settings of embedded postgres does not allow to search russian text on Windows.
            return;
        }
        validateCriteriaMatches(OfferCriterias.externalTextSearch("тест"), true,
            baseOffer().setTitle("Тест offer").setShopSku("some"),
            baseOffer().setTitle("Some offer").setShopSku("тест-sku1"),
            baseOffer().setTitle("тест offer").setShopSku("test-sku2"));

        validateCriteriaMatches(OfferCriterias.externalTextSearch("тест"), false,
            baseOffer().setTitle("Some offer").setShopSku("some"));
    }

    @Test
    public void testSearchBySupplyerIdShopSku() {
        validateCriteriaMatches(OfferCriterias.supplierIdShopSkuIn(
            ImmutableList.of(
                MboMappings.UpdateAvailabilityRequest.Locator.newBuilder()
                    .setShopSku("sku1")
                    .setSupplierId(1)
                    .build(),
                MboMappings.UpdateAvailabilityRequest.Locator.newBuilder()
                    .setShopSku("sku2")
                    .setSupplierId(2)
                    .build()
            )),
            true,
            baseOffer().setShopSku("sku1").setBusinessId(1),
            baseOffer().setShopSku("sku2").setBusinessId(2)
        );

        validateCriteriaMatches(
            OfferCriterias.supplierIdShopSkuIn(
                ImmutableList.of(
                    MboMappings.UpdateAvailabilityRequest.Locator.newBuilder()
                        .setShopSku("sku3")
                        .setSupplierId(1)
                        .build(),
                    MboMappings.UpdateAvailabilityRequest.Locator.newBuilder()
                        .setShopSku("sku2")
                        .setSupplierId(3)
                        .build()
                )
            ),
            false,
            baseOffer().setShopSku("sku1").setBusinessId(1),
            baseOffer().setShopSku("sku2").setBusinessId(2)
        );

        validateCriteriaMatches(
            OfferCriterias.supplierIdShopSkuIn(Collections.emptyList()),
            false,
            baseOffer().setShopSku("sku1").setBusinessId(1),
            baseOffer().setShopSku("sku2").setBusinessId(2)
        );
    }

    @Test
    public void testCategories() {
        validateCriteriaMatches(OfferCriterias.categoryIdInCollection(Arrays.asList(42L, 43L)), true,
            baseOffer().setCategoryIdForTests(42L, Offer.BindingKind.SUGGESTED),
            baseOffer().setCategoryIdForTests(43L, Offer.BindingKind.SUGGESTED));

        validateCriteriaMatches(OfferCriterias.externalTextSearch("test"), false,
            baseOffer(),
            baseOffer().setCategoryIdForTests(54L, Offer.BindingKind.SUGGESTED));
    }

    @Test
    public void testMappingDestination() {
        validateCriteriaMatches(
            OfferCriterias.mappingDestination(Collections.singleton(Offer.MappingDestination.BLUE)), true,
            baseOffer().setMappingDestination(Offer.MappingDestination.BLUE));

        validateCriteriaMatches(
            OfferCriterias.mappingDestination(Collections.singleton(Offer.MappingDestination.BLUE)), false,
            baseOffer().setMappingDestination(Offer.MappingDestination.WHITE));

        validateCriteriaMatches(
            OfferCriterias.mappingDestination(Collections.singleton(Offer.MappingDestination.BLUE)), false,
            baseOffer().setMappingDestination(Offer.MappingDestination.FMCG));
    }

    @Test
    public void testUploadToYtStampCriteria() {
        validateCriteriaMatches(
            StampCriteria.of(1L), true,
            offers -> {
                OfferTestUtils.hardSetYtStamp(namedParameterJdbcTemplate, offers[0].getId(), 1L);
                OfferTestUtils.hardSetYtStamp(namedParameterJdbcTemplate, offers[1].getId(), 2L);
            },
            baseOffer().setMappingDestination(Offer.MappingDestination.WHITE),
            baseOffer().setMappingDestination(Offer.MappingDestination.WHITE)
        );

        validateCriteriaMatches(
            StampCriteria.of(2L), false,
            offers -> {
                OfferTestUtils.hardSetYtStamp(namedParameterJdbcTemplate, offers[1].getId(), 1L);
            },
            baseOffer().setUploadToYtStamp(null),
            baseOffer()
                .setMappingDestination(Offer.MappingDestination.WHITE)
        );
    }

    @Test
    public void testStatus() {
        validateCriteriaMatches(
            OfferCriterias.supplierMappingStatus(Collections.singleton(Offer.MappingStatus.ACCEPTED)), true,
            baseOffer().setSupplierSkuMappingStatus(Offer.MappingStatus.ACCEPTED));

        validateCriteriaMatches(
            OfferCriterias.supplierMappingStatus(Collections.singleton(Offer.MappingStatus.ACCEPTED)), false,
            baseOffer(),
            baseOffer().setSupplierSkuMappingStatus(Offer.MappingStatus.REJECTED));
    }

    @Test
    public void testWithFmcgSuggestMappingBarcodeSkutchCriteria() {
        validateCriteriaMatches(
            new FmcgSuggestMappingBarcodeSkutchCriteria(),
            false,
            baseOffer()
        );

        validateCriteriaMatches(
            new FmcgSuggestMappingBarcodeSkutchCriteria(),
            false,
            baseFmcgOffer()
        );

        validateCriteriaMatches(
            new FmcgSuggestMappingBarcodeSkutchCriteria(),
            false,
            baseFmcgOffer()
                .setSuggestSkuMapping(new Offer.Mapping(1L, DateTimeUtils.dateTimeNow()))
                .setSuggestSkuMappingType(SkuBDApi.SkutchType.SKUTCH_BY_PARAMETERS)
        );

        validateCriteriaMatches(
            new FmcgSuggestMappingBarcodeSkutchCriteria(),
            true,
            baseFmcgOffer()
                .setSuggestSkuMapping(new Offer.Mapping(2L, DateTimeUtils.dateTimeNow()))
                .setSuggestSkuMappingType(SkuBDApi.SkutchType.BARCODE_SKUTCH)
        );
    }

    @Test
    public void testHasMapping() {
        testHasMappingForType(Offer.MappingType.SUGGEST);
        testHasMappingForType(Offer.MappingType.APPROVED);
        testHasMappingForType(Offer.MappingType.SUPPLIER);
        testHasMappingForType(Offer.MappingType.CONTENT);
    }

    private void testHasMappingForType(Offer.MappingType mappingType) {
        validateCriteriaMatches(OfferCriterias.hasMapping(mappingType, true), true,
            mappingType.set(
                baseOffer().setCategoryIdForTests(99L, Offer.BindingKind.APPROVED)
                    .setApprovedSkuMappingConfidence(CONTENT),
                OfferTestUtils.mapping(123)
            ));
        validateCriteriaMatches(OfferCriterias.hasMapping(mappingType, true), false,
            baseOffer(),
            mappingType.set(baseOffer().setCategoryIdForTests(99L, Offer.BindingKind.APPROVED), OfferTestUtils.mapping(0)));

        validateCriteriaMatches(OfferCriterias.hasMapping(mappingType, false), false,
            mappingType.set(
                baseOffer()
                    .setCategoryIdForTests(99L, Offer.BindingKind.APPROVED)
                    .setApprovedSkuMappingConfidence(CONTENT), OfferTestUtils.mapping(123)));
        validateCriteriaMatches(OfferCriterias.hasMapping(mappingType, false), true,
            baseOffer(),
            mappingType.set(baseOffer().setCategoryIdForTests(99L, Offer.BindingKind.APPROVED), OfferTestUtils.mapping(0)));
    }

    private Offer baseOffer() {
        return new Offer()
            .setShopSku("Sku" + skuNum++)
            .setMappingDestination(Offer.MappingDestination.BLUE)
            .setTitle("Title")
            .setShopCategoryName("Category")
            .storeOfferContent(OfferContent.initEmptyContent())
            .setBusinessId(42)
            .addNewServiceOfferIfNotExistsForTests(supplierRepository.findById(42));
    }

    private Offer baseWhiteOffer() {
        return new Offer()
            .setShopSku("Sku" + skuNum++)
            .setMappingDestination(Offer.MappingDestination.WHITE)
            .setTitle("Title White")
            .setShopCategoryName("Category")
            .storeOfferContent(OfferContent.initEmptyContent())
            .setBusinessId(202)
            .addNewServiceOfferIfNotExistsForTests(supplierRepository.findById(202));
    }

    private Offer baseFmcgOffer() {
        return new Offer()
            .setShopSku("Sku" + skuNum++)
            .setMappingDestination(Offer.MappingDestination.FMCG)
            .setTitle("Title")
            .setShopCategoryName("Category")
            .storeOfferContent(OfferContent.initEmptyContent())
            .setBusinessId(80)
            .addNewServiceOfferIfNotExistsForTests(supplierRepository.findById(80));
    }

    private void runThenRollback(Runnable action) {
        transactionHelper.doInTransactionVoid(status -> {
            Object savepoint = status.createSavepoint();
            action.run();
            status.rollbackToSavepoint(savepoint);
        });
    }

    private void validateCriteriaMatches(Criteria<Offer> criteria, boolean matches, Offer... offers) {
        validateCriteriaMatches(criteria, matches, o -> {
        }, offers);
    }

    private void validateCriteriaMatches(Criteria<Offer> criteria, boolean matches, Consumer<Offer[]> postInsert, Offer... offers) {
        runThenRollback(() -> {
            repository.insertOffers(Arrays.asList(offers));
            postInsert.accept(offers);

            List<Offer> allOffers = repository.findAll();
            Set<Long> selected = repository.findOffers(new OffersFilter().addCriteria(criteria)).stream()
                .map(Offer::getId)
                .collect(Collectors.toSet());

            SoftAssertions.assertSoftly(softly -> {
                allOffers.forEach(offer -> {
                    if (matches != criteria.matches(offer)) {
                        if (matches) {
                            softly.fail(".matches() failed but should match " + offer);
                        } else {
                            softly.fail(".matches() accepted but shouldn't " + offer);
                        }
                    }

                    if (matches != selected.contains(offer.getId())) {
                        if (matches) {
                            softly.fail("select failed but should match " + offer);
                        } else {
                            softly.fail("select accepted but shouldn't " + offer);
                        }
                    }
                });
            });
        });
    }

    @Test
    public void testSupplierIdIn() {
        Criteria<Offer> supplierIdInCriteria = OfferCriterias
            .supplierIdInCriteria(Arrays.asList(42, 43));

        validateCriteriaMatches(supplierIdInCriteria, true,
            baseOffer());
        validateCriteriaMatches(supplierIdInCriteria, true,
            baseOffer().setBusinessId(43));
        validateCriteriaMatches(supplierIdInCriteria, false,
            baseOffer().setBusinessId(2));
    }

    @Test
    public void testProcessedOffers() {
        Criteria<Offer> processedOffersCriteria = OfferCriterias.supplierProcessedOffersCriteria();
        assertProcessedCriteria(processedOffersCriteria);
    }

    @Test
    public void testProcessedOffersWithTs() {
        LocalDateTime beforeNow = DateTimeUtils.dateTimeNow().minusHours(1);
        Criteria<Offer> processedOffersTsCriteria = OfferCriterias
            .supplierProcessedOffersWithTsCriteria(beforeNow);

        assertProcessedCriteria(processedOffersTsCriteria);

        validateCriteriaMatches(processedOffersTsCriteria, false, baseOffer()
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.TRASH)
            .setAcceptanceStatusModifiedInternal(DateTimeUtils.dateTimeNow().minusDays(1))
            .updateProcessingStatusIfValid(Offer.ProcessingStatus.PROCESSED)
            .setProcessingStatusModifiedInternal(DateTimeUtils.dateTimeNow().minusDays(1))
            .setSupplierSkuMappingStatus(Offer.MappingStatus.ACCEPTED));

        validateCriteriaMatches(processedOffersTsCriteria, false, baseOffer()
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.TRASH)
            .setAcceptanceStatusModifiedInternal(DateTimeUtils.dateTimeNow().minusDays(1))
            .updateProcessingStatusIfValid(Offer.ProcessingStatus.IN_PROCESS)
            .setSupplierSkuMappingStatus(Offer.MappingStatus.NONE));

        validateCriteriaMatches(processedOffersTsCriteria, false, baseOffer()
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
            .updateProcessingStatusIfValid(Offer.ProcessingStatus.PROCESSED)
            .setProcessingStatusModifiedInternal(DateTimeUtils.dateTimeNow().minusDays(1))
            .setSupplierSkuMappingStatus(Offer.MappingStatus.NONE));
    }

    @Test
    public void testWithDeadlineCriteria() {
        Criteria<Offer> deadlineCriteria = new HasDeadlineOrOldProcessingStatusCriteria();

        validateCriteriaMatches(deadlineCriteria, false, baseOffer()
            .setTicketDeadline(null)
            .setProcessingStatusModifiedInternal(DateTimeUtils.dateTimeNow()));

        validateCriteriaMatches(deadlineCriteria, true, baseOffer()
            .setTicketDeadline(LocalDate.now())
            .setProcessingStatusModifiedInternal(DateTimeUtils.dateTimeNow()));
        validateCriteriaMatches(deadlineCriteria, true, baseOffer()
            .setTicketDeadline(null)
            .setProcessingStatusModifiedInternal(DateTimeUtils.dateTimeNow()
                .minusHours(HasDeadlineOrOldProcessingStatusCriteria.OLD_HOURS + 1)));
        validateCriteriaMatches(deadlineCriteria, true, baseOffer()
            .setTicketDeadline(LocalDate.now())
            .setProcessingStatusModifiedInternal(DateTimeUtils.dateTimeNow()
                .minusHours(HasDeadlineOrOldProcessingStatusCriteria.OLD_HOURS + 1)));
    }

    @Test
    public void testNewOffers() {
        LocalDateTime beforeNow = DateTimeUtils.dateTimeNow().minusHours(1);
        Criteria<Offer> supplierIdInCriteria = OfferCriterias
            .newSupplierOffersCriteria(beforeNow);

        validateCriteriaMatches(supplierIdInCriteria, true, baseOffer()
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.NEW)
            .setCreated(DateTimeUtils.dateTimeNow())
            .setCreatedByLogin(PROTO_API_USER));

        validateCriteriaMatches(supplierIdInCriteria, true, baseOffer()
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.NEW)
            .setCreated(DateTimeUtils.dateTimeNow())
            .setCreatedByLogin(MBO_MAPPINGS_SERVICE_DEFAULT_USER));

        validateCriteriaMatches(supplierIdInCriteria, false, baseOffer()
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.NEW)
            .setCreated(DateTimeUtils.dateTimeNow())
            .setCreatedByLogin("test-user"));

        validateCriteriaMatches(supplierIdInCriteria, false, baseOffer()
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.TRASH)
            .setCreated(DateTimeUtils.dateTimeNow())
            .setCreatedByLogin(MBO_MAPPINGS_SERVICE_DEFAULT_USER));

        validateCriteriaMatches(supplierIdInCriteria, false, baseOffer()
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.NEW)
            .setCreated(DateTimeUtils.dateTimeNow().minusDays(1))
            .setCreatedByLogin(MBO_MAPPINGS_SERVICE_DEFAULT_USER));
    }

    @Test
    public void nullManualVendorShouldNotFailAndConsideredAsFalse() {
        Offer offer = OfferTestUtils.simpleOffer()
            .setManualVendor(false);

        repository.insertOffers(offer);
        offer = repository.findAll().get(0);
        offer.setManualVendor(true);

        repository.updateOffer(offer);
        List<Offer> offers = repository.findAll();
        assertThat(offers.get(0).isManualVendor()).isTrue();
    }

    private void assertProcessedCriteria(Criteria<Offer> offerCriteria) {
        validateCriteriaMatches(offerCriteria, true, baseOffer()
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
            .updateProcessingStatusIfValid(Offer.ProcessingStatus.PROCESSED)
            .setSupplierSkuMappingStatus(Offer.MappingStatus.ACCEPTED));

        validateCriteriaMatches(offerCriteria, true, baseOffer()
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
            .updateProcessingStatusIfValid(Offer.ProcessingStatus.NEED_INFO)
            .setSupplierSkuMappingStatus(Offer.MappingStatus.NONE));

        validateCriteriaMatches(offerCriteria, true, baseOffer()
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
            .updateProcessingStatusIfValid(Offer.ProcessingStatus.NO_KNOWLEDGE)
            .setSupplierSkuMappingStatus(Offer.MappingStatus.REJECTED));

        validateCriteriaMatches(offerCriteria, true, baseOffer()
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
            .updateProcessingStatusIfValid(Offer.ProcessingStatus.NO_CATEGORY)
            .setSupplierSkuMappingStatus(Offer.MappingStatus.REJECTED));

        validateCriteriaMatches(offerCriteria, true, baseOffer()
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.TRASH)
            .updateProcessingStatusIfValid(Offer.ProcessingStatus.OPEN)
            .setSupplierSkuMappingStatus(Offer.MappingStatus.NEW));

        validateCriteriaMatches(offerCriteria, false, baseOffer()
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.NEW)
            .updateProcessingStatusIfValid(Offer.ProcessingStatus.OPEN)
            .setSupplierSkuMappingStatus(Offer.MappingStatus.REJECTED));

        validateCriteriaMatches(offerCriteria, false, baseOffer()
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.NEW)
            .updateProcessingStatusIfValid(Offer.ProcessingStatus.PROCESSED)
            .setSupplierSkuMappingStatus(Offer.MappingStatus.RE_SORT));
    }

    @Test
    public void forNeedContentCriteriaTest() {
        Offer unclassifiedOffer = baseOffer().setCategoryIdForTests(1L, Offer.BindingKind.SUGGESTED)
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
            .updateProcessingStatusIfValid(Offer.ProcessingStatus.OPEN);
        Offer classifiedOffer = baseOffer().setCategoryIdForTests(1L, Offer.BindingKind.APPROVED)
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
            .updateProcessingStatusIfValid(Offer.ProcessingStatus.OPEN);
        Offer matchedOffer = baseOffer().setCategoryIdForTests(1L, Offer.BindingKind.SUGGESTED)
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
            .updateProcessingStatusIfValid(Offer.ProcessingStatus.OPEN)
            .setModelId(1L);
        // white offer, should go to NEED_CONTENT even if BindingKind is SUGGESTED
        Offer unclassifiedWhiteOffer = baseWhiteOffer().setCategoryIdForTests(1L, Offer.BindingKind.SUGGESTED)
            .updateProcessingStatusIfValid(Offer.ProcessingStatus.OPEN);

        repository.insertOffers(classifiedOffer, unclassifiedOffer, matchedOffer, unclassifiedWhiteOffer);

        List<Long> offersForNeedContentIdsOld =
            repository.findOffers(new OffersFilter().addCriteria(OfferCriterias.forNeedContent()))
                .stream().map(Offer::getId).collect(Collectors.toList());
        Assertions.assertThat(offersForNeedContentIdsOld)
            .containsExactlyInAnyOrder(
                unclassifiedOffer.getId(),
                classifiedOffer.getId(),
                matchedOffer.getId(),
                unclassifiedWhiteOffer.getId());
    }

    @Test
    public void offerDestinationTest() {
        Offer offer1 = baseOffer()
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK);
        Offer offer2 = baseWhiteOffer();
        repository.insertOffers(offer1, offer2);
        List<Long> offers =
            repository.findOffers(new OffersFilter().addCriteria(OfferCriterias.offerDestination(Set.of(
                Offer.MappingDestination.BLUE))))
                .stream().map(Offer::getId).collect(Collectors.toList());
        Assertions.assertThat(offers).containsExactly(offer1.getId());
        List<Long> offersWithWhite =
            repository.findOffers(new OffersFilter().addCriteria(OfferCriterias.offerDestination(Set.of(
                Offer.MappingDestination.BLUE, Offer.MappingDestination.WHITE))))
                .stream().map(Offer::getId).collect(Collectors.toList());
        Assertions.assertThat(offersWithWhite).containsExactlyInAnyOrder(offer1.getId(), offer2.getId());
    }

}
