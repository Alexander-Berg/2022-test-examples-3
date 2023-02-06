package ru.yandex.market.fintech.banksint.controller.installment;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.fintech.banksint.FunctionalTest;
import ru.yandex.market.fintech.banksint.mybatis.installment.InstallmentCustomGroupMapper;
import ru.yandex.market.fintech.banksint.mybatis.installment.model.CustomInstallmentGroup;
import ru.yandex.market.fintech.banksint.service.dtoconverter.CustomInstallmentGroupDtoEntityConverter;
import ru.yandex.market.fintech.instalment.model.CustomInstallmentGroupList;

import static org.assertj.core.api.Assertions.assertThat;

public class InstallmentForGroupProductsControllerListTest extends FunctionalTest {
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
    void listCustomInstallmentGroupDto() {
        var response = testRestTemplate.exchange(
                "/supplier/fin-service/installments/group/custom?shop_id={shopId}&pageSize={pageSize}",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<CustomInstallmentGroupList>() {
                },
                Map.of(
                        "shopId", "42",
                        "pageSize", "4"
                ));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getData()).hasSize(4);
        assertThat(response.getBody().getHasMore()).isTrue();
        assertThat(response.getBody().getNextPageToken()).isNotBlank();

        var expectedResult = Stream.of(
                        installmentCustomGroupMapper.getCustomInstallmentGroupByIdAndShopId(8L, 42L),
                        installmentCustomGroupMapper.getCustomInstallmentGroupByIdAndShopId(7L, 42L),
                        installmentCustomGroupMapper.getCustomInstallmentGroupByIdAndShopId(6L, 42L),
                        installmentCustomGroupMapper.getCustomInstallmentGroupByIdAndShopId(5L, 42L))
                .map(CustomInstallmentGroup::toDto)
                .collect(Collectors.toList());
        assertThat(response.getBody().getData()).isEqualTo(expectedResult);

        //second page
        response = testRestTemplate.exchange(
                "/supplier/fin-service" +
                        "/installments/group/custom?shop_id={shopId}&pageSize={pageSize}&pageToken={pageToken}",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<CustomInstallmentGroupList>() {
                },
                Map.of(
                        "shopId", "42",
                        "pageSize", "4",
                        "pageToken", response.getBody().getNextPageToken()
                ));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getData()).hasSize(4);
        assertThat(response.getBody().getHasMore()).isFalse();
        assertThat(response.getBody().getNextPageToken()).isNullOrEmpty();

        expectedResult = Stream.of(
                        installmentCustomGroupMapper.getCustomInstallmentGroupByIdAndShopId(4L, 42L),
                        installmentCustomGroupMapper.getCustomInstallmentGroupByIdAndShopId(3L, 42L),
                        installmentCustomGroupMapper.getCustomInstallmentGroupByIdAndShopId(2L, 42L),
                        installmentCustomGroupMapper.getCustomInstallmentGroupByIdAndShopId(1L, 42L))
                .map(CustomInstallmentGroup::toDto)
                .collect(Collectors.toList());
        assertThat(response.getBody().getData()).isEqualTo(expectedResult);
    }

    @Test
    void emptyListCustomInstallmentGroupDto() {
        var response = testRestTemplate.exchange(
                "/supplier/fin-service/installments/group/custom?shop_id={shopId}&pageSize={pageSize}",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<CustomInstallmentGroupList>() {
                },
                Map.of(
                        "shopId", "9999999",
                        "pageSize", "30"
                ));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getData()).hasSize(0);
        assertThat(response.getBody().getHasMore()).isFalse();
        assertThat(response.getBody().getNextPageToken()).isNullOrEmpty();
    }

    @Test
    void listCustomInstallmentGroupDtoWithoutPageSize() {
        var response = testRestTemplate.exchange(
                "/supplier/fin-service/installments/group/custom?shop_id={shopId}",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<CustomInstallmentGroupList>() {
                },
                Map.of(
                        "shopId", "42"
                ));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getData()).hasSizeGreaterThan(0);
        assertThat(response.getBody().getHasMore()).isFalse();
        assertThat(response.getBody().getNextPageToken()).isNullOrEmpty();
    }

    @Test
    void listCustomInstallmentGroupDtoWithResourceIdNotNull() {
        jdbcTemplate.execute(readClasspathFile("ListInstallmentCustomGroupWithFIleSourceEntries.sql"));

        var response = testRestTemplate.exchange(
                "/supplier/fin-service/installments/group/custom?shop_id={shopId}&pageSize={pageSize}",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<CustomInstallmentGroupList>() {
                },
                Map.of(
                        "shopId", "42",
                        "pageSize", "4"
                ));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getData()).hasSize(4);
        assertThat(response.getBody().getHasMore()).isFalse();
        assertThat(response.getBody().getNextPageToken()).isNullOrEmpty();

        var expectedResult = Stream.of(
                        installmentCustomGroupMapper.getCustomInstallmentGroupByIdAndShopId(4L, 42L),
                        installmentCustomGroupMapper.getCustomInstallmentGroupByIdAndShopId(3L, 42L),
                        installmentCustomGroupMapper.getCustomInstallmentGroupByIdAndShopId(2L, 42L),
                        installmentCustomGroupMapper.getCustomInstallmentGroupByIdAndShopId(1L, 42L))
                .map(dtoConverter::toDto)
                .collect(Collectors.toList());
        assertThat(response.getBody().getData()).isEqualTo(expectedResult);

    }
}
