package ru.yandex.calendar.logic.event;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.calendar.logic.beans.GenericBeanDao;
import ru.yandex.calendar.logic.beans.generated.Event;
import ru.yandex.calendar.logic.beans.generated.EventFields;
import ru.yandex.calendar.logic.user.UserManager;
import ru.yandex.calendar.test.auto.db.util.TestManager;
import ru.yandex.calendar.test.generic.AbstractConfTest;
import ru.yandex.inside.passport.PassportUid;
import ru.yandex.inside.passport.login.PassportLogin;

public class ActionSourceTest extends AbstractConfTest {
    @Autowired
    private UserManager userManager;
    @Autowired
    private TestManager testManager;
    @Autowired
    private GenericBeanDao genericBeanDao;

    @Test
    public void values() {
        PassportLogin user = new PassportLogin("tester11");
        PassportUid uid = userManager.getUidByLoginForTest(user);
        testManager.cleanUser(uid);

        Event event = testManager.createDefaultEvent(uid, "Test event");
        for (ActionSource actionSource : ActionSource.values()) {
            event.setCreationSource(actionSource);
            event.setModificationSource(actionSource);
            genericBeanDao.updateBeanFields(
                    event, EventFields.CREATION_SOURCE, EventFields.MODIFICATION_SOURCE);
        }
    }
}
