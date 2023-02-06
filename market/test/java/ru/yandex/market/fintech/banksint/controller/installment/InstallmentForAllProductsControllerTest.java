package ru.yandex.market.fintech.banksint.controller.installment;

import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.fintech.banksint.FunctionalTest;
import ru.yandex.market.fintech.banksint.mybatis.installment.InstallmentGroupMapper;

import static java.util.Collections.emptySet;
import static org.assertj.core.api.Assertions.assertThat;

class InstallmentForAllProductsControllerTest extends FunctionalTest {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private InstallmentGroupMapper installmentGroupMapper;

    @BeforeEach
    public void setUp() {
        jdbcTemplate.execute(readClasspathFile("InstallmentType.sql"));
    }

    @ParameterizedTest
    @EnumSource(names = {"POST", "PUT"}, value = HttpMethod.class)
    void createInstallmentGroupDto(HttpMethod httpMethod) {
        var shopId = System.currentTimeMillis();
        assertThat(getInstallmentGroup(shopId)).isEmpty();

        var expectedResult = Set.of("MONTH_AND_HALF", "BRAND_TEST");
        updateAndCheck(httpMethod, shopId, expectedResult);

        expectedResult = Set.of("MONTH_AND_HALF");
        updateAndCheck(httpMethod, shopId, expectedResult);

        expectedResult = Set.of("CATEGORY_TEST", "BRAND_TEST");
        updateAndCheck(httpMethod, shopId, expectedResult);

        updateAndCheck(httpMethod, shopId, emptySet());
    }

    @ParameterizedTest
    @EnumSource(names = {"POST", "PUT"}, value = HttpMethod.class)
    void tryToCreateInvalidInstallmentGroup(HttpMethod httpMethod) {
        var shopId = System.currentTimeMillis();
        assertThat(getInstallmentGroup(shopId)).isEmpty();

        var expectedResult = Set.of("INVALID", "BRAND_TEST");
        var response = tryToCreateOrUpdateInvalidInstallmentGroup(shopId, expectedResult, httpMethod);
        assertThat(response).contains("Invalid installments: [INVALID]");

        expectedResult = Set.of("MONTH_AND_HALF", "DISABLED_TEST");
        response = tryToCreateOrUpdateInvalidInstallmentGroup(shopId, expectedResult, httpMethod);
        assertThat(response).contains("Invalid installments: [DISABLED_TEST]");
    }

    private void updateAndCheck(HttpMethod httpMethod, long shopId, Set<String> expectedResult) {
        var response = updateInstallmentGroup(shopId, expectedResult, httpMethod);
        assertThat(response).isEqualTo(expectedResult);
        assertThat(installmentGroupMapper.findInstallmentsByShopId(shopId)).isEqualTo(expectedResult);
    }

    private Set<String> getInstallmentGroup(Long shopId) {
        var response = testRestTemplate.exchange(
                "/supplier/fin-service/installments/group?shop_id={shopId}",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<Set<String>>() {
                },
                Map.of(
                        "shopId", shopId.toString()
                ));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return response.getBody();
    }

    private Set<String> updateInstallmentGroup(Long shopId, Set<String> installments, HttpMethod httpMethod) {
        var response = testRestTemplate.exchange(
                "/supplier/fin-service/installments/group?shop_id={shopId}",
                httpMethod,
                new HttpEntity<>(installments),
                new ParameterizedTypeReference<Set<String>>() {
                },
                Map.of(
                        "shopId", shopId.toString()
                ));
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        return response.getBody();
    }

    private String tryToCreateOrUpdateInvalidInstallmentGroup(Long shopId, Set<String> installments,
                                                              HttpMethod httpMethod) {
        var response = testRestTemplate.exchange(
                "/supplier/fin-service/installments/group?shop_id={shopId}",
                httpMethod,
                new HttpEntity<>(installments),
                new ParameterizedTypeReference<String>() {
                },
                Map.of(
                        "shopId", shopId.toString()
                ));
        assertThat(response.getStatusCode().is4xxClientError()).isTrue();
        return response.getBody();
    }
}
