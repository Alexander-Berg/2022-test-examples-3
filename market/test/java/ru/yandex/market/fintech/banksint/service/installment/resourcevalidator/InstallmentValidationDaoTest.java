package ru.yandex.market.fintech.banksint.service.installment.resourcevalidator;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import ru.yandex.market.fintech.banksint.FunctionalTest;
import ru.yandex.market.fintech.banksint.mybatis.installment.model.InstallmentSku;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InstallmentValidationDaoTest extends FunctionalTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private InstallmentValidationDao installmentValidationDao;

    @BeforeEach
    public void setUp() {
        jdbcTemplate.execute(readClasspathFile("InstallmentValidationDaoTest.sql"));
    }

    @Test
    void testBatchDeletes() {
        var sku1 = new InstallmentSku();
        var sku2 = new InstallmentSku();
        var sku3 = new InstallmentSku();
        String resourceId = "85b438fc-a482-4fd7-83d1-505c6b5dcc30";

        sku1.setSku("sku_11");
        sku1.setMsku(11);
        sku1.setResourceId(resourceId);

        sku3.setSku("sku_13");
        sku3.setMsku(13);
        sku3.setResourceId(resourceId);

        sku2.setSku("sku_2");
        sku2.setMsku(12);
        sku2.setResourceId(resourceId);

        List<InstallmentSku> batch = List.of(
                sku1, sku2, sku3
        );
        installmentValidationDao.insertSkus(batch);

        installmentValidationDao.insertSkuInstallments(batch);

        installmentValidationDao.clearOldSkus(batch);

        installmentValidationDao.insertSkus(batch);

        Long installments = jdbcTemplate.query("" +
                        "select count(*) from installment_skus where " +
                        "resource_id = '" + resourceId + "'",
                (rs, rowNum) -> rs.getLong(1)).get(0);

        Long skuRelations = jdbcTemplate.query("" +
                        "select count(*) from sku_to_installments where " +
                        "resource_id = '" + resourceId + "'",
                (rs, rowNum) -> rs.getLong(1)).get(0);

        assertEquals(3, installments);
        assertEquals(0, skuRelations);

    }
}
