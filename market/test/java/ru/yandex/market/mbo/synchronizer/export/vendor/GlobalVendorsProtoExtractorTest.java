package ru.yandex.market.mbo.synchronizer.export.vendor;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import ru.yandex.market.mbo.common.model.Language;
import ru.yandex.market.mbo.db.vendor.GlobalVendorService;
import ru.yandex.market.mbo.export.MboParameters;
import ru.yandex.market.mbo.export.vendor.GlobalVendorsSeoHelper;
import ru.yandex.market.mbo.gwt.models.vendor.GlobalVendor;
import ru.yandex.market.mbo.gwt.models.vendor.LinkedLocalVendor;
import ru.yandex.market.mbo.gwt.models.vendor.LogoPosition;
import ru.yandex.market.mbo.gwt.models.visual.Word;
import ru.yandex.market.mbo.http.MboVendors;
import ru.yandex.market.mbo.synchronizer.export.BaseExtractor;
import ru.yandex.market.mbo.synchronizer.export.ExtractorBaseTestClass;
import ru.yandex.market.mbo.synchronizer.export.ExtractorWriterService;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author ayratgdl
 * @date 18.04.18
 */
public class GlobalVendorsProtoExtractorTest extends ExtractorBaseTestClass {
    private static final int MAGIC_PREFIX_LENGTH = 4;
    private static final long VENDOR_ID = 1;
    private static final long LINE_ID_1 = 201;
    private static final long LOCAL_VENDOR_1 = 42;
    private static final long LOCAL_VENDOR_2 = 4242;
    private static final long CATEGORY_ID = 71;

    private List<GlobalVendor> vendors = new ArrayList<>();
    private Map<Long, List<GlobalVendor.Line>> vendorToLines = new HashMap<>();
    private Map<Long, List<LinkedLocalVendor>> vendorToLocalVendors = new HashMap<>();

    @Override
    protected BaseExtractor createExtractor() {
        GlobalVendorService vendorService = Mockito.mock(GlobalVendorService.class);
        Mockito.when(vendorService.loadAllVendors()).thenReturn(vendors);
        Mockito.when(vendorService.loadAllVendorLines()).thenReturn(vendorToLines);
        Mockito.when(vendorService.loadAllLinkedLocalVendors()).thenReturn(vendorToLocalVendors);

        GlobalVendorsExtractor vendorsExtractor = new GlobalVendorsExtractor(vendorService, null);
        vendorsExtractor.setOutputFileName("global-vendors.pb");
        ExtractorWriterService extractorWriterService = new ExtractorWriterService();
        vendorsExtractor.setExtractorWriterService(extractorWriterService);
        return vendorsExtractor;
    }

    @Test
    public void extractVendorWithLines() throws Exception {
        GlobalVendor vendor = new GlobalVendor();
        vendor.setPublished(true);
        vendor.setId(VENDOR_ID);
        vendor.setLogoPosition(LogoPosition.RIGHT);
        vendor.setNames(
            Collections.singletonList(new Word(Word.EMPTY_ID, Language.RUSSIAN.getId(), "BigCompany", true)));
        addVendor(vendor);
        addVendorLine(VENDOR_ID, LINE_ID_1, "Line 1");

        extractor.perform("");

        MboVendors.GlobalVendor expectedVendor = MboVendors.GlobalVendor.newBuilder()
            .setPublished(true)
            .setId(VENDOR_ID)
            .setSite("")
            .setPictureUrl("")
            .setComment("")
            .setLogoPosition(MboVendors.Position.RIGHT)
            .addName(MboParameters.Word.newBuilder()
                         .setLangId(Language.RUSSIAN.getId())
                         .setName("BigCompany")
                         .setMorphologicalProcessing(true)
            )
            .addVendorLine(MboVendors.VendorLine.newBuilder().setId(LINE_ID_1).setName("Line 1"))
            .setSeoTitle(GlobalVendorsSeoHelper.buildDefaultSeoTitle("BigCompany"))
            .setSeoDescription(GlobalVendorsSeoHelper.buildDefaultSeoDescription("BigCompany"))
            .build();
        Assert.assertEquals(Collections.singletonList(expectedVendor), readVendors());
    }

    @Test
    public void extractVendorWithLocalVendor() throws Exception {
        GlobalVendor vendor = new GlobalVendor();
        vendor.setPublished(true);
        vendor.setId(VENDOR_ID);
        vendor.setLogoPosition(LogoPosition.RIGHT);
        vendor.setNames(
            Collections.singletonList(new Word(Word.EMPTY_ID, Language.RUSSIAN.getId(), "BigCompany", true)));
        addVendor(vendor);
        addLocalVendor(VENDOR_ID, LOCAL_VENDOR_1, CATEGORY_ID, true);
        addLocalVendor(VENDOR_ID, LOCAL_VENDOR_2, CATEGORY_ID, false);

        extractor.perform("");

        MboVendors.GlobalVendor expectedVendor = MboVendors.GlobalVendor.newBuilder()
            .setPublished(true)
            .setId(VENDOR_ID)
            .setSite("")
            .setPictureUrl("")
            .setComment("")
            .setLogoPosition(MboVendors.Position.RIGHT)
            .addName(MboParameters.Word.newBuilder()
                .setLangId(Language.RUSSIAN.getId())
                .setName("BigCompany")
                .setMorphologicalProcessing(true)
            )
            .addLinkedLocalVendor(
                MboVendors.LinkedLocalVendor.newBuilder()
                    .setId(LOCAL_VENDOR_1)
                    .setCategoryId(CATEGORY_ID)
                    .setType(MboVendors.LinkedLocalVendor.LocalVendorType.GURU))
            .addLinkedLocalVendor(
                MboVendors.LinkedLocalVendor.newBuilder()
                    .setId(LOCAL_VENDOR_2)
                    .setCategoryId(CATEGORY_ID)
                    .setType(MboVendors.LinkedLocalVendor.LocalVendorType.VISUAL))
            .setSeoTitle(GlobalVendorsSeoHelper.buildDefaultSeoTitle("BigCompany"))
            .setSeoDescription(GlobalVendorsSeoHelper.buildDefaultSeoDescription("BigCompany"))
            .build();
        Assert.assertEquals(Collections.singletonList(expectedVendor), readVendors());
    }

    @Test
    public void extractVendorWithoutLinesOrLocalVendor() throws Exception {
        GlobalVendor vendor = new GlobalVendor();
        vendor.setPublished(true);
        vendor.setId(VENDOR_ID);
        vendor.setLogoPosition(LogoPosition.RIGHT);
        vendor.setNames(
            Collections.singletonList(new Word(Word.EMPTY_ID, Language.RUSSIAN.getId(), "BigCompany", true)));
        addVendor(vendor);

        extractor.perform("");

        MboVendors.GlobalVendor expectedVendor = MboVendors.GlobalVendor.newBuilder()
            .setPublished(true)
            .setId(VENDOR_ID)
            .setSite("")
            .setPictureUrl("")
            .setComment("")
            .setLogoPosition(MboVendors.Position.RIGHT)
            .addName(MboParameters.Word.newBuilder()
                         .setLangId(Language.RUSSIAN.getId())
                         .setName("BigCompany")
                         .setMorphologicalProcessing(true)
            )
            .setSeoTitle(GlobalVendorsSeoHelper.buildDefaultSeoTitle("BigCompany"))
            .setSeoDescription(GlobalVendorsSeoHelper.buildDefaultSeoDescription("BigCompany"))
            .build();
        Assert.assertEquals(Collections.singletonList(expectedVendor), readVendors());
    }

    @Test
    public void extractVendorWithSeoTitleAndSeoDescription() throws Exception {
        GlobalVendor vendor = new GlobalVendor();
        vendor.setPublished(true);
        vendor.setId(VENDOR_ID);
        vendor.setSeoTitle("seo title");
        vendor.setSeoDescription("seo description");
        vendor.setNames(
            Collections.singletonList(new Word(Word.EMPTY_ID, Language.RUSSIAN.getId(), "BigCompany", true)));
        addVendor(vendor);

        extractor.perform("");

        MboVendors.GlobalVendor expectedVendor = MboVendors.GlobalVendor.newBuilder()
            .setPublished(true)
            .setId(VENDOR_ID)
            .setSite("")
            .setPictureUrl("")
            .setComment("")
            .setLogoPosition(MboVendors.Position.RIGHT)
            .addName(MboParameters.Word.newBuilder()
                         .setLangId(Language.RUSSIAN.getId())
                         .setName("BigCompany")
                         .setMorphologicalProcessing(true)
            )
            .setSeoTitle("seo title")
            .setSeoDescription("seo description")
            .build();
        Assert.assertEquals(Collections.singletonList(expectedVendor), readVendors());
    }

    private List<MboVendors.GlobalVendor> readVendors() throws IOException {
        List<MboVendors.GlobalVendor> result = new ArrayList<>();
        try (InputStream input = new ByteArrayInputStream(getExtractContent())) {
            input.skip(MAGIC_PREFIX_LENGTH);
            MboVendors.GlobalVendor vendor;
            while ((vendor = MboVendors.GlobalVendor.parseDelimitedFrom(input)) != null) {
                result.add(vendor);
            }

            return result;
        }
    }

    private void addVendor(GlobalVendor vendor) {
        vendors.add(vendor);
    }

    private void addVendorLine(long vendorId, long lineId, String lineName) {
        GlobalVendor.Line line = new GlobalVendor.Line(lineId, lineName);
        vendorToLines.computeIfAbsent(vendorId, key -> new ArrayList<>()).add(line);
    }

    private void addLocalVendor(long vendorId, long localVendorId, long categoryId, boolean guru) {
        LinkedLocalVendor localVendor = new LinkedLocalVendor(localVendorId, categoryId, "");
        localVendor.setGuru(guru);
        vendorToLocalVendors.computeIfAbsent(vendorId, key -> new ArrayList<>()).add(localVendor);
    }
}
