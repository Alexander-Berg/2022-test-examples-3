package ru.yandex.calendar.logic.ics.imp;


import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.calendar.logic.user.UserManager;
import ru.yandex.calendar.test.auto.db.util.TestManager;
import ru.yandex.inside.passport.PassportUid;
import ru.yandex.inside.passport.login.PassportLogin;
import ru.yandex.misc.test.Assert;

/**
 * @author Stepan Koltsov
 */
public class IcsImporterFromFileUpdateRdateTest extends IcsImporterFromFileTestBase {

    @Autowired
    private UserManager userManager;
    @Autowired
    private TestManager testManager;

    private void checkRdatesCount(long count) {
        String sql = "SELECT COUNT(id) FROM rdate WHERE is_rdate = FALSE";
        int actualCount = jdbcTemplate.queryForInt(sql);
        Assert.assertEquals(count, actualCount);
    }

    @Test
    public void exdateUpdate() throws Exception {
        PassportLogin login = new PassportLogin("yandex-team-mm-10801");
        PassportUid uid = userManager.getUidByLoginForTest(login);
        testManager.cleanUser(uid);

        int startCount = jdbcTemplate.queryForInt("SELECT COUNT(*) FROM rdate WHERE is_rdate = FALSE");
        importIcsByUid("update_rdate/exdate.ics", uid);
        checkRdatesCount(startCount + 0);
        importIcsByUid("update_rdate/exdate_upd1.ics", uid);
        checkRdatesCount(startCount + 1);
        importIcsByUid("update_rdate/exdate_upd2.ics", uid);
        checkRdatesCount(startCount + 2);
    }

}
