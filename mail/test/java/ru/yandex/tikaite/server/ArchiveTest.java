package ru.yandex.tikaite.server;

import java.util.Map;

import org.junit.Test;

import ru.yandex.test.util.TestBase;
import ru.yandex.tikaite.util.CommonFields;
import ru.yandex.tikaite.util.Json;

public class ArchiveTest extends TestBase {
    private static final String CONTENT_TYPE_META = "Content-Type:";
    private static final String ZIP = "application/zip";
    private static final String RAR = "application/x-rar-compressed";
    private static final String TAR = "application/x-tar";
    private static final String GTAR = "application/x-gtar";
    private static final String SEVENZ = "application/x-7z-compressed";
    private static final String EPUB = "application/epub+zip";

    public ArchiveTest() {
        super(false, 0L);
    }

    @Test
    public void testZip() throws Exception {
        // TODO: default Tika ZIP parser doesn't list empty folders
        // (there is a folder txt/tmp; see testRar contents)
        DiskHandlerTest.testJson(
            "zip.zip",
            new Json(
                ZIP,
                new Json.AllOf(
                    new Json.Contains("Давай протестируем презентацию"),
                    new Json.Contains("Continue endurance training")),
                true,
                null,
                new Json.Contains(CONTENT_TYPE_META + ZIP)));
    }

    @Test
    public void testRar4() throws Exception {
        DiskHandlerTest.testJson(
            "rar4.rar",
            new Json(
                RAR,
                "Привет Мир\ntver.jpg\n"
                + "test.ppt\nrehab.pdf\n"
                + "hello.utf8.txt\nfb2.eml\n"
                + "wu\\tang\\Wu-Tang\n"
                + "txt\\complete.raw.txt\n"
                + "txt\\disk.mail.txt\n"
                + "txt\\hello.utf8.txt\n"
                + "txt\\html.raw.txt\n"
                + "wu\\tang\n"
                + "txt\\tmp\n"
                + "wu\n"
                + "txt",
                true,
                null,
                new Json.Contains(CONTENT_TYPE_META + RAR)));
    }

    @Test
    public void testRar4Ansi() throws Exception {
        DiskHandlerTest.testJson(
            "ansi.rar",
            new Json(
                RAR,
                "tikaite-rar\\Lykke_Li_-_I_Follow_Rivers_(Album_Version)_"
                + "(www.primemusic.ru).mp3\n"
                + "tikaite-rar\\text файл.txt\n"
                + "tikaite-rar\\yandex_email1.eml\n"
                + "tikaite-rar\\Добро Пожаловать.pdf\n"
                + "tikaite-rar",
                true,
                null,
                new Json.Contains(CONTENT_TYPE_META + RAR)));
    }

    @Test
    public void testBigRar() throws Exception {
        Json json = new Json(
            RAR,
            "",
            true,
            null,
            new Json.Contains(CONTENT_TYPE_META + RAR));
        Map<String, Object> root = json.root();
        String body =
            "folder\\subfolder\\Привет, мир\n"
            + "folder\\subfolder\\empty.txt\n"
            + "zeroes\nfolder\\subfolder\nпустая папка";
        root.put(CommonFields.BODY_TEXT, body);
        DiskHandlerTest.testJson("rar4big.rar", json);
        root.put(CommonFields.BODY_TEXT, body.replace('\\', '/'));
        DiskHandlerTest.testJson("rar5.rar", json);
    }

    @Test
    public void testRarComment() throws Exception {
        Json json = new Json(
            RAR,
            "",
            true,
            null,
            new Json.Contains(CONTENT_TYPE_META + RAR));
        Map<String, Object> root = json.root();
        String body =
            "folder\\subfolder\\Привет, мир\nfolder\\subfolder\\"
            + "empty.txt\nfolder\\subfolder\nпустая папка\nemptydir";
        root.put(CommonFields.BODY_TEXT, body);
        DiskHandlerTest.testJson("rar4comment.rar", json);
        root.put(CommonFields.COMMENT, "Как дела, world?");
        root.put(CommonFields.BODY_TEXT, body.replace('\\', '/'));
        DiskHandlerTest.testJson("rar5comment.rar", json);
    }

    @Test
    public void testRarWithRecoveryRecord() throws Exception {
        Json json = new Json(
            RAR,
            "",
            true,
            null,
            new Json.Contains(CONTENT_TYPE_META + RAR));
        Map<String, Object> root = json.root();
        String body =
            "мкаббер.tar.gz\nfolder\\subfolder\\empty.txt\n"
            + "folder\\subfolder\\файл.txt\nempty\\subempty\n"
            + "folder\\subfolder\nempty\nfolder";
        root.put(CommonFields.BODY_TEXT, body);
        DiskHandlerTest.testJson("rar4recovery.rar", json);
        DiskHandlerTest.testJson("rar4times.rar", json);
        root.put(CommonFields.BODY_TEXT, body.replace('\\', '/'));
        DiskHandlerTest.testJson("rar5recovery.rar", json);
        DiskHandlerTest.testJson("rar5times.rar", json);
    }

    @Test
    public void testTar() throws Exception {
        Json json = new Json(
            TAR,
            "",
            true,
            null,
            CONTENT_TYPE_META + TAR);
        Map<String, Object> root = json.root();
        root.put(CommonFields.BODY_TEXT, "OggSfile.txt\nHello, world");
        DiskHandlerTest.testJson("fakeogg.tar", json);
        root.put(
            CommonFields.BODY_TEXT,
            new Json.Contains(
                "Инструкция по оказанию первой медицинской помощи при "
                + "поражении электрическим током."));
        DiskHandlerTest.testJson("v7.tar", json);
        DiskHandlerTest.testJson("posix.tar", json);
        DiskHandlerTest.testJson("ustar.tar", json);
        root.put(CommonFields.MIMETYPE, GTAR);
        root.put(CommonFields.META, CONTENT_TYPE_META + GTAR);
        DiskHandlerTest.testJson("gnu.tar", json);
        DiskHandlerTest.testJson("oldgnu.tar", json);
    }

    @Test
    public void test7zv02() throws Exception {
        DiskHandlerTest.testJson(
            "v0.2.7z",
            new Json(SEVENZ, null, false, null, null));
    }

    @Test
    public void test7zv03() throws Exception {
        DiskHandlerTest.testJson(
            "v0.3.7z",
            new Json(SEVENZ, null, false, null, null));
    }

    @Test
    public void test7zv04() throws Exception {
        DiskHandlerTest.testJson(
            "v0.4.7z",
            new Json(SEVENZ, null, false, null, null));
    }

    @Test
    public void testEpub() throws Exception {
        Json json = new Json(
            EPUB,
            new Json.Contains("Peki, yaşamak mı, eğer yaşı uyuyorsa"),
            true,
            null,
            new Json.Contains(CONTENT_TYPE_META + EPUB));
        Map<String, Object> root = json.root();
        root.put(CommonFields.TITLE, "Seytanca");
        root.put(CommonFields.AUTHOR, "Yalçın Küçük");
        final long created = 1334240209L;
        root.put(CommonFields.CREATED, created);
        DiskHandlerTest.testJson("double-title.epub", json);
    }
}

