package ru.yandex.market.mbo.gwt.models.modelstorage;

import ru.yandex.market.mbo.gwt.models.ModificationSource;
import ru.yandex.market.mbo.http.ModelStorage;

import java.util.Date;

/**
 * @author Anastasiya Emelianova / orphie@ / 5/26/21
 */
public class VideoBuilder {
    private Video video = new Video();

    private VideoBuilder() {

    }

    public static VideoBuilder newBuilder() {
        return new VideoBuilder();
    }

    public static VideoBuilder newBuilder(
        String url,
        ModificationSource modificationSource,
        Long lastModificationUid,
        Date lastModificationDate,
        String urlSource,
        ModelStorage.VideoSource videoSource
    ) {
        return new VideoBuilder()
            .setUrl(url)
            .setModificationSource(modificationSource)
            .setLastModificationUid(lastModificationUid)
            .setLastModificationDate(lastModificationDate)
            .setUrlSource(urlSource)
            .setVideoSource(videoSource);
    }

    public VideoBuilder setUrl(String url) {
        video.setUrl(url);
        return this;
    }

    public VideoBuilder setModificationSource(ModificationSource modificationSource) {
        video.setModificationSource(modificationSource);
        return this;
    }

    public VideoBuilder setLastModificationUid(Long lastModificationUid) {
        video.setLastModificationUid(lastModificationUid);
        return this;
    }

    public VideoBuilder setLastModificationDate(Date lastModificationDate) {
        video.setLastModificationDate(lastModificationDate);
        return this;
    }

    public VideoBuilder setUrlSource(String urlSource) {
        video.setUrlSource(urlSource);
        return this;
    }

    public VideoBuilder setVideoSource(ModelStorage.VideoSource videoSource) {
        video.setVideoSource(Video.VideoSource.valueOf(videoSource.name()));
        return this;
    }

    public Video build() {
        return new Video(this.video);
    }
}
