package ru.yandex.chemodan.app.notes.core.test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.io.IOUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.app.dataapi.api.user.DataApiUserId;
import ru.yandex.chemodan.app.dataapi.core.dao.test.ImportDataApiEmbeddedPg;
import ru.yandex.chemodan.app.dataapi.test.TestConstants;
import ru.yandex.chemodan.app.notes.api.NotesActionsContextConfiguration;
import ru.yandex.chemodan.app.notes.core.NotesContentManager;
import ru.yandex.chemodan.app.notes.core.NotesCoreContextConfiguration;
import ru.yandex.chemodan.app.notes.dao.test.ImportNotesEmbeddedPg;
import ru.yandex.chemodan.boot.ChemodanInitContextConfiguration;
import ru.yandex.chemodan.mpfs.MpfsCallbackResponse;
import ru.yandex.chemodan.mpfs.MpfsClient;
import ru.yandex.chemodan.mpfs.MpfsClientImpl;
import ru.yandex.chemodan.mpfs.MpfsListResponse;
import ru.yandex.chemodan.mpfs.MpfsStoreOperation;
import ru.yandex.chemodan.mpfs.MpfsStoreOperationContext;
import ru.yandex.chemodan.util.ZkUtils;
import ru.yandex.chemodan.zk.configuration.ImportZkEmbeddedConfiguration;
import ru.yandex.commune.a3.action.result.ApplicationInfo;
import ru.yandex.commune.alive2.location.TestLocationResolverConfiguration;
import ru.yandex.commune.bazinga.BazingaTaskManager;
import ru.yandex.commune.dynproperties.DynamicProperty;
import ru.yandex.commune.json.jackson.JacksonContextConfiguration;
import ru.yandex.commune.zk2.ZkPath;
import ru.yandex.inside.mds.MdsFileKey;
import ru.yandex.inside.passport.PassportUid;
import ru.yandex.inside.passport.blackbox2.Blackbox2;
import ru.yandex.inside.passport.blackbox2.BlackboxQueryable;
import ru.yandex.inside.passport.blackbox2.protocol.response.BlackboxCorrectResponse;
import ru.yandex.inside.passport.blackbox2.protocol.response.BlackboxDbFields;
import ru.yandex.inside.utils.Language;
import ru.yandex.misc.ExceptionUtils;
import ru.yandex.misc.env.EnvironmentType;
import ru.yandex.misc.io.OutputStreamX;
import ru.yandex.misc.io.http.HttpStatus;
import ru.yandex.misc.ip.IpAddress;
import ru.yandex.misc.reflection.FieldX;
import ru.yandex.misc.version.AppName;
import ru.yandex.misc.version.SimpleAppName;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author yashunsky
 */
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

public class NotesTestContextConfiguration {
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
    @Primary
    NotesContentManager contentMdsManager() {
        return new NotesContentManager() {
            private volatile byte[] bytes = new byte[0];

            @Override
            public MdsFileKey put(String id, DataApiUserId uid, long revision, byte[] content) {
                this.bytes = content;
                return MdsFileKey.parse("1/notes-snapsot-1");
            }

            @Override
            public void get(MdsFileKey key, OutputStream outputStream) {
                try (InputStream stream = new ByteArrayInputStream(bytes);
                     OutputStreamX streamX = OutputStreamX.wrap(outputStream)) {
                    IOUtils.copy(stream, streamX);
                } catch (Exception e) {
                    throw ExceptionUtils.translate(e);
                }
            }

            @Override
            public void deleteData(MdsFileKey key) {
            }
        };
    }

    @Bean
    @Primary
    MpfsClient mpfsClient() {
        MpfsClient mpfsClient = mock(MpfsClientImpl.class);
        //установка DynamicProperies в значение not-null - без этого контекст не поднимется, поскольку ZooKeeper-свзянная
        //кофигурация проверяет все DynamicProperty поля на not-null
        Cf.x(MpfsClientImpl.class.getDeclaredFields()).filter(field -> field.getType().equals(DynamicProperty.class))
                .map(FieldX::wrap).forEach(field -> {
                    field.setAccessible(true);
                    field.set(mpfsClient, new DynamicProperty<>("test", ""));
                });
        MpfsStoreOperation mpfsStoreOperation = mock(MpfsStoreOperation.class);
        when(mpfsStoreOperation.getUploadUrl()).thenReturn(Option.of("https://upload.url"));
        when(mpfsClient.store(any(MpfsStoreOperationContext.class), any())).thenReturn(mpfsStoreOperation);

        MpfsCallbackResponse initResponse = mock(MpfsCallbackResponse.class);
        when(initResponse.getStatusCode()).thenReturn(HttpStatus.SC_200_OK);
        when(mpfsClient.initNotes(any(), any(), any(), any())).thenReturn(initResponse);

        MpfsListResponse listResponse = mock(MpfsListResponse.class);
        when(listResponse.getChildren()).thenReturn(Cf.list());
        when(mpfsClient.listByUidAndPath(any(), any(), any(), any(), any(), anyBoolean())).thenReturn(listResponse);
        return mpfsClient;
    }

    @Bean
    @Primary
    BazingaTaskManager bazingaTaskManager() {
        return mock(BazingaTaskManager.class);
    }

    @Bean
    @Primary
    Blackbox2 blackbox() {
        BlackboxCorrectResponse response = mock(BlackboxCorrectResponse.class);
        when(response.getDbFields()).thenReturn(Cf.map(BlackboxDbFields.LANG, Language.ENGLISH.value()));

        BlackboxQueryable blackboxQueryable = mock(BlackboxQueryable.class);
        when(blackboxQueryable.userInfo(
                any(IpAddress.class), any(PassportUid.class), any(ListF.class), any(ListF.class))).thenReturn(response);

        Blackbox2 blackbox = mock(Blackbox2.class);
        when(blackbox.query()).thenReturn(blackboxQueryable);
        return blackbox;
    }
}
