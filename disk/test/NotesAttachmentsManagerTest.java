package ru.yandex.chemodan.app.notes.core.test;

import org.joda.time.Instant;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.app.dataapi.api.user.DataApiUserId;
import ru.yandex.chemodan.app.dataapi.core.dao.test.ImportDataApiEmbeddedPg;
import ru.yandex.chemodan.app.dataapi.test.TestConstants;
import ru.yandex.chemodan.app.notes.api.NotesActionsContextConfiguration;
import ru.yandex.chemodan.app.notes.core.NotesAttachmentsManager;
import ru.yandex.chemodan.app.notes.core.NotesAttachmentsManagerImpl;
import ru.yandex.chemodan.app.notes.core.NotesAttachmentsProperties;
import ru.yandex.chemodan.app.notes.core.NotesCoreContextConfiguration;
import ru.yandex.chemodan.app.notes.core.ResultWithRevision;
import ru.yandex.chemodan.app.notes.core.model.notes.Attachment;
import ru.yandex.chemodan.app.notes.core.model.notes.AttachmentAddition;
import ru.yandex.chemodan.app.notes.core.model.notes.AttachmentCreationResult;
import ru.yandex.chemodan.app.notes.core.model.notes.AttachmentQuery;
import ru.yandex.chemodan.app.notes.core.model.notes.Attachments;
import ru.yandex.chemodan.app.notes.dao.NotesDao;
import ru.yandex.chemodan.app.notes.dao.test.ImportNotesEmbeddedPg;
import ru.yandex.chemodan.boot.ChemodanInitContextConfiguration;
import ru.yandex.chemodan.boot.ChemodanPropertiesLoadStrategy;
import ru.yandex.chemodan.mpfs.MpfsClientImpl;
import ru.yandex.chemodan.test.TestHelper;
import ru.yandex.chemodan.util.ZkUtils;
import ru.yandex.chemodan.zk.configuration.ImportZkEmbeddedConfiguration;
import ru.yandex.commune.a3.action.result.ApplicationInfo;
import ru.yandex.commune.alive2.location.TestLocationResolverConfiguration;
import ru.yandex.commune.json.jackson.JacksonContextConfiguration;
import ru.yandex.commune.zk2.ZkPath;
import ru.yandex.devtools.test.annotations.YaIgnore;
import ru.yandex.misc.env.EnvironmentType;
import ru.yandex.misc.property.load.PropertiesLoader;
import ru.yandex.misc.test.Assert;
import ru.yandex.misc.version.AppName;
import ru.yandex.misc.version.SimpleAppName;

import static ru.yandex.chemodan.app.dataapi.core.dao.test.ActivateDataApiEmbeddedPg.DATAAPI_EMBEDDED_PG;
import static ru.yandex.chemodan.app.notes.dao.test.ActivateNotesEmbeddedPg.NOTES_EMBEDDED_PG;
import static ru.yandex.misc.db.embedded.ActivateEmbeddedPg.EMBEDDED_PG;

@ActiveProfiles({EMBEDDED_PG, NOTES_EMBEDDED_PG, DATAAPI_EMBEDDED_PG})
@ContextConfiguration(classes = NotesAttachmentsManagerTest.ContextConfiguration.class)
@RunWith(SpringJUnit4ClassRunner.class)
public class NotesAttachmentsManagerTest {

    @Autowired
    NotesAttachmentsManager attachmentsManager;

    @BeforeClass
    public static void init() {
        TestHelper.initialize();
        PropertiesLoader.initialize(
                new ChemodanPropertiesLoadStrategy(new SimpleAppName("disk", "counters-api"), true));
    }

    @Test
    @YaIgnore
    //Not for CI
    public void getAttachments() {

        DataApiUserId uid = DataApiUserId.parse("3000185708");
        String noteId = "test";
        ResultWithRevision<Attachments> test = attachmentsManager.getAttachments(AttachmentQuery.builder()
                        .uid(uid)
                        .noteId(noteId)
                        .limit(Option.of(100))
                        .rev(Option.of(10L))
                        .offset(Option.empty())
                        .build());
        Assert.notNull(test);
        String resourceId = test.result.items.get(0).resourceId;

        ResultWithRevision<Attachment> attachment = attachmentsManager.getAttachment(uid, noteId, resourceId);
        Assert.notNull(attachment);

    }

    @Before
    public void addAttachments() {
        ResultWithRevision<AttachmentCreationResult> attachmentCreationResultWithRevision = attachmentsManager.
                addAttachments(AttachmentAddition.builder().uid(DataApiUserId.parse("3000185708")).id("test")
                        .name("name").revision(Option.of(1L)).noteId("noteId").build(), Instant.now());

        Assert.notNull(attachmentCreationResultWithRevision.result.href);

        attachmentsManager.deleteAttachment(DataApiUserId.parse("3000185708"),
                "test", "attach", Option.of(20L), Instant.now());
    }

    @Configuration
    @ImportNotesEmbeddedPg
    @ImportDataApiEmbeddedPg
    @ImportZkEmbeddedConfiguration
    @Import({
            NotesActionsContextConfiguration.class,
            ChemodanInitContextConfiguration.class,
            NotesCoreContextConfiguration.class,
            TestLocationResolverConfiguration.class,
            JacksonContextConfiguration.class
    })
    public static class ContextConfiguration {

        @Bean
        public AppName appName() {
            return new SimpleAppName("disk", "notes");
        }

        @Bean
        public ApplicationInfo applicationInfo() {
            return ApplicationInfo.UNKNOWN;
        }

        @Bean
        public ZkPath zkRoot() {
            return ZkUtils.rootPath(TestConstants.DATAAPI, EnvironmentType.TESTS);
        }

        @Bean
        public NotesAttachmentsManager attachmentsManager(NotesDao notesDao)
        {
            NotesAttachmentsProperties notesAttachmentsProperties = new NotesAttachmentsProperties();
            notesAttachmentsProperties.setNotesRoot("/disk/notes");
            MpfsClientImpl mpfsClient = new MpfsClientImpl("mpfs-stable.dst.yandex.net");
            return new NotesAttachmentsManagerImpl(mpfsClient, notesDao, notesAttachmentsProperties, "");
        }

    }
}
