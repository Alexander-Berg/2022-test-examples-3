package ru.yandex.chemodan.app.psbilling.core.dao.products.impl;

import java.util.UUID;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.CollectionF;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.function.Function;
import ru.yandex.chemodan.app.psbilling.core.AbstractPsBillingCoreTest;
import ru.yandex.chemodan.app.psbilling.core.dao.products.ProductLineDao;
import ru.yandex.chemodan.app.psbilling.core.dao.products.ProductSetDao;
import ru.yandex.chemodan.app.psbilling.core.dao.products.UserProductDao;
import ru.yandex.chemodan.app.psbilling.core.dao.products.UserProductPricesDao;
import ru.yandex.chemodan.app.psbilling.core.entities.CustomPeriodUnit;
import ru.yandex.chemodan.app.psbilling.core.entities.products.ProductLineEntity;
import ru.yandex.chemodan.app.psbilling.core.entities.products.ProductSetEntity;
import ru.yandex.chemodan.app.psbilling.core.entities.products.UserProductEntity;
import ru.yandex.chemodan.app.psbilling.core.entities.products.UserProductPriceEntity;
import ru.yandex.chemodan.app.psbilling.core.products.UserProduct;
import ru.yandex.chemodan.app.psbilling.core.products.UserProductManager;
import ru.yandex.chemodan.app.psbilling.core.products.UserProductPrice;

public class ProductCardDaoImplTest extends AbstractPsBillingCoreTest {
    @Autowired
    public ProductLineDao productLineDao;
    @Autowired
    public ProductSetDao productSetDao;
    @Autowired
    public UserProductDao userProductDao;
    @Autowired
    public UserProductPricesDao pricesDao;
    @Autowired
    public UserProductManager userProductManager;

    @Test
    public void test() {
        ProductSetEntity productSet = productSetDao.create(ProductSetDao.InsertData.builder().key("111").build());
        ProductLineEntity productLine = productLineDao
                .create(ProductLineDao.InsertData.builder().productSetId(productSet.getId()).orderNum(1).build());

        UserProductEntity userProduct1 = psBillingProductsFactory.createUserProduct();
        UserProductEntity userProduct2 = psBillingProductsFactory.createUserProduct();
        productLineDao.bindUserProducts(productLine.getId(), Cf.list(userProduct1.getId(), userProduct2.getId()));
        CollectionF<UserProductEntity> products = userProductDao.findByProductLine(productLine.getId());
        Assert.assertEquals(Cf.set(userProduct1, userProduct2), Cf.toSet(products));

        UserProductPriceEntity price11 =
                psBillingProductsFactory.createUserProductPrices(userProduct1, CustomPeriodUnit.ONE_MONTH);
        UserProductPriceEntity price12 =
                psBillingProductsFactory.createUserProductPrices(userProduct1, CustomPeriodUnit.ONE_YEAR);

        UserProductPriceEntity price21 =
                psBillingProductsFactory.createUserProductPrices(userProduct2, CustomPeriodUnit.ONE_MONTH);
        UserProductPriceEntity price22 =
                psBillingProductsFactory.createUserProductPrices(userProduct2, CustomPeriodUnit.ONE_YEAR);

        ListF<UserProduct> dbProducts =
                userProductManager.findByIds(Cf.list(userProduct1.getId(), userProduct2.getId()));
        Assert.assertEquals(2, dbProducts.size());
        UserProduct dbProduct1 = dbProducts.find(p -> p.getId().equals(userProduct1.getId())).get();
        UserProduct dbProduct2 = dbProducts.find(p -> p.getId().equals(userProduct2.getId())).get();
        ListF<UUID> product1DbPrices =
                dbProduct1.getProductPrices().values().flatMap(Function.identityF()).map(UserProductPrice::getId);
        ListF<UUID> product2DbPrices =
                dbProduct2.getProductPrices().values().flatMap(Function.identityF()).map(UserProductPrice::getId);

        Assert.assertEquals(Cf.list(price11.getId(), price12.getId()).sorted(), product1DbPrices.sorted());
        Assert.assertEquals(Cf.list(price21.getId(), price22.getId()).sorted(), product2DbPrices.sorted());
    }

}
