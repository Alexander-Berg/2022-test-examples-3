package ru.yandex.tikaite.server;

import java.util.Map;

import org.junit.Test;

import ru.yandex.test.util.TestBase;
import ru.yandex.tikaite.util.CommonFields;
import ru.yandex.tikaite.util.Json;

// CSOFF: MagicNumber
public class PdfBoxTest extends TestBase {
    public static final String PDFBOX_META =
        "\nparser:pdfbox";

    private static final String APPLICATION_PDF = "application/pdf";
    private static final String CONTENT_TYPE_META = "Content-Type:";
    private static final String PSCRIPT = "PScript5.dll Version 5.2.2";
    private static final String PDF_CREATOR = "Paychex MMS PDF Creator v1.1.0";
    private static final String POWER_PDF = "PowerPdf version 0.9";
    private static final String ONE_TWO = "1.2";
    private static final String ONE_FOUR = "1.4";
    private static final String ONE_FIVE = "1.5";
    private static final String ONE_SIX = "1.6";

    public PdfBoxTest() {
        super(false, 0L);
    }

    private static String pdfMeta(final String version) {
        return "\ndc:format:application/pdf; version=" + version
            + "\npdf:PDFVersion:" + version + "\npdf:encrypted:false\n"
            + "pdf:hasXFA:false\npdf:hasMarkedContent:false\n"
            + "pdf:hasXMP:false";
    }

    @Test
    public void testKasperskyPdf() throws Exception {
        Json json = new Json(
            APPLICATION_PDF,
            new Json.StartsWith(
                "AV-Desk\n«Доктор Веб», "
                + "Центральный офис в России\n125124\n"
                + "Россия, Москва\n3-я улица Ямского "
                + "поля, вл.2, корп.12А\n"
                + "Веб-сайт: www.drweb.com\n"
                + "Телефон: +7 (495) 789-45-87\n"
                + "Информацию о региональных "
                + "представительствах и офисах Вы\n"
                + "можете найти на официальном "
                + "сайте компании.\n"
                + "Dr.Web AV-Desk\n"
                + "Версия 5.0.1\n"
                + "Руководство администратора"),
            true,
            null,
            new Json.Headers(
                CONTENT_TYPE_META + APPLICATION_PDF + PDFBOX_META
                + "\npdf:docinfo:created:2009-07-16T16:06:37Z"
                + "\npdf:docinfo:creator:«Äîêòîð Âåá»"
                + "\npdf:docinfo:creator_tool:Help & Manual 5"
                + "\npdf:docinfo:modified:2009-07-16T16:06:37Z"
                + "\npdf:docinfo:producer:wPDF3 by WPCubed GmbH"
                + "\npdf:docinfo:title:Dr.Web AV-Desk"
                + "\nxmpMM:DocumentID:367494b61071fc4a9dd8f6ea87775eac"
                + pdfMeta(ONE_FOUR)
                    .replace("encrypted:false", "encrypted:true")
                    .replace(
                        "hasXMP:false",
                        "hasXMP:true\npdf:producer:wPDF3 by WPCubed GmbH\n"
                        + "xmp:CreateDate:2009-07-16T16:06:37Z\n"
                        + "xmp:MetadataDate:2009-07-16T16:06:37Z\n"
                        + "xmp:ModifyDate:2009-07-16T16:06:37Z")));
        Map<String, Object> root = json.root();
        root.put(CommonFields.AUTHOR, "«Доктор Веб»");
        root.put(CommonFields.CREATED, 1247760397L);
        root.put(CommonFields.MODIFIED, 1247760397L);
        root.put(CommonFields.PAGES, 468L);
        root.put(CommonFields.PRODUCER, "wPDF3 by WPCubed GmbH");
        root.put(CommonFields.TITLE, "Dr.Web AV-Desk");
        root.put(CommonFields.TOOL, "Help & Manual 5");
        DiskHandlerTest.testJson("kaspersky.pdf", json);
    }

    @Test
    public void testPaulsyardPdf() throws Exception {
        Json json = new Json(
            APPLICATION_PDF,
            new Json.StartsWith("Офисное здание в престижном"),
            true,
            null,
            new Json.Headers(
                CONTENT_TYPE_META + APPLICATION_PDF + PDFBOX_META
                + "\npdf:docinfo:created:2013-07-10T14:28:09Z"
                + "\npdf:docinfo:creator:Kozin R.I."
                + "\npdf:docinfo:creator_tool:Paul`s Yard Realty"
                + "\npdf:docinfo:modified:2013-07-10T14:28:10Z"
                + "\npdf:docinfo:producer:PowerPdf version 0.9"
                + pdfMeta(ONE_TWO)));
        Map<String, Object> root = json.root();
        root.put(CommonFields.AUTHOR, "Kozin R.I.");
        root.put(CommonFields.PAGES, 1L);
        root.put(CommonFields.PRODUCER, POWER_PDF);
        root.put(CommonFields.TOOL, "Paul`s Yard Realty");
        root.put(CommonFields.CREATED, 1373466489L);
        root.put(CommonFields.MODIFIED, 1373466490L);
        DiskHandlerTest.testJson("paulsyard.pdf", json);
    }

    @Test
    public void testEmiratesPdf() throws Exception {
        Json json = new Json(
            APPLICATION_PDF,
            new Json.Contains("Scan the barcode or use the e-ticket number"),
            true,
            null,
            new Json.Headers(
                CONTENT_TYPE_META + APPLICATION_PDF + PDFBOX_META
                + "\npdf:docinfo:created:2012-12-26T07:06:33Z"
                + "\npdf:docinfo:modified:2012-12-26T07:06:33Z"
                + "\npdf:docinfo:producer:iText 2.0.6 (by lowagie.com)"
                + pdfMeta(ONE_FOUR)));
        Map<String, Object> root = json.root();
        root.put(CommonFields.PAGES, 2L);
        root.put(CommonFields.PRODUCER, "iText 2.0.6 (by lowagie.com)");
        root.put(CommonFields.CREATED, 1356505593L);
        root.put(CommonFields.MODIFIED, 1356505593L);
        DiskHandlerTest.testJson("emirates.pdf", json);
    }

    @Test
    public void testSindbadPdf() throws Exception {
        Json json = new Json(
            APPLICATION_PDF,
            new Json.Contains("Маршрутная квитанция"),
            true,
            null,
            new Json.Headers(
                CONTENT_TYPE_META + APPLICATION_PDF + PDFBOX_META
                + "\npdf:docinfo:created:2013-03-11T08:58:28Z"
                + "\npdf:docinfo:producer:wkhtmltopdf"
                + "\npdf:docinfo:title:Sindbad.Ru — маршрутная квитанция"
                + pdfMeta(ONE_FOUR)));
        Map<String, Object> root = json.root();
        root.put(CommonFields.PAGES, 1L);
        root.put(CommonFields.PRODUCER, "wkhtmltopdf");
        root.put(CommonFields.CREATED, 1362992308L);
        root.put(
            CommonFields.TITLE,
            "Sindbad.Ru — маршрутная квитанция");
        DiskHandlerTest.testJson("sindbad.pdf", json);
    }

    @Test
    public void testBigPdf() throws Exception {
        Json json = new Json(
            APPLICATION_PDF,
            new Json.Contains("Standard Catalog of World Coins"),
            true,
            null,
            new Json.Headers(
                CONTENT_TYPE_META + APPLICATION_PDF + PDFBOX_META + '\n'
                + "xmpMM:DocumentID:uuid:3989f32c-52c0-11df-bd58-000a9572478c"
                + "\npdf:docinfo:created:2010-04-28T12:41:00Z"
                + "\npdf:docinfo:creator:Numismatics Staff"
                + "\npdf:docinfo:creator_tool:PScript5.dll Version 5.2.2"
                + "\npdf:docinfo:keywords:world coin; coin; world coin "
                + "listings; world coin value; world coin guide; world coin "
                + "grade"
                + "\npdf:docinfo:modified:2010-12-24T22:15:24Z"
                + "\npdf:docinfo:producer:Acrobat Distiller 7.0.5 for "
                + "Macintosh"
                + "\npdf:docinfo:subject:World Coins 1901-2000"
                + "\npdf:docinfo:title:2010 Standard Catalog of World Coins "
                + "1901-2000"
                + pdfMeta(ONE_SIX)
                    .replace(
                        "pdf:hasMarkedContent:false\npdf:hasXMP:false",
                        "pdf:hasMarkedContent:true\npdf:hasXMP:true\n"
                        + "pdf:producer:Acrobat Distiller 7.0.5 for Macintosh\n"
                        + "xmp:CreateDate:2010-04-28T07:41:05Z\n"
                        + "xmp:MetadataDate:2010-12-25T00:15:24Z\n"
                        + "xmp:ModifyDate:2010-12-25T00:15:24Z")));
        Map<String, Object> root = json.root();
        root.put(CommonFields.AUTHOR, "Numismatics Staff");
        root.put(
            CommonFields.KEYWORDS,
            "world coin; coin; world coin listings"
            + "; world coin value; world coin guide; world coin grade");
        root.put(
            CommonFields.PRODUCER,
            "Acrobat Distiller 7.0.5 for Macintosh");
        root.put(
            CommonFields.SUBJECT,
            "world coin; coin; world coin listings;"
            + " world coin value; world coin guide; world coin "
            + "grade\nWorld Coins 1901-2000");
        root.put(
            CommonFields.TITLE,
            "2010 Standard Catalog of World Coins 1901-2000");
        root.put(
            CommonFields.DESCRIPTION,
            "World Coins 1901-2000");
        root.put(CommonFields.TOOL, PSCRIPT);
        root.put(CommonFields.CREATED, 1272458460L);
        root.put(CommonFields.MODIFIED, 1293228924L);
        root.put(CommonFields.PAGES, 2305L);
        String filename = "big.pdf";
        DiskHandlerTest.testJson(filename, json);
        root.put(CommonFields.TRUNCATED, 10000L);
        DiskHandlerTest.testJson(filename, json, "&limit=10000");
    }

    @Test
    public void testRehabPdf() throws Exception {
        Json json = new Json(
            APPLICATION_PDF,
            new Json.Contains(
                "Arthroscopic Anterior Stabilization "
                + "Rehab\nPhase I (0-3weeks)\n"),
            true,
            null,
            new Json.Headers(
                CONTENT_TYPE_META + APPLICATION_PDF + PDFBOX_META + '\n'
                + "xmpMM:DocumentID:uuid:05b73650-4e6d-4c18-a588-15036d40f694"
                + "\npdf:docinfo:created:2009-02-12T23:11:16Z"
                + "\npdf:docinfo:creator:Administrator"
                + "\npdf:docinfo:creator_tool:PScript5.dll Version "
                + "5.2.2"
                + "\npdf:docinfo:modified:2009-02-12T23:11:16Z"
                + "\npdf:docinfo:producer:GNU Ghostscript 7.06"
                + "\npdf:docinfo:title:Microsoft Word - Arthroscopic Anterior "
                + "Stabilization Rehab.doc"
                + pdfMeta(ONE_SIX)
                    .replace(
                        "pdf:hasMarkedContent:false\npdf:hasXMP:false",
                        "pdf:hasMarkedContent:true\npdf:hasXMP:true\n"
                        + "pdf:producer:GNU Ghostscript 7.06\n"
                        + "xmp:CreateDate:2009-02-12T15:11:16Z\n"
                        + "xmp:MetadataDate:2009-02-12T15:11:16Z\n"
                        + "xmp:ModifyDate:2009-02-12T15:11:16Z")));
        Map<String, Object> root = json.root();
        root.put(CommonFields.AUTHOR, "Administrator");
        root.put(CommonFields.PAGES, 8L);
        root.put(CommonFields.PRODUCER, "GNU Ghostscript 7.06");
        root.put(CommonFields.CREATED, 1234480276L);
        root.put(CommonFields.MODIFIED, 1234480276L);
        root.put(
            CommonFields.TITLE,
            "Microsoft Word - Arthroscopic Anterior Stabilization Rehab.doc");
        root.put(CommonFields.TOOL, PSCRIPT);
        DiskHandlerTest.testJson("rehab.pdf", json);
    }

    @Test
    public void testAccountPdf() throws Exception {
        Json json = new Json(
            APPLICATION_PDF,
            new Json.Contains("Дмитрий Александрович"),
            true,
            null,
            new Json.Headers(
                CONTENT_TYPE_META + APPLICATION_PDF + PDFBOX_META
                + "\npdf:docinfo:creator_tool:cairo 1.12.14 "
                + "(http://cairographics.org)"
                + "\npdf:docinfo:producer:cairo 1.12.14 ("
                + "http://cairographics.org)"
                + pdfMeta(ONE_FIVE)));
        Map<String, Object> root = json.root();
        root.put(CommonFields.PAGES, 1L);
        String cairo = "cairo 1.12.14 (http://cairographics.org)";
        root.put(CommonFields.PRODUCER, cairo);
        root.put(CommonFields.TOOL, cairo);
        DiskHandlerTest.testJson("account.pdf", json);
    }

    @Test
    public void testBradPdf() throws Exception {
        Json json = new Json(
            APPLICATION_PDF,
            new Json.AllOf(
                new Json.Contains("COMPENSATION REPORT"),
                new Json.Contains("BRADLEY")),
            true,
            null,
            new Json.Headers(
                CONTENT_TYPE_META + APPLICATION_PDF + PDFBOX_META
                + "\npdf:docinfo:creator_tool:Paychex MMS PDF Creator "
                + "v1.1.0"
                + pdfMeta(ONE_FOUR)));
        Map<String, Object> root = json.root();
        root.put(CommonFields.PAGES, 5L);
        root.put(CommonFields.TOOL, PDF_CREATOR);
        DiskHandlerTest.testJson("brad.pdf", json);
    }

    @Test
    public void testJohnPdf() throws Exception {
        Json json = new Json(
            APPLICATION_PDF,
            new Json.AllOf(
                new Json.Contains("PERIOD BEGIN"),
                new Json.Contains("JOHN")),
            true,
            null,
            new Json.Headers(
                CONTENT_TYPE_META + APPLICATION_PDF + PDFBOX_META
                + "\npdf:docinfo:creator_tool:Paychex MMS PDF Creator v1.1.0"
                + pdfMeta(ONE_FOUR)));
        Map<String, Object> root = json.root();
        root.put(CommonFields.PAGES, 2L);
        root.put(CommonFields.TOOL, PDF_CREATOR);
        DiskHandlerTest.testJson("john.pdf", json);
    }

    @Test
    public void testUnlimitedPdf() throws Exception {
        Json json = new Json(
            APPLICATION_PDF,
            new Json.StartsWith("Page 0\nPage 1\nPage 2\n"),
            true,
            null,
            new Json.Headers(
                CONTENT_TYPE_META + APPLICATION_PDF + PDFBOX_META
                + "\npdf:docinfo:created:2014-04-01T14:46:21Z"
                + "\npdf:docinfo:creator:Koma-Code"
                + "\npdf:docinfo:creator_tool:Koma-Code"
                + "\npdf:docinfo:modified:2014-06-27T12:04:54Z"
                + "\npdf:docinfo:producer:PowerPdf version "
                + "0.9"
                + pdfMeta(ONE_TWO)));
        Map<String, Object> root = json.root();
        String author = "Koma-Code";
        root.put(CommonFields.AUTHOR, author);
        root.put(CommonFields.TOOL, author);
        root.put(CommonFields.PRODUCER, POWER_PDF);
        root.put(CommonFields.PAGES, 28L);
        root.put(CommonFields.TRUNCATED, 1000L);
        root.put(CommonFields.CREATED, 1396363581L);
        root.put(CommonFields.MODIFIED, 1403870694L);
        DiskHandlerTest.testJson("unlimited.pdf", json, "&limit=1000");
    }
}
// CSON: MagicNumber

