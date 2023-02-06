package ru.yandex.chemodan.app.psbilling.core.dao.users.impl;

import java.util.UUID;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.chemodan.app.psbilling.core.AbstractPsBillingCoreTest;
import ru.yandex.chemodan.app.psbilling.core.dao.products.UserProductDao;
import ru.yandex.chemodan.app.psbilling.core.entities.products.BillingType;
import ru.yandex.chemodan.app.psbilling.core.entities.products.UserProductEntity;

public class UserProductDaoTest extends AbstractPsBillingCoreTest {

    @Autowired
    private UserProductDao userProductDao;

    @Test
    public void createAndFind() {
        UserProductDao.InsertData data = UserProductDao.InsertData.builder()
                .code(UUID.randomUUID().toString())
                .codeFamily(UUID.randomUUID().toString())
                .productOwnerId(psBillingProductsFactory.getOrCreateProductOwner().getId())
                .billingType(BillingType.GROUP).build();

        UserProductEntity userProduct = userProductDao.insert(data);

        Assert.assertEquals(userProduct.getCode(), data.getCode());
        Assert.assertEquals(userProduct.getCodeFamily(), data.getCodeFamily());
        Assert.assertEquals(userProduct.getProductOwnerId(), data.getProductOwnerId());
        Assert.assertEquals(userProduct.getBillingType(), data.getBillingType());
    }

}
