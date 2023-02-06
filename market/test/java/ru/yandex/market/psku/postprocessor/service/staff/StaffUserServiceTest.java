package ru.yandex.market.psku.postprocessor.service.staff;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import ru.yandex.market.psku.postprocessor.common.BaseDBTest;
import ru.yandex.market.psku.postprocessor.common.db.dao.StaffUserDao;
import ru.yandex.market.psku.postprocessor.common.db.jooq.tables.pojos.StaffUser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class StaffUserServiceTest extends BaseDBTest {

    @Autowired
    private StaffUserDao staffUserDao;


    @Test
    public void test() {
        StaffUser user1 = new StaffUser();
        user1.setId(1L);
        user1.setLogin("user1");
        user1.setUid("1");
        StaffUser user2 = new StaffUser();
        user2.setId(2L);
        user2.setLogin("user2");
        user2.setUid("2");
        staffUserDao.insert(user1);

        StaffClient staffClient = Mockito.mock(StaffClient.class);
        List<StaffUser> users = new ArrayList<>();
        users.add(user2);
        Mockito.when(staffClient.getUsers(Collections.singletonList("user2")))
                .thenReturn(users);
        StaffUserService staffUserService = new StaffUserService(staffClient, staffUserDao);

        List<StaffUser> result = staffUserService.getUsers(Arrays.asList("user1", "user2"));
        Assertions.assertThat(result.size()).isEqualTo(2);
        StaffUser resultUser1 = result.get(0);
        StaffUser resultUser2 = result.get(1);

        Assertions.assertThat(resultUser1.getId()).isEqualTo(user1.getId());
        Assertions.assertThat(resultUser1.getLogin()).isEqualTo(user1.getLogin());
        Assertions.assertThat(resultUser1.getUid()).isEqualTo(user1.getUid());
        Assertions.assertThat(resultUser2.getId()).isEqualTo(user2.getId());
        Assertions.assertThat(resultUser2.getLogin()).isEqualTo(user2.getLogin());
        Assertions.assertThat(resultUser2.getUid()).isEqualTo(user2.getUid());

        Assertions.assertThat(staffUserDao.count()).isEqualTo(2);
    }
}
