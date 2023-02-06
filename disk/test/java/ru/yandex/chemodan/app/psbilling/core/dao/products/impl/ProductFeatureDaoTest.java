package ru.yandex.chemodan.app.psbilling.core.dao.products.impl;

import java.math.BigDecimal;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.app.psbilling.core.AbstractPsBillingCoreTest;
import ru.yandex.chemodan.app.psbilling.core.dao.products.ProductFeatureDao;
import ru.yandex.chemodan.app.psbilling.core.entities.products.FeatureEntity;
import ru.yandex.chemodan.app.psbilling.core.entities.products.FeatureScope;
import ru.yandex.chemodan.app.psbilling.core.entities.products.FeatureType;
import ru.yandex.chemodan.app.psbilling.core.entities.products.ProductFeatureEntity;
import ru.yandex.chemodan.app.psbilling.core.entities.products.UserProductEntity;
import ru.yandex.chemodan.app.psbilling.core.entities.texts.TankerKeyEntity;
import ru.yandex.misc.test.Assert;

public class ProductFeatureDaoTest extends AbstractPsBillingCoreTest {

    @Autowired
    private ProductFeatureDao productFeatureDao;

    @Test
    public void create() {
        UserProductEntity userProduct = psBillingProductsFactory.createUserProduct();
        FeatureEntity feature = psBillingProductsFactory.createFeature(FeatureType.TOGGLEABLE);
        TankerKeyEntity tankerKeyEntity = psBillingTextsFactory.create();

        ProductFeatureEntity inserted = productFeatureDao.insert(ProductFeatureDao.InsertData.builder()
                .code("code")
                .amount(BigDecimal.TEN)
                .descriptionTankerKeyId(Option.of(tankerKeyEntity.getId()))
                .featureId(Option.of(feature.getId()))
                .orderNum(1)
                .userProductId(userProduct.getId())
                .scope(FeatureScope.USER)
                .build());

        Assert.equals(inserted.getCode(), "code");
        Assert.equals(inserted.getAmount(), BigDecimal.TEN);
        Assert.equals(inserted.getDescriptionTankerKeyId(), Option.of(tankerKeyEntity.getId()));
        Assert.equals(inserted.getFeatureId(), Option.of(feature.getId()));
        Assert.equals(inserted.getOrderNum(), 1);
        Assert.equals(inserted.getUserProductId(), userProduct.getId());
        Assert.equals(inserted.getScope(), FeatureScope.USER);
    }
}
