package ru.yandex.chemodan.app.djfs.core.index;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import lombok.SneakyThrows;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.internal.util.reflection.FieldSetter;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.DjfsFileId;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.DjfsResourceId;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.DjfsResourcePath;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.FileDjfsResource;
import ru.yandex.chemodan.app.djfs.core.test.DjfsSingleUserTestBase;
import ru.yandex.commune.bazinga.BazingaTaskManager;
import ru.yandex.misc.geo.Coordinates;
import ru.yandex.misc.io.InputStreamX;
import ru.yandex.misc.test.Assert;
import ru.yandex.misc.web.servlet.HttpServletRequestX;


public class IndexerSaveCoordinatesTest extends DjfsSingleUserTestBase {

    @Autowired
    private IndexerActions indexerActions;

    @Autowired
    private IndexerManager indexerManager;

    final private static DjfsFileId fileId = DjfsFileId.random();
    final private double latitude = 55.913917;
    final private double longitude = 37.490106;

    @Test
    public void setCoordinates() {
        DjfsResourceId resourceId = DjfsResourceId.cons(UID, fileId.getValue());
        DjfsResourcePath path = DjfsResourcePath.cons(UID, "/disk/test.jpg");
        filesystem.createFile(PRINCIPAL, path, x -> x.fileId(resourceId.getFileId()));

        indexerManager.setCoordinates(UID, resourceId, new Coordinates(latitude, longitude));

        FileDjfsResource fileResource = filesystem.find(PRINCIPAL, path, Option.empty()).cast(FileDjfsResource.class).get();
        Assert.some(fileResource.getCoordinates());
        Assert.equals(latitude, fileResource.getCoordinates().get().getLatitude());
        Assert.equals(longitude, fileResource.getCoordinates().get().getLongitude());
    }

    @Test
    @SneakyThrows
    public void submitSaveCoordinatesTask() {
        String content = "[{\"width\": 10, \"height\": 10, \"orientation\": \"portrait\","
                + " \"id\": \"" + fileId.getValue() + "\","
                + " \"altitude\": 180.07, \"latitude\": " + String.format("%.6f", latitude)
                + ", \"longitude\": " + String.format("%.6f", longitude) + "}]";

        HttpServletRequestX request = Mockito.mock(HttpServletRequestX.class);
        Mockito.when(request.getInputStreamX()).thenReturn(
                new InputStreamX(new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)))
        );

        DjfsResourceId resourceId = DjfsResourceId.cons(UID, fileId.getValue());
        DjfsResourcePath path = DjfsResourcePath.cons(UID, "/disk/test.jpg");
        filesystem.createFile(PRINCIPAL, path, x -> x.fileId(resourceId.getFileId()));

        BazingaTaskManager bazingaClientMock = Mockito.mock(BazingaTaskManager.class);
        FieldSetter.setField(
                indexerActions,
                indexerActions.getClass().getDeclaredField("bazingaTaskManager"),
                bazingaClientMock
        );

        indexerActions.saveBinaryDataByResourceId(
                UID.toString(),
                resourceId.toString(),
                request
        );

        Mockito
                .verify(bazingaClientMock, Mockito.times(1))
                .schedule(Mockito.isA(IndexerSaveCoordinatesTask.class));
    }
}
