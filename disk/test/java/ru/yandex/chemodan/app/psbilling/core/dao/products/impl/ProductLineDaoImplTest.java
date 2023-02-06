package ru.yandex.chemodan.app.psbilling.core.dao.products.impl;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.chemodan.app.psbilling.core.AbstractPsBillingCoreTest;
import ru.yandex.chemodan.app.psbilling.core.dao.products.ProductLineDao;
import ru.yandex.chemodan.app.psbilling.core.dao.products.ProductSetDao;
import ru.yandex.chemodan.app.psbilling.core.entities.products.ProductLineEntity;
import ru.yandex.chemodan.app.psbilling.core.entities.products.ProductSetEntity;

public class ProductLineDaoImplTest extends AbstractPsBillingCoreTest {

    @Autowired
    public ProductLineDao productLineDao;
    @Autowired
    public ProductSetDao productSetDao;

    @Test
    public void findSetLines() {
        ProductSetEntity productSet = productSetDao.create(ProductSetDao.InsertData.builder().key("111").build());
        ProductLineEntity productLine = createLine(productSet, 1);

        ListF<ProductLineEntity> productLines = productLineDao.findByProductSetId(productSet.getId());
        Assert.assertEquals(1, productLines.size());
        Assert.assertEquals(productLine, productLines.first());
    }

    @Test
    public void findMultipleSetsLines() {
        ProductSetEntity productSet1 = productSetDao.create(ProductSetDao.InsertData.builder().key("set-1").build());
        ProductSetEntity productSet2 = productSetDao.create(ProductSetDao.InsertData.builder().key("set-2").build());
        ProductSetEntity productSet3 = productSetDao.create(ProductSetDao.InsertData.builder().key("set-3").build());

        ProductLineEntity productLine1 = createLine(productSet1, 1);
        ProductLineEntity productLine2 = createLine(productSet2, 1);
        createLine(productSet3, 1);

        ListF<ProductLineEntity> productLines = productLineDao.findByProductSetIds(
                Cf.list(productSet1, productSet2).map(ProductSetEntity::getId));

        Assert.assertEquals(2, productLines.size());
        Assert.assertTrue(productLines.containsTs(productLine1));
        Assert.assertTrue(productLines.containsTs(productLine2));
    }

    private ProductLineEntity createLine(ProductSetEntity set, int orderNum) {
        return productLineDao
                .create(ProductLineDao.InsertData.builder().productSetId(set.getId()).orderNum(orderNum).build());
    }
}
