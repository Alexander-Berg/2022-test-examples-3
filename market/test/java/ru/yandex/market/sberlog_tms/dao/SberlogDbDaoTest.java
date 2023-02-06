package ru.yandex.market.sberlog_tms.dao;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import ru.yandex.market.sberlog_tms.SberlogtmsConfig;

/**
 * @author Strakhov Artem <a href="mailto:dukeartem@yandex-team.ru"></a>
 * @date 29.10.19
 */
@Disabled
@SpringJUnitConfig(SberlogtmsConfig.class)
public class SberlogDbDaoTest {

    @Autowired
    private SberlogDbDao sberlogDbDao;

    @Test
    @DisplayName("can we decrypt and get user")
    void getAllUsersInfo() {
        Assertions.assertTrue(sberlogDbDao.getAllUsersInfo().size() > 0);
    }

    @Test
    void getUnlinkedUser() {
        sberlogDbDao.setUserStatusIsUnLink("2190550858753009288", "12313125");
        sberlogDbDao.setUserStatusIsUnLink("2190550858753009289", "12313126");

        Assertions.assertNotNull(sberlogDbDao.getUnlinkedUser(0));
        Assertions.assertNotNull(sberlogDbDao.getUnlinkedUser(1));
        Assertions.assertNull(sberlogDbDao.getUnlinkedUser(2));
    }

    @Test
    void setUserStatusIsLink() {
        sberlogDbDao.setUserStatusIsUnLink("2190550858753009288", "12313125");
        sberlogDbDao.setUserStatusIsUnLink("2190550858753009289", "12313126");

        Assertions.assertNotNull(sberlogDbDao.getUnlinkedUser(0));
        Assertions.assertNotNull(sberlogDbDao.getUnlinkedUser(1));

        sberlogDbDao.setUserStatusIsLink("2190550858753009288", "12313125");
        sberlogDbDao.setUserStatusIsLink("2190550858753009289", "12313126");

        Assertions.assertNull(sberlogDbDao.getUnlinkedUser(0));
    }

    @Test
    void setUserStatusIsUnLink() {
        sberlogDbDao.setUserStatusIsLink("2190550858753009288", "12313125");
        sberlogDbDao.setUserStatusIsLink("2190550858753009289", "12313126");

        Assertions.assertNull(sberlogDbDao.getUnlinkedUser(0));
        Assertions.assertNull(sberlogDbDao.getUnlinkedUser(1));

        sberlogDbDao.setUserStatusIsUnLink("2190550858753009288", "12313125");
        sberlogDbDao.setUserStatusIsUnLink("2190550858753009289", "12313126");

        Assertions.assertNotNull(sberlogDbDao.getUnlinkedUser(0));
        Assertions.assertNotNull(sberlogDbDao.getUnlinkedUser(1));
    }
}
