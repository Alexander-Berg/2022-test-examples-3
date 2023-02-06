package ru.yandex.market.mboc.common.services.converter.validation;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

import javax.annotation.Resource;

import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mbo.excel.StreamExcelParser;
import ru.yandex.market.mbo.mdm.common.masterdata.services.SupplierConverterServiceMock;
import ru.yandex.market.mboc.app.proto.MasterDataServiceMock;
import ru.yandex.market.mboc.app.proto.SupplierDocumentServiceMock;
import ru.yandex.market.mboc.common.BaseIntegrationTestClass;
import ru.yandex.market.mboc.common.dict.MbocSupplierType;
import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.offers.model.OfferForService;
import ru.yandex.market.mboc.common.offers.repository.IMasterDataRepository;
import ru.yandex.market.mboc.common.services.business.BusinessSupplierHelper;
import ru.yandex.market.mboc.common.services.converter.ErrorAtLine;
import ru.yandex.market.mboc.common.services.excel.template.ExcelGeneratorTemplateCacheDownloader;
import ru.yandex.market.mboc.common.services.excel.template.ExcelTemplateGenerator;
import ru.yandex.market.mboc.common.services.excel.template.GeneratorContext;
import ru.yandex.market.mboc.common.services.excel.template.TemplateExcelColumnSpecs;
import ru.yandex.market.mboc.common.services.proto.MasterDataHelperService;
import ru.yandex.market.mboc.common.services.storage.StorageKeyValueServiceMock;
import ru.yandex.market.mboc.common.test.YamlTestUtil;

/**
 * @author s-ermakov
 */
public class IsTemplateFileExcelValidationTest extends BaseIntegrationTestClass {

    @Autowired
    private IsTemplateFileExcelValidation isTemplateFileExcelValidation;

    @Autowired
    private ExcelGeneratorTemplateCacheDownloader excelGeneratorTemplateCacheDownloader;

    private ExcelTemplateGenerator generator;

    @Autowired
    IMasterDataRepository masterDataFor1pRepository;

    @Resource
    private TemplateExcelColumnSpecs excelColumnSpecs;

    private MasterDataServiceMock masterDataServiceMock;
    private SupplierDocumentServiceMock supplierDocumentServiceMock;
    private MasterDataHelperService masterDataHelperService;

    private List<OfferForService> offers;

    @Before
    public void setUp() {
        masterDataServiceMock = new MasterDataServiceMock();
        supplierDocumentServiceMock = new SupplierDocumentServiceMock(masterDataServiceMock);
        var storageKeyValueService = new StorageKeyValueServiceMock();
        masterDataHelperService = new MasterDataHelperService(masterDataServiceMock, supplierDocumentServiceMock,
            new SupplierConverterServiceMock(), storageKeyValueService);

        generator = new ExcelTemplateGenerator(
            excelGeneratorTemplateCacheDownloader,
            masterDataHelperService,
            excelColumnSpecs,
            masterDataFor1pRepository
        );

        var baseOffers = YamlTestUtil.readOffersFromResources("offers/offers-for-export.yml");
        //offers for template fetched from db
        baseOffers.forEach(Offer::markLoadedContent);
        offers = BusinessSupplierHelper.getAllOffersForService(baseOffers);
    }

    @Test
    public void downloadTemplateAndValidateIt() throws FileNotFoundException {
        File templateFile1P = excelGeneratorTemplateCacheDownloader.getTemplateFile(MbocSupplierType.REAL_SUPPLIER);
        File templateFile3P = excelGeneratorTemplateCacheDownloader.getTemplateFile(MbocSupplierType.THIRD_PARTY);

        Assertions.assertThat(validateTemplate(templateFile1P)).isEmpty();
        Assertions.assertThat(validateTemplate(templateFile3P)).isEmpty();
    }

    @Test
    public void downloadTemplateEnrichAndValidateIt() throws IOException {
        generator.generateExcelByTemplate(offers, new GeneratorContext(), MbocSupplierType.REAL_SUPPLIER, path -> {
            try {
                Assertions.assertThat(validateTemplate(path.toFile())).isEmpty();
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
        });
        generator.generateExcelByTemplate(offers, new GeneratorContext(), MbocSupplierType.THIRD_PARTY, path -> {
            try {
                Assertions.assertThat(validateTemplate(path.toFile())).isEmpty();
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private List<ErrorAtLine> validateTemplate(File templateFile) throws FileNotFoundException {
        List<StreamExcelParser.Sheet> sheets = StreamExcelParser.parse(new FileInputStream(templateFile));
        return isTemplateFileExcelValidation.validate(sheets);
    }

    @Test
    public void simpleXlsWontPassValidation() {
        List<String> simpleFiles = Arrays.asList(
            "excel/CorrectSampleWithoutSettingsSheet.xls", "excel/DescriptionSample.xls",
            "excel/DuplicateHeader.xls", "excel/Karavan_otborka_2404.xlsx");

        SoftAssertions.assertSoftly(softAssertions -> {
            for (String resourceFile : simpleFiles) {
                InputStream resource = readResource(resourceFile);
                List<StreamExcelParser.Sheet> sheets = StreamExcelParser.parse(resource);
                List<ErrorAtLine> errors = isTemplateFileExcelValidation.validate(sheets);

                softAssertions.assertThat(errors)
                    .withFailMessage("Expected to have validation errors in file " + resourceFile)
                    .isNotEmpty();
            }
        });
    }

    private InputStream readResource(String fileName) {
        return getClass().getClassLoader().getResourceAsStream(fileName);
    }
}
