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
import ru.yandex.market.fintech.banksint.mybatis.installment.model.CustomInstallmentGroup;
import ru.yandex.market.fintech.instalment.model.CustomInstallmentGroupDto;

import static org.assertj.core.api.Assertions.assertThat;

public class InstallmentForGroupProductsControllerDisableAllCustomGroupsTest extends FunctionalTest {
    private static final String REQUEST_URL = "/supplier/fin-service" +
            "/installments/group/custom/disable/all?shop_id={shopId}";
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private InstallmentCustomGroupMapper installmentCustomGroupMapper;

    @BeforeEach
    public void setUp() {
        jdbcTemplate.execute(readClasspathFile("ListInstallmentCustomGroup.sql"));
    }

    @Test
    void disableAllCustomInstallmentGroups() {
        var enabledCount = installmentCustomGroupMapper.getCustomInstallmentGroupList(42L, 0, 30)
                .stream()
                .filter(CustomInstallmentGroup::getEnabled)
                .count();
        assertThat(enabledCount).isGreaterThan(0L);

        var response = testRestTemplate.exchange(
                REQUEST_URL,
                HttpMethod.POST,
                null,
                new ParameterizedTypeReference<CustomInstallmentGroupDto>() {
                },
                Map.of(
                        "shopId", "42"
                ));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        enabledCount = installmentCustomGroupMapper.getCustomInstallmentGroupList(42L, 0, 30)
                .stream()
                .filter(CustomInstallmentGroup::getEnabled)
                .count();
        assertThat(enabledCount).isEqualTo(0L);
    }
}
