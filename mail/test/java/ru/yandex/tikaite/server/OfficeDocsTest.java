package ru.yandex.tikaite.server;

import java.util.Map;

import org.junit.Test;

import ru.yandex.test.util.TestBase;
import ru.yandex.tikaite.util.CommonFields;
import ru.yandex.tikaite.util.Json;

public class OfficeDocsTest extends TestBase {
    private static final String CONTENT_TYPE_META = "Content-Type:";
    private static final String RTF = "application/rtf";
    private static final String SPREADSHEET_TEMPLATE =
        "application/vnd.oasis.opendocument.spreadsheet-template";
    private static final String PUB = "application/x-mspublisher";
    private static final String MS_EXCEL = "application/vnd.ms-excel";
    private static final String XLSX =
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    private static final String BELOVA = "belova";

    public OfficeDocsTest() {
        super(false, 0L);
    }

    @Test
    public void testDateFormatOts() throws Exception {
        Json json = new Json(
            SPREADSHEET_TEMPLATE,
            new Json.Contains("Страница"),
            true,
            null,
            new Json.Headers(
                CONTENT_TYPE_META + SPREADSHEET_TEMPLATE
                + "\nediting-cycles:1\nnbTab:3\nmeta:table-count:3"
                + "\nnbObject:0\nEdit-Time:PT53S\nTable-Count:3"
                + "\nObject-Count:0\nmeta:object-count:0\ngenerator:LibreOffic"
                + "e/3.4$Win32 LibreOffice_project/340m1$Build-302"));
        Map<String, Object> root = json.root();
        root.put(CommonFields.CREATED, 1319789763L);
        root.put(CommonFields.MODIFIED, 1319790281L);
        DiskHandlerTest.testJson("dateformat.ots", json);
    }

    @Test
    public void testTzRtf() throws Exception {
        Json json = new Json(
            RTF,
            "Rtf file",
            true,
            null,
            new Json.Headers(
                CONTENT_TYPE_META + RTF
                + "\nmeta:word-count:1\nmeta:character-count:8"));
        Map<String, Object> root = json.root();
        root.put(CommonFields.AUTHOR, "dell");
        root.put(CommonFields.CREATED, 1312979820L);
        root.put(CommonFields.PAGES, 1L);
        DiskHandlerTest.testJson("tz.rtf", json);
    }

    @Test
    public void testPub() throws Exception {
        DiskHandlerTest.testJson(
            "mspublisher.pub",
            new Json(
                PUB,
                "",
                true,
                null,
                new Json.Headers(CONTENT_TYPE_META + PUB)));
    }

    @Test
    public void testThousandsXlsSeparator() throws Exception {
        Json json = new Json(
            MS_EXCEL,
            new Json.Contains("1000.00"),
            true,
            null,
            new Json.Headers(CONTENT_TYPE_META + MS_EXCEL + '\n'));
        Map<String, Object> root = json.root();
        root.put(CommonFields.CREATED, 1400794816L);
        root.put(CommonFields.MODIFIED, 1400794943L);
        DiskHandlerTest.testJson("thousand.xls", json);
    }

    @Test
    public void testThousandsXlsxSeparator() throws Exception {
        Json json = new Json(
            XLSX,
            new Json.Contains("ИТОГО НАКЛАДНЫЕ 1169.00"),
            true,
            null,
            new Json.Headers(
                CONTENT_TYPE_META + XLSX
                + "\nmeta:last-author:Ирина Н.А.\nLast-Author:Ирина Н.А.\n"
                + "Application-Name:Microsoft Excel\nApplication-Version:12.00"
                + "00\nextended-properties:AppVersion:12.0000\nprotected:true"
                + "\nextended-properties:Application:Microsoft Excel\n"
                + "extended-properties:DocSecurityString:None"));
        Map<String, Object> root = json.root();
        root.put(CommonFields.AUTHOR, "Пользователь");
        root.put(CommonFields.CREATED, 1367168495L);
        root.put(CommonFields.MODIFIED, 1398662789L);
        root.put(CommonFields.PRINT_DATE, 1398352641L);
        String file = "menu.xlsx";
        DiskHandlerTest.testJson(file, json);
        DiskHandlerTest.testJson(
            file,
            json,
            "",
            "extractor.tmp-file-limit = 50K");
    }

    @Test
    public void testXlsWithFractions() throws Exception {
        Json json = new Json(
            MS_EXCEL,
            new Json.StartsWith("Оглавление\nПРАЙС ЛИСТ\n"),
            true,
            null,
            new Json.Headers(
                CONTENT_TYPE_META + MS_EXCEL
                + "\nApplication-Name:Microsoft Excel\n"
                + "Last-Author:user_1\n"
                + "extended-properties:Application:Microsoft Excel\n"
                + "meta:last-author:user_1"));
        Map<String, Object> root = json.root();
        root.put(CommonFields.AUTHOR, BELOVA);
        root.put(CommonFields.CREATED, 1318311278L);
        root.put(CommonFields.MODIFIED, 1322816330L);
        DiskHandlerTest.testJson("frac.xls", json);
    }

    @Test
    public void testXlsxWithFractions() throws Exception {
        Json json = new Json(
            XLSX,
            new Json.AllOf(
                new Json.StartsWith("Оглавление\nООО "),
                new Json.Contains(
                    "Коллектор из "
                    + "нержавеющей стали с расходомерами, 8 выходов, шт")),
            true,
            null,
            new Json.Headers(
                CONTENT_TYPE_META + XLSX
                + "\nApplication-Name:Microsoft Excel\nApplication-Version"
                + ":14.0300\n"
                + "Last-Author:user\n"
                + "extended-properties:DocSecurityString:None\n"
                + "extended-properties:AppVersion:14.0300\n"
                + "extended-properties:Application:"
                + "Microsoft Excel\nmeta:last-author:user\n"
                + "protected:false"));
        Map<String, Object> root = json.root();
        root.put(CommonFields.AUTHOR, BELOVA);
        root.put(CommonFields.CREATED, 1305798937L);
        root.put(CommonFields.MODIFIED, 1371195940L);
        root.put(CommonFields.PRINT_DATE, 1337158514L);
        String name = "long.xlsx";
        DiskHandlerTest.testJson(name, json);
        root.put(CommonFields.BODY_TEXT, "Оглавл");
        root.put(CommonFields.TRUNCATED, 6L);
        DiskHandlerTest.testJson(name, json, "&limit=6");
    }

    @Test
    public void testLimitlessXlsx() throws Exception {
        Json json = new Json(
            XLSX,
            new Json.Contains("Екатеринбург"),
            true,
            null,
            new Json.Headers(
                CONTENT_TYPE_META + XLSX
                + "\nApplication-Version:14.0300\nApplication-Name:Microsoft "
                + "Excel\nextended-properties:AppVersion:14.0300\nextended-"
                + "properties:Application:Microsoft "
                + "Excel\nprotected:false\n"
                + "extended-properties:DocSecurityString:None"));
        Map<String, Object> root = json.root();
        root.put(CommonFields.CREATED, 1158364800L);
        root.put(CommonFields.MODIFIED, 1377189919L);
        String name = "limitless.xlsx";
        DiskHandlerTest.testJson(name, json);
        root.put(CommonFields.TRUNCATED, 5000L);
        DiskHandlerTest.testJson(name, json, "&limit=5000");
    }

    @Test
    public void testOomPptx() throws Exception {
        String mimetype =
            "application/vnd.openxmlformats-officedocument."
            + "presentationml.presentation";
        Json json = new Json(
            mimetype,
            new Json.Contains("Художественно-эстетическая деятельность"),
            true,
            null,
            new Json.Headers(
                CONTENT_TYPE_META + mimetype
                + "\ncp:revision:46\nlanguage:en-US\n"
                + "Application-Name:R7-Office/7.0.1.62\nNotes:108\n"
                + "Slide-Count:108\ndc:language:en-US\n"
                + "extended-properties:DocSecurityString:None\n"
                + "extended-properties:Notes:108\n"
                + "extended-properties:Application:R7-Office/7.0.1.62\n"
                + "meta:last-author:Юля Александрова\nmeta:slide-count:108\n"
                + "Last-Author:Юля Александрова\nRevision-Number:46"));
        Map<String, Object> root = json.root();
        root.put(CommonFields.AUTHOR, "Home");
        root.put(CommonFields.TITLE, "Слайд 1");
        root.put(CommonFields.CREATED, 1309947660L);
        root.put(CommonFields.MODIFIED, 1652125794L);
        root.put(CommonFields.PAGES, 108L);
        DiskHandlerTest.testJson("oom.pptx", json);
    }
}

