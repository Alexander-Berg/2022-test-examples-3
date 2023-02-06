package ru.yandex.direct.core.entity.idm.repository;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.idm.model.IdmGroup;
import ru.yandex.direct.core.entity.idm.model.IdmRequiredRole;
import ru.yandex.direct.core.testing.configuration.CoreTest;

import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThat;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class IdmGroupsRepositoryTest {

    @Autowired
    private IdmGroupsRepository idmGroupsRepository;

    @Test
    public void add_and_getGroupsRoles_success() {
        long idmGroupId = 20L;

        idmGroupsRepository.add(singleton(new IdmGroup()
                .withIdmGroupId(idmGroupId)
                .withRequiredRole(IdmRequiredRole.MANAGER)));

        IdmGroup expectedIdmGroup = new IdmGroup()
                .withIdmGroupId(idmGroupId)
                .withRequiredRole(IdmRequiredRole.MANAGER);

        IdmGroup group = idmGroupsRepository.getGroups(singleton(idmGroupId)).get(0);
        assertThat(group).isEqualTo(expectedIdmGroup);
    }

}
