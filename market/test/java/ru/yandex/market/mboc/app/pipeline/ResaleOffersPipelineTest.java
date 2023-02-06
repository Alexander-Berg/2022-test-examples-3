package ru.yandex.market.mboc.app.pipeline;

import java.util.function.Consumer;
import java.util.function.Function;

import Market.DataCamp.DataCampOffer;
import Market.DataCamp.DataCampOfferMeta;
import Market.UltraControllerServiceData.UltraController;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.mboc.app.pipeline.datacamp.BaseDatacampPipelineTest;
import ru.yandex.market.mboc.common.datacamp.OfferBuilder;
import ru.yandex.market.mboc.common.dict.Supplier;
import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.offers.repository.search.OffersFilter;
import ru.yandex.market.mboc.common.services.category.models.Category;
import ru.yandex.market.mboc.common.services.category_info.info.CategoryInfo;
import ru.yandex.market.mboc.common.utils.OfferTestUtils;

public class ResaleOffersPipelineTest extends BaseDatacampPipelineTest {
    private static final long AUTO_ACCEPTED_CATEGORY_ID = OfferTestUtils.TEST_CATEGORY_INFO_ID + 8;
    private final Supplier supplier = OfferTestUtils.blueSupplierUnderBiz1();

    @Before
    public void setUpMigration() {
        Mockito.when(booksService.isBookCategory(Mockito.any(Category.class))).thenReturn(true);
        createCategory();
    }

    @Test
    public void testResaleOfferPipeline() {
        var dcOffer = resaleDataCampOffer();

        // Import offer from DataCamp and check that offer is saved to DB
        importOfferFromDC(toMessage(supplier.getId(), dcOffer)).accept(null);
        var importedOffers = offerRepository.findOffers(new OffersFilter().setBusinessIds(supplier.getBusinessId()));
        Assertions.assertThat(importedOffers.size()).isEqualTo(1);

        var offer = importedOffers.get(0);

        // Check processingStatus = IN_CLASSIFICATION and contentProcessingStatus = NONE
        // and offer is not sent to AG
        verify(Offer::getProcessingStatus, Offer.ProcessingStatus.IN_CLASSIFICATION)
            .andThen(verify(Offer::getContentProcessingStatus, Offer.ContentProcessingStatus.NONE))
            .andThen(verifyNotSentToAg())
            .accept(offer);

        // Create ticket for classification
        // Classify the offer and check processingStatus = IN_PROCESS and contentProcessingStatus = NONE
        createTickets()
            .andThen(operatorClassifiesOfferByFile(AUTO_ACCEPTED_CATEGORY_ID))
            .andThen(tmsProcessClassificationTicketsResults())
            .andThen(verify(Offer::getProcessingStatus, Offer.ProcessingStatus.IN_PROCESS))
            .andThen(verify(Offer::getContentProcessingStatus, Offer.ContentProcessingStatus.NONE))
            .andThen(verifyNotSentToAg())
            .accept(offer);

        // Create ticket for matching
        // Match the offer and check processingStatus = PROCESSED and contentProcessingStatus = NONE
        createTickets()
            .andThen(operatorMatchesOffer(MARKET_SKU_ID_1))
            .andThen(tmsProcessMatchingTicketsResults())
            .andThen(verify(Offer::getProcessingStatus, Offer.ProcessingStatus.PROCESSED))
            .andThen(verify(Offer::getContentProcessingStatus, Offer.ContentProcessingStatus.NONE))
            .accept(offer);
    }

    private <T> Consumer<Offer> verify(Function<Offer, T> actual, T expected) {
        return offer -> {
            var offerFromDb = offerRepository.getOfferById(offer.getId());
            Assertions.assertThat(actual.apply(offerFromDb)).isEqualTo(expected);
        };
    }

    private Consumer<Offer> verifyNotSentToAg() {
        return offer ->
            verifyOfferIsNotInContentProcessingQueue(offer.getBusinessId(), offer.getShopSku()).accept(null, null);
    }

    private Consumer<Offer> createTickets() {
        return offer -> tmsCreateTrackerTickets().accept(offer);
    }

    private void createCategory() {
        Category categoryNoKnowledge = new Category().setCategoryId(AUTO_ACCEPTED_CATEGORY_ID)
            .setAcceptGoodContent(true)
            .setAcceptContentFromWhiteShops(true)
            .setHasKnowledge(false)
            .setAcceptPartnerSkus(true)
            .setAllowFastSkuCreation(true);
        categoryCachingService.addCategory(categoryNoKnowledge);
        updateCategoryKnowledgeInRepo(AUTO_ACCEPTED_CATEGORY_ID, true);
        var info = new CategoryInfo(AUTO_ACCEPTED_CATEGORY_ID)
            .setManualAcceptance(false);
        categoryInfoRepository.insertOrUpdate(info);
    }

    private DataCampOffer.Offer resaleDataCampOffer() {
        return OfferBuilder.create(initialOffer())
            .withUcMapping(
                OfferBuilder.modelMapping(AUTO_ACCEPTED_CATEGORY_ID, MODEL_PARENT_ID,
                    OfferTestUtils.DEFAULT_CATEGORY_NAME, MODEL_PARENT_TITLE))
            .withDefaultMarketContent(market ->
                market.getIrDataBuilder()
                    .setClassifierCategoryId((int) AUTO_ACCEPTED_CATEGORY_ID)
                    .setClassifierConfidentTopPercision(0.1)
                    .setEnrichType(UltraController.EnrichedOffer.EnrichType.ET_MAIN)
                    .setMatchedId((int) MODEL_PARENT_ID))
            .withDefaultMarketSpecificContent()
            .withDefaultProcessedSpecification(customizer -> {
                    customizer.setUrl(DataCampOfferMeta.StringValue.newBuilder().setValue("http://some-url.ru/").build());
                    customizer.setIsResale(DataCampOfferMeta.Flag.newBuilder().setFlag(true).build());
                }
            )
            .get().build();
    }
}
