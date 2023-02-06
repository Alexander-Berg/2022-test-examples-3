package ru.yandex.market.admin.service.remote;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.admin.FunctionalTest;
import ru.yandex.market.admin.ui.model.supplier.UIBillingCorrectionCommonInfo;
import ru.yandex.market.admin.ui.model.supplier.UIParsedSupplyBillingCorrection;
import ru.yandex.market.admin.ui.model.supplier.UIRawCorrection;
import ru.yandex.market.admin.ui.model.supplier.UIServiceType;
import ru.yandex.market.admin.ui.service.SupplyBillingBatchCorrectionUIService;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.fulfillment.StockDao;
import ru.yandex.market.core.fulfillment.correction.SupplyBillingCorrectionService;
import ru.yandex.market.core.supplier.SupplierService;
import ru.yandex.market.mbi.environment.EnvironmentService;

import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Функциональные тесты для {@link RemoteSupplyBillingBatchCorrectionUIService}.
 *
 * @author sergeitelnov
 */
class RemoteSupplyBillingBatchCorrectionUIServiceTest extends FunctionalTest {

    @Autowired
    private SupplyBillingCorrectionService supplyBillingCorrectionService;

    @Autowired
    private SupplierService supplierService;

    @Autowired
    private StockDao stockDao;

    @Autowired
    private EnvironmentService environmentService;

    private SupplyBillingBatchCorrectionUIService batchCorrectionService;

    static Stream<Arguments> setInvalidInputArgs() {
        return Stream.of(
                Arguments.of("Невалидные числа",
                        "12aa3,sku_1,-100\n" +
                                "123,sku_2,-1ss00",
                        "В строке '1' ожидалось число, For input string: \"12aa3\"."
                ),
                Arguments.of("Различное число столбцов в строке",
                        "11,sku_1,-100\n" +
                                "11",
                        "Неверное число столбцов в строке '2'."
                ),
                Arguments.of(
                        "Неправильное число столбцов в первой строке",
                        "11,sku_1,comment,11,100\n" +
                                "11,sku_2,comment,100",
                        "Неверное число столбцов в строке '1'."
                ),
                Arguments.of("Пустой csv",
                        "                    \t\n\r\t",
                        "Неверное число столбцов в строке '1'."
                ),
                Arguments.of(
                        "Не найден supplier",
                        "11,sku_1,100",
                        "Поставщик 11 не найден"
                ),
                Arguments.of(
                        "Не найден shopSku",
                        "12,sku_invalid,100",
                        "Shop sku [sku_invalid] для поставщика 12 не найдены"
                ),
                Arguments.of(
                        "Одинаковые данные",
                        "" +
                                "12,sku_1,100\n" +
                                "12,sku_1,100",
                        "Найдены дубликаты для supplierId 12 и shop sku sku_1"
                ),
                Arguments.of(
                        "Больше чем 1 поставщик в корректировке",
                        "" +
                                "12,sku_1,100\n" +
                                "13,sku_1,100",
                        "Корректировки можно делать только в пределах одного поставщика"
                )
        );
    }

    @BeforeEach
    void setUp() {
        Instant instant = LocalDateTime.of(2019, Month.APRIL, 25, 14, 2)
                .atZone(ZoneId.systemDefault())
                .toInstant();
        Clock clock = Clock.fixed(instant, ZoneOffset.UTC);

        batchCorrectionService = new RemoteSupplyBillingBatchCorrectionUIService(
                supplyBillingCorrectionService,
                null,
                supplierService,
                stockDao,
                environmentService,
                clock
        );
    }

    @Test
    @DisplayName("Парсить csv")
    @DbUnitDataSet(before = "RemoteSupplyBillingBatchCorrectionUIServiceTest.create.before.csv")
    void testParseCsv() {
        UIRawCorrection raw = createRawCorrection(
                "12,sku_1,-100\n" +
                        "12,sku_2,-100"
        );

        List<UIParsedSupplyBillingCorrection> parsedData = batchCorrectionService.parseAndValidate(raw);

        assertThat(parsedData, hasSize(2));

        assertThatUIParsedCorrection(parsedData.get(0), 12, "sku_1", "some_comment", -100);
        assertThatUIParsedCorrection(parsedData.get(1), 12, "sku_2", "some_comment", -100);
    }

    @DisplayName("Обработка некорректных данных")
    @MethodSource("setInvalidInputArgs")
    @ParameterizedTest(name = "{0}")
    @DbUnitDataSet(before = "RemoteSupplyBillingBatchCorrectionUIServiceTest.create.before.csv")
    void testInvalidInput(String description, String raw, String errorMessage) {
        UIRawCorrection correction = createRawCorrection(raw);

        IllegalArgumentException thrown = assertThrows(
                IllegalArgumentException.class,
                () -> batchCorrectionService.parseAndValidate(correction)
        );

        assertThat(thrown.getMessage(), equalTo(errorMessage));
    }

    @Test
    @DisplayName("Создать новые корректировки")
    @DbUnitDataSet(
            before = "RemoteSupplyBillingBatchCorrectionUIServiceTest.create.before.csv",
            after = "RemoteSupplyBillingBatchCorrectionUIServiceTest.create.after.csv"
    )
    void testStorageAndWithdrawCorrections() {
        List<UIParsedSupplyBillingCorrection> correctionPack1 = createCorrectionList(12, "sku_", 3);
        UIBillingCorrectionCommonInfo info1 = createCorrectionInfo(UIServiceType.FF_WITHDRAW);

        batchCorrectionService.createCorrections(correctionPack1, info1);

        List<UIParsedSupplyBillingCorrection> correctionPack2 = createCorrectionList(12, "sku", 1);
        UIBillingCorrectionCommonInfo info2 = createCorrectionInfo(UIServiceType.FF_STORAGE_BILLING);

        batchCorrectionService.createCorrections(correctionPack2, info2);
    }

    private UIRawCorrection createRawCorrection(String csv) {
        return new UIRawCorrection(csv, "some_comment", UIServiceType.FF_WITHDRAW);
    }

    private List<UIParsedSupplyBillingCorrection> createCorrectionList(
            final long supplierId,
            final String shopSku,
            final int count
    ) {
        List<UIParsedSupplyBillingCorrection> list = new ArrayList<>();

        for (int i = 0; i != count; i++) {
            UIParsedSupplyBillingCorrection correction = new UIParsedSupplyBillingCorrection(
                    supplierId, shopSku + (i + 1), "comment", -100 - 10 * i
            );
            list.add(correction);
        }

        return list;
    }

    private UIBillingCorrectionCommonInfo createCorrectionInfo(UIServiceType serviceType) {
        return new UIBillingCorrectionCommonInfo(
                55L,
                "ya",
                serviceType,
                "ignored comment"
        );
    }

    private void assertThatUIParsedCorrection(UIParsedSupplyBillingCorrection actual,
                                              long supplier,
                                              String shopSku,
                                              String description,
                                              int amount
    ) {
        assertThat(actual.getSupplierId(), equalTo(supplier));
        assertThat(actual.getShopSku(), equalTo(shopSku));
        assertThat(actual.getComment(), equalTo(description));
        assertThat(actual.getAmount(), equalTo(amount));
    }
}
