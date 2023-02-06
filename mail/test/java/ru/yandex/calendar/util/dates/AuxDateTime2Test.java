package ru.yandex.calendar.util.dates;

import ru.yandex.misc.test.Assert;
import org.junit.Ignore;
import org.junit.Test;


/**
 * @author ssytnik
 */
public class AuxDateTime2Test {
    private static final String[] TZ_IDS = {
        "GMT+01:00", "GMT-02:30", "GMT+5:00", "GMT+04", "GMT+7",
        "UTC", "GMT", "Europe/Moscow", "Asia/Yekaterinburg"
    }, MYSQL_TZ_IDS = {
        "+01:00", "-02:30", "+5:00", "+04:00", "+7:00",
        "UTC", "GMT", "Europe/Moscow", "Asia/Yekaterinburg"
    };

    @Test
    public void getVerifyTimezone() throws Exception {
        for (String tzId : TZ_IDS) { AuxDateTime.getVerifyDateTimeZone(tzId); }
    }

    @Test
    public void tzId2mysqlTzId() throws Exception {
        for (int i = 0; i < TZ_IDS.length; ++i) {
            String convertedTzId = AuxDateTime.tzId2mysqlTzId(TZ_IDS[i]);
            Assert.A.equals(MYSQL_TZ_IDS[i], convertedTzId);
        }
    }

    @Ignore
    @Test
    public void mysqlTzIdValidity() throws Exception {
//        String sql = "SELECT CONVERT_TZ(UTC_TIMESTAMP, 'UTC', ?)";
//        for (String mysqlTzId : MYSQL_TZ_IDS) {
//            Timestamp ts = DbUtils.getSingleTimestamp(ctx.getDbConf(), sql, mysqlTzId);
//            AssertF.assertNotNull(ts);
//        }
//        DbUtils.verifyMysqlTzIds(jdbcTemplate, MYSQL_TZ_IDS);
    }
}
