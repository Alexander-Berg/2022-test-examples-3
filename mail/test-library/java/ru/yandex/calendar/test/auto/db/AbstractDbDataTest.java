package ru.yandex.calendar.test.auto.db;

import org.junit.Before;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.calendar.test.auto.db.util.TestManager;
import ru.yandex.calendar.test.generic.AbstractConfTest;

/**
 * @author Stepan Koltsov
 */
public abstract class AbstractDbDataTest extends AbstractConfTest {

    @Autowired
    private TestManager testManager;

    @Before
    public void setUpDbData() {
        testManager.cleanUser(TestManager.UID);
        testManager.cleanUser(TestManager.UID2);
    }

} //~
