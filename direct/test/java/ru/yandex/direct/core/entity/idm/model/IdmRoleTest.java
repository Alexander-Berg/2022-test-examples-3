package ru.yandex.direct.core.entity.idm.model;

import java.util.Set;

import one.util.streamex.StreamEx;
import org.junit.Test;

import ru.yandex.direct.rbac.RbacRole;

import static org.assertj.core.api.Assertions.assertThat;

public class IdmRoleTest {

    @Test
    public void checkNames() {
        Set<String> rbacRoleNames = StreamEx.of(RbacRole.values())
                .map(Enum::name)
                .toSet();
        Set<String> idmRoleNames = StreamEx.of(IdmRequiredRole.values())
                .map(Enum::name)
                .toSet();
        assertThat(rbacRoleNames)
                .as("IdmRole must contain only subset of RbacRole")
                .containsAll(idmRoleNames);
    }

}
