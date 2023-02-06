package ru.yandex.market.tsum.registry.v2.controllers;

import java.util.Collections;
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
import ru.yandex.market.tsum.registry.v2.RegistryRole;
import ru.yandex.market.tsum.registry.v2.RegistryRolesProvider;
import ru.yandex.market.tsum.registry.v2.TestComponentsGroupBuilder;
import ru.yandex.market.tsum.registry.v2.TestServiceBuilder;
import ru.yandex.market.tsum.registry.v2.dao.*;
import ru.yandex.market.tsum.registry.v2.dao.model.ComponentsGroup;
import ru.yandex.market.tsum.registry.v2.dao.model.Service;

/**
 * @author David Burnazyan <a href="mailto:dburnazyan@yandex-team.ru"></a>
 * @date 25/07/2018
 */
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@ContextConfiguration(classes = {TestMongo.class, ServicesDao.class, ComponentsGroupsDao.class,
    ComponentsDao.class, DutiesDao.class, InstallationsDao.class, ResourceDao.class, GeneralDao.class,
    RegistryRolesProvider.class, ComponentUpdateRequestDao.class, ComponentSpecsDao.class})
public class ServiceControllerTest {

    private static final String OLD_DESCRIPTION = "OLD_DESCRIPTION";
    private static final String NEW_DESCRIPTION = "NEW_DESCRIPTION";

    @Autowired
    private ServicesDao servicesDao;
    @Autowired
    private ComponentsGroupsDao componentsGroupsDao;
    @Autowired
    private ComponentsDao componentsDao;
    @Autowired
    private RegistryRolesProvider registryRolesProvider;

    private Service serviceWithOwner;

    private ComponentsGroup componentsGroup;

    @Before
    public void prepare() {
        serviceWithOwner = new TestServiceBuilder()
            .withRandomName()
            .withRandomOwner()
            .withRandomAbcSlug()
            .withDescription(OLD_DESCRIPTION)
            .build();
        servicesDao.save(serviceWithOwner);

        componentsGroup = new TestComponentsGroupBuilder()
            .withRandomName()
            .withServiceId(serviceWithOwner.getId())
            .build();
        componentsGroupsDao.save(componentsGroup);
    }

    @Test
    public void testSave_userHasTsumAdminRole_ok() {
        TsumUser user = new TsumUser("someOtherLogin", Collections.singleton(TsumUser.ADMIN_ROLE));
        checkResponseFromSaveMethod(user, HttpStatus.OK, true);
    }

    @Test
    public void testSave_userHasRegistryAdminRole_ok() {
        TsumUser user = new TsumUser("someOtherLogin", Collections.singleton(
            registryRolesProvider.getRootRole(RegistryRole.ADMIN)));
        checkResponseFromSaveMethod(user, HttpStatus.OK, true);
    }

    @Test
    public void testSave_userIsServiceOwner_ok() {
        TsumUser user = new TsumUser(serviceWithOwner.getOwner(), Collections.emptySet());
        checkResponseFromSaveMethod(user, HttpStatus.OK, true);
    }

    @Test
    public void testSave_userHasServiceAdminRole_ok() {
        TsumUser user = new TsumUser("userWithServiceAdminRole", Collections.singleton(
                registryRolesProvider.getServiceRole(serviceWithOwner.getId(), RegistryRole.SERVICE_ADMIN)));
        checkResponseFromSaveMethod(user, HttpStatus.OK, true);
    }

    private void checkResponseFromSaveMethod(TsumUser user, HttpStatus expectedStatus, boolean isUpdated) {
        ResponseEntity<?> responseEntity = tryUpdateServiceDescriptionWithUser(user);

        Service savedService = servicesDao.get(serviceWithOwner.getId());

        Assert.assertEquals(responseEntity.getStatusCode(), expectedStatus);
        Assert.assertEquals(savedService.getDescription(), isUpdated ? NEW_DESCRIPTION : OLD_DESCRIPTION);
    }

    private ResponseEntity<?> tryUpdateServiceDescriptionWithUser(TsumUser user) {
        setSecurityContextWithUser(user);
        serviceWithOwner.setDescription(NEW_DESCRIPTION);

        ServiceController serviceController = new ServiceController(servicesDao, componentsGroupsDao,
            componentsDao, registryRolesProvider);
        return serviceController.saveService(serviceWithOwner, false);
    }

    private static void setSecurityContextWithUser(TsumUser user) {
        SecurityContext securityContext = Mockito.mock(SecurityContext.class, Mockito.RETURNS_DEEP_STUBS);
        Mockito.when(securityContext.getAuthentication().getDetails()).thenReturn(user);
        SecurityContextHolder.setContext(securityContext);
    }
}
