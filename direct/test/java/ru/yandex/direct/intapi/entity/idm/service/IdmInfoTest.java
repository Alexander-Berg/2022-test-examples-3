package ru.yandex.direct.intapi.entity.idm.service;

import java.util.Map;
import java.util.Set;

import one.util.streamex.EntryStream;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.client.repository.ClientRepository;
import ru.yandex.direct.core.entity.user.repository.UserRepository;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.intapi.configuration.IntApiTest;
import ru.yandex.direct.intapi.entity.idm.model.InfoResponse;
import ru.yandex.direct.intapi.entity.idm.model.InfoResponseRoleWithFields;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

@IntApiTest
@RunWith(SpringJUnit4ClassRunner.class)
public class IdmInfoTest {

    private IdmUserManagementIntapiService idmUserManagementIntapiService;

    @Before
    public void setUp() {
        IdmGetRolesService idmGetRolesService = new IdmGetRolesService(
                mock(ShardHelper.class),
                mock(ClientRepository.class),
                mock(UserRepository.class));

        idmUserManagementIntapiService = new IdmUserManagementIntapiService(
                idmGetRolesService,
                mock(IdmAddRoleService.class),
                mock(IdmRemoveRoleService.class),
                mock(IdmGroupRolesService.class),
                mock(IdmMainManagerService.class),
                mock(IdmSupportForClientService.class));
    }

    @Test
    public void infoTest() {
        InfoResponse response = idmUserManagementIntapiService.info();
        Map<String, Object> roles = response.getRoles().getValues();
        assertThat(roles.keySet(), containsInAnyOrder(
                "super", "superreader", "placer", "media", "support", "limited_support",
                "manager", "teamleader", "superteamleader", "internal_ad_admin", "internal_ad_manager",
                "internal_ad_superreader", "main_manager_for_client", "support_for_client",
                "manager_for_client", "developer"
        ));

        Set<String> exclusiveRoles = EntryStream.of(roles)
                .selectValues(InfoResponseRoleWithFields.class)
                .filterValues(r -> (r.getIsExclusive() != null) && r.getIsExclusive())
                .keys()
                .toSet();
        assertThat(exclusiveRoles, contains("main_manager_for_client"));
    }
}
