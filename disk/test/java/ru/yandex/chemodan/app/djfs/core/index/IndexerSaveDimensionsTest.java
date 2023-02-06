package ru.yandex.chemodan.app.djfs.core.index;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.chemodan.app.djfs.core.filesystem.model.DjfsFileId;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.DjfsResourceId;
import ru.yandex.chemodan.app.djfs.core.test.DjfsSingleUserTestBase;
import ru.yandex.misc.io.InputStreamX;
import ru.yandex.misc.web.servlet.HttpServletRequestX;


/**
 * Created by kis8ya on 28.08.19.
 */
public class IndexerSaveDimensionsTest extends DjfsSingleUserTestBase {

    @Autowired
    private IndexerActions indexerActions;

    private HttpServletRequestX request;
    private String content;

    final private static DjfsFileId fileId = DjfsFileId.random();

    @Test(expected = IndexerInvalidBodyException.class)
    public void setDimensionsWithMissedFileId() {
        content = "[{\"width\": 10, \"height\": 10, \"orientation\": \"portrait\"}]";
        request = Mockito.mock(HttpServletRequestX.class);
        Mockito.when(request.getInputStreamX()).thenReturn(
                new InputStreamX(new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)))
        );

        DjfsResourceId resourceId = DjfsResourceId.cons(UID, fileId.getValue());

        indexerActions.saveBinaryDataByResourceId(
                UID.toString(),
                resourceId.toString(),
                request
        );
    }

    @Test(expected = IndexerInvalidDimensionsException.class)
    public void setDimensionsWithInvalidOrientation() {
        content = "[{\"width\": 10, \"height\": 10, \"orientation\": \"unknown\", \"id\": \"" + fileId.getValue() + "\"}]";
        request = Mockito.mock(HttpServletRequestX.class);
        Mockito.when(request.getInputStreamX()).thenReturn(
                new InputStreamX(new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)))
        );

        DjfsResourceId resourceId = DjfsResourceId.cons(UID, fileId.getValue());

        indexerActions.saveBinaryDataByResourceId(
                UID.toString(),
                resourceId.toString(),
                request
        );
    }

    @Test(expected = IndexerInvalidDimensionsException.class)
    public void setDimensionsWithInvalidDimensions() {
        content = "[{\"width\": -1, \"height\": 10, \"orientation\": \"portrait\", \"id\": \"" + fileId.getValue() + "\"}]";
        request = Mockito.mock(HttpServletRequestX.class);
        Mockito.when(request.getInputStreamX()).thenReturn(
                new InputStreamX(new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)))
        );

        DjfsResourceId resourceId = DjfsResourceId.cons(UID, fileId.getValue());

        indexerActions.saveBinaryDataByResourceId(
                UID.toString(),
                resourceId.toString(),
                request
        );
    }
}
