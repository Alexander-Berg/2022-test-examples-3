package ru.yandex.chemodan.app.djfs.core.test;

import org.junit.Before;

import ru.yandex.chemodan.app.djfs.core.filesystem.DjfsPrincipal;
import ru.yandex.chemodan.app.djfs.core.user.DjfsUid;


public abstract class DjfsSinglePostgresUserTestBase extends DjfsTestBase {
    final protected static DjfsUid UID = DjfsUid.cons(31337), UID_BABE = DjfsUid.cons(0xBABE);
    final protected static DjfsPrincipal PRINCIPAL = DjfsPrincipal.cons(UID);

    @Override
    @Before
    public void setUp() {
        super.setUp();
        initializeUser(UID, 1);
        initializeUser(UID_BABE, 1);
    }

    public void makeQuickMoveUser(DjfsUid uid) {
        userDao.setQuickMoveFlag(uid);
    }
}
