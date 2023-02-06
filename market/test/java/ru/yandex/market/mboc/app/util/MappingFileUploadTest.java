package ru.yandex.market.mboc.app.util;

import java.io.InputStream;
import java.util.List;
import java.util.stream.Collectors;

import lombok.SneakyThrows;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;

import ru.yandex.market.BaseMbocAppTest;
import ru.yandex.market.mboc.app.controller.FileUploadController;
import ru.yandex.market.mboc.app.mapping.MappingExcelParser;
import ru.yandex.market.mboc.app.mapping.MappingFileSavingService;
import ru.yandex.market.mboc.app.mapping.MappingFileSavingServiceImpl;
import ru.yandex.market.mboc.app.mapping.MappingFileUploadException;
import ru.yandex.market.mboc.app.mapping.MappingTsvParser;
import ru.yandex.market.mboc.common.dict.SupplierRepository;
import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.offers.repository.OfferRepository;
import ru.yandex.market.mboc.common.services.mapping_import.MappingFileRepository;
import ru.yandex.market.mboc.common.services.mapping_import.MappingFileRow;
import ru.yandex.market.mboc.common.services.mapping_import.MappingFileRowRepository;
import ru.yandex.market.mboc.common.services.modelstorage.ModelStorageCachingServiceMock;
import ru.yandex.market.mboc.common.services.modelstorage.models.Model;
import ru.yandex.market.mboc.common.utils.OfferTestUtils;

public class MappingFileUploadTest extends BaseMbocAppTest {
    @Autowired
    private OfferRepository offerRepository;
    @Autowired
    private SupplierRepository supplierRepository;
    @Autowired
    private MappingFileRepository fileRepo;
    @Autowired
    private MappingFileRowRepository fileRowRepo;

    private ModelStorageCachingServiceMock modelCache = new ModelStorageCachingServiceMock();

    private MappingFileSavingService savingService;
    private FileUploadController controller;
    private MappingExcelParser excelParser;
    private MappingTsvParser tsvParser;

    @Before
    public void setup() {
        savingService = new MappingFileSavingServiceImpl(fileRepo, fileRowRepo);
        excelParser = new MappingExcelParser();
        tsvParser = new MappingTsvParser();
        controller = new FileUploadController(null,
            savingService, excelParser, tsvParser, null);

        fileRepo.deleteAll();
        fileRowRepo.deleteAll();

        supplierRepository.deleteAll();
        var supplier1 = OfferTestUtils.simpleSupplier().setId(1019355);
        var supplier2 = OfferTestUtils.simpleSupplier().setId(1019351);
        supplierRepository.insertBatch(supplier1, supplier2);

        offerRepository.deleteAllInTest();
        offerRepository.insertOffers(createOffer(), createInvalidOffer());
    }

    @Test(expected = MappingFileUploadException.class)
    public void parseXlsxTest() {
        InputStream is = getClass().getClassLoader().getResourceAsStream("xslxForMappingTest.xlsx");
        excelParser.parse(is);
    }

    @Test
    public void parseXlsxBatchTest() {
        InputStream is = getClass().getClassLoader().getResourceAsStream("xslxForMappingBatchTest.xlsx");
        var results = excelParser.parse(is);
        Assert.assertEquals(5, results.size());
    }

    @Test
    public void savingMappingFileTest() {
        var name = "xslxForMappingBatchTest.xlsx";
        controller.saveExcelFile(getResourceFile(name));

        var all = fileRepo.findAll();
        Assert.assertEquals(1, all.size());
        var mappingFile = all.get(0);
        Assert.assertEquals(5, mappingFile.getSize());
        Assert.assertEquals(name, mappingFile.getFilename());

        var rowsOf = fileRowRepo.findRowsToProcess(all);
        Assert.assertEquals(5, rowsOf.size());
        var count = rowsOf.stream().mapToLong(MappingFileRow::getMskuId).distinct().count();
        Assert.assertEquals(3, count);
        var firstRow = rowsOf.get(0);
        Assert.assertEquals(5144704, firstRow.getMskuId());
        Assert.assertTrue(firstRow.getSsku().startsWith("66974"));
    }

    @Test
    public void savingMappingFileWithRewriteTest() {
        var name = "xslxForMappingBatchTest_rewrite.xlsx";
        controller.saveExcelFile(getResourceFile(name));

        var all = fileRepo.findAll();
        Assert.assertEquals(1, all.size());
        var mappingFile = all.get(0);
        Assert.assertEquals(5, mappingFile.getSize());
        Assert.assertEquals(name, mappingFile.getFilename());

        var rowsOf = fileRowRepo.findRowsToProcess(all);
        Assert.assertEquals(5, rowsOf.size());
        var count = rowsOf.stream().mapToLong(MappingFileRow::getMskuId).distinct().count();
        Assert.assertEquals(3, count);
        var firstRow = rowsOf.get(0);
        Assert.assertEquals(5144704, firstRow.getMskuId());
        Assert.assertTrue(firstRow.getSsku().startsWith("66974"));
        var actualRewrites = rowsOf.stream().map(MappingFileRow::isRewrite).collect(Collectors.toList());
        Assert.assertEquals(List.of(true, false, true, true, false), actualRewrites);
    }

    @Test
    public void savingTsvFile() {
        var name = "mappingWithOutRewrite.tsv";
        controller.uploadTsvMapping(getResourceFile(name));

        var allFiles = fileRepo.findAll();
        Assert.assertEquals(1, allFiles.size());
        var mappingFile = allFiles.get(0);
        Assert.assertEquals(5, mappingFile.getSize());
        Assert.assertEquals(name, mappingFile.getFilename());

        var rows = fileRowRepo.findRowsToProcess(mappingFile);
        Assert.assertEquals(5, rows.size());
        for (MappingFileRow row : rows) {
            Assert.assertFalse("no rewrite for " + row, row.isRewrite());
        }
        var firstRow = rows.get(0);
        Assert.assertEquals(5144704, firstRow.getMskuId());
    }

    @SneakyThrows
    private MockMultipartFile getResourceFile(String name) {
        InputStream is = getClass().getClassLoader().getResourceAsStream(name);
        Assert.assertNotNull(is);
        return new MockMultipartFile(name, name, "", is);
    }

    @Test
    public void savingTsvFileWithRewrites() {
        var name = "mappingWithRewrite.tsv";
        controller.uploadTsvMapping(getResourceFile(name));

        var all = fileRepo.findAll();
        Assert.assertEquals(1, all.size());
        var mappingFile = all.get(0);
        Assert.assertEquals(5, mappingFile.getSize());
        Assert.assertEquals(name, mappingFile.getFilename());

        var rowsOf = fileRowRepo.findRowsToProcess(all);
        Assert.assertEquals(5, rowsOf.size());
        var count = rowsOf.stream().mapToLong(MappingFileRow::getMskuId).distinct().count();
        Assert.assertEquals(3, count);
        var firstRow = rowsOf.get(0);
        Assert.assertEquals(5144704, firstRow.getMskuId());
        Assert.assertTrue(firstRow.getSsku().startsWith("66974"));
        var actualRewrites = rowsOf.stream().map(MappingFileRow::isRewrite).collect(Collectors.toList());
        Assert.assertEquals(List.of(true, false, true, true, false), actualRewrites);
    }

    @Test(expected = MappingFileUploadException.class)
    public void failOnEmptyShopSKU() {
        var name = "mappingWithOutRewrite_fail_due_to_empty_ssku.tsv";
        controller.uploadTsvMapping(getResourceFile(name));
    }

    @Test(expected = MappingFileUploadException.class)
    public void failOnMissingValue() {
        var name = "mappingWithOutRewrite_fail_due_to_missing_value.tsv";
        controller.uploadTsvMapping(getResourceFile(name));
    }

    @Test(expected = MappingFileUploadException.class)
    public void failOnStringInNumberField() {
        var name = "mappingWithOutRewrite_fail_due_to_string_in_number.tsv";
        controller.uploadTsvMapping(getResourceFile(name));
    }

    @Test(expected = MappingFileUploadException.class)
    public void failOnMissingRewrite() {
        var name = "mappingWithRewrite_fail_due_to_missing_rewrite.tsv";
        controller.uploadTsvMapping(getResourceFile(name));
    }

    @Test(expected = MappingFileUploadException.class)
    public void failOnStringInNumberFieldForRewriteFileType() {
        var name = "mappingWithRewrite_fail_due_to_string_in_number.tsv";
        controller.uploadTsvMapping(getResourceFile(name));
    }

    @Test(expected = MappingFileUploadException.class)
    public void failOnIncorrectRewrite() {
        var name = "mappingWithRewrite_fail_due_to_incorrect_rewrite.tsv";
        controller.uploadTsvMapping(getResourceFile(name));
    }


    private Offer createOffer() {
        Offer offer = new Offer();
        offer.setId(1L);
        offer.setBusinessId(1019355);
        offer.setShopSku("669744");
        offer.setAcceptanceStatusInternal(Offer.AcceptanceStatus.OK);
        offer.setTitle("title");
        offer.setShopCategoryName("cat");
        return offer;
    }

    private Offer createInvalidOffer() {
        Offer offer = new Offer();
        offer.setId(2L);
        offer.setBusinessId(1019351);
        offer.setShopSku("669741");
        offer.setAcceptanceStatusInternal(Offer.AcceptanceStatus.TRASH);
        offer.setTitle("title");
        offer.setShopCategoryName("cat");
        return offer;
    }

    private Model createModel() {
        long mId = 5144704L;
        Model model = new Model();
        model.setDeleted(false);
        model.setPublishedOnMarket(true);
        model.setId(mId);
        model.setCategoryId(1);
        modelCache.addModel(model);
        return model;
    }
}
