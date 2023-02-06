package ru.yandex.market.tsum.registry.v2.dao;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import ru.yandex.market.tsum.core.TestMongo;
import ru.yandex.market.tsum.core.auth.TsumRole;
import ru.yandex.market.tsum.registry.v2.*;
import ru.yandex.market.tsum.registry.v2.dao.model.Component;
import ru.yandex.market.tsum.registry.v2.dao.model.ComponentsGroup;
import ru.yandex.market.tsum.registry.v2.dao.model.Service;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Collections.singletonList;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {TestMongo.class, ServicesDao.class, ComponentsGroupsDao.class, ComponentsDao.class,
    DutiesDao.class, InstallationsDao.class, ResourceDao.class, GeneralDao.class, RegistryRolesProvider.class,
    ComponentUpdateRequestDao.class, ComponentSpecsDao.class})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class RegistryRolesProviderTest {

    @Autowired
    private ServicesDao servicesDao;

    @Autowired
    private ComponentsGroupsDao componentsGroupsDao;

    @Autowired
    private ComponentsDao componentsDao;

    private RegistryRolesProvider registryRolesProvider;

    private GeneralDao generalDao;

    private List<String> serviceIds;
    private List<String> componentsGroupIds;
    private List<String> componentIds;


    private List<String> buildServices() {
        Service service0 = new TestServiceBuilder()
            .withName("service0")
            .withRandomAbcSlug()
            .build();
        Service service1 = new TestServiceBuilder()
            .withName("service1")
            .withRandomAbcSlug()
            .build();

        //Should be ignored while building role tree
        Service serviceWithNullFields = new TestServiceBuilder().build();

        List<Service> services = Arrays.asList(service0, service1, serviceWithNullFields);
        services.forEach(service -> servicesDao.save(service));

        return services.stream()
            .filter(service -> service.getName() != null)
            .map(Service::getId)
            .collect(Collectors.toList());
    }

    private List<String> buildComponentGroups() {
        ComponentsGroup componentsGroup0 = new TestComponentsGroupBuilder()
            .withName("componentsGroup0")
            .withServiceId(serviceIds.get(0))
            .build();
        ComponentsGroup componentsGroup1 = new TestComponentsGroupBuilder()
            .withName("componentsGroup1")
            .withServiceId(serviceIds.get(0))
            .build();

        ComponentsGroup componentsGroup2 = new TestComponentsGroupBuilder()
            .withName("componentsGroup2")
            .withServiceId(serviceIds.get(1))
            .build();
        ComponentsGroup componentsGroup3 = new TestComponentsGroupBuilder()
            .withName("componentsGroup3")
            .withServiceId(serviceIds.get(1))
            .withParentComponentsGroupId(componentsGroup2.getId())
            .build();

        //Should be ignored while building role tree
        ComponentsGroup componentsGroupWithNullFields = new TestComponentsGroupBuilder()
            .withServiceId(serviceIds.get(0))
            .build();

        List<ComponentsGroup> componentsGroups = Arrays.asList(componentsGroup0, componentsGroup1,
            componentsGroup2, componentsGroup3, componentsGroupWithNullFields);

        componentsGroups.forEach(componentsGroup -> componentsGroupsDao.save(componentsGroup));

        return componentsGroups.stream()
            .filter(service -> service.getName() != null)
            .map(ComponentsGroup::getId)
            .collect(Collectors.toList());
    }

    private List<String> buildComponents() {
        Component component0 = new TestComponentBuilder()
            .withName("component0")
            .withServiceId(serviceIds.get(0))
            .withComponentsGroupId(componentsGroupIds.get(0))
            .withRandomAbcSlug()
            .build();
        Component component1 = new TestComponentBuilder()
            .withName("component1")
            .withServiceId(serviceIds.get(0))
            .withComponentsGroupId(componentsGroupIds.get(1))
            .withRandomAbcSlug()
            .build();
        Component component2 = new TestComponentBuilder()
            .withName("component2")
            .withServiceId(serviceIds.get(0))
            .withComponentsGroupId(componentsGroupIds.get(1))
            .withRandomAbcSlug()
            .build();

        Component component3 = new TestComponentBuilder()
            .withName("component3")
            .withServiceId(serviceIds.get(1))
            .withComponentsGroupId(componentsGroupIds.get(2))
            .withRandomAbcSlug()
            .build();
        Component component4 = new TestComponentBuilder()
            .withName("component4")
            .withServiceId(serviceIds.get(1))
            .withComponentsGroupId(componentsGroupIds.get(3))
            .withRandomAbcSlug()
            .build();
        Component component5 = new TestComponentBuilder()
            .withName("component5")
            .withServiceId(serviceIds.get(1))
            .withRandomAbcSlug()
            .build();

        //Should be ignored while building role tree
        Component component6 = new TestComponentBuilder()
            .withComponentsGroupId(componentsGroupIds.get(1))
            .withRandomAbcSlug()
            .build();

        List<Component> components = Arrays.asList(component0, component1, component2,
            component3, component4, component5, component6);

        components.forEach(component -> componentsDao.save(component));

        return components.stream()
            .filter(component -> component.getName() != null)
            .map(Component::getId)
            .collect(Collectors.toList());
    }

    @Before
    public void prepare() {
        serviceIds = buildServices();
        componentsGroupIds = buildComponentGroups();
        componentIds = buildComponents();

        generalDao = new GeneralDao(servicesDao, componentsGroupsDao, componentsDao, null, null, null);
        registryRolesProvider = new RegistryRolesProvider(generalDao);
    }

    @Test
    public void checkTsumRolesTreeStructure() {
        List<TsumRole> roles = registryRolesProvider.getTsumRoles();

        checkSubRoles(getRoleByName(roles, "service0"),
            Arrays.asList(componentsGroupIds.get(0), componentsGroupIds.get(1), RegistryRole.SERVICE_ADMIN.getId()));

        checkSubRoles(getRoleByName(roles, "service1"),
            Arrays.asList(componentsGroupIds.get(2), componentIds.get(5), RegistryRole.SERVICE_ADMIN.getId()));

        checkSubRoles(getRoleByName(roles, "componentsGroup0"),
            Arrays.asList(componentIds.get(0), RegistryRole.COMPONENTS_GROUP_ADMIN.getId()));

        checkSubRoles(getRoleByName(roles, "componentsGroup1"),
            Arrays.asList(componentIds.get(1), componentIds.get(2), RegistryRole.COMPONENTS_GROUP_ADMIN.getId()));

        checkSubRoles(getRoleByName(roles, "componentsGroup2"),
            Arrays.asList(componentIds.get(3), componentsGroupIds.get(3), RegistryRole.COMPONENTS_GROUP_ADMIN.getId()));

        checkSubRoles(getRoleByName(roles, "componentsGroup3"),
            Arrays.asList(componentIds.get(4), RegistryRole.COMPONENTS_GROUP_ADMIN.getId()));

        for (int i = 0; i < componentIds.size(); i++) {
            checkSubRoles(getRoleByName(roles, "component" + i), singletonList(RegistryRole.COMPONENT_ADMIN.getId()));
        }
    }

    private TsumRole getRoleByName(List<TsumRole> roles, String requiredRoleName) {
        for (TsumRole role : roles) {
            if (role.getName().equals(requiredRoleName)) {
                return role;
            }
            TsumRole foundRole = getRoleByName(role.getSubRoles(), requiredRoleName);
            if (foundRole != null) {
                return foundRole;
            }
        }
        return null;
    }

    private List<String> getSubRoleIds(TsumRole role) {
        return role.getSubRoles().stream().map(TsumRole::getId).collect(Collectors.toList());
    }

    private void checkNotNullFields(TsumRole role) {
        Assert.assertNotNull(role.getName());
        Assert.assertNotNull(role.getDescription());
        Assert.assertNotNull(role.getId());
    }

    private void checkSubRoles(TsumRole role, List<String> subRoleIds) {
        checkNotNullFields(role);
        Assert.assertEquals(subRoleIds.size(), getSubRoleIds(role).size());
        Assert.assertTrue(getSubRoleIds(role).containsAll(subRoleIds));
    }

    @Test
    public void getServicePathTest() {
        for (String serviceId : serviceIds) {
            Assert.assertEquals(buildPath(serviceId), generalDao.getServicePath(serviceId));
        }
    }

    private String buildPath(String... registryObjects) {
        return String.join("/", registryObjects);
    }

    @Test
    public void getComponentsGroupPathTest() {
        Assert.assertEquals(
            buildPath(serviceIds.get(0), componentsGroupIds.get(0)),
            generalDao.getComponentGroupPath(componentsGroupIds.get(0)));

        Assert.assertEquals(
            buildPath(serviceIds.get(0), componentsGroupIds.get(1)),
            generalDao.getComponentGroupPath(componentsGroupIds.get(1)));

        Assert.assertEquals(
            buildPath(serviceIds.get(1), componentsGroupIds.get(2)),
            generalDao.getComponentGroupPath(componentsGroupIds.get(2)));

        Assert.assertEquals(
            buildPath(serviceIds.get(1), componentsGroupIds.get(2), componentsGroupIds.get(3)),
            generalDao.getComponentGroupPath(componentsGroupIds.get(3)));
    }

    @Test
    public void getComponentPathTest() {
        Assert.assertEquals(
            buildPath(serviceIds.get(0), componentsGroupIds.get(0), componentIds.get(0)),
            generalDao.getComponentPath(componentIds.get(0)));

        Assert.assertEquals(
            buildPath(serviceIds.get(0), componentsGroupIds.get(1), componentIds.get(1)),
            generalDao.getComponentPath(componentIds.get(1)));

        Assert.assertEquals(
            buildPath(serviceIds.get(0), componentsGroupIds.get(1), componentIds.get(2)),
            generalDao.getComponentPath(componentIds.get(2)));

        Assert.assertEquals(
            buildPath(serviceIds.get(1), componentsGroupIds.get(2), componentIds.get(3)),
            generalDao.getComponentPath(componentIds.get(3)));

        Assert.assertEquals(
            buildPath(serviceIds.get(1), componentsGroupIds.get(2), componentsGroupIds.get(3), componentIds.get(4)),
            generalDao.getComponentPath(componentIds.get(4)));

        Assert.assertEquals(
            buildPath( serviceIds.get(1), componentIds.get(5)),
            generalDao.getComponentPath(componentIds.get(5)));
    }
}
