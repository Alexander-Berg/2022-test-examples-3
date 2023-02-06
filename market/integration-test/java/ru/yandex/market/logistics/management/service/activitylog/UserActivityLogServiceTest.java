package ru.yandex.market.logistics.management.service.activitylog;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.management.AbstractContextualTest;
import ru.yandex.market.logistics.management.service.UserActivityLogService;
import ru.yandex.market.logistics.management.util.WithBlackBoxUser;

import static ru.yandex.market.logistics.management.service.plugin.LMSPlugin.AUTHORITY_ROLE_PARTNER_EDIT;

public class UserActivityLogServiceTest extends AbstractContextualTest {

    private static final String USER = "lmsUser";

    @Autowired
    private UserActivityLogService userActivityLogService;

    @Test
    @WithBlackBoxUser(login = USER, uid = 1, authorities = {AUTHORITY_ROLE_PARTNER_EDIT})
    @DatabaseSetup("/data/service/achievery/prepare_data.xml")
    void countEditSuccessRecordsTest() {
        softly.assertThat(userActivityLogService.getEditSuccessRequestCountByUserLogin(USER)).isEqualTo(4);
    }
}
