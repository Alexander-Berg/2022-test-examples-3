package ru.yandex.chemodan.app.notes.core.test;

import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.chemodan.boot.ChemodanPropertiesLoadStrategy;
import ru.yandex.chemodan.test.TestHelper;
import ru.yandex.misc.property.load.PropertiesLoader;
import ru.yandex.misc.version.SimpleAppName;

import static ru.yandex.chemodan.app.dataapi.core.dao.test.ActivateDataApiEmbeddedPg.DATAAPI_EMBEDDED_PG;
import static ru.yandex.chemodan.app.notes.dao.test.ActivateNotesEmbeddedPg.NOTES_EMBEDDED_PG;
import static ru.yandex.misc.db.embedded.ActivateEmbeddedPg.EMBEDDED_PG;

/**
 * @author yashunsky
 */
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext
@ActiveProfiles({EMBEDDED_PG, NOTES_EMBEDDED_PG, DATAAPI_EMBEDDED_PG})
@ContextConfiguration(classes = NotesTestContextConfiguration.class)
public abstract class NotesWithContextAbstractTest {
    @BeforeClass
    public static void init() {
        TestHelper.initialize();
        PropertiesLoader.initialize(
                new ChemodanPropertiesLoadStrategy(new SimpleAppName("disk", "counters-api"), true));
    }
}
