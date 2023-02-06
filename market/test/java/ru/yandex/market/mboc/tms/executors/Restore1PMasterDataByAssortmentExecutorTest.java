package ru.yandex.market.mboc.tms.executors;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.mbo.excel.ExcelFile;
import ru.yandex.market.mbo.excel.ExcelFileConverter;
import ru.yandex.market.mbo.mdm.common.masterdata.services.SupplierConverterServiceMock;
import ru.yandex.market.mboc.app.proto.MasterDataServiceMock;
import ru.yandex.market.mboc.app.proto.SupplierDocumentServiceMock;
import ru.yandex.market.mboc.common.config.OffersToExcelFileConverterConfig;
import ru.yandex.market.mboc.common.dict.MbocSupplierType;
import ru.yandex.market.mboc.common.dict.Supplier;
import ru.yandex.market.mboc.common.dict.SupplierRepositoryMock;
import ru.yandex.market.mboc.common.masterdata.parsing.ImportedOfferToMasterDataConverter;
import ru.yandex.market.mboc.common.masterdata.parsing.MasterDataParsingConfig;
import ru.yandex.market.mboc.common.masterdata.services.category.MboTimeUnitAliasesService;
import ru.yandex.market.mboc.common.services.category.CategoryCachingServiceMock;
import ru.yandex.market.mboc.common.services.modelstorage.ModelStorageCachingServiceMock;
import ru.yandex.market.mboc.common.services.proto.MasterDataHelperService;
import ru.yandex.market.mboc.common.services.storage.StorageKeyValueServiceMock;
import ru.yandex.market.mboc.common.utils.DateTimeUtils;
import ru.yandex.market.mboc.tms.executors.Restore1PMasterDataByAssortmentExecutor.AssortmentFile;
import ru.yandex.market.mboc.tms.executors.Restore1PMasterDataByAssortmentExecutor.FileLink;
import ru.yandex.market.mdm.http.MasterDataProto;
import ru.yandex.market.mdm.http.MdmCommon;

@SuppressWarnings("checkstyle:MagicNumber")
public class Restore1PMasterDataByAssortmentExecutorTest {
    private static final int SUPPLIER_ID = 14020;
    private SupplierDocumentServiceMock supplierDocumentService;
    private MasterDataServiceMock masterDataService;
    private Restore1PMasterDataByAssortmentExecutor executor;
    private FileGrabberMock fileGrabber;
    private SupplierRepositoryMock supplierRepository;
    private CategoryCachingServiceMock categoryCachingService;

    @Before
    public void setup() {
        supplierRepository = new SupplierRepositoryMock();
        categoryCachingService = new CategoryCachingServiceMock();
        masterDataService = new MasterDataServiceMock();
        supplierDocumentService = new SupplierDocumentServiceMock(masterDataService);
        fileGrabber = new FileGrabberMock();
        var storageKeyValueService = new StorageKeyValueServiceMock();
        MasterDataHelperService helper = new MasterDataHelperService(masterDataService, supplierDocumentService,
            new SupplierConverterServiceMock(), storageKeyValueService);
        ImportedOfferToMasterDataConverter converter = new ImportedOfferToMasterDataConverter(
            new MasterDataParsingConfig(Mockito.mock(MboTimeUnitAliasesService.class), storageKeyValueService),
            helper
        );
        OffersToExcelFileConverterConfig offersToExcelFileConverterConfig =
            new OffersToExcelFileConverterConfig(categoryCachingService);

        executor = new Restore1PMasterDataByAssortmentExecutor(
            fileGrabber,
            converter,
            helper,
            offersToExcelFileConverterConfig.importedExcelFileConverter(new ModelStorageCachingServiceMock(),
                Mockito.mock(MboTimeUnitAliasesService.class), supplierRepository, storageKeyValueService)
        );
        supplierRepository.insert(new Supplier()
            .setId(SUPPLIER_ID)
            .setType(MbocSupplierType.REAL_SUPPLIER)
            .setRealSupplierId("0064")
        );
    }

    @Test
    public void whenNoFilesShouldDoNothing() {
        executor.execute();
        Assertions.assertThat(masterDataService.getSskuMasterData()).isEmpty();
    }

    @Test
    public void whenNewMasterDataShouldSaveIt() {
        ExcelFile file = loadFile("ABC_with_md.xlsm");
        fileGrabber.addFile(SUPPLIER_ID, file, DateTimeUtils.dateTimeNow());

        executor.execute();

        Assertions.assertThat(masterDataService.getSskuMasterData()).hasSize(3);
        Map<MdmCommon.ShopSkuKey, MdmCommon.SskuMasterData> protoMD = masterDataService.getSskuMasterData();

        // Все нижеследующие magic number-ы - значения из эксельки.
        MdmCommon.SskuMasterData a = protoMD.get(protoKey(SUPPLIER_ID, "A"));
        MdmCommon.SskuMasterData b = protoMD.get(protoKey(SUPPLIER_ID, "B"));
        MdmCommon.SskuMasterData c = protoMD.get(protoKey(SUPPLIER_ID, "C"));
        Assertions.assertThat(a.getManufacturerCountriesList()).containsExactlyInAnyOrder("Испания");
        Assertions.assertThat(b.getManufacturerCountriesList()).containsExactlyInAnyOrder("Китай");
        Assertions.assertThat(c.getManufacturerCountriesList()).containsExactlyInAnyOrder("Италия");
        Assertions.assertThat(a.getGtinsList()).containsExactlyInAnyOrder("309805987043");
        Assertions.assertThat(b.getGtinsList()).containsExactlyInAnyOrder("229805987044");
        Assertions.assertThat(c.getGtinsList()).containsExactlyInAnyOrder("730859870645");
        Assertions.assertThat(a.getShelfLifeComment())
            .isEqualTo("хранить любя.");
        Assertions.assertThat(b.getShelfLifeComment())
            .isEqualTo("хранить бережно.");
        Assertions.assertThat(c.getShelfLifeComment())
            .isEqualTo("хранить в самом лучшем, защищённом от негатива месте.");
        Assertions.assertThat(a.getTransportUnitSize()).isEqualTo(1);
        Assertions.assertThat(b.getTransportUnitSize()).isEqualTo(2);
        Assertions.assertThat(c.getTransportUnitSize()).isEqualTo(35);
        Assertions.assertThat(a.getMinShipment()).isEqualTo(16);
        Assertions.assertThat(b.getMinShipment()).isEqualTo(15);
        Assertions.assertThat(c.getMinShipment()).isEqualTo(1);
        Assertions.assertThat(a.getQuantumOfSupply()).isEqualTo(1);
        Assertions.assertThat(b.getQuantumOfSupply()).isEqualTo(1);
        Assertions.assertThat(c.getQuantumOfSupply()).isEqualTo(1);
    }

    @Test
    public void whenSskuInSeveralFilesShouldTakeLatest() {
        ExcelFile file1 = loadFile("ABC_with_md.xlsm");
        ExcelFile file2 = loadFile("C_with_md.xlsm");
        fileGrabber.addFile(SUPPLIER_ID, file1, DateTimeUtils.dateTimeNow());
        fileGrabber.addFile(SUPPLIER_ID, file2, DateTimeUtils.dateTimeNow().plusDays(10));

        executor.execute();

        Assertions.assertThat(masterDataService.getSskuMasterData()).hasSize(3);
        Map<MdmCommon.ShopSkuKey, MdmCommon.SskuMasterData> protoMD = masterDataService.getSskuMasterData();

        // Нас интересует только оффер "С", т.к. он встретился в двух эксельках.
        MdmCommon.SskuMasterData c = protoMD.get(protoKey(SUPPLIER_ID, "C"));
        Assertions.assertThat(c.getManufacturerCountriesList()).containsExactlyInAnyOrder("Португалия"); // новое
        Assertions.assertThat(c.getGtinsList()).containsExactlyInAnyOrder("730859870645");
        Assertions.assertThat(c.getShelfLifeComment())
            .isEqualTo("хранить в самом лучшем, защищённом от негатива месте.");
        Assertions.assertThat(c.getTransportUnitSize()).isEqualTo(77); // тоже из нового второго файла
        Assertions.assertThat(c.getMinShipment()).isEqualTo(1);
        Assertions.assertThat(c.getQuantumOfSupply()).isEqualTo(1);
    }

    @Test
    public void whenErrorsInFilesShouldSkipThem() {
        // Здесь проверяется, что если файл не парсится, то он скипается целиком от греха подальше. Валидность файла
        // определяется эксельными конфигами парсинга в КИ.
        ExcelFile file1 = loadFile("ABC_with_md.xlsm");
        ExcelFile file2 = loadFile("ABC_with_md_incorrect_AB.xlsm");
        fileGrabber.addFile(SUPPLIER_ID, file1, DateTimeUtils.dateTimeNow());
        fileGrabber.addFile(SUPPLIER_ID, file2, DateTimeUtils.dateTimeNow());

        executor.execute();

        Assertions.assertThat(masterDataService.getSskuMasterData()).hasSize(3);
        Map<MdmCommon.ShopSkuKey, MdmCommon.SskuMasterData> protoMD = masterDataService.getSskuMasterData();

        // Все нижеследующие magic number-ы - значения из первой эксельки. А вторая невалидная и проигнорилась, хотя
        // значения в ней отличаются.
        MdmCommon.SskuMasterData a = protoMD.get(protoKey(SUPPLIER_ID, "A"));
        MdmCommon.SskuMasterData b = protoMD.get(protoKey(SUPPLIER_ID, "B"));
        MdmCommon.SskuMasterData c = protoMD.get(protoKey(SUPPLIER_ID, "C"));
        Assertions.assertThat(a.getManufacturerCountriesList()).containsExactlyInAnyOrder("Испания");
        Assertions.assertThat(b.getManufacturerCountriesList()).containsExactlyInAnyOrder("Китай");
        Assertions.assertThat(c.getManufacturerCountriesList()).containsExactlyInAnyOrder("Италия");
        Assertions.assertThat(a.getGtinsList()).containsExactlyInAnyOrder("309805987043");
        Assertions.assertThat(b.getGtinsList()).containsExactlyInAnyOrder("229805987044");
        Assertions.assertThat(c.getGtinsList()).containsExactlyInAnyOrder("730859870645");
        Assertions.assertThat(a.getShelfLifeComment())
            .isEqualTo("хранить любя.");
        Assertions.assertThat(b.getShelfLifeComment())
            .isEqualTo("хранить бережно.");
        Assertions.assertThat(c.getShelfLifeComment())
            .isEqualTo("хранить в самом лучшем, защищённом от негатива месте.");
        Assertions.assertThat(a.getTransportUnitSize()).isEqualTo(1);
        Assertions.assertThat(b.getTransportUnitSize()).isEqualTo(2);
        Assertions.assertThat(c.getTransportUnitSize()).isEqualTo(35);
        Assertions.assertThat(a.getMinShipment()).isEqualTo(16);
        Assertions.assertThat(b.getMinShipment()).isEqualTo(15);
        Assertions.assertThat(c.getMinShipment()).isEqualTo(1);
        Assertions.assertThat(a.getQuantumOfSupply()).isEqualTo(1);
        Assertions.assertThat(b.getQuantumOfSupply()).isEqualTo(1);
        Assertions.assertThat(c.getQuantumOfSupply()).isEqualTo(1);
    }

    @Test
    public void whenErrorsInMasterDataShouldPickLatestValid() {
        // Здесь мы полагаем, что оба файла успешно парсятся КИ, однако само логическое содержимое мастер-данных
        // будет давать ошибки валидации. В этом случае мы для каждого SSKU-ключа постараемся взять максимально позднюю
        // версию МД из тех, что валидны.

        ExcelFile file1 = loadFile("ABC_with_md.xlsm");
        ExcelFile file2 = loadFile("ABC_with_md_parsable_incorrect_AB.xlsm");
        fileGrabber.addFile(SUPPLIER_ID, file1, DateTimeUtils.dateTimeNow());
        fileGrabber.addFile(SUPPLIER_ID, file2, DateTimeUtils.dateTimeNow());

        masterDataService.setSaveErrorsOnIteration(0, errors(protoKey(SUPPLIER_ID, "A"), protoKey(SUPPLIER_ID, "B")));
        executor.execute();

        Assertions.assertThat(masterDataService.getSskuMasterData()).hasSize(3);
        Map<MdmCommon.ShopSkuKey, MdmCommon.SskuMasterData> protoMD = masterDataService.getSskuMasterData();

        // Для "А" и "В" вторая экселька не пройдёт валидацию МДМ и для них рабочей станет версия первой эксельки.
        MdmCommon.SskuMasterData a = protoMD.get(protoKey(SUPPLIER_ID, "A"));
        MdmCommon.SskuMasterData b = protoMD.get(protoKey(SUPPLIER_ID, "B"));
        MdmCommon.SskuMasterData c = protoMD.get(protoKey(SUPPLIER_ID, "C"));
        Assertions.assertThat(a.getManufacturerCountriesList()).containsExactlyInAnyOrder("Испания");
        Assertions.assertThat(b.getManufacturerCountriesList()).containsExactlyInAnyOrder("Китай");
        Assertions.assertThat(c.getManufacturerCountriesList()).containsExactlyInAnyOrder("Кирибати"); // у "С" всё ОК
        Assertions.assertThat(a.getGtinsList()).containsExactlyInAnyOrder("309805987043");
        Assertions.assertThat(b.getGtinsList()).containsExactlyInAnyOrder("229805987044");
        Assertions.assertThat(c.getGtinsList()).containsExactlyInAnyOrder("730859870645");
        Assertions.assertThat(a.getShelfLifeComment())
            .isEqualTo("хранить любя.");
        Assertions.assertThat(b.getShelfLifeComment())
            .isEqualTo("хранить бережно.");
        Assertions.assertThat(c.getShelfLifeComment())
            .isEqualTo("хранить в самом лучшем, защищённом от негатива месте.");
        Assertions.assertThat(a.getTransportUnitSize()).isEqualTo(1);
        Assertions.assertThat(b.getTransportUnitSize()).isEqualTo(2);
        Assertions.assertThat(c.getTransportUnitSize()).isEqualTo(100); // тоже обновилось из второй эксельки
        Assertions.assertThat(a.getMinShipment()).isEqualTo(16);
        Assertions.assertThat(b.getMinShipment()).isEqualTo(15);
        Assertions.assertThat(c.getMinShipment()).isEqualTo(1);
        Assertions.assertThat(a.getQuantumOfSupply()).isEqualTo(1);
        Assertions.assertThat(b.getQuantumOfSupply()).isEqualTo(1);
        Assertions.assertThat(c.getQuantumOfSupply()).isEqualTo(1);
    }

    private MdmCommon.ShopSkuKey protoKey(int supplierId, String shopSku) {
        return MdmCommon.ShopSkuKey.newBuilder().setSupplierId(supplierId).setShopSku(shopSku).build();
    }

    private ExcelFile loadFile(String name) {
        URL resource = getClass().getClassLoader().getResource("assortment/" + name);
        if (resource == null) {
            throw new IllegalStateException("Failed to find resource by file: " + "assortment/" + name);
        }
        try (var stream = resource.openStream()) {
            return ExcelFileConverter.convert(stream, Restore1PMasterDataByAssortmentExecutor.BASIC_IGNORES);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static List<MasterDataProto.OperationInfo> errors(MdmCommon.ShopSkuKey... keys) {
        List<MasterDataProto.OperationInfo> errors = new ArrayList<>();
        for (MdmCommon.ShopSkuKey key : keys) {
            errors.add(MasterDataProto.OperationInfo.newBuilder().setKey(key)
                .setStatus(MasterDataProto.OperationStatus.VALIDATION_ERROR)
                .addErrors(MasterDataProto.ErrorInfo.newBuilder()
                    .setErrorCode("mock-error")
                    .build()).build());
        }
        return errors;
    }

    private static class FileGrabberMock implements Restore1PMasterDataByAssortmentExecutor.FileGrabber {

        private Map<Integer, List<AssortmentFile>> files = new HashMap<>();

        @Override
        public Map<Integer, List<AssortmentFile>> getSupplierFiles() {
            return files;
        }

        @Override
        public LocalDateTime getStartFrom() {
            return DateTimeUtils.dateTimeNow();
        }

        public FileGrabberMock addFile(int supplierId, ExcelFile file, LocalDateTime uploadTs) {
            files.computeIfAbsent(supplierId, k -> new ArrayList<>()).add(new AssortmentFile(
                new FileLink("", "", uploadTs),
                file
            ));
            return this;
        }

        @Override
        public LocalDateTime getFinishAt() {
            return DateTimeUtils.dateTimeNow();
        }
    }
}
