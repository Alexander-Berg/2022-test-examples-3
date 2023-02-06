package ru.yandex.market.mboc.common.masterdata.parsing;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

import com.google.common.collect.ImmutableMap;
import io.github.benas.randombeans.randomizers.range.IntegerRangeRandomizer;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.core.tax.model.VatRate;
import ru.yandex.market.mbo.excel.Header;
import ru.yandex.market.mbo.mdm.common.masterdata.services.SupplierConverterServiceMock;
import ru.yandex.market.mbo.mdm.common.masterdata.validator.block.CustomsCommodityCodeBlockValidator;
import ru.yandex.market.mbo.mdm.common.masterdata.validator.block.DeliveryTimeBlockValidator;
import ru.yandex.market.mbo.mdm.common.masterdata.validator.block.GuaranteePeriodBlockValidator;
import ru.yandex.market.mbo.mdm.common.masterdata.validator.block.ManufacturerCountriesBlockValidator;
import ru.yandex.market.mbo.mdm.common.masterdata.validator.block.MinShipmentBlockValidator;
import ru.yandex.market.mbo.mdm.common.masterdata.validator.block.QuantumOfSupplyBlockValidator;
import ru.yandex.market.mbo.mdm.common.masterdata.validator.block.TransportUnitBlockValidator;
import ru.yandex.market.mboc.app.proto.MasterDataServiceMock;
import ru.yandex.market.mboc.app.proto.SupplierDocumentServiceMock;
import ru.yandex.market.mboc.common.masterdata.TestDataUtils;
import ru.yandex.market.mboc.common.masterdata.model.MasterData;
import ru.yandex.market.mboc.common.masterdata.model.QualityDocument;
import ru.yandex.market.mboc.common.masterdata.model.SupplyEvent;
import ru.yandex.market.mboc.common.masterdata.model.TimeInUnits;
import ru.yandex.market.mboc.common.masterdata.model.TransportUnit;
import ru.yandex.market.mboc.common.masterdata.model.metadata.MdmSource;
import ru.yandex.market.mboc.common.masterdata.parsing.ImportedOfferToMasterDataConverter.MasterDataConvertResult;
import ru.yandex.market.mboc.common.masterdata.services.category.MboTimeUnitAliasesService;
import ru.yandex.market.mboc.common.offers.ImportedOffer;
import ru.yandex.market.mboc.common.services.converter.ErrorAtLine;
import ru.yandex.market.mboc.common.services.excel.ExcelHeaders;
import ru.yandex.market.mboc.common.services.proto.MasterDataHelperService;
import ru.yandex.market.mboc.common.services.storage.StorageKeyValueServiceMock;
import ru.yandex.market.mboc.common.utils.ErrorInfo;
import ru.yandex.market.mboc.common.utils.ErrorInfo.Level;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

/**
 * @author jkt on 20.07.18.
 */
@SuppressWarnings("checkstyle:linelength")
public class ImportedOfferToMasterDataConverterTest {

    private static final int DAY_SHIFT = 50;
    private static final String INVALID_DOCUMENT_TYPE = "Totally incorrect type";
    private static final String INVALID_COUNTRY = "INVALID_COUNTRY";
    private static final int INVALID_MIN_SHIPMENT = MinShipmentBlockValidator.MIN_SHIPMENT_MAX + 100;
    private static final int INVALID_TRANSPORT_UNIT_SIZE = TransportUnitBlockValidator.TRANSPORT_UNIT_SIZE_MAX + 100;
    private static final int INVALID_DELIVERY_TIME = DeliveryTimeBlockValidator.DELIVERY_DAYS_MAX + 10;
    private static final int INVALID_QUANTUM_OF_SUPPLY = QuantumOfSupplyBlockValidator.QUANTUM_MAX + 100;
    private static final String INVALID_CUSTOMS_COMMODITY_CODE = "INVALID_CUSTOMS_COMMODITY_CODE";

    private MasterDataParsingConfigProvider configProvider;
    private MboTimeUnitAliasesService mboTimeUnitAliasesService;
    private ImportedOfferToMasterDataConverter masterDataConverter;
    private MasterDataServiceMock masterDataServiceMock;
    private SupplierDocumentServiceMock supplierDocumentServiceMock;
    private MasterDataHelperService masterDataHelperService;

    private static boolean hasFailedErrors(MasterDataConvertResult masterDataConvertResult) {
        return masterDataConvertResult.getErrors().stream().anyMatch(error -> error.getErrorInfo().getLevel() == Level.ERROR);
    }

    private static boolean hasWarnings(MasterDataConvertResult masterDataConvertResult) {
        return masterDataConvertResult.getErrors().stream().anyMatch(error -> error.getErrorInfo().getLevel() == Level.WARNING);
    }

    @Before
    public void setUp() {
        masterDataServiceMock = new MasterDataServiceMock();
        supplierDocumentServiceMock = new SupplierDocumentServiceMock(masterDataServiceMock);
        var storageKeyValueService = new StorageKeyValueServiceMock();
        masterDataHelperService = new MasterDataHelperService(masterDataServiceMock, supplierDocumentServiceMock,
            new SupplierConverterServiceMock(), storageKeyValueService);

        mboTimeUnitAliasesService = Mockito.mock(MboTimeUnitAliasesService.class);
        configProvider = new MasterDataParsingConfig(mboTimeUnitAliasesService, storageKeyValueService);
        masterDataConverter = new ImportedOfferToMasterDataConverter(
            configProvider, masterDataHelperService
        );
    }

    @Test
    public void whenConvertingShouldSetCorrectFieldsToMasterData() {
        LocalDate startDate = LocalDate.now().minusDays(DAY_SHIFT);
        LocalDate endDate = LocalDate.now().plusDays(DAY_SHIFT);

        ImportedOffer offer = generateOffer();
        offer.setMasterData(ExcelHeaders.SUPPLY_SCHEDULE, "пн,вт");
        offer.setMasterData(ExcelHeaders.NDS, "18%");
        offer.setMasterData(ExcelHeaders.MANUFACTURER_COUNTRY, "Россия");
        offer.setMasterData(ExcelHeaders.DOCUMENT_TYPE, "Сертификат соответствия");
        offer.setMasterData(ExcelHeaders.DOCUMENT_START_DATE, startDate.format(DateTimeFormatter.ISO_LOCAL_DATE));
        offer.setMasterData(ExcelHeaders.DOCUMENT_END_DATE, endDate.format(DateTimeFormatter.ISO_LOCAL_DATE));
        offer.setMasterData(ExcelHeaders.DOCUMENT_PICTURE, "http://test.url.com/some_picture");

        MasterDataConvertResult masterDataConvertResult = masterDataConverter.convertSingle(0, offer);

        assertSoftly(softly -> {
            softly.assertThat(hasFailedErrors(masterDataConvertResult)).isFalse();
            softly.assertThat(masterDataConvertResult.getErrors()).isEmpty();

            MasterData masterData = masterDataConvertResult.getMasterData();
            softly.assertThat(masterData.getSupplierId()).isEqualTo(offer.getSupplierId());
            softly.assertThat(masterData.getShopSku()).isEqualTo(offer.getShopSkuId());

            softly.assertThat(masterData.getManufacturerCountries())
                .containsExactly(offer.getMasterData().get(ExcelHeaders.MANUFACTURER_COUNTRY));
            softly.assertThat(masterData.getShelfLife().getTime())
                .isEqualTo(asInteger(offer.getMasterData().get(ExcelHeaders.SHELF_LIFE)));
            softly.assertThat(masterData.getShelfLife().getUnit()).isEqualTo(TimeInUnits.TimeUnit.DAY);
            softly.assertThat(masterData.getLifeTime().getTime())
                .isEqualTo(asInteger(offer.getMasterData().get(ExcelHeaders.LIFE_TIME)));
            softly.assertThat(masterData.getLifeTime().getUnit()).isEqualTo(TimeInUnits.TimeUnit.DAY);
            softly.assertThat(masterData.getGuaranteePeriod().getTime())
                .isEqualTo(asInteger(offer.getMasterData().get(ExcelHeaders.GUARANTEE_PERIOD)));
            softly.assertThat(masterData.getGuaranteePeriod().getUnit()).isEqualTo(TimeInUnits.TimeUnit.DAY);
            softly.assertThat(masterData.getMinShipment())
                .isEqualTo(asInteger(offer.getMasterData().get(ExcelHeaders.MIN_SHIPMENT)));
            softly.assertThat(masterData.getTransportUnitSize())
                .isEqualTo(asInteger(offer.getMasterData().get(ExcelHeaders.TRANSPORT_UNIT_SIZE)));
            softly.assertThat(masterData.getDeliveryTime())
                .isEqualTo(asInteger(offer.getMasterData().get(ExcelHeaders.DELIVERY_TIME)));
            softly.assertThat(masterData.getCustomsCommodityCode())
                .isEqualTo(offer.getMasterData().get(ExcelHeaders.CUSTOMS_COMMODITY_CODE));
            softly.assertThat(masterData.getQuantumOfSupply())
                .isEqualTo(asInteger(offer.getMasterData().get(ExcelHeaders.QUANTUM_OF_SUPPLY)));
            softly.assertThat(masterData.getSupplySchedule())
                .extracting(SupplyEvent::getDayOfWeek)
                .containsExactlyInAnyOrder(DayOfWeek.MONDAY, DayOfWeek.TUESDAY);
            softly.assertThat(masterData.getVat())
                .isEqualTo(VatRate.VAT_18);

            softly.assertThat(masterData.getQualityDocuments())
                .extracting(QualityDocument::getRegistrationNumber)
                .containsExactlyInAnyOrder(offer.getMasterData().get(ExcelHeaders.DOCUMENT_REG_NUMBER));
            softly.assertThat(masterData.getQualityDocuments())
                .extracting(QualityDocument::getCertificationOrgRegNumber)
                .containsExactlyInAnyOrder(offer.getMasterData()
                    .get(ExcelHeaders.DOCUMENT_CERTIFICATION_ORG_REG_NUMBER));
            softly.assertThat(masterData.getQualityDocuments())
                .extracting(QualityDocument::getType)
                .containsExactlyInAnyOrder(QualityDocument.QualityDocumentType.CERTIFICATE_OF_CONFORMITY);
            softly.assertThat(masterData.getQualityDocuments())
                .extracting(QualityDocument::getStartDate)
                .containsExactlyInAnyOrder(startDate);
            softly.assertThat(masterData.getQualityDocuments())
                .extracting(QualityDocument::getEndDate)
                .containsExactlyInAnyOrder(endDate);
            softly.assertThat(masterData.getQualityDocuments())
                .flatExtracting(QualityDocument::getPictures)
                .containsExactlyInAnyOrder(offer.getMasterData().get(ExcelHeaders.DOCUMENT_PICTURE));
        });

    }

    @Test
    public void whenSupplyScheduleSetAsNumbersShouldParseCorrectly() {
        ImportedOffer offer = generateOffer();
        offer.setMasterData(ExcelHeaders.SUPPLY_SCHEDULE, "1,5");

        MasterDataConvertResult masterDataConvertResult = masterDataConverter.convertSingle(0, offer);

        assertSoftly(softly -> {
            softly.assertThat(hasFailedErrors(masterDataConvertResult)).isFalse();
            softly.assertThat(masterDataConvertResult.getErrors()).isEmpty();

            softly.assertThat(masterDataConvertResult.getMasterData().getSupplySchedule())
                .extracting(SupplyEvent::getDayOfWeek)
                .containsExactlyInAnyOrder(DayOfWeek.MONDAY, DayOfWeek.FRIDAY);
        });

    }

    @Test
    public void whenSupplyScheduleSetAsStringsShouldParseCorrectly() {
        ImportedOffer offer = generateOffer();
        offer.setMasterData(ExcelHeaders.SUPPLY_SCHEDULE, "пн,вт,ср,чт,пт,сб,вс");

        MasterDataConvertResult masterDataConvertResult = masterDataConverter.convertSingle(0, offer);

        assertSoftly(softly -> {
            softly.assertThat(hasFailedErrors(masterDataConvertResult)).isFalse();
            softly.assertThat(masterDataConvertResult.getErrors()).isEmpty();

            softly.assertThat(masterDataConvertResult.getMasterData().getSupplySchedule())
                .extracting(SupplyEvent::getDayOfWeek)
                .containsExactlyInAnyOrder(DayOfWeek.values());
        });

    }

    @Test
    public void whenSupplyScheduleIsInvalidShouldFail() {
        ImportedOffer offer = generateOffer();
        offer.setMasterData(ExcelHeaders.SUPPLY_SCHEDULE, "Пн-Вт");

        MasterDataConvertResult masterDataConvertResult = masterDataConverter.convertSingle(0, offer);
        assertSoftly(softly -> {
            softly.assertThat(hasFailedErrors(masterDataConvertResult)).isTrue();
            ErrorInfo errorInfo = masterDataConvertResult.getErrors().get(0).getErrorInfo();
            softly.assertThat(errorInfo.toString()).contains("Дни недели указаны в неверном формате. Должно быть " +
                "\"пн,вт,чт\", или \"понедельник,вторник,четверг\", или \"1,2,4\".");
            softly.assertThat(errorInfo.toString()).contains("Передано: Пн-Вт");
        });

    }

    @Test
    public void whenMultipleCountriesAreProvidedShouldParseAsCommaSeparated() {
        ImportedOffer offer = generateOffer();
        String[] countries = new String[]{"Китай", "Россия"};
        offer.setMasterData(ExcelHeaders.MANUFACTURER_COUNTRY, String.join(",", countries));

        MasterDataConvertResult masterDataConvertResult = masterDataConverter.convertSingle(0, offer);

        assertSoftly(softly -> {
            softly.assertThat(hasFailedErrors(masterDataConvertResult)).isFalse();
            softly.assertThat(masterDataConvertResult.getMasterData().getManufacturerCountries())
                .containsExactlyInAnyOrder(countries);
        });

    }

    @Test
    public void whenVatProvidedAsEnumNamesShouldParseCorrectly() {
        ImportedOffer offer = generateOffer();
        offer.setMasterData(ExcelHeaders.NDS, "VAT_18");

        MasterDataConvertResult masterDataConvertResult = masterDataConverter.convertSingle(0, offer);

        assertSoftly(softly -> {
            softly.assertThat(hasFailedErrors(masterDataConvertResult)).isFalse();
            softly.assertThat(masterDataConvertResult.getErrors()).isEmpty();

            softly.assertThat(masterDataConvertResult.getMasterData().getVat()).isEqualTo(VatRate.VAT_18);
        });
    }

    @Test
    public void whenVatProvidedAsPercentShouldParseCorrectly() {
        ImportedOffer offer = generateOffer();
        offer.setMasterData(ExcelHeaders.NDS, "10%");

        MasterDataConvertResult masterDataConvertResult = masterDataConverter.convertSingle(0, offer);

        assertSoftly(softly -> {
            softly.assertThat(hasFailedErrors(masterDataConvertResult)).isFalse();
            softly.assertThat(masterDataConvertResult.getErrors()).isEmpty();

            softly.assertThat(masterDataConvertResult.getMasterData().getVat()).isEqualTo(VatRate.VAT_10);
        });
    }

    @Test
    public void whenVatProvidedAsIdShouldParseCorrectly() {
        ImportedOffer offer = generateOffer();
        offer.setMasterData(ExcelHeaders.NDS, "3");

        MasterDataConvertResult masterDataConvertResult = masterDataConverter.convertSingle(0, offer);

        assertSoftly(softly -> {
            softly.assertThat(hasFailedErrors(masterDataConvertResult)).isFalse();
            softly.assertThat(masterDataConvertResult.getErrors()).isEmpty();

            softly.assertThat(masterDataConvertResult.getMasterData().getVat()).isEqualTo(VatRate.VAT_18_118);
        });
    }

    @Test
    public void whenVatProvidedAsNoVatShouldParseCorrectly() {
        ImportedOffer offer = generateOffer();
        offer.setMasterData(ExcelHeaders.NDS, "без ндс");

        MasterDataConvertResult masterDataConvertResult = masterDataConverter.convertSingle(0, offer);

        assertSoftly(softly -> {
            softly.assertThat(hasFailedErrors(masterDataConvertResult)).isFalse();
            softly.assertThat(masterDataConvertResult.getErrors()).isEmpty();

            softly.assertThat(masterDataConvertResult.getMasterData().getVat()).isEqualTo(VatRate.NO_VAT);
        });
    }

    @SuppressWarnings("checkstyle:linelength")
    @Test
    public void whenVatIsInvalidShouldProduceCorrectError() {
        ImportedOffer offer = generateOffer();
        String invalidNds = "INVALID_TEST_NDS";
        offer.setMasterData(ExcelHeaders.NDS, invalidNds);

        MasterDataConvertResult masterDataConvertResult = masterDataConverter.convertSingle(0, offer);

        assertSoftly(softly -> {
            softly.assertThat(hasFailedErrors(masterDataConvertResult)).isTrue();
            softly.assertThat(masterDataConvertResult.getErrors()).allMatch(error -> error.toString().contains(invalidNds));
            softly.assertThat(masterDataConvertResult.getErrors()).allMatch(error -> error.toString().matches(".*НДС" +
                ".*должен иметь одно из значений.*"));
            softly.assertThat(masterDataConvertResult.getErrors()).allMatch(error -> error.toString().contains(
                "VAT_18, VAT_10, VAT_18_118, VAT_10_110, VAT_0, NO_VAT, VAT_20, VAT_20_120, 0, 10, 18, Без НДС"));
        });
    }

    @Test
    public void whenDocumentFieldsNotProvidedShouldBeEmpty() {
        ImportedOffer offer = generateOffer();
        configProvider.getDocumentParsingConfig().keySet().forEach(offer.getMasterData()::remove);

        MasterDataConvertResult masterDataConvertResult = masterDataConverter.convertSingle(0, offer);

        assertSoftly(softly -> {
            softly.assertThat(hasFailedErrors(masterDataConvertResult)).isFalse();
            softly.assertThat(masterDataConvertResult.getErrors()).isEmpty();

            softly.assertThat(masterDataConvertResult.getMasterData().getQualityDocuments()).isEmpty();
        });

    }

    @Test
    public void whenMinShipmentNotSetShouldSetZero() {
        ImportedOffer offer = generateOffer();
        offer.setMasterData(ExcelHeaders.MIN_SHIPMENT, "");

        MasterDataConvertResult masterDataConvertResult = masterDataConverter.convertSingle(0, offer);

        assertSoftly(softly -> {
            softly.assertThat(hasFailedErrors(masterDataConvertResult)).isFalse();
            softly.assertThat(masterDataConvertResult.getErrors()).isEmpty();

            softly.assertThat(masterDataConvertResult.getMasterData().getMinShipment()).isEqualTo(0);
        });

    }

    @Test
    public void whenTransportUnitNotSetShouldSetZero() {
        ImportedOffer offer = generateOffer();
        offer.setMasterData(ExcelHeaders.TRANSPORT_UNIT_SIZE, "");

        MasterDataConvertResult masterDataConvertResult = masterDataConverter.convertSingle(0, offer);

        assertSoftly(softly -> {
            softly.assertThat(hasFailedErrors(masterDataConvertResult)).isFalse();
            softly.assertThat(masterDataConvertResult.getErrors()).isEmpty();

            softly.assertThat(masterDataConvertResult.getMasterData().getTransportUnitSize()).isEqualTo(0);
        });

    }

    @Test
    public void setNotEmptyTransportUnit() {
        final Integer transportUnitSize = 123;
        ImportedOffer offer = generateOffer();
        offer.setMasterData(ExcelHeaders.TRANSPORT_UNIT_SIZE, transportUnitSize.toString());

        MasterDataConvertResult masterDataConvertResult = masterDataConverter.convertSingle(0, offer);

        assertSoftly(softly -> {
            softly.assertThat(hasFailedErrors(masterDataConvertResult)).isFalse();
            softly.assertThat(masterDataConvertResult.getErrors()).isEmpty();

            softly.assertThat(masterDataConvertResult.getMasterData().getTransportUnitSize())
                .isEqualTo(transportUnitSize);
        });

    }

    @Test
    public void setNotEmptyMinShipment() {
        final Integer minShipment = 456;
        ImportedOffer offer = generateOffer();
        offer.setMasterData(ExcelHeaders.MIN_SHIPMENT, minShipment.toString());

        MasterDataConvertResult masterDataConvertResult = masterDataConverter.convertSingle(0, offer);

        assertSoftly(softly -> {
            softly.assertThat(hasFailedErrors(masterDataConvertResult)).isFalse();
            softly.assertThat(masterDataConvertResult.getErrors()).isEmpty();

            softly.assertThat(masterDataConvertResult.getMasterData().getMinShipment())
                .isEqualTo(minShipment);
        });

    }

    @Test
    public void whenQualityDocumentEndDateIsUnlimitedShouldSetDefaultUnlimitedEndDate() {
        ImportedOffer offer = generateOffer();
        offer.setMasterData(ExcelHeaders.DOCUMENT_END_DATE, "Бессрочный");

        MasterDataConvertResult masterDataConvertResult = masterDataConverter.convertSingle(0, offer);

        assertSoftly(softly -> {
            softly.assertThat(hasFailedErrors(masterDataConvertResult)).isFalse();
            softly.assertThat(masterDataConvertResult.getErrors()).isEmpty();

            softly.assertThat(masterDataConvertResult.getMasterData().getQualityDocuments())
                .extracting(QualityDocument::getEndDate)
                .containsExactly(QualityDocument.UNLIMITED_END_DATE);
        });
    }

    @Test
    @SuppressWarnings("checkstyle:MagicNumber")
    public void whenQualityDocumentStartDateIsFullFormatShouldParseCorrectly() {
        ImportedOffer offer = generateOffer();
        offer.setMasterData(ExcelHeaders.DOCUMENT_START_DATE, "04 октября 2018");

        MasterDataConvertResult masterDataConvertResult = masterDataConverter.convertSingle(0, offer);

        assertSoftly(softly -> {
            softly.assertThat(hasFailedErrors(masterDataConvertResult)).isFalse();
            softly.assertThat(masterDataConvertResult.getErrors()).isEmpty();

            softly.assertThat(masterDataConvertResult.getMasterData().getQualityDocuments())
                .extracting(QualityDocument::getStartDate)
                .containsExactly(LocalDate.of(2018, 10, 4));
        });
    }

    @Test
    public void whenQualityDocumentRegistrationNumberIsNotProvidedShouldIgnoreDocument() {
        ImportedOffer offer = generateOffer();
        offer.setMasterData(ExcelHeaders.DOCUMENT_REG_NUMBER, "");

        MasterDataConvertResult masterDataConvertResult = masterDataConverter.convertSingle(0, offer);

        assertSoftly(softly -> {
            softly.assertThat(hasFailedErrors(masterDataConvertResult)).isFalse();
            softly.assertThat(masterDataConvertResult.getErrors()).isEmpty();

            softly.assertThat(masterDataConvertResult.getMasterData().getQualityDocuments()).isEmpty();
        });
    }

    @Test
    public void whenConvertingQualityDocumentShouldSetSourceSupplier() {
        ImportedOffer offer = generateOffer();

        MasterDataConvertResult masterDataConvertResult = masterDataConverter.convertSingle(0, offer);

        assertSoftly(softly -> {
            softly.assertThat(hasFailedErrors(masterDataConvertResult)).isFalse();
            softly.assertThat(masterDataConvertResult.getErrors()).isEmpty();

            softly.assertThat(masterDataConvertResult.getMasterData().getQualityDocuments())
                .extracting(QualityDocument::getMetadata)
                .extracting(QualityDocument.Metadata::getSource)
                .containsExactly(MdmSource.SUPPLIER);
        });
    }

    @Test
    public void whenRequiredFieldIsMissedShouldFail() {
        MasterDataParsingConfigProvider testConfig = createTestConfig(ImmutableMap.of(ExcelHeaders.NDS,
            FieldConfig.Builder.config(FieldParsers::asVatRate, MasterData::setVat)
                .required()
                .build()
        ));

        ImportedOfferToMasterDataConverter dataConverter = new ImportedOfferToMasterDataConverter(
            testConfig, masterDataHelperService
        );
        ImportedOffer offer = generateOffer();
        offer.getMasterData().remove(ExcelHeaders.NDS);

        MasterDataConvertResult masterDataConvertResult = dataConverter.convertSingle(0, offer);

        assertSoftly(softly -> {
            softly.assertThat(hasFailedErrors(masterDataConvertResult)).isTrue();
            softly.assertThat(hasWarnings(masterDataConvertResult)).isFalse();
            softly.assertThat(masterDataConvertResult.getErrors().get(0).toString())
                .contains("Отсутствует значение для колонки 'НДС'");
        });
    }

    @Test
    public void whenRequiredFieldIsMissedButLevelOverriddenShouldLogMessage() {
        MasterDataParsingConfigProvider testConfig = createTestConfig(
            ImmutableMap.of(ExcelHeaders.NDS,
                FieldConfig.Builder.config(FieldParsers::asVatRate, MasterData::setVat)
                    .required()
                    .makeErrorsNotWorse(Level.WARNING)
                    .build()
            ),
            ImmutableMap.of(ExcelHeaders.DOCUMENT_REG_NUMBER,
                FieldConfig.Builder.config(FieldParsers::asIs, QualityDocument::setRegistrationNumber)
                    .build()
            )
        );

        ImportedOfferToMasterDataConverter dataConverter = new ImportedOfferToMasterDataConverter(
            testConfig, masterDataHelperService
        );
        ImportedOffer offer = generateOffer();
        offer.getMasterData().remove(ExcelHeaders.NDS);

        MasterDataConvertResult masterDataConvertResult = dataConverter.convertSingle(0, offer);

        assertSoftly(softly -> {
            softly.assertThat(masterDataConvertResult.getErrors()).hasSize(1);
            softly.assertThat(hasFailedErrors(masterDataConvertResult)).isFalse();
            softly.assertThat(hasWarnings(masterDataConvertResult)).isTrue();
            softly.assertThat(masterDataConvertResult.getErrors())
                .extracting(Objects::toString)
                .allMatch(s -> s.contains("Отсутствует значение для колонки 'НДС'"));
            softly.assertThat(masterDataConvertResult.getErrors())
                .extracting(ErrorAtLine::getErrorInfo)
                .extracting(ErrorInfo::getLevel)
                .containsExactly(Level.WARNING);
        });
    }

    @Test
    public void whenRequiredWeakFieldIsMissedShouldAddMessage() {
        MasterDataParsingConfigProvider testConfig = createTestConfig(
            ImmutableMap.of(ExcelHeaders.NDS,
                FieldConfig.Builder.config(FieldParsers::asVatRate, MasterData::setVat)
                    .required()
                    .makeErrorsNotWorse(Level.WARNING)
                    .build()
            ),
            ImmutableMap.of(ExcelHeaders.DOCUMENT_REG_NUMBER,
                FieldConfig.Builder.config(FieldParsers::asIs, QualityDocument::setRegistrationNumber)
                    .build()
            )
        );

        ImportedOfferToMasterDataConverter dataConverter = new ImportedOfferToMasterDataConverter(
            testConfig, masterDataHelperService
        );
        ImportedOffer offer = generateOffer();
        offer.getMasterData().remove(ExcelHeaders.NDS);

        MasterDataConvertResult masterDataConvertResult = dataConverter.convertSingle(0, offer);

        assertSoftly(softly -> {
            softly.assertThat(masterDataConvertResult.getErrors()).hasSize(1);
            softly.assertThat(hasFailedErrors(masterDataConvertResult)).isFalse();
            softly.assertThat(hasWarnings(masterDataConvertResult)).isTrue();
            softly.assertThat(masterDataConvertResult.getErrors())
                .extracting(Objects::toString)
                .allMatch(s -> s.contains("Отсутствует значение для колонки 'НДС'"));
        });
    }

    private MasterDataParsingConfigProvider createTestConfig(
        ImmutableMap<Header, FieldConfig<MasterData>> masterData) {
        return createTestConfig(masterData, ImmutableMap.of());
    }

    private MasterDataParsingConfigProvider createTestConfig(
        ImmutableMap<Header, FieldConfig<MasterData>> masterData, ImmutableMap<Header,
        FieldConfig<QualityDocument>> document) {

        return new MasterDataParsingConfigProvider() {
            @Override
            public ImmutableMap<Header, FieldConfig<MasterData>> getMasterDataParsingConfig() {
                return masterData;
            }

            @Override
            public ImmutableMap<Header, FieldConfig<QualityDocument>> getDocumentParsingConfig() {
                return document;
            }
        };
    }

    @Test
    public void whenNoRequiredFieldIsProvidedShouldFail() {
        ImportedOffer offer = generateOffer();
        offer.setMasterData(ExcelHeaders.DOCUMENT_PICTURE, "Totally incorrect url");


        MasterDataConvertResult masterDataConvertResult = masterDataConverter.convertSingle(0, offer);

        assertSoftly(softly -> {
            softly.assertThat(hasFailedErrors(masterDataConvertResult)).isTrue();
            softly.assertThat(masterDataConvertResult.getErrors().get(0).toString())
                .contains("не является валидным URL");
        });
    }

    @Test
    public void whenIncorrectDocumentTypeShouldFail() {
        ImportedOffer offer = generateOffer();
        offer.setMasterData(ExcelHeaders.DOCUMENT_TYPE, INVALID_DOCUMENT_TYPE);

        MasterDataConvertResult masterDataConvertResult = masterDataConverter.convertSingle(0, offer);

        assertSoftly(softly -> {
            softly.assertThat(hasFailedErrors(masterDataConvertResult)).isTrue();
            softly.assertThat(masterDataConvertResult.getErrors().get(0).toString())
                .contains(ExcelHeaders.DOCUMENT_TYPE.getTitle());
            softly.assertThat(masterDataConvertResult.getErrors().get(0).toString())
                .contains("Сертификат соответствия, Декларация соответствия, Свидетельство о государственной " +
                    "регистрации, Отказное письмо");
            softly.assertThat(masterDataConvertResult.getErrors().get(0).toString())
                .contains(INVALID_DOCUMENT_TYPE);
        });

    }

    @Test
    public void whenManufacturerCountryIsNotProvidedShouldFail() {
        ImportedOffer offer = generateOffer();
        offer.setMasterData(ExcelHeaders.MANUFACTURER_COUNTRY, "");

        MasterDataConvertResult masterDataConvertResult = masterDataConverter.convertSingle(0, offer);

        assertThat(masterDataConvertResult.getErrors())
            .extracting(ErrorAtLine::getErrorInfo)
            .containsExactlyElementsOf(
                ManufacturerCountriesBlockValidator.validateManufacturerCountries(Collections.emptyList(), true)
            );
    }

    @Test
    public void whenManufacturerCountryIsNotValidShouldFail() {
        ImportedOffer offer = generateOffer();
        offer.setMasterData(ExcelHeaders.MANUFACTURER_COUNTRY, INVALID_COUNTRY);

        MasterDataConvertResult masterDataConvertResult = masterDataConverter.convertSingle(0, offer);

        assertThat(masterDataConvertResult.getErrors())
            .extracting(ErrorAtLine::getErrorInfo)
            .containsExactlyElementsOf(
                ManufacturerCountriesBlockValidator.validateManufacturerCountries(
                    Collections.singleton(INVALID_COUNTRY), true)
            );
    }

    @Test
    public void whenMinShipmentIsInvalidShouldFail() {
        ImportedOffer offer = generateOffer();
        offer.setMasterData(ExcelHeaders.MIN_SHIPMENT, INVALID_MIN_SHIPMENT + "");

        MasterDataConvertResult masterDataConvertResult = masterDataConverter.convertSingle(0, offer);

        MinShipmentBlockValidator minShipmentBlockValidator = new MinShipmentBlockValidator();

        assertThat(masterDataConvertResult.getErrors())
            .extracting(ErrorAtLine::getErrorInfo)
            .containsExactly(minShipmentBlockValidator.validateValue(INVALID_MIN_SHIPMENT).get());
    }

    @Test
    public void whenTransportUnitSizeIsInvalidShouldFail() {
        ImportedOffer offer = generateOffer();
        offer.setMasterData(ExcelHeaders.TRANSPORT_UNIT_SIZE, INVALID_TRANSPORT_UNIT_SIZE + "");

        MasterDataConvertResult masterDataConvertResult = masterDataConverter.convertSingle(0, offer);

        assertThat(masterDataConvertResult.getErrors())
            .extracting(ErrorAtLine::getErrorInfo)
            .containsExactly(TransportUnitBlockValidator.validateTransportUnit(
                new TransportUnit(INVALID_TRANSPORT_UNIT_SIZE, 0)).get());
    }

    @Test
    public void whenDeliveryTimeIsInvalidShouldFail() {
        ImportedOffer offer = generateOffer();
        offer.setMasterData(ExcelHeaders.DELIVERY_TIME, INVALID_DELIVERY_TIME + "");

        MasterDataConvertResult masterDataConvertResult = masterDataConverter.convertSingle(0, offer);

        DeliveryTimeBlockValidator deliveryTimeValidator = new DeliveryTimeBlockValidator();

        assertThat(masterDataConvertResult.getErrors())
            .extracting(ErrorAtLine::getErrorInfo)
            .containsExactly(deliveryTimeValidator.validateValue(INVALID_DELIVERY_TIME).get());
    }

    @Test
    public void whenQuantumOfSupplyIsInvalidShouldFail() {
        ImportedOffer offer = generateOffer();
        offer.setMasterData(ExcelHeaders.QUANTUM_OF_SUPPLY, INVALID_QUANTUM_OF_SUPPLY + "");

        MasterDataConvertResult masterDataConvertResult = masterDataConverter.convertSingle(0, offer);

        QuantumOfSupplyBlockValidator quantumOfSupplyValidator = new QuantumOfSupplyBlockValidator();

        assertThat(masterDataConvertResult.getErrors())
            .extracting(ErrorAtLine::getErrorInfo)
            .containsExactly(quantumOfSupplyValidator.validateValue(INVALID_QUANTUM_OF_SUPPLY).get());
    }

    @Test
    public void whenCustomsCommodityCodeIsInvalidShouldFail() {
        ImportedOffer offer = generateOffer();
        offer.setMasterData(ExcelHeaders.CUSTOMS_COMMODITY_CODE, INVALID_CUSTOMS_COMMODITY_CODE + "");

        MasterDataConvertResult masterDataConvertResult = masterDataConverter.convertSingle(0, offer);

        assertThat(masterDataConvertResult.getErrors())
            .extracting(ErrorAtLine::getErrorInfo)
            .containsExactly(
                CustomsCommodityCodeBlockValidator.validateCustomsCommodityCode(INVALID_CUSTOMS_COMMODITY_CODE).get()
            );
    }

    private Integer asInteger(String value) {
        return Integer.parseInt(value);
    }

    private ImportedOffer generateOffer() {
        LocalDate startDate = LocalDate.now().minusDays(DAY_SHIFT);
        LocalDate endDate = LocalDate.now().plusDays(DAY_SHIFT);

        ImportedOffer offer = new ImportedOffer();
        AtomicLong seed = new AtomicLong(0);
        configProvider.getAllHeaders().forEach(header -> offer.setMasterData(
            header,
            TestDataUtils.defaultRandomBuilder(seed.incrementAndGet())
                .randomize(Integer.class, new IntegerRangeRandomizer(
                        GuaranteePeriodBlockValidator.DEFAULT_TIME_LIMIT.getMinValue()
                            .getTimeInUnit(TimeInUnits.TimeUnit.DAY),
                        DeliveryTimeBlockValidator.DELIVERY_DAYS_MAX,
                        seed.incrementAndGet()
                    )
                ).build()
                .nextObject(Integer.class).toString()
        ));
        offer.setMasterData(ExcelHeaders.MANUFACTURER_COUNTRY, "Россия");
        offer.setMasterData(ExcelHeaders.SUPPLY_SCHEDULE, "пн,вт");
        offer.setMasterData(ExcelHeaders.NDS, "18%");
        offer.setMasterData(ExcelHeaders.CUSTOMS_COMMODITY_CODE, "1234567890");
        offer.setMasterData(ExcelHeaders.DOCUMENT_REG_NUMBER, "01234567890123");
        offer.setMasterData(ExcelHeaders.DOCUMENT_CERTIFICATION_ORG_REG_NUMBER, "4_8_15_16");
        offer.setMasterData(ExcelHeaders.DOCUMENT_TYPE, "Сертификат соответствия");
        offer.setMasterData(ExcelHeaders.DOCUMENT_START_DATE, startDate.format(DateTimeFormatter.ISO_LOCAL_DATE));
        offer.setMasterData(ExcelHeaders.DOCUMENT_END_DATE, endDate.format(DateTimeFormatter.ISO_LOCAL_DATE));
        offer.setMasterData(ExcelHeaders.DOCUMENT_PICTURE, "http://test.url.com/some_picture");
        offer.setMasterData(ExcelHeaders.BOX_DIMENSIONS, "55/40/23");
        offer.setMasterData(ExcelHeaders.USE_IN_MERCURY, "нет");
        return offer;
    }
}
