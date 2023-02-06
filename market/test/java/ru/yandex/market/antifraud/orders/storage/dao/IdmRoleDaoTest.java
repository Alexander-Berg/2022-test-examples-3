package ru.yandex.market.antifraud.orders.storage.dao;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.market.antifraud.orders.storage.entity.auth.IdmRole;
import ru.yandex.market.antifraud.orders.storage.entity.auth.IdmUser;
import ru.yandex.market.antifraud.orders.test.annotations.DaoLayerTest;

/**
 * @author dzvyagin
 */
@DaoLayerTest
@RunWith(SpringJUnit4ClassRunner.class)
public class IdmRoleDaoTest {

    @Autowired
    private NamedParameterJdbcOperations jdbcTemplate;

    private IdmRoleDao idmRoleDao;

    @Before
    public void init() {
        idmRoleDao = new IdmRoleDao(jdbcTemplate);
    }

    @Test
    public void getAllUsers(){
        IdmRole role = IdmRole.builder()
                .slug("role_getAllUsers")
                .enname("role_getAllUsers")
                .runame("role_getAllUsers")
                .build();
        role = idmRoleDao.saveRole(role);

        IdmUser user1 = idmRoleDao.saveUser(new IdmUser("getAllUsers_1", List.of(role)));
        IdmUser user2 = idmRoleDao.saveUser(new IdmUser("getAllUsers_2", List.of(role)));

        List<IdmUser> allUsers = idmRoleDao.getAllUsers();
        assertThat(allUsers).contains(user1, user2);
    }


    @Test
    public void addRole(){
        IdmRole role = IdmRole.builder()
                .slug("role_addRole")
                .enname("role_addRole")
                .runame("role_addRole")
                .build();
        role = idmRoleDao.saveRole(role);

        IdmUser user = new IdmUser("addRole_1", List.of());
        idmRoleDao.saveUser(user);
        idmRoleDao.addRole("addRole_1", role);

        user = idmRoleDao.findByLogin("addRole_1").get();
        assertThat(user.getRoles()).contains(role);
    }

    @Test
    public void removeRole(){
        IdmRole role = IdmRole.builder()
                .slug("role_removeRole")
                .enname("role_removeRole")
                .runame("role_removeRole")
                .build();
        role = idmRoleDao.saveRole(role);

        IdmUser user = new IdmUser("removeRole_1", List.of());
        idmRoleDao.saveUser(user);
        idmRoleDao.addRole("removeRole_1", role);

        user = idmRoleDao.findByLogin("removeRole_1").get();
        assertThat(user.getRoles()).contains(role);
        idmRoleDao.removeRole("removeRole_1", role);
        user = idmRoleDao.findByLogin("removeRole_1").get();
        assertThat(user.getRoles()).isEmpty();
    }

    @Test
    public void getAllRoles(){
        IdmRole role1 = IdmRole.builder()
                .slug("role_getAllRoles1")
                .enname("role_getAllRoles")
                .runame("role_getAllRoles")
                .build();
        role1 = idmRoleDao.saveRole(role1);
        IdmRole role2 = IdmRole.builder()
                .slug("role_getAllRoles2")
                .enname("role_getAllRoles")
                .runame("role_getAllRoles")
                .build();
        role2 = idmRoleDao.saveRole(role2);

        List<IdmRole> allRoles = idmRoleDao.getAllRoles();
        assertThat(allRoles).contains(role1, role2);
    }

}
