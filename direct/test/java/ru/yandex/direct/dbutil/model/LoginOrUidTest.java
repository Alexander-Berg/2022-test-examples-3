package ru.yandex.direct.dbutil.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SuppressWarnings("ResultOfMethodCallIgnored")
class LoginOrUidTest {
    @Test
    void testCreate_bothNull_error() {
        assertThrows(IllegalArgumentException.class,
                () -> LoginOrUid.of(null, null));
    }

    @Test
    void testCreate_bothNonNull_error() {
        assertThrows(IllegalArgumentException.class,
                () -> LoginOrUid.of("a", 0L));
    }

    @Test
    void testCreate_loginSet_noError() {
        LoginOrUid.of("a", null);
    }

    @Test
    void testCreate_uidSet_noError() {
        LoginOrUid.of(null, 0L);
    }

    @Test
    void testGet_loginSet() {
        LoginOrUid loginOrUid = LoginOrUid.of("a", null);
        assertEquals("a", loginOrUid.get());
    }

    @Test
    void testGet_uidSet() {
        LoginOrUid loginOrUid = LoginOrUid.of(null, 0L);
        assertEquals(0L, loginOrUid.get());
    }

    @Test
    void testGetLogin_isSet_returnValue() {
        assertEquals("a", LoginOrUid.of("a").getLogin());
    }

    @Test
    void testGetLogin_notSet_returnEmpty() {
        assertThrows(NullPointerException.class,
                () -> LoginOrUid.of(null, 0L).getLogin());
    }

    @Test
    void testGetUid_isSet_returnValue() {
        assertEquals(0L, (long) LoginOrUid.of(0L).getUid());
    }

    @Test
    void testGetUid_notSet_returnEmpty() {
        assertThrows(NullPointerException.class,
                () -> LoginOrUid.of("a", null).getUid());
    }

}
