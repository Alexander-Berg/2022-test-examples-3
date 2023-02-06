package ru.yandex.market.mboc.app.proto;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.mboc.app.proto.mappings.AbstractMboMappingsServiceImplTest;
import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.offers.model.OfferContent;
import ru.yandex.market.mboc.common.offers.repository.search.OffersFilter;
import ru.yandex.market.mboc.common.utils.OfferTestUtils;
import ru.yandex.market.mboc.http.MboMappings;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static ru.yandex.market.mboc.common.offers.model.Offer.RecheckMappingStatus.MAPPING_CONFIRMED;
import static ru.yandex.market.mboc.common.offers.model.Offer.RecheckMappingStatus.NEED_RECHECK;
import static ru.yandex.market.mboc.common.offers.model.Offer.RecheckMappingStatus.ON_RECHECK;
import static ru.yandex.market.mboc.http.MboMappings.Answer.Result.Status.ERROR;
import static ru.yandex.market.mboc.http.MboMappings.Answer.Result.Status.OK;
import static ru.yandex.market.mboc.http.MboMappings.OfferAndMappingIds;
import static ru.yandex.market.mboc.http.MboMappings.SendToRecheckMappingRequest;
import static ru.yandex.misc.test.Assert.assertContains;

public class MboMappingsServiceImplSendToRecheckTest extends AbstractMboMappingsServiceImplTest {
    private static final short count = 100;
    private static final int offerId = 123556;
    private static final int mappingId = 200;

    @Before
    public void setUp() {
        offerRepository.deleteAllInTest();
    }

    @Test
    public void ok() {
        List<Offer> offers = new ArrayList<>();
        List<OfferAndMappingIds> offerAndMappingIdsList = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            offers.add(getOffer(offerId + i, mappingId + i, NEED_RECHECK));
            offerAndMappingIdsList.add(
                OfferAndMappingIds.newBuilder()
                    .setOfferId(offerId + i)
                    .setMappingId(mappingId + i)
                    .build());

        }

        offerRepository.insertOffers(offers);
        var recheckMappingResponse = service.sendToRecheck(getSendToRecheckMappingRequest(offerAndMappingIdsList));

        assertNotNull(recheckMappingResponse);

        var answersList = recheckMappingResponse.getAnswersList();
        assertFalse(answersList.isEmpty());
        validate(answersList, offers);
    }

    @Test
    public void wrongOfferId() {
        List<Offer> offers = new ArrayList<>();
        List<OfferAndMappingIds> offerAndMappingIdsList = new ArrayList<>();
        int halfCount = count / 2;
        for (int i = 0; i < halfCount; i++) {
            offers.add(getOffer(offerId + i, mappingId + i, ON_RECHECK));
            offerAndMappingIdsList.add(
                OfferAndMappingIds.newBuilder()
                    .setOfferId(offerId + i)
                    .setMappingId(mappingId + i)
                    .build());
        }
        for (int i = halfCount; i < count; i++) {
            offers.add(getOffer(offerId + i, mappingId + i, ON_RECHECK));
            offerAndMappingIdsList.add(
                OfferAndMappingIds.newBuilder()
                    .setOfferId(offerId + halfCount + i)
                    .setMappingId(mappingId + i)
                    .build());
        }

        offerRepository.insertOffers(offers);
        var recheckMappingResponse = service.sendToRecheck(getSendToRecheckMappingRequest(offerAndMappingIdsList));

        assertNotNull(recheckMappingResponse);

        var answersList = recheckMappingResponse.getAnswersList();
        assertFalse(answersList.isEmpty());
        for (int i = 0; i < halfCount; i++) {
            assertEquals(OK, answersList.get(i).getResult().getStatus());
        }
        for (int i = halfCount; i < count; i++) {
            assertEquals(ERROR, answersList.get(i).getResult().getStatus());
            assertContains(answersList.get(i).getResult().getMessage(), "Unable to find an offer with offer_id");
        }
    }

    @Test
    public void haveNotApprovedMapping() {
        List<Offer> offers = new ArrayList<>();
        List<OfferAndMappingIds> offerAndMappingIdsList = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            offers.add(getOffer(offerId + i, mappingId + i, ON_RECHECK).updateApprovedSkuMapping(null));
            offerAndMappingIdsList.add(
                OfferAndMappingIds.newBuilder()
                    .setOfferId(offerId + i)
                    .setMappingId(mappingId + i)
                    .build());
        }

        offerRepository.insertOffers(offers);
        var recheckMappingResponse = service.sendToRecheck(getSendToRecheckMappingRequest(offerAndMappingIdsList));

        assertNotNull(recheckMappingResponse);

        var answersList = recheckMappingResponse.getAnswersList();
        assertFalse(answersList.isEmpty());
        for (int i = 0; i < count; i++) {
            assertEquals(ERROR, answersList.get(i).getResult().getStatus());
            assertContains(answersList.get(i).getResult().getMessage(), "no approved mapping");
        }
    }

    @Test
    public void wrongMappingId() {
        List<Offer> offers = new ArrayList<>();
        List<OfferAndMappingIds> offerAndMappingIdsList = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            offers.add(getOffer(offerId + i, mappingId + i, ON_RECHECK));
            offerAndMappingIdsList.add(
                OfferAndMappingIds.newBuilder()
                    .setOfferId(offerId + i)
                    .setMappingId(mappingId + count + 1 + i)
                    .build());
        }

        offerRepository.insertOffers(offers);
        var recheckMappingResponse = service.sendToRecheck(getSendToRecheckMappingRequest(offerAndMappingIdsList));

        assertNotNull(recheckMappingResponse);

        var answersList = recheckMappingResponse.getAnswersList();
        assertFalse(answersList.isEmpty());
        for (int i = 0; i < count; i++) {
            assertEquals(ERROR, answersList.get(i).getResult().getStatus());
            assertContains(answersList.get(i).getResult().getMessage(), "approved mapping does not match");
        }
    }

    @Test
    public void haveNotRecheckMappingStatus() {
        List<Offer> offers = new ArrayList<>();
        List<OfferAndMappingIds> offerAndMappingIdsList = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            offers.add(getOffer(offerId + i, mappingId + i, ON_RECHECK).setRecheckMappingStatus(null));
            offerAndMappingIdsList.add(
                OfferAndMappingIds.newBuilder()
                    .setOfferId(offerId + i)
                    .setMappingId(mappingId + i)
                    .build());
        }

        offerRepository.insertOffers(offers);
        var recheckMappingResponse = service.sendToRecheck(getSendToRecheckMappingRequest(offerAndMappingIdsList));

        assertNotNull(recheckMappingResponse);

        var answersList = recheckMappingResponse.getAnswersList();
        assertFalse(answersList.isEmpty());
        validate(answersList, offers);
    }

    @Test
    public void mappingConfirmed() {
        List<Offer> offers = new ArrayList<>();
        List<OfferAndMappingIds> offerAndMappingIdsList = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            offers.add(getOffer(offerId + i, mappingId + i, MAPPING_CONFIRMED));
            offerAndMappingIdsList.add(
                OfferAndMappingIds.newBuilder()
                    .setOfferId(offerId + i)
                    .setMappingId(mappingId + i)
                    .build());
        }

        offerRepository.insertOffers(offers);
        var recheckMappingResponse = service.sendToRecheck(getSendToRecheckMappingRequest(offerAndMappingIdsList));

        assertNotNull(recheckMappingResponse);

        var answersList = recheckMappingResponse.getAnswersList();
        validate(answersList, offers);
    }

    private void validate(List<MboMappings.Answer> answersList, List<Offer> offers) {
        answersList.forEach(i -> assertEquals(OK, i.getResult().getStatus()));
        List<Long> offerIds = offers.stream().map(Offer::getId).collect(Collectors.toList());

        List<Offer> updatedOffers = offerRepository.findOffers(new OffersFilter().setOfferIds(offerIds));

        updatedOffers.forEach(offer -> {
            assertNull(offer.getSuggestSkuMapping());
            assertNull(offer.getSuggestSkuMappingType());
            assertNull(offer.getSuggestMappingSource());
            assertNull(offer.getModelId());
            assertNull(offer.getMarketModelName());
        });
    }

    private Offer getOffer(long offerId, long mappingId, Offer.RecheckMappingStatus recheckMappingStatus) {
        return new Offer()
            .setId(offerId)
            .setBusinessId(1)
            .setShopSku("ssku" + offerId)
            .setTitle("title: " + "ssku")
            .setShopCategoryName("category")
            .setIsOfferContentPresent(true)
            .storeOfferContent(OfferContent.initEmptyContent())
            .setVendor("offer_vendor")
            .setCategoryIdForTests(1L, Offer.BindingKind.APPROVED)
            .updateApprovedSkuMapping(OfferTestUtils.mapping(mappingId), Offer.MappingConfidence.CONTENT)
            .setRecheckMappingStatus(recheckMappingStatus)
            .setServiceOffers(List.of(new Offer.ServiceOffer(10)));
    }

    private SendToRecheckMappingRequest getSendToRecheckMappingRequest(List<OfferAndMappingIds> offerAndMappingIds) {
        return SendToRecheckMappingRequest
            .newBuilder()
            .setRecheckMappingSources(SendToRecheckMappingRequest.RecheckMappingSource.OPERATOR)
            .addAllOfferAndMappingIds(offerAndMappingIds)
            .build();
    }
}
