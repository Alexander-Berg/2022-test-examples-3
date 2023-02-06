package ru.yandex.chemodan.app.notes.core.test;

import org.junit.BeforeClass;

import ru.yandex.chemodan.util.AppNameHolder;
import ru.yandex.chemodan.util.test.AbstractTest;
import ru.yandex.devtools.test.annotations.YaIgnore;
import ru.yandex.misc.version.SimpleAppName;

/**
 * @author vpronto
 */
@YaIgnore
public abstract class NotesAbstractTest extends AbstractTest {

    @BeforeClass
    public static void setup() {
        AppNameHolder.setIfNotPresent(new SimpleAppName("chemodan", "notes"));
        AbstractTest.setup();
    }
}
