package ru.yandex.chemodan.app.psbilling.core.products;

import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.bolts.collection.Tuple2;
import ru.yandex.chemodan.app.psbilling.core.AbstractPsBillingCoreTest;
import ru.yandex.chemodan.app.psbilling.core.PsBillingProductsFactory;
import ru.yandex.chemodan.app.psbilling.core.dao.products.ProductLineDao;
import ru.yandex.chemodan.app.psbilling.core.dao.products.ProductSetDao;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.Group;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.GroupService;
import ru.yandex.chemodan.app.psbilling.core.entities.products.ProductLineEntity;
import ru.yandex.chemodan.app.psbilling.core.entities.products.ProductSetEntity;
import ru.yandex.chemodan.app.psbilling.core.synchronization.engine.Target;
import ru.yandex.chemodan.app.uaas.experiments.ExperimentsManager;
import ru.yandex.chemodan.directory.client.DirectoryClient;
import ru.yandex.chemodan.directory.client.DirectoryOrganizationFeaturesResponse;
import ru.yandex.inside.passport.PassportUid;
import ru.yandex.misc.test.Assert;

public class GroupProductManagerTest extends AbstractPsBillingCoreTest {

    @Autowired
    private ProductLineDao productLineDao;
    @Autowired
    private ProductSetDao productSetDao;
    @Autowired
    private GroupProductManager groupProductManager;
    @Autowired
    private DirectoryClient directoryClient;
    @Autowired
    private ExperimentsManager experimentsManager;

    @Test
    public void testProductLineAvailableSelectors() {
        Option<PassportUid> uid = Option.of(PassportUid.cons(3000185708L));
        Option<Group> group = Option.empty();

        ProductSetEntity productSet = productSetDao.create(ProductSetDao.InsertData.builder().key("group_1").build());
        ProductLineEntity availableProductLine = productLineDao
                .create(ProductLineDao.InsertData.builder().productSetId(productSet.getId()).orderNum(1)
                        .selectorBeanEL("productLineSelectorFactory.availableSelector()").build());

        GroupProductQuery query = new GroupProductQuery(
                productSet.getKey(), uid, group, Cf.list(), Option.empty()
        );

        Option<LimitedTimeProductLineB2b> productLine = groupProductManager.findProductLineWithPromo(query);
        Assert.assertSome(productLine);
        ProductLineEntity lineSelected = productLine.get().getProductLine();
        Assert.equals(availableProductLine.getId(), lineSelected.getId());
    }

    @Test
    public void testProductLineByEducationalSelectors() {
        Group group = psBillingGroupsFactory.createGroup();
        Option<PassportUid> uid = Option.of(PassportUid.cons(1111));

        ProductSetEntity productSet = productSetDao.create(ProductSetDao.InsertData.builder().key("111").build());
        ProductLineEntity educationalProductLine = productLineDao
                .create(ProductLineDao.InsertData.builder().productSetId(productSet.getId()).orderNum(1)
                        .selectorBeanEL(
                                "productLineSelectorFactory.educationalGroupProductSelector()")
                        .build());
        ProductLineEntity availableByDefaultProductLine = productLineDao
                .create(ProductLineDao.InsertData.builder().productSetId(productSet.getId()).orderNum(2)
                        .selectorBeanEL(
                                "productLineSelectorFactory.availableSelector()")
                        .build());

        Mockito.when(directoryClient
                .getOrganizationFeatures(Mockito.anyString()))
                .thenReturn(new DirectoryOrganizationFeaturesResponse(true, false));

        GroupProductQuery query = new GroupProductQuery(
                productSet.getKey(), uid, Option.of(group), Cf.list(), Option.empty()
        );

        Option<LimitedTimeProductLineB2b> productLine = groupProductManager.findProductLineWithPromo(query);
        Assert.assertSome(productLine);
        ProductLineEntity lineSelected = productLine.get().getProductLine();
        Assert.equals(educationalProductLine.getId(), lineSelected.getId());
        Assert.notEquals(availableByDefaultProductLine.getId(), lineSelected.getId());
    }

    @Test
    public void testProductLineExperimentByOwnerSelectors() {
        String experiment = "test_experiment";

        PassportUid owner = PassportUid.cons(1234);
        Group group = psBillingGroupsFactory.createGroup(c -> c.ownerUid(owner));
        PassportUid owner2 = PassportUid.cons(5678);
        Group group2 = psBillingGroupsFactory.createGroup(c -> c.ownerUid(owner2));
        Option<PassportUid> uid = Option.of(PassportUid.cons(1111));

        ProductSetEntity productSet = productSetDao.create(ProductSetDao.InsertData.builder().key("111").build());
        ProductLineEntity experimentalProductLine = productLineDao
                .create(ProductLineDao.InsertData.builder().productSetId(productSet.getId()).orderNum(1)
                        .selectorBeanEL(
                                String.format(
                                    "productLineSelectorFactory.experimentByOwnerIsActiveSelector(\"%s\")",
                                    experiment))
                        .build());
        ProductLineEntity availableByDefaultProductLine = productLineDao
                .create(ProductLineDao.InsertData.builder().productSetId(productSet.getId()).orderNum(2)
                        .selectorBeanEL(
                                "productLineSelectorFactory.availableSelector()")
                        .build());

        Mockito.when(experimentsManager.getFlags(owner.getUid()))
                .thenReturn(Cf.list(experiment));

        // группа в экспе
        GroupProductQuery query = new GroupProductQuery(
                productSet.getKey(), uid, Option.of(group), Cf.list(), Option.empty()
        );

        Option<LimitedTimeProductLineB2b> productLine = groupProductManager.findProductLineWithPromo(query);

        Assert.assertSome(productLine);
        ProductLineEntity lineSelected = productLine.get().getProductLine();
        Assert.equals(experimentalProductLine.getId(), lineSelected.getId());
        Assert.notEquals(availableByDefaultProductLine.getId(), lineSelected.getId());

        // группа не в экспе

        GroupProductQuery query2 = new GroupProductQuery(
                productSet.getKey(), uid, Option.of(group2), Cf.list(), Option.empty()
        );

        productLine = groupProductManager.findProductLineWithPromo(query2);
        Assert.assertSome(productLine);
        lineSelected = productLine.get().getProductLine();
        Assert.equals(availableByDefaultProductLine.getId(), lineSelected.getId());
    }

    @Test
    public void findActiveProductsTest_empty() {
        Group group = psBillingGroupsFactory.createGroup();
        Assert.equals(Cf.set(), groupProductManager.getActiveProducts(group));
    }

    @Test
    public void findActiveProductsTest_enabled() {
        GroupService groupService = psBillingGroupsFactory.createGroupService();
        Assert.equals(Cf.list(groupService.getGroupProductId()), groupProductManager.getActiveProducts(groupService.getGroupId()).map(GroupProduct::getId));

    }

    @Test
    public void findActiveProductsTest_disabled() {
        GroupService groupService = psBillingGroupsFactory.createGroupService(Target.DISABLED);
        Assert.equals(Cf.set(), groupProductManager.getActiveProducts(groupService.getGroupId()));
    }

    @Test
    public void findGroupProductsWithPromoTest_filterOutAddon() {
        psBillingProductsFactory.createMainsAndAddons();
        Group group = psBillingGroupsFactory.createGroup();
        String addonSet = PsBillingProductsFactory.DEFAULT_ADDONS_SET;
        LimitedTimeGroupProducts products =
                groupProductManager.findGroupProductsWithPromo(new GroupProductQuery(addonSet,
                        Option.of(group.getOwnerUid()), Option.of(group), Cf.list(), Option.empty()));
        Assert.equals(Cf.list(), products.getGroupProducts());
    }

    @Test
    public void findGroupProductsWithPromoTest_showAddon() {
        Tuple2<ListF<GroupProduct>, ListF<GroupProduct>> mainsAndAddons =
                psBillingProductsFactory.createMainsAndAddons();
        GroupProduct mainProduct = mainsAndAddons.get1().get(0);
        ListF<GroupProduct> addons = mainsAndAddons.get2();
        Group group = psBillingGroupsFactory.createGroup();
        psBillingGroupsFactory.createGroupService(group, mainProduct);

        GroupProductQuery query = new GroupProductQuery(PsBillingProductsFactory.DEFAULT_ADDONS_SET,
                Option.of(group.getOwnerUid()), Option.of(group), Cf.list(), Option.empty());
        LimitedTimeGroupProducts products = groupProductManager.findGroupProductsWithPromo(query);

        Assert.equals(addons, products.getGroupProducts());
    }

    @Test
    public void findGroupProductsWithPromoTest_showAddonForChild() {
        Tuple2<ListF<GroupProduct>, ListF<GroupProduct>> mainsAndAddons =
                psBillingProductsFactory.createMainsAndAddons();
        GroupProduct mainProduct = mainsAndAddons.get1().get(0);
        ListF<GroupProduct> addons = mainsAndAddons.get2();
        Group parent = psBillingGroupsFactory.createGroup();
        psBillingGroupsFactory.createGroupService(parent, mainProduct);

        Group child = psBillingGroupsFactory.createGroup(builder -> builder.parentGroupId(Option.of(parent.getId())));

        GroupProductQuery query = new GroupProductQuery(PsBillingProductsFactory.DEFAULT_ADDONS_SET,
                Option.of(child.getOwnerUid()), Option.of(child), Cf.list(), Option.empty());


        LimitedTimeGroupProducts products = groupProductManager.findGroupProductsWithPromo(query);

        Assert.equals(addons, products.getGroupProducts());
    }
}
