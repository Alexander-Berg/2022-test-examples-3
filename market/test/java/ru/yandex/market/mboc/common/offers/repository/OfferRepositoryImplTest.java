package ru.yandex.market.mboc.common.offers.repository;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import ru.yandex.market.mbo.lightmapper.exceptions.SqlConcurrentModificationException;
import ru.yandex.market.mboc.common.availability.msku.MskuRepository;
import ru.yandex.market.mboc.common.db.jooq.generated.mbo_category.tables.pojos.Msku;
import ru.yandex.market.mboc.common.dict.SupplierRepository;
import ru.yandex.market.mboc.common.dict.SupplierRepositoryImpl;
import ru.yandex.market.mboc.common.msku.TestUtils;
import ru.yandex.market.mboc.common.offers.model.AntiMapping;
import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.offers.model.OfferContent;
import ru.yandex.market.mboc.common.offers.model.OfferForService;
import ru.yandex.market.mboc.common.offers.repository.search.OffersFilter;
import ru.yandex.market.mboc.common.repo.bindings.pojos.MskuCargoParameter;
import ru.yandex.market.mboc.common.repo.bindings.pojos.MskuParameters;
import ru.yandex.market.mboc.common.test.YamlTestUtil;
import ru.yandex.market.mboc.common.utils.DateTimeUtils;
import ru.yandex.market.mboc.common.utils.OfferTestUtils;

/**
 * @author s-ermakov
 */
public class OfferRepositoryImplTest extends OfferRepositoryTest {
    private static final int OFFERS_COUNT = 100;
    private static final int BATCH_SIZE = 5;
    private static final long TEST_CATEGORY_ID = 10;

    @Autowired
    private SupplierRepositoryImpl supplierRepository;

    @Autowired
    private OfferRepositoryImpl repository;

    @Autowired
    private MskuRepository mskuRepository;

    @Autowired
    private OfferContentRepository offerContentRepository;

    @Autowired
    private AntiMappingRepository antiMappingRepository;

    @Override
    protected OfferRepository getRepository() {
        return repository;
    }

    @Override
    protected SupplierRepository getSupplierRepository() {
        return supplierRepository;
    }

    @Test
    public void testOfferApprovedMappingIdYtStampConstraint() {
        var offerValid1 = repository.insertAndGetOffer(OfferTestUtils.simpleOffer()
            .setShopSku("white-offer")
            .setMappingDestination(Offer.MappingDestination.WHITE));
        OfferTestUtils.hardSetYtStamp(namedParameterJdbcTemplate, offerValid1.getId(), 1L);

        var offerValid2 = repository.insertAndGetOffer((OfferTestUtils.simpleOffer()
            .setShopSku("approved-offer")
            .setUploadToYtStamp(1L)
            .setCategoryIdForTests(99L, Offer.BindingKind.APPROVED)
            .updateApprovedSkuMapping(new Offer.Mapping(1L, DateTimeUtils.dateTimeNow()),
                Offer.MappingConfidence.CONTENT)));
        OfferTestUtils.hardSetYtStamp(namedParameterJdbcTemplate, offerValid2.getId(), 2L);

        var offerInvalid = repository.insertAndGetOffer(OfferTestUtils.simpleOffer()
            .setShopSku("incorrect-offer")
            .updateApprovedSkuMapping(null, null)
            .setMappingDestination(Offer.MappingDestination.BLUE));
        Assertions.assertThatThrownBy(() ->
                OfferTestUtils.hardSetYtStamp(namedParameterJdbcTemplate, offerInvalid.getId(), 3L))
            .isInstanceOf(DataIntegrityViolationException.class)
            .hasMessageContaining("approved_mapping_and_yt_stamp_comply");
    }

    @Test
    public void testIterateBatch() {
        List<Offer> offers = generateTestOffers(OFFERS_COUNT);
        repository.insertOffers(offers);

        List<Long> iteratedIds = new ArrayList<>();
        repository.iterateOffersIdsInBatch(new OffersFilter(), BATCH_SIZE, ids -> {
            Assertions.assertThat(ids.size()).isLessThanOrEqualTo(BATCH_SIZE);
            iteratedIds.addAll(ids);
        });

        Assertions.assertThat(iteratedIds).containsExactlyElementsOf(
            LongStream.range(1, OFFERS_COUNT + 1).boxed().collect(Collectors.toList())
        );
    }

    @Test
    public void saveTerriblyWrongDataShouldThrowException() {
        Assertions.assertThatThrownBy(
            () -> repository.insertOffer(OfferTestUtils.nextOffer().setShopSku("wrong shopsku"))
        ).isInstanceOf(IllegalArgumentException.class);

        Offer offer = OfferTestUtils.nextOffer();
        repository.insertOffer(offer);
        offer.setShopSku("Wrong shopsku");

        Assertions.assertThatThrownBy(() -> repository.updateOffer(offer))
            .isInstanceOf(IllegalArgumentException.class);

    }

    @Test
    public void testRemoveOffers() {
        Offer newOffer = OfferTestUtils.simpleOffer()
            .setShopSku("white-offer")
            .setUploadToYtStamp(1L)
            .setMappingDestination(Offer.MappingDestination.WHITE);
        Assertions.assertThat(
                repository.insertOffer(newOffer))
            .isTrue();

        newOffer = repository.findOfferByBusinessSkuKey(newOffer.getBusinessSkuKey());
        OfferContent offerContent = offerContentRepository.findById(newOffer.getId());
        Assertions.assertThat(offerContent).isNotNull();

        AntiMapping antiMapping = new AntiMapping()
            .setOfferId(newOffer.getId())
            .setNotModelId(OfferTestUtils.TEST_MODEL_ID)
            .setNotSkuId(OfferTestUtils.TEST_SKU_ID)
            .setSourceType(AntiMapping.SourceType.MODERATION_REJECT)
            .setCreatedTs(Instant.now().minus(3, ChronoUnit.DAYS))
            .setUpdatedTs(Instant.now().minus(2, ChronoUnit.DAYS))
            .setUpdatedUser("test user updated")
            .setDeletedUser("test user deleted")
            .setDeletedTs(Instant.now().minus(1, ChronoUnit.DAYS))
            .markNeedsUpload();
        antiMappingRepository.insert(antiMapping);

        Assertions.assertThat(
                repository.removeOffer(newOffer))
            .isTrue();

        Offer removedOffer = repository.findOfferByBusinessSkuKey(newOffer.getBusinessSkuKey());
        Assertions.assertThat(removedOffer).isNull();
        List<OfferContent> offerContentList = offerContentRepository.findByIds(List.of(newOffer.getId()));
        Assertions.assertThat(offerContentList).isEmpty();

        List<AntiMapping> antiMappingList = antiMappingRepository.findByIds(List.of(newOffer.getId()));
        Assertions.assertThat(antiMappingList).isEmpty();
    }

    @Test
    public void testRemoveStaleTimestamp() {
        Offer newOffer = OfferTestUtils.simpleOffer()
            .setShopSku("white-offer")
            .setUploadToYtStamp(1L)
            .setMappingDestination(Offer.MappingDestination.WHITE);
        Assertions.assertThat(
                repository.insertOffer(newOffer))
            .isTrue();

        Assertions.assertThatThrownBy(() -> repository.removeOffer(newOffer))
            .isInstanceOf(SqlConcurrentModificationException.class);

        Offer removedOffer = repository.findOfferByBusinessSkuKey(newOffer.getBusinessSkuKey());
        Assertions.assertThat(removedOffer).isNotNull();
    }

    @Test
    public void testFindCargoTypes() {
        long mskuId = 1L;
        Offer offer = OfferTestUtils.simpleOffer()
            .setCategoryIdForTests(TEST_CATEGORY_ID, Offer.BindingKind.APPROVED)
            .updateApprovedSkuMapping(new Offer.Mapping(mskuId, LocalDateTime.now(), Offer.SkuType.MARKET))
            .setApprovedSkuMappingConfidence(Offer.MappingConfidence.CONTENT);

        repository.insertOffers(offer);

        var filter = new OffersFilter().setJoinApprovedMappingSkuCargoType(true);

        List<OfferForService> offers = repository.findOffersForService(filter, false);
        Assertions.assertThat(offers).extracting(OfferForService::getBaseOffer).extracting(Offer::getApprovedSkuCargoType)
            .containsExactly(new Offer.SkuCargoType(null, null, null));

        Msku msku = TestUtils.newMsku(mskuId, TEST_CATEGORY_ID);
        mskuRepository.save(msku);

        offers = repository.findOffersForService(filter, false);
        Assertions.assertThat(offers).extracting(OfferForService::getBaseOffer).extracting(Offer::getApprovedSkuCargoType)
            .containsExactly(new Offer.SkuCargoType(null, null, null));

        var mskuParameters = new MskuParameters();
        MskuCargoParameter cargo985 = new MskuCargoParameter(MskuOfferJoin.CARGO_TYPE_985_PARAM_ID,
            "cargoType985", 123L);
        mskuParameters.setCargoParameters(Map.of(MskuOfferJoin.CARGO_TYPE_985_PARAM_ID, cargo985));
        msku = mskuRepository.getById(mskuId);
        msku.setMskuParameterValues(mskuParameters);
        mskuRepository.save(msku);

        offers = repository.findOffersForService(filter, false);
        Assertions.assertThat(offers).extracting(OfferForService::getBaseOffer).extracting(Offer::getApprovedSkuCargoType)
            .containsExactly(new Offer.SkuCargoType(true, null, null));
    }

    @Test
    public void testFilterCargoTypes() {
        long mskuId1 = 1L;
        long mskuId2 = 2L;
        Offer offer1 = OfferTestUtils.simpleOffer()
            .setShopSku("offer1")
            .setCategoryIdForTests(TEST_CATEGORY_ID, Offer.BindingKind.APPROVED)
            .updateApprovedSkuMapping(new Offer.Mapping(mskuId1, LocalDateTime.now(), Offer.SkuType.MARKET))
            .setApprovedSkuMappingConfidence(Offer.MappingConfidence.CONTENT);
        Offer offer2 = OfferTestUtils.simpleOffer()
            .setShopSku("offer2")
            .setCategoryIdForTests(TEST_CATEGORY_ID, Offer.BindingKind.APPROVED)
            .updateApprovedSkuMapping(new Offer.Mapping(mskuId2, LocalDateTime.now(), Offer.SkuType.MARKET))
            .setApprovedSkuMappingConfidence(Offer.MappingConfidence.CONTENT);

        repository.insertOffers(offer1, offer2);

        var filter = new OffersFilter().setJoinApprovedMappingSkuCargoType(true)
            .setApprovedSkuCargoTypeFilters(List.of(new Offer.SkuCargoType(true, false, false)));

        MskuCargoParameter cargo985 = new MskuCargoParameter(MskuOfferJoin.CARGO_TYPE_985_PARAM_ID,
            "cargoType985", 123L);
        MskuCargoParameter cargo980 = new MskuCargoParameter(MskuOfferJoin.CARGO_TYPE_980_PARAM_ID,
            "cargoType980", 123L);

        var mskuParameters1 = new MskuParameters();
        mskuParameters1.setCargoParameters(Map.of(MskuOfferJoin.CARGO_TYPE_985_PARAM_ID, cargo985));
        Msku msku1 = TestUtils.newMsku(mskuId1, TEST_CATEGORY_ID);
        msku1.setMskuParameterValues(mskuParameters1);
        mskuRepository.save(msku1);

        var mskuParameters2 = new MskuParameters();
        mskuParameters2.setCargoParameters(Map.of(MskuOfferJoin.CARGO_TYPE_980_PARAM_ID, cargo980));
        Msku msku2 = TestUtils.newMsku(mskuId2, TEST_CATEGORY_ID);
        msku2.setMskuParameterValues(mskuParameters2);
        mskuRepository.save(msku2);

        var offers = repository.findOffersForService(filter, false);
        Assertions.assertThat(offers).extracting(OfferForService::getBaseOffer).extracting(Offer::getApprovedSkuCargoType)
            .containsExactly(new Offer.SkuCargoType(true, null, null));
        Assertions.assertThat(offers).extracting(OfferForService::getBaseOffer).extracting(Offer::getShopSku)
            .containsExactly(offer1.getShopSku());
    }

    @Test
    public void testMultipleFilterCargoTypes() {
        long mskuId1 = 1L;
        long mskuId2 = 2L;
        long mskuId3 = 3L;
        Offer offer1 = OfferTestUtils.simpleOffer()
            .setShopSku("offer1")
            .setCategoryIdForTests(TEST_CATEGORY_ID, Offer.BindingKind.APPROVED)
            .updateApprovedSkuMapping(new Offer.Mapping(mskuId1, LocalDateTime.now(), Offer.SkuType.MARKET))
            .setApprovedSkuMappingConfidence(Offer.MappingConfidence.CONTENT);
        Offer offer2 = OfferTestUtils.simpleOffer()
            .setShopSku("offer2")
            .setCategoryIdForTests(TEST_CATEGORY_ID, Offer.BindingKind.APPROVED)
            .updateApprovedSkuMapping(new Offer.Mapping(mskuId2, LocalDateTime.now(), Offer.SkuType.MARKET))
            .setApprovedSkuMappingConfidence(Offer.MappingConfidence.CONTENT);
        Offer offer3 = OfferTestUtils.simpleOffer()
            .setShopSku("offer3")
            .setCategoryIdForTests(TEST_CATEGORY_ID, Offer.BindingKind.APPROVED)
            .updateApprovedSkuMapping(new Offer.Mapping(mskuId3, LocalDateTime.now(), Offer.SkuType.MARKET))
            .setApprovedSkuMappingConfidence(Offer.MappingConfidence.CONTENT);

        repository.insertOffers(offer1, offer2, offer3);

        var filter = new OffersFilter().setJoinApprovedMappingSkuCargoType(true)
            .setApprovedSkuCargoTypeFilters(List.of(
                new Offer.SkuCargoType(true, false, false),
                new Offer.SkuCargoType(false, false, true)
            ));

        MskuCargoParameter cargo985 = new MskuCargoParameter(MskuOfferJoin.CARGO_TYPE_985_PARAM_ID,
            "cargoType985", 123L);
        MskuCargoParameter cargo980 = new MskuCargoParameter(MskuOfferJoin.CARGO_TYPE_980_PARAM_ID,
            "cargoType980", 123L);
        MskuCargoParameter cargo990 = new MskuCargoParameter(MskuOfferJoin.CARGO_TYPE_990_PARAM_ID,
            "cargoType990", 123L);

        var mskuParameters1 = new MskuParameters();
        mskuParameters1.setCargoParameters(Map.of(MskuOfferJoin.CARGO_TYPE_985_PARAM_ID, cargo985));
        Msku msku1 = TestUtils.newMsku(mskuId1, TEST_CATEGORY_ID);
        msku1.setMskuParameterValues(mskuParameters1);
        mskuRepository.save(msku1);

        var mskuParameters2 = new MskuParameters();
        mskuParameters2.setCargoParameters(Map.of(MskuOfferJoin.CARGO_TYPE_980_PARAM_ID, cargo980));
        Msku msku2 = TestUtils.newMsku(mskuId2, TEST_CATEGORY_ID);
        msku2.setMskuParameterValues(mskuParameters2);
        mskuRepository.save(msku2);

        var mskuParameters3 = new MskuParameters();
        mskuParameters3.setCargoParameters(Map.of(MskuOfferJoin.CARGO_TYPE_990_PARAM_ID, cargo990));
        Msku msku3 = TestUtils.newMsku(mskuId3, TEST_CATEGORY_ID);
        msku3.setMskuParameterValues(mskuParameters3);
        mskuRepository.save(msku3);

        var offers = repository.findOffersForService(filter, false);
        Assertions.assertThat(offers).extracting(OfferForService::getBaseOffer).extracting(Offer::getShopSku)
            .containsExactlyInAnyOrder(offer1.getShopSku(), offer2.getShopSku());
    }

    @Test
    public void testParallelImportedOffers() {
        var offer = repository.insertAndGetOffer(OfferTestUtils.simpleOffer()
            .setShopSku("not-parallel-imported-implicitly")
        );
        Assertions.assertThat(offer.isParallelImported()).isEqualTo(false);
        offer = repository.insertAndGetOffer(OfferTestUtils.simpleOffer()
            .setShopSku("not-parallel-imported-explicitly")
            .setParallelImported(false)
        );
        Assertions.assertThat(offer.isParallelImported()).isEqualTo(false);
        offer = repository.insertAndGetOffer(OfferTestUtils.simpleOffer()
            .setShopSku("parallel-imported")
            .setParallelImported(true)
        );
        Assertions.assertThat(offer.isParallelImported()).isEqualTo(true);
    }

    @Test
    public void testIsResaleOffers() {
        var offer = repository.insertAndGetOffer(OfferTestUtils.simpleOffer()
            .setShopSku("not-is-resale-implicitly")
        );
        Assertions.assertThat(offer.isResale()).isEqualTo(false);

        offer = repository.insertAndGetOffer(OfferTestUtils.simpleOffer()
            .setShopSku("not-is-resale-explicitly")
            .setResale(false)
        );
        Assertions.assertThat(offer.isResale()).isEqualTo(false);

        offer = repository.insertAndGetOffer(OfferTestUtils.simpleOffer()
            .setShopSku("is-resale")
            .setResale(true)
        );
        Assertions.assertThat(offer.isResale()).isEqualTo(true);
    }

    private List<Offer> generateTestOffers(int count) {
        List<Offer> result = new ArrayList<>();
        Offer sampleOffer = YamlTestUtil.readFromResources("offers/test-offer.yml", Offer.class);
        for (int i = 1; i <= count; i++) {
            sampleOffer.setId(i);
            sampleOffer.setShopSku("ssku" + i);
            sampleOffer.setIsOfferContentPresent(true);
            result.add(sampleOffer.copy());
        }
        return result;
    }

}
