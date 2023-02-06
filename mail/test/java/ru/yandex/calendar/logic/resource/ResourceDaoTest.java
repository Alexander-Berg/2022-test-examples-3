package ru.yandex.calendar.logic.resource;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.Tuple2List;
import ru.yandex.calendar.logic.beans.generated.Office;
import ru.yandex.calendar.test.generic.AbstractConfTest;
import ru.yandex.inside.passport.blackbox.PassportDomain;
import ru.yandex.misc.test.Assert;

/**
 * @author Stepan Koltsov
 */
public class ResourceDaoTest extends AbstractConfTest {

    @Autowired
    private ResourceDao resourceDao;
    @Autowired
    private ResourceTestManager resourceTestManager;

    @Test
    public void findOfficesWithActiveResourceCount() {
        resourceTestManager.clearResourcesAndOffices(PassportDomain.YANDEX_TEAM_RU);
        int officeCount = resourceTestManager.saveSomeOffices(PassportDomain.YANDEX_TEAM_RU).length();
        Tuple2List<Office, Integer> officesAndCounts = resourceDao.findOfficesWithActiveResourceCount(PassportDomain.YANDEX_TEAM_RU);
        Assert.A.hasSize(officeCount, officesAndCounts);
        Assert.A.equals(Cf.set(0), officesAndCounts.get2().unique());
    }

} //~
