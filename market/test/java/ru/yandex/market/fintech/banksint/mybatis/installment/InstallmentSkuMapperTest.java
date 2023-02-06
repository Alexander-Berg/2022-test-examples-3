package ru.yandex.market.fintech.banksint.mybatis.installment;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.fintech.banksint.FunctionalTest;
import ru.yandex.market.fintech.banksint.mybatis.installment.model.InstallmentSku;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

class InstallmentSkuMapperTest extends FunctionalTest {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private InstallmentSkuMapper installmentSkuMapper;

    @BeforeEach
    public void setUp() {
        jdbcTemplate.execute(readClasspathFile("InstallmentSkuMapperTest.sql"));
    }

    @Test
    void saveShouldWorkProperly() {
        InstallmentSku installmentSku = new InstallmentSku();
        installmentSku.setResourceId("85b438fc-a482-4fd7-83d1-505c6b5dcc30");
        installmentSku.setSku("100257133102.352");
        installmentSku.setMsku(100257133102L);
        installmentSku.setInstallments(Set.of("MONTH_AND_HALF", "HALF_YEAR"));
        installmentSkuMapper.save(installmentSku);

        Map<String, Object> installmentSkusMap = jdbcTemplate.queryForMap("select * from installment_skus");
        assertEquals("85b438fc-a482-4fd7-83d1-505c6b5dcc30", installmentSkusMap.get("resource_id"));
        assertEquals("100257133102.352", installmentSkusMap.get("sku"));
        assertEquals(100257133102L, installmentSkusMap.get("msku"));

        List<String> installments = jdbcTemplate.query("select installment_id from sku_to_installments where " +
                        "resource_id = '85b438fc-a482-4fd7-83d1-505c6b5dcc30' and msku = 100257133102",
                (rs, rowNum) -> rs.getString(1));
        assertThat(installments).hasSameElementsAs(List.of("MONTH_AND_HALF", "HALF_YEAR"));
    }

}
