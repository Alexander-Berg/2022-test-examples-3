package ru.yandex.market.fintech.banksint.controller.installment;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.fintech.banksint.FunctionalTest;
import ru.yandex.market.fintech.banksint.exception.NotFoundInstallmentResourceException;
import ru.yandex.market.fintech.banksint.model.ClientErrorResponse;
import ru.yandex.market.fintech.banksint.mybatis.installment.InstallmentResourceMapper;
import ru.yandex.market.fintech.instalment.model.InstallmentsResourceInfoDto;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class InstallmentOffersResourceControllerTest extends FunctionalTest {

    private static final String RESOURCE_ID = "de7cd822-d5f9-47cb-8fee-36546d7c9ab1";
    private static final String REQUEST_URL = "/supplier/fin-service/installments/offers/resource?shop_id={shopId}" +
            "&business_id={businessId}&resource_id={resourceId}";

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private InstallmentResourceMapper installmentResourceMapper;

    @BeforeEach
    public void setUp() {
        jdbcTemplate.execute(readClasspathFile("InstallmentOffersResource.sql"));
    }

    @Test
    void getResourceInfoOkTest() {
        InstallmentsResourceInfoDto expected =
                installmentResourceMapper.getInstallmentsResourceInfoByResourceId(RESOURCE_ID).toDto();

        ResponseEntity<InstallmentsResourceInfoDto> response = testRestTemplate.exchange(
                REQUEST_URL,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {
                },
                Map.of(
                        "shopId", "1",
                        "businessId", "2",
                        "resourceId", RESOURCE_ID
                )
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(expected);
    }

    @Test
    void getResourceInfoNotFoundTest() {
        Long shopId = 2L;
        Long businessId = 2L;

        ClientErrorResponse expected = new ClientErrorResponse()
                .message(new NotFoundInstallmentResourceException(RESOURCE_ID, shopId, businessId).getMessage());

        ResponseEntity<ClientErrorResponse> response = testRestTemplate.exchange(
                REQUEST_URL,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {
                },
                Map.of(
                        "shopId", shopId.toString(),
                        "businessId", businessId.toString(),
                        "resourceId", RESOURCE_ID
                )
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().tracingInfo(null)).isEqualTo(expected);
    }
}
