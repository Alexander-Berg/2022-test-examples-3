package ru.yandex.market.mboc.common.offers.repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableList;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.mboc.common.dict.MbocSupplierType;
import ru.yandex.market.mboc.common.dict.SupplierRepository;
import ru.yandex.market.mboc.common.offers.model.ContentComment;
import ru.yandex.market.mboc.common.offers.model.ContentCommentType;
import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.offers.model.OfferContent;
import ru.yandex.market.mboc.common.offers.repository.search.LinkedToBusinessSupplierCriteria;
import ru.yandex.market.mboc.common.test.YamlTestUtil;
import ru.yandex.market.mboc.common.utils.BaseDbTestClass;
import ru.yandex.market.mboc.common.utils.DateTimeUtils;
import ru.yandex.market.mboc.common.utils.OfferTestUtils;
import ru.yandex.misc.thread.ThreadUtils;

/**
 * @author yuramalinov
 * @created 13.04.18
 */
public abstract class OfferRepositoryTest extends BaseDbTestClass {

    private OfferRepository repository;
    private SupplierRepository supplierRepository;

    @Before
    public void setUp() throws Exception {
        repository = getRepository();
        supplierRepository = getSupplierRepository();
        supplierRepository.insertBatch(YamlTestUtil.readSuppliersFromResource("suppliers/test-suppliers.yml"));
    }

    @Test
    public void testFullOffer() {
        Offer sampleOffer = YamlTestUtil.readFromResources("offers/test-offer.yml", Offer.class);

        Assertions.assertThat(repository.insertOffer(sampleOffer)).isTrue();
        Assertions.assertThat(sampleOffer.getId()).isPositive();

        Offer offer = repository.getOfferById(sampleOffer.getId());

        Assertions.assertThat(offer.getLastVersion()).isGreaterThan(sampleOffer.getLastVersion());
        Assertions.assertThat(offer.getUpdated()).isAfter(sampleOffer.getUpdated());
        sampleOffer.setLastVersion(offer.getLastVersion()); // For below assertion not to check it
        sampleOffer.setUpdated(offer.getUpdated());
        sampleOffer.markLoadedContent();
        sampleOffer.storeOfferContent(offer.getOfferContentBuilder().id(offer.getId()).build());

        Assertions.assertThat(offer).usingRecursiveComparison().isEqualTo(sampleOffer);
    }

    @Test
    public void testMinimalOffer() {
        Offer sample = YamlTestUtil.readFromResources("offers/minimal-offer.yml", Offer.class);

        Assertions.assertThat(repository.insertOffer(sample)).isTrue();
        Assertions.assertThat(sample.getId()).isPositive();

        Offer offer = repository.getOfferById(sample.getId());
        Assertions.assertThat(offer.getLastVersion()).isGreaterThan(sample.getLastVersion());
        Assertions.assertThat(offer.getCreated()).isNotNull();
        Assertions.assertThat(offer.getUpdated()).isNotNull();

        sample.setUpdated(offer.getUpdated());
        sample.setCreated(offer.getCreated());
        sample.setLastVersion(offer.getLastVersion());
        sample.markLoadedContent();
        sample.storeOfferContent(offer.getOfferContentBuilder().id(offer.getId()).build());

        Assertions.assertThat(offer).usingRecursiveComparison().isEqualTo(sample);
    }

    @Test
    public void testMinimalOfferWithServiceOffer() {
        Offer sample = YamlTestUtil.readFromResources("offers/minimal-offer.yml", Offer.class);
        sample.setServiceOffers(List.of(
            new Offer.ServiceOffer(99, MbocSupplierType.THIRD_PARTY, Offer.AcceptanceStatus.OK)));
        Assertions.assertThat(repository.insertOffer(sample)).isTrue();
        Assertions.assertThat(sample.getId()).isPositive();

        Offer offer = repository.getOfferById(sample.getId());
        Assertions.assertThat(offer.getLastVersion()).isGreaterThan(sample.getLastVersion());
        Assertions.assertThat(offer.getCreated()).isNotNull();
        Assertions.assertThat(offer.getUpdated()).isNotNull();

        sample.setUpdated(offer.getUpdated());
        sample.setCreated(offer.getCreated());
        sample.setLastVersion(offer.getLastVersion());
        sample.markLoadedContent();
        sample.storeOfferContent(offer.getOfferContentBuilder().id(offer.getId()).build());

        Assertions.assertThat(offer).usingRecursiveComparison().isEqualTo(sample);
    }

    @Test
    public void testUpdateOffer() {
        Offer sample = YamlTestUtil.readFromResources("offers/minimal-offer.yml", Offer.class);

        Assertions.assertThat(repository.insertOffer(sample)).isTrue();
        Assertions.assertThat(sample.getId()).isPositive();

        Offer insertedState = repository.getOfferById(sample.getId());
        Offer offer = repository.getOfferById(sample.getId());
        Offer updateValues = YamlTestUtil.readFromResources("offers/test-offer.yml", Offer.class);

        // Копируем все свойства из одного в другой
        long id = offer.getId();
        long lastVersion = offer.getLastVersion();
        offer = new Offer(updateValues)
            .setId(id)
            .setLastVersion(lastVersion);

        Assertions.assertThat(repository.updateOffer(offer)).isTrue();
        // Refresh offer:
        offer = repository.getOfferById(offer.getId());

        Assertions.assertThat(offer.getLastVersion()).isGreaterThan(insertedState.getLastVersion());
        Assertions.assertThat(offer.getUpdated()).isAfter(insertedState.getUpdated());

        // Проставляем поля, которые не надо сравнивать
        updateValues.setId(offer.getId());
        updateValues.setUpdated(offer.getUpdated());
        updateValues.setCreated(offer.getCreated());
        updateValues.setLastVersion(offer.getLastVersion());
        updateValues.setProcessingStatusModifiedInternal(offer.getProcessingStatusModified());
        updateValues.setAcceptanceStatusModifiedInternal(offer.getAcceptanceStatusModified());
        updateValues.markLoadedContent();
        updateValues.storeOfferContent(offer.getOfferContentBuilder().id(offer.getId()).build());
        updateValues.setUploadToYtStamp(offer.getUploadToYtStamp());

        Assertions.assertThat(offer).isEqualToIgnoringGivenFields(updateValues);
    }

    @Test(expected = ConcurrentModificationException.class)
    public void testOptimisticLocking() {
        Offer offer = YamlTestUtil.readFromResources("offers/minimal-offer.yml", Offer.class);

        Assertions.assertThat(repository.insertOffer(offer)).isTrue();
        Assertions.assertThat(offer.getId()).isPositive();

        offer = repository.getOfferById(offer.getId());

        offer.setLastVersion(offer.getLastVersion() + 1);
        offer.setTitle("new title"); // At least one change
        repository.updateOffer(offer);
    }

    @Test
    public void testChangeTicketAndStatus() {
        Offer offer1 = OfferTestUtils.simpleOffer();
        Offer offer2 = OfferTestUtils.simpleOffer().setShopSku("sku2");
        repository.insertOffer(offer1);
        repository.insertOffer(offer2);

        // Minus seconds because PG uses millisecond resolution, while Java - microsecond, and there is a chance
        // to get something like
        // Expecting: <2019-08-27T15:21:15.868> to be after or equals to: <2019-08-27T15:21:15.868011>
        LocalDateTime now = DateTimeUtils.dateTimeNow().minusSeconds(1);

        repository.updateTrackerTicket("SOME-1234", Offer.AdditionalTicketType.RECLASSIFICATION,
            1,
            Collections.singletonList(offer1.getId()));

        Offer updatedOffer1 = repository.getOfferById(offer1.getId());
        Offer updatedOffer2 = repository.getOfferById(offer2.getId());

        Assertions.assertThat(updatedOffer1.getTrackerTicket()).isEqualTo("SOME-1234");
        Assertions.assertThat(updatedOffer1.getProcessingStatusModified()).isAfterOrEqualTo(now);
        Assertions.assertThat(updatedOffer1.getAdditionalTickets())
            .contains(Map.entry(Offer.AdditionalTicketType.RECLASSIFICATION, "SOME-1234"));
        Assertions.assertThat(updatedOffer1.getProcessingTicketId()).isEqualTo(1);

        Assertions.assertThat(updatedOffer2.getTrackerTicket()).isNull();
        Assertions.assertThat(updatedOffer2.getAdditionalTickets()).isEmpty();
        Assertions.assertThat(updatedOffer2.getProcessingTicketId()).isNull();
    }

    @Test
    public void testOverwriteAdditionalTicket() {
        Offer offer = OfferTestUtils.simpleOffer()
            .addAdditionalTicket(Offer.AdditionalTicketType.ADD_SIZE_MEASURE, "SOME-1234");
        repository.insertOffer(offer);


        Offer inserted = repository.getOfferById(offer.getId());
        Assertions.assertThat(inserted.getAdditionalTickets())
            .containsExactly(Map.entry(Offer.AdditionalTicketType.ADD_SIZE_MEASURE, "SOME-1234"));

        repository.updateTrackerTicket("SOME-4321",
            Offer.AdditionalTicketType.ADD_SIZE_MEASURE,
            1,
            Collections.singletonList(offer.getId()));

        Offer updated = repository.getOfferById(offer.getId());
        Assertions.assertThat(updated.getAdditionalTickets())
            .containsExactly(Map.entry(Offer.AdditionalTicketType.ADD_SIZE_MEASURE, "SOME-4321"));
    }

    @Test
    public void testDontAffectAdditionalTicketsOnNull() {
        Offer offer = OfferTestUtils.simpleOffer()
            .addAdditionalTicket(Offer.AdditionalTicketType.ADD_SIZE_MEASURE, "SOME-1234");
        repository.insertOffer(offer);


        Offer inserted = repository.getOfferById(offer.getId());
        Assertions.assertThat(inserted.getAdditionalTickets())
            .containsExactly(Map.entry(Offer.AdditionalTicketType.ADD_SIZE_MEASURE, "SOME-1234"));

        repository.updateTrackerTicket("SOME-4321",
            null,
            null,
            Collections.singletonList(offer.getId()));

        Offer updated = repository.getOfferById(offer.getId());
        Assertions.assertThat(updated.getAdditionalTickets())
            .containsExactly(Map.entry(Offer.AdditionalTicketType.ADD_SIZE_MEASURE, "SOME-1234"));
    }

    @Test
    @SuppressWarnings("checkstyle:MagicNumber")
    public void testProcessingStatusModifiedIsNotUpdatedInCaseStatusNotChanged() {
        Offer offer = OfferTestUtils.simpleOffer();
        repository.insertOffer(offer);

        LocalDateTime insertedTs = offer.getProcessingStatusModified();

        repository.updateTrackerTicket("SOME-1234", Offer.AdditionalTicketType.WAIT_CONTENT,
            null,
            Collections.singletonList(offer.getId()));

        Offer updatedOffer = repository.getOfferById(offer.getId());
        Assertions.assertThat(updatedOffer.getAdditionalTickets())
            .contains(Map.entry(Offer.AdditionalTicketType.WAIT_CONTENT, "SOME-1234"));

        LocalDateTime updatedTs = updatedOffer.getProcessingStatusModified();
        ThreadUtils.sleep(100);

        repository.updateTrackerTicket("SOME-4321", Offer.AdditionalTicketType.RECLASSIFICATION,
            1,
            Collections.singletonList(offer.getId()));

        updatedOffer = repository.getOfferById(offer.getId());
        Assertions.assertThat(updatedOffer.getProcessingStatusModified()).isAfterOrEqualTo(insertedTs);
        Assertions.assertThat(updatedOffer.getProcessingStatusModified()).isEqualTo(updatedTs);
        Assertions.assertThat(updatedOffer.getAdditionalTickets())
            .contains(Map.entry(Offer.AdditionalTicketType.WAIT_CONTENT, "SOME-1234"))
            .contains(Map.entry(Offer.AdditionalTicketType.RECLASSIFICATION, "SOME-4321"));
        Assertions.assertThat(updatedOffer.getProcessingTicketId()).isEqualTo(1);
    }

    @Test
    public void testNeedInfoDetails() {
        List<ContentComment> details = ImmutableList.of(
            new ContentComment(ContentCommentType.DEPARTMENT_FROZEN),
            new ContentComment(ContentCommentType.CONFLICTING_INFORMATION, "test", "test2"));

        Offer offer = OfferTestUtils.simpleOffer().setContentComments(details);
        repository.insertOffer(offer);

        Offer stored = repository.getOfferById(offer.getId());
        Assertions.assertThat(stored.getContentComments()).isEqualTo(details);
    }

    @Test
    public void testFindByServiceOfferSupplier() {
        Offer offer = Offer.builder()
            .title("1")
            .mappingDestination(Offer.MappingDestination.BLUE)
            .shopCategoryName("c")
            .offerContent(OfferContent.initEmptyContent())
            .shopSku("shopsku")
            .businessId(42)
            .serviceOffers(List.of(new Offer.ServiceOffer(
                42, MbocSupplierType.THIRD_PARTY, Offer.AcceptanceStatus.OK)))
            .build();
        repository.insertOffer(offer);
        LinkedToBusinessSupplierCriteria criteria = new LinkedToBusinessSupplierCriteria(List.of(42));
        Collection<Offer> result = repository.findOffers(criteria);
        Assertions.assertThat(result)
            .extracting(Offer::getId)
            .containsExactly(offer.getId());
    }

    @Test
    public void testCountServiceOfferParts() {
        Offer offer1 = OfferTestUtils.simpleOffer().setBusinessId(1);
        Offer offer2 = OfferTestUtils.simpleOffer()
            .setBusinessId(2)
            .setServiceOffers(List.of(
                new Offer.ServiceOffer(42, MbocSupplierType.THIRD_PARTY, Offer.AcceptanceStatus.OK),
                new Offer.ServiceOffer(43, MbocSupplierType.DSBS, Offer.AcceptanceStatus.OK)
            ));
        Offer offer3 = OfferTestUtils.simpleOffer().setBusinessId(3);

        repository.insertOffers(offer1, offer2, offer3);
        var offerIds = repository.findAll().stream()
            .filter(x -> x.getBusinessId() != 3)
            .collect(Collectors.toMap(Offer::getBusinessId, Offer::getId));

        var servicePartsCount = repository.countServicePartsForOfferIds(offerIds.values());

        Assertions.assertThat(servicePartsCount.size()).isEqualTo(2);
        Assertions.assertThat(servicePartsCount.get(offerIds.get(1))).isEqualTo(1);
        Assertions.assertThat(servicePartsCount.get(offerIds.get(2))).isEqualTo(2);
    }

    protected abstract OfferRepository getRepository();

    protected abstract SupplierRepository getSupplierRepository();
}

