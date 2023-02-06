package ru.yandex.market.logistics.tarifficator.service;

import java.io.ByteArrayInputStream;
import java.net.URL;
import java.util.Map;
import java.util.Objects;

import javax.annotation.Nonnull;

import com.google.common.collect.ImmutableMap;
import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.mds.s3.client.model.ResourceLocation;
import ru.yandex.market.common.mds.s3.client.service.api.MdsS3Client;
import ru.yandex.market.common.mds.s3.client.service.factory.ResourceLocationFactory;
import ru.yandex.market.logistics.tarifficator.AbstractContextualTest;
import ru.yandex.market.logistics.tarifficator.model.pricelist.CellPosition;
import ru.yandex.market.logistics.tarifficator.model.pricelist.raw.LocationRaw;
import ru.yandex.market.logistics.tarifficator.model.pricelist.scheme.SheetType;
import ru.yandex.market.logistics.tarifficator.model.source.SpreadsheetPriceListSource;
import ru.yandex.market.logistics.tarifficator.service.pricelist.PriceListParsingLoggingService;
import ru.yandex.market.logistics.tarifficator.util.TestUtils;
import ru.yandex.market.logistics.test.integration.utils.ExcelFileComparisonUtils;

import static java.lang.ClassLoader.getSystemResourceAsStream;
import static org.mockito.Mockito.when;

class PriceListParsingLoggingServiceTest extends AbstractContextualTest {
    private static final String MDS_FILE_URL = "http://localhost:8080/price_list_log_document_1.xlsx";
    private static final LocationRaw MOSCOW_LOCATION = LocationRaw.builder()
        .address("Москва")
        .geoId(213)
        .locationAddress("Москва")
        .build();
    private static final LocationRaw BARNAUL_LOCATION = LocationRaw.builder()
        .address("Россия, Алтайский край, Барнаул")
        .geoId(197)
        .locationAddress("Россия, Алтайский край, Барнаул")
        .build();
    private static final LocationRaw SARANSK_LOCATION = LocationRaw.builder()
        .geoId(42)
        .locationAddress("Саранск")
        .build();
    private static final LocationRaw UNKNOWN_CITY_LOCATION = LocationRaw.builder()
        .address("Неизвестный город")
        .build();

    @Autowired
    private PriceListParsingLoggingService priceListParsingLoggingService;
    @Autowired
    private MdsS3Client mdsS3Client;
    @Autowired
    private ResourceLocationFactory resourceLocationFactory;

    @Test
    @DisplayName("Обработать прайс-лист файл c ошибками в некоторых строках")
    @SneakyThrows
    void commentsAndErrorsAreEqualTest() {
        when(mdsS3Client.getUrl(ResourceLocation.create("tarifficator", "price_list_log_document_1.xlsx")))
            .thenReturn(new URL(MDS_FILE_URL));
        when(resourceLocationFactory.createLocation("price_list_log_document_1.xlsx")).thenAnswer(invocation ->
            ResourceLocation.create("tarifficator", invocation.getArgument(0, String.class))
        );

        SpreadsheetPriceListSource source = new SpreadsheetPriceListSource(IOUtils.toByteArray(
            Objects.requireNonNull(getSystemResourceAsStream(
                "service/price-list-files/xlsx/price-list-with-not-found-directions.xlsx"
            )))
        );

        byte[] logFile = priceListParsingLoggingService.createExcelLog(buildErrors(), buildLocations(), source);

        ExcelFileComparisonUtils.assertEquals(
            TestUtils.getFileAsInputStream("service/price-list-files/xlsx/log_file.xlsx"),
            new ByteArrayInputStream(logFile)
        );
    }

    @Nonnull
    private ImmutableMap<CellPosition, String> buildErrors() {
        return new ImmutableMap.Builder<CellPosition, String>()
            .put(new CellPosition(SheetType.SERVICES, "A", 3), "Локация отправления не заполнена")
            .put(
                new CellPosition(SheetType.SERVICES, "B", 4),
                "Диапазон доступных значений широты от -90.0 до 90.0")
            .put(
                new CellPosition(SheetType.SERVICES, "C", 4),
                "Диапазон доступных значений долготы от -180.0 до 180.0")
            .put(new CellPosition(SheetType.DELIVERY, "D", 3), "Локация назначения не заполнена")
            .put(
                new CellPosition(SheetType.DELIVERY, "G", 4),
                "Минимальный вес больше чем Максимальный вес [min: 5.0, max: 3.0]"
            )
            .put(new CellPosition(SheetType.DELIVERY, "G", 5), "Не заполнено")
            .put(new CellPosition(SheetType.DELIVERY, "H", 6), "Не заполнено")
            .put(new CellPosition(SheetType.DELIVERY, "I", 7), "Не заполнено")
            .put(new CellPosition(SheetType.DELIVERY, "J", 8), "Не заполнено")
            .put(new CellPosition(SheetType.DELIVERY, "K", 9), "Не заполнено")
            .put(new CellPosition(SheetType.DELIVERY, "L", 10), "Не заполнено")
            .put(new CellPosition(SheetType.DELIVERY, "M", 11), "Не заполнено")
            .put(new CellPosition(SheetType.DELIVERY, "N", 12), "Не заполнено")
            .put(new CellPosition(SheetType.DELIVERY, "L", 4), "Значение ячейки не должно быть отрицательным")
            .put(new CellPosition(SheetType.DELIVERY, "K", 4), "Значение ячейки не должно быть отрицательным")
            .put(
                new CellPosition(SheetType.DELIVERY, "I", 4),
                "Минимальный срок больше чем Максимальный срок [min: 5, max: 3]"
            )
            .build();
    }

    @Nonnull
    private Map<CellPosition, LocationRaw> buildLocations() {
        ImmutableMap.Builder<CellPosition, LocationRaw> mapBuilder = new ImmutableMap.Builder<>();
        mapBuilder.put(new CellPosition(SheetType.SERVICES, "A", 2), MOSCOW_LOCATION);
        mapBuilder.put(new CellPosition(SheetType.SERVICES, "A", 4), MOSCOW_LOCATION);
        mapBuilder.put(new CellPosition(SheetType.SERVICES, "D", 2), BARNAUL_LOCATION);
        mapBuilder.put(new CellPosition(SheetType.SERVICES, "D", 3), SARANSK_LOCATION);
        mapBuilder.put(new CellPosition(SheetType.SERVICES, "D", 4), UNKNOWN_CITY_LOCATION);

        for (int i = 2; i < 20; i++) {
            mapBuilder.put(new CellPosition(SheetType.DELIVERY, "A", i), MOSCOW_LOCATION);
        }
        mapBuilder.put(new CellPosition(SheetType.DELIVERY, "D", 2), BARNAUL_LOCATION);
        for (int i = 4; i < 8; i++) {
            mapBuilder.put(new CellPosition(SheetType.DELIVERY, "D", i), BARNAUL_LOCATION);
        }
        for (int i = 8; i < 14; i++) {
            mapBuilder.put(new CellPosition(SheetType.DELIVERY, "D", i), SARANSK_LOCATION);
        }
        for (int i = 14; i < 20; i++) {
            mapBuilder.put(new CellPosition(SheetType.DELIVERY, "D", i), UNKNOWN_CITY_LOCATION);
        }
        return mapBuilder.build();
    }
}
