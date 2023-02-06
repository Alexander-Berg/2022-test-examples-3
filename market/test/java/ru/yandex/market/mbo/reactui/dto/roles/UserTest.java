package ru.yandex.market.mbo.reactui.dto.roles;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class UserTest {

    @Test
    public void isNotManager() {
        final User user = new User("", 1L, "Trololo", "", "");
        assertFalse(user.isManager());
    }

    @Test
    public void isManager() {
        final User user = new User("", 1L, "AutoW_6_M_VASYA", "", "");
        assertTrue(user.isManager());
    }
}
