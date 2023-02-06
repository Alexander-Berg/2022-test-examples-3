package ru.yandex.market.mboc.app.pipeline.datacamp;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import Market.DataCamp.DataCampExplanation;
import Market.DataCamp.DataCampOffer;
import Market.DataCamp.DataCampOfferMapping;
import Market.DataCamp.DataCampOfferMarketContent;
import Market.DataCamp.DataCampOfferMeta;
import Market.DataCamp.DataCampResolution;
import Market.DataCamp.DataCampUnitedOffer;
import Market.DataCamp.DataCampValidationResult;
import org.assertj.core.api.Assertions;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.mbo.mdm.common.masterdata.services.SupplierConverterServiceMock;
import ru.yandex.market.mboc.common.datacamp.OfferBuilder;
import ru.yandex.market.mboc.common.datacamp.model.DataCampUnitedOffersEvent;
import ru.yandex.market.mboc.common.dict.MbocSupplierType;
import ru.yandex.market.mboc.common.dict.Supplier;
import ru.yandex.market.mboc.common.offers.model.BusinessSkuKey;
import ru.yandex.market.mboc.common.offers.model.ContentProcessingResponse;
import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.offers.model.ShopSkuKey;
import ru.yandex.market.mboc.common.utils.OfferTestUtils;

import static ru.yandex.market.mboc.common.services.datacamp.LogbrokerDatacampOfferMessageHandlerDbTest.offerToProcess;
import static ru.yandex.market.mboc.common.utils.OfferTestUtils.TEST_CATEGORY_INFO_ID;

public class DatacampPipelineTest extends BaseDatacampPipelineTest {
    private static final int BIZ_ID = 123456;
    private static final int SUPPLIER_ID = 321654;
    private static final long CATEGORY_ID = TEST_CATEGORY_INFO_ID;
    private static final String CATEGORY_NAME = "category";

    @Before
    public void setUpBeforeEach() {
        supplierRepository.insertBatch(
            new Supplier(BIZ_ID, "asdf")
                .setDatacamp(false)
                .setType(MbocSupplierType.BUSINESS)
                .setNewContentPipeline(true)
                .setFulfillment(true)
                .setEats(true)
        );
    }

    @Test
    public void testRemoveGutginVerdictIfOfferHasContentMapping() {
        var shopSku1 = "someoffer1";

        var dcOfferReceived = OfferBuilder.create(initialOffer())
            .withIdentifiers(BIZ_ID, shopSku1)
            .get()
            .setContent(offerToProcess().getContentBuilder()
                .setBinding(DataCampOfferMapping.ContentBinding.newBuilder()
                    .setPartner(DataCampOfferMapping.Mapping.newBuilder()
                        .setMarketCategoryId(Math.toIntExact(TEST_CATEGORY_INFO_ID))
                        .build())
                    .setUcMapping(OfferBuilder.categoryMapping(CATEGORY_ID, CATEGORY_NAME))
                    .build()))
            .build();

        logbrokerDatacampOfferMessageHandler.process(Collections.singletonList(toMessage(SUPPLIER_ID, dcOfferReceived)));

        var foundOffer = offerRepository.findOfferByBusinessSkuKeyWithContent(new BusinessSkuKey(BIZ_ID, shopSku1));

        Assert.assertEquals(Offer.ProcessingStatus.OPEN, foundOffer.getProcessingStatus());
        Assert.assertEquals(Offer.MappingDestination.BLUE, foundOffer.getOfferDestination());

        var response = DataCampOfferMarketContent.MarketContentProcessing.newBuilder()
            .setResult(DataCampOfferMarketContent.MarketContentProcessing.TotalResult.TOTAL_ERROR)
            .build();

        DataCampResolution.Resolution resolution = DataCampResolution.Resolution.newBuilder()
            .addBySource(DataCampResolution.Verdicts.newBuilder()
                .setMeta(DataCampOfferMeta.UpdateMeta.newBuilder()
                    .setSource(DataCampOfferMeta.DataSource.MARKET_GUTGIN)
                    .build())
                .addVerdict(DataCampResolution.Verdict.newBuilder()
                    .addResults(DataCampValidationResult.ValidationResult.newBuilder()
                        .addMessages(DataCampExplanation.Explanation.newBuilder()
                            .setText("VALIDATION_TEXT")
                            .setCode("CODE")
                            .setLevel(DataCampExplanation.Explanation.Level.FATAL)
                            .build())
                        .build())
                    .build())
                .build())
            .build();

        queueFromContentProcessingRepository.insert(new ContentProcessingResponse()
            .setOfferId(foundOffer.getId())
            .setProcessingResponse(response)
            .setResolution(resolution)
        );

        foundOffer.updateApprovedSkuMapping(
            OfferTestUtils.mapping(MARKET_SKU_ID_1, Offer.SkuType.MARKET),
            Offer.MappingConfidence.CONTENT);
        offerRepository.updateOffer(foundOffer);

        tmsSendDataCampOfferStates().accept(null);
        List<DataCampUnitedOffersEvent> events = logbrokerEventPublisherMock.getSendEvents();
        Assertions.assertThat(events).hasSize(1);

        List<DataCampOffer.Offer> dataCampOffers = events.stream()
            .flatMap(e -> e.getPayload().getUnitedOffersList().stream())
            .flatMap(offersBatch -> offersBatch.getOfferList().stream())
            .map(DataCampUnitedOffer.UnitedOffer::getBasic)
            .collect(Collectors.toList());

        Assertions.assertThat(dataCampOffers).hasSize(1);

        Map<Integer, DataCampOffer.Offer> offersByBusiness =
            dataCampOffers.stream().collect(Collectors.toMap(o -> o.getIdentifiers().getBusinessId(),
                Function.identity()));
        var event = offersByBusiness.get(BIZ_ID);
        Assert.assertNotNull(event);
        var res = event.getResolution();

        var verdictsBySource = res.getBySourceList().stream()
            .collect(Collectors.groupingBy(v -> v.getMeta().getSource()));
        Assertions.assertThat(verdictsBySource).containsKey(DataCampOfferMeta.DataSource.MARKET_GUTGIN);
        var gutginVerdicts = verdictsBySource.get(DataCampOfferMeta.DataSource.MARKET_GUTGIN);
        Assertions.assertThat(gutginVerdicts).hasSize(1);
        Assertions.assertThat(gutginVerdicts).allSatisfy(
            v -> Assertions.assertThat(v.getVerdictList()).hasSize(1)
        );

        var recommendations = unwrapAcceptanceRecommendations(
            verdictsBySource.get(DataCampOfferMeta.DataSource.MARKET_MBO)
        );
        Assertions.assertThat(recommendations.get(DataCampValidationResult.RecommendationStatus.MANUAL)).hasSize(5);

        // update mapping
        foundOffer = offerRepository.findOfferByBusinessSkuKeyWithContent(new BusinessSkuKey(BIZ_ID, shopSku1));

        foundOffer.updateApprovedSkuMapping(
            OfferTestUtils.mapping(MARKET_SKU_ID_2, Offer.SkuType.MARKET),
            Offer.MappingConfidence.CONTENT);
        offerRepository.updateOffer(foundOffer);

        logbrokerEventPublisherMock.clear();
        tmsSendDataCampOfferStates().accept(null);
        events = logbrokerEventPublisherMock.getSendEvents();
        Assertions.assertThat(events).hasSize(1);

        dataCampOffers = events.stream()
            .flatMap(e -> e.getPayload().getUnitedOffersList().stream())
            .flatMap(offersBatch -> offersBatch.getOfferList().stream())
            .map(DataCampUnitedOffer.UnitedOffer::getBasic)
            .collect(Collectors.toList());

        Assertions.assertThat(dataCampOffers).hasSize(1);

        offersByBusiness =
            dataCampOffers.stream().collect(Collectors.toMap(o -> o.getIdentifiers().getBusinessId(),
                Function.identity()));
        event = offersByBusiness.get(BIZ_ID);
        Assert.assertNotNull(event);
        res = event.getResolution();

        verdictsBySource = res.getBySourceList().stream()
            .collect(Collectors.groupingBy(v -> v.getMeta().getSource()));
        Assertions.assertThat(verdictsBySource).containsKey(DataCampOfferMeta.DataSource.MARKET_GUTGIN);
        gutginVerdicts = verdictsBySource.get(DataCampOfferMeta.DataSource.MARKET_GUTGIN);
        Assertions.assertThat(gutginVerdicts).hasSize(1);
        Assertions.assertThat(gutginVerdicts).allSatisfy(
            v -> Assertions.assertThat(v.getVerdictList()).isEmpty()
        );

        recommendations = unwrapAcceptanceRecommendations(
            verdictsBySource.get(DataCampOfferMeta.DataSource.MARKET_MBO)
        );
        Assertions.assertThat(recommendations.get(DataCampValidationResult.RecommendationStatus.MANUAL)).hasSize(5);
    }

    @Test
    public void testFilterOutBeruBusinessId() {
        var supplier = OfferTestUtils.realSupplier();

        supplierRepository.insertBatch(
            new Supplier(SupplierConverterServiceMock.BERU_BUSINESS_ID,
                "beru biz",
                "qwe",
                "qwe",
                MbocSupplierType.BUSINESS),
            new Supplier(SupplierConverterServiceMock.BERU_ID,
                "beru",
                "beru",
                "beru",
                MbocSupplierType.FIRST_PARTY),
            supplier
        );
        offerRepository.deleteAllInTest();

        var offer = OfferTestUtils.simpleOffer(supplier);
        String goodTitle = "good title";
        String shopSku1 = "someoffer1";
        offer.setTitle(goodTitle);
        offer.setShopSku(shopSku1);
        offer.setBusinessId(supplier.getId());
        offerRepository.insertOffer(offer);

        var dcOfferReceived = OfferBuilder.create(initialOffer())
            .withIdentifiers(SupplierConverterServiceMock.BERU_BUSINESS_ID,
                supplier.getRealSupplierId() + "." + shopSku1)
            .withPartnerTitle("wrong title")
            .build();

        supplierConverterService.addInternalToExternalMapping(
            new ShopSkuKey(supplier.getId(), shopSku1),
            new ShopSkuKey(SupplierConverterServiceMock.BERU_ID, supplier.getRealSupplierId() + "." + shopSku1)
        );

        logbrokerDatacampOfferMessageHandler.process(
            Collections.singletonList(toMessage(SupplierConverterServiceMock.BERU_ID, dcOfferReceived)));

        var found = offerRepository.findAll().get(0);
        Assert.assertEquals(goodTitle, found.getTitle());
    }

    @Test
    public void testReportFormingForOfferWithoutServiceParts() {
        var business = OfferTestUtils.businessSupplier();
        var serviceSupplier = OfferTestUtils.simpleSupplier().setBusinessId(business.getId()).setFulfillment(true);
        supplierRepository.deleteAll();
        supplierRepository.insertBatch(business, serviceSupplier);
        var offerBuilder = OfferBuilder.create(initialOffer())
            .withIdentifiers(business.getId(), "some-offer-id")
            .withPartnerTitle("some title")
            .build();
        logbrokerDatacampOfferMessageHandler.process(
            List.of(toMessage(Set.of(), offerBuilder))
        );
        Assert.assertTrue(offerRepository.findAll().isEmpty());
    }

}
