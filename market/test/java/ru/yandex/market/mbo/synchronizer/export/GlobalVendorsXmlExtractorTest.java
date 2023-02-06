package ru.yandex.market.mbo.synchronizer.export;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.mbo.common.model.Language;
import ru.yandex.market.mbo.core.guruexport.XmlDataGeneratorImpl;
import ru.yandex.market.mbo.core.kdepot.saver.GlobalVendorsRenderer;
import ru.yandex.market.mbo.core.kdepot.saver.XmlDataGenerator;
import ru.yandex.market.mbo.db.params.IParameterLoaderService;
import ru.yandex.market.mbo.db.vendor.GlobalVendorDBInterface;
import ru.yandex.market.mbo.db.vendor.GlobalVendorDBMock;
import ru.yandex.market.mbo.db.vendor.GlobalVendorLoaderService;
import ru.yandex.market.mbo.db.vendor.GlobalVendorService;
import ru.yandex.market.mbo.export.vendor.GlobalVendorsSeoHelper;
import ru.yandex.market.mbo.gwt.models.params.Option;
import ru.yandex.market.mbo.gwt.models.params.OptionImpl;
import ru.yandex.market.mbo.gwt.models.vendor.GlobalVendor;
import ru.yandex.market.mbo.gwt.models.vendor.LinkedLocalVendor;
import ru.yandex.market.mbo.gwt.models.vendor.Logo;
import ru.yandex.market.mbo.gwt.models.vendor.LogoPosition;
import ru.yandex.market.mbo.gwt.models.vendor.LogoType;
import ru.yandex.market.mbo.gwt.models.visual.Word;

/**
 * @author ayratgdl
 * @date 29.03.18
 */
public class GlobalVendorsXmlExtractorTest extends XmlSingleFileExtractorTest {
    private static final String XML_SCHEMA_PATH =
        "/ru/yandex/market/mbo/synchronizer/export/xsd/global.vendors.xsd";
    private static final Integer FOUNDATION_YEAR = 1986;
    private static final long LINE_ID_1 = 201;
    private static final long UID = 1;
    private static final long LOCAL_VENDOR_1 = 42;
    private static final long LOCAL_VENDOR_2 = 4242;
    private static final long CATEGORY_ID = 71;

    private Map<Long, List<OptionImpl>> vendorToLines = new HashMap<>();
    private Map<Long, List<LinkedLocalVendor>> vendorToLocalVendors = new HashMap<>();
    private GlobalVendorDBInterface vendorDB;

    @Override
    protected XmlDataGenerator createDataGenerator() {
        vendorDB = new GlobalVendorDBMock();
        IParameterLoaderService parameterLoaderService = Mockito.mock(IParameterLoaderService.class);
        Mockito.when(parameterLoaderService.getLines(null)).thenReturn(vendorToLines);

        GlobalVendorLoaderService globalVendorLoaderService = new GlobalVendorLoaderService();
        GlobalVendorService vendorService = Mockito.spy(new GlobalVendorService());
        globalVendorLoaderService.setVendorDb(vendorDB);
        globalVendorLoaderService.setParameterLoaderService(parameterLoaderService);
        vendorService.setVendorDb(vendorDB);
        vendorService.setParameterLoaderService(parameterLoaderService);
        vendorService.setGlobalVendorLoaderService(globalVendorLoaderService);
        Mockito.doReturn(vendorToLocalVendors).when(vendorService).loadAllLinkedLocalVendors();

        GlobalVendorsRenderer renderer = new GlobalVendorsRenderer(vendorService);

        return new XmlDataGeneratorImpl(renderer);
    }

    @Override
    protected String getSchemaPath() {
        return XML_SCHEMA_PATH;
    }

    @Test
    public void renderWithoutVendors() throws Exception {
        extractor.perform("");

        String expectedXml = "<?xml version=\"1.0\" encoding=\"utf-8\"?><global-vendors/>";
        Assert.assertEquals(expectedXml, getResultXml());
    }

    @Test
    public void renderWithoutPublishedVendors() throws Exception {
        GlobalVendor vendor = new GlobalVendor();
        vendor.setPublished(false);
        vendor.setId(1);
        vendorDB.createVendor(vendor, UID);

        extractor.perform("");

        String expectedXml = "<?xml version=\"1.0\" encoding=\"utf-8\"?><global-vendors/>";
        Assert.assertEquals(expectedXml, getResultXml());
    }

    @Test
    public void renderEmptyGlobalVendor() throws Exception {
        GlobalVendor vendor = new GlobalVendor();
        vendor.setPublished(true);
        vendor.setNames(Collections.singletonList(new Word(Language.RUSSIAN.getId(), "BigCompany")));
        long vendorId = vendorDB.createVendor(vendor, UID);

        extractor.perform("");

        String expectedXml = "<?xml version=\"1.0\" encoding=\"utf-8\"?><global-vendors>\n" +
            "<vendor id=\"" + vendorId + "\" name=\"BigCompany\">\n" +
            "<logo-position>3</logo-position>\n" +
            "<is-fake-vendor>false</is-fake-vendor>\n" +
            "<vendor-lines/>\n" +
            "<linked-local-vendors/>\n" +
            "<seo-title>" + GlobalVendorsSeoHelper.buildDefaultSeoTitle("BigCompany") + "</seo-title>\n" +
            "<seo-description>" + GlobalVendorsSeoHelper.buildDefaultSeoDescription("BigCompany") +
            "</seo-description>\n" +
            "<is-require-gtin-barcodes>false</is-require-gtin-barcodes>\n" +
            "</vendor>\n" +
            "</global-vendors>";
        Assert.assertEquals(expectedXml, getResultXml());
    }

    @Test
    public void renderFullGlobalVendor() throws Exception {
        GlobalVendor vendor = new GlobalVendor();
        vendor.setPublished(true);
        vendor.setNames(Collections.singletonList(new Word(Language.RUSSIAN.getId(), "BigCompany")));
        vendor.setDescription("Very interesting company");
        vendor.setSite("company.example.com");
        vendor.setPictureUrl("picture.example.com/company.png");
        vendor.setShortDescription("Short description");
        vendor.setCountry("Russian");
        vendor.setLogoPosition(LogoPosition.RIGHT);
        Logo logo = new Logo();
        logo.setUrl("picture.example.com/logo.png");
        logo.setType(LogoType.BRAND_ZONE);
        vendor.setLogos(Collections.singletonList(logo));
        vendor.setDescrSrcUrlText("text");
        vendor.setDescrSrcUrlHref("href");
        vendor.setRecommendedShopsRulesUrl("company.example.com/shops");
        vendor.setFoundationYear(FOUNDATION_YEAR);
        vendor.setRequireGtinBarcodes(true);
        long vendorId = vendorDB.createVendor(vendor, UID);

        extractor.perform("");

        String expectedXml = "<?xml version=\"1.0\" encoding=\"utf-8\"?><global-vendors>\n" +
            "<vendor id=\"" + vendorId + "\" name=\"BigCompany\">\n" +
            "<description>Very interesting company</description>\n" +
            "<site>company.example.com</site>\n" +
            "<picture>picture.example.com/company.png</picture>\n" +
            "<short-description>Short description</short-description>\n" +
            "<country>Russian</country>\n" +
            "<logo-position>3</logo-position>\n" +
            "<logos>\n" +
            "<logo type=\"BRANDZONE\" show_in_suggest=\"false\">picture.example.com/logo.png</logo>\n" +
            "</logos>\n" +
            "<descr-src-url-text>text</descr-src-url-text>\n" +
            "<descr-src-url-href>href</descr-src-url-href>\n" +
            "<rec-shop-url>company.example.com/shops</rec-shop-url>\n" +
            "<is-fake-vendor>false</is-fake-vendor>\n" +
            "<foundation-year>1986</foundation-year>\n" +
            "<vendor-lines/>\n" +
            "<linked-local-vendors/>\n" +
            "<seo-title>" + GlobalVendorsSeoHelper.buildDefaultSeoTitle("BigCompany") + "</seo-title>\n" +
            "<seo-description>" + GlobalVendorsSeoHelper.buildDefaultSeoDescription("BigCompany") +
            "</seo-description>\n" +
            "<is-require-gtin-barcodes>true</is-require-gtin-barcodes>\n" +
            "</vendor>\n" +
            "</global-vendors>";
        Assert.assertEquals(expectedXml, getResultXml());
    }

    @Test(expected = RuntimeException.class)
    public void failWhenAbsentNameOnNotValidXml() throws Exception {
        GlobalVendor vendor = new GlobalVendor();
        vendor.setPublished(true);
        vendorDB.createVendor(vendor, UID);

        extractor.perform("");
    }

    @Test
    public void renderVendorWithLines() throws Exception {
        GlobalVendor vendor = new GlobalVendor();
        vendor.setPublished(true);
        vendor.setNames(Collections.singletonList(new Word(Language.RUSSIAN.getId(), "BigCompany")));
        long vendorId = vendorDB.createVendor(vendor, UID);
        addVendorLine(1, LINE_ID_1, "Line 1");

        extractor.perform("");

        String expectedXml = "<?xml version=\"1.0\" encoding=\"utf-8\"?><global-vendors>\n" +
            "<vendor id=\"" + vendorId + "\" name=\"BigCompany\">\n" +
            "<logo-position>3</logo-position>\n" +
            "<is-fake-vendor>false</is-fake-vendor>\n" +
            "<vendor-lines>\n" +
            "<vendor-line id=\"201\" name=\"Line 1\"/>\n" +
            "</vendor-lines>\n" +
            "<linked-local-vendors/>\n" +
            "<seo-title>" + GlobalVendorsSeoHelper.buildDefaultSeoTitle("BigCompany") + "</seo-title>\n" +
            "<seo-description>" + GlobalVendorsSeoHelper.buildDefaultSeoDescription("BigCompany") +
            "</seo-description>\n" +
            "<is-require-gtin-barcodes>false</is-require-gtin-barcodes>\n" +
            "</vendor>\n" +
            "</global-vendors>";
        Assert.assertEquals(expectedXml, getResultXml());
    }

    @Test
    public void renderVendorWithLocalVendors() throws Exception {
        GlobalVendor vendor = new GlobalVendor();
        vendor.setPublished(true);
        vendor.setNames(Collections.singletonList(new Word(Language.RUSSIAN.getId(), "BigCompany")));
        long vendorId = vendorDB.createVendor(vendor, UID);
        addLocalVendor(vendorId, LOCAL_VENDOR_1, CATEGORY_ID, true);
        addLocalVendor(vendorId, LOCAL_VENDOR_2, CATEGORY_ID, false);

        extractor.perform("");

        String expectedXml = "<?xml version=\"1.0\" encoding=\"utf-8\"?><global-vendors>\n" +
            "<vendor id=\"" + vendorId + "\" name=\"BigCompany\">\n" +
            "<logo-position>3</logo-position>\n" +
            "<is-fake-vendor>false</is-fake-vendor>\n" +
            "<vendor-lines/>\n" +
            "<linked-local-vendors>\n" +
            "<linked-local-vendor id=\"42\" category_id=\"71\" type=\"GURU\"/>\n" +
            "<linked-local-vendor id=\"4242\" category_id=\"71\" type=\"VISUAL\"/>\n" +
            "</linked-local-vendors>\n" +
            "<seo-title>" + GlobalVendorsSeoHelper.buildDefaultSeoTitle("BigCompany") + "</seo-title>\n" +
            "<seo-description>" + GlobalVendorsSeoHelper.buildDefaultSeoDescription("BigCompany") +
            "</seo-description>\n" +
            "<is-require-gtin-barcodes>false</is-require-gtin-barcodes>\n" +
            "</vendor>\n" +
            "</global-vendors>";
        Assert.assertEquals(expectedXml, getResultXml());
    }

    @Test
    public void extractVendorWithSeoTitleAndSeoDescription() throws Exception {
        GlobalVendor vendor = new GlobalVendor();
        vendor.setPublished(true);
        vendor.setNames(Collections.singletonList(new Word(Language.RUSSIAN.getId(), "BigCompany")));
        vendor.setSeoTitle("seo title");
        vendor.setSeoDescription("seo description");
        long vendorId = vendorDB.createVendor(vendor, UID);

        extractor.perform("");

        String expectedXml = "<?xml version=\"1.0\" encoding=\"utf-8\"?><global-vendors>\n" +
            "<vendor id=\"" + vendorId + "\" name=\"BigCompany\">\n" +
            "<logo-position>3</logo-position>\n" +
            "<is-fake-vendor>false</is-fake-vendor>\n" +
            "<vendor-lines/>\n" +
            "<linked-local-vendors/>\n" +
            "<seo-title>seo title</seo-title>\n" +
            "<seo-description>seo description</seo-description>\n" +
            "<is-require-gtin-barcodes>false</is-require-gtin-barcodes>\n" +
            "</vendor>\n" +
            "</global-vendors>";
        Assert.assertEquals(expectedXml, getResultXml());
    }

    private void addVendorLine(long vendorId, long lineId, String lineName) {
        OptionImpl option = new OptionImpl(Option.OptionType.LINE);
        option.setId(lineId);
        option.addName(new Word(Language.RUSSIAN.getId(), lineName));
        vendorToLines.computeIfAbsent(vendorId, key -> new ArrayList<>()).add(option);
    }

    private void addLocalVendor(long vendorId, long localVendorId, long categoryId, boolean guru) {
        LinkedLocalVendor localVendor = new LinkedLocalVendor(localVendorId, categoryId, "");
        localVendor.setGuru(guru);
        vendorToLocalVendors.computeIfAbsent(vendorId, key -> new ArrayList<>()).add(localVendor);
    }
}
