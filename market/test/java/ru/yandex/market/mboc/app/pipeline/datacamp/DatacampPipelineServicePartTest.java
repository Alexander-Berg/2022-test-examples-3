package ru.yandex.market.mboc.app.pipeline.datacamp;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import Market.DataCamp.DataCampValidationResult;
import org.junit.Test;

import ru.yandex.market.mboc.app.pipeline.GenericScenario;
import ru.yandex.market.mboc.common.datacamp.OfferBuilder;
import ru.yandex.market.mboc.common.offers.model.BusinessSkuKey;
import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.services.category_info.info.CategoryInfo;
import ru.yandex.market.mboc.common.utils.OfferTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.mboc.common.utils.OfferTestUtils.BLUE_SUPPLIER_ID_1;
import static ru.yandex.market.mboc.common.utils.OfferTestUtils.BLUE_SUPPLIER_ID_2;
import static ru.yandex.market.mboc.common.utils.OfferTestUtils.WHITE_SUPPLIER_ID;
import static ru.yandex.market.mboc.common.utils.OfferTestUtils.blueSupplierUnderBiz1;
import static ru.yandex.market.mboc.common.utils.OfferTestUtils.blueSupplierUnderBiz2;
import static ru.yandex.market.mboc.common.utils.OfferTestUtils.whiteSupplierUnderBiz;

public class DatacampPipelineServicePartTest extends BaseDatacampPipelineTest {
    @Test
    public void newOfferFormsAcceptanceReportIfEmptyServicePart() {
        var dcOffer = initialOffer();

        GenericScenario<DCPipelineState> noServicePartScenario = createScenario();
        noServicePartScenario
            .step("New offer without service part")
            .action(importOfferFromDC(toMessage(Set.of(), dcOffer)))
            .ignoreDefaultCheck()
            .addCheck((__, state) -> {
                var identifiers = dcOffer.getIdentifiers();
                var key = new BusinessSkuKey(identifiers.getBusinessId(), identifiers.getOfferId());
                var sentDCOffer = getLastSentDCOffer(key);
                var unitedOffer = sentDCOffer.orElseThrow(() -> new RuntimeException("acceptance " +
                    "suggests required"));
                assertThat(unitedOffer.getServiceMap()).isEmpty();
                var resolutions = unitedOffer.getBasic().getResolution().getBySourceList();
                assertThat(resolutions).hasSize(1);
                var verdicts = resolutions.get(0);
                assertThat(verdicts.getVerdictList()).hasSize(1);
                var resultsList = verdicts.getVerdictList().get(0).getResultsList();
                assertThat(resultsList).hasSize(1);
                var validationResult = resultsList.get(0);
                assertThat(validationResult.getApplicationsList()).hasSameElementsAs(List.of(
                    DataCampValidationResult.Feature.FBY,
                    DataCampValidationResult.Feature.FBY_PLUS,
                    DataCampValidationResult.Feature.FBS,
                    DataCampValidationResult.Feature.DBS,
                    DataCampValidationResult.Feature.EXPRESS
                ));
                assertThat(validationResult.getRecommendationStatus())
                    .isEqualTo(DataCampValidationResult.RecommendationStatus.MANUAL);
            })
            .expectedState(DCPipelineState.empty())
            .endStep()
            .execute();
        logbrokerEventPublisherMock.clear();

        GenericScenario<DCPipelineState> withServicePartScenario = createScenario();
        withServicePartScenario
            .step("New offer with service part")
            .action(importOfferFromDC(toMessage(Set.of(BLUE_SUPPLIER_ID_1), dcOffer)))
            .expectedState(DCPipelineState.onlyOffer(testDCOffer(blueSupplierUnderBiz1())
                .setCategoryIdForTests(OfferTestUtils.TEST_CATEGORY_INFO_ID, Offer.BindingKind.SUGGESTED)
                .setSuggestCategoryMappingId(OfferTestUtils.TEST_CATEGORY_INFO_ID)))
            .endStep()
            .execute();
    }

    @Test
    public void newOfferFormsAcceptanceReportWithMultipleVerdictsIfEmptyServicePart() {
        var categoryInfo = categoryInfoRepository.findById(OfferTestUtils.TEST_CATEGORY_INFO_ID)
            .setFbyAcceptanceMode(CategoryInfo.AcceptanceMode.AUTO_REJECT)
            .setFbyPlusAcceptanceMode(CategoryInfo.AcceptanceMode.AUTO_ACCEPT)
            .setFbsAcceptanceMode(CategoryInfo.AcceptanceMode.AUTO_ACCEPT)
            .setDsbsAcceptanceMode(CategoryInfo.AcceptanceMode.AUTO_ACCEPT)
            .setExpressAcceptanceMode(CategoryInfo.AcceptanceMode.MANUAL);
        categoryInfoRepository.update(categoryInfo);
        var dcOffer = initialOffer();

        GenericScenario<DCPipelineState> noServicePartScenario = createScenario();
        noServicePartScenario
            .step("New offer without service part")
            .action(importOfferFromDC(toMessage(Set.of(), dcOffer)))
            .ignoreDefaultCheck()
            .addCheck((__, state) -> {
                var identifiers = dcOffer.getIdentifiers();
                var key = new BusinessSkuKey(identifiers.getBusinessId(), identifiers.getOfferId());
                var sentDCOffer = getLastSentDCOffer(key);
                var unitedOffer = sentDCOffer.orElseThrow(() -> new RuntimeException("acceptance " +
                    "suggests required"));
                assertThat(unitedOffer.getServiceMap()).isEmpty();
                var resolutions = unitedOffer.getBasic().getResolution().getBySourceList();
                assertThat(resolutions).hasSize(1);
                var verdicts = resolutions.get(0);
                assertThat(verdicts.getVerdictList()).hasSize(1);
                var resultsList = verdicts.getVerdictList().get(0).getResultsList();
                assertThat(resultsList).hasSize(3);

                var simpleResults = new HashMap<DataCampValidationResult.Feature,
                    DataCampValidationResult.RecommendationStatus>();

                for (var result : resultsList) {
                    var status = result.getRecommendationStatus();
                    for (var feature : result.getApplicationsList()) {
                        simpleResults.put(feature, status);
                    }
                }

                assertThat(simpleResults).containsAllEntriesOf(Map.of(
                    DataCampValidationResult.Feature.FBY, DataCampValidationResult.RecommendationStatus.NO_WAY,
                    DataCampValidationResult.Feature.FBY_PLUS, DataCampValidationResult.RecommendationStatus.FINE,
                    DataCampValidationResult.Feature.FBS, DataCampValidationResult.RecommendationStatus.FINE,
                    DataCampValidationResult.Feature.DBS, DataCampValidationResult.RecommendationStatus.FINE,
                    DataCampValidationResult.Feature.EXPRESS, DataCampValidationResult.RecommendationStatus.MANUAL
                ));
            })
            .expectedState(DCPipelineState.empty())
            .endStep()
            .execute();
    }

    @Test
    public void existingOfferIsUpdatedWithNoServicePart() {
        var offer = testDCOffer(blueSupplierUnderBiz1());
        offerRepository.insertOffer(offer);

        var dcOffer = OfferBuilder.create(initialOffer())
            .build();

        GenericScenario<DCPipelineState> scenario = createScenario();
        scenario
            .step("Existing offer without service part")
            .action(importOfferFromDC(toMessage(Set.of(), dcOffer)))
            .expectedState(DCPipelineState.onlyOffer(testDCOffer(blueSupplierUnderBiz1())
                .setCategoryIdForTests(OfferTestUtils.TEST_CATEGORY_INFO_ID, Offer.BindingKind.SUGGESTED)
                .setSuggestCategoryMappingId(OfferTestUtils.TEST_CATEGORY_INFO_ID)))
            .endStep()
            .execute();
    }

    @Test
    public void existingServiceOffersAreRetainedIfNotInServicePart() {
        var offer = testDCOffer(blueSupplierUnderBiz1());
        offerRepository.insertOffer(offer);

        var dcOffer = OfferBuilder.create(initialOffer())
            .build();

        GenericScenario<DCPipelineState> scenario = createScenario();
        scenario
            .step("Existing offer with different service part")
            .action(importOfferFromDC(toMessage(Set.of(BLUE_SUPPLIER_ID_2), dcOffer)))
            .expectedState(DCPipelineState.onlyOffer(testDCOffer(blueSupplierUnderBiz1())
                .setCategoryIdForTests(OfferTestUtils.TEST_CATEGORY_INFO_ID, Offer.BindingKind.SUGGESTED)
                .setSuggestCategoryMappingId(OfferTestUtils.TEST_CATEGORY_INFO_ID)
                .addNewServiceOfferIfNotExistsForTests(blueSupplierUnderBiz2())))
            .endStep()
            .execute();
    }

    @Test
    public void existingServiceOffersAreRetainedIfOnlyWhiteIsInServicePart() {
        var offer = testDCOffer(blueSupplierUnderBiz1());
        offer.updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK);
        offerRepository.insertOffer(offer);

        var dcOffer = OfferBuilder.create(initialOffer())
            .build();

        GenericScenario<DCPipelineState> scenario = createScenario();
        scenario
            .step("Existing offer with different service part (only white)")
            .action(importOfferFromDC(toMessage(Set.of(WHITE_SUPPLIER_ID), dcOffer)))
            .expectedState(DCPipelineState.onlyOffer(testDCOffer(blueSupplierUnderBiz1())
                .addNewServiceOfferIfNotExistsForTests(whiteSupplierUnderBiz())
                .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
                .updateProcessingStatusIfValid(Offer.ProcessingStatus.IN_CLASSIFICATION)
                .incrementProcessingCounter()
                .setCategoryIdForTests(OfferTestUtils.TEST_CATEGORY_INFO_ID, Offer.BindingKind.SUGGESTED)
                .setSuggestCategoryMappingId(OfferTestUtils.TEST_CATEGORY_INFO_ID)))
            .endStep()
            .execute();
    }
}
