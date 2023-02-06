package ru.yandex.market.fintech.banksint.controller.installment;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.fintech.banksint.FunctionalTest;
import ru.yandex.market.fintech.banksint.mybatis.installment.InstallmentCustomGroupMapper;

import static org.assertj.core.api.Assertions.assertThat;

class InstallmentForGroupProductsControllerDeleteTest extends FunctionalTest {
    private static final String REQUEST_URL = "/supplier/fin-service" +
            "/installments/group/custom/{id}?shop_id={shopId}";
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private InstallmentCustomGroupMapper installmentCustomGroupMapper;

    @BeforeEach
    public void setUp() {
        jdbcTemplate.execute(readClasspathFile("ListInstallmentCustomGroup.sql"));
    }

    @Test
    void deleteCustomInstallmentGroupDto() {
        assertThat(installmentCustomGroupMapper.getCustomInstallmentGroupByIdAndShopId(4L, 42L))
                .isNotNull();

        var response = testRestTemplate.exchange(
                REQUEST_URL,
                HttpMethod.DELETE,
                null,
                new ParameterizedTypeReference<String>() {
                },
                Map.of(
                        "shopId", "42",
                        "id", "4"
                ));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(installmentCustomGroupMapper.getCustomInstallmentGroupByIdAndShopId(4L, 42L))
                .isNull();
    }

}
