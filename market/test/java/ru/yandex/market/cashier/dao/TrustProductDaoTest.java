package ru.yandex.market.cashier.dao;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.market.cashier.AbstractApplicationTest;
import ru.yandex.market.cashier.entities.TrustProductEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


public class TrustProductDaoTest  extends AbstractApplicationTest {

    @Autowired
    private TrustProductDao trustProductDao;

    @Test
    @Transactional
    public void testFindProduct() {
        List<TrustProductEntity> entities = trustProductDao.find("dsss", "sss");
        assertEquals(0, entities.size());

        trustProductDao.create(entity("sss","dsss"));

        entities = trustProductDao.find("dsss", "sss");
        assertEquals(1, entities.size());
    }

    @Transactional
    @Test
    public void testCreate(){
        trustProductDao.create(entity("sss","dsss"));
        trustProductDao.flush();

        List<TrustProductEntity> entities = trustProductDao.find("dsss", "sss");
        assertEquals(1, entities.size());
        TrustProductEntity entity = entities.iterator().next();
        assertNotNull(entity);
        assertNotNull(entity.getCreationDate());
        assertEquals(entity.getCreationDate(), entity.getUpdateDate());
    }

    private static TrustProductEntity entity(String productId, String serviceTokenHash){
        TrustProductEntity entity = new TrustProductEntity();
        entity.setServiceTokenHash(serviceTokenHash);
        entity.setServiceProductId(productId);
        entity.setProductName("someName");
        entity.setPartnerId(11111L);
        entity.setServiceFee(1);
        return entity;
    }
}
