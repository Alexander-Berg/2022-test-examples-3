package ru.yandex.market.mboc.common.contentprocessing.to.service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Vector;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.mboc.common.MbocErrors;
import ru.yandex.market.mboc.common.assertions.MbocAssertions;
import ru.yandex.market.mboc.common.contentprocessing.to.model.BusinessOfferGroupId;
import ru.yandex.market.mboc.common.contentprocessing.to.model.ContentProcessingOffer;
import ru.yandex.market.mboc.common.contentprocessing.to.service.AbstractContentProcessingSenderService.FailureReason;
import ru.yandex.market.mboc.common.contentprocessing.to.service.AbstractContentProcessingSenderService.FailureType;
import ru.yandex.market.mboc.common.contentprocessing.to.service.AbstractContentProcessingSenderService.SenderTaskResult;
import ru.yandex.market.mboc.common.db.jooq.generated.mbo_category.enums.MigrationModelChangeSource;
import ru.yandex.market.mboc.common.db.jooq.generated.mbo_category.enums.MigrationModelStatus;
import ru.yandex.market.mboc.common.db.jooq.generated.mbo_category.tables.pojos.MigrationModel;
import ru.yandex.market.mboc.common.db.jooq.generated.mbo_category.tables.pojos.Msku;
import ru.yandex.market.mboc.common.dict.SupplierService;
import ru.yandex.market.mboc.common.offers.ContextedOfferDestinationCalculator;
import ru.yandex.market.mboc.common.offers.model.BusinessSkuKey;
import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.utils.DateTimeUtils;
import ru.yandex.market.mboc.common.utils.ErrorInfo;
import ru.yandex.market.mboc.common.utils.OfferTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static ru.yandex.market.mboc.common.contentprocessing.to.service.AbstractContentProcessingSenderService.AG_BATCH_LIMIT_KEY;
import static ru.yandex.market.mboc.common.contentprocessing.to.service.AbstractContentProcessingSenderService.FailureType.MISSING_IN_DC;
import static ru.yandex.market.mboc.common.contentprocessing.to.service.GroupedContentProcessingSenderServiceImpl.BATCH_SIZE_KEY;
import static ru.yandex.market.mboc.common.contentprocessing.to.service.GroupedContentProcessingSenderServiceImpl.GROUP_SIZE_LIMIT_KEY;
import static ru.yandex.market.mboc.common.datacamp.service.converter.DataCampConverterService.CONFIG_ENABLE_MODEL_ALLOW_PARTNER_CONTENT_KEY;
import static ru.yandex.market.mboc.common.offers.model.Offer.ProcessingStatus.AUTO_PROCESSED;
import static ru.yandex.market.mboc.common.offers.model.Offer.ProcessingStatus.CONTENT_PROCESSING;
import static ru.yandex.market.mboc.common.offers.model.Offer.ProcessingStatus.INVALID;
import static ru.yandex.market.mboc.common.offers.model.Offer.ProcessingStatus.IN_RE_SORT;
import static ru.yandex.market.mboc.common.offers.model.Offer.ProcessingStatus.NEED_CONTENT;
import static ru.yandex.market.mboc.common.offers.model.Offer.ProcessingStatus.PROCESSED;

public class GroupedContentProcessingSenderServiceImplTest
    extends AbstractContentProcessingSenderServiceTest {
    @Before
    @Override
    public void setUp() {
        super.setUp();

        storageKeyValueService.putValue(BATCH_SIZE_KEY, 1);
        storageKeyValueService.invalidateCache();

        service = new AdaptedForTestingGroupedContentProcessingSenderServiceImpl(
            queue,
            agService,
            transactionHelper,
            offerRepository,
            mskuRepository,
            categoryService,
            new SupplierService(supplierRepository),
            offersProcessingStatusService,
            needContentStatusService,
            dataCampService,
            dataCampConverterService,
            datacampImportService,
            keyValueService,
            contentProcessingLog,
            new ContextedOfferDestinationCalculator(categoryInfoCache, keyValueService),
            hashCalculator,
            migrationModelRepository
        );
    }

    @Test
    public void testProcessesOnlyGroupedOffers() {
        var nonGrouped = offer(businessSupplier, "nonGrouped", CONTENT_PROCESSING, null, 1L);
        var grouped1 = offer(businessSupplier, "grouped1", CONTENT_PROCESSING, 11, 1L);
        var grouped2 = offer(businessSupplier, "grouped2", CONTENT_PROCESSING, 11, 1L);
        offerRepository.insertOffers(nonGrouped, grouped1, grouped2);
        makeReadyInQueue(nonGrouped, grouped1, grouped2);
        configureDatacampResponse(Map.of(grouped1, 1L, grouped2, 1L));
        configureAgResponses(0);

        assertThat(queue.findGroupsReady()).hasSize(1);
        queue.readAllWithoutGroupId(b -> assertThat(b).hasSize(1), 100, 100);

        service.sendFromQueue();

        assertThat(queue.findGroupsReady()).isEmpty();
        assertThat(queue.findAll()).hasSize(1).element(0)
            .matches(it -> it.getKey().equals(nonGrouped.getBusinessSkuKey()));

        verify(agServiceImpl, times(1)).startContentProcessing(any(), any());
    }

    @Test
    public void testDequeuedWhenMissingFromDatacamp() {
        var existingInDc = offer(businessSupplier, "existingInDc", CONTENT_PROCESSING, 11, 1L);
        var missingInDc = offer(businessSupplier, "missingInDc", CONTENT_PROCESSING, 11, 1L);
        offerRepository.insertOffers(existingInDc, missingInDc);
        makeReadyInQueue(existingInDc, missingInDc);
        configureDatacampResponse(Map.of(existingInDc, 1L));
        configureAgResponses(0);

        assertThat(queue.findGroupsReady())
            .containsExactly(new BusinessOfferGroupId(businessSupplier.getId(), 11));
        assertThat(queue.findAll()).hasSize(2);

        var result = service.executeTask(
            service.new SenderTaskContext(queue.findAll())
        );

        assertThat(queue.findGroupsReady()).isEmpty();
        assertThat(queue.findAll()).isEmpty();
        verify(agServiceImpl, times(1)).startContentProcessing(any(), any());
        assertThat(result.getOffersSent()).containsExactly(existingInDc.getBusinessSkuKey());
        assertThat(result.getOffersFailed())
            .containsOnlyKeys(missingInDc.getBusinessSkuKey())
            .extractingFromEntries(e -> e.getValue().getType())
            .containsExactlyInAnyOrder(MISSING_IN_DC);
    }

    @Test
    public void testFailsWholeGroupIfAnyOfferIsOutdated() {
        var groupId = new BusinessOfferGroupId(businessSupplier.getId(), 11);
        var upToDate = offer(businessSupplier, "upToDate", CONTENT_PROCESSING, groupId.getGroupId(), 1L);
        var upToDateNotInQueue = offer(businessSupplier, "upToDateNotInQueue", CONTENT_PROCESSING,
            groupId.getGroupId(), 1L);
        var outdatedNotInQueue1 = offer(businessSupplier, "outdatedNotInQueue1", CONTENT_PROCESSING,
            groupId.getGroupId(), 0L);
        var outdatedNotInQueue2 = offer(businessSupplier, "outdatedNotInQueue2", CONTENT_PROCESSING,
            groupId.getGroupId(), 0L);

        offerRepository.insertOffers(upToDate, upToDateNotInQueue, outdatedNotInQueue1, outdatedNotInQueue2);
        queue.deleteChangedBeforeByBusinessSkuKeys(List.of(
            upToDateNotInQueue.getBusinessSkuKey(),
            outdatedNotInQueue1.getBusinessSkuKey(),
            outdatedNotInQueue2.getBusinessSkuKey()
        ), Instant.now());
        makeReadyInQueue(upToDate);
        configureDatacampResponse(Map.of(
            upToDate, 1L,
            upToDateNotInQueue, 1L,
            outdatedNotInQueue1, 1L,
            outdatedNotInQueue2, 1L
        ));
        configureAgResponses(0);

        assertThat(queue.findGroupsReady()).containsExactly(groupId);
        assertThat(queue.findAll()).hasSize(1);

        SenderTaskResult result = service.executeTask(service.new SenderTaskContext(queue.findAllByGroupId(groupId)));

        assertThat(result.getOffersFailed())
            .containsOnlyKeys(
                upToDate.getBusinessSkuKey(),
                upToDateNotInQueue.getBusinessSkuKey(),
                outdatedNotInQueue1.getBusinessSkuKey(),
                outdatedNotInQueue2.getBusinessSkuKey()
            )
            .extractingByKeys(
                outdatedNotInQueue1.getBusinessSkuKey(),
                outdatedNotInQueue2.getBusinessSkuKey()
            )
            .extracting(FailureReason::getType)
            .allMatch(FailureType.OUTDATED_DC_VERSION::equals);
        assertThat(queue.findGroupsReady()).containsExactly(groupId);
        assertThat(queue.findAll()).hasSize(1);
        verify(agServiceImpl, times(0)).startContentProcessing(any(), any());
    }

    @Test
    public void testIgnoreOffersNotPresentInMboc() {
        // Group 11: two offers, second is not in queue but present in mboc
        var wholeGroup1 = offer(businessSupplier, "wholeGroup1", CONTENT_PROCESSING, 11, 1L);
        var wholeGroup2 = offer(businessSupplier, "wholeGroup2", CONTENT_PROCESSING, 11, 1L);
        // Group 22: two offers, second is not present in mboc
        var partialGroup1 = offer(businessSupplier, "partialGroup1", CONTENT_PROCESSING, 22, 1L);
        offerRepository.insertOffers(wholeGroup1, wholeGroup2, partialGroup1);
        queue.deleteChangedBeforeByBusinessSkuKeys(List.of(wholeGroup2.getBusinessSkuKey()), Instant.now());
        makeReadyInQueue(wholeGroup1, partialGroup1);
        configureDatacampResponse(Map.of(wholeGroup1, 1L, wholeGroup2, 1L, partialGroup1, 1L));
        configureDatacampResponse(new BusinessSkuKey(businessSupplier.getId(), "partialGroup2-MISSING"), 22, 1L);
        configureAgResponses(0);

        assertThat(queue.findGroupsReady()).hasSize(2);
        assertThat(queue.findAll()).hasSize(2);
        assertThat(queue.findAllByBusinessSkuKeys(wholeGroup2.getBusinessSkuKey())).isEmpty();

        service.sendFromQueue();

        assertThat(queue.findAll()).isEmpty();
        verify(agServiceImpl, times(2)).startContentProcessing(any(), any());
    }

    @Test
    public void testAgSendFailDoesNotRemovesFromQueue() {
        var first = offer(cskuSupplier, "first", CONTENT_PROCESSING, 11, 1L);
        var second = offer(businessSupplier, "second", CONTENT_PROCESSING, 22, 1L);
        offerRepository.insertOffers(first, second);
        makeReadyInQueue(first, second);
        configureDatacampResponse(Map.of(first, 1L, second, 1L));
        configureAgResponses(1);

        assertThat(queue.findGroupsReady()).hasSize(2);
        assertThat(queue.findAll()).hasSize(2);

        service.sendFromQueue();

        assertThat(queue.findGroupsReady()).hasSize(1);
        assertThat(queue.findAll()).hasSize(1);
        verify(agServiceImpl, times(2)).startContentProcessing(any(), any());
    }

    @Test
    public void testSendsPartialGroupWhenFailedCanBeExcluded() {
        var grouped1 = offer(businessSupplier, "grouped1", CONTENT_PROCESSING, 11, 1L);
        var grouped2 = offer(businessSupplier, "grouped2", NEED_CONTENT, 11, 1L);
        var grouped3 = offer(businessSupplier, "grouped3", CONTENT_PROCESSING, 11, 1L);
        offerRepository.insertOffers(grouped1, grouped2, grouped3);

        // Make invalid
        offerRepository.updateOffer(offerRepository.findOfferByBusinessSkuKey(grouped3.getBusinessSkuKey())
            .setProcessingStatusInternal(INVALID)
            .setContentProcessingStatusInternal(Offer.ContentProcessingStatus.NONE));

        makeReadyInQueue(grouped1, grouped2, grouped3);
        configureDatacampResponse(Map.of(grouped1, 1L, grouped2, 1L, grouped3, 1L));
        configureAgResponses(0);

        assertThat(queue.findGroupsReady()).hasSize(1);
        assertThat(queue.findAll()).hasSize(3);

        var result = service.executeTask(
            service.new SenderTaskContext(queue.findAll())
        );

        // partial group sent, failed offer excluded and dequeued
        assertThat(queue.findGroupsReady()).hasSize(0);
        assertThat(queue.findAll()).hasSize(0);
        verify(agServiceImpl, times(1)).startContentProcessing(any(), any());
        assertThat(result.getOffersFailed()).hasSize(1);
        assertThat(result.getOffersDequeued()).hasSize(3);
        assertThat(result.getOffersSent()).hasSize(2);

        List<Offer> offers = offerRepository.findOffersByBusinessSkuKeys(
            grouped1.getBusinessSkuKey(),
            grouped2.getBusinessSkuKey()
        );
        assertThat(offers)
            .extracting(Offer::getContentStatusActiveError)
            .allMatch(Objects::isNull, "ContentStatusActiveErrors are null");

        Offer failedOffer = offerRepository.findOfferByBusinessSkuKey(grouped3.getBusinessSkuKey());
        assertThat(failedOffer.getContentStatusActiveError()).isNull();
    }

    @Test
    public void testNotSendsIfFailedCantBeExcludedInGroup() {
        var grouped1 = offer(businessSupplier, "grouped1", CONTENT_PROCESSING, 11, 1L);
        var grouped2 = offer(businessSupplier, "grouped2", NEED_CONTENT, 11, 1L);
        var grouped3 = offer(businessSupplier, "grouped3", CONTENT_PROCESSING, 11, 1L);
        offerRepository.insertOffers(grouped1, grouped2, grouped3);

        makeReadyInQueue(grouped1, grouped2, grouped3);
        // configure wrong version for grouped3
        configureDatacampResponse(Map.of(grouped1, 1L, grouped2, 1L, grouped3, 2L));
        configureAgResponses(0);

        assertThat(queue.findGroupsReady()).hasSize(1);
        assertThat(queue.findAll()).hasSize(3);

        var result = service.executeTask(
            service.new SenderTaskContext(queue.findAll())
        );

        // all group marked failed, nothing is dequeued
        assertThat(queue.findGroupsReady()).hasSize(1);
        assertThat(queue.findAll()).hasSize(3);
        verify(agServiceImpl, times(0)).startContentProcessing(any(), any());
        assertThat(result.getOffersFailed()).hasSize(3);
        assertThat(result.getOffersDequeued()).hasSize(0);
        assertThat(result.getOffersSent()).hasSize(0);

        List<Offer> offers = offerRepository.findOffersByBusinessSkuKeys(
            grouped1.getBusinessSkuKey(),
            grouped2.getBusinessSkuKey(),
            grouped3.getBusinessSkuKey()
        );
        assertThat(offers)
            .extracting(Offer::getContentStatusActiveError)
            .allMatch(Objects::isNull, "ContentStatusActiveErrors are null");
    }

    @Test
    public void testSendAllGroupForTooOldOffers() {
        var grouped1 = offer(businessSupplier, "grouped1", NEED_CONTENT, 11, 2L)
            .setContentChangedTs(LocalDateTime.of(1970, 01, 01, 00, 00, 00))
            .setMarketSpecificContentHashSent(2L);
        var grouped2 = offer(businessSupplier, "grouped2", CONTENT_PROCESSING, 11, 1L);
        var grouped3 = offer(businessSupplier, "grouped3", CONTENT_PROCESSING, 11, 1L);
        offerRepository.insertOffers(grouped1, grouped2, grouped3);

        makeReadyInQueue(grouped1, grouped2, grouped3);
        configureDatacampResponse(Map.of(grouped1, 2L, grouped2, 1L, grouped3, 1L));
        configureAgResponses(0);

        // should set old hashSent after configureDatacampResponse
        var offer1 = offerRepository.findOfferByBusinessSkuKey(grouped1.getBusinessSkuKey());
        offer1.setMarketSpecificContentHashSent(2L);
        offerRepository.updateOffer(offer1);

        assertThat(queue.findGroupsReady()).hasSize(1);
        assertThat(queue.findAll()).hasSize(2); // first shouldn't be added by observer

        var result = service.executeTask(
            service.new SenderTaskContext(queue.findAll())
        );

        // all group successfully sent
        assertThat(queue.findGroupsReady()).isEmpty();
        assertThat(queue.findAll()).isEmpty();
        verify(agServiceImpl, times(1)).startContentProcessing(any(), any());
        assertThat(result.getOffersFailed()).hasSize(0);
        assertThat(result.getOffersDequeued()).hasSize(3);
        assertThat(result.getOffersSent()).hasSize(3);
    }

    @Test
    @Override
    public void testErrorWrongProcessingStatus() {
        var offer = offer(businessSupplier, "offer1", IN_RE_SORT, 111, 1L);
        offer.setMarketSpecificContentHashSent(1L);
        testErrorWrongProcessingStatus(offer, Config.builder().expectedQueueSize(0).build());
    }

    @Test
    public void testBarcodeError() {
        var existingOffer1 = offer(businessSupplier, "existingOffer1", NEED_CONTENT, 100, 1L)
            .setCategoryIdForTests(100L, Offer.BindingKind.APPROVED)
            .setBarCode(null)
            .addNewServiceOfferIfNotExistsForTests(fulfillmentSupplier)
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK);
        var existingOffer2 = offer(businessSupplier, "existingOffer2", NEED_CONTENT, 100, 1L)
            .setCategoryIdForTests(100L, Offer.BindingKind.APPROVED)
            .setBarCode(null)
            .addNewServiceOfferIfNotExistsForTests(businessSupplier)
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK);
        testBarcodeError(existingOffer1, existingOffer2, Config.builder().expectedQueueSize(0).build());
    }

    @Test
    @Override
    public void testFirstPicError() {
        var first = offer(businessSupplier, "first", NEED_CONTENT, 100, 1L);
        var second = offer(businessSupplier, "second", NEED_CONTENT, 100, 1L);
        testFirstPicError(first, second);
    }

    @Test
    @Override
    public void testNoPicturesNoError() {
        var offer = offer(businessSupplier, "offer", NEED_CONTENT, 100, 1L);
        testNoPicturesNoError(offer);
    }

    @Test
    @Override
    public void testDequeueIfNotAllPicturesDownloaded() {
        var offer = offer(businessSupplier, "offer", NEED_CONTENT, 11, 1L);
        testDequeueIfNotAllPicturesDownloaded(offer);
    }

    @Test
    @Override
    public void testDequeueWhenNotAllPicturesDownloadedEmptyActual() {
        var offer = offer(businessSupplier, "offer", NEED_CONTENT, 11, 1L);
        testDequeueWhenNotAllPicturesDownloadedEmptyActual(offer);
    }

    @Test
    public void testOfferGroupChangedInDatacamp() {
        var grouped1 = offer(businessSupplier, "grouped1", CONTENT_PROCESSING, 11, 1L);
        var grouped2 = offer(businessSupplier, "grouped2", NEED_CONTENT, 11, 1L);
        offerRepository.insertOffers(grouped1, grouped2);
        makeReadyInQueue(grouped1, grouped2);
        configureDatacampResponse(Map.of(grouped1, 1L, grouped2, 1L));
        configureDatacampResponse(grouped2.copy().setGroupId(22), 1L, __ -> {
        }, false);
        configureAgResponses(0);

        assertThat(queue.findGroupsReady()).hasSize(1);
        assertThat(queue.findAll()).hasSize(2);

        var result = service.executeTask(
            service.new SenderTaskContext(queue.findAll())
        );

        assertThat(queue.findGroupsReady()).hasSize(1)
            .extracting(BusinessOfferGroupId::getGroupId).containsExactlyInAnyOrder(22);
        assertThat(queue.findAll()).hasSize(1);
        verify(agServiceImpl, times(1)).startContentProcessing(any(), any());
        assertThat(result.getOffersFailed()).hasSize(1).containsOnlyKeys(grouped2.getBusinessSkuKey());
        assertThat(result.getOffersDequeued()).hasSize(1);
        assertThat(result.getOffersSent()).containsExactly(grouped1.getBusinessSkuKey());
    }

    @Test
    @Override
    public void testDontSendAndIgnoreWhenHasContentMapping() {
        var offerBad = offer(businessSupplier, "offerBad", PROCESSED, 111, 1L)
            .updateApprovedSkuMapping(OfferTestUtils.mapping(1L), Offer.MappingConfidence.CONTENT);
        var offerGood = offer(businessSupplier, "offerGood", CONTENT_PROCESSING, 111, 1L);
        testDontSendAndIgnoreWhenHasContentMapping(offerBad, offerGood);
    }

    @Test
    public void testDontSendWhenGroupIdInvalid() {
        var offerBadGroup = offer(businessSupplier, "offer", CONTENT_PROCESSING, -1, 1L);

        offerRepository.insertOffers(offerBadGroup);
        assertThat(queue.findAll()).hasSize(1);

        makeReadyInQueue(offerBadGroup);
        // no DC response is configured
        configureAgResponses(0);

        var result = service.executeTask(
            service.new SenderTaskContext(queue.findAll())
        );

        var expectedError = MbocErrors.get()
            .contentProcessingBadGroupId(offerBadGroup.getShopSku(), offerBadGroup.getGroupId());

        assertThat(queue.findAll()).hasSize(0);
        assertThat(result.getOffersFailed()).extractingByKey(offerBadGroup.getBusinessSkuKey())
            .matches(fr -> fr.getType() == FailureType.VALIDATION)
            .matches(fr -> fr.hasError())
            .extracting(fr -> fr.getError().getErrorCode())
            .isEqualTo(expectedError.getErrorCode());
        assertThat(result.getOffersSentCount()).isEqualTo(0);

        Offer offerWithErrorAfter = offerRepository.findOfferByBusinessSkuKey(offerBadGroup.getBusinessSkuKey());
        MbocAssertions.assertThat(offerWithErrorAfter)
            .hasProcessingStatus(Offer.ProcessingStatus.NEED_CONTENT)
            .hasContentStatusActiveError(expectedError);

        assertThatLogWasWritten(0);
    }

    @Test
    public void testDontSendWhenGroupIsTooBig() {
        int groupSizeLimit = 1;
        storageKeyValueService.putValue(GROUP_SIZE_LIMIT_KEY, groupSizeLimit);
        storageKeyValueService.invalidateCache();

        var offer11 = offer(businessSupplier, "offer11", CONTENT_PROCESSING, 1, 1L);
        var offer12 = offer(businessSupplier, "offer12", CONTENT_PROCESSING, 1, 1L);

        var offer21 = offer(businessSupplier, "offer21", CONTENT_PROCESSING, 2, 1L);
        var offer22 = offer(businessSupplier, "offer22", CONTENT_PROCESSING, 2, 1L);

        offerRepository.insertOffers(offer22);
        queue.deleteAll();

        offerRepository.insertOffers(offer11, offer12, offer21);
        assertThat(queue.findAll()).hasSize(3);

        makeReadyInQueue(offer11, offer12, offer21);
        configureDatacampResponse(offer11.getBusinessSkuKey(), 1, 1L);
        configureDatacampResponse(offer12.getBusinessSkuKey(), 1, 1L);
        configureDatacampResponse(offer21.getBusinessSkuKey(), 2, 1L);
        configureDatacampResponse(offer22.getBusinessSkuKey(), 2, 1L);
        configureAgResponses(0);

        var result = service.executeTask(
            service.new SenderTaskContext(queue.findAll())
        );

        var expectedError = MbocErrors.get()
            .contentProcessingGroupTooBig(offer11.getShopSku(), offer11.getGroupId(), groupSizeLimit);

        assertThat(queue.findAll()).hasSize(0);
        assertThat(result.getOffersFailed().values())
            .hasSize(3)
            .allMatch(fr -> fr.getType() == FailureType.VALIDATION)
            .allMatch(fr -> fr.hasError())
            .extracting(fr -> fr.getError().getErrorCode())
            .allMatch(expectedError.getErrorCode()::equals);
        assertThat(result.getOffersSentCount()).isEqualTo(0);

        List<Offer> offersWithErrorAfter = offerRepository.findOffersByBusinessSkuKeys(
            offer11.getBusinessSkuKey(),
            offer12.getBusinessSkuKey(),
            offer21.getBusinessSkuKey()
        );

        assertThat(offersWithErrorAfter)
            .hasSize(3)
            .allSatisfy(offerAfter ->
                MbocAssertions.assertThat(offerAfter)
                    .hasProcessingStatus(Offer.ProcessingStatus.NEED_CONTENT)
                    .hasContentStatusActiveError(MbocErrors.get()
                        .contentProcessingGroupTooBig(offerAfter.getShopSku(), offerAfter.getGroupId(), groupSizeLimit))
            );

        assertThatLogWasWritten(0);
    }

    @Test
    public void testSendingNullGroupId() {
        var offer11 = offer(businessSupplier, "offer11", CONTENT_PROCESSING, null, 1L);

        var offer12 = offer(businessSupplier, "offer12", CONTENT_PROCESSING, null, 1L);
        offer11.setMarketSpecificContentHash(-7018494869575931829L);
        offer12.setMarketSpecificContentHash(-7018494869575931829L);
        offerRepository.insertOffers(offer11, offer12);

        List<ContentProcessingOffer> offersToProcess = new Vector<>();
        offersToProcess.add(new ContentProcessingOffer(offer11.getBusinessId(), offer11.getShopSku(), 1,
            offer11.getCreated().toInstant(ZoneOffset.UTC)));
        offersToProcess.add(new ContentProcessingOffer(offer12.getBusinessId(), offer12.getShopSku(), 1,
            offer12.getCreated().toInstant(ZoneOffset.UTC)));

        assertThat(queue.findAll()).hasSize(2);
        offer11.setGroupId(1);
        offer12.setGroupId(1);
        makeReadyInQueue(offer11);
        makeReadyInQueue(offer12);
        configureDatacampResponse(new Offer(offer11), 1L, b -> {
        });
        configureDatacampResponse(new Offer(offer12), 1L, b -> {
        });
        configureAgResponses(0);

        var result = service.executeTask(
            service.new SenderTaskContext(offersToProcess)
        );

        assertThat(queue.findAll()).hasSize(2);
        assertThat(result.getOffersFailed().values())
            .hasSize(2)
            .allMatch(fr -> fr.getType() == FailureType.FAILED_IN_GROUP);
        result.getOffersFailed().entrySet().forEach(it -> {
            assertThat(it.getValue().hasError()).isTrue();
            assertThat(it.getValue().getError().getErrorCode())
                .isEqualTo(gerNullGroupExpectedError(it.getKey().getShopSku()).getErrorCode());
        });

        assertThat(result.getOffersSentCount()).isEqualTo(0);

        List<Offer> offersWithErrorAfter = offerRepository.findOffersByBusinessSkuKeys(
            offer11.getBusinessSkuKey(),
            offer12.getBusinessSkuKey()
        );

        assertThat(offersWithErrorAfter)
            .hasSize(2)
            .allSatisfy(offerAfter ->
                MbocAssertions.assertThat(offerAfter)
                    .hasProcessingStatus(CONTENT_PROCESSING)
                    .hasContentStatusActiveError(gerNullGroupExpectedError(offerAfter.getShopSku()))
            );

        assertThatLogWasWritten(0);
    }

    private ErrorInfo gerNullGroupExpectedError(String offerId) {
        return MbocErrors.get()
            .contentProcessingBadGroupId(offerId, null);
    }

    @Test
    public void testGroupedOfferWithoutContentIsExcluded() {
        var offerGood = offer(businessSupplier, "offer11", CONTENT_PROCESSING, 1, 1L);
        var offerNoHash = offer(businessSupplier, "offer12", NEED_CONTENT, 1, 1L)
            .setMarketSpecificContentHash(null)
            .setMarketSpecificContentHashSent(null);

        offerRepository.insertOffers(offerGood, offerNoHash);
        assertThat(queue.findAll())
            .extracting(ContentProcessingOffer::getKey)
            .containsExactlyInAnyOrder(offerGood.getBusinessSkuKey());

        makeReadyInQueue(offerGood);
        configureDatacampResponse(offerGood.getBusinessSkuKey(), 1, 1L);
        configureDatacampResponse(offerNoHash.copy(), 1L, __ -> {
        }, false);
        configureAgResponses(0);

        var result = service.executeTask(
            service.new SenderTaskContext(queue.findAll())
        );

        assertThat(queue.findAll()).isEmpty();

        assertThat(result.getOffersFailed().values())
            .extracting(FailureReason::getType)
            .containsExactlyInAnyOrder(FailureType.EMPTY_MBOC_SENT_HASH);
        // send only good offer, error for bad
        assertThat(result.getOffersSentCount()).isEqualTo(1);

        var offersAfter = offerRepository.findOffersByBusinessSkuKeys(
            offerGood.getBusinessSkuKey(),
            offerNoHash.getBusinessSkuKey()
        );
        var offersStatuses = offersAfter.stream()
            .collect(Collectors.toMap(Offer::getBusinessSkuKey, Offer::getProcessingStatus));
        assertThat(offersStatuses)
            .containsExactlyInAnyOrderEntriesOf(Map.of(
                offerGood.getBusinessSkuKey(), CONTENT_PROCESSING,
                offerNoHash.getBusinessSkuKey(), NEED_CONTENT
            ));

        assertThatLogWasWritten(1);
    }

    @Test
    @Override
    public void testChooseValidationCheckWithHigherLevel() {
        // OK, will be sent
        var offerGood = offer(businessSupplier, "offerGood", CONTENT_PROCESSING, 1, 1L);
        // has two problems: different hash + approved content mapping
        // approved content mapping wins and offer is dequeued
        var offerNoHashButMapped = offer(businessSupplier, "offerNoHashButMapped", CONTENT_PROCESSING, 1, 1L)
            .setMarketSpecificContentHashSent(null)
            .updateApprovedSkuMapping(
                OfferTestUtils.mapping(100L, Offer.SkuType.MARKET), Offer.MappingConfidence.CONTENT);
        testChooseValidationCheckWithHigherLevelBody(offerGood, offerNoHashButMapped);
    }

    @Test
    @Override
    public void testErrorCardCreationNotAllowed() {
        var offerBad = offer(businessSupplier, "offerBad", INVALID, 111, 1L)
            .updateContentProcessingStatus(Offer.ContentProcessingStatus.CONTENT_PROCESSING)
            .setCategoryIdForTests(restrictedCategory.getCategoryId(), Offer.BindingKind.APPROVED);
        var offerGood = offer(businessSupplier, "offerGood", CONTENT_PROCESSING, 111, 1L)
            .updateContentProcessingStatus(Offer.ContentProcessingStatus.CONTENT_PROCESSING);
        testErrorCardCreationNotAllowed(offerBad, offerGood);
    }

    @Test
    public void testAgBatchLimit() {
        storageKeyValueService.putValue(BATCH_SIZE_KEY, 100);
        storageKeyValueService.putValue(AG_BATCH_LIMIT_KEY, 3);
        storageKeyValueService.invalidateCache();

        // big group of 4 offers (more than AG_BATCH_LIMIT_KEY)
        var offer11 = offer(businessSupplier, "offer11", NEED_CONTENT, 1, 1L);
        var offer12 = offer(businessSupplier, "offer12", NEED_CONTENT, 1, 1L);
        var offer13 = offer(businessSupplier, "offer13", NEED_CONTENT, 1, 1L);
        var offer14 = offer(businessSupplier, "offer14", NEED_CONTENT, 1, 1L);

        // group of 2 offers (less then AG_BATCH_LIMIT_KEY)
        var offer21 = offer(businessSupplier, "offer21", NEED_CONTENT, 2, 1L);
        var offer22 = offer(businessSupplier, "offer22", NEED_CONTENT, 2, 1L);

        // group of 2 offers (less then AG_BATCH_LIMIT_KEY)
        var offer31 = offer(businessSupplier, "offer31", NEED_CONTENT, 3, 1L);
        var offer32 = offer(businessSupplier, "offer32", NEED_CONTENT, 3, 1L);

        // group of 1 offer, should be processed together with another group (less then AG_BATCH_LIMIT_KEY)
        var offer41 = offer(businessSupplier, "offer41", NEED_CONTENT, 4, 1L);

        offerRepository.insertOffers(
            offer11, offer12, offer13, offer14,
            offer21, offer22,
            offer31, offer32,
            offer41
        );

        makeReadyInQueue(
            offer11, offer12, offer13, offer14,
            offer21, offer22,
            offer31, offer32,
            offer41
        );
        configureDatacampResponse(Map.of(
            offer11, 1L,
            offer12, 1L,
            offer13, 1L,
            offer14, 1L,
            offer21, 1L,
            offer22, 1L,
            offer31, 1L,
            offer32, 1L,
            offer41, 1L
        ));
        configureAgResponses(0);

        queue.readAllWithoutGroupId(b -> assertThat(b).hasSize(4), 100, 100);

        service.sendFromQueue();

        // three batches:
        // * batch of 4 offers (although limit is 3) for group 1
        // * batch of 3 offers for groups (2 and 4) or (3 and 4)
        // * batch of 2 offers for group 3 or 3
        verify(agServiceImpl, times(3)).startContentProcessing(any(), any());
        assertThatLogWasWritten(9);
        assertThat(queue.findAll()).hasSize(0);
    }

    @Test
    public void testUnforcesForcedGroupedOffer() {
        var grouped = offer(businessSupplier, "grouped1", CONTENT_PROCESSING, 11, 1L);
        offerRepository.insertOffers(grouped);

        makeReadyInQueue(grouped);
        var queuItemsForced = queue.findAll().stream().map(x -> x.setForce(true)).collect(Collectors.toList());
        queue.updateBatch(queuItemsForced);

        // send offer to TRASH to trigger force-able WRONG_STATUS error
        grouped = offerRepository.findOfferByBusinessSkuKey(grouped.getBusinessSkuKey());
        grouped.updateAcceptanceStatusForTests(Offer.AcceptanceStatus.TRASH);
        offerRepository.updateOffers(grouped);

        configureDatacampResponse(Map.of(grouped, 1L));
        configureAgResponses(0);

        assertThat(queue.findGroupsReady()).hasSize(1);
        assertThat(queue.findAll())
            .hasSize(1)
            .allMatch(ContentProcessingOffer::isForce);

        service.sendFromQueue();

        assertThat(queue.findGroupsReady()).hasSize(0);
        assertThat(queue.findAll()).isEmpty();
        // grouped offer has been un-forced and not sent because of error
        verify(agServiceImpl, times(0)).startContentProcessing(any(), any());
    }

    @Test
    @Override
    public void testSuccessfullyProcessDeduplicated() {
        var offer1 = offer(businessSupplier, "offer1", NEED_CONTENT, 1, 1L)
            .updateApprovedSkuMapping(
                OfferTestUtils.mapping(100, Offer.SkuType.PARTNER20),
                Offer.MappingConfidence.PARTNER_SELF);
        var offer2 = offer(businessSupplier, "offer2", AUTO_PROCESSED, 2, 1L)
            .setVendorId(OfferTestUtils.TEST_VENDOR_ID)
            .setForceGoodContentStatus(Offer.ForceGoodContentStatus.FORCE_NOT_GOOD_CONTENT)
            .setModelId(1L)
            .updateApprovedSkuMapping(
                OfferTestUtils.mapping(100, Offer.SkuType.PARTNER20),
                Offer.MappingConfidence.PARTNER)
            .updateProcessingStatusIfValid(Offer.ProcessingStatus.AUTO_PROCESSED);

        testSuccessfullyProcessDeduplicated(List.of(offer1, offer2), true);
    }

    @Test
    @Override
    public void testCorrectHandlingSecondaryOffers() {
        var offer1 = offer(businessSupplier, "offer1", NEED_CONTENT, 1, 1L,
            DateTimeUtils.dateTimeNow().minus(100, ChronoUnit.SECONDS))
            .updateApprovedSkuMapping(
                OfferTestUtils.mapping(100, Offer.SkuType.PARTNER20),
                Offer.MappingConfidence.PARTNER_SELF);
        var offer2 = offer(businessSupplier, "offer2", AUTO_PROCESSED, 1, 1L)
            .setVendorId(OfferTestUtils.TEST_VENDOR_ID)
            .setForceGoodContentStatus(Offer.ForceGoodContentStatus.FORCE_NOT_GOOD_CONTENT)
            .setModelId(1L)
            .updateApprovedSkuMapping(
                OfferTestUtils.mapping(100, Offer.SkuType.PARTNER20),
                Offer.MappingConfidence.PARTNER)
            .updateProcessingStatusIfValid(Offer.ProcessingStatus.AUTO_PROCESSED);

        testCorrectHandlingSecondaryOffers(List.of(offer1, offer2), offer1);
    }

    @Test
    @Override
    public void testSendAnyCsku() {
        var offerGood = offer(cskuSupplier, "offerGood", PROCESSED, 1, 1L)
            .updateApprovedSkuMapping(OfferTestUtils.mapping(2L, Offer.SkuType.PARTNER20),
                Offer.MappingConfidence.CONTENT);

        testSendAnyCsku(offerGood);
    }

    @Test
    @Override
    public void testSendMskuMarkedAllowPartnerContent() {
        storageKeyValueService.putValue(CONFIG_ENABLE_MODEL_ALLOW_PARTNER_CONTENT_KEY, true);
        storageKeyValueService.invalidateCache();

        var msku = createMsku(2L)
            .setAllowPartnerContent(true);
        mskuRepository.save(msku);

        var offerGood = offer(businessSupplier, "offerGood", PROCESSED, 1, 1L)
            .updateApprovedSkuMapping(
                OfferTestUtils.mapping(msku.getMarketSkuId(), Offer.SkuType.MARKET),
                Offer.MappingConfidence.CONTENT
            );

        testSendMskuMarkedAllowPartnerContent(offerGood);

        storageKeyValueService.putValue(CONFIG_ENABLE_MODEL_ALLOW_PARTNER_CONTENT_KEY, false);
        storageKeyValueService.invalidateCache();
    }

    @Test
    @Override
    public void testSendCorrectAllowCreateUpdateForCskuWithManualInsertToQueue() {
        var offer = offer(cskuSupplier, "offerGood", PROCESSED, 1, 1L)
            .updateApprovedSkuMapping(OfferTestUtils.mapping(2L, Offer.SkuType.PARTNER20),
                Offer.MappingConfidence.CONTENT);

        testSendCorrectAllowCreateUpdateForCskuWithManualInsertToQueue(offer, true);
    }

    @Test
    public void testShouldSkipFrozenModelsOffersGrouped() {
        storageKeyValueService.putValue(AbstractContentProcessingSenderService.DISABLE_GG_CATEGORY_CHANGE, true);
        storageKeyValueService.invalidateCache();

        var frozenMsku = new Msku();
        frozenMsku.setMarketSkuId(1L)
            .setCategoryId(1L)
            .setParentModelId(111L)
            .setVendorId(1L)
            .setCreationTs(Instant.now())
            .setModificationTs(Instant.now());
        var simpleMsku1 = new Msku();
        simpleMsku1.setMarketSkuId(2L)
            .setCategoryId(2L)
            .setParentModelId(222L)
            .setVendorId(2L)
            .setCreationTs(Instant.now())
            .setModificationTs(Instant.now());
        var simpleMsku2 = new Msku();
        simpleMsku2.setMarketSkuId(3L)
            .setCategoryId(2L)
            .setParentModelId(333L)
            .setVendorId(2L)
            .setCreationTs(Instant.now())
            .setModificationTs(Instant.now());
        mskuRepository.save(frozenMsku, simpleMsku1, simpleMsku2);

        categoryService.addCategory(1L);
        categoryService.addCategory(2L);
        categoryService.addCategory(3L);

        var offerWithFrozenMsku = offer(businessSupplier, "offer1", NEED_CONTENT, null, 1L)
            .setId(1111)
            .setModelId(111L)
            .setGroupId(1)
            .setCategoryIdInternal(1L)
            .setMappedCategoryId(1L)
            .setApprovedSkuMappingInternal(new Offer.Mapping(frozenMsku.getMarketSkuId(), LocalDateTime.now()))
            .setApprovedSkuMappingConfidence(Offer.MappingConfidence.AUTO_APPROVE);

        var offerWithFrozenMskuAndDiffCategories = offer(businessSupplier, "offer2", NEED_CONTENT, null, 1L)
            .setId(2000)
            .setModelId(222L)
            .setGroupId(2)
            .setCategoryIdInternal(2L)
            .setMappedCategoryId(2L)
            .setApprovedSkuMappingInternal(new Offer.Mapping(frozenMsku.getMarketSkuId(), LocalDateTime.now()))
            .setApprovedSkuMappingConfidence(Offer.MappingConfidence.AUTO_APPROVE);

        var offerWithInconsistentCategories = offer(businessSupplier, "offer3", NEED_CONTENT, null, 1L)
            .setId(3000)
            .setModelId(333L)
            .setGroupId(3)
            .setCategoryIdInternal(4L)
            .setMappedCategoryId(4L)
            .setApprovedSkuMappingInternal(new Offer.Mapping(simpleMsku2.getMarketSkuId(), LocalDateTime.now()))
            .setApprovedSkuMappingConfidence(Offer.MappingConfidence.AUTO_APPROVE);

        var migrationModel1 = new MigrationModel()
            .setSourceOfferId(offerWithFrozenMsku.getId())
            .setModelId(offerWithFrozenMsku.getModelId())
            .setChangeSource(MigrationModelChangeSource.CLASSIFICATION)
            .setStatus(MigrationModelStatus.IN_PROCESS);
        var migrationModel2 = new MigrationModel()
            .setModelId(offerWithFrozenMskuAndDiffCategories.getModelId())
            .setSourceOfferId(offerWithFrozenMskuAndDiffCategories.getId())
            .setChangeSource(MigrationModelChangeSource.CLASSIFICATION)
            .setStatus(MigrationModelStatus.IN_PROCESS);
        migrationModelRepository.save(migrationModel1, migrationModel2);

        var offers = List.of(offerWithFrozenMsku, offerWithFrozenMskuAndDiffCategories,
            offerWithInconsistentCategories);
        testShouldSkipFrozenModelsOffers(offers);
    }
}
