package ru.yandex.chemodan.app.psbilling.core.dao.products.impl;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.app.psbilling.core.AbstractPsBillingCoreTest;
import ru.yandex.chemodan.app.psbilling.core.dao.products.ProductSetDao;
import ru.yandex.chemodan.app.psbilling.core.entities.products.ProductSetEntity;

public class ProductSetDaoImplTest extends AbstractPsBillingCoreTest {

    @Autowired
    private ProductSetDao productSetDao;

    @Test
    public void test() {
        ProductSetEntity productSet = productSetDao.create(ProductSetDao.InsertData.builder().key("1111").build());
        Option<ProductSetEntity> dbEntity = productSetDao.findByKey("1111");
        Assert.assertTrue(dbEntity.isPresent());
        Assert.assertEquals(productSet, dbEntity.get());
    }
}
