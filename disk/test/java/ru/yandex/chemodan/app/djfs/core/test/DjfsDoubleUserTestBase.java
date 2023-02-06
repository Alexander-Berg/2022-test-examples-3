package ru.yandex.chemodan.app.djfs.core.test;

import org.junit.Before;

import ru.yandex.chemodan.app.djfs.core.filesystem.DjfsPrincipal;
import ru.yandex.chemodan.app.djfs.core.user.DjfsUid;
import ru.yandex.chemodan.app.djfs.core.user.UserData;

/**
 * @author eoshch
 */
public abstract class DjfsDoubleUserTestBase extends DjfsTestBase {
    protected static DjfsUid UID_1 = DjfsUid.cons(1337);
    protected static DjfsUid UID_2 = DjfsUid.cons(31337);

    protected static DjfsPrincipal PRINCIPAL_1 = DjfsPrincipal.cons(UID_1);
    protected static DjfsPrincipal PRINCIPAL_2 = DjfsPrincipal.cons(UID_2);

    protected UserData USER_1;
    protected UserData USER_2;

    @Override
    @Before
    public void setUp() {
        super.setUp();
        initializeUser(UID_1, 1);
        initializeUser(UID_2, 1);
        USER_1 = userDao.find(UID_1).get();
        USER_2 = userDao.find(UID_2).get();
        blackbox2.add(UID_1, "user1", "User 1.");
        blackbox2.add(UID_2, "user2", "User 2.");
    }
}
