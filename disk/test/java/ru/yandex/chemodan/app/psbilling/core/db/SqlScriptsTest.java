package ru.yandex.chemodan.app.psbilling.core.db;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;

import ru.yandex.bolts.collection.ListF;
import ru.yandex.chemodan.app.psbilling.core.AbstractPsBillingCoreTest;
import ru.yandex.chemodan.app.psbilling.core.dao.products.ProductOwnerDao;
import ru.yandex.chemodan.app.psbilling.core.entities.products.ProductOwner;

public class SqlScriptsTest extends AbstractPsBillingCoreTest {
    @Autowired
    private ProductOwnerDao productOwnerDao;

    @Sql("fill_db_example.sql")
    @Test
    public void testSqlAnnotation() {
        ListF<ProductOwner> owners = productOwnerDao.findAll();
        Assert.assertEquals(1, owners.size());
        Assert.assertEquals("test_owner_code", owners.single().getCode());
    }
}
