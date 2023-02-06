package ru.yandex.market.markup2.tasks.logs_processing_old;

import ru.yandex.market.markup2.tasks.logs_processing_old.LogsProcessingResponse.Result;
import ru.yandex.market.markup2.utils.aliasmaker.ModelOffersAliases;
import ru.yandex.market.markup2.utils.aliasmaker.OfferAliasPair;
import ru.yandex.market.markup2.utils.param.ParamUtils;
import ru.yandex.market.markup2.utils.tovarTree.TovarTreeProvider;
import ru.yandex.market.markup2.utils.vendor.Vendor;
import ru.yandex.market.markup2.utils.vendor.VendorService;
import ru.yandex.market.markup2.workflow.general.TaskDataItem;
import ru.yandex.market.mbo.export.MboParameters;
import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.mbo.http.OffersStorage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static ru.yandex.market.markup2.utils.ParameterTestUtils.verifyModelParameter;

/**
 * @author V.Zaytsev (breezzo@yandex-team.ru)
 * @since 27.06.2017
 */
@SuppressWarnings("checkstyle:MagicNumber")
public class LogsProcessingTestCommon {

    public static final String CATEGORY_NAME = "Best category ever";
    public static final int CATEGORY_ID = 100500;
    public static final Long VENDOR_ID = 500L;

    private static final String CATEGORY_INSTRUCTION = "Do whatever needed";
    private static final String VENDOR_NAME = "Sharazh Montazh";
    private static final String VENDOR_SITE = "http://sh-mt.ru";

    private static final String URL_SHOP = "http://my.shop.ru";
    private static final String URL_VITAL = "http://best.vendor.ever";

    private LogsProcessingTestCommon() {
    }

    public static void commonMocks(
        LogsProcessingRequestGenerator generator,
        List<ModelStorage.Model> etalonModelList, VendorService vendorService,
        TovarTreeProvider tovarTreeProvider) {
        doAnswer(i -> {
            Vendor result = new Vendor();
            result.setId(VENDOR_ID);
            result.setName(VENDOR_NAME);
            result.setSite(VENDOR_SITE);
            return Optional.of(result);
        }).when(vendorService).getVendor(anyLong());

        doReturn(CATEGORY_INSTRUCTION).when(tovarTreeProvider).getTolokerInstructions(anyInt());

        doReturn(CATEGORY_NAME).when(generator).loadCategoryName(anyInt());



        LogsProcessingRequestGenerator.EtalonModels etalonModels = new LogsProcessingRequestGenerator.EtalonModels();
        String etalonStr = etalonModelList.stream()
            .map(etalon -> etalon.getTitles(0).getValue())
            .collect(Collectors.joining(","));
        etalonModels.add(VENDOR_ID, etalonStr);

        Map<Integer, LogsProcessingRequestGenerator.EtalonModels> etalonModelsMap = new HashMap<>();
        etalonModelsMap.put(CATEGORY_ID, etalonModels);

        doReturn(etalonModelsMap).when(generator).loadEtalonModels(anySet());
    }

    public static void verifyTask(TaskDataItem<LogsProcessingDataItemPayload, LogsProcessingResponse> item,
                           OffersStorage.GenerationDataOffer offer,
                           List<ModelStorage.Model> etalonModels) {

        LogsProcessingDataItemPayload payload = item.getInputData();
        LogsProcessingDataAttributes attributes = payload.getAttributes();

        assertEquals(CATEGORY_ID, attributes.getCategoryId());
        assertEquals(offer.getClassifierMagicId(), payload.getDataIdentifier().getOfferId());
        assertEquals(VENDOR_ID.longValue(), attributes.getGlobalVendorId());
        assertEquals(offer.getOffer(), attributes.getOfferTitle());
        assertEquals(offer.getDescription(), attributes.getOfferDescription());
        assertEquals(CATEGORY_NAME, attributes.getCategoryName());
        assertEquals(VENDOR_NAME, attributes.getVendorName());
        assertEquals(VENDOR_SITE, attributes.getVendorUrl());
        assertEquals(CATEGORY_INSTRUCTION, attributes.getInstruction());
        String ethalonStr = etalonModels.stream()
            .map(ethalon -> ethalon.getTitles(0).getValue())
            .collect(Collectors.joining(","));
        assertEquals(ethalonStr, attributes.getEtalonModels().stream().collect(Collectors.joining(",")));
    }

    public static void verifySavedModel(ModelStorage.Model model, String name) {
        assertEquals(CATEGORY_ID, model.getCategoryId());
        if (model.getId() == 0) {
            assertFalse(model.getPublished());
            assertEquals(ModelStorage.ModelType.TOLOKA.name(), model.getSourceType());
        }
        assertEquals(ModelStorage.ModelType.GURU.name(), model.getCurrentType());
        verifyModelParameter(model, ParamUtils.NAME_ID, "name",
            MboParameters.ValueType.STRING, name);
        verifyModelParameter(model, ParamUtils.VENDOR_ID, "vendor",
            MboParameters.ValueType.ENUM, VENDOR_ID.intValue());
        verifyModelParameter(model, ParamUtils.URL_ID, "url",
            MboParameters.ValueType.STRING, URL_VITAL);
        verifyModelParameter(model, ParamUtils.ADDITIONAL_URL_ID, "additional_url",
            MboParameters.ValueType.STRING, URL_SHOP);
    }

    public static void verifyModelOffersAliases(List<ModelOffersAliases> offersAliases, Long modelId,
                                         OfferAliasPair... offerAlias) {
        for (ModelOffersAliases candidate : offersAliases) {
            if (candidate.getModelId().equals(modelId)) {
                assertEquals(offerAlias.length, candidate.getOfferAliasPairs().size());
                for (OfferAliasPair expected : offerAlias) {
                    boolean found = false;
                    for (OfferAliasPair pair : candidate.getOfferAliasPairs()) {
                        if (expected.getOfferId().equals(pair.getOfferId()) &&
                            expected.getAlias().equals(pair.getAlias())) {
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        assertTrue("OfferAliasPair " + expected + " not found for model " + modelId, false);
                    }
                }
                return;
            }
        }
        assertTrue("ModelOffersAliases not found in model " + modelId, false);
    }

    public static List<TaskDataItem<LogsProcessingDataItemPayload, LogsProcessingResponse>> prepareResponseData() {
        List<TaskDataItem<LogsProcessingDataItemPayload, LogsProcessingResponse>> result = new ArrayList<>();

        LogsProcessingDataAttributes.Builder attributesBuilder = LogsProcessingDataAttributes.newBuilder()
            .setCategoryId(CATEGORY_ID)
            .setGlobalVendorId(VENDOR_ID);

        LogsProcessingDataItemPayload payload = payload("1", attributesBuilder);
        TaskDataItem<LogsProcessingDataItemPayload, LogsProcessingResponse> item = new TaskDataItem<>(1, payload);

        item.setResponseInfo(
            new LogsProcessingResponse(1, Result.ADD_NAME, "Qwerty", URL_SHOP, URL_VITAL, null, null));

        result.add(item);

        payload = payload("2", attributesBuilder);
        item = new TaskDataItem<>(2, payload);
        item.setResponseInfo(
            new LogsProcessingResponse(2, Result.ADD_NAME, "   Qwerty  ", URL_SHOP, URL_VITAL, null, null));
        result.add(item);

        payload = payload("3", attributesBuilder);
        item = new TaskDataItem<>(3, payload);
        item.setResponseInfo(
            new LogsProcessingResponse(3, Result.ADD_NAME, "Model 1 ", URL_SHOP, URL_VITAL, null, 1L));
        result.add(item);

        payload = payload("4", attributesBuilder);
        item = new TaskDataItem<>(4, payload);
        item.setResponseInfo(
            new LogsProcessingResponse(4, Result.ADD_NAME, "  Model   2!!!", URL_SHOP, URL_VITAL, null, 2L));
        result.add(item);

        return result;
    }

    private static LogsProcessingDataItemPayload payload(
        String offerId, LogsProcessingDataAttributes.Builder attributesBuilder) {
        return new LogsProcessingDataItemPayload(offerId, null, attributesBuilder.build());
    }
}

