package ru.yandex.chemodan.app.djfs.core.index;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;

import net.jodah.failsafe.FailsafeException;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.internal.util.reflection.FieldSetter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.UncategorizedSQLException;

import ru.yandex.chemodan.app.djfs.core.filesystem.model.DjfsFileId;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.DjfsResourceId;
import ru.yandex.chemodan.app.djfs.core.test.DjfsSingleUserTestBase;
import ru.yandex.commune.bazinga.BazingaTaskManager;
import ru.yandex.misc.io.InputStreamX;
import ru.yandex.misc.web.servlet.HttpServletRequestX;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;

/**
 * Created by kis8ya on 28.08.19.
 */
public class IndexerSaveAestheticsTest extends DjfsSingleUserTestBase {

    @Autowired
    private IndexerActions indexerActions;

    private HttpServletRequestX request;
    private String content;

    final private static DjfsFileId fileId = DjfsFileId.random();

    @Test(expected = IndexerInvalidBodyException.class)
    public void setAestheticsWithMissedFileId() {
        content = "{\"cost_disk_aethetic_0\": 7}";
        request = mock(HttpServletRequestX.class);
        Mockito.when(request.getInputStreamX()).thenReturn(
                new InputStreamX(new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)))
        );

        DjfsResourceId resourceId = DjfsResourceId.cons(UID, fileId.getValue());

        indexerActions.saveAestheticsByResourceId(
                UID.toString(),
                resourceId.toString(),
                request
        );
    }

    @Test(expected = IndexerInvalidBodyException.class)
    public void setAestheticsWithMissedBeauty() {
        content = "{\"enot\": 777, \"id\": \"" + fileId.getValue() + "\"}";
        request = mock(HttpServletRequestX.class);
        Mockito.when(request.getInputStreamX()).thenReturn(
                new InputStreamX(new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)))
        );

        DjfsResourceId resourceId = DjfsResourceId.cons(UID, fileId.getValue());

        indexerActions.saveAestheticsByResourceId(
                UID.toString(),
                resourceId.toString(),
                request
        );
    }

    @Test
    public void submitTaskRetries() throws NoSuchFieldException {
        int expectedRetriesCount = 3;
        content = "{\"cost_disk_aethetic_0\": 777, \"id\": \"" + fileId.getValue() + "\"}";
        request = mock(HttpServletRequestX.class);
        Mockito.when(request.getInputStreamX()).thenReturn(
                new InputStreamX(new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)))
        );

        DjfsResourceId resourceId = DjfsResourceId.cons(UID, fileId.getValue());

        BazingaTaskManager bazingaClientMock = mock(BazingaTaskManager.class);
        when(bazingaClientMock.schedule(any()))
                .thenThrow(new UncategorizedSQLException("task", "sql", new SQLException()));
        FieldSetter.setField(
                indexerActions,
                indexerActions.getClass().getDeclaredField("bazingaTaskManager"),
                bazingaClientMock
        );

        try {
            indexerActions.saveAestheticsByResourceId(
                    UID.toString(),
                    resourceId.toString(),
                    request
            );
        } catch (FailsafeException e) {
            // замоконый менеджер постановки тасок постоянно будет бросать исключения что в итоге приведет
            // что ретрай бросит исключение FailsafeException на последней попытке
        }

        verify(bazingaClientMock, times(1 + expectedRetriesCount)).schedule(any());
    }

}
