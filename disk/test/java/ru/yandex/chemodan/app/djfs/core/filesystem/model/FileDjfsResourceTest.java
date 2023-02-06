package ru.yandex.chemodan.app.djfs.core.filesystem.model;

import org.joda.time.Instant;
import org.junit.Test;

import ru.yandex.bolts.collection.Option;
import ru.yandex.misc.test.Assert;

public class FileDjfsResourceTest {
    private static Instant EXIF_TIME = new Instant(100);
    private static Instant CREATION_TIME = new Instant(110);

    @Test
    public void emptyFetchPhotosliceTimeMissingMimetypeTest() {
        FileDjfsResource file = FileDjfsResource.random(DjfsResourcePath.cons("444:/disk/Camera Uploads/1.o"),
                x -> x.mimetype(Option.empty())
                        .exifTime(Option.of(EXIF_TIME))
                        .creationTime(CREATION_TIME));
        Assert.isEmpty(file.getPhotosliceTime());
    }

    @Test
    public void emptyFetchPhotosliceTimeNotImageMimetypeNotInPhotostreamDirsOrPhotounlim() {
        FileDjfsResource file = FileDjfsResource.random(DjfsResourcePath.cons("444:/disk/not_photostream/1.avi"),
                x -> x.mimetype(Option.of("application/vnd.rn-realmedia-vbr"))
                        .exifTime(Option.of(EXIF_TIME))
                        .creationTime(CREATION_TIME));
        Assert.isEmpty(file.getPhotosliceTime());
    }

    @Test
    public void emptyFetchPhotosliceTimeNotImageMimetypeNotInPhotostreamDirsOrPhotounlim2() {
        FileDjfsResource file = FileDjfsResource.random(DjfsResourcePath.cons("444:/disk/Camera Uploads 2/1.avi"),
                x -> x.mimetype(Option.of("application/vnd.rn-realmedia-vbr"))
                        .exifTime(Option.of(EXIF_TIME))
                        .creationTime(CREATION_TIME));
        Assert.isEmpty(file.getPhotosliceTime());
    }

    @Test
    public void emptyFetchDifferentMimetype() {
        FileDjfsResource file = FileDjfsResource.random(DjfsResourcePath.cons("444:/disk/Camera Uploads/1.jpg"),
                x -> x.mimetype(Option.of("my/mimetype"))
                        .exifTime(Option.of(EXIF_TIME))
                        .creationTime(CREATION_TIME));
        Assert.isEmpty(file.getPhotosliceTime());
    }

    @Test
    public void fetchPhotosliceTimeImageMimetypeHasExifTest() {
        FileDjfsResource file = FileDjfsResource.random(DjfsResourcePath.cons("444:/disk/Camera Uploads/1.jpg"),
                x -> x.mimetype(Option.of("image/jpeg"))
                        .exifTime(Option.of(EXIF_TIME))
                        .creationTime(CREATION_TIME));
        Assert.equals(file.getPhotosliceTime().get(), EXIF_TIME);
    }

    @Test
    public void fetchPhotosliceTimeImageMimetypeNoExifTestHasCreationTime() {
        FileDjfsResource file = FileDjfsResource.random(DjfsResourcePath.cons("444:/disk/Camera Uploads/1.jpg"),
                x -> x.mimetype(Option.of("image/jpeg"))
                        .exifTime(Option.empty())
                        .creationTime(CREATION_TIME));
        Assert.equals(file.getPhotosliceTime().get(), CREATION_TIME);
    }

    @Test
    public void fetchPhotosliceTimeVideoMimetypeHasExifTest() {
        FileDjfsResource file = FileDjfsResource.random(DjfsResourcePath.cons("444:/disk/Camera Uploads/1.avi"),
                x -> x.mimetype(Option.of("application/vnd.rn-realmedia-vbr"))
                        .exifTime(Option.of(EXIF_TIME))
                        .creationTime(CREATION_TIME));
        Assert.equals(file.getPhotosliceTime().get(), EXIF_TIME);
    }

    @Test
    public void fetchPhotosliceTimeVideoMimetypeNoExifTestHasCreationTime() {
        FileDjfsResource file = FileDjfsResource.random(DjfsResourcePath.cons("444:/disk/Camera Uploads/1.avi"),
                x -> x.mimetype(Option.of("application/vnd.rn-realmedia-vbr"))
                        .exifTime(Option.empty())
                        .creationTime(CREATION_TIME));
        Assert.equals(file.getPhotosliceTime().get(), CREATION_TIME);
    }

    @Test
    public void fetchPhotosliceTimeCustomVideoMimetypeHasExifTest() {
        FileDjfsResource file = FileDjfsResource.random(DjfsResourcePath.cons("444:/disk/Camera Uploads/1.mov"),
                x -> x.mimetype(Option.of("video/blablabla"))
                        .exifTime(Option.of(EXIF_TIME))
                        .creationTime(CREATION_TIME));
        Assert.equals(file.getPhotosliceTime().get(), EXIF_TIME);
    }

    @Test
    public void fetchPhotosliceTimeCustomVideoMimetypeNoExifTestHasCreationTime() {
        FileDjfsResource file = FileDjfsResource.random(DjfsResourcePath.cons("444:/disk/Camera Uploads/1.mov"),
                x -> x.mimetype(Option.of("video/blablabla"))
                        .exifTime(Option.empty())
                        .creationTime(CREATION_TIME));
        Assert.equals(file.getPhotosliceTime().get(), CREATION_TIME);
    }

    @Test
    public void fetchPhotosliceTimePngMimetypeHasExifTest() {
        FileDjfsResource file = FileDjfsResource.random(DjfsResourcePath.cons("444:/disk/Camera Uploads/1.png"),
                x -> x.mimetype(Option.of("image/png"))
                        .exifTime(Option.of(EXIF_TIME))
                        .creationTime(CREATION_TIME));
        Assert.equals(file.getPhotosliceTime().get(), EXIF_TIME);
    }

    @Test
    public void fetchPhotosliceTimePngMimetypeNoExifTestHasCreationTime() {
        FileDjfsResource file = FileDjfsResource.random(DjfsResourcePath.cons("444:/disk/Camera Uploads/1.png"),
                x -> x.mimetype(Option.of("image/png"))
                        .exifTime(Option.empty())
                        .creationTime(CREATION_TIME));
        Assert.equals(file.getPhotosliceTime().get(), CREATION_TIME);
    }

    @Test
    public void fetchPhotosliceTimeNonImageCorrectMimetypeRuUaLocalePhotostreamDir() {
        FileDjfsResource file = FileDjfsResource.random(DjfsResourcePath.cons("444:/disk/Фотокамера/1.png"),
                x -> x.mimetype(Option.of("image/png"))
                        .exifTime(Option.empty())
                        .creationTime(CREATION_TIME));
        Assert.equals(file.getPhotosliceTime().get(), CREATION_TIME);
    }

    @Test
    public void fetchPhotosliceTimeNonImageCorrectMimetypeTrLocalePhotostreamDir() {
        FileDjfsResource file = FileDjfsResource.random(DjfsResourcePath.cons("444:/disk/Kameradan yüklenenler/1.png"),
                x -> x.mimetype(Option.of("image/png"))
                        .exifTime(Option.empty())
                        .creationTime(CREATION_TIME));
        Assert.equals(file.getPhotosliceTime().get(), CREATION_TIME);
    }

    @Test
    public void fetchPhotosliceTimeNonImageCorrectMimetypePhotounlim() {
        FileDjfsResource file = FileDjfsResource.random(DjfsResourcePath.cons("444:/photounlim/1.png"),
                x -> x.mimetype(Option.of("image/png"))
                        .exifTime(Option.empty())
                        .creationTime(CREATION_TIME));
        Assert.equals(file.getPhotosliceTime().get(), CREATION_TIME);
    }
}
