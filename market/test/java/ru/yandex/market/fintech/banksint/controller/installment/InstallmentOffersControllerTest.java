package ru.yandex.market.fintech.banksint.controller.installment;

import java.time.Instant;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.fintech.banksint.FunctionalTest;
import ru.yandex.market.fintech.banksint.mybatis.installment.OffersGenerationMapper;
import ru.yandex.market.fintech.banksint.mybatis.installment.model.InstallmentsFileInfo;
import ru.yandex.market.fintech.banksint.util.InstallmentsUtils;
import ru.yandex.market.fintech.instalment.model.FileInfoDto;



class InstallmentOffersControllerTest extends FunctionalTest {

    @Autowired
    private OffersGenerationMapper mapper;

    @Test
    void generateXlsxWithInstallmentsOffersAsync() {

        ResponseEntity<FileInfoDto> response = testRestTemplate.exchange(
                "/supplier/fin-service/installments/offers/generate/xlsx/async" +
                        "?shop_id={shopId}&business_id={businessId}",
                HttpMethod.POST,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<>() {
                },
                Map.of(
                        "shopId", "1",
                        "businessId", "2"
                )
        );
        System.out.println(Instant.now());
        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
        Assertions.assertEquals(2, response.getBody().getBusinessId());
        Assertions.assertEquals(1, response.getBody().getShopId());
        Assertions.assertEquals(FileInfoDto.StateEnum.PENDING, response.getBody().getState());
        Assertions.assertNotNull(response.getBody().getName());

        String id = response.getBody().getResourceId();
        var info = mapper.getInstallmentFileInfoByResourceId(id);
        Assertions.assertEquals(2, info.getBusinessId());
        Assertions.assertEquals(1, info.getShopId());
        Assertions.assertNotNull(info.getName());


    }

    @Test
    void getAsyncResourceInfo() {
        var info = InstallmentsFileInfo.builder()
                .setResourceId(InstallmentsUtils.generateResourceId())
                .setBusinessId(123L)
                .setShopId(234L)
                .setName("name")
                .build();

        mapper.insertNewOfferGenerationTask(info);
        var response = testRestTemplate.exchange(
                "/supplier/fin-service/installments/offers/generate/xlsx/async" +
                        "?shop_id={shopId}&business_id={businessId}&resource_id={resourceId}",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<FileInfoDto>() {
                },
                Map.of(
                        "shopId", "234",
                        "businessId", "123",
                        "resourceId", info.getResourceId()
                ));
        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
        Assertions.assertEquals(FileInfoDto.StateEnum.PENDING, response.getBody().getState());
        Assertions.assertNull(response.getBody().getUrlToDownload());

        String url = "url";
        mapper.setUrlAndFinishProcessingTask(info.getResourceId(), url);
        response = testRestTemplate.exchange(
                "/supplier/fin-service/installments/offers/generate/xlsx/async" +
                        "?shop_id={shopId}&business_id={businessId}&resource_id={resourceId}",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<FileInfoDto>() {
                },
                Map.of(
                        "shopId", "234",
                        "businessId", "123",
                        "resourceId", info.getResourceId()
                ));
        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
        Assertions.assertEquals(FileInfoDto.StateEnum.DONE, response.getBody().getState());
        Assertions.assertEquals(url, response.getBody().getUrlToDownload());
    }
}
