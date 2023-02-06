package ru.yandex.market.wms.common.spring.dao;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Optional;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.wms.common.model.enums.LocationType;
import ru.yandex.market.wms.common.spring.IntegrationTest;
import ru.yandex.market.wms.common.spring.dao.entity.SkuId;
import ru.yandex.market.wms.common.spring.dao.entity.SkuLoc;
import ru.yandex.market.wms.common.spring.dao.implementation.SkuLocDao;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT;

public class SkuLocDaoTest extends IntegrationTest {

    @Autowired
    private SkuLocDao skuLocDao;

    @Test
    @DatabaseSetup("/db/dao/sku-loc/before.xml")
    @ExpectedDatabase(value = "/db/dao/sku-loc/before.xml", assertionMode = NON_STRICT)
    public void findWhenExists() {
        Optional<SkuId> maybeSerialKey = skuLocDao.getExistingSkuId(createSkuId(), "STAGE");
        assertions.assertThat(maybeSerialKey).isPresent();
        SkuId skuId = maybeSerialKey.get();
        assertions.assertThat(skuId.getStorerKey()).isEqualTo("465852");
        assertions.assertThat(skuId.getSku()).isEqualTo("ROV0000000000000001456");
    }

    @Test
    @DatabaseSetup("/db/dao/sku-loc/before.xml")
    @ExpectedDatabase(value = "/db/dao/sku-loc/before.xml", assertionMode = NON_STRICT)
    public void findWhenNotExists() {
        Optional<SkuId> maybeSerialKey = skuLocDao.getExistingSkuId(createSkuId(), "STAGE1");
        assertions.assertThat(maybeSerialKey).isEmpty();
    }

    @Test
    @DatabaseSetup("/db/dao/sku-loc/before.xml")
    @ExpectedDatabase(value = "/db/dao/sku-loc/after-save.xml", assertionMode = NON_STRICT)
    public void save() {
        SkuLoc skuLoc = SkuLoc.builder()
            .storerKey("465855")
            .sku("ROV0000000000000001459")
            .loc("STAGE1")
            .qty(BigDecimal.ONE)
            .qtyPicked(BigDecimal.ZERO)
            .locationType(LocationType.OTHER)
            .addWho("TEST")
            .editWho("TEST")
            .build();
        skuLocDao.insert(Collections.singletonList(skuLoc));
    }

    @Test
    @DatabaseSetup("/db/dao/sku-loc/before.xml")
    @ExpectedDatabase(value = "/db/dao/sku-loc/after-add-one.xml", assertionMode = NON_STRICT)
    public void addOneToQty() {
        skuLocDao.addToQty(createSkuId(), "STAGE", BigDecimal.ONE, "TEST");
    }

    @Test
    @DatabaseSetup("/db/dao/sku-loc/before.xml")
    @ExpectedDatabase(value = "/db/dao/sku-loc/after-subtract-one.xml", assertionMode = NON_STRICT)
    public void subtractOneQty() {
        skuLocDao.subtractQty(createSkuId(), "STAGE", BigDecimal.ONE, "TEST");
    }

    @Test
    @DatabaseSetup("/db/dao/sku-loc/before.xml")
    @ExpectedDatabase(value = "/db/dao/sku-loc/after-subtract-one.xml", assertionMode = NON_STRICT)
    public void subtractOneQtyLessThanZero() {
        skuLocDao.subtractQty(createSkuId(), "STAGE", BigDecimal.TEN, "TEST");
    }

    private SkuId createSkuId() {
        return new SkuId("465852", "ROV0000000000000001456");
    }
}
