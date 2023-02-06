package ru.yandex.chemodan.uploader.exif;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Instant;
import org.joda.time.LocalDateTime;
import org.joda.time.chrono.ISOChronology;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import ru.yandex.bolts.collection.Option;
import ru.yandex.bolts.function.Function1V;
import ru.yandex.chemodan.test.TestHelper;
import ru.yandex.chemodan.uploader.registry.record.status.ExifInfo;
import ru.yandex.chemodan.uploader.registry.record.status.ExifInfo.GeoCoords;
import ru.yandex.commune.json.JsonArray;
import ru.yandex.commune.json.JsonObject;
import ru.yandex.commune.json.JsonString;
import ru.yandex.commune.json.serialize.JsonParser;
import ru.yandex.misc.db.embedded.sandbox.SandBoxResourceRule;
import ru.yandex.misc.db.embedded.tar.TarArchiveExtractor;
import ru.yandex.misc.io.InputStreamSource;
import ru.yandex.misc.io.cl.ClassLoaderUtils;
import ru.yandex.misc.io.file.File2;
import ru.yandex.misc.test.Assert;

/**
 * @author akirakozov
 */
public class ExifToolTest {
    private static final String EXIFTOOL_PATH = "Image-ExifTool-10.62/exiftool";
    private static final double EPS = 0.0001;
    private static ExifTool exifTool;

    @ClassRule
    public static final SandBoxResourceRule resourceRule = SandBoxResourceRule
            .create(new ExitToolBinaryResolver())
            .withCompressType(TarArchiveExtractor.CompressType.GZIP)
            .withIsExecutable(s -> s.equals(EXIFTOOL_PATH))
            .withEnabledCache(true)
            .build();

    @BeforeClass
    public static void prepareLogger() {
        TestHelper.initialize();
        exifTool = new ExifTool(resourceRule.getExtractedResourceDir().getAbsolutePath() + "/" + EXIFTOOL_PATH);
    }

    @Test
    public void shouldReadCreationDate() {
        withSmallImage(imgFile -> {
            ExifInfo exif = exifTool.getExif(imgFile);

            Instant expected = new DateTime(2012, 1, 7, 14, 38, 20, 0, ISOChronology.getInstanceUTC()).toInstant();
            Assert.equals(Option.of(expected), exif.getCreationDate());
        });
    }

    @Test
    public void shouldReadGeoCoords() {
        withSmallImage(imgFile -> {
            ExifInfo exif = exifTool.getExif(imgFile);

            ExifInfo.GeoCoords geoCoords = exif.getGeoCoords().get();
            Assert.equals(geoCoords.getLatitude(), 59.9591, EPS);
            Assert.equals(geoCoords.getLongitude(), 30.406, EPS);
        });
    }

    @Test
    public void copyExif() {
        File2.withNewTempDir(copyExifF(false).asFunctionReturnNull());
    }

    @Test
    public void copyExifWithoutOrientation() {
        File2.withNewTempDir(copyExifF(true).asFunctionReturnNull());
    }

    private Function1V<File2> copyExifF(final boolean excludeOrientation) {
        return parentDir -> {
            File2 child = parentDir.child("new_file");
            File2 catCopy = parentDir.child("cat");

            smallImg().readTo(child);
            catImg().readTo(catCopy);

            Assert.notEquals(exifTool.getExif(catCopy), exifTool.getExif(child));

            exifTool.copyExif(catCopy, child, excludeOrientation);
            Assert.equals(exifTool.getExif(catCopy), exifTool.getExif(child));
        };
    }

    @Test
    public void addGeoAndCreateDateTimeTags() {
        File2.withNewTempFile("", "", new Function1V<File2>() {
            public void apply(File2 copy) {
                smallImg().readTo(copy);

                LocalDateTime dt = new LocalDateTime(2010, 10, 2, 10, 32, 20);
                GeoCoords coords = new GeoCoords(-53.2, 130.2);
                exifTool.addGeoAndCreateTimeTags(copy, Option.of(coords), Option.of(dt));
                ExifInfo info = exifTool.getExif(copy);
                Assert.equals(coords, info.getGeoCoords().get());
                // Parser parse datetime in default timezone CHEMODAN-16607
                Assert.equals(dt, new LocalDateTime(info.getCreationDate().get(), DateTimeZone.getDefault()));

                coords = new GeoCoords(13.2, -30.8);
                exifTool.addGeoAndCreateTimeTags(copy, Option.of(coords), Option.of(dt));
                Assert.equals(coords, exifTool.getExif(copy).getGeoCoords().get());
            }
        }.asFunctionReturnNull());
    }

    @Test
    public void extractExifInJson() {
        withSmallImage(imgFile -> {
            String exif = exifTool.getFullExifInJson(imgFile);

            JsonArray value = (JsonArray) JsonParser.getInstance().parse(exif);
            JsonObject dict = (JsonObject) value.getArray().get(0);
            Assert.equals("iPhone 4", ((JsonString) dict.get("Model")).getString());
            Assert.isEmpty(dict.getO("MIMEType"));
            Assert.isEmpty(dict.getO("ExifToolVersion"));
        });
    }

    private InputStreamSource smallImg() {
        return ClassLoaderUtils.streamSourceForResource(this.getClass(), "small.jpg");
    }

    private InputStreamSource catImg() {
        return ClassLoaderUtils.streamSourceForResource(this.getClass(), "cat90.jpg");
    }

    private void withSmallImage(Function1V<File2> handle) {
        File2.withNewTempFile(imgFile -> {
            smallImg().readTo(imgFile);
            handle.apply(imgFile);
        });
    }

}
