package ru.yandex.direct.rbac;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static ru.yandex.direct.rbac.RbacRole.AGENCY;
import static ru.yandex.direct.rbac.RbacRole.CLIENT;
import static ru.yandex.direct.rbac.RbacRole.SUPER;
import static ru.yandex.direct.rbac.RbacRole.SUPERREADER;

public class RbacRoleTest {
    @Test
    public void anyOf_positiveForOneRole() throws Exception {
        assertThat(CLIENT.anyOf(CLIENT), is(true));
    }

    @Test
    public void anyOf_negativeForOneRole() throws Exception {
        assertThat(CLIENT.anyOf(SUPER), is(false));
    }

    @Test
    public void anyOf_positiveForTwoRolesMatchFirst() throws Exception {
        assertThat(CLIENT.anyOf(CLIENT, AGENCY), is(true));
    }

    @Test
    public void anyOf_positiveForTwoRolesMatchSecond() throws Exception {
        assertThat(CLIENT.anyOf(SUPER, CLIENT), is(true));
    }

    @Test
    public void anyOf_negativeForTwoRoles() throws Exception {
        assertThat(SUPER.anyOf(CLIENT, SUPERREADER), is(false));
    }
}
