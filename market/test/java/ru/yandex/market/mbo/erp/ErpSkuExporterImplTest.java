package ru.yandex.market.mbo.erp;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import com.google.common.collect.Lists;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import ru.yandex.market.mbo.db.modelstorage.ModelStorageService;
import ru.yandex.market.mbo.db.modelstorage.index.MboIndexesFilter;
import ru.yandex.market.mbo.db.modelstorage.yt.indexes.payload.ModelIndexPayload;
import ru.yandex.market.mbo.db.vendor.GlobalVendorService;
import ru.yandex.market.mbo.erp.dao.ErpExporter;
import ru.yandex.market.mbo.erp.model.ErpSku;
import ru.yandex.market.mbo.export.CategoryModelsService;
import ru.yandex.market.mbo.export.MboExport;
import ru.yandex.market.mbo.export.modelstorage.pipe.IsSkuModelPipePart;
import ru.yandex.market.mbo.export.modelstorage.pipe.ModelPipeContext;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.vendor.GlobalVendor;
import ru.yandex.market.mbo.gwt.models.visual.Word;
import ru.yandex.market.mbo.gwt.utils.XslNames;
import ru.yandex.market.mbo.http.ModelStorage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author yuramalinov
 * @created 04.06.18
 */
@SuppressWarnings("checkstyle:magicnumber")
public class ErpSkuExporterImplTest {

    private ErpExporter erpExporter;
    private CategoryModelsService modelsService;
    private ModelStorageService modelStorageService;
    private GlobalVendorService globalVendorService;
    private ErpSkuExporterImpl exporter;

    @Before
    public void setup() {
        erpExporter = mock(ErpExporter.class);
        modelsService = mock(CategoryModelsService.class);
        modelStorageService = mock(ModelStorageService.class);
        globalVendorService = mock(GlobalVendorService.class);
        exporter = new ErpSkuExporterImpl(
            erpExporter, modelsService, modelStorageService, globalVendorService);
    }

    @Test
    public void testEmptyRun() {
        // Моки вернут пустоту, мы отработаем, как если не нашлось моделей
        MboErpExporter.ExportSkuResponse response = exporter.exportSku(
            MboErpExporter.ExportSkuRequest.newBuilder().addSkuId(123L).build());

        assertEquals(1, response.getExportResultsCount());
        assertFalse(response.getExportResults(0).getOk());
        Assertions.assertThat(response.getExportResults(0).getMessage()).containsIgnoringCase("not found");
    }

    @Test
    public void testNullInVendorName() {
        Mockito.doAnswer(call -> {
            Consumer<ModelIndexPayload> callback = call.getArgument(1);
            callback.accept(new ModelIndexPayload(123, 12300L, null));
            return null;
        })
            .doAnswer(call -> null)
            .when(modelStorageService).processQueryIndexModels(any(), any());

        when(modelsService.getModels(any())).thenAnswer(call -> {
            MboExport.GetCategoryModelsRequest request = call.getArgument(0);
            MboExport.GetCategoryModelsResponse.Builder response = MboExport.GetCategoryModelsResponse.newBuilder();
            request.getModelIdList().forEach(id -> {
                response.addModels(ModelStorage.Model.newBuilder()
                    .setId(id)
                    .setCategoryId(request.getCategoryId())
                    .setVendorId(42)
                    .setCurrentType(CommonModel.Source.SKU.name())
                    .setPublished(true)
                    .setPublishedOnMarket(true)
                    .addPictures(ModelStorage.Picture.newBuilder().setUrl("http://test.it").build())
                    .addParameterValues(paramStringValue(XslNames.VENDOR_CODE, "Test code"))
                    .build());
            });
            return response.build();
        });

        when(globalVendorService.loadVendors(anyCollection())).thenReturn(Collections.emptyList());

        exporter.exportSku(MboErpExporter.ExportSkuRequest.newBuilder().addSkuId(123L).build());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ErpSku>> captor = ArgumentCaptor.forClass(List.class);
        verify(erpExporter).writeSkuBatchInTransaction(captor.capture());

        ErpSku erpSku = captor.getValue().get(0);
        assertEquals(null, erpSku.getVendorName());
    }

    @Test
    public void testOkRun() {
        Mockito.doAnswer(call -> {
            Consumer<ModelIndexPayload> callback = call.getArgument(1);
            callback.accept(new ModelIndexPayload(123, 12300L, null));
            return null;
        })
            .doAnswer(call -> null)
            .when(modelStorageService).processQueryIndexModels(any(MboIndexesFilter.class), any());

        when(modelsService.getModels(any())).thenAnswer(call -> {
            MboExport.GetCategoryModelsRequest request = call.getArgument(0);
            MboExport.GetCategoryModelsResponse.Builder response = MboExport.GetCategoryModelsResponse.newBuilder();
            request.getModelIdList().forEach(id -> {
                response.addModels(ModelStorage.Model.newBuilder()
                    .setId(id)
                    .setCategoryId(request.getCategoryId())
                    .setVendorId(42)
                    .setCurrentType(CommonModel.Source.SKU.name())
                    .setPublished(true)
                    .setPublishedOnMarket(true)
                    .addPictures(ModelStorage.Picture.newBuilder().setUrl("http://test.it").build())
                    .addParameterValues(paramStringValue(XslNames.VENDOR_CODE, "Test code"))
                    .addParameterValues(paramValue("packageDepth", "1"))
                    .addParameterValues(paramValue("packageWidth", "2"))
                    .addParameterValues(paramValue("packageHeight", "3"))
                    .addParameterValues(paramValue("packageWeight", "4"))
                    .build());
            });
            return response.build();
        });

        GlobalVendor vendor = new GlobalVendor();
        vendor.setId(42);
        vendor.setNames(Collections.singletonList(new Word(Word.DEFAULT_LANG_ID, "Test vendor")));
        when(globalVendorService.loadVendors(anyCollection())).thenReturn(Lists.newArrayList(vendor));

        MboErpExporter.ExportSkuResponse response = exporter.exportSku(
            MboErpExporter.ExportSkuRequest.newBuilder().addSkuId(123L).build());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ErpSku>> captor = ArgumentCaptor.forClass(List.class);

        verify(erpExporter).writeSkuBatchInTransaction(captor.capture());

        Assertions.assertThat(response.getExportResultsCount()).isEqualTo(1);
        Assertions.assertThat(response.getExportResults(0).getOk()).isTrue();
        Assertions.assertThat(captor.getValue()).hasSize(1);
        ErpSku erpSku = captor.getValue().get(0);
        assertEquals(12300, erpSku.getCategoryId());
        assertEquals(123, erpSku.getMskuId());
        assertEquals("Test code", erpSku.getVendorCode());
        assertEquals("Test vendor", erpSku.getVendorName());
        assertTrue(erpSku.isPublished());
        assertEquals("http://test.it", erpSku.getPictureUrl());
        assertEquals(new BigDecimal(1), erpSku.getPackageDepth());
        assertEquals(new BigDecimal(2), erpSku.getPackageWidth());
        assertEquals(new BigDecimal(3), erpSku.getPackageHeight());
        assertEquals(new BigDecimal(4), erpSku.getPackageWeight());
        assertEquals(42, erpSku.getVendorId());
    }

    @Test
    public void testWrongModel() {
        Mockito
            .doAnswer(call -> {
                Consumer<ModelIndexPayload> callback = call.getArgument(1);
                callback.accept(new ModelIndexPayload(123, 100, null));
                return null;
            })
            .doAnswer(call -> null)
            .when(modelStorageService).processQueryIndexModels(any(MboIndexesFilter.class), any());

        when(modelsService.getModels(any())).thenAnswer(call -> {
            MboExport.GetCategoryModelsRequest request = call.getArgument(0);
            MboExport.GetCategoryModelsResponse.Builder response = MboExport.GetCategoryModelsResponse.newBuilder();
            request.getModelIdList().forEach(id -> {
                response.addModels(ModelStorage.Model.newBuilder()
                    .setId(id)
                    .setCategoryId(request.getCategoryId())
                    .setVendorId(42)
                    .setCurrentType("MODEL")
                    .setPublished(true)
                    .setPublishedOnMarket(true)
                    .addPictures(ModelStorage.Picture.newBuilder().setUrl("http://test.it").build())
                    .addParameterValues(paramStringValue(XslNames.VENDOR_CODE, "Test code"))
                    .addParameterValues(paramValue("packageDepth", "1"))
                    .addParameterValues(paramValue("packageWidth", "2"))
                    .addParameterValues(paramValue("packageHeight", "3"))
                    .addParameterValues(paramValue("packageWeight", "4"))
                    .build());
            });
            return response.build();
        });

        GlobalVendor vendor = new GlobalVendor();
        vendor.setId(42);
        vendor.setNames(Collections.singletonList(new Word(Word.DEFAULT_LANG_ID, "Test vendor")));
        when(globalVendorService.loadVendors(anyCollection())).thenReturn(Lists.newArrayList(vendor));

        MboErpExporter.ExportSkuResponse response = exporter.exportSku(
            MboErpExporter.ExportSkuRequest.newBuilder().addSkuId(123L).build());

        assertEquals(1, response.getExportResultsCount());
        MboErpExporter.ExportSkuResult result = response.getExportResults(0);
        assertFalse(result.getOk());
        assertEquals("SKU not processed.", result.getMessage());
    }

    @Test
    public void testExtractGuruWillFail() {
        Mockito
            .doAnswer(call -> null)
            .doAnswer(call -> null)
            .when(modelStorageService).processQueryIndexModels(any(MboIndexesFilter.class), any());

        when(modelsService.getModels(any())).thenAnswer(call -> {
            MboExport.GetCategoryModelsRequest request = call.getArgument(0);
            MboExport.GetCategoryModelsResponse.Builder response = MboExport.GetCategoryModelsResponse.newBuilder();
            request.getModelIdList().forEach(id -> {
                response.addModels(ModelStorage.Model.newBuilder()
                    .setId(id)
                    .setCategoryId(request.getCategoryId())
                    .setVendorId(42)
                    .setCurrentType(CommonModel.Source.GURU.name())
                    .setPublished(true)
                    .setPublishedOnMarket(true)
                    .addPictures(ModelStorage.Picture.newBuilder().setUrl("http://test.it").build())
                    .addParameterValues(paramStringValue(XslNames.VENDOR_CODE, "Test code"))
                    .build());
            });
            return response.build();
        });

        GlobalVendor vendor = new GlobalVendor();
        vendor.setId(42);
        vendor.setNames(Collections.singletonList(new Word(Word.DEFAULT_LANG_ID, "Test vendor")));
        when(globalVendorService.loadVendors(anyCollection())).thenReturn(Lists.newArrayList(vendor));

        MboErpExporter.ExportSkuResponse response = exporter.exportSku(
            MboErpExporter.ExportSkuRequest.newBuilder().addSkuId(321).build());

        assertEquals(1, response.getExportResultsCount());
        MboErpExporter.ExportSkuResult result = response.getExportResults(0);
        assertFalse(result.getOk());
        assertEquals("Sku not found. " +
            "Probably not in type: SKU, PARTNER_SKU or GURU with param IsSku equals true.", result.getMessage());
    }

    @Test
    public void testExtractGuruWithIsSku() {
        Mockito
            .doAnswer(call -> null)
            .doAnswer(call -> {
                Consumer<ModelIndexPayload> callback = call.getArgument(1);
                callback.accept(new ModelIndexPayload(321, 100, null));
                return null;
            }).when(modelStorageService).processQueryIndexModels(any(MboIndexesFilter.class), any());


        when(modelsService.getModels(any())).thenAnswer(call -> {
            MboExport.GetCategoryModelsRequest request = call.getArgument(0);
            MboExport.GetCategoryModelsResponse.Builder response = MboExport.GetCategoryModelsResponse.newBuilder();
            request.getModelIdList().forEach(id -> {
                ModelStorage.Model model = ModelStorage.Model.newBuilder()
                    .setId(id)
                    .setCategoryId(request.getCategoryId())
                    .setVendorId(42)
                    .setCurrentType(CommonModel.Source.GURU.name())
                    .addParameterValues(ModelStorage.ParameterValue.newBuilder()
                        .setXslName(XslNames.IS_SKU).setBoolValue(true))
                    .setPublished(true)
                    .setPublishedOnMarket(true)
                    .addPictures(ModelStorage.Picture.newBuilder().setUrl("http://test.it").build())
                    .addParameterValues(paramStringValue(XslNames.VENDOR_CODE, "Test code"))
                    .build();

                ModelPipeContext context = new ModelPipeContext(model,
                    Collections.emptyList(), Collections.emptyList());
                IsSkuModelPipePart.INSTANCE.acceptModelsGroup(context);
                context.performForAll(response::addModels);
            });
            return response.build();
        });

        GlobalVendor vendor = new GlobalVendor();
        vendor.setId(42);
        vendor.setNames(Collections.singletonList(new Word(Word.DEFAULT_LANG_ID, "Test vendor")));
        when(globalVendorService.loadVendors(anyCollection())).thenReturn(Lists.newArrayList(vendor));

        MboErpExporter.ExportSkuResponse response = exporter.exportSku(
            MboErpExporter.ExportSkuRequest.newBuilder().addSkuId(321).build());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ErpSku>> captor = ArgumentCaptor.forClass(List.class);

        verify(erpExporter).writeSkuBatchInTransaction(captor.capture());

        Assertions.assertThat(response.getExportResultsCount()).isEqualTo(1);
        Assertions.assertThat(response.getExportResults(0).getOk()).isTrue();
        Assertions.assertThat(captor.getValue()).hasSize(1);
        ErpSku erpSku = captor.getValue().get(0);
        assertEquals(100L, erpSku.getCategoryId());
        assertEquals(321, erpSku.getMskuId());
        assertEquals("Test code", erpSku.getVendorCode());
        assertEquals("Test vendor", erpSku.getVendorName());
        assertTrue(erpSku.isPublished());
        assertEquals("http://test.it", erpSku.getPictureUrl());
        assertEquals(42, erpSku.getVendorId());
    }

    @Test
    public void testExtractPartnerSku() {
        Mockito.doAnswer(call -> {
            Consumer<ModelIndexPayload> callback = call.getArgument(1);
            callback.accept(new ModelIndexPayload(321, 100, null));
            return null;
        })
            .doAnswer(call -> null)
            .when(modelStorageService).processQueryIndexModels(any(MboIndexesFilter.class), any());

        when(modelsService.getModels(any())).thenAnswer(call -> {
            MboExport.GetCategoryModelsRequest request = call.getArgument(0);
            MboExport.GetCategoryModelsResponse.Builder response = MboExport.GetCategoryModelsResponse.newBuilder();
            request.getModelIdList().forEach(id -> {
                response.addModels(ModelStorage.Model.newBuilder()
                    .setId(id)
                    .setCategoryId(request.getCategoryId())
                    .setVendorId(42)
                    .setCurrentType(CommonModel.Source.PARTNER_SKU.name())
                    .setPublished(true)
                    .setPublishedOnMarket(true)
                    .addPictures(ModelStorage.Picture.newBuilder().setUrl("http://test.it").build())
                    .addParameterValues(paramStringValue(XslNames.VENDOR_CODE, "Test code"))
                    .build());
            });
            return response.build();
        });

        GlobalVendor vendor = new GlobalVendor();
        vendor.setId(42);
        vendor.setNames(Collections.singletonList(new Word(Word.DEFAULT_LANG_ID, "Test vendor")));
        when(globalVendorService.loadVendors(anyCollection())).thenReturn(Lists.newArrayList(vendor));

        MboErpExporter.ExportSkuResponse response = exporter.exportSku(
            MboErpExporter.ExportSkuRequest.newBuilder().addSkuId(321).build());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ErpSku>> captor = ArgumentCaptor.forClass(List.class);

        verify(erpExporter).writeSkuBatchInTransaction(captor.capture());

        Assertions.assertThat(response.getExportResultsCount()).isEqualTo(1);
        Assertions.assertThat(response.getExportResults(0).getOk()).isTrue();
        Assertions.assertThat(captor.getValue()).hasSize(1);
        ErpSku erpSku = captor.getValue().get(0);
        assertEquals(100L, erpSku.getCategoryId());
        assertEquals(321, erpSku.getMskuId());
        assertEquals("Test code", erpSku.getVendorCode());
        assertEquals("Test vendor", erpSku.getVendorName());
        assertTrue(erpSku.isPublished());
        assertEquals("http://test.it", erpSku.getPictureUrl());
        assertEquals(42, erpSku.getVendorId());
    }

    private ModelStorage.ParameterValue.Builder paramValue(String name, String value) {
        return ModelStorage.ParameterValue.newBuilder().setXslName(name).setNumericValue(value);
    }

    private ModelStorage.ParameterValue.Builder paramStringValue(String name, String value) {
        return ModelStorage.ParameterValue.newBuilder()
            .setXslName(name)
            .addStrValue(ModelStorage.LocalizedString.newBuilder().setIsoCode("RU").setValue(value));
    }
}
