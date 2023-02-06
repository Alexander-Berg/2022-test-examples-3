package ru.yandex.market.wms.servicebus.async.service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.messaging.MessageHeaders;

import ru.yandex.market.logistic.api.model.fulfillment.UnitId;
import ru.yandex.market.wms.common.spring.dao.entity.AltSku;
import ru.yandex.market.wms.common.spring.servicebus.model.dto.ManufacturerSkuDto;
import ru.yandex.market.wms.common.spring.servicebus.model.request.AltSkuByManufacturerSkuRequest;
import ru.yandex.market.wms.common.spring.servicebus.model.request.UpdateAltSkuRequest;
import ru.yandex.market.wms.servicebus.IntegrationTest;
import ru.yandex.market.wms.servicebus.api.external.mbo.category.client.MboCategoryClient;
import ru.yandex.market.wms.servicebus.api.external.mbo.category.client.model.entity.FulfilmentInfo;
import ru.yandex.market.wms.servicebus.api.external.mbo.category.client.model.entity.ShopSku;
import ru.yandex.market.wms.servicebus.api.external.mbo.category.client.model.request.SearchFulfilmentSskuRequest;
import ru.yandex.market.wms.servicebus.api.external.mbo.category.client.model.response.SearchFulfilmentSskuResponse;
import ru.yandex.market.wms.servicebus.api.external.wrap.client.WrapInforClient;
import ru.yandex.market.wms.servicebus.api.external.wrap.client.dto.IdentifierMappingDto;
import ru.yandex.market.wms.servicebus.api.external.wrap.client.dto.InforUnitId;
import ru.yandex.market.wms.servicebus.api.internal.api.client.impl.WmsApiClientImpl;

import static org.mockito.Mockito.reset;

public class AltSkuByManufacturerSkuServiceTest extends IntegrationTest {
    @MockBean
    @Autowired
    private MboCategoryClient mboCategoryClient;

    @MockBean
    @Autowired
    private WrapInforClient wrapInforClient;

    @MockBean
    @Autowired
    private WmsApiClientImpl wmsApiClient;

    @Autowired
    private AltSkuByManufacturerSkuService altSkuByManufacturerSkuService;

    @AfterEach
    public void resetMocks() {
        Mockito.reset(mboCategoryClient, wrapInforClient, wmsApiClient);
    }

    @Test
    void shouldSuccess() {

        reset(mboCategoryClient);
        reset(wrapInforClient);
        reset(wmsApiClient);

        Mockito.when(
                this.mboCategoryClient.searchFulfilmentSskuParams(getSearchFulfilmentSskuRequest())
        ).thenReturn(getSearchFulfilmentSskuResponse());

        Mockito.when(
                this.wrapInforClient.mapReferenceItems(getMapReferenceItemsRequest())
        ).thenReturn(getMapReferenceItemsResponse());

        altSkuByManufacturerSkuService.getAltSkuByManufacturerSku(
                getAltSkuByManufacturerSkuRequest(),
                new MessageHeaders(Collections.emptyMap())
        );

        Mockito.verify(this.wmsApiClient).updateAltSkuAsync(getUpdateAltSkuRequest());
    }

    private SearchFulfilmentSskuRequest getSearchFulfilmentSskuRequest() {
        List<ShopSku> shopSkuList = Stream.of(
                ShopSku
                        .builder()
                        .supplierId(10264169L)
                        .shopSku("00065.00026.100126176174")
                        .build(),
                ShopSku
                        .builder()
                        .supplierId(10264169L)
                        .shopSku("00065.00026.100126177747")
                        .build(),
                ShopSku
                        .builder()
                        .supplierId(10264169L)
                        .shopSku("00065.00026.100126176191")
                        .build()
        ).collect(Collectors.toList());

        return SearchFulfilmentSskuRequest
                .builder()
                .warehouseId(0)
                .returnMasterData(true)
                .keys(shopSkuList)
                .build();
    };

    private SearchFulfilmentSskuResponse getSearchFulfilmentSskuResponse() {
        List<FulfilmentInfo> fulfilmentInfoList = Stream.of(
                FulfilmentInfo.builder()
                        .supplierId(10264169L)
                        .shopSku("00065.00026.100126176174")
                        .shopBarcode("1001261761742, 100126176174, 00065.00026.100126176174")
                        .build(),
                FulfilmentInfo.builder()
                        .supplierId(10264169L)
                        .shopSku("00065.00026.100126177747")
                        .shopBarcode("100257134916, 654624562345234, 4607004650127")
                        .build(),
                FulfilmentInfo.builder()
                        .supplierId(10264169L)
                        .shopSku("00065.00026.100126176191")
                        .shopBarcode("100126176191, 4620753724280")
                        .build()
        ).collect(Collectors.toList());

        return SearchFulfilmentSskuResponse.builder()
                .fulfilmentInfoList(fulfilmentInfoList)
                .build();
    };

    private List<UnitId> getMapReferenceItemsRequest() {
        return Stream.of(
                new UnitId("00065.00026.100126176174", 10264169L, "00065.00026.100126176174"),
                new UnitId("00065.00026.100126177747", 10264169L, "00065.00026.100126177747"),
                new UnitId("00065.00026.100126176191", 10264169L, "00065.00026.100126176191")
        ).collect(Collectors.toList());
    }

    private List<IdentifierMappingDto> getMapReferenceItemsResponse() {
        return Stream.of(
                new IdentifierMappingDto(
                        new UnitId("00065.00026.100126176174", 10264169L, "00065.00026.100126176174"),
                        new InforUnitId("ROV0000000000000000277", 10264169L)
                ),
                new IdentifierMappingDto(
                        new UnitId("00065.00026.100126177747", 10264169L, "00065.00026.100126177747"),
                        new InforUnitId("ROV0000000000000000317", 10264169L)
                ),
                new IdentifierMappingDto(
                        new UnitId("00065.00026.100126176191", 10264169L, "00065.00026.100126176191"),
                        new InforUnitId("ROV0000000000000000282", 10264169L)
                )
        ).collect(Collectors.toList());
    }

    private AltSkuByManufacturerSkuRequest getAltSkuByManufacturerSkuRequest() {
        List<ManufacturerSkuDto> manufacturerSkuDtoList = Stream.of(
                ManufacturerSkuDto.builder()
                        .storerKey(10264169L)
                        .manufacturerSku("00065.00026.100126176174")
                        .build(),
                ManufacturerSkuDto.builder()
                        .storerKey(10264169L)
                        .manufacturerSku("00065.00026.100126177747")
                        .build(),
                ManufacturerSkuDto.builder()
                        .storerKey(10264169L)
                        .manufacturerSku("00065.00026.100126176191")
                        .build()
        ).collect(Collectors.toList());

        return AltSkuByManufacturerSkuRequest.builder()
                .manufacturerSkuDtoList(manufacturerSkuDtoList)
                .build();
    }

    private UpdateAltSkuRequest getUpdateAltSkuRequest() {
        List<AltSku> altSkuList = Stream.of(
                AltSku.builder()
                        .storerKey("10264169")
                        .sku("ROV0000000000000000277")
                        .altsku("1001261761742")
                        .build(),
                AltSku.builder()
                        .storerKey("10264169")
                        .sku("ROV0000000000000000277")
                        .altsku("100126176174")
                        .build(),
                AltSku.builder()
                        .storerKey("10264169")
                        .sku("ROV0000000000000000277")
                        .altsku("00065.00026.100126176174")
                        .build(),
                AltSku.builder()
                        .storerKey("10264169")
                        .sku("ROV0000000000000000317")
                        .altsku("100257134916")
                        .build(),
                AltSku.builder()
                        .storerKey("10264169")
                        .sku("ROV0000000000000000317")
                        .altsku("654624562345234")
                        .build(),
                AltSku.builder()
                        .storerKey("10264169")
                        .sku("ROV0000000000000000317")
                        .altsku("4607004650127")
                        .build(),
                AltSku.builder()
                        .storerKey("10264169")
                        .sku("ROV0000000000000000282")
                        .altsku("100126176191")
                        .build(),
                AltSku.builder()
                        .storerKey("10264169")
                        .sku("ROV0000000000000000282")
                        .altsku("4620753724280")
                        .build()
        ).collect(Collectors.toList());

        return UpdateAltSkuRequest.builder()
                .altSkuList(altSkuList)
                .build();
    }

}

