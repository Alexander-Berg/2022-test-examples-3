package ru.yandex.chemodan.app.psbilling.core.dao.groups.impl;

import java.util.UUID;

import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.bolts.collection.Tuple2;
import ru.yandex.chemodan.app.psbilling.core.AbstractPsBillingCoreTest;
import ru.yandex.chemodan.app.psbilling.core.dao.groups.GroupProductDao;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.GroupProductEntity;
import ru.yandex.chemodan.app.psbilling.core.products.GroupProduct;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


public class GroupProductDaoImplTest extends AbstractPsBillingCoreTest {
    @Autowired
    private GroupProductDao groupProductDao;

    @Test
    public void findProductByCode() {
        Option<GroupProductEntity> productO = groupProductDao.findProductByCode("somecode");
        Assert.assertFalse(productO.isPresent());

        GroupProductEntity afterInsert = groupProductDao.findById(
                psBillingProductsFactory.createGroupProduct(builder -> builder.code("zzzzzz")).getId());

        productO = groupProductDao.findProductByCode("zzzzzz");
        assertTrue(productO.isPresent());
        GroupProductEntity product = productO.get();
        Assert.assertEquals(afterInsert, product);
    }

    @Test
    public void findByIds() {
        UUID id1 = psBillingProductsFactory.createGroupProduct().getId();
        UUID id2 = psBillingProductsFactory.createGroupProduct().getId();
        psBillingProductsFactory.createGroupProduct();

        ListF<GroupProductEntity> products = groupProductDao.findByIds(Cf.list(id1, id2));
        ListF<UUID> selectedIds = products.map(GroupProductEntity::getId);
        assertEquals(2, selectedIds.length());
        MatcherAssert.assertThat(selectedIds, CoreMatchers.hasItems(id1, id2));
    }

    @Test
    public void findAddons() {
        Tuple2<ListF<GroupProduct>, ListF<GroupProduct>> mainAndAddons = createMainsAndAddons(false);
        GroupProduct main = mainAndAddons.get1().get(0);
        Assert.assertTrue(main.getAvailableAddons().containsAllTs(mainAndAddons.get2()));
        Assert.assertEquals(Cf.list(), main.getEligibleMainProducts());
    }

    @Test
    public void findMains() {
        Tuple2<ListF<GroupProduct>, ListF<GroupProduct>> mainAndAddons = createMainsAndAddons(false);
        GroupProduct addon = mainAndAddons.get2().get(0);
        Assert.assertTrue(addon.getEligibleMainProducts().containsAllTs(mainAndAddons.get1()));
        Assert.assertEquals(Cf.list(), addon.getAvailableAddons());
    }

    @Test
    public void findUnlinkedMain() {
        Tuple2<ListF<GroupProduct>, ListF<GroupProduct>> mainAndAddons = createMainsAndAddons(true);
        GroupProduct main = mainAndAddons.get1().get(0);
        Assert.assertEquals(Cf.list(), main.getAvailableAddons());
    }

    @Test
    public void findUnlinkedAddons() {
        Tuple2<ListF<GroupProduct>, ListF<GroupProduct>> mainAndAddons = createMainsAndAddons(true);
        GroupProduct addon = mainAndAddons.get2().get(0);
        Assert.assertEquals(Cf.list(), addon.getEligibleMainProducts());
    }


    private Tuple2<ListF<GroupProduct>, ListF<GroupProduct>> createMainsAndAddons(boolean unlink) {
        Tuple2<ListF<GroupProduct>, ListF<GroupProduct>> mainsAndAddons =
                psBillingProductsFactory.createMainsAndAddons();
        if (unlink) {
            for (GroupProduct main : mainsAndAddons._1) {
                for (GroupProduct addon : mainsAndAddons._2) {
                    groupProductDao.unlinkAddon(main, addon);
                }
            }
        }
        return mainsAndAddons;
    }
}
