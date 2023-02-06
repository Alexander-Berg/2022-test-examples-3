package ru.yandex.calendar.logic.suggest;

import org.joda.time.Instant;

import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.MapF;
import ru.yandex.bolts.collection.Tuple2;
import ru.yandex.calendar.logic.beans.generated.Office;
import ru.yandex.calendar.logic.beans.generated.Resource;
import ru.yandex.calendar.logic.resource.ResourceInfo;
import ru.yandex.misc.test.Assert;
import ru.yandex.misc.time.InstantInterval;

/**
 * @author gutman
 */
public class SuggestTestHelper {

    static void assertSuggestEquals(int expectedStartMinutes, int expectedEndMinutes,
            MapF<Long, ListF<Long>> expectedOfficesAndResources, AvailableResourcesInOffices i)
    {
        Assert.equals(new InstantInterval(minutes(expectedStartMinutes), minutes(expectedEndMinutes)), i.getInterval());
        for (Tuple2<Long, ListF<Long>> officeAndResources : expectedOfficesAndResources.entries()) {
            OfficeAndFreeResources officeAndFreeResources = i.getOfficeAndFreeResources()
                    .find(OfficeAndFreeResources.getOfficeF().andThen(Office.getIdF().andThenEquals(officeAndResources._1))).get();

            Assert.equals(officeAndResources.get2(),
                    officeAndFreeResources.getAvailableResources().get1().map(ResourceInfo.resourceIdF()));
        }
    }

    private static Instant minutes(int i) {
        return new Instant(i * 60 * 1000);
    }

    public static TestOffice createTestOffice(
            long officeId, long resource1Id, long resource2Id, long resource3Id)
    {
        Office office = new Office();
        office.setId(officeId);
        office.setName("office " + officeId);

        Resource resource1 = new Resource();
        resource1.setId(resource1Id);
        resource1.setName("resource " + resource1Id);
        resource1.setDomain("test");
        resource1.setOfficeId(officeId);
        ResourceInfo ri1 = new ResourceInfo(resource1, office);

        Resource resource2 = new Resource();
        resource2.setId(resource2Id);
        resource2.setName("resource " + resource2Id);
        resource2.setDomain("test");
        resource2.setOfficeId(officeId);
        ResourceInfo ri2 = new ResourceInfo(resource2, office);

        Resource resource3 = new Resource();
        resource3.setId(resource3Id);
        resource3.setName("resource " + resource3Id);
        resource3.setDomain("test");
        resource3.setOfficeId(officeId);
        ResourceInfo ri3 = new ResourceInfo(resource3, office);

        return new TestOffice(office, ri1, ri2, ri3);
    }

}
