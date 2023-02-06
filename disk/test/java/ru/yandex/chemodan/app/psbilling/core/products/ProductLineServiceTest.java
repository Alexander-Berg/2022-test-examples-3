package ru.yandex.chemodan.app.psbilling.core.products;

import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.SetF;
import ru.yandex.chemodan.app.psbilling.core.dao.products.ProductLineDao;
import ru.yandex.chemodan.app.psbilling.core.entities.products.ProductLineEntity;
import ru.yandex.misc.test.Assert;

public class ProductLineServiceTest  {

    private ProductLineService service;

    private ProductLineDao productLineDao;

    @Before
    public void setUp() throws Exception {
        this.productLineDao = Mockito.mock(ProductLineDao.class);
        this.service = new ProductLineService(this.productLineDao);
    }

    @Test
    public void getProductSetIds() {
        UUID expected = UUID.randomUUID();
        ProductLineEntity entity = Mockito.mock(ProductLineEntity.class);
        Mockito.when(entity.getProductSetId()).thenReturn(expected);

        SetF<UUID> lines = Cf.set(UUID.randomUUID());
        lines.forEach(l -> Mockito.when(productLineDao.findById(l)).thenReturn(entity));

        Assert.equals(service.getProductSetIds(lines).single(), expected);
    }

    @Test
    public void getProductSetIdsUnique() {
        UUID expected = UUID.randomUUID();
        ProductLineEntity entity = Mockito.mock(ProductLineEntity.class);
        Mockito.when(entity.getProductSetId()).thenReturn(expected);

        SetF<UUID> lines = Cf.set(UUID.randomUUID(), UUID.randomUUID());
        lines.forEach(l -> Mockito.when(productLineDao.findById(l)).thenReturn(entity));

        Assert.equals(service.getProductSetIds(lines).single(), expected);
    }
}
