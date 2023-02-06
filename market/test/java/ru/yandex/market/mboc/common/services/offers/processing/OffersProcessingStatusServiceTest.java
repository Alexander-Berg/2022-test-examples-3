package ru.yandex.market.mboc.common.services.offers.processing;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.market.mboc.common.MbocErrors;
import ru.yandex.market.mboc.common.assertions.MbocAssertions;
import ru.yandex.market.mboc.common.assertions.custom.OfferAssertions;
import ru.yandex.market.mboc.common.offers.model.AntiMapping;
import ru.yandex.market.mboc.common.offers.model.ContentComment;
import ru.yandex.market.mboc.common.offers.model.ContentCommentType;
import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.offers.repository.search.OfferCriterias;
import ru.yandex.market.mboc.common.offers.repository.search.OffersFilter;
import ru.yandex.market.mboc.common.services.category.models.Category;
import ru.yandex.market.mboc.common.services.category_info.info.CategoryInfo;
import ru.yandex.market.mboc.common.utils.DateTimeUtils;
import ru.yandex.market.mboc.common.utils.OfferTestUtils;
import ru.yandex.market.mboc.common.utils.expression.DescribeExprHandler;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.mboc.common.offers.model.Offer.MappingConfidence.CONTENT;
import static ru.yandex.market.mboc.common.offers.model.Offer.MappingConfidence.PARTNER;
import static ru.yandex.market.mboc.common.offers.model.Offer.MappingConfidence.PARTNER_SELF;
import static ru.yandex.market.mboc.common.offers.model.Offer.MappingConfidence.RESET;

public class OffersProcessingStatusServiceTest extends OffersProcessingStatusServiceTestBase {

    @Test
    public void sendNotOkToOpen() {
        supplier.setNewContentPipeline(false);
        supplierRepository.update(supplier);

        Offer offerOk = newOffer(supplier, Offer.ProcessingStatus.OPEN, Offer.BindingKind.APPROVED)
            .setId(1)
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
            .setSupplierSkuMapping(OfferTestUtils.mapping(2));
        Offer offerNew = newOffer(supplier, Offer.ProcessingStatus.OPEN, Offer.BindingKind.APPROVED)
            .setId(2)
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
            .setSupplierSkuMapping(OfferTestUtils.mapping(2));
        Offer offerTrash = newOffer(supplier, Offer.ProcessingStatus.OPEN, Offer.BindingKind.APPROVED)
            .setId(3)
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
            .setSupplierSkuMapping(OfferTestUtils.mapping(2));

        offersProcessingStatusService.processOffers(List.of(offerOk, offerNew, offerTrash));
        assertThat(offerOk.getProcessingStatus()).isEqualTo(Offer.ProcessingStatus.IN_PROCESS);
        assertThat(offerNew.getProcessingStatus()).isEqualTo(Offer.ProcessingStatus.IN_PROCESS);
        assertThat(offerTrash.getProcessingStatus()).isEqualTo(Offer.ProcessingStatus.IN_PROCESS);

        // And now NEW'd and TRASH'd
        offerNew.updateAcceptanceStatusForTests(Offer.AcceptanceStatus.NEW);
        offerTrash.updateAcceptanceStatusForTests(Offer.AcceptanceStatus.TRASH);
        offersProcessingStatusService.processOffers(List.of(offerOk, offerNew, offerTrash));
        assertThat(offerOk.getProcessingStatus()).isEqualTo(Offer.ProcessingStatus.IN_PROCESS);
        assertThat(offerNew.getProcessingStatus()).isEqualTo(Offer.ProcessingStatus.OPEN);
        assertThat(offerTrash.getProcessingStatus()).isEqualTo(Offer.ProcessingStatus.OPEN);
    }

    @Test
    public void sendToWaitContent() {
        long id = 1234L;
        Offer offer = OfferTestUtils.nextOffer()
            .setId(id)
            .setAcceptanceStatusInternal(Offer.AcceptanceStatus.OK)
            .setCategoryIdForTests(CATEGORY_ID, Offer.BindingKind.SUGGESTED)
            .setBusinessId(supplier.getId())
            .setTitle("OfferTitle")
            .setProcessingStatusInternal(Offer.ProcessingStatus.OPEN)
            .setContentComments(List.of(new ContentComment(ContentCommentType.FOR_REVISION, List.of("Comment1"))));
        offerRepository.insertOffer(offer);

        offersProcessingStatusService.processAndUpdateOfferByIds(List.of(id));
        Offer updated = offerRepository.getOfferById(id);
        assertThat(updated)
            .extracting(Offer::getProcessingStatus)
            .isEqualTo(Offer.ProcessingStatus.WAIT_CONTENT);
    }

    @Test
    public void testOnlyApprovedGoesToNeedContent() {
        Offer unclassifiedOffer = newOffer(Offer.ProcessingStatus.OPEN, Offer.BindingKind.SUGGESTED)
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK);
        Offer classifiedOffer = newOffer(Offer.ProcessingStatus.OPEN, Offer.BindingKind.APPROVED)
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK);
        Offer matchedBlueOffer = newOffer(Offer.ProcessingStatus.OPEN, Offer.BindingKind.APPROVED)
            .setModelId(1L)
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
            .setOfferDestination(Offer.MappingDestination.BLUE);
        Offer matchedWhiteOffer = newOffer(Offer.ProcessingStatus.OPEN, Offer.BindingKind.APPROVED)
            .setModelId(1L)
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
            .setOfferDestination(Offer.MappingDestination.WHITE);
        Offer matchedDSBSOffer = newOffer(Offer.ProcessingStatus.OPEN, Offer.BindingKind.APPROVED)
            .setModelId(1L)
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
            .setOfferDestination(Offer.MappingDestination.DSBS);
        Offer inModerationOffer = newOffer(Offer.ProcessingStatus.IN_MODERATION, Offer.BindingKind.APPROVED)
            .setSuggestSkuMapping(OfferTestUtils.mapping(1L, Offer.SkuType.MARKET))
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK);

        Collection<Offer> offers = Arrays.asList(unclassifiedOffer, classifiedOffer, inModerationOffer,
            matchedBlueOffer, matchedWhiteOffer, matchedDSBSOffer);
        offerRepository.insertOffers(offers);
        Collection<Long> ids = offers.stream().map(Offer::getId).collect(Collectors.toList());

        offersProcessingStatusService.processAndUpdateOfferByIds(ids);

        Map<Long, Offer> updatedOffers = getOffers(ids);
        OfferAssertions.assertThat(updatedOffers.get(unclassifiedOffer.getId()))
            .hasProcessingStatus(Offer.ProcessingStatus.IN_CLASSIFICATION);
        OfferAssertions.assertThat(updatedOffers.get(classifiedOffer.getId()))
            .hasProcessingStatus(Offer.ProcessingStatus.NEED_CONTENT);
        OfferAssertions.assertThat(updatedOffers.get(matchedBlueOffer.getId()))
            .hasProcessingStatus(Offer.ProcessingStatus.NEED_CONTENT);
        OfferAssertions.assertThat(updatedOffers.get(matchedWhiteOffer.getId()))
            .hasProcessingStatus(Offer.ProcessingStatus.OPEN);
        OfferAssertions.assertThat(updatedOffers.get(matchedDSBSOffer.getId()))
            .hasProcessingStatus(Offer.ProcessingStatus.NEED_CONTENT);
        OfferAssertions.assertThat(updatedOffers.get(inModerationOffer.getId()))
            .hasProcessingStatus(Offer.ProcessingStatus.IN_MODERATION);
    }

    @Test
    public void testInReclassification() {
        categoryCachingServiceMock.setCategoryAcceptGoodContent(CATEGORY_ID, false);
        long id = 1234L;

        Offer offer = OfferTestUtils.nextOffer()
            .setId(id)
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
            .setCategoryIdForTests(CATEGORY_ID, Offer.BindingKind.SUGGESTED)
            .setBusinessId(supplier.getId())
            .setTitle("OfferTitle")
            .setProcessingStatusInternal(Offer.ProcessingStatus.OPEN)
            .setContentComments(List.of(new ContentComment(ContentCommentType.WRONG_CATEGORY, List.of("Comment1"))));

        offerRepository.insertOffers(List.of(offer));
        offersProcessingStatusService.processAndUpdateOfferByIds(List.of(id));

        Offer updatedOffer = offerRepository.getOfferById(id);
        Assert.assertEquals(Offer.ProcessingStatus.IN_RECLASSIFICATION, updatedOffer.getProcessingStatus());

        offersProcessingStatusService.processAndUpdateOfferByIds(List.of(id));
        updatedOffer = offerRepository.getOfferById(id);
        // remains in reclassification
        Assert.assertEquals(Offer.ProcessingStatus.IN_RECLASSIFICATION, updatedOffer.getProcessingStatus());
    }

    @Test
    public void testAllButWhiteGoesToNeedContent() {
        Offer unclassifiedOffer = newOffer(OfferTestUtils.whiteSupplier(),
            Offer.ProcessingStatus.OPEN, Offer.BindingKind.SUGGESTED)
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK);
        Offer classifiedOffer = newOffer(Offer.ProcessingStatus.OPEN, Offer.BindingKind.APPROVED)
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK);
        Offer matchedBlueOffer = newOffer(Offer.ProcessingStatus.OPEN, Offer.BindingKind.APPROVED)
            .setModelId(1L)
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
            .setOfferDestination(Offer.MappingDestination.BLUE);
        Offer matchedWhiteOffer = newOffer(Offer.ProcessingStatus.OPEN, Offer.BindingKind.APPROVED)
            .setModelId(1L)
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
            .setOfferDestination(Offer.MappingDestination.WHITE);
        Offer matchedDSBSOffer = newOffer(Offer.ProcessingStatus.OPEN, Offer.BindingKind.APPROVED)
            .setModelId(1L)
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
            .setOfferDestination(Offer.MappingDestination.DSBS);
        Offer inModerationOffer = newOffer(Offer.ProcessingStatus.IN_MODERATION, Offer.BindingKind.APPROVED)
            .setSuggestSkuMapping(OfferTestUtils.mapping(1L, Offer.SkuType.MARKET))
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK);

        Collection<Offer> offers = Arrays.asList(unclassifiedOffer, classifiedOffer, inModerationOffer,
            matchedBlueOffer, matchedWhiteOffer, matchedDSBSOffer);
        offerRepository.insertOffers(offers);
        Collection<Long> ids = offers.stream().map(Offer::getId).collect(Collectors.toList());

        offersProcessingStatusService.processAndUpdateOfferByIds(ids);

        Map<Long, Offer> updatedOffers = getOffers(ids);
        OfferAssertions.assertThat(updatedOffers.get(unclassifiedOffer.getId()))
            .hasProcessingStatus(Offer.ProcessingStatus.OPEN);
        OfferAssertions.assertThat(updatedOffers.get(classifiedOffer.getId()))
            .hasProcessingStatus(Offer.ProcessingStatus.NEED_CONTENT);
        OfferAssertions.assertThat(updatedOffers.get(matchedBlueOffer.getId()))
            .hasProcessingStatus(Offer.ProcessingStatus.NEED_CONTENT);
        OfferAssertions.assertThat(updatedOffers.get(matchedWhiteOffer.getId()))
            .hasProcessingStatus(Offer.ProcessingStatus.OPEN);
        OfferAssertions.assertThat(updatedOffers.get(matchedDSBSOffer.getId()))
            .hasProcessingStatus(Offer.ProcessingStatus.NEED_CONTENT);
        OfferAssertions.assertThat(updatedOffers.get(inModerationOffer.getId()))
            .hasProcessingStatus(Offer.ProcessingStatus.IN_MODERATION);
    }

    @Test
    public void testContentProcessingBeforeNeedContent() {
        Offer offerWithContent = newOffer(Offer.ProcessingStatus.OPEN, Offer.BindingKind.APPROVED)
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
            .setMarketSpecificContentHash(1234L);
        Offer offerWithoutContent = newOffer(Offer.ProcessingStatus.OPEN, Offer.BindingKind.APPROVED)
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
            .setMarketSpecificContentHash(null);

        Collection<Offer> offers = List.of(offerWithContent, offerWithoutContent);
        offerRepository.insertOffers(offers);
        Collection<Long> ids = offers.stream().map(Offer::getId).collect(Collectors.toList());

        offersProcessingStatusService.processAndUpdateOfferByIds(ids);

        Map<Long, Offer> updatedOffers = getOffers(ids);
        OfferAssertions.assertThat(updatedOffers.get(offerWithContent.getId()))
            .hasProcessingStatus(Offer.ProcessingStatus.CONTENT_PROCESSING);
        OfferAssertions.assertThat(updatedOffers.get(offerWithoutContent.getId()))
            .hasProcessingStatus(Offer.ProcessingStatus.NEED_CONTENT);
    }

    @Test
    public void testOpenedWhiteOffersStayInOpenStatusButDsbsGoToNeedContent() {
        Offer whiteOffer = newOffer(
            OfferTestUtils.whiteSupplier(), Offer.ProcessingStatus.OPEN, Offer.BindingKind.SUGGESTED
        );
        Offer dsbsOffer = newOffer(
            OfferTestUtils.dsbsSupplierUnderBiz(), Offer.ProcessingStatus.OPEN, Offer.BindingKind.SUGGESTED
        );

        Collection<Offer> offers = List.of(whiteOffer, dsbsOffer);
        offerRepository.insertOffers(offers);
        Collection<Long> ids = offers.stream().map(Offer::getId).collect(Collectors.toList());

        offersProcessingStatusService.processAndUpdateOfferByIds(ids);

        Map<Long, Offer> updatedOffers = getOffers(ids);
        OfferAssertions.assertThat(updatedOffers.get(whiteOffer.getId()))
            .hasProcessingStatus(Offer.ProcessingStatus.OPEN);
        OfferAssertions.assertThat(updatedOffers.get(dsbsOffer.getId()))
            .hasProcessingStatus(Offer.ProcessingStatus.NEED_CONTENT);
    }

    @Test
    public void testContentProcessingWithErrorGoesToNeedContent() {
        Offer offerContentNotProcessed = newOffer(Offer.ProcessingStatus.CONTENT_PROCESSING, Offer.BindingKind.APPROVED)
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
            .setMarketSpecificContentHash(1234L)
            .setMarketSpecificContentHashSent(1234L)
            .setContentStatusActiveError(MbocErrors.get().contentProcessingFailed(OfferTestUtils.DEFAULT_SHOP_SKU))
            .setContentProcessed(false);
        Offer offerContentProcessed = newOffer(Offer.ProcessingStatus.CONTENT_PROCESSING, Offer.BindingKind.APPROVED)
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
            .setMarketSpecificContentHash(1234L)
            .setMarketSpecificContentHashSent(1234L)
            .setContentStatusActiveError(MbocErrors.get().contentProcessingFailed(OfferTestUtils.DEFAULT_SHOP_SKU))
            .setContentProcessed(true);

        offersProcessingStatusService.processOffers(List.of(offerContentNotProcessed, offerContentProcessed));

        OfferAssertions.assertThat(offerContentNotProcessed)
            .hasProcessingStatus(Offer.ProcessingStatus.CONTENT_PROCESSING);
        OfferAssertions.assertThat(offerContentProcessed)
            .hasProcessingStatus(Offer.ProcessingStatus.NEED_CONTENT);
    }

    @Test
    public void testDisableModerationForSupplier() {
        List<Offer> offers = List.of(
            createTestOfferForModeration(1),
            createTestOfferForModeration(2).setBusinessId(blueNoModerationSupplier.getId())
        );

        offersProcessingStatusService.processOffers(offers);

        assertThat(offers)
            .usingElementComparatorOnFields("id", "processingStatus")
            .containsExactlyInAnyOrder(
                new Offer().setId(1).setProcessingStatusInternal(Offer.ProcessingStatus.IN_MODERATION),
                // Gone past moderation
                new Offer().setId(2).setProcessingStatusInternal(Offer.ProcessingStatus.IN_CLASSIFICATION)
            );
    }

    @Test
    public void testReprocessedOffersForModeration() {
        offerRepository.insertOffer(createTestOfferForModeration(1)
            .updateProcessingStatusIfValid(Offer.ProcessingStatus.OPEN)
            .setReprocessRequested(true));
        offerRepository.insertOffer(createTestOfferForModeration(2)
            .updateProcessingStatusIfValid(Offer.ProcessingStatus.OPEN));
        offerRepository.insertOffer(createTestOfferForModeration(3)
            .updateProcessingStatusIfValid(Offer.ProcessingStatus.OPEN)
            .setReprocessRequested(true));

        offersProcessingStatusService.processAndUpdateOfferByIds(List.of(1L, 2L, 3L));

        assertThat(offerRepository.findOffers(new OffersFilter()))
            .usingElementComparatorOnFields("id", "processingStatus")
            .containsExactlyInAnyOrder(
                new Offer().setId(1).setProcessingStatusInternal(Offer.ProcessingStatus.IN_CLASSIFICATION),
                new Offer().setId(2).setProcessingStatusInternal(Offer.ProcessingStatus.IN_MODERATION),
                new Offer().setId(3).setProcessingStatusInternal(Offer.ProcessingStatus.IN_CLASSIFICATION)
            );
    }

    @Test
    public void testWhiteOffersSentToClassification() {
        offersProcessingStatusService.setEnableWhiteOffersClassification(true);
        Offer offerBlueOk =
            OfferTestUtils.nextOffer(supplier)
                .setCategoryIdForTests(CATEGORY_ID, Offer.BindingKind.SUGGESTED)
                .updateProcessingStatusIfValid(Offer.ProcessingStatus.OPEN)
                .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK);
        Offer offerWhiteOk =
            OfferTestUtils.nextOffer(whiteSupplier)
                .setCategoryIdForTests(CATEGORY_ID, Offer.BindingKind.SUGGESTED)
                .updateProcessingStatusIfValid(Offer.ProcessingStatus.OPEN)
                .setDataCampOffer(true)
                .setMarketSpecificContentHash(1L) // simulating "white offer has content" case
                .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK);
        Offer offerWhiteNoContent =
            OfferTestUtils.nextOffer(whiteSupplier)
                .setCategoryIdForTests(CATEGORY_ID, Offer.BindingKind.SUGGESTED)
                .updateProcessingStatusIfValid(Offer.ProcessingStatus.OPEN)
                .setDataCampOffer(true)
                .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK);
        offerRepository.insertOffers(offerBlueOk, offerWhiteNoContent, offerWhiteOk);
        long blueId = offerBlueOk.getId();
        long whiteOkId = offerWhiteOk.getId();
        long whiteNoContentId = offerWhiteNoContent.getId();

        offersProcessingStatusService.processAndUpdateOfferByIds(List.of(blueId, whiteOkId, whiteNoContentId));

        OfferAssertions.assertThat(offerRepository.getOfferById(blueId))
            .hasProcessingStatus(Offer.ProcessingStatus.IN_CLASSIFICATION);
        OfferAssertions.assertThat(offerRepository.getOfferById(whiteOkId))
            .hasProcessingStatus(Offer.ProcessingStatus.IN_CLASSIFICATION);
        OfferAssertions.assertThat(offerRepository.getOfferById(whiteNoContentId))
            .hasProcessingStatus(Offer.ProcessingStatus.OPEN);
    }

    @Test
    public void testOffersForModerationFromOpenAndReopen() {
        categoryCachingServiceMock.addCategory(2);
        categoryCachingServiceMock.addCategory(5);
        categoryInfoRepository.insertBatch(Arrays.asList(
            new CategoryInfo(1).setModerationInYang(true),
            new CategoryInfo(2).setModerationInYang(false)));

        // correct offer
        offerRepository.insertOffer(createTestOfferForModeration(1));
        // no mapping
        offerRepository.insertOffer(createTestOfferForModeration(2).setSupplierSkuMapping(null));
        // not in status
        offerRepository.insertOffer(createTestOfferForModeration(3)
            .setProcessingStatusInternal(Offer.ProcessingStatus.IN_MODERATION_REJECTED));
        // no categoryInfo
        offerRepository.insertOffer(createTestOfferForModeration(4).setCategoryIdForTests(5L,
            Offer.BindingKind.SUGGESTED));
        // categoryInfo.moderationInYang==false
        offerRepository.insertOffer(createTestOfferForModeration(5).setCategoryIdForTests(2L,
            Offer.BindingKind.SUGGESTED));
        // correct offer (suggest mapping)
        offerRepository.insertOffer(createTestOfferForModeration(6)
            .setSupplierSkuMapping(null)
            .setSuggestSkuMapping(OfferTestUtils.mapping(15L, Offer.SkuType.MARKET)));
        // REOPEN not allowed with CONTENT mapping
        offerRepository.insertOffer(createTestOfferForModeration(7)
            .updateProcessingStatusIfValid(Offer.ProcessingStatus.REOPEN)
            .updateApprovedSkuMapping(OfferTestUtils.mapping(15L, Offer.SkuType.MARKET), CONTENT));
        // REOPEN not allowed with PARTNER_SELF mapping
        offerRepository.insertOffer(createTestOfferForModeration(8)
            .updateProcessingStatusIfValid(Offer.ProcessingStatus.REOPEN)
            .updateApprovedSkuMapping(OfferTestUtils.mapping(15L, Offer.SkuType.MARKET), PARTNER_SELF));
        // correct REOPEN without approved mapping
        offerRepository.insertOffer(createTestOfferForModeration(9)
            .updateProcessingStatusIfValid(Offer.ProcessingStatus.REOPEN));
        // correct REOPEN with PARTNER approved mapping
        offerRepository.insertOffer(createTestOfferForModeration(10)
            .updateProcessingStatusIfValid(Offer.ProcessingStatus.REOPEN)
            .updateApprovedSkuMapping(OfferTestUtils.mapping(15L, Offer.SkuType.MARKET), PARTNER));
        // not in status
        offerRepository.insertOffer(createTestOfferForModeration(11)
            .setProcessingStatusInternal(Offer.ProcessingStatus.CLASSIFIED));
        // correct offer in status open
        offerRepository.insertOffer(createTestOfferForModeration(12)
            .setProcessingStatusInternal(Offer.ProcessingStatus.OPEN));
        // in_moderation from need_content for GC
        Offer.Mapping gcMapping = new Offer.Mapping(123, DateTimeUtils.dateTimeNow(), Offer.SkuType.MARKET);
        offerRepository.insertOffer(createTestOfferForModeration(13)
            .setProcessingStatusInternal(Offer.ProcessingStatus.NEED_CONTENT)
            .setSupplierSkuMappingStatus(Offer.MappingStatus.NEW)
            .setSupplierSkuMapping(gcMapping));

        List<Long> offerIds = offerRepository.findOffers(new OffersFilter()).stream()
            .map(Offer::getId)
            .collect(Collectors.toList());

        offersProcessingStatusService.processAndUpdateOfferByIds(offerIds);

        List<Offer> offers = offerRepository.findOffers(new OffersFilter());
        assertThat(offers)
            .usingElementComparatorOnFields("id", "processingStatus")
            .containsExactlyInAnyOrder(
                new Offer().setId(1).setProcessingStatusInternal(Offer.ProcessingStatus.IN_MODERATION),
                new Offer().setId(2).setProcessingStatusInternal(Offer.ProcessingStatus.IN_CLASSIFICATION),
                new Offer().setId(3).setProcessingStatusInternal(Offer.ProcessingStatus.IN_CLASSIFICATION),
                new Offer().setId(4).setProcessingStatusInternal(Offer.ProcessingStatus.IN_CLASSIFICATION),
                new Offer().setId(5).setProcessingStatusInternal(Offer.ProcessingStatus.IN_CLASSIFICATION),
                new Offer().setId(6).setProcessingStatusInternal(Offer.ProcessingStatus.IN_MODERATION),
                new Offer().setId(7).setProcessingStatusInternal(Offer.ProcessingStatus.PROCESSED),
                new Offer().setId(8).setProcessingStatusInternal(Offer.ProcessingStatus.AUTO_PROCESSED),
                new Offer().setId(9).setProcessingStatusInternal(Offer.ProcessingStatus.IN_MODERATION),
                new Offer().setId(10).setProcessingStatusInternal(Offer.ProcessingStatus.IN_MODERATION),
                new Offer().setId(11).setProcessingStatusInternal(Offer.ProcessingStatus.IN_MODERATION),
                new Offer().setId(12).setProcessingStatusInternal(Offer.ProcessingStatus.IN_MODERATION),
                new Offer().setId(13).setProcessingStatusInternal(Offer.ProcessingStatus.IN_MODERATION)
                    .setSupplierSkuMapping(gcMapping).setSupplierSkuMappingStatus(Offer.MappingStatus.NEW)
            );
    }

    @Test
    public void testOffersForModerationSplitBySkuType() {
        Offer.Mapping mappingToMarket = new Offer.Mapping(123, DateTimeUtils.dateTimeNow(),
            Offer.SkuType.MARKET);
        Offer.Mapping mappingToPartnerLegacy = new Offer.Mapping(123, DateTimeUtils.dateTimeNow(),
            Offer.SkuType.PARTNER);
        Offer.Mapping mappingToPartner10 = new Offer.Mapping(123, DateTimeUtils.dateTimeNow(),
            Offer.SkuType.PARTNER10);
        Offer.Mapping mappingToPartner20 = new Offer.Mapping(123, DateTimeUtils.dateTimeNow(),
            Offer.SkuType.PARTNER20);

        offerRepository.insertOffer(createTestOfferForModeration(1)
            .setSupplierSkuMapping(mappingToPartnerLegacy)
            .setSuggestSkuMapping(mappingToMarket)
        ); // partner
        offerRepository.insertOffer(createTestOfferForModeration(2)
            .setSupplierSkuMapping(mappingToPartnerLegacy)
            .setSuggestSkuMapping(mappingToPartnerLegacy)
        ); // partner
        offerRepository.insertOffer(createTestOfferForModeration(3)
            .setSupplierSkuMapping(mappingToPartnerLegacy)
            .setSuggestSkuMapping(null)
        ); // partner

        offerRepository.insertOffer(createTestOfferForModeration(4)
            .setSupplierSkuMapping(mappingToPartner10)
            .setSuggestSkuMapping(mappingToMarket)
        ); // market, psku10 not for moderation
        offerRepository.insertOffer(createTestOfferForModeration(5)
            .setSupplierSkuMapping(mappingToPartner10)
            .setSuggestSkuMapping(mappingToPartner10)
        ); // skipped, psku10 not for moderation
        offerRepository.insertOffer(createTestOfferForModeration(6)
            .setSupplierSkuMapping(mappingToPartner10)
            .setSuggestSkuMapping(null)
        ); // skipped, psku10 not for moderation

        offerRepository.insertOffer(createTestOfferForModeration(7)
            .setSupplierSkuMapping(mappingToPartner20)
            .setSuggestSkuMapping(mappingToMarket)
        ); // partner
        offerRepository.insertOffer(createTestOfferForModeration(8)
            .setSupplierSkuMapping(mappingToPartner20)
            .setSuggestSkuMapping(mappingToPartner20)
        ); // partner
        offerRepository.insertOffer(createTestOfferForModeration(9)
            .setSupplierSkuMapping(mappingToPartner20)
            .setSuggestSkuMapping(null)
        ); // partner

        offerRepository.insertOffer(createTestOfferForModeration(10)
            .setSupplierSkuMapping(mappingToMarket)
            .setSuggestSkuMapping(mappingToMarket)
        ); // market
        offerRepository.insertOffer(createTestOfferForModeration(11)
            .setSupplierSkuMapping(mappingToMarket)
            .setSuggestSkuMapping(null)
        ); // market

        List<Long> offerIds = offerRepository.findOffers(new OffersFilter()).stream()
            .map(Offer::getId)
            .collect(Collectors.toList());

        offersProcessingStatusService.processAndUpdateOfferByIds(offerIds);

        assertThat(offerRepository.findOffers(new OffersFilter()
            .addCriteria(OfferCriterias.moderationSkuType(Offer.SkuType.MARKET))))
            .extracting(Offer::getId)
            .containsExactlyInAnyOrder(10L, 11L);
        assertThat(offerRepository.findOffers(new OffersFilter()
            .addCriteria(OfferCriterias.moderationSkuType(Offer.SkuType.PARTNER, Offer.SkuType.PARTNER20))))
            .extracting(Offer::getId)
            .containsExactlyInAnyOrder(1L, 2L, 3L, 7L, 8L, 9L);
    }

    @Test
    public void testAntiMappedIsNotModerated() {
        long idsGenerator = 1L;

        long firstSkuId = 15L;
        long secondSkuId = 16L;

        Offer offerAntiSupplier = createTestOfferForModeration(idsGenerator++)
            .setSupplierSkuMapping(OfferTestUtils.mapping(firstSkuId, Offer.SkuType.MARKET));

        Offer offerAntiSuggest = createTestOfferForModeration(idsGenerator++)
            .setSupplierSkuMapping(null)
            .setSuggestSkuMapping(OfferTestUtils.mapping(firstSkuId, Offer.SkuType.MARKET));

        Offer offerAntiSupplierAndOkSuggest = createTestOfferForModeration(idsGenerator++)
            .setSuggestSkuMapping(OfferTestUtils.mapping(firstSkuId, Offer.SkuType.MARKET))
            .setSuggestSkuMapping(OfferTestUtils.mapping(secondSkuId, Offer.SkuType.MARKET));

        Offer offerOkSupplierAndAntiSuggest = createTestOfferForModeration(idsGenerator++)
            .setSuggestSkuMapping(OfferTestUtils.mapping(firstSkuId, Offer.SkuType.MARKET))
            .setSuggestSkuMapping(OfferTestUtils.mapping(secondSkuId, Offer.SkuType.MARKET));

        Offer offerAntiSupplierAndAntiSuggest = createTestOfferForModeration(idsGenerator++)
            .setSuggestSkuMapping(OfferTestUtils.mapping(firstSkuId, Offer.SkuType.MARKET))
            .setSuggestSkuMapping(OfferTestUtils.mapping(secondSkuId, Offer.SkuType.MARKET));

        Offer offerNeedInfoAntiSupplier = createTestOfferForModeration(idsGenerator++)
            .setSupplierSkuMapping(OfferTestUtils.mapping(firstSkuId, Offer.SkuType.MARKET));

        Offer offerNeedInfoAntiSupplierAndAntiSuggest = createTestOfferForModeration(idsGenerator)
            .setSuggestSkuMapping(OfferTestUtils.mapping(firstSkuId, Offer.SkuType.MARKET))
            .setSuggestSkuMapping(OfferTestUtils.mapping(secondSkuId, Offer.SkuType.MARKET));

        offerRepository.insertOffers(offerAntiSupplier, offerAntiSuggest,
            offerAntiSupplierAndOkSuggest, offerOkSupplierAndAntiSuggest, offerAntiSupplierAndAntiSuggest,
            offerNeedInfoAntiSupplier, offerNeedInfoAntiSupplierAndAntiSuggest);

        antiMappingRepository.insertBatch(
            createAntiMapping(offerAntiSupplier, firstSkuId),
            createAntiMapping(offerAntiSuggest, firstSkuId),
            createAntiMapping(offerAntiSupplierAndOkSuggest, firstSkuId),
            createAntiMapping(offerOkSupplierAndAntiSuggest, secondSkuId),

            createAntiMapping(offerAntiSupplierAndAntiSuggest, firstSkuId),
            createAntiMapping(offerAntiSupplierAndAntiSuggest, secondSkuId),

            createAntiMapping(offerNeedInfoAntiSupplier, firstSkuId)
                .setSourceType(AntiMapping.SourceType.MODERATION_NEED_INFO),

            createAntiMapping(offerNeedInfoAntiSupplierAndAntiSuggest, firstSkuId)
                .setSourceType(AntiMapping.SourceType.MODERATION_NEED_INFO),
            createAntiMapping(offerNeedInfoAntiSupplierAndAntiSuggest, secondSkuId)
        );

        List<Long> offerIds = offerRepository.findOffers(new OffersFilter()).stream()
            .map(Offer::getId)
            .collect(Collectors.toList());

        offersProcessingStatusService.processAndUpdateOfferByIds(offerIds);

        assertThat(offerRepository.findOffers(new OffersFilter()))
            .usingElementComparatorOnFields("id", "processingStatus")
            .containsExactlyInAnyOrder(
                offerAntiSupplier.setProcessingStatusInternal(Offer.ProcessingStatus.IN_CLASSIFICATION),
                offerAntiSuggest.setProcessingStatusInternal(Offer.ProcessingStatus.IN_CLASSIFICATION),
                offerAntiSupplierAndOkSuggest.setProcessingStatusInternal(Offer.ProcessingStatus.IN_MODERATION),
                offerOkSupplierAndAntiSuggest.setProcessingStatusInternal(Offer.ProcessingStatus.IN_MODERATION),
                offerAntiSupplierAndAntiSuggest.setProcessingStatusInternal(Offer.ProcessingStatus.IN_CLASSIFICATION),
                offerNeedInfoAntiSupplier.setProcessingStatusInternal(Offer.ProcessingStatus.IN_MODERATION),
                offerNeedInfoAntiSupplierAndAntiSuggest
                    .setProcessingStatusInternal(Offer.ProcessingStatus.IN_MODERATION)
            );
    }

    @Test
    public void testUnprocessedWhiteOfferIsSentToOpen() {
        supplierRepository.insertOrUpdateAll(List.of(
            OfferTestUtils.businessSupplier()
                .setNewContentPipeline(false),
            OfferTestUtils.whiteSupplierUnderBiz()
        ));

        Offer whiteOfferSuddenlyInProcess = newOffer(OfferTestUtils.whiteSupplierUnderBiz(),
            Offer.ProcessingStatus.IN_PROCESS,
            Offer.BindingKind.APPROVED)
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
            .setBusinessId(OfferTestUtils.BIZ_ID_SUPPLIER);

        offersProcessingStatusService.processOffers(List.of(whiteOfferSuddenlyInProcess));

        MbocAssertions.assertThat(whiteOfferSuddenlyInProcess)
            .hasProcessingStatus(Offer.ProcessingStatus.OPEN);
    }

    @Test
    public void testUnprocessedDsbsOfferIsSentToOpen() {
        supplierRepository.insertOrUpdateAll(List.of(
            OfferTestUtils.businessSupplier()
                .setNewContentPipeline(false),
            OfferTestUtils.dsbsSupplierUnderBiz()
        ));

        Offer dsbsNoSuggestNoContent = newOffer(OfferTestUtils.dsbsSupplierUnderBiz(),
            Offer.ProcessingStatus.IN_MODERATION,
            Offer.BindingKind.APPROVED)
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
            .setBusinessId(OfferTestUtils.BIZ_ID_SUPPLIER);

        offersProcessingStatusService.processOffers(List.of(dsbsNoSuggestNoContent));

        MbocAssertions.assertThat(dsbsNoSuggestNoContent)
            .hasProcessingStatus(Offer.ProcessingStatus.OPEN);
    }

    @Test
    public void approveForNewPipeline() {
        long categoryIdNoGood = CATEGORY_ID;
        long categoryIdNoGoodNoKnowledge = CATEGORY_ID + 1;
        long categoryIdGood = CATEGORY_ID + 2;
        categoryCachingServiceMock.addCategories(
            new Category()
                .setCategoryId(categoryIdNoGood)
                .setHasKnowledge(true)
                .setAcceptGoodContent(false),
            new Category()
                .setCategoryId(categoryIdNoGoodNoKnowledge)
                .setHasKnowledge(false)
                .setAcceptGoodContent(false),
            new Category()
                .setCategoryId(categoryIdGood)
                .setHasKnowledge(true)
                .setAcceptGoodContent(true)
        );

        categoryKnowledgeService.addCategory(categoryIdNoGood);
        categoryKnowledgeService.removeCategory(categoryIdNoGoodNoKnowledge);
        categoryKnowledgeService.addCategory(categoryIdGood);

        long idGen = 0;

        Offer withSupplierMapping = newOffer(supplier, Offer.ProcessingStatus.OPEN, Offer.BindingKind.SUGGESTED)
            .setId(idGen++)
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
            .setCategoryIdForTests(categoryIdNoGood, Offer.BindingKind.SUGGESTED)
            .setSuggestSkuMapping(null)
            .setSupplierSkuMapping(OfferTestUtils.mapping(2));

        Offer withSuggestMapping = newOffer(supplier, Offer.ProcessingStatus.OPEN, Offer.BindingKind.SUGGESTED)
            .setId(idGen++)
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
            .setCategoryIdForTests(categoryIdNoGood, Offer.BindingKind.SUGGESTED)
            .setSuggestSkuMapping(OfferTestUtils.mapping(2))
            .setSupplierSkuMapping(null);

        Offer noMappingNoGoodContent = newOffer(supplier, Offer.ProcessingStatus.OPEN, Offer.BindingKind.SUGGESTED)
            .setId(idGen++)
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
            .setSupplierSkuMapping(null)
            .setSuggestSkuMapping(null)
            .setCategoryIdForTests(categoryIdNoGood, Offer.BindingKind.SUGGESTED);

        Offer noMappingGoodContent = newOffer(supplier, Offer.ProcessingStatus.OPEN, Offer.BindingKind.SUGGESTED)
            .setId(idGen++)
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
            .setSupplierSkuMapping(null)
            .setSuggestSkuMapping(null)
            .setCategoryIdForTests(categoryIdGood, Offer.BindingKind.SUGGESTED);

        Offer noMappingNoModelNoGoodContent = newOffer(supplier, Offer.ProcessingStatus.OPEN,
            Offer.BindingKind.SUGGESTED)
            .setId(idGen++)
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
            .setModelId(null)
            .setSupplierSkuMapping(null)
            .setSuggestSkuMapping(null)
            .setCategoryIdForTests(categoryIdNoGood, Offer.BindingKind.SUGGESTED);

        Offer noMappingNoModelGoodContent = newOffer(supplier, Offer.ProcessingStatus.OPEN, Offer.BindingKind.SUGGESTED)
            .setId(idGen++)
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
            .setModelId(null)
            .setSupplierSkuMapping(null)
            .setSuggestSkuMapping(null)
            .setCategoryIdForTests(categoryIdGood, Offer.BindingKind.SUGGESTED);

        Offer whiteSkutchedOfferGoodContent = newOffer(whiteSupplier, Offer.ProcessingStatus.OPEN,
            Offer.BindingKind.SUGGESTED)
            .setId(idGen++)
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
            .setSupplierSkuMapping(null)
            .setSuggestSkuMapping(OfferTestUtils.mapping(123L, Offer.SkuType.MARKET))
            .setCategoryIdForTests(categoryIdGood, Offer.BindingKind.SUGGESTED);

        Offer whiteMatchedOfferGoodContent = newOffer(whiteSupplier, Offer.ProcessingStatus.OPEN,
            Offer.BindingKind.SUGGESTED)
            .setId(idGen++)
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
            .setSupplierSkuMapping(null)
            .setSuggestSkuMapping(null)
            .setModelId(11L)
            .setCategoryIdForTests(categoryIdGood, Offer.BindingKind.SUGGESTED);

        Offer noKnowledge = newOffer(supplier, Offer.ProcessingStatus.OPEN, Offer.BindingKind.SUGGESTED)
            .setId(idGen)
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
            .setOfferDestination(Offer.MappingDestination.DSBS)
            .setSupplierSkuMapping(null)
            .setCategoryIdForTests(categoryIdNoGoodNoKnowledge, Offer.BindingKind.SUGGESTED);

        CategoryInfo categoryInfo = new CategoryInfo(categoryIdNoGood);
        categoryInfo.setManualAcceptance(false);

        CategoryInfo categoryInfoGoodContent = new CategoryInfo(categoryIdGood);
        categoryInfoGoodContent.setManualAcceptance(false);

        categoryInfoRepository.insertOrUpdate(categoryInfo);
        categoryInfoRepository.insertOrUpdate(categoryInfoGoodContent);

        offersProcessingStatusService.processOffers(List.of(
            withSupplierMapping, withSuggestMapping, noMappingNoGoodContent, noMappingGoodContent,
            noMappingNoModelNoGoodContent, noMappingNoModelGoodContent, noKnowledge,
            whiteSkutchedOfferGoodContent, whiteMatchedOfferGoodContent
        ));

        assertThat(withSupplierMapping.getProcessingStatus()).isEqualTo(Offer.ProcessingStatus.IN_CLASSIFICATION);
        assertThat(withSuggestMapping.getProcessingStatus()).isEqualTo(Offer.ProcessingStatus.IN_CLASSIFICATION);
        assertThat(noMappingNoGoodContent.getProcessingStatus()).isEqualTo(Offer.ProcessingStatus.IN_CLASSIFICATION);
        assertThat(noMappingGoodContent.getProcessingStatus()).isEqualTo(Offer.ProcessingStatus.IN_CLASSIFICATION);
        assertThat(noMappingNoModelNoGoodContent.getProcessingStatus())
            .isEqualTo(Offer.ProcessingStatus.IN_CLASSIFICATION);
        assertThat(noMappingNoModelGoodContent.getProcessingStatus())
            .isEqualTo(Offer.ProcessingStatus.IN_CLASSIFICATION);
        assertThat(whiteSkutchedOfferGoodContent.getProcessingStatus()).isEqualTo(Offer.ProcessingStatus.OPEN);
        assertThat(whiteMatchedOfferGoodContent.getProcessingStatus()).isEqualTo(Offer.ProcessingStatus.OPEN);
        assertThat(noKnowledge.getProcessingStatus()).isEqualTo(Offer.ProcessingStatus.NO_KNOWLEDGE);
    }

    @Test
    public void approvedInNewPipeline() {
        long categoryIdNoGood = CATEGORY_ID;
        long categoryIdNoGoodNoKnowledge = CATEGORY_ID + 1;
        long categoryIdGood = CATEGORY_ID + 2;
        categoryCachingServiceMock.addCategories(
            new Category()
                .setCategoryId(categoryIdNoGood)
                .setHasKnowledge(true)
                .setAcceptGoodContent(false),
            new Category()
                .setCategoryId(categoryIdNoGoodNoKnowledge)
                .setHasKnowledge(false)
                .setAcceptGoodContent(false),
            new Category()
                .setCategoryId(categoryIdGood)
                .setHasKnowledge(true)
                .setAcceptGoodContent(true)
        );

        categoryKnowledgeService.addCategory(categoryIdNoGood);
        categoryKnowledgeService.removeCategory(categoryIdNoGoodNoKnowledge);
        categoryKnowledgeService.addCategory(categoryIdGood);

        long idGen = 0;

        Offer withMappingAccepted = newOffer(supplier, Offer.ProcessingStatus.OPEN, Offer.BindingKind.SUGGESTED)
            .setId(idGen++)
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
            .setCategoryIdForTests(categoryIdNoGood, Offer.BindingKind.SUGGESTED)
            .setSupplierSkuMapping(OfferTestUtils.mapping(2));

        Offer withMappingNotAccepted = newOffer(supplier, Offer.ProcessingStatus.OPEN, Offer.BindingKind.SUGGESTED)
            .setId(idGen++)
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.NEW)
            .setCategoryIdForTests(categoryIdNoGood, Offer.BindingKind.SUGGESTED)
            .setSupplierSkuMapping(OfferTestUtils.mapping(2));

        Offer noMappingNoGoodContent = newOffer(supplier, Offer.ProcessingStatus.OPEN, Offer.BindingKind.SUGGESTED)
            .setId(idGen++)
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
            .setSupplierSkuMapping(null)
            .setSuggestSkuMapping(null)
            .setCategoryIdForTests(categoryIdNoGood, Offer.BindingKind.SUGGESTED);

        Offer noMappingGoodContent = newOffer(supplier, Offer.ProcessingStatus.OPEN, Offer.BindingKind.SUGGESTED)
            .setId(idGen++)
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
            .setSupplierSkuMapping(null)
            .setSuggestSkuMapping(null)
            .setCategoryIdForTests(categoryIdGood, Offer.BindingKind.SUGGESTED);

        Offer noMappingNoModelNoGoodContent = newOffer(supplier, Offer.ProcessingStatus.OPEN,
            Offer.BindingKind.SUGGESTED)
            .setId(idGen++)
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
            .setModelId(null)
            .setSupplierSkuMapping(null)
            .setSuggestSkuMapping(null)
            .setCategoryIdForTests(categoryIdNoGood, Offer.BindingKind.SUGGESTED);

        Offer noMappingNoModelGoodContent = newOffer(supplier, Offer.ProcessingStatus.OPEN, Offer.BindingKind.SUGGESTED)
            .setId(idGen++)
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
            .setModelId(null)
            .setSupplierSkuMapping(null)
            .setSuggestSkuMapping(null)
            .setCategoryIdForTests(categoryIdGood, Offer.BindingKind.SUGGESTED);

        Offer whiteSkutchedOfferGoodContent = newOffer(whiteSupplier, Offer.ProcessingStatus.OPEN,
            Offer.BindingKind.SUGGESTED)
            .setId(idGen++)
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
            .setSupplierSkuMapping(null)
            .setSuggestSkuMapping(OfferTestUtils.mapping(123L, Offer.SkuType.MARKET))
            .setCategoryIdForTests(categoryIdGood, Offer.BindingKind.SUGGESTED);

        Offer whiteMatchedOfferGoodContent = newOffer(whiteSupplier, Offer.ProcessingStatus.OPEN,
            Offer.BindingKind.SUGGESTED)
            .setId(idGen++)
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
            .setSupplierSkuMapping(null)
            .setSuggestSkuMapping(null)
            .setModelId(11L)
            .setCategoryIdForTests(categoryIdGood, Offer.BindingKind.SUGGESTED);

        Offer freezeGoodContentOffer = newOffer(whiteSupplier, Offer.ProcessingStatus.OPEN,
            Offer.BindingKind.SUGGESTED)
            .setId(idGen++)
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
            .setSupplierSkuMapping(null)
            .setSuggestSkuMapping(null)
            .setDsbsAssortmentStatus(Offer.DsbsAssortmentStatus.FROZEN)
            .setModelId(11L)
            .setCategoryIdForTests(categoryIdGood, Offer.BindingKind.SUGGESTED);

        Offer noKnowledge = newOffer(supplier, Offer.ProcessingStatus.OPEN, Offer.BindingKind.SUGGESTED)
            .setId(idGen)
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
            .setOfferDestination(Offer.MappingDestination.DSBS)
            .setSupplierSkuMapping(null)
            .setCategoryIdForTests(categoryIdNoGoodNoKnowledge, Offer.BindingKind.SUGGESTED);

        CategoryInfo categoryInfo = new CategoryInfo(CATEGORY_ID);
        categoryInfo.setManualAcceptance(false);

        CategoryInfo categoryInfoGoodContent = new CategoryInfo(categoryIdGood);
        categoryInfoGoodContent.setManualAcceptance(false);

        categoryInfoRepository.insertOrUpdate(categoryInfo);
        categoryInfoRepository.insertOrUpdate(categoryInfoGoodContent);

        offersProcessingStatusService.processOffers(Arrays.asList(
            withMappingAccepted, withMappingNotAccepted,
            noMappingNoGoodContent, noMappingGoodContent,
            noMappingNoModelNoGoodContent, noMappingNoModelGoodContent,
            whiteSkutchedOfferGoodContent, whiteMatchedOfferGoodContent,
            noKnowledge
        ));

        assertThat(Arrays.asList(
            withMappingAccepted,
            noMappingNoGoodContent, noMappingGoodContent,
            whiteSkutchedOfferGoodContent, whiteMatchedOfferGoodContent,
            noKnowledge)
        )
            .extracting(Offer::getAcceptanceStatus)
            .containsOnly(Offer.AcceptanceStatus.OK);
        assertThat(withMappingNotAccepted.getAcceptanceStatus()).isEqualTo(Offer.AcceptanceStatus.NEW);

        assertThat(withMappingAccepted.getProcessingStatus()).isEqualTo(Offer.ProcessingStatus.IN_CLASSIFICATION);
        assertThat(withMappingNotAccepted.getProcessingStatus()).isEqualTo(Offer.ProcessingStatus.OPEN);
        assertThat(noMappingNoGoodContent.getProcessingStatus()).isEqualTo(Offer.ProcessingStatus.IN_CLASSIFICATION);
        assertThat(noMappingGoodContent.getProcessingStatus()).isEqualTo(Offer.ProcessingStatus.IN_CLASSIFICATION);
        assertThat(noMappingNoModelNoGoodContent.getProcessingStatus())
            .isEqualTo(Offer.ProcessingStatus.IN_CLASSIFICATION);
        assertThat(noMappingNoModelGoodContent.getProcessingStatus()).isEqualTo(Offer.ProcessingStatus.IN_CLASSIFICATION);
        assertThat(whiteSkutchedOfferGoodContent.getProcessingStatus()).isEqualTo(Offer.ProcessingStatus.OPEN);
        assertThat(whiteMatchedOfferGoodContent.getProcessingStatus()).isEqualTo(Offer.ProcessingStatus.OPEN);
        assertThat(noKnowledge.getProcessingStatus()).isEqualTo(Offer.ProcessingStatus.NO_KNOWLEDGE);
        assertThat(freezeGoodContentOffer.getProcessingStatus()).isEqualTo(Offer.ProcessingStatus.OPEN);
    }

    @Test
    public void approvedInRegularPipeline() {
        supplier.setNewContentPipeline(false);
        supplierRepository.update(supplier);

        Offer withMappingAccepted = newOffer(supplier, Offer.ProcessingStatus.OPEN, Offer.BindingKind.APPROVED)
            .setId(1)
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
            .setSupplierSkuMapping(OfferTestUtils.mapping(2));

        CategoryInfo categoryInfo = categoryInfoRepository.findById(CATEGORY_ID);
        categoryInfo.setManualAcceptance(true);
        categoryInfoRepository.update(categoryInfo);

        Category category = categoryCachingServiceMock.getCategoryOrThrow(CATEGORY_ID);
        category.setAcceptGoodContent(false);
        categoryCachingServiceMock.addCategory(category);

        offersProcessingStatusService.processOffers(Collections.singletonList(withMappingAccepted));

        assertThat(withMappingAccepted.getProcessingStatus()).isEqualTo(Offer.ProcessingStatus.IN_PROCESS);
    }

    @Test
    public void ignoreProcessFrozenOffer() {
        supplier.setNewContentPipeline(true);
        supplierRepository.update(supplier);

        Offer withMappingAccepted = newOffer(supplier, Offer.ProcessingStatus.OPEN, Offer.BindingKind.APPROVED)
            .setId(1)
            .setDsbsAssortmentStatus(Offer.DsbsAssortmentStatus.FROZEN)
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
            .setSupplierSkuMapping(OfferTestUtils.mapping(2));

        CategoryInfo categoryInfo = categoryInfoRepository.findById(CATEGORY_ID);
        categoryInfo.setManualAcceptance(true);
        categoryInfoRepository.update(categoryInfo);

        Category category = categoryCachingServiceMock.getCategoryOrThrow(CATEGORY_ID);
        category.setAcceptGoodContent(false);
        categoryCachingServiceMock.addCategory(category);

        offersProcessingStatusService.processOffers(Collections.singletonList(withMappingAccepted));

        assertThat(withMappingAccepted.getProcessingStatus()).isEqualTo(Offer.ProcessingStatus.OPEN);
    }

    @Test
    public void acceptAndApproveInFMCGPipeline() {
        Offer offer = newOffer(fmcgSupplier, Offer.ProcessingStatus.OPEN, Offer.BindingKind.SUGGESTED)
            .setBusinessId(fmcgSupplier.getId())
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK);

        offersProcessingStatusService.processOffers(Collections.singletonList(offer));

        assertThat(offer.getProcessingStatus()).isEqualTo(Offer.ProcessingStatus.NEED_MAPPING);
    }

    @Test
    public void testUpdateProcessStatistics() {
        OffersProcessingStatusService.ProcessStatistics emptyStat =
            new OffersProcessingStatusService.ProcessStatistics();

        OffersProcessingStatusService.ProcessStatistics randomStat =
            random.nextObject(OffersProcessingStatusService.ProcessStatistics.class);

        emptyStat.updateFrom(randomStat);

        assertThat(emptyStat)
            .usingRecursiveComparison()
            .isEqualTo(randomStat);
    }

    @Test
    public void testNotGoodOfferInContentProcessingWithPskuMappingStaysInContentProcessing() {
        var notGoodCategory = new Category()
            .setCategoryId(CATEGORY_ID)
            .setHasKnowledge(true)
            .setAcceptGoodContent(false);
        categoryCachingServiceMock.addCategories(notGoodCategory);

        Offer offer = newOffer(supplier, Offer.ProcessingStatus.CONTENT_PROCESSING, Offer.BindingKind.APPROVED)
            .setCategoryIdForTests(notGoodCategory.getCategoryId(), Offer.BindingKind.APPROVED)
            .setBusinessId(supplier.getId())
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK);

        offersProcessingStatusService.processOffers(Collections.singletonList(offer));

        assertThat(offer.getProcessingStatus()).isEqualTo(Offer.ProcessingStatus.CONTENT_PROCESSING);
    }

    @Test
    public void testOfferInContentProcessingWithoutMappingStaysInContentProcessing() {
        Offer offer = newOffer(supplier, Offer.ProcessingStatus.CONTENT_PROCESSING, Offer.BindingKind.APPROVED)
            .setBusinessId(supplier.getId())
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK);

        offersProcessingStatusService.processOffers(Collections.singletonList(offer));

        assertThat(offer.getProcessingStatus()).isEqualTo(Offer.ProcessingStatus.CONTENT_PROCESSING);
    }

    @Test
    public void testOfferInContentProcessingWithPSKUMappingStaysInContentProcessing() {
        Offer offer = newOffer(supplier, Offer.ProcessingStatus.CONTENT_PROCESSING, Offer.BindingKind.APPROVED)
            .setBusinessId(supplier.getId())
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
            .updateApprovedSkuMapping(OfferTestUtils.mapping(100L, Offer.SkuType.PARTNER20), PARTNER_SELF)
            .setContentProcessed(false);

        offersProcessingStatusService.processOffers(Collections.singletonList(offer));

        assertThat(offer.getProcessingStatus()).isEqualTo(Offer.ProcessingStatus.CONTENT_PROCESSING);
    }

    @Test
    public void testOfferInContentProcessingIsNotSentToMM() {
        Offer offer = newOffer(supplier, Offer.ProcessingStatus.CONTENT_PROCESSING, Offer.BindingKind.APPROVED)
            .setBusinessId(supplier.getId())
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
            .setSuggestSkuMapping(OfferTestUtils.mapping(100L, Offer.SkuType.PARTNER20));

        offersProcessingStatusService.processOffers(Collections.singletonList(offer));

        assertThat(offer.getProcessingStatus()).isEqualTo(Offer.ProcessingStatus.CONTENT_PROCESSING);
    }

    @Test
    public void testNotAcceptedOfferInProcessedContentProcessingSentToNeedContentChangesStatus() {
        Offer offer = newOffer(supplier, Offer.ProcessingStatus.CONTENT_PROCESSING, Offer.BindingKind.APPROVED)
            .setBusinessId(supplier.getId())
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.TRASH)
            .setContentProcessed(true);

        offersProcessingStatusService.processOffers(Collections.singletonList(offer));

        assertThat(offer.getProcessingStatus()).isEqualTo(Offer.ProcessingStatus.OPEN);
    }

    @Test
    public void testNotAcceptedMappedOfferInProcessedContentProcessingSentToNeedContentChangesStatus() {
        Offer offer = newOffer(supplier, Offer.ProcessingStatus.CONTENT_PROCESSING, Offer.BindingKind.APPROVED)
            .setBusinessId(supplier.getId())
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.TRASH)
            .updateApprovedSkuMapping(OfferTestUtils.mapping(100L, Offer.SkuType.PARTNER20), PARTNER_SELF)
            .setContentProcessed(true);

        offersProcessingStatusService.processOffers(Collections.singletonList(offer));

        assertThat(offer.getProcessingStatus()).isEqualTo(Offer.ProcessingStatus.OPEN);
    }

    @Test
    public void testResetOffer() {
        var offerInModeration = newOffer(Offer.ProcessingStatus.IN_MODERATION, Offer.BindingKind.APPROVED)
            .setTrackerTicket("MCP-123")
            .setProcessingTicketId(123)
            .setTicketCritical(true)
            .setTicketDeadline(LocalDate.now())
            .setThroughContentLab(true)
            .updateContentLabState(Offer.ContentLabState.CL_CONTENT)
            .setSupplierSkuMapping(OfferTestUtils.mapping(15L, Offer.SkuType.MARKET))
            .setSupplierSkuMappingStatus(Offer.MappingStatus.NEW);
        var offerInNeedInfo = newOffer(Offer.ProcessingStatus.NEED_INFO, Offer.BindingKind.APPROVED)
            .setCommentModifiedBy("someLogin")
            .setContentComment("what ho")
            .setCommentFromClab("hello clab")
            .setCommentsFromClab(List.of(new ContentComment(ContentCommentType.NEED_INFORMATION)))
            .setCommentFromClabModifiedBy("someClabLogin")
            .setContentComments(new ContentComment(ContentCommentType.NEED_INFORMATION));
        var offerWithMapping = newOffer(Offer.ProcessingStatus.PROCESSED, Offer.BindingKind.APPROVED)
            .setContentSkuMapping(OfferTestUtils.mapping(15L, Offer.SkuType.MARKET))
            .updateApprovedSkuMapping(OfferTestUtils.mapping(15L, Offer.SkuType.MARKET), CONTENT);
        var offerWithDeletedMapping = newOffer(Offer.ProcessingStatus.PROCESSED, Offer.BindingKind.APPROVED)
            .setContentSkuMapping(OfferTestUtils.mapping(0, Offer.SkuType.MARKET))
            .updateApprovedSkuMapping(OfferTestUtils.mapping(0, Offer.SkuType.MARKET));

        offerInModeration = offerRepository.insertAndGetOffer(offerInModeration);
        offerInNeedInfo = offerRepository.insertAndGetOffer(offerInNeedInfo);
        offerWithMapping = offerRepository.insertAndGetOffer(offerWithMapping);
        offerWithDeletedMapping = offerRepository.insertAndGetOffer(offerWithDeletedMapping);

        var offers = offerRepository.findOffers(new OffersFilter().setOfferIds(
            offerInModeration.getId(),
            offerInNeedInfo.getId(),
            offerWithMapping.getId(),
            offerWithDeletedMapping.getId()
        ));

        var resetOffers = offersProcessingStatusService.reset(offers);

        var resetOffersById = resetOffers.stream()
            .collect(Collectors.toMap(Offer::getId, Function.identity()));

        assertThat(resetOffersById.keySet())
            .containsExactlyInAnyOrder(
                offerInModeration.getId(),
                offerInNeedInfo.getId(),
                offerWithMapping.getId(),
                offerWithDeletedMapping.getId()
            );

        MbocAssertions.assertThat(resetOffersById.get(offerInModeration.getId()))
            .isNotNull()
            .isEqualToIgnoreContent(offerInModeration
                .updateProcessingStatusIfValid(Offer.ProcessingStatus.OPEN)
                .clearTrackerTicket()
                .setProcessingTicketId(null)
                .setTicketCritical(false)
                .setTicketDeadline(null)
                .setThroughContentLab(false)
            );
        MbocAssertions.assertThat(resetOffersById.get(offerInNeedInfo.getId()))
            .isNotNull()
            .isEqualToIgnoreContent(offerInNeedInfo
                .updateProcessingStatusIfValid(Offer.ProcessingStatus.OPEN)
                .setTicketCritical(false)
                .setCommentModifiedBy(null)
                .setContentComment(null)
                .setCommentFromClab(null)
                .setCommentsFromClab(List.of())
                .setCommentFromClabModifiedBy(null)
                .setContentComments(List.of())
            );
        MbocAssertions.assertThat(resetOffersById.get(offerWithMapping.getId()))
            .isNotNull()
            .isEqualToIgnoreContent(offerWithMapping
                .updateProcessingStatusIfValid(Offer.ProcessingStatus.OPEN)
                .setTicketCritical(false)
            );
        MbocAssertions.assertThat(resetOffersById.get(offerWithDeletedMapping.getId()))
            .isNotNull()
            .isEqualToIgnoreContent(offerWithDeletedMapping
                .updateProcessingStatusIfValid(Offer.ProcessingStatus.OPEN)
                .setTicketCritical(false)
                .setApprovedSkuMappingConfidence(RESET)
                .setContentSkuMapping(null)
            );

        offersProcessingStatusService.processOffers(resetOffersById.values());

        MbocAssertions.assertThat(resetOffersById.get(offerInModeration.getId()))
            .isNotNull()
            .isEqualToIgnoreContent(offerInModeration
                .updateProcessingStatusIfValid(Offer.ProcessingStatus.IN_MODERATION)
                .setProcessingCounter(1)
            );
        MbocAssertions.assertThat(resetOffersById.get(offerInNeedInfo.getId()))
            .isNotNull()
            .isEqualToIgnoreContent(offerInNeedInfo
                .updateProcessingStatusIfValid(Offer.ProcessingStatus.NEED_CONTENT)
                .updateContentProcessingStatus(Offer.ContentProcessingStatus.NEED_CONTENT)
            );
        MbocAssertions.assertThat(resetOffersById.get(offerWithMapping.getId()))
            .isNotNull()
            .isEqualToIgnoreContent(offerWithMapping
                .updateProcessingStatusIfValid(Offer.ProcessingStatus.PROCESSED)
            );
        MbocAssertions.assertThat(resetOffersById.get(offerWithDeletedMapping.getId()))
            .isNotNull()
            .isEqualToIgnoreContent(offerWithDeletedMapping
                .updateProcessingStatusIfValid(Offer.ProcessingStatus.NEED_CONTENT)
                .updateContentProcessingStatus(Offer.ContentProcessingStatus.NEED_CONTENT)
            );

    }

    @Test
    public void autoClassifiedBlueOffer() {
        var categoryId = CATEGORY_ID + 100;

        var category = new Category().setCategoryId(categoryId).setLeaf(false).setHasKnowledge(false);
        categoryCachingServiceMock.addCategory(category);

        var categoryInfo = new CategoryInfo().setCategoryId(categoryId);
        categoryInfoRepository.insert(categoryInfo);

        var offer = newOffer(Offer.ProcessingStatus.OPEN, Offer.BindingKind.SUGGESTED)
            .setOfferDestination(Offer.MappingDestination.BLUE)
            .setCategoryIdForTests(categoryId, Offer.BindingKind.SUGGESTED)
            .setMappedCategoryId(categoryId, PARTNER);
        offerRepository.insertOffer(offer);

        offer = offerRepository.getOfferById(offer.getId());

        offersProcessingStatusService.processOffers(List.of(offer));

        assertThat(offer.getProcessingStatus()).isEqualTo(Offer.ProcessingStatus.IN_CLASSIFICATION);
    }

    @Test
    public void blueOfferInNonLeafCategoryAndNoKnowledgeComment() {
        var categoryId = CATEGORY_ID + 100;

        var category = new Category().setCategoryId(categoryId).setLeaf(false).setHasKnowledge(false);
        categoryCachingServiceMock.addCategory(category);

        var categoryInfo = new CategoryInfo().setCategoryId(categoryId);
        categoryInfoRepository.insert(categoryInfo);

        var offer = newOffer(Offer.ProcessingStatus.OPEN, Offer.BindingKind.SUGGESTED)
            .setOfferDestination(Offer.MappingDestination.BLUE)
            .setContentComments(new ContentComment(ContentCommentType.NO_KNOWLEDGE, "no knowledge"))
            .setCategoryIdForTests(categoryId, Offer.BindingKind.SUGGESTED);

        offerRepository.insertOffer(offer);

        offer = offerRepository.getOfferById(offer.getId());

        offersProcessingStatusService.processOffers(List.of(offer));

        assertThat(offer.getProcessingStatus()).isEqualTo(Offer.ProcessingStatus.NO_KNOWLEDGE);
    }

    @Test
    public void manuallyClassifiedBlueOffer() {
        var categoryId = CATEGORY_ID + 100;

        var category = new Category().setCategoryId(categoryId).setLeaf(false).setHasKnowledge(false);
        categoryCachingServiceMock.addCategory(category);

        var categoryInfo = new CategoryInfo().setCategoryId(categoryId);
        categoryInfoRepository.insert(categoryInfo);

        var offer = newOffer(Offer.ProcessingStatus.OPEN, Offer.BindingKind.APPROVED)
            .setOfferDestination(Offer.MappingDestination.BLUE)
            .setCategoryIdForTests(categoryId, Offer.BindingKind.APPROVED)
            .setMappedCategoryId(categoryId, CONTENT);

        offerRepository.insertOffer(offer);

        offer = offerRepository.getOfferById(offer.getId());

        offersProcessingStatusService.processOffers(List.of(offer));

        assertThat(offer.getProcessingStatus()).isEqualTo(Offer.ProcessingStatus.NO_KNOWLEDGE);
    }

    @Test
    public void testNotForModerationLeavesModeration() {
        categoryCachingServiceMock.setCategoryAcceptGoodContent(CATEGORY_ID, false);

        var offer = createTestOfferForModeration(1L)
            .setSuggestSkuMapping(null)
            .setSupplierSkuMapping(null)
            .updateProcessingStatusIfValid(Offer.ProcessingStatus.IN_MODERATION);

        offersProcessingStatusService.processOffers(List.of(offer));

        assertThat(offer.getProcessingStatus()).isNotEqualTo(Offer.ProcessingStatus.IN_MODERATION);
    }

    @Test
    public void testNotForModerationGoodLeavesModeration() {
        categoryCachingServiceMock.setCategoryAcceptGoodContent(CATEGORY_ID, true);

        var offer = createTestOfferForModeration(1L)
            .setSuggestSkuMapping(null)
            .setSupplierSkuMapping(null)
            .updateProcessingStatusIfValid(Offer.ProcessingStatus.IN_MODERATION);

        offersProcessingStatusService.processOffers(List.of(offer));

        assertThat(offer.getProcessingStatus()).isNotEqualTo(Offer.ProcessingStatus.IN_MODERATION);
    }

    @Test
    public void checkCurrentCalculationMatchesCommitted() throws IOException {
        DescribeExprHandler.Node node = offersProcessingStatusService.describeStatusCalculation();

        // История изменения пайплайнов. Когда падает - надо чекнуть и просто вкоммитеть дифф тоже.
        // зачем: можно будет в текстовом виде видеть диффы того, как обрабатываются оферы.
        assertThat(normalize(
            new String(getClass().getClassLoader()
                .getResourceAsStream("OfferProcessingStatusService/current-offer-pipeline.txt")
                .readAllBytes(),
                StandardCharsets.UTF_8)))
            .isEqualTo(normalize(node.toString()));
    }

    @Test
    public void shouldChangeProcessingStatusToRecheckMapping() {
        var offerNeedRecheck = newOffer(Offer.ProcessingStatus.PROCESSED, Offer.BindingKind.APPROVED)
            .setContentSkuMapping(OfferTestUtils.mapping(15L, Offer.SkuType.MARKET))
            .updateApprovedSkuMapping(OfferTestUtils.mapping(15L, Offer.SkuType.MARKET), CONTENT)
            .setRecheckSkuMapping(OfferTestUtils.mapping(15L, Offer.SkuType.MARKET))
            .setRecheckMappingStatus(Offer.RecheckMappingStatus.NEED_RECHECK);

        var offerRecheckConfirmed = newOffer(Offer.ProcessingStatus.PROCESSED, Offer.BindingKind.APPROVED)
            .setContentSkuMapping(OfferTestUtils.mapping(15L, Offer.SkuType.MARKET))
            .updateApprovedSkuMapping(OfferTestUtils.mapping(15L, Offer.SkuType.MARKET), CONTENT)
            .setRecheckMappingStatus(Offer.RecheckMappingStatus.MAPPING_CONFIRMED);

        var offerNoNeedRecheck = newOffer(Offer.ProcessingStatus.PROCESSED, Offer.BindingKind.APPROVED)
            .setContentSkuMapping(OfferTestUtils.mapping(15L, Offer.SkuType.MARKET))
            .updateApprovedSkuMapping(OfferTestUtils.mapping(15L, Offer.SkuType.MARKET), CONTENT);

        offersProcessingStatusService.processOffers(
            List.of(offerNeedRecheck, offerRecheckConfirmed, offerNoNeedRecheck)
        );

        assertThat(offerNeedRecheck.getProcessingStatus())
            .isEqualTo(Offer.ProcessingStatus.IN_RECHECK_MODERATION);
        assertThat(offerRecheckConfirmed.getProcessingStatus())
            .isNotEqualTo(Offer.ProcessingStatus.IN_RECHECK_MODERATION);
        assertThat(offerNoNeedRecheck.getProcessingStatus())
            .isNotEqualTo(Offer.ProcessingStatus.IN_RECHECK_MODERATION);
    }

    @Test
    public void shouldChangeProcessingStatusToRecheckClassification() {
        var offerNeedRecheck = newOffer(Offer.ProcessingStatus.PROCESSED, Offer.BindingKind.APPROVED)
            .setRecheckCategoryId(1L)
            .updateApprovedSkuMapping(OfferTestUtils.mapping(15L, Offer.SkuType.MARKET), CONTENT)
            .setRecheckClassificationStatus(Offer.RecheckClassificationStatus.ON_RECHECK);

        var offerRecheckConfirmed = newOffer(Offer.ProcessingStatus.PROCESSED, Offer.BindingKind.APPROVED)
            .setRecheckCategoryId(1L)
            .updateApprovedSkuMapping(OfferTestUtils.mapping(15L, Offer.SkuType.MARKET), CONTENT)
            .setRecheckClassificationStatus(Offer.RecheckClassificationStatus.CONFIRMED);

        var offerNoNeedRecheck = newOffer(Offer.ProcessingStatus.PROCESSED, Offer.BindingKind.APPROVED)
            .updateApprovedSkuMapping(OfferTestUtils.mapping(15L, Offer.SkuType.MARKET), CONTENT);

        var offerInRecheckModeration = newOffer(Offer.ProcessingStatus.PROCESSED, Offer.BindingKind.APPROVED)
            .setContentSkuMapping(OfferTestUtils.mapping(15L, Offer.SkuType.MARKET))
            .updateApprovedSkuMapping(OfferTestUtils.mapping(15L, Offer.SkuType.MARKET), CONTENT)
            .setRecheckSkuMapping(OfferTestUtils.mapping(15L, Offer.SkuType.MARKET))
            .setRecheckMappingStatus(Offer.RecheckMappingStatus.NEED_RECHECK)
            .setRecheckCategoryId(1L)
            .setRecheckClassificationStatus(Offer.RecheckClassificationStatus.ON_RECHECK);
        var offerNeedInfo = offerNeedRecheck.copy()
            .setContentComments(new ContentComment(ContentCommentType.NEED_INFORMATION));

        offersProcessingStatusService.processOffers(
            List.of(offerNeedRecheck, offerRecheckConfirmed, offerNoNeedRecheck, offerInRecheckModeration,
                offerNeedInfo)
        );

        // status should change
        assertThat(offerNeedRecheck.getProcessingStatus())
            .isEqualTo(Offer.ProcessingStatus.IN_RECHECK_CLASSIFICATION);

        // status should not change
        assertThat(offerRecheckConfirmed.getProcessingStatus())
            .isNotEqualTo(Offer.ProcessingStatus.IN_RECHECK_CLASSIFICATION);
        assertThat(offerNoNeedRecheck.getProcessingStatus())
            .isNotEqualTo(Offer.ProcessingStatus.IN_RECHECK_CLASSIFICATION);
        assertThat(offerInRecheckModeration.getProcessingStatus())
            .isNotEqualTo(Offer.ProcessingStatus.IN_RECHECK_CLASSIFICATION);
        assertThat(offerNeedInfo.getProcessingStatus())
            .isNotEqualTo(Offer.ProcessingStatus.IN_RECHECK_CLASSIFICATION);
    }

    @Test
    public void shouldNotChangeToRecheckClassificationOnNeedInfo() {
        var offer = newOffer(Offer.ProcessingStatus.PROCESSED, Offer.BindingKind.APPROVED)
            .setRecheckCategoryId(1L)
            .updateApprovedSkuMapping(OfferTestUtils.mapping(15L, Offer.SkuType.MARKET), CONTENT)
            .setRecheckClassificationStatus(Offer.RecheckClassificationStatus.ON_RECHECK);

        offersProcessingStatusService.processOffers(List.of(offer));

        assertThat(offer.getProcessingStatus())
            .isEqualTo(Offer.ProcessingStatus.IN_RECHECK_CLASSIFICATION);
        assertThat(offer.getRecheckClassificationStatus())
            .isEqualTo(Offer.RecheckClassificationStatus.ON_RECHECK);

        // need info condition
        offer.setContentComments(new ContentComment(ContentCommentType.NEED_INFORMATION));

        offersProcessingStatusService.processOffers(List.of(offer));

        assertThat(offer.getProcessingStatus())
            .isNotEqualTo(Offer.ProcessingStatus.NEED_INFO);
        assertThat(offer.getRecheckClassificationStatus())
            .isEqualTo(Offer.RecheckClassificationStatus.ON_RECHECK);

        // remove need info condition
        offer.setContentComments(List.of());
        offersProcessingStatusService.processOffers(List.of(offer));

        assertThat(offer.getProcessingStatus())
            .isEqualTo(Offer.ProcessingStatus.IN_RECHECK_CLASSIFICATION);
        assertThat(offer.getRecheckClassificationStatus())
            .isEqualTo(Offer.RecheckClassificationStatus.ON_RECHECK);
    }

    @Test
    public void testFashionWithoutGroupIdShouldNotGoToClassification() {
        var categoryId = CATEGORY_ID + 2 * 2 * 8 + 6 * 6 - 6;
        categoryCachingServiceMock.addCategory(new Category()
            .setCategoryId(categoryId)
            .setLeaf(false)
            .setHasKnowledge(false)
        );
        categoryInfoRepository.insert(new CategoryInfo()
            .setCategoryId(categoryId)
            .setTags(List.of(CategoryInfo.CategoryTag.FASHION, CategoryInfo.CategoryTag.SIZED_FASHION))
        );
        Offer offerToInsert = newOffer(Offer.ProcessingStatus.CONTENT_PROCESSING, Offer.BindingKind.SUGGESTED)
            .setOfferDestination(Offer.MappingDestination.BLUE)
            .setCategoryIdForTests(categoryId, Offer.BindingKind.SUGGESTED)
            .setMappedCategoryId(categoryId, PARTNER)
            .setGroupId(null);
        offerRepository.insertOffer(offerToInsert);
        Offer offer = offerRepository.getOfferById(offerToInsert.getId());
        offersProcessingStatusService.processOffers(List.of(offer));
        Offer.ProcessingStatus processingStatus = offer.getProcessingStatus();
        assertThat(processingStatus).isNotEqualTo(Offer.ProcessingStatus.IN_CLASSIFICATION);
        assertThat(processingStatus).isEqualTo(Offer.ProcessingStatus.CONTENT_PROCESSING);
    }

    @Test
    public void shouldNotChangeToRecheckClassificationOnForbiddenStatus() {
        var offerInAllowedStatus = newOffer(Offer.ProcessingStatus.AUTO_PROCESSED, Offer.BindingKind.APPROVED)
            .setRecheckCategoryId(1L)
            .updateApprovedSkuMapping(OfferTestUtils.mapping(15L, Offer.SkuType.MARKET), CONTENT)
            .setRecheckClassificationStatus(Offer.RecheckClassificationStatus.ON_RECHECK);
        var readyForRecheckOffer = offerInAllowedStatus.copy();

        offersProcessingStatusService.processOffers(List.of(offerInAllowedStatus));

        assertThat(offerInAllowedStatus)
            .extracting(Offer::getProcessingStatus)
            .isEqualTo(Offer.ProcessingStatus.IN_RECHECK_CLASSIFICATION);

        var forbiddenToChangeStatusOffers = OffersProcessingStatusService.FORBID_TO_DISCARD_STATUSES.stream()
            .map(processingStatus -> readyForRecheckOffer.copy().updateProcessingStatusIfValid(processingStatus))
            .collect(Collectors.toList());

        offersProcessingStatusService.processOffers(forbiddenToChangeStatusOffers);

        assertThat(forbiddenToChangeStatusOffers)
            .noneMatch(postProcessOffer -> postProcessOffer.getProcessingStatus()
                .equals(Offer.ProcessingStatus.IN_RECHECK_CLASSIFICATION));
    }
}
