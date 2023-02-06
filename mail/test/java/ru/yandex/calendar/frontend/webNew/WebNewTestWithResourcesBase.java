package ru.yandex.calendar.frontend.webNew;

import org.joda.time.Duration;
import org.joda.time.ReadableDuration;
import org.joda.time.ReadableInstant;
import org.junit.Before;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.calendar.logic.beans.generated.Event;
import ru.yandex.calendar.logic.beans.generated.Office;
import ru.yandex.calendar.logic.beans.generated.Resource;
import ru.yandex.calendar.logic.resource.ResourceRoutines;
import ru.yandex.misc.email.Email;

/**
 * @author dbrylev
 */
public abstract class WebNewTestWithResourcesBase extends WebNewTestBase {

    protected Office office;

    protected Resource resource1;
    protected Resource resource2;

    protected Resource nextOfficeResource;

    protected Email resource1Email;
    protected Email resource2Email;

    protected Email nextOfficeResourceEmail;

    @Before
    public void setup() {
        super.setup();

        office = testManager.createDefaultOffice();

        resource1 = testManager.cleanAndCreateResource("resource_1", "Resource 1", office);
        resource2 = testManager.cleanAndCreateResource("resource_2", "Resource 2", office);

        nextOfficeResource = testManager.cleanAndCreateResource("resource_21", "Another office resource");

        resource1Email = ResourceRoutines.getResourceEmail(resource1);
        resource2Email = ResourceRoutines.getResourceEmail(resource2);

        nextOfficeResourceEmail = ResourceRoutines.getResourceEmail(nextOfficeResource);

        testManager.updateNoSyncWithExchange(Cf.list(resource1, resource2, nextOfficeResource));
    }

    protected Event createResourceEvent(ReadableInstant start, Resource... resource) {
        return createResourceEvent(start, Duration.standardHours(1), resource);
    }

    protected Event createResourceEvent(ReadableInstant start, ReadableDuration duration, Resource... resource) {
        Event event = testManager.createDefaultEvent(uid, "Event", start.toInstant(), start.toInstant().plus(duration));

        Cf.x(resource).forEach(r -> testManager.addResourceParticipantToEvent(event.getId(), r));

        testManager.updateEventTimeIndents(event);

        return event;
    }
}
