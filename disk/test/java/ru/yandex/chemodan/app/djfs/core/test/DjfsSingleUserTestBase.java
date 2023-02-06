package ru.yandex.chemodan.app.djfs.core.test;

import org.junit.Before;

import ru.yandex.chemodan.app.djfs.core.filesystem.DjfsPrincipal;
import ru.yandex.chemodan.app.djfs.core.user.DjfsUid;
import ru.yandex.chemodan.app.djfs.core.user.UserData;

/**
 * @author eoshch
 */
public abstract class DjfsSingleUserTestBase extends DjfsTestBase {
    protected static DjfsUid UID = DjfsUid.cons(31337);
    protected static DjfsPrincipal PRINCIPAL = DjfsPrincipal.cons(UID);
    protected UserData USER;

    @Override
    @Before
    public void setUp() {
        super.setUp();
        initializeUser(UID, 1);
        USER = userDao.find(UID).get();
    }
}
