package ru.yandex.market.logistics.iris.service.mbo;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.Executors;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.logistics.iris.entity.item.EmbeddableItemIdentifier;
import ru.yandex.market.logistics.iris.model.ItemIdentifierDTO;
import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.mboc.http.MboMappings;
import ru.yandex.market.mboc.http.MboMappingsServiceStub;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SupplierMappingServiceImplTest {
    
    private SupplierMappingServiceImpl service;

    @Test
    public void getMarketSkuMappingMultiSupplier() {
        MboMappings.SearchApprovedMappingsResponse response = getManyMappingResponse();
        service = getMockMappingService(response);

        final Map<ItemIdentifierDTO, SupplierContentMapping> mapping =
                service.getMarketSkuMapping(Arrays.asList(
                        new EmbeddableItemIdentifier("100500", "sku1"),
                        new EmbeddableItemIdentifier("100500", "sku2"),
                        new EmbeddableItemIdentifier("100500", "sku3")
                ));

        assertThat(mapping, notNullValue());
        assertThat(mapping.size(), equalTo(2));

        assertFirst(mapping.get(new ItemIdentifierDTO("100500", "sku1")));
        assertSecond(mapping.get(new ItemIdentifierDTO("100500", "sku2")));
    }

    @Test
    public void shouldConvertSpikeNumberToIntegerValue() {
        final ModelStorage.ParameterValue param = ModelStorage.ParameterValue.newBuilder()
                .setParamId(SupplierMappingService.PACKAGE_NUM_IN_SPIKE_PARAM_ID)
                .setNumericValue("15.0")
                .build();

        final MboMappings.SearchApprovedMappingsResponse response =
                MboMappings.SearchApprovedMappingsResponse.newBuilder()
                        .addMapping(MboMappings.ApprovedMappingInfo.newBuilder()
                                .setSupplierId(Integer.valueOf("100500"))
                                .setShopSku("sku1")
                                .addModelParam(param)
                                .build())
                        .build();

        service = getMockMappingService(response);

        final Map<ItemIdentifierDTO, SupplierContentMapping> mapping =
                service.getMarketSkuMapping(Arrays.asList(
                        new EmbeddableItemIdentifier("100500", "sku1")
                ));

        assertThat(mapping, notNullValue());
        assertThat(mapping.size(), equalTo(1));

        final SupplierContentMapping contentMapping = mapping.get(new ItemIdentifierDTO("100500", "sku1"));
        assertThat(mapping, notNullValue());
        assertThat(contentMapping.getPackageNumInSpike(), equalTo(15));
    }

    private SupplierMappingServiceImpl getMockMappingService(
            MboMappings.SearchApprovedMappingsResponse mappingResponse) {
        final MboMappingsServiceStub mappingService = mock(MboMappingsServiceStub.class);
        when(mappingService.searchApprovedMappingsByKeys(any()))
                .thenReturn(mappingResponse);

        SupplierMappingServiceImpl service = new SupplierMappingServiceImpl(mappingService);
        service.setPartitionSize(10);
        service.setExecutorService(Executors.newSingleThreadExecutor());

        return service;
    }

    private MboMappings.SearchApprovedMappingsResponse getManyMappingResponse() {
        final ModelStorage.ParameterValue param1 = ModelStorage.ParameterValue.newBuilder()
                .setParamId(SupplierMappingService.EXPIR_DATE_PARAM_ID)
                .setBoolValue(true)
                .build();
        final ModelStorage.ParameterValue param2 = ModelStorage.ParameterValue.newBuilder()
                .setParamId(SupplierMappingService.PACKAGE_NUM_IN_SPIKE_PARAM_ID)
                .setNumericValue("12")
                .build();

        final MboMappings.SearchApprovedMappingsResponse mappingResponse =
                MboMappings.SearchApprovedMappingsResponse.newBuilder()
                        .addMapping(MboMappings.ApprovedMappingInfo.newBuilder()
                                .setSupplierId(Integer.valueOf("100500"))
                                .setShopSku("sku1")
                                .setMarketSkuId(1)
                                .setShopTitle("title1")
                                .setShopVendorcode("vendorCode1")
                                .setMasterDataInfo(getMasterDataInfo())
                                .addMskuVendorcode("vendorCode1")
                                .addMskuBarcode("barcode1")
                                .build())
                        .addMapping(MboMappings.ApprovedMappingInfo.newBuilder()
                                .setSupplierId(Integer.valueOf("100500"))
                                .setShopSku("sku2")
                                .setMarketSkuId(2)
                                .setMskuTitle("name2")
                                .setShopTitle("title2")
                                .setShopVendorcode("vendorCode1")
                                .addMskuVendorcode("vendorCode1")
                                .addMskuVendorcode("vendorCode2")
                                .addMskuBarcode("barcode1")
                                .addMskuBarcode("barcode2")
                                .addModelParam(param1)
                                .addModelParam(param2)
                                .build())
                        .build();

        return mappingResponse;
    }

    private void assertFirst(final SupplierContentMapping mapping) {
        assertThat(mapping, notNullValue());
        assertThat(mapping.getSupplierSku(), equalTo("sku1"));
        assertThat(mapping.getMarketSku(), equalTo(1L));
        assertThat(mapping.getTitle(), equalTo("title1"));
        assertThat(mapping.getMarketName(), equalTo(""));
        assertThat(mapping.getMarketVendorCodes(), equalTo(Collections.singletonList("vendorCode1")));
        assertThat(mapping.getMarketBarcodes(), equalTo(Collections.singletonList("barcode1")));
        assertThat(mapping.getBoxCount(), equalTo(5));
    }

    private void assertSecond(final SupplierContentMapping mapping) {
        assertThat(mapping, notNullValue());
        assertThat(mapping.getSupplierSku(), equalTo("sku2"));
        assertThat(mapping.getMarketSku(), equalTo(2L));
        assertThat(mapping.getTitle(), equalTo("title2"));
        assertThat(mapping.getMarketName(), equalTo("name2"));
        assertThat(mapping.isHasExpirationDate(), equalTo(true));
        assertThat(mapping.getPackageNumInSpike(), equalTo(12));
        assertThat(mapping.getMarketVendorCodes(), equalTo(Arrays.asList("vendorCode1", "vendorCode2")));
        assertThat(mapping.getMarketBarcodes(), equalTo(Arrays.asList("barcode1", "barcode2")));
        assertThat(mapping.getBoxCount(), equalTo(SupplierMappingServiceImpl.DEFAULT_BOX_COUNT));
    }

    private ru.yandex.market.mdm.http.MasterDataProto.MasterDataInfo getMasterDataInfo() {
        return ru.yandex.market.mdm.http.MasterDataProto.MasterDataInfo.newBuilder()
                .setProviderProductMasterData(
                        ru.yandex.market.mdm.http.MasterDataProto.ProviderProductMasterData.newBuilder()
                                .setBoxCount(5)
                                .build())
                .build();
    }
}