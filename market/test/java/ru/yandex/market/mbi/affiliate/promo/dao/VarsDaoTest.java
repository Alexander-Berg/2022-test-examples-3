package ru.yandex.market.mbi.affiliate.promo.dao;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.db.DbUnitTestExecutionListener;
import ru.yandex.market.mbi.affiliate.promo.config.FunctionalTestConfig;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = FunctionalTestConfig.class)
@TestExecutionListeners(listeners = {
        DbUnitTestExecutionListener.class,
}, mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS)
public class VarsDaoTest {

    @Autowired
    private VarsDao dao;

    @Test
    @DbUnitDataSet(dataSource = "promoDataSource", before = "vars_before.csv")
    public void testGet() {
        assertThat(dao.getVar("good morning"), is("Guten Tag"));
    }

    @Test
    @DbUnitDataSet(dataSource = "promoDataSource", before = "vars_before.csv")
    public void testGetNull() {
        assertThat(dao.getVar("bye"), nullValue());
    }

    @Test
    @DbUnitDataSet(dataSource = "promoDataSource", before = "vars_before.csv", after = "vars_after_insert.csv")
    public void testInsert() {
        dao.setVar("bye", "Auf Wiedersehen");
    }

    @Test
    @DbUnitDataSet(dataSource = "promoDataSource", before = "vars_before.csv", after = "vars_after_update.csv")
    public void testUpdate() {
        dao.setVar("good morning", "Bonjour");
    }

    @Test
    @DbUnitDataSet(dataSource = "promoDataSource", before = "vars_before.csv", after = "vars_after_delete.csv")
    public void testDelete() {
        dao.setVar("good morning", null);
    }
}