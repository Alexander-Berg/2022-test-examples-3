package ru.yandex.market.wms.common.spring.dao;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import ru.yandex.market.logistic.api.model.fulfillment.UnitId;
import ru.yandex.market.wms.common.model.enums.RotationType;
import ru.yandex.market.wms.common.model.enums.ShelfLifeCodeType;
import ru.yandex.market.wms.common.model.enums.ShelfLifeIndicatorType;
import ru.yandex.market.wms.common.model.enums.ShelfLifeTemplate;
import ru.yandex.market.wms.common.spring.IntegrationTest;
import ru.yandex.market.wms.common.spring.dao.entity.Sku;
import ru.yandex.market.wms.common.spring.dao.entity.SkuId;
import ru.yandex.market.wms.common.spring.dao.entity.SkuShelfLifeInReceivingProcess;
import ru.yandex.market.wms.common.spring.dao.entity.SkuWithBomSku;
import ru.yandex.market.wms.common.spring.dao.implementation.SkuDaoImpl;
import ru.yandex.market.wms.common.spring.pojo.SkuDimensions;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT;

public class SkuDaoImplTest extends IntegrationTest {

    @Autowired
    private SkuDaoImpl skuDao;

    @Autowired
    @Qualifier("enterpriseSkuDao")
    private SkuDaoImpl enterpriseSkuDao;

    @Test
    @DatabaseSetup(value = "/db/dao/sku/before.xml", connection = "wmwhseConnection")
    @ExpectedDatabase(value = "/db/dao/sku/before.xml", assertionMode = NON_STRICT, connection = "wmwhseConnection")
    public void findWhenExists() {
        Optional<Sku> maybeSku = skuDao.find("ROV0000000000000001456", "465852");
        assertions.assertThat(maybeSku).isPresent();
        Sku sku = maybeSku.get();
        assertSkuExpected(sku,
                new AssertSkuExpectedParamOne("ROV0000000000000001456", "MAN_1456", "Sku Sku",
                        ShelfLifeIndicatorType.SHELF_LIFE_APPLICABLE, ShelfLifeTemplate.EXPIRATION_DATE, 10, 5),
                new AssertSkuExpectedParamTwo(3, 50, 30, null, null),
                new AssertSkuExpectedParamThree(RotationType.BY_MANUFACTURE_DATE,
                        ShelfLifeCodeType.BY_MANUFACTURED_DATE, "1", "Sku Sku"));
    }

    @Test
    @DatabaseSetup(value = "/db/dao/sku/before.xml", connection = "wmwhseConnection")
    @ExpectedDatabase(value = "/db/dao/sku/before.xml", assertionMode = NON_STRICT, connection = "wmwhseConnection")
    public void findWhenNotExists() {
        Optional<Sku> maybeSku = skuDao.find("ROV0000000000000001453", "465852");
        assertions.assertThat(maybeSku).isEmpty();
        maybeSku = skuDao.find("ROV0000000000000001456", "465851");
        assertions.assertThat(maybeSku).isEmpty();
    }

    @Test
    @DatabaseSetup("/db/dao/sku/before.xml")
    @ExpectedDatabase(value = "/db/dao/sku/before.xml", assertionMode = NON_STRICT)
    public void findAllWhenExists() {
        Set<SkuId> skuIds = new HashSet<>();
        SkuId firstId = new SkuId("465852", "ROV0000000000000001456");
        SkuId secondId = new SkuId("465852", "ROV0000000000000001459");
        skuIds.add(firstId);
        skuIds.add(secondId);
        Map<SkuId, Sku> skuById = skuDao.findAll(skuIds);
        assertions.assertThat(skuById).hasSize(2);
        assertSkuExpected(skuById.get(firstId),
                new AssertSkuExpectedParamOne("ROV0000000000000001456", "MAN_1456", "Sku Sku",
                        ShelfLifeIndicatorType.SHELF_LIFE_APPLICABLE, ShelfLifeTemplate.EXPIRATION_DATE, 10, 5),
                new AssertSkuExpectedParamTwo(3, 50, 30, null, null),
                new AssertSkuExpectedParamThree(RotationType.BY_MANUFACTURE_DATE,
                        ShelfLifeCodeType.BY_MANUFACTURED_DATE, "1", "Sku Sku"));
        assertSkuExpected(skuById.get(secondId),
                new AssertSkuExpectedParamOne("ROV0000000000000001459", "MAN_1459", "Sku 2",
                        ShelfLifeIndicatorType.SHELF_LIFE_APPLICABLE, ShelfLifeTemplate.MANUFACTURED, 100, 30),
                new AssertSkuExpectedParamTwo(null, 30, null, null, 10),
                new AssertSkuExpectedParamThree(RotationType.BY_EXPIRATION_DATE,
                        ShelfLifeCodeType.BY_BEST_EXPIRATION_DATE, "2", "Sku 2"));
    }

    @Test
    @DatabaseSetup("/db/dao/sku/before.xml")
    @ExpectedDatabase(value = "/db/dao/sku/before.xml", assertionMode = NON_STRICT)
    public void findAllWhenNotExists() {
        Map<SkuId, Sku> skuById = skuDao.findAll(Collections.singleton(new SkuId("465852", "ROV0000000000000001453")));
        assertions.assertThat(skuById).isEmpty();
    }

    @Test
    @DatabaseSetup("/db/dao/sku/before.xml")
    @ExpectedDatabase(value = "/db/dao/sku/before.xml", assertionMode = NON_STRICT)
    public void findWithBomsWhenExists() {
        List<SkuWithBomSku> skuWithBoms = skuDao.findWithBoms("ROV0000000000000001459", "465852");
        assertions.assertThat(skuWithBoms).hasSize(2);
        for (SkuWithBomSku skuWithBom : skuWithBoms) {
            Sku sku = skuWithBom.getSku();
            assertSkuExpected(sku,
                    new AssertSkuExpectedParamOne("ROV0000000000000001459", "MAN_1459",
                            "Sku 2", ShelfLifeIndicatorType.SHELF_LIFE_APPLICABLE, ShelfLifeTemplate.MANUFACTURED,
                            100, 30),
                    new AssertSkuExpectedParamTwo(null, 30, null, null, 10),
                    new AssertSkuExpectedParamThree(RotationType.BY_EXPIRATION_DATE,
                            ShelfLifeCodeType.BY_BEST_EXPIRATION_DATE, "2", "Sku 2"));
            Sku bomSku = skuWithBom.getBomSku();
            if (bomSku.getSku().equals("ROV0000000000000001459BOM1")) {
                assertSkuExpected(bomSku,
                        new AssertSkuExpectedParamOne("ROV0000000000000001459BOM1", "MAN_1459_1", "Sku 2 Bom 1",
                                ShelfLifeIndicatorType.SHELF_LIFE_NOT_APPLICABLE, ShelfLifeTemplate.WITHOUT_LIMIT,
                                null, 0),
                        new AssertSkuExpectedParamTwo(null, null, null, null, null),
                        new AssertSkuExpectedParamThree(RotationType.BY_EXPIRATION_DATE,
                                ShelfLifeCodeType.BY_BEST_EXPIRATION_DATE, "1", "Sku 2 Bom 1"));
            } else {
                assertSkuExpected(bomSku,
                        new AssertSkuExpectedParamOne("ROV0000000000000001459BOM2", "MAN_1459_2", "Sku 2 Bom 2",
                                ShelfLifeIndicatorType.SHELF_LIFE_NOT_APPLICABLE, ShelfLifeTemplate.WITHOUT_LIMIT, null,
                                0),
                        new AssertSkuExpectedParamTwo(null, null, null, null, null),
                        new AssertSkuExpectedParamThree(RotationType.BY_EXPIRATION_DATE,
                                ShelfLifeCodeType.BY_BEST_EXPIRATION_DATE, "1", "Sku 2 Bom 2"));
            }
        }
    }

    @Test
    @DatabaseSetup("/db/dao/sku/before-without-boms.xml")
    @ExpectedDatabase(value = "/db/dao/sku/before-without-boms.xml", assertionMode = NON_STRICT)
    public void findWithBomsWhenExistsButWithoutBoms() {
        List<SkuWithBomSku> skuWithBoms = skuDao.findWithBoms("ROV0000000000000001459", "465852");
        assertions.assertThat(skuWithBoms).hasSize(1);
        for (SkuWithBomSku skuWithBom : skuWithBoms) {
            Sku sku = skuWithBom.getSku();
            assertSkuExpected(sku,
                    new AssertSkuExpectedParamOne("ROV0000000000000001459", "MAN_1459", "Sku 2",
                            ShelfLifeIndicatorType.SHELF_LIFE_APPLICABLE, ShelfLifeTemplate.MANUFACTURED, 100, 0),
                    new AssertSkuExpectedParamTwo(null, null, null, 30, 10),
                    new AssertSkuExpectedParamThree(RotationType.BY_LOT, ShelfLifeCodeType.BY_BEST_EXPIRATION_DATE, "1",
                            "Sku 2"));
            Sku bomSku = skuWithBom.getBomSku();
            assertions.assertThat(bomSku).isNull();
        }
    }

    @Test
    @DatabaseSetup("/db/dao/sku/before.xml")
    @ExpectedDatabase(value = "/db/dao/sku/before.xml", assertionMode = NON_STRICT)
    public void findWithBomsWhenNotExists() {
        List<SkuWithBomSku> skuWithBoms = skuDao.findWithBoms("ROV0000000000000001453", "465852");
        assertions.assertThat(skuWithBoms).isEmpty();
        skuWithBoms = skuDao.findWithBoms("ROV0000000000000001456", "465851");
        assertions.assertThat(skuWithBoms).isEmpty();
    }

    @Test
    @DatabaseSetup(value = "/db/dao/sku/before-without-boms.xml", connection = "wmwhseConnection")
    @DatabaseSetup(value = "/db/dao/sku/before-without-boms.xml", connection = "enterpriseConnection")
    @ExpectedDatabase(value = "/db/dao/sku/after-update-one.xml",
            connection = "wmwhseConnection", assertionMode = NON_STRICT)
    @ExpectedDatabase(value = "/db/dao/sku/after-update-one.xml",
            connection = "enterpriseConnection", assertionMode = NON_STRICT)
    public void updateSkuShelfLifeAfterReceivingForOneSku() {
        SkuShelfLifeInReceivingProcess skuShelfLifeInReceivingProcess = SkuShelfLifeInReceivingProcess.builder()
                .shelfLifeOnReceiving(20)
                .shelfLife(10)
                .shelfLifeCodeType(ShelfLifeCodeType.BY_MANUFACTURED_DATE)
                .shelfLifeTemplate(ShelfLifeTemplate.MANUFACTURED_AND_EXPIRATION_DATE)
                .toExpireDays(50)
                .rotationType(RotationType.BY_MANUFACTURE_DATE)
                .build();
        SkuId skuId = new SkuId("465852", "ROV0000000000000001459");
        skuDao.updateSkuShelfLifeAfterReceiving(skuShelfLifeInReceivingProcess, Collections.singleton(skuId), "TEST");
        enterpriseSkuDao.updateSkuShelfLifeAfterReceiving(skuShelfLifeInReceivingProcess, Collections.singleton(skuId),
                "TEST");
    }

    @Test
    @DatabaseSetup(value = "/db/dao/sku/before-without-boms.xml", connection = "wmwhseConnection")
    @DatabaseSetup(value = "/db/dao/sku/before-without-boms.xml", connection = "enterpriseConnection")
    @ExpectedDatabase(value = "/db/dao/sku/after-update-two.xml",
            connection = "wmwhseConnection", assertionMode = NON_STRICT)
    @ExpectedDatabase(value = "/db/dao/sku/after-update-two.xml",
            connection = "enterpriseConnection", assertionMode = NON_STRICT)
    public void updateSkuShelfLifeAfterReceivingForTwoSkus() {
        SkuShelfLifeInReceivingProcess skuShelfLifeInReceivingProcess = SkuShelfLifeInReceivingProcess.builder()
                .shelfLifeOnReceiving(20)
                .shelfLife(10)
                .shelfLifeCodeType(ShelfLifeCodeType.BY_MANUFACTURED_DATE)
                .shelfLifeTemplate(ShelfLifeTemplate.MANUFACTURED_AND_EXPIRATION_DATE)
                .toExpireDays(50)
                .rotationType(RotationType.BY_MANUFACTURE_DATE)
                .build();
        SkuId skuId = new SkuId("465852", "ROV0000000000000001459");
        SkuId skuId2 = new SkuId("465852", "ROV0000000000000001456");
        Set<SkuId> skuIds = new HashSet<>();
        skuIds.add(skuId);
        skuIds.add(skuId2);
        skuDao.updateSkuShelfLifeAfterReceiving(skuShelfLifeInReceivingProcess, skuIds, "TEST");
        enterpriseSkuDao.updateSkuShelfLifeAfterReceiving(skuShelfLifeInReceivingProcess, skuIds, "TEST");
    }

    @Test
    @DatabaseSetup(value = "/db/dao/sku/before-without-boms.xml", connection = "wmwhseConnection")
    @DatabaseSetup(value = "/db/dao/sku/before-without-boms.xml", connection = "enterpriseConnection")
    @ExpectedDatabase(value = "/db/dao/sku/after-update-one-shelflife-template.xml",
            connection = "wmwhseConnection", assertionMode = NON_STRICT)
    @ExpectedDatabase(value = "/db/dao/sku/after-update-one-shelflife-template.xml",
            connection = "enterpriseConnection", assertionMode = NON_STRICT)
    public void updateSkuShelfLifeTemplateForOneSku() {
        SkuId skuId = new SkuId("465852", "ROV0000000000000001459");
        skuDao.updateSkuShelfLifeTemplate(Collections.singleton(skuId), ShelfLifeTemplate.EXPIRATION_DATE, "TEST");
        enterpriseSkuDao.updateSkuShelfLifeTemplate(Collections.singleton(skuId), ShelfLifeTemplate.EXPIRATION_DATE,
                "TEST");
    }

    @Test
    @DatabaseSetup(value = "/db/dao/sku/before-without-boms.xml", connection = "wmwhseConnection")
    @DatabaseSetup(value = "/db/dao/sku/before-without-boms.xml", connection = "enterpriseConnection")
    @ExpectedDatabase(value = "/db/dao/sku/after-update-two-shelflife-templates.xml",
            connection = "wmwhseConnection", assertionMode = NON_STRICT)
    @ExpectedDatabase(value = "/db/dao/sku/after-update-two-shelflife-templates.xml",
            connection = "enterpriseConnection", assertionMode = NON_STRICT)
    public void updateSkuShelfLifeTemplateForTwoSkus() {
        SkuId skuId = new SkuId("465852", "ROV0000000000000001459");
        SkuId skuId2 = new SkuId("465852", "ROV0000000000000001456");
        Set<SkuId> skuIds = new HashSet<>();
        skuIds.add(skuId);
        skuIds.add(skuId2);
        skuDao.updateSkuShelfLifeTemplate(skuIds, ShelfLifeTemplate.MANUFACTURED_AND_EXPIRATION_DATE, "TEST");
        enterpriseSkuDao.updateSkuShelfLifeTemplate(skuIds, ShelfLifeTemplate.MANUFACTURED_AND_EXPIRATION_DATE, "TEST");
    }

    @Test
    @DatabaseSetup(value = "/db/dao/sku/before.xml", connection = "wmwhseConnection")
    @DatabaseSetup(value = "/db/dao/sku/before.xml", connection = "enterpriseConnection")
    @ExpectedDatabase(value = "/db/dao/sku/after-update-pack.xml", assertionMode = NON_STRICT,
            connection = "wmwhseConnection")
    @ExpectedDatabase(value = "/db/dao/sku/after-update-pack.xml", assertionMode = NON_STRICT,
            connection = "enterpriseConnection")
    public void updateSkuPack() {
        ru.yandex.market.wms.common.pojo.Dimensions dimensions =
                new ru.yandex.market.wms.common.pojo.Dimensions.DimensionsBuilder()
                        .weight(BigDecimal.valueOf(12.03))
                        .cube(BigDecimal.valueOf(2.32))
                        .build();
        skuDao.updateSkuPack("465852", "ROV0000000000000001456", "PACK2", dimensions, "user");
        enterpriseSkuDao.updateSkuPack("465852", "ROV0000000000000001456", "PACK2", dimensions, "user");
    }

    @Test
    @DatabaseSetup(value = "/db/dao/sku/before.xml", connection = "wmwhseConnection")
    @DatabaseSetup(value = "/db/dao/sku/before.xml", connection = "enterpriseConnection")
    public void mapManufacturerSkus_emptyInput() {
        Map<UnitId, Sku> mapping = skuDao.mapManufacturerSkus(Collections.emptyList());
        assertions.assertThat(mapping).isEmpty();
    }

    @Test
    @DatabaseSetup(value = "/db/dao/sku/before.xml", connection = "wmwhseConnection")
    @DatabaseSetup(value = "/db/dao/sku/before.xml", connection = "enterpriseConnection")
    public void mapManufacturerSkus() {
        UnitId id = new UnitId("MAN_1456", 465852L, "MAN_1456");
        List<UnitId> unitIds = Lists.newArrayList(id);
        Map<UnitId, Sku> mapping = skuDao.mapManufacturerSkus(unitIds);
        assertions.assertThat(mapping).containsOnlyKeys(id);
        assertions.assertThat(mapping.get(id).getSku()).isEqualTo("ROV0000000000000001456");
        assertions.assertThat(mapping.get(id).getStorerKey()).isEqualTo("465852");
        assertions.assertThat(mapping.get(id).getManufacturerSku()).isEqualTo("MAN_1456");
    }

    private void assertSkuExpected(Sku sku,
                                   AssertSkuExpectedParamOne assertSkuExpectedParamOne,
                                   AssertSkuExpectedParamTwo assertSkuExpectedParamTwo,
                                   AssertSkuExpectedParamThree assertSkuExpectedParamThree) {
        SkuDimensions dimensions = sku.getDimensions();
        assertions.assertThat(sku.getSku()).isEqualTo(assertSkuExpectedParamOne.getExpectedSku());
        assertions.assertThat(sku.getStorerKey()).isEqualTo("465852");
        assertions.assertThat(sku.getManufacturerSku()).
                isEqualTo(assertSkuExpectedParamOne.getExpectedManufacturerSku());
        assertions.assertThat(sku.getDescription()).isEqualTo(assertSkuExpectedParamOne.getExpectedDescription());
        assertions.assertThat(sku.getShelfLifeIndicatorType()).
                isEqualTo(assertSkuExpectedParamOne.getExpectedIndicator());
        assertions.assertThat(sku.getShelfLifeTemplate()).isEqualTo(assertSkuExpectedParamOne.getExpectedTemplate());
        assertions.assertThat(sku.getPutAwayClass()).isEqualTo("5");
        assertions.assertThat(sku.getToExpireDays()).isEqualTo(assertSkuExpectedParamOne.getToExpireDays());
        assertions.assertThat(sku.getShelfLifeOnReceivingDays()).
                isEqualTo(assertSkuExpectedParamOne.getShelfLifeOnReceivingDays());
        assertions.assertThat(sku.getShelfLifeDays()).isEqualTo(assertSkuExpectedParamTwo.getShelfLifeDays());
        assertions.assertThat(sku.getShelfLifeOnReceivingPercentage()).
                isEqualTo(assertSkuExpectedParamTwo.getShelfLifeOnReceivingPercentage());
        assertions.assertThat(sku.getShelfLifePercentage()).
                isEqualTo(assertSkuExpectedParamTwo.getShelfLifePercentage());
        assertions.assertThat(sku.getShelfLifeOnReceivingDaysBeforeReceiving()).
                isEqualTo(assertSkuExpectedParamTwo.getShelfLifeOnReceivingDaysBeforeReceiving());
        assertions.assertThat(sku.getShelfLifeDaysBeforeReceiving()).
                isEqualTo(assertSkuExpectedParamTwo.getShelfLifeDaysBeforeReceiving());
        assertions.assertThat(sku.getShelfLifeEditDate()).isEqualTo(Instant.parse("2020-01-01T00:00:00.000Z"));
        assertions.assertThat(sku.getRotationType()).isEqualTo(assertSkuExpectedParamThree.getRotationType());
        assertions.assertThat(sku.getShelfLifeCodeType()).isEqualTo(assertSkuExpectedParamThree.getShelfLifeCodeType());
        assertions.assertThat(sku.getBoxCount()).isEqualTo(assertSkuExpectedParamThree.getBoxCount());
        assertions.assertThat(sku.getName()).isEqualTo(assertSkuExpectedParamThree.getName());
        assertions.assertThat(dimensions.getGrossWeight()).isEqualByComparingTo(BigDecimal.valueOf(10.05));
        assertions.assertThat(dimensions.getNetWeight()).isEqualByComparingTo(BigDecimal.valueOf(9.33));
        assertions.assertThat(dimensions.getCube()).isEqualByComparingTo(BigDecimal.valueOf(1.54));
    }
}
