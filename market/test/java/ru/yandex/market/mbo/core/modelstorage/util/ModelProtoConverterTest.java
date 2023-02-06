package ru.yandex.market.mbo.core.modelstorage.util;

import org.junit.Test;
import ru.yandex.market.mbo.db.modelstorage.ModelStorageTestUtil;
import ru.yandex.market.mbo.gwt.models.ModificationSource;
import ru.yandex.market.mbo.gwt.models.modelstorage.Video;
import ru.yandex.market.mbo.http.ModelStorage;

import java.util.Date;

import static org.junit.Assert.assertEquals;

/**
 * @author Anastasiya Emelianova / orphie@ / 5/26/21
 */
public class ModelProtoConverterTest {
    private static final Long LAST_MODIFICATION_UID = 1L;
    private static final Long LAST_MODIFICATION_DATE = 11L;
    private static final String URL = "url";
    private static final String URL_SOURCE = "url_source";

    @Test
    public void testConvertVideoToProto() {
        Video video = createVideo();

        ModelStorage.Video protoVideo = ModelProtoConverter.convert(video);
        assertEquals(video.getUrl(), protoVideo.getUrl());
        assertEquals((long) video.getLastModificationUid(), protoVideo.getUserId());
        assertEquals(video.getLastModificationDate().getTime(), protoVideo.getModificationDate());
        assertEquals(video.getVideoSource().name(), protoVideo.getSource().name());
        assertEquals(video.getModificationSource().name(), protoVideo.getModificationSource().name());
        assertEquals(video.getUrlSource(), protoVideo.getUrlSource());
    }

    @Test
    public void testConvertToVideoObject() {
        Video video = createVideo();

        ModelStorage.Video protoVideo1 = ModelProtoConverter.convert(video);
        Video video1 = ModelProtoConverter.convert(protoVideo1);

        ModelStorage.Video protoVideo2 = ModelProtoConverter.convert(video1);
        ModelStorageTestUtil.Diff diff = ModelStorageTestUtil.generateDiff(protoVideo1, protoVideo2);
        diff.assertEquals();
    }

    @Test
    public void testConvertWithCopy() {
        Video video = createVideo();
        Video copy = new Video(video);

        ModelStorage.Video protoVideo1 = ModelProtoConverter.convert(video);
        ModelStorage.Video protoVideo2 = ModelProtoConverter.convert(copy);
        ModelStorageTestUtil.Diff diff = ModelStorageTestUtil.generateDiff(protoVideo1, protoVideo2);
        diff.assertEquals();
    }

    private Video createVideo() {
        Video video = new Video();
        video.setUrl(URL);
        video.setLastModificationUid(LAST_MODIFICATION_UID);
        video.setLastModificationDate(new Date(LAST_MODIFICATION_DATE));
        video.setVideoSource(Video.VideoSource.YANDEX);
        video.setModificationSource(ModificationSource.ASSESSOR);
        video.setUrlSource(URL_SOURCE);
        return video;
    }
}
