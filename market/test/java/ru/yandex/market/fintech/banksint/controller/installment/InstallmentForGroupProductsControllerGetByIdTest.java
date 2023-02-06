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
import ru.yandex.market.fintech.banksint.service.dtoconverter.CustomInstallmentGroupDtoEntityConverter;
import ru.yandex.market.fintech.instalment.model.CustomInstallmentGroupDto;

import static org.assertj.core.api.Assertions.assertThat;

class InstallmentForGroupProductsControllerGetByIdTest extends FunctionalTest {
    private static final String REQUEST_URL = "/supplier/fin-service" +
            "/installments/group/custom/{id}?shop_id={shopId}";
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private InstallmentCustomGroupMapper installmentCustomGroupMapper;

    @Autowired
    private CustomInstallmentGroupDtoEntityConverter dtoConverter;

    @BeforeEach
    public void setUp() {
        jdbcTemplate.execute(readClasspathFile("ListInstallmentCustomGroup.sql"));
    }

    @Test
    void getCustomInstallmentGroupDtoById() {
        var expected = installmentCustomGroupMapper.getCustomInstallmentGroupByIdAndShopId(4L, 42L)
                .toDto();
        assertThat(expected.getName()).isEqualTo("fourth");

        var response = testRestTemplate.exchange(
                REQUEST_URL,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<CustomInstallmentGroupDto>() {
                },
                Map.of(
                        "shopId", "42",
                        "id", "4"
                ));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(expected);
    }

    @Test
    void getCustomInstallmentGroupDtoByIdWithResourceIdNotNull() {
        jdbcTemplate.execute(readClasspathFile("ListInstallmentCustomGroupWithFIleSourceEntries.sql"));

        var expected = dtoConverter.toDto(
                installmentCustomGroupMapper.getCustomInstallmentGroupByIdAndShopId(3L, 42L));
        assertThat(expected.getName()).isEqualTo("third");

        var response = testRestTemplate.exchange(
                REQUEST_URL,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<CustomInstallmentGroupDto>() {
                },
                Map.of(
                        "shopId", "42",
                        "id", "3"
                ));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(expected);
    }

    @Test
    void getCustomInstallmentGroupDtoByIdNotFoundTest() {
        var response = testRestTemplate.exchange(
                REQUEST_URL,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<String>() {
                },
                Map.of(
                        "shopId", "42",
                        "id", "99999"
                ));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).contains("InstallmentGroup(id=99999, shopId=42) not found");
    }
}
