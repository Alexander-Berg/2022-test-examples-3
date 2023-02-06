package ru.yandex.market.fintech.banksint.controller.installment;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

import ru.yandex.market.fintech.banksint.FunctionalTest;

import static org.assertj.core.api.Assertions.assertThat;

class InstallmentControllerTest extends FunctionalTest {

    @Test
    void getDenyBrandList() {
        var response = testRestTemplate.exchange(
                "/supplier/fin-service/installments/brands/deny/list?shop_id={shopId}",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<Long>>() {
                },
                Map.of(
                        "shopId", "42"
                ));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEmpty();
    }

    @Test
    void getDenyCategoryList() {
        var response = testRestTemplate.exchange(
                "/supplier/fin-service/installments/categories/deny/list?shop_id={shopId}",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<Long>>() {
                },
                Map.of(
                        "shopId", "42"
                ));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEmpty();
    }
}
