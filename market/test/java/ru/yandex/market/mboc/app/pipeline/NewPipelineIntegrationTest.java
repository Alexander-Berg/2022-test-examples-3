package ru.yandex.market.mboc.app.pipeline;

import java.time.LocalDateTime;
import java.util.List;
import java.util.function.BiConsumer;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import ru.yandex.market.mboc.common.honestmark.OfferCategoryRestrictionCalculator;
import ru.yandex.market.mboc.common.offers.model.ContentComment;
import ru.yandex.market.mboc.common.offers.model.ContentCommentType;
import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.offers.model.upload.OfferUploadQueueItem;
import ru.yandex.market.mboc.common.services.offers.upload.OfferUploadQueueService;
import ru.yandex.market.mboc.common.utils.DateTimeUtils;
import ru.yandex.market.mboc.common.utils.OfferTestUtils;
import ru.yandex.market.mboc.http.MboMappings.UpdateContentProcessingTasksRequest.ContentProcessingTask;
import ru.yandex.market.mboc.http.SupplierOffer.MappingModerationTaskResult.SupplierMappingModerationResult;

import static ru.yandex.market.mboc.http.MboMappings.ProductUpdateRequestInfo.ChangeType.SKU_REMOVAL_DUPLICATE;

/**
 * Test several scenarios from new pipeline.
 */
public class NewPipelineIntegrationTest extends BasePipelineTest {

    @Test
    public void supplierImportsNewOffer() {
        createScenario()
            .step("import offer form excel")
            .action(importOfferFromExcel())
            .expectedState(startingOffer())
            .endStep()

            .execute();
    }

    @Test
    public void newOfferAcceptedByCatmanInUI() {
        GenericScenario<Offer> scenario = createScenario();
        scenario
            .step("import offer form excel")
            .action(importOfferFromExcel())
            .expectedState(startingOffer())
            .endStep()

            .step("thumbs up in UI (with knowledge)")
            .action(catmanUpdatesAcceptanceStatus(Offer.AcceptanceStatus.OK, true))
            .expectedState(scenario.previousValidState()
                .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
                .updateProcessingStatusIfValid(Offer.ProcessingStatus.IN_CLASSIFICATION)
                .incrementProcessingCounter())
            .endStep()

            .step("create classification ticket")
            .action(tmsCreateTrackerTickets())
            .expectedState(scenario.previousValidState())
            .endStep()

            .step("get category from classification and move to NEED_CONTENT")
            .action(operatorClassifiesOfferByFile(OfferTestUtils.TEST_CATEGORY_INFO_ID)
                .andThen(tmsProcessClassificationTicketsResults()))
            .expectedState(scenario.previousValidState()
                .setMappedCategoryId(OfferTestUtils.TEST_CATEGORY_INFO_ID, Offer.MappingConfidence.CONTENT)
                .setBindingKind(Offer.BindingKind.APPROVED)
                .setContentCategoryMappingId(OfferTestUtils.TEST_CATEGORY_INFO_ID)
                .setContentCategoryMappingStatus(Offer.MappingStatus.NEW)
                .updateProcessingStatusIfValid(Offer.ProcessingStatus.NEED_CONTENT)
                .updateContentProcessingStatus(Offer.ContentProcessingStatus.NEED_CONTENT))
            .endStep()

            .execute();
    }

    @Test
    public void newOfferRejectedByCatmanInUI() {
        GenericScenario<Offer> scenario = createScenario();
        scenario
            .step("import offer form excel")
            .action(importOfferFromExcel())
            .expectedState(startingOffer())
            .endStep()

            .step("thumbs down in UI (with knowledge)")
            .action(catmanUpdatesAcceptanceStatus(Offer.AcceptanceStatus.TRASH, true))
            .expectedState(scenario.previousValidState()
                .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.TRASH))
            .endStep()

            .execute();
    }

    @Test
    public void newBlueOfferAcceptedByCatmanInUIWithoutCatKnowledgeThenKnowledgeAppears() {
        GenericScenario<Offer> scenario = createScenario();
        scenario
            .step("import offer form excel")
            .action(importOfferFromExcel())
            .expectedState(startingOffer())
            .endStep()

            .step("thumbs up in UI")
            .action(catmanUpdatesAcceptanceStatus(Offer.AcceptanceStatus.OK, false))
            .expectedState(scenario.previousValidState()
                .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
                .updateProcessingStatusIfValid(Offer.ProcessingStatus.IN_CLASSIFICATION)
                .incrementProcessingCounter())
            .endStep()

            .execute();
    }

    @Test
    public void newOfferAcceptedByCatmanInUIWithoutCatKnowledgeThenKnowledgeAppears() {
        GenericScenario<Offer> scenario = createScenario();
        scenario
            .step("import offer form excel")
            .action(importOfferFromExcel())
            .expectedState(startingOffer())
            .endStep()

            .step("thumbs up in UI")
            .action(catmanUpdatesAcceptanceStatus(Offer.AcceptanceStatus.OK, false))
            .expectedState(scenario.previousValidState()
                .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
                .updateProcessingStatusIfValid(Offer.ProcessingStatus.IN_CLASSIFICATION)
                .incrementProcessingCounter())
            .endStep()

            .step("operator marked as NO_KNOWLEDGE")
            .action(o -> {
                var offer = offerRepository.findOfferByBusinessSkuKey(o.getBusinessSkuKey());
                offer.setProcessingStatusInternal(Offer.ProcessingStatus.OPEN)
                    .setContentComments(new ContentComment(ContentCommentType.NO_KNOWLEDGE, ""));
                offersProcessingStatusService.processOffers(List.of(offer));
                offerRepository.updateOffer(offer);
            })
            .expectedState(scenario.previousValidState()
                .setProcessingStatusInternal(Offer.ProcessingStatus.NO_KNOWLEDGE)
                .setContentComments(new ContentComment(ContentCommentType.NO_KNOWLEDGE, "")))
            .endStep()

            .step("tms executed, still no knowledge")
            .action(tmsImportCategoryKnowledge(false))
            .expectedState(scenario.previousValidState())
            .endStep()

            .step("tms executed, knowledge appeared")
            .action(tmsImportCategoryKnowledge(true))
            .expectedState(scenario.previousValidState()
                .updateProcessingStatusIfValid(Offer.ProcessingStatus.NEED_CONTENT)
                .updateContentProcessingStatus(Offer.ContentProcessingStatus.NEED_CONTENT)
                .setContentComments())
            .endStep()

            .execute();
    }


    @Test
    public void newOfferAcceptedByCatmanInUIWithoutCatKnowledgeThenForceClassification() {
        GenericScenario<Offer> scenario = createScenario();
        scenario
            .step("import offer form excel")
            .action(importOfferFromExcel())
            .expectedState(startingOffer())
            .endStep()

            .step("thumbs up in UI")
            .action(catmanUpdatesAcceptanceStatus(Offer.AcceptanceStatus.OK, false))
            .expectedState(scenario.previousValidState()
                .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
                .updateProcessingStatusIfValid(Offer.ProcessingStatus.IN_CLASSIFICATION)
                .incrementProcessingCounter())
            .endStep()

            .execute();
    }

    @Test
    public void supplierProvidesMappingThenOperatorAcceptsIt() {
        GenericScenario<Offer> scenario = createScenario();
        scenario
            .step("import offer form excel")
            .action(importOfferFromExcel())
            .expectedState(startingOffer())
            .endStep()

            .step("simulating APPROVED category from UC")
            .action(offerGetsConfidentCategoryFromUc(OfferTestUtils.TEST_CATEGORY_INFO_ID))
            .expectedState(scenario.previousValidState()
                .setCategoryIdForTests(OfferTestUtils.TEST_CATEGORY_INFO_ID, Offer.BindingKind.APPROVED)
                .setClassifierCategoryId(OfferTestUtils.TEST_CATEGORY_INFO_ID,
                    OfferCategoryRestrictionCalculator.DEFAULT_CLASSIFIER_TRUST_THRESHOLD))
            .endStep()

            .step("thumbs up in UI (with knowledge)")
            .action(catmanUpdatesAcceptanceStatus(Offer.AcceptanceStatus.OK, true))
            .expectedState(scenario.previousValidState()
                .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
                .updateProcessingStatusIfValid(Offer.ProcessingStatus.NEED_CONTENT)
                .updateContentProcessingStatus(Offer.ContentProcessingStatus.NEED_CONTENT))
            .endStep()

            .step("supplier sends mapping")
            .action(supplierSendsNewMapping(MARKET_SKU_ID_1))
            .expectedState(scenario.previousValidState()
                .updateProcessingStatusIfValid(Offer.ProcessingStatus.IN_MODERATION)
                .updateContentProcessingStatus(Offer.ContentProcessingStatus.NONE)
                .setSupplierSkuMapping(MAPPING_1)
                .setSupplierSkuMappingStatus(Offer.MappingStatus.NEW)
                .incrementProcessingCounter())
            .endStep()

            .step("operator accepted mapping")
            .action(operatorModeratesMapping(SupplierMappingModerationResult.ACCEPTED, MARKET_SKU_ID_1))
            .addChecks(List.of(
                verifyEnqueued(ytOfferUploadQueueService),
                verifyEnqueued(mdmOfferUploadQueueService)
            ))
            .expectedState(scenario.previousValidState()
                .setLastPrimaryProcessingStatus(Offer.ProcessingStatus.IN_MODERATION)
                .setBindingKind(Offer.BindingKind.APPROVED)
                .updateProcessingStatusIfValid(Offer.ProcessingStatus.PROCESSED)
                .updateContentProcessingStatus(Offer.ContentProcessingStatus.NONE)
                .setContentSkuMapping(MAPPING_1)
                .approve(Offer.MappingType.CONTENT, Offer.MappingConfidence.CONTENT)
                .setSupplierSkuMappingStatus(Offer.MappingStatus.ACCEPTED,
                    YANG_OPERATOR_LOGIN, DateTimeUtils.dateTimeNow())
                .setModelId(MODEL_PARENT_ID)
                .setVendorId(OfferTestUtils.TEST_VENDOR_ID))
            .endStep()

            .execute();
    }

    @Test
    public void supplierProvidesMappingThenOperatorAcceptsItButMappingIsOnDeletedSku() {
        GenericScenario<Offer> scenario = createScenario();
        scenario
            .step("import offer form excel")
            .action(importOfferFromExcel())
            .expectedState(startingOffer())
            .endStep()

            .step("thumbs up in UI (with knowledge)")
            .action(catmanUpdatesAcceptanceStatus(Offer.AcceptanceStatus.OK, true))
            .expectedState(scenario.previousValidState()
                .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
                .updateProcessingStatusIfValid(Offer.ProcessingStatus.IN_CLASSIFICATION)
                .incrementProcessingCounter())
            .endStep()

            .step("create classification ticket")
            .action(tmsCreateTrackerTickets())
            .expectedState(scenario.previousValidState())
            .endStep()

            .step("got category from classification")
            .action(operatorClassifiesOfferByFile(OfferTestUtils.TEST_CATEGORY_INFO_ID)
                .andThen(tmsProcessClassificationTicketsResults()))
            .expectedState(scenario.previousValidState()
                .setMappedCategoryId(OfferTestUtils.TEST_CATEGORY_INFO_ID, Offer.MappingConfidence.CONTENT)
                .setBindingKind(Offer.BindingKind.APPROVED)
                .setContentCategoryMappingId(OfferTestUtils.TEST_CATEGORY_INFO_ID)
                .setContentCategoryMappingStatus(Offer.MappingStatus.NEW)
                .updateProcessingStatusIfValid(Offer.ProcessingStatus.NEED_CONTENT)
                .updateContentProcessingStatus(Offer.ContentProcessingStatus.NEED_CONTENT))
            .endStep()

            .step("supplier sends mapping")
            .action(supplierSendsNewMapping(DELETED_SKU_ID))
            .expectedState(scenario.previousValidState()
                .updateProcessingStatusIfValid(Offer.ProcessingStatus.IN_MODERATION)
                .updateContentProcessingStatus(Offer.ContentProcessingStatus.NONE)
                .setSupplierSkuMapping(OfferTestUtils.mapping(DELETED_SKU_ID, DELETED_SKU_TITLE))
                .setSupplierSkuMappingStatus(Offer.MappingStatus.NEW)
                .incrementProcessingCounter())
            .endStep()

            .step("operator accepted mapping (but sku is deleted)")
            .action(operatorModeratesMapping(SupplierMappingModerationResult.ACCEPTED, DELETED_SKU_ID))
            .expectedState(scenario.previousValidState()
                .setLastPrimaryProcessingStatus(Offer.ProcessingStatus.IN_MODERATION)
                .updateProcessingStatusIfValid(Offer.ProcessingStatus.NEED_CONTENT)
                .updateContentProcessingStatus(Offer.ContentProcessingStatus.NEED_CONTENT)
                .setSupplierSkuMappingStatus(Offer.MappingStatus.REJECTED,
                    YANG_OPERATOR_LOGIN, DateTimeUtils.dateTimeNow())
                .setContentComment(
                    "Can't set content mapping, because market sku (" + DELETED_SKU_ID + ") " +
                        "is invalid (not exist, or not sku, or not published on blue, or smth else)"))
            .endStep()

            .execute();
    }

    @Test
    public void supplierProvidesMappingThenOperatorRejectsItThenSupplierProvidesOtherThenOperatorAcceptsIt() {
        GenericScenario<Offer> scenario = createScenario();
        scenario
            .step("import offer form excel")
            .action(importOfferFromExcel())
            .expectedState(startingOffer())
            .endStep()

            .step("simulating APPROVED category from UC")
            .action(offerGetsConfidentCategoryFromUc(OfferTestUtils.TEST_CATEGORY_INFO_ID))
            .expectedState(scenario.previousValidState()
                .setCategoryIdForTests(OfferTestUtils.TEST_CATEGORY_INFO_ID, Offer.BindingKind.APPROVED)
                .setClassifierCategoryId(OfferTestUtils.TEST_CATEGORY_INFO_ID,
                    OfferCategoryRestrictionCalculator.DEFAULT_CLASSIFIER_TRUST_THRESHOLD))
            .endStep()

            .step("thumbs up in UI (with knowledge)")
            .action(catmanUpdatesAcceptanceStatus(Offer.AcceptanceStatus.OK, true))
            .expectedState(scenario.previousValidState()
                .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
                .updateProcessingStatusIfValid(Offer.ProcessingStatus.NEED_CONTENT)
                .updateContentProcessingStatus(Offer.ContentProcessingStatus.NEED_CONTENT))
            .endStep()

            .step("supplier sends mapping")
            .action(supplierSendsNewMapping(MARKET_SKU_ID_1))
            .expectedState(scenario.previousValidState()
                .updateProcessingStatusIfValid(Offer.ProcessingStatus.IN_MODERATION)
                .updateContentProcessingStatus(Offer.ContentProcessingStatus.NONE)
                .setSupplierSkuMapping(MAPPING_1)
                .setSupplierSkuMappingStatus(Offer.MappingStatus.NEW)
                .incrementProcessingCounter())
            .endStep()

            .step("operator rejects mapping")
            .action(operatorModeratesMapping(SupplierMappingModerationResult.REJECTED, MARKET_SKU_ID_1))
            .expectedState(scenario.previousValidState()
                .setLastPrimaryProcessingStatus(Offer.ProcessingStatus.IN_MODERATION)
                .updateProcessingStatusIfValid(Offer.ProcessingStatus.NEED_CONTENT)
                .updateContentProcessingStatus(Offer.ContentProcessingStatus.NEED_CONTENT)
                .setSupplierSkuMappingStatus(Offer.MappingStatus.REJECTED,
                    YANG_OPERATOR_LOGIN, DateTimeUtils.dateTimeNow()))
            .endStep()

            .step("supplier sends other mapping")
            .action(supplierSendsNewMapping(MARKET_SKU_ID_2))
            .expectedState(scenario.previousValidState()
                .updateProcessingStatusIfValid(Offer.ProcessingStatus.IN_MODERATION)
                .updateContentProcessingStatus(Offer.ContentProcessingStatus.NONE)
                .setSupplierSkuMapping(MAPPING_2)
                .setSupplierSkuMappingStatus(Offer.MappingStatus.NEW, null, null)
                .incrementProcessingCounter())
            .endStep()

            .step("operator accepted mapping")
            .action(operatorModeratesMapping(SupplierMappingModerationResult.ACCEPTED, MARKET_SKU_ID_2))
            .expectedState(scenario.previousValidState()
                .setLastPrimaryProcessingStatus(Offer.ProcessingStatus.IN_MODERATION)
                .setBindingKind(Offer.BindingKind.APPROVED)
                .updateProcessingStatusIfValid(Offer.ProcessingStatus.PROCESSED)
                .updateContentProcessingStatus(Offer.ContentProcessingStatus.NONE)
                .setContentSkuMapping(MAPPING_2)
                .approve(Offer.MappingType.CONTENT, Offer.MappingConfidence.CONTENT)
                .setSupplierSkuMappingStatus(Offer.MappingStatus.ACCEPTED,
                    YANG_OPERATOR_LOGIN, DateTimeUtils.dateTimeNow())
                .setModelId(MODEL_PARENT_ID)
                .setVendorId(OfferTestUtils.TEST_VENDOR_ID))
            .endStep()

            .execute();
    }

    @Test
    public void supplierProvidesMappingThenOperatorNeedsInfoThenSupplierUpdatesOfferThenOperatorAcceptsIt() {
        GenericScenario<Offer> scenario = createScenario();
        scenario
            .step("import offer form excel")
            .action(importOfferFromExcel())
            .expectedState(startingOffer())
            .endStep()

            .step("thumbs up in UI (with knowledge)")
            .action(catmanUpdatesAcceptanceStatus(Offer.AcceptanceStatus.OK, true))
            .expectedState(scenario.previousValidState()
                .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
                .updateProcessingStatusIfValid(Offer.ProcessingStatus.IN_CLASSIFICATION)
                .incrementProcessingCounter())
            .endStep()

            .step("create classification ticket")
            .action(tmsCreateTrackerTickets())
            .expectedState(scenario.previousValidState())
            .endStep()

            .step("got category from classification")
            .action(operatorClassifiesOfferByFile(OfferTestUtils.TEST_CATEGORY_INFO_ID)
                .andThen(tmsProcessClassificationTicketsResults()))
            .expectedState(scenario.previousValidState()
                .setMappedCategoryId(OfferTestUtils.TEST_CATEGORY_INFO_ID, Offer.MappingConfidence.CONTENT)
                .setBindingKind(Offer.BindingKind.APPROVED)
                .setContentCategoryMappingId(OfferTestUtils.TEST_CATEGORY_INFO_ID)
                .setContentCategoryMappingStatus(Offer.MappingStatus.NEW)
                .updateProcessingStatusIfValid(Offer.ProcessingStatus.NEED_CONTENT)
                .updateContentProcessingStatus(Offer.ContentProcessingStatus.NEED_CONTENT))
            .endStep()

            .step("supplier sends mapping")
            .action(supplierSendsNewMapping(MARKET_SKU_ID_1))
            .expectedState(scenario.previousValidState()
                .updateProcessingStatusIfValid(Offer.ProcessingStatus.IN_MODERATION)
                .updateContentProcessingStatus(Offer.ContentProcessingStatus.NONE)
                .setSupplierSkuMapping(MAPPING_1)
                .setSupplierSkuMappingStatus(Offer.MappingStatus.NEW)
                .incrementProcessingCounter())
            .endStep()

            .step("operator sets need_info")
            .action(operatorModeratesMapping(SupplierMappingModerationResult.NEED_INFO, MARKET_SKU_ID_1))
            .expectedState(scenario.previousValidState()
                .setLastPrimaryProcessingStatus(Offer.ProcessingStatus.IN_MODERATION)
                .updateProcessingStatusIfValid(Offer.ProcessingStatus.NEED_INFO))
            .endStep()

            .step("offer not changed yet")
            .action(tmsReopenNeedInfoOffers())
            .expectedState(scenario.previousValidState())
            .endStep()

            .step("supplier updated description")
            .action(supplierUpdatesOfferInformation(MARKET_SKU_ID_1))
            .expectedState(scenario.previousValidState()
                .storeOfferContent(scenario.previousValidState().getOfferContentBuilder().description(DESCRIPTION).build())
                .setContentChangedTs(DateTimeUtils.dateTimeNow()))
            .endStep()

            .step("offer changed")
            .action(tmsReopenNeedInfoOffers())
            .expectedState(scenario.previousValidState()
                .setLastPrimaryProcessingStatus(Offer.ProcessingStatus.REOPEN)
                .updateProcessingStatusIfValid(Offer.ProcessingStatus.IN_MODERATION)
                .incrementProcessingCounter()
            )
            .endStep()

            .step("operator accepted mapping")
            .action(operatorModeratesMapping(SupplierMappingModerationResult.ACCEPTED, MARKET_SKU_ID_1))
            .addChecks(List.of(
                verifyEnqueued(ytOfferUploadQueueService),
                verifyEnqueued(mdmOfferUploadQueueService)
            ))
            .expectedState(scenario.previousValidState()
                .setLastPrimaryProcessingStatus(Offer.ProcessingStatus.IN_MODERATION)
                .setBindingKind(Offer.BindingKind.APPROVED)
                .updateProcessingStatusIfValid(Offer.ProcessingStatus.PROCESSED)
                .updateContentProcessingStatus(Offer.ContentProcessingStatus.NONE)
                .setContentSkuMapping(MAPPING_1)
                .approve(Offer.MappingType.CONTENT, Offer.MappingConfidence.CONTENT)
                .setSupplierSkuMappingStatus(Offer.MappingStatus.ACCEPTED,
                    YANG_OPERATOR_LOGIN, DateTimeUtils.dateTimeNow())
                .setModelId(MODEL_PARENT_ID)
                .setVendorId(OfferTestUtils.TEST_VENDOR_ID))
            .endStep()

            .execute();
    }

    @Test
    public void supplierCantFindAnyMappingForNewOffer() {
        GenericScenario<Offer> scenario = createScenario();
        scenario
            .step("import offer form excel")
            .action(importOfferFromExcel())
            .expectedState(startingOffer())
            .endStep()

            .step("thumbs up in UI (with knowledge)")
            .action(catmanUpdatesAcceptanceStatus(Offer.AcceptanceStatus.OK, true))
            .expectedState(scenario.previousValidState()
                .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
                .updateProcessingStatusIfValid(Offer.ProcessingStatus.IN_CLASSIFICATION)
                .incrementProcessingCounter())
            .endStep()

            .step("create classification ticket")
            .action(tmsCreateTrackerTickets())
            .expectedState(scenario.previousValidState())
            .endStep()

            .step("get category from classification and move to NEED_CONTENT")
            .action(operatorClassifiesOfferByFile(OfferTestUtils.TEST_CATEGORY_INFO_ID)
                .andThen(tmsProcessClassificationTicketsResults()))
            .expectedState(scenario.previousValidState()
                .setMappedCategoryId(OfferTestUtils.TEST_CATEGORY_INFO_ID, Offer.MappingConfidence.CONTENT)
                .setBindingKind(Offer.BindingKind.APPROVED)
                .setContentCategoryMappingId(OfferTestUtils.TEST_CATEGORY_INFO_ID)
                .setContentCategoryMappingStatus(Offer.MappingStatus.NEW)
                .updateProcessingStatusIfValid(Offer.ProcessingStatus.NEED_CONTENT)
                .updateContentProcessingStatus(Offer.ContentProcessingStatus.NEED_CONTENT))
            .endStep()

            .step("supplier can't find mapping and gives up")
            .action(supplierDoesNotKnowMapping())
            .expectedState(scenario.previousValidState()
                .updateProcessingStatusIfValid(Offer.ProcessingStatus.NEED_CONTENT)
                .updateContentProcessingStatus(Offer.ContentProcessingStatus.NEED_CONTENT))
            .endStep()

            .execute();
    }

    @Test
    public void supplierCreatesNewSkuThenItIsAcceptedByIR() {
        GenericScenario<Offer> scenario = createScenario();
        scenario
            .step("import offer form excel")
            .action(importOfferFromExcel())
            .expectedState(startingOffer())
            .endStep()

            .step("thumbs up in UI (with knowledge)")
            .action(catmanUpdatesAcceptanceStatus(Offer.AcceptanceStatus.OK, true))
            .expectedState(scenario.previousValidState()
                .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
                .updateProcessingStatusIfValid(Offer.ProcessingStatus.IN_CLASSIFICATION)
                .incrementProcessingCounter())
            .endStep()

            .step("create classification ticket")
            .action(tmsCreateTrackerTickets())
            .expectedState(scenario.previousValidState())
            .endStep()

            .step("get category from classification and move to NEED_CONTENT")
            .action(operatorClassifiesOfferByFile(OfferTestUtils.TEST_CATEGORY_INFO_ID)
                .andThen(tmsProcessClassificationTicketsResults()))
            .expectedState(scenario.previousValidState()
                .setMappedCategoryId(OfferTestUtils.TEST_CATEGORY_INFO_ID, Offer.MappingConfidence.CONTENT)
                .setBindingKind(Offer.BindingKind.APPROVED)
                .setContentCategoryMappingId(OfferTestUtils.TEST_CATEGORY_INFO_ID)
                .setContentCategoryMappingStatus(Offer.MappingStatus.NEW)
                .updateProcessingStatusIfValid(Offer.ProcessingStatus.NEED_CONTENT)
                .updateContentProcessingStatus(Offer.ContentProcessingStatus.NEED_CONTENT))
            .endStep()

            .step("supplier can't find mapping and gives up")
            .action(supplierDoesNotKnowMapping())
            .expectedState(scenario.previousValidState()
                .updateProcessingStatusIfValid(Offer.ProcessingStatus.NEED_CONTENT)
                .updateContentProcessingStatus(Offer.ContentProcessingStatus.NEED_CONTENT))
            .endStep()

            .step("ir started processing sku from supplier")
            .action(irUpdatesContentProcessingTask(CONTENT_PROCESSING_TASK_ID_1,
                ContentProcessingTask.State.STARTED))
            .expectedState(scenario.previousValidState()
                .updateProcessingStatusIfValid(Offer.ProcessingStatus.CONTENT_PROCESSING)
                .updateContentProcessingStatus(Offer.ContentProcessingStatus.CONTENT_PROCESSING)
                .setContentProcessingTaskId(CONTENT_PROCESSING_TASK_ID_1))
            .endStep()

            .step("ir sends approved mapping")
            .action(irSendsNewMapping(MARKET_SKU_ID_1, "some login"))
            .addChecks(List.of(
                verifyEnqueued(ytOfferUploadQueueService),
                verifyEnqueued(mdmOfferUploadQueueService)
            ))
            .expectedState(scenario.previousValidState()
                .setModelId(MODEL_PARENT_ID)
                .setVendorId(OfferTestUtils.TEST_VENDOR_ID)
                .setBindingKind(Offer.BindingKind.APPROVED)
                .setSupplierSkuMapping(MAPPING_1)
                .setSupplierSkuMappingStatus(Offer.MappingStatus.ACCEPTED)
                .setSupplierSkuMappingCheckLogin("auto-accepted")
                .setSupplierSkuMappingCheckTs(LocalDateTime.now())
                .setGutginSkuMapping(MAPPING_1)
                .approve(Offer.MappingType.SUPPLIER,
                    Offer.MappingConfidence.PARTNER_SELF)
                .updateProcessingStatusIfValid(Offer.ProcessingStatus.AUTO_PROCESSED)
                .updateContentProcessingStatus(Offer.ContentProcessingStatus.PROCESSED))
            .endStep()

            .step("ir updates content processing task")
            .action(irUpdatesContentProcessingTask(CONTENT_PROCESSING_TASK_ID_1,
                ContentProcessingTask.State.SUCCESS))
            .expectedState(scenario.previousValidState()
                .updateProcessingStatusIfValid(Offer.ProcessingStatus.AUTO_PROCESSED)
                .updateContentProcessingStatus(Offer.ContentProcessingStatus.PROCESSED))
            .endStep()

            .execute();
    }

    @Test
    public void supplierCantFindMappingThenFindsMapping() {
        GenericScenario<Offer> scenario = createScenario();
        scenario
            .step("import offer form excel")
            .action(importOfferFromExcel())
            .expectedState(startingOffer())
            .endStep()

            .step("thumbs up in UI (with knowledge)")
            .action(catmanUpdatesAcceptanceStatus(Offer.AcceptanceStatus.OK, true))
            .expectedState(scenario.previousValidState()
                .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
                .updateProcessingStatusIfValid(Offer.ProcessingStatus.IN_CLASSIFICATION)
                .incrementProcessingCounter())
            .endStep()

            .step("create classification ticket")
            .action(tmsCreateTrackerTickets())
            .expectedState(scenario.previousValidState())
            .endStep()

            .step("get category from classification and move to NEED_CONTENT")
            .action(operatorClassifiesOfferByFile(OfferTestUtils.TEST_CATEGORY_INFO_ID)
                .andThen(tmsProcessClassificationTicketsResults()))
            .expectedState(scenario.previousValidState()
                .setMappedCategoryId(OfferTestUtils.TEST_CATEGORY_INFO_ID, Offer.MappingConfidence.CONTENT)
                .setBindingKind(Offer.BindingKind.APPROVED)
                .setContentCategoryMappingId(OfferTestUtils.TEST_CATEGORY_INFO_ID)
                .setContentCategoryMappingStatus(Offer.MappingStatus.NEW)
                .updateProcessingStatusIfValid(Offer.ProcessingStatus.NEED_CONTENT)
                .updateContentProcessingStatus(Offer.ContentProcessingStatus.NEED_CONTENT))
            .endStep()

            .step("supplier can't find mapping and gives up")
            .action(supplierDoesNotKnowMapping())
            .expectedState(scenario.previousValidState()
                .updateProcessingStatusIfValid(Offer.ProcessingStatus.NEED_CONTENT)
                .updateContentProcessingStatus(Offer.ContentProcessingStatus.NEED_CONTENT)
            )
            .endStep()

            .step("supplier sends mapping")
            .action(supplierSendsNewMapping(MARKET_SKU_ID_1))
            .expectedState(scenario.previousValidState()
                .updateProcessingStatusIfValid(Offer.ProcessingStatus.IN_MODERATION)
                .updateContentProcessingStatus(Offer.ContentProcessingStatus.NONE)
                .setSupplierSkuMapping(MAPPING_1)
                .setSupplierSkuMappingStatus(Offer.MappingStatus.NEW)
                .incrementProcessingCounter())
            .endStep()

            .execute();
    }

    @Test
    public void supplierCreatesNewSkuThenItIsRejectByIRThenSupplierCreatesOtherSkuThenItIsAcceptedByIR() {
        GenericScenario<Offer> scenario = createScenario();
        scenario
            .step("import offer form excel")
            .action(importOfferFromExcel())
            .expectedState(startingOffer())
            .endStep()


            .step("thumbs up in UI (with knowledge)")
            .action(catmanUpdatesAcceptanceStatus(Offer.AcceptanceStatus.OK, true))
            .expectedState(scenario.previousValidState()
                .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
                .updateProcessingStatusIfValid(Offer.ProcessingStatus.IN_CLASSIFICATION)
                .incrementProcessingCounter())
            .endStep()

            .step("create classification ticket")
            .action(tmsCreateTrackerTickets())
            .expectedState(scenario.previousValidState())
            .endStep()

            .step("get category from classification and move to NEED_CONTENT")
            .action(operatorClassifiesOfferByFile(OfferTestUtils.TEST_CATEGORY_INFO_ID)
                .andThen(tmsProcessClassificationTicketsResults()))
            .expectedState(scenario.previousValidState()
                .setMappedCategoryId(OfferTestUtils.TEST_CATEGORY_INFO_ID, Offer.MappingConfidence.CONTENT)
                .setBindingKind(Offer.BindingKind.APPROVED)
                .setContentCategoryMappingId(OfferTestUtils.TEST_CATEGORY_INFO_ID)
                .setContentCategoryMappingStatus(Offer.MappingStatus.NEW)
                .updateProcessingStatusIfValid(Offer.ProcessingStatus.NEED_CONTENT)
                .updateContentProcessingStatus(Offer.ContentProcessingStatus.NEED_CONTENT))
            .endStep()


            .step("supplier can't find mapping and gives up")
            .action(supplierDoesNotKnowMapping())
            .expectedState(scenario.previousValidState()
                .updateProcessingStatusIfValid(Offer.ProcessingStatus.NEED_CONTENT)
                .updateContentProcessingStatus(Offer.ContentProcessingStatus.NEED_CONTENT))
            .endStep()


            .step("ir started processing sku from supplier")
            .action(irUpdatesContentProcessingTask(CONTENT_PROCESSING_TASK_ID_1,
                ContentProcessingTask.State.STARTED))
            .expectedState(scenario.previousValidState()
                .updateProcessingStatusIfValid(Offer.ProcessingStatus.CONTENT_PROCESSING)
                .updateContentProcessingStatus(Offer.ContentProcessingStatus.CONTENT_PROCESSING)
                .setContentProcessingTaskId(CONTENT_PROCESSING_TASK_ID_1))
            .endStep()


            .step("ir updates content processing task")
            .action(irUpdatesContentProcessingTask(CONTENT_PROCESSING_TASK_ID_1, ContentProcessingTask.State.ERROR))
            .expectedState(scenario.previousValidState()
                .updateProcessingStatusIfValid(Offer.ProcessingStatus.NEED_CONTENT)
                .updateContentProcessingStatus(Offer.ContentProcessingStatus.NEED_CONTENT)
                .setContentComments(new ContentComment(ContentCommentType.INCORRECT_MODEL)))
            .endStep()


            .step("ir started processing other sku from supplier")
            .action(irUpdatesContentProcessingTask(CONTENT_PROCESSING_TASK_ID_2,
                ContentProcessingTask.State.STARTED))
            .expectedState(scenario.previousValidState()
                .updateProcessingStatusIfValid(Offer.ProcessingStatus.CONTENT_PROCESSING)
                .updateContentProcessingStatus(Offer.ContentProcessingStatus.CONTENT_PROCESSING)
                .setContentProcessingTaskId(CONTENT_PROCESSING_TASK_ID_2))
            .endStep()


            .step("ir sends approved mapping")
            .action(irSendsNewMapping(MARKET_SKU_ID_2, "some login"))
            .addChecks(List.of(
                verifyEnqueued(ytOfferUploadQueueService),
                verifyEnqueued(mdmOfferUploadQueueService)
            ))
            .expectedState(scenario.previousValidState()
                .setModelId(MODEL_PARENT_ID)
                .setVendorId(OfferTestUtils.TEST_VENDOR_ID)
                .setBindingKind(Offer.BindingKind.APPROVED)
                .setSupplierSkuMapping(MAPPING_2)
                .setSupplierSkuMappingStatus(Offer.MappingStatus.ACCEPTED)
                .setSupplierSkuMappingCheckLogin("auto-accepted")
                .setSupplierSkuMappingCheckTs(LocalDateTime.now())
                .setGutginSkuMapping(MAPPING_2)
                .approve(Offer.MappingType.SUPPLIER, Offer.MappingConfidence.PARTNER_SELF)
                .updateProcessingStatusIfValid(Offer.ProcessingStatus.AUTO_PROCESSED)
                .updateContentProcessingStatus(Offer.ContentProcessingStatus.PROCESSED))
            .endStep()

            .step("ir updates content processing task")
            .action(irUpdatesContentProcessingTask(CONTENT_PROCESSING_TASK_ID_2,
                ContentProcessingTask.State.SUCCESS))
            .expectedState(scenario.previousValidState())
            .endStep()

            .execute();
    }


    @Test
    public void supplierCreatesTwoSkusThenFirstAcceptedThenSecondRejected() {
        GenericScenario<Offer> scenario = createScenario();
        scenario
            .step("import offer form excel")
            .action(importOfferFromExcel())
            .expectedState(startingOffer())
            .endStep()

            .step("thumbs up in UI (with knowledge)")
            .action(catmanUpdatesAcceptanceStatus(Offer.AcceptanceStatus.OK, true))
            .expectedState(scenario.previousValidState()
                .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
                .updateProcessingStatusIfValid(Offer.ProcessingStatus.IN_CLASSIFICATION)
                .incrementProcessingCounter())
            .endStep()

            .step("create classification ticket")
            .action(tmsCreateTrackerTickets())
            .expectedState(scenario.previousValidState())
            .endStep()

            .step("get category from classification and move to NEED_CONTENT")
            .action(operatorClassifiesOfferByFile(OfferTestUtils.TEST_CATEGORY_INFO_ID)
                .andThen(tmsProcessClassificationTicketsResults()))
            .expectedState(scenario.previousValidState()
                .setMappedCategoryId(OfferTestUtils.TEST_CATEGORY_INFO_ID, Offer.MappingConfidence.CONTENT)
                .setBindingKind(Offer.BindingKind.APPROVED)
                .setContentCategoryMappingId(OfferTestUtils.TEST_CATEGORY_INFO_ID)
                .setContentCategoryMappingStatus(Offer.MappingStatus.NEW)
                .updateProcessingStatusIfValid(Offer.ProcessingStatus.NEED_CONTENT)
                .updateContentProcessingStatus(Offer.ContentProcessingStatus.NEED_CONTENT))
            .endStep()

            .step("supplier can't find mapping and gives up")
            .action(supplierDoesNotKnowMapping())
            .expectedState(scenario.previousValidState()
                .updateProcessingStatusIfValid(Offer.ProcessingStatus.NEED_CONTENT)
                .updateContentProcessingStatus(Offer.ContentProcessingStatus.NEED_CONTENT))
            .endStep()

            .step("ir started processing sku from supplier")
            .action(irUpdatesContentProcessingTask(CONTENT_PROCESSING_TASK_ID_1,
                ContentProcessingTask.State.STARTED))
            .expectedState(scenario.previousValidState()
                .updateProcessingStatusIfValid(Offer.ProcessingStatus.CONTENT_PROCESSING)
                .updateContentProcessingStatus(Offer.ContentProcessingStatus.CONTENT_PROCESSING)
                .setContentProcessingTaskId(CONTENT_PROCESSING_TASK_ID_1))
            .endStep()

            .step("ir sends approved mapping")
            .action(irSendsNewMapping(MARKET_SKU_ID_1, "some login"))
            .addChecks(List.of(
                verifyEnqueued(ytOfferUploadQueueService),
                verifyEnqueued(mdmOfferUploadQueueService)
            ))
            .expectedState(scenario.previousValidState()
                .setModelId(MODEL_PARENT_ID)
                .setVendorId(OfferTestUtils.TEST_VENDOR_ID)
                .setBindingKind(Offer.BindingKind.APPROVED)
                .setSupplierSkuMapping(MAPPING_1)
                .setSupplierSkuMappingStatus(Offer.MappingStatus.ACCEPTED)
                .setSupplierSkuMappingCheckLogin("auto-accepted")
                .setSupplierSkuMappingCheckTs(LocalDateTime.now())
                .setGutginSkuMapping(MAPPING_1)
                .approve(Offer.MappingType.SUPPLIER, Offer.MappingConfidence.PARTNER_SELF)
                .updateProcessingStatusIfValid(Offer.ProcessingStatus.AUTO_PROCESSED)
                .updateContentProcessingStatus(Offer.ContentProcessingStatus.PROCESSED))
            .endStep()

            .step("ir updates content processing task")
            .action(irUpdatesContentProcessingTask(CONTENT_PROCESSING_TASK_ID_1,
                ContentProcessingTask.State.SUCCESS))
            .expectedState(scenario.previousValidState()
                .updateProcessingStatusIfValid(Offer.ProcessingStatus.AUTO_PROCESSED)
                .updateContentProcessingStatus(Offer.ContentProcessingStatus.PROCESSED))
            .endStep()

            .step("ir started processing other sku from supplier")
            .action(irUpdatesContentProcessingTask(CONTENT_PROCESSING_TASK_ID_2,
                ContentProcessingTask.State.STARTED))
            .expectedState(scenario.previousValidState()
                .updateProcessingStatusIfValid(Offer.ProcessingStatus.CONTENT_PROCESSING)
                .updateContentProcessingStatus(Offer.ContentProcessingStatus.CONTENT_PROCESSING)
                .setContentProcessingTaskId(CONTENT_PROCESSING_TASK_ID_2))
            .endStep()

            .step("ir updates content processing task")
            .action(irUpdatesContentProcessingTask(CONTENT_PROCESSING_TASK_ID_2, ContentProcessingTask.State.ERROR))
            .expectedState(scenario.previousValidState()
                .updateProcessingStatusIfValid(Offer.ProcessingStatus.AUTO_PROCESSED)
                .updateContentProcessingStatus(Offer.ContentProcessingStatus.PROCESSED)
                .setContentComments(new ContentComment(ContentCommentType.INCORRECT_MODEL)))
            .endStep()

            .execute();
    }

    @Test
    public void supplierCreatesNewSkuThenItIsAcceptedByIRThenRemappedToMskuByIR() {
        GenericScenario<Offer> scenario = createScenario();
        scenario
            .step("import offer form excel")
            .action(importOfferFromExcel())
            .expectedState(startingOffer())
            .endStep()

            .step("thumbs up in UI (with knowledge)")
            .action(catmanUpdatesAcceptanceStatus(Offer.AcceptanceStatus.OK, true))
            .expectedState(scenario.previousValidState()
                .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
                .updateProcessingStatusIfValid(Offer.ProcessingStatus.IN_CLASSIFICATION)
                .incrementProcessingCounter())
            .endStep()

            .step("create classification ticket")
            .action(tmsCreateTrackerTickets())
            .expectedState(scenario.previousValidState())
            .endStep()

            .step("get category from classification and move to NEED_CONTENT")
            .action(operatorClassifiesOfferByFile(OfferTestUtils.TEST_CATEGORY_INFO_ID)
                .andThen(tmsProcessClassificationTicketsResults()))
            .expectedState(scenario.previousValidState()
                .setMappedCategoryId(OfferTestUtils.TEST_CATEGORY_INFO_ID, Offer.MappingConfidence.CONTENT)
                .setBindingKind(Offer.BindingKind.APPROVED)
                .setContentCategoryMappingId(OfferTestUtils.TEST_CATEGORY_INFO_ID)
                .setContentCategoryMappingStatus(Offer.MappingStatus.NEW)
                .updateProcessingStatusIfValid(Offer.ProcessingStatus.NEED_CONTENT)
                .updateContentProcessingStatus(Offer.ContentProcessingStatus.NEED_CONTENT))
            .endStep()

            .step("supplier can't find mapping and gives up")
            .action(supplierDoesNotKnowMapping())
            .expectedState(scenario.previousValidState()
                .updateProcessingStatusIfValid(Offer.ProcessingStatus.NEED_CONTENT)
                .updateContentProcessingStatus(Offer.ContentProcessingStatus.NEED_CONTENT))
            .endStep()

            .step("ir started processing sku from supplier")
            .action(irUpdatesContentProcessingTask(CONTENT_PROCESSING_TASK_ID_1,
                ContentProcessingTask.State.STARTED))
            .expectedState(scenario.previousValidState()
                .updateProcessingStatusIfValid(Offer.ProcessingStatus.CONTENT_PROCESSING)
                .updateContentProcessingStatus(Offer.ContentProcessingStatus.CONTENT_PROCESSING)
                .setContentProcessingTaskId(CONTENT_PROCESSING_TASK_ID_1))
            .endStep()

            .step("ir sends approved mapping")
            .action(irSendsNewMapping(MARKET_SKU_ID_1, "some login"))
            .addChecks(List.of(
                verifyEnqueued(ytOfferUploadQueueService),
                verifyEnqueued(mdmOfferUploadQueueService)
            ))
            .expectedState(scenario.previousValidState()
                .setModelId(MODEL_PARENT_ID)
                .setVendorId(OfferTestUtils.TEST_VENDOR_ID)
                .setBindingKind(Offer.BindingKind.APPROVED)
                .setSupplierSkuMapping(MAPPING_1)
                .setSupplierSkuMappingStatus(Offer.MappingStatus.ACCEPTED)
                .setSupplierSkuMappingCheckLogin("auto-accepted")
                .setSupplierSkuMappingCheckTs(LocalDateTime.now())
                .setGutginSkuMapping(MAPPING_1)
                .approve(Offer.MappingType.SUPPLIER, Offer.MappingConfidence.PARTNER_SELF)
                .updateProcessingStatusIfValid(Offer.ProcessingStatus.AUTO_PROCESSED)
                .updateContentProcessingStatus(Offer.ContentProcessingStatus.PROCESSED))
            .endStep()

            .step("ir updates content processing task")
            .action(irUpdatesContentProcessingTask(CONTENT_PROCESSING_TASK_ID_1,
                ContentProcessingTask.State.SUCCESS))
            .expectedState(scenario.previousValidState()
                .updateProcessingStatusIfValid(Offer.ProcessingStatus.AUTO_PROCESSED)
                .updateContentProcessingStatus(Offer.ContentProcessingStatus.PROCESSED))
            .endStep()

            .step("ir PPP remaps offer to msku")
            .action(irPPPRemapsOffer(MARKET_SKU_ID_2, "some login", SKU_REMOVAL_DUPLICATE))
            .expectedState(scenario.previousValidState()
                .setSupplierSkuMappingStatus(Offer.MappingStatus.REJECTED)
                .setSupplierSkuMappingCheckLogin("some login")
                .setContentSkuMapping(MAPPING_2)
                .updateApprovedSkuMapping(MAPPING_2, Offer.MappingConfidence.CONTENT)
                .updateProcessingStatusIfValid(Offer.ProcessingStatus.PROCESSED)
                .updateContentProcessingStatus(Offer.ContentProcessingStatus.NONE))
            .endStep()

            .execute();
    }

    @Test
    public void offerIsProcessedThenSupplierPassesZeroAsMappingThenAddNewMapping() {
        GenericScenario<Offer> scenario = createScenario();
        scenario
            .step("import offer form excel")
            .action(importOfferFromExcel())
            .expectedState(startingOffer())
            .endStep()

            .step("thumbs up in UI (with knowledge)")
            .action(catmanUpdatesAcceptanceStatus(Offer.AcceptanceStatus.OK, true))
            .expectedState(scenario.previousValidState()
                .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
                .updateProcessingStatusIfValid(Offer.ProcessingStatus.IN_CLASSIFICATION)
                .incrementProcessingCounter())
            .endStep()

            .step("create classification ticket")
            .action(tmsCreateTrackerTickets())
            .expectedState(scenario.previousValidState())
            .endStep()

            .step("get category from classification and move to NEED_CONTENT")
            .action(operatorClassifiesOfferByFile(OfferTestUtils.TEST_CATEGORY_INFO_ID)
                .andThen(tmsProcessClassificationTicketsResults()))
            .expectedState(scenario.previousValidState()
                .setMappedCategoryId(OfferTestUtils.TEST_CATEGORY_INFO_ID, Offer.MappingConfidence.CONTENT)
                .setBindingKind(Offer.BindingKind.APPROVED)
                .setContentCategoryMappingId(OfferTestUtils.TEST_CATEGORY_INFO_ID)
                .setContentCategoryMappingStatus(Offer.MappingStatus.NEW)
                .updateProcessingStatusIfValid(Offer.ProcessingStatus.NEED_CONTENT)
                .updateContentProcessingStatus(Offer.ContentProcessingStatus.NEED_CONTENT))
            .endStep()

            .step("supplier sends mapping")
            .action(supplierSendsNewMapping(MARKET_SKU_ID_1))
            .expectedState(scenario.previousValidState()
                .updateProcessingStatusIfValid(Offer.ProcessingStatus.IN_MODERATION)
                .updateContentProcessingStatus(Offer.ContentProcessingStatus.NONE)
                .setSupplierSkuMapping(MAPPING_1)
                .setSupplierSkuMappingStatus(Offer.MappingStatus.NEW)
                .incrementProcessingCounter())
            .endStep()

            .step("operator accepted mapping")
            .action(operatorModeratesMapping(SupplierMappingModerationResult.ACCEPTED, MARKET_SKU_ID_1))
            .addChecks(List.of(
                verifyEnqueued(ytOfferUploadQueueService),
                verifyEnqueued(mdmOfferUploadQueueService)
            ))
            .expectedState(scenario.previousValidState()
                .setLastPrimaryProcessingStatus(Offer.ProcessingStatus.IN_MODERATION)
                .setBindingKind(Offer.BindingKind.APPROVED)
                .updateProcessingStatusIfValid(Offer.ProcessingStatus.PROCESSED)
                .updateContentProcessingStatus(Offer.ContentProcessingStatus.NONE)
                .setContentSkuMapping(MAPPING_1)
                .approve(Offer.MappingType.CONTENT, Offer.MappingConfidence.CONTENT)
                .setSupplierSkuMappingStatus(Offer.MappingStatus.ACCEPTED,
                    YANG_OPERATOR_LOGIN, DateTimeUtils.dateTimeNow())
                .setModelId(MODEL_PARENT_ID)
                .setVendorId(OfferTestUtils.TEST_VENDOR_ID))
            .endStep()

            .step("supplier sends zero")
            .action(supplierSendsNewMapping(0))
            .expectedState(
                scenario.previousValidState()
                    .updateProcessingStatusIfValid(Offer.ProcessingStatus.PROCESSED)
                    .updateContentProcessingStatus(Offer.ContentProcessingStatus.NONE)
                    .setSupplierSkuMapping(new Offer.Mapping(0, DateTimeUtils.dateTimeNow()))
                    .setSupplierSkuMappingStatus(Offer.MappingStatus.NONE, null, null)
            )
            .endStep()

            .step("supplier sends other mapping")
            .action(supplierSendsNewMapping(MARKET_SKU_ID_2))
            .expectedState(scenario.previousValidState()
                .updateProcessingStatusIfValid(Offer.ProcessingStatus.IN_RE_SORT)
                .updateContentProcessingStatus(Offer.ContentProcessingStatus.NONE)
                .setSupplierSkuMapping(new Offer.Mapping(MARKET_SKU_ID_2, DateTimeUtils.dateTimeNow()))
                .setSupplierSkuMappingStatus(Offer.MappingStatus.RE_SORT))
            .endStep()

            .execute();
    }

    @Test
    public void offerGoesFromNeedInfoModerationResultThenNegativeOnePassedPassed() {
        GenericScenario<Offer> scenario = createScenario();
        scenario
            .step("import offer form excel")
            .action(importOfferFromExcel())
            .expectedState(startingOffer())
            .endStep()

            .step("simulating APPROVED category from UC")
            .action(offerGetsConfidentCategoryFromUc(OfferTestUtils.TEST_CATEGORY_INFO_ID))
            .expectedState(scenario.previousValidState()
                .setCategoryIdForTests(OfferTestUtils.TEST_CATEGORY_INFO_ID, Offer.BindingKind.APPROVED)
                .setClassifierCategoryId(OfferTestUtils.TEST_CATEGORY_INFO_ID,
                    OfferCategoryRestrictionCalculator.DEFAULT_CLASSIFIER_TRUST_THRESHOLD))
            .endStep()

            .step("thumbs up in UI (with knowledge)")
            .action(catmanUpdatesAcceptanceStatus(Offer.AcceptanceStatus.OK, true))
            .expectedState(scenario.previousValidState()
                .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
                .updateProcessingStatusIfValid(Offer.ProcessingStatus.NEED_CONTENT)
                .updateContentProcessingStatus(Offer.ContentProcessingStatus.NEED_CONTENT))
            .endStep()

            .step("supplier sends mapping")
            .action(supplierSendsNewMapping(MARKET_SKU_ID_1))
            .expectedState(scenario.previousValidState()
                .updateProcessingStatusIfValid(Offer.ProcessingStatus.IN_MODERATION)
                .updateContentProcessingStatus(Offer.ContentProcessingStatus.NONE)
                .setSupplierSkuMapping(MAPPING_1)
                .setSupplierSkuMappingStatus(Offer.MappingStatus.NEW)
                .incrementProcessingCounter())
            .endStep()

            .step("operator sets need_info")
            .action(operatorModeratesMapping(SupplierMappingModerationResult.NEED_INFO, MARKET_SKU_ID_1))
            .expectedState(scenario.previousValidState()
                .updateProcessingStatusIfValid(Offer.ProcessingStatus.NEED_INFO)
                .setLastPrimaryProcessingStatus(Offer.ProcessingStatus.IN_MODERATION))
            .endStep()

            .step("supplier sends -1 mapping")
            .action(supplierSendsNewMapping(-1))
            .expectedState(scenario.previousValidState()
                .updateProcessingStatusIfValid(Offer.ProcessingStatus.NEED_CONTENT)
                .updateContentProcessingStatus(Offer.ContentProcessingStatus.NEED_CONTENT)
                .setSupplierSkuMapping(null)
                .setSupplierSkuMappingStatus(Offer.MappingStatus.NONE, null, null))
            .endStep()

            .execute();
    }

    @Test
    public void supplierProvidesMappingThenOperatorNeedsInfoWithLegalConflictComment() {
        GenericScenario<Offer> scenario = createScenario();
        scenario
            .step("import offer form excel")
            .action(importOfferFromExcel())
            .expectedState(startingOffer())
            .endStep()

            .step("thumbs up in UI (with knowledge)")
            .action(catmanUpdatesAcceptanceStatus(Offer.AcceptanceStatus.OK, true))
            .expectedState(scenario.previousValidState()
                .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
                .updateProcessingStatusIfValid(Offer.ProcessingStatus.IN_CLASSIFICATION)
                .incrementProcessingCounter())
            .endStep()

            .step("create classification ticket")
            .action(tmsCreateTrackerTickets())
            .expectedState(scenario.previousValidState())
            .endStep()

            .step("get category from classification and move to NEED_CONTENT")
            .action(operatorClassifiesOfferByFile(OfferTestUtils.TEST_CATEGORY_INFO_ID)
                .andThen(tmsProcessClassificationTicketsResults()))
            .expectedState(scenario.previousValidState()
                .setMappedCategoryId(OfferTestUtils.TEST_CATEGORY_INFO_ID, Offer.MappingConfidence.CONTENT)
                .setBindingKind(Offer.BindingKind.APPROVED)
                .setContentCategoryMappingId(OfferTestUtils.TEST_CATEGORY_INFO_ID)
                .setContentCategoryMappingStatus(Offer.MappingStatus.NEW)
                .updateProcessingStatusIfValid(Offer.ProcessingStatus.NEED_CONTENT)
                .updateContentProcessingStatus(Offer.ContentProcessingStatus.NEED_CONTENT))
            .endStep()

            .step("supplier sends mapping")
            .action(supplierSendsNewMapping(MARKET_SKU_ID_1))
            .expectedState(scenario.previousValidState()
                .updateProcessingStatusIfValid(Offer.ProcessingStatus.IN_MODERATION)
                .updateContentProcessingStatus(Offer.ContentProcessingStatus.NONE)
                .setSupplierSkuMapping(MAPPING_1)
                .setSupplierSkuMappingStatus(Offer.MappingStatus.NEW)
                .incrementProcessingCounter())
            .endStep()

            .step("operator sets need_info")
            .action(operatorModeratesMapping(SupplierMappingModerationResult.NEED_INFO, MARKET_SKU_ID_1,
                ContentCommentType.LEGAL_CONFLICT))
            .expectedState(scenario.previousValidState()
                .setLastPrimaryProcessingStatus(Offer.ProcessingStatus.IN_MODERATION)
                .updateProcessingStatusIfValid(Offer.ProcessingStatus.LEGAL_PROBLEM)
                .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
                .setContentComments(new ContentComment(ContentCommentType.LEGAL_CONFLICT)))
            .endStep()

            .execute();
    }

    @Test
    public void supplierProvidesMappingThenOperatorNeedsInfoWithWrongCategoryComment() {
        GenericScenario<Offer> scenario = createScenario();
        scenario
            .step("import offer form excel")
            .action(importOfferFromExcel())
            .expectedState(startingOffer())
            .endStep()

            .step("thumbs up in UI (with knowledge)")
            .action(catmanUpdatesAcceptanceStatus(Offer.AcceptanceStatus.OK, true))
            .expectedState(scenario.previousValidState()
                .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
                .updateProcessingStatusIfValid(Offer.ProcessingStatus.IN_CLASSIFICATION)
                .incrementProcessingCounter())
            .endStep()

            .step("create classification ticket")
            .action(tmsCreateTrackerTickets())
            .expectedState(scenario.previousValidState())
            .endStep()

            .step("get category from classification and move to NEED_CONTENT")
            .action(operatorClassifiesOfferByFile(OfferTestUtils.TEST_CATEGORY_INFO_ID)
                .andThen(tmsProcessClassificationTicketsResults()))
            .expectedState(scenario.previousValidState()
                .setMappedCategoryId(OfferTestUtils.TEST_CATEGORY_INFO_ID, Offer.MappingConfidence.CONTENT)
                .setBindingKind(Offer.BindingKind.APPROVED)
                .setContentCategoryMappingId(OfferTestUtils.TEST_CATEGORY_INFO_ID)
                .setContentCategoryMappingStatus(Offer.MappingStatus.NEW)
                .updateProcessingStatusIfValid(Offer.ProcessingStatus.NEED_CONTENT)
                .updateContentProcessingStatus(Offer.ContentProcessingStatus.NEED_CONTENT))
            .endStep()

            .step("supplier sends mapping")
            .action(supplierSendsNewMapping(MARKET_SKU_ID_1))
            .expectedState(scenario.previousValidState()
                .updateProcessingStatusIfValid(Offer.ProcessingStatus.IN_MODERATION)
                .updateContentProcessingStatus(Offer.ContentProcessingStatus.NONE)
                .setSupplierSkuMapping(MAPPING_1)
                .setSupplierSkuMappingStatus(Offer.MappingStatus.NEW)
                .incrementProcessingCounter())
            .endStep()

            .step("operator sets need_info")
            .action(operatorModeratesMapping(SupplierMappingModerationResult.NEED_INFO, MARKET_SKU_ID_1,
                ContentCommentType.WRONG_CATEGORY))
            .expectedState(scenario.previousValidState()
                .setLastPrimaryProcessingStatus(Offer.ProcessingStatus.IN_MODERATION)
                .updateProcessingStatusIfValid(Offer.ProcessingStatus.NEED_CONTENT)
                .updateContentProcessingStatus(Offer.ContentProcessingStatus.NEED_CONTENT)
                .setContentComments(new ContentComment(ContentCommentType.WRONG_CATEGORY))
                .setSupplierSkuMappingStatus(Offer.MappingStatus.REJECTED)
                .setSupplierSkuMappingCheckTs(DateTimeUtils.dateTimeNow())
                .setSupplierSkuMappingCheckLogin(YANG_OPERATOR_LOGIN))
            .endStep()

            .execute();
    }

    private GenericScenario<Offer> createScenario() {
        return new GenericScenario<>(
            offerInRepoIsValid(),
            Offer::copy
        );
    }

    protected BiConsumer<String, Offer> verifyNotEnqueued(OfferUploadQueueService queueService) {
        return (description, offer) -> {
            var offerDb = offerRepository.findOfferByBusinessSkuKey(offer.getBusinessSkuKey());
            Assertions.assertThat(queueService.getForUpload(10))
                .extracting(OfferUploadQueueItem::getOfferId)
                .doesNotContain(offerDb.getId());
        };
    }

    protected BiConsumer<String, Offer> verifyEnqueued(OfferUploadQueueService queueService) {
        return (description, offer) -> {
            var offerDb = offerRepository.findOfferByBusinessSkuKey(offer.getBusinessSkuKey());
            Assertions.assertThat(queueService.getForUpload(10))
                .extracting(OfferUploadQueueItem::getOfferId)
                .contains(offerDb.getId());
        };
    }
}
