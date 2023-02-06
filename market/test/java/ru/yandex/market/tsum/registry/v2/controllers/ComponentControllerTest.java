package ru.yandex.market.tsum.registry.v2.controllers;

import java.util.Collections;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import ru.yandex.market.tsum.core.TestMongo;
import ru.yandex.market.tsum.core.auth.TsumUser;
import ru.yandex.market.tsum.registry.v2.*;
import ru.yandex.market.tsum.registry.v2.dao.*;
import ru.yandex.market.tsum.registry.v2.dao.model.Component;
import ru.yandex.market.tsum.registry.v2.dao.model.ComponentsGroup;
import ru.yandex.market.tsum.registry.v2.dao.model.Service;

/**
 * @author David Burnazyan <a href="mailto:dburnazyan@yandex-team.ru"></a>
 * @date 01/08/2018
 */
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@ContextConfiguration(classes = {TestMongo.class, ServicesDao.class, ComponentsGroupsDao.class,
    ComponentsDao.class, DutiesDao.class, InstallationsDao.class, ResourceDao.class, GeneralDao.class,
    RegistryRolesProvider.class, ComponentUpdateRequestDao.class, ComponentSpecsDao.class})
public class ComponentControllerTest {
    private static final Gson GSON = new GsonBuilder().create();

    private String OLD_NAME = "OLD_NAME";
    private String NEW_NAME = "NEW_NAME";

    @Autowired
    private ServicesDao servicesDao;
    @Autowired
    private ComponentsGroupsDao componentsGroupsDao;
    @Autowired
    private ComponentsDao componentsDao;
    @Autowired
    private RegistryRolesProvider registryRolesProvider;


    private ComponentController componentController;
    private Service serviceWithOwner;
    private Service otherServiceWithOwner;
    private ComponentsGroup componentsGroup;
    private ComponentsGroup otherComponentsGroup;
    private Component component;

    @Before
    public void prepare() {

        serviceWithOwner = new TestServiceBuilder()
            .withRandomOwner()
            .withRandomName()
            .withRandomAbcSlug()
            .build();
        servicesDao.save(serviceWithOwner);
        otherServiceWithOwner = new TestServiceBuilder()
            .withRandomName()
            .withRandomOwner()
            .withRandomAbcSlug()
            .build();
        servicesDao.save(otherServiceWithOwner);

        componentsGroup = new TestComponentsGroupBuilder()
            .withRandomName()
            .withServiceId(serviceWithOwner.getId())
            .build();
        componentsGroupsDao.save(componentsGroup);
        otherComponentsGroup = new TestComponentsGroupBuilder()
            .withRandomName()
            .withServiceId(serviceWithOwner.getId())
            .build();
        componentsGroupsDao.save(otherComponentsGroup);

        component = new TestComponentBuilder()
            .withName(OLD_NAME)
            .withServiceId(serviceWithOwner.getId())
            .withRandomAbcSlug()
            .withComponentsGroupId(componentsGroup.getId())
            .build();
        componentsDao.save(component);

        componentController = new ComponentController(
            componentsDao, null, servicesDao, componentsGroupsDao, registryRolesProvider);
    }

    @Test
    public void testSave__userHasTsumAdminRole__ok() {
        TsumUser user = new TsumUser("userWithTsumAdminRole", Collections.singleton(TsumUser.ADMIN_ROLE));
        checkResponseFromSaveMethod(user, HttpStatus.OK, true);
    }

    @Test
    public void testSave__userHasRegistryAdminRole__ok() {
        TsumUser user = new TsumUser("userWithRegistryAdminRole", Collections.singleton(
            registryRolesProvider.getRootRole(RegistryRole.ADMIN)));
        checkResponseFromSaveMethod(user, HttpStatus.OK, true);
    }

    @Test
    public void testSave__userIsServiceOwner__ok() {
        TsumUser user = new TsumUser(serviceWithOwner.getOwner(), Collections.emptySet());
        checkResponseFromSaveMethod(user, HttpStatus.OK, true);
    }

    @Test
    public void testSave__userIsServiceAdmin__ok() {
        TsumUser user = new TsumUser("userWithServiceAdminRole", Collections.singleton(
            registryRolesProvider.getServiceRole(serviceWithOwner.getId(), RegistryRole.SERVICE_ADMIN)));
        checkResponseFromSaveMethod(user, HttpStatus.OK, true);
    }

    @Test
    public void testSave__userIsComponentGroupAdmin__ok() {
        TsumUser user = new TsumUser("userWithComponentGroupAdminRole", Collections.singleton(
            registryRolesProvider.getComponentGroupRole(componentsGroup.getId(), RegistryRole.COMPONENTS_GROUP_ADMIN)));
        checkResponseFromSaveMethod(user, HttpStatus.OK, true);
    }

    @Test
    public void testSave__userIsComponentAdmin__ok() {
        TsumUser user = new TsumUser("userWithComponentAdminRole", Collections.singleton(
            registryRolesProvider.getComponentRole(component.getId(), RegistryRole.COMPONENT_ADMIN)));
        checkResponseFromSaveMethod(user, HttpStatus.OK, true);
    }

    @Test
    public void testSave__userIsOtherServiceAdmin__httpStatusForbidden() {
        TsumUser user = new TsumUser("userWithOtherServiceAdminRole", Collections.singleton(
            registryRolesProvider.getServiceRole(otherServiceWithOwner.getId(), RegistryRole.SERVICE_ADMIN)));
        checkResponseFromSaveMethod(user, HttpStatus.FORBIDDEN, false);
    }

    @Test
    public void testSave__userIsOtherComponentGroupAdmin__httpStatusForbidden() {
        TsumUser user = new TsumUser("userWithOtherComponentGroupAdminRole", Collections.singleton(
            registryRolesProvider.getComponentGroupRole(otherComponentsGroup.getId(), RegistryRole.COMPONENTS_GROUP_ADMIN)));
        checkResponseFromSaveMethod(user, HttpStatus.FORBIDDEN, false);
    }

    @Test
    public void testSave__userHasNoAccessRights__httpStatusForbidden() {
        TsumUser user = new TsumUser("userWithoutAccess", Collections.emptySet());
        checkResponseFromSaveMethod(user, HttpStatus.FORBIDDEN, false);
    }

    private void checkResponseFromSaveMethod(TsumUser user, HttpStatus expectedStatus, boolean isUpdated) {
        ResponseEntity<?> responseEntity = tryUpdateComponentNameWithUser(user);

        Component savedComponent = componentsDao.get(component.getId());

        Assert.assertEquals(responseEntity.getStatusCode(), expectedStatus);
        Assert.assertEquals(savedComponent.getName(), isUpdated ? NEW_NAME : OLD_NAME);
    }

    private ResponseEntity<?> tryUpdateComponentNameWithUser(TsumUser user) {
        setSecurityContextWithUser(user);
        component.setName(NEW_NAME);

        return componentController.saveComponent(GSON.toJson(component, Component.class), false);
    }

    private static void setSecurityContextWithUser(TsumUser user) {
        SecurityContext securityContext = Mockito.mock(SecurityContext.class, Mockito.RETURNS_DEEP_STUBS);
        Mockito.when(securityContext.getAuthentication().getDetails()).thenReturn(user);
        SecurityContextHolder.setContext(securityContext);
    }
}
