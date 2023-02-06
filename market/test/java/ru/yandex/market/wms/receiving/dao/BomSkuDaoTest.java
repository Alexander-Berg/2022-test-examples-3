package ru.yandex.market.wms.receiving.dao;

import java.math.BigDecimal;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.wms.common.spring.dao.entity.BillOfMaterial;
import ru.yandex.market.wms.common.spring.dao.entity.SkuId;
import ru.yandex.market.wms.common.spring.dao.implementation.BomSkuDao;
import ru.yandex.market.wms.receiving.ReceivingIntegrationTest;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT;

public class BomSkuDaoTest extends ReceivingIntegrationTest {

    private static final String USER = "receiving";
    private static final String MASTER_SKU = "masterSku";
    private static final String STORER_KEY = "storerKey";

    @Autowired
    private BomSkuDao bomSkuDao;

    /**
     * В БД нет BOM SKU для пары masterSku + storerKey.
     * Возвращается пустой Optional
     */
    @Test
    @DatabaseSetup("/dao/bom-sku/generate-first-bom-sku-name.xml")
    @ExpectedDatabase(value = "/dao/bom-sku/generate-first-bom-sku-name.xml", assertionMode = NON_STRICT)
    public void successGenerateFirstNextBomSkuName() {
        Integer actual = bomSkuDao.getActualSequence(new SkuId(STORER_KEY, MASTER_SKU)).orElse(null);
        Assertions.assertNull(actual);
    }

    /**
     * В БД уже есть две BOM SKU для пары masterSku + storerKey.
     * Получаем максимальный Sequence=2
     */
    @Test
    @DatabaseSetup("/dao/bom-sku/generate-third-bom-sku-name.xml")
    @ExpectedDatabase(value = "/dao/bom-sku/generate-third-bom-sku-name.xml", assertionMode = NON_STRICT)
    public void successGenerateThirdNextBomSkuName() {
        Integer actual = bomSkuDao.getActualSequence(new SkuId(STORER_KEY, MASTER_SKU)).orElse(null);
        Assertions.assertEquals(2, actual.intValue());
    }

    /**
     * В БД нет BOM SKU для заданных masterSku + bomSku + storerKey.
     * Возвращается пустой Optional
     */
    @Test
    @DatabaseSetup("/dao/bom-sku/get-qty-for-not-exists-bom-sku.xml")
    @ExpectedDatabase(value = "/dao/bom-sku/get-qty-for-not-exists-bom-sku.xml", assertionMode = NON_STRICT)
    public void successGetQtyForNotExistsBomSku() {
        BigDecimal actual = bomSkuDao.getBomQty(MASTER_SKU, new SkuId(STORER_KEY, "masterSkuBOM1")).orElse(null);
        Assertions.assertNull(actual);
    }

    /**
     * В БД уже есть BOM SKU для заданных masterSku + bomSku + storerKey.
     * Возвращается значение QTY
     */
    @Test
    @DatabaseSetup("/dao/bom-sku/get-qty-for-exists-bom-sku.xml")
    @ExpectedDatabase(value = "/dao/bom-sku/get-qty-for-exists-bom-sku.xml", assertionMode = NON_STRICT)
    public void successGetQtyForExistsBomSku() {
        BigDecimal actual = bomSkuDao.getBomQty(MASTER_SKU, new SkuId(STORER_KEY, "masterSkuBOM1")).orElse(null);
        Assertions.assertEquals(0, BigDecimal.ONE.compareTo(actual));
    }

    /**
     * Создаем новую БОМку.
     */
    @Test
    @DatabaseSetup("/dao/bom-sku/create-new-bom-sku-before.xml")
    @ExpectedDatabase(value = "/dao/bom-sku/create-new-bom-sku-after.xml", assertionMode = NON_STRICT)
    public void successCreateNewBomSku() {
        bomSkuDao.createBom(getBillOfMaterial("masterSkuBOM3", "3"));
    }

    /**
     * Пытаемся создать уже существующую БОМку.
     * Падаем с исключением
     */
    @Test
    @DatabaseSetup("/dao/bom-sku/create-new-bom-sku-before.xml")
    @ExpectedDatabase(value = "/dao/bom-sku/create-new-bom-sku-before.xml", assertionMode = NON_STRICT)
    public void failedCreateDuplicateOfBomSku() {
        Assertions.assertThrows(Exception.class, () ->
            bomSkuDao.createBom(getBillOfMaterial("masterSkuBOM2", "2"))
        );
    }

    private BillOfMaterial getBillOfMaterial(String bomSku, String sequence) {
        return BillOfMaterial.builder()
            .sku(MASTER_SKU)
            .componentSku(bomSku)
            .storerKey(STORER_KEY)
            .sequence(sequence)
            .editWho(USER)
            .addWho(USER)
            .build();
    }
}
