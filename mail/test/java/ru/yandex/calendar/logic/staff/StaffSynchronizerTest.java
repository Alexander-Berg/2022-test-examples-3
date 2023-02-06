package ru.yandex.calendar.logic.staff;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.function.Function1B;
import ru.yandex.calendar.micro.yt.entity.YtUserWithDepartmentIds;
import ru.yandex.calendar.test.generic.AbstractConfTest;
import ru.yandex.mail.cerberus.yt.staff.dto.StaffUser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class StaffSynchronizerTest extends AbstractConfTest {

    @Autowired
    StaffSynchronizer staffSynchronizer;

    @Test
    public void testSynchronize() throws IOException {
        final int userDaoSize = staffSynchronizer.usersDao.getAll(100, 0).size();
        final int groupSize = staffSynchronizer.groupsDao.getAll(100, 0).size();
        final int officeSize = staffSynchronizer.officesDao.getAll(100, 0).size();
        final int roomsSize = staffSynchronizer.roomsDao.getAll(100, 0).size();
        staffSynchronizer.synchronize(new StaffV3("https://staff-api.yandex-team.ru", new TestStaffHttpProvider()));
        assertEquals(userDaoSize + 1, staffSynchronizer.usersDao.getAll(100, 0).size());
        assertEquals(groupSize + 2, staffSynchronizer.groupsDao.getAll(100, 0).size());
        assertEquals(officeSize + 2, staffSynchronizer.officesDao.getAll(100, 0).size());
        assertEquals(roomsSize + 1, staffSynchronizer.roomsDao.getAll(100, 0).size());

        final List<YtUserWithDepartmentIds> all = staffSynchronizer.usersDao.getAll(100, 0);
        Optional<YtUserWithDepartmentIds> user0Option = all.stream().filter(new Function1B<YtUserWithDepartmentIds>() {
            @Override
            public boolean apply(YtUserWithDepartmentIds ytUserWithDepartmentIds) {
                return ytUserWithDepartmentIds.getUser().getUid().getValue() == 1120000000022901L;
            }
        }).findFirst();
        assertTrue(user0Option.isPresent());
        YtUserWithDepartmentIds user0 = user0Option.get();
        assertEquals(1120000000022901L, user0.getUser().getUid().getValue());
        assertEquals("amosov-f", user0.getUser().getLogin());
        assertEquals("amosov-f@yandex-team.ru", user0.getUser().getInfo().getWorkEmail());
        assertEquals(StaffUser.Gender.MALE, user0.getUser().getInfo().getGender());
        assertEquals(81, user0.getDepartmentIds().size());
    }
}
