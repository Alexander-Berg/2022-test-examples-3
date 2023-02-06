package ru.yandex.market.antifraud.orders.auth;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.inside.passport.blackbox2.Blackbox2;
import ru.yandex.market.antifraud.orders.storage.dao.IdmRoleDao;
import ru.yandex.market.antifraud.orders.storage.entity.auth.IdmRole;
import ru.yandex.market.antifraud.orders.storage.entity.auth.IdmUser;
import ru.yandex.market.antifraud.orders.web.dto.idm.IdmInfoDto;
import ru.yandex.market.antifraud.orders.web.dto.idm.IdmNodeDto;
import ru.yandex.market.antifraud.orders.web.dto.idm.IdmRoleGroupDto;
import ru.yandex.market.antifraud.orders.web.dto.idm.IdmRolesDto;

import static org.mockito.Mockito.when;

/**
 * @author dzvyagin
 */
@RunWith(MockitoJUnitRunner.class)
public class AuthServiceTest {

    @Mock
    private Blackbox2 blackbox2;

    @Mock
    private IdmRoleDao idmRoleDao;

    @Test
    public void getIdmInfo() {
        AuthService authService = new AuthService(blackbox2, "scope", idmRoleDao);
        IdmRole role1 = IdmRole.builder().slug("role1").enname("role-1").runame("role-1-ru").build();
        IdmRole role2 = IdmRole.builder().slug("role2").enname("role-2").runame("role-2-ru").build();
        when(idmRoleDao.getAllRoles()).thenReturn(List.of(role1,role2));
        IdmInfoDto infoDto = authService.getIdmInfo();
        System.out.println(infoDto);
        assertThat(infoDto.getCode()).isEqualTo(0);
        assertThat(infoDto.getRoles()).hasSize(1);
        IdmRoleGroupDto roleGroupDto = infoDto.getRoles().get(0);
        assertThat(roleGroupDto.getSlug()).isEqualTo("antifraud-orders");
        IdmNodeDto role1Node = roleGroupDto.getValues().get("role1");
        assertThat(role1Node.getSlug()).isEqualTo(role1.getSlug());
        assertThat(role1Node.getName().getEn()).isEqualTo(role1.getEnname());
        assertThat(role1Node.getName().getRu()).isEqualTo(role1.getRuname());
        IdmNodeDto role2Node = roleGroupDto.getValues().get("role2");
        assertThat(role2Node.getSlug()).isEqualTo(role2.getSlug());
        assertThat(role2Node.getName().getEn()).isEqualTo(role2.getEnname());
        assertThat(role2Node.getName().getRu()).isEqualTo(role2.getRuname());
    }

    @Test
    public void getAllRoles(){
        AuthService authService = new AuthService(blackbox2, "scope", idmRoleDao);
        when(idmRoleDao.getAllUsers()).thenReturn(List.of(
                new IdmUser("login1", List.of(IdmRole.builder().slug("role1").build())),
                new IdmUser("login2", List.of(IdmRole.builder().slug("role2").build()))
        ));
        IdmRolesDto rolesDto = authService.getAllRoles();
        assertThat(rolesDto.getCode()).isEqualTo(0);
        assertThat(rolesDto.getUsers()).hasSize(2);
        assertThat(rolesDto.getUsers().get(0).getLogin()).isEqualTo("login1");
        assertThat(rolesDto.getUsers().get(0).getRoles().stream().findFirst().get().getRole())
                .isEqualTo("antifraud-orders/role1");
        assertThat(rolesDto.getUsers().get(1).getLogin()).isEqualTo("login2");
        assertThat(rolesDto.getUsers().get(1).getRoles().stream().findFirst().get().getRole())
                .isEqualTo("antifraud-orders/role2");
        System.out.println(rolesDto);
    }
}
