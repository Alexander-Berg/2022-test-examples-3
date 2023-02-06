package ru.yandex.calendar.frontend.web.cmd.run.api;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.calendar.logic.beans.generated.Office;
import ru.yandex.calendar.logic.beans.generated.Resource;
import ru.yandex.calendar.logic.resource.ResourceDao;
import ru.yandex.calendar.logic.resource.ResourceInfo;
import ru.yandex.calendar.test.auto.db.util.TestManager;
import ru.yandex.calendar.test.auto.db.util.TestUserInfo;
import ru.yandex.calendar.test.generic.AbstractConfTest;
import ru.yandex.misc.test.Assert;

/**
 * @author gutman
 */
public class CmdGetEventsForStaffTest extends AbstractConfTest {

    @Autowired
    private TestManager testManager;
    @Autowired
    private ResourceDao resourceDao;

    @Test
    public void resourceOrdering() {
        TestUserInfo user = testManager.prepareUser("yandex-team-mm-12101");

        Office usersOffice = testManager.createDefaultOffice();
        Office anotherOffice = testManager.createDefaultOffice();
        Office sameCityOffice = testManager.createDefaultOffice();

        usersOffice.setCityName(Option.of("usersCity"));
        anotherOffice.setCityName(Option.of("otherCity"));
        sameCityOffice.setCityName(Option.of("usersCity"));
        resourceDao.updateOffices(Cf.list(usersOffice, anotherOffice, sameCityOffice));

        Resource inUsersOffice1 = testManager.createResource("inUsersOffice1", "inUsersOffice1", usersOffice);
        Resource inUsersOffice2 = testManager.createResource("inUsersOffice2", "inUsersOffice2", usersOffice);
        Resource notInUsersOffice1 = testManager.createResource("notInUsersOffice1", "notInUsersOffice1", anotherOffice);
        Resource notInUsersOffice2 = testManager.createResource("notInUsersOffice2", "notInUsersOffice2", anotherOffice);
        Resource inUsersCity = testManager.createResource("inUsersCity", "inUsersCity", sameCityOffice);

        ListF<String> resourcesForStaff = Cf.list(
                resourceDao.findResourceInfoByResourceId(inUsersOffice1.getId()),
                resourceDao.findResourceInfoByResourceId(inUsersOffice2.getId()),
                resourceDao.findResourceInfoByResourceId(notInUsersOffice1.getId()),
                resourceDao.findResourceInfoByResourceId(notInUsersOffice2.getId()),
                resourceDao.findResourceInfoByResourceId(inUsersCity.getId())
        )
                .sorted(new ResourcesForStaffComparator(usersOffice))
                .map(ResourceInfo.nameF());

        Assert.equals(Cf.set("inUsersOffice1", "inUsersOffice2"), resourcesForStaff.take(2).unique());
        Assert.equals("inUsersCity", resourcesForStaff.get(2));
        Assert.equals(Cf.set("notInUsersOffice1", "notInUsersOffice2"), resourcesForStaff.drop(3).unique());
    }

}
