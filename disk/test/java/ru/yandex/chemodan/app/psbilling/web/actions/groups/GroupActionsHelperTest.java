package ru.yandex.chemodan.app.psbilling.web.actions.groups;

import java.util.List;
import java.util.UUID;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.bolts.collection.Tuple2;
import ru.yandex.chemodan.app.psbilling.core.AbstractPsBillingCoreTest;
import ru.yandex.chemodan.app.psbilling.core.PsBillingGroupsFactory;
import ru.yandex.chemodan.app.psbilling.core.PsBillingProductsFactory;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.Group;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.GroupService;
import ru.yandex.chemodan.app.psbilling.core.groups.GroupsManager;
import ru.yandex.chemodan.app.psbilling.core.products.GroupProduct;
import ru.yandex.chemodan.app.psbilling.core.products.GroupProductManager;
import ru.yandex.chemodan.app.psbilling.core.synchronization.engine.Target;
import ru.yandex.chemodan.app.psbilling.web.PsBillingWebTestConfig;
import ru.yandex.chemodan.app.psbilling.web.model.GroupAddonPojo;
import ru.yandex.chemodan.app.psbilling.web.model.GroupServicePojo;

@ContextConfiguration(classes = {
        PsBillingWebTestConfig.class,
        GroupActionsTest.BalanceMockConfiguration.class
})
public class GroupActionsHelperTest extends AbstractPsBillingCoreTest {
    @Autowired
    PsBillingGroupsFactory groupsFactory;
    @Autowired
    PsBillingProductsFactory productsFactory;
    @Autowired
    GroupProductManager groupProductManager;
    @Autowired
    GroupsManager groupsManager;
    private Group parentGroup;
    private GroupProduct main;
    private GroupProduct addon;
    @Autowired
    GroupActionsHelper groupActionsHelper;
    private Group childGroup;


    @Before
    public void setUp() throws Exception {
        parentGroup = groupsFactory.createGroup();
        childGroup = groupsFactory.createGroup(builder -> builder.parentGroupId(Option.of(parentGroup.getId())));
        Tuple2<ListF<GroupProduct>, ListF<GroupProduct>> mainsAndAddons = productsFactory.createMainsAndAddons();
        main = mainsAndAddons.get1().get(0);
        addon = mainsAndAddons.get2().get(0);
    }

    @Test
    public void findWithEmptyTitleProducts() {
        GroupProduct noTitleProduct =
                productsFactory.createGroupProduct(builder -> builder.titleTankerKeyId(Option.empty()));
        groupsFactory.createGroupService(parentGroup, noTitleProduct);
        validateGroupServices(Cf.list());
    }

    @Test
    public void findMainService() {
        List<GroupService> expectedList = Cf.list(groupsFactory.createGroupService(parentGroup, main));
        validateGroupServices(expectedList);
    }

    @Test
    public void findMainServiceCheckNoAddons() {
        List<GroupService> expectedList = Cf.list(groupsFactory.createGroupService(parentGroup, main));
        groupsFactory.createGroupService(childGroup, addon);
        validateGroupServices(expectedList);
    }

    @Test
    public void findAddon() {
        groupsFactory.createGroupService(parentGroup, main);
        List<GroupService> expectedList = Cf.list(groupsFactory.createGroupService(parentGroup, addon));
        validateAddons(expectedList);

    }

    @Test
    public void findAddonWithSubgroup() {
        groupsFactory.createGroupService(parentGroup, main);
        List<GroupService> expectedList = Cf.list(groupsFactory.createGroupService(childGroup, addon));
        validateAddons(expectedList);
    }

    private void validateGroupServices(List<GroupService> groupServices) {
        List<GroupServicePojo> pojos = groupActionsHelper.getGroupServices(parentGroup, Option.empty(), uid,
                Option.empty(), Target.ENABLED).getItems();
        Assert.assertEquals(groupServices.size(), pojos.size());
        for (GroupService groupService : groupServices) {
            Assert.assertTrue(pojos.stream().anyMatch(pojo -> isMatch(groupService, pojo)));
        }
    }

    private void validateAddons(List<GroupService> groupServices) {
        List<GroupAddonPojo> pojos = groupActionsHelper.getGroupAddons(parentGroup, Option.empty(), uid,
                Option.empty(), Target.ENABLED).getAddons();
        Assert.assertEquals(groupServices.size(), pojos.size());
        for (GroupService groupService : groupServices) {
            Assert.assertTrue(pojos.stream().anyMatch(pojo -> isMatch(groupService, pojo)));
        }
    }

    private boolean isMatch(GroupService groupService, GroupServicePojo groupServicePojo) {
        GroupProduct groupProduct = groupProductManager.findById(groupService.getGroupProductId());
        return groupService.getId().equals(UUID.fromString(groupServicePojo.getGroupServiceId())) &&
                groupProduct.getCode().equals(groupServicePojo.getProduct().getProductId());
    }

    private boolean isMatch(GroupService groupService, GroupAddonPojo addonPojo) {
        GroupServicePojo groupServicePojo = addonPojo.getGroupService();
        Group group = groupsManager.findById(groupService.getGroupId());
        return isMatch(groupService, groupServicePojo) && isMatch(group, addonPojo);
    }

    private boolean isMatch(Group group, GroupAddonPojo addonPojo) {
        return addonPojo.getGroupType().toCoreEnum().equals(group.getType()) &&
                group.getExternalId().equals(addonPojo.getGroupExternalId());
    }
}
