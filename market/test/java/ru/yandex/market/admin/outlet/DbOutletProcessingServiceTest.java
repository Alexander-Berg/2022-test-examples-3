package ru.yandex.market.admin.outlet;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;

import org.apache.http.entity.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.common.geocoder.client.GeoClient;
import ru.yandex.common.geocoder.model.response.AddressInfo;
import ru.yandex.common.geocoder.model.response.AreaInfo;
import ru.yandex.common.geocoder.model.response.Boundary;
import ru.yandex.common.geocoder.model.response.CountryInfo;
import ru.yandex.common.geocoder.model.response.GeoObject;
import ru.yandex.common.geocoder.model.response.Kind;
import ru.yandex.common.geocoder.model.response.LocalityInfo;
import ru.yandex.common.geocoder.model.response.SimpleGeoObject;
import ru.yandex.common.geocoder.model.response.ToponymInfo;
import ru.yandex.market.admin.FunctionalTest;
import ru.yandex.market.common.test.db.DbUnitDataSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Функциональные тесты на {@link DbOutletProcessingService}
 */
class DbOutletProcessingServiceTest extends FunctionalTest {

    private static final String CITY_NAME = "Санкт-Петербург";
    private static final String WRONG_CITY = "Москва";

    private final static GeoObject GEO_OBJECT = SimpleGeoObject.newBuilder()
            .withToponymInfo(ToponymInfo.newBuilder()
                    .withGeoid("2")
                    .withKind(Kind.LOCALITY)
                    .build()
            )
            .withAddressInfo(AddressInfo.newBuilder()
                    .withCountryInfo(CountryInfo
                            .newBuilder()
                            .build()
                    )
                    .withAreaInfo(AreaInfo.newBuilder()
                            .build()
                    )
                    .withLocalityInfo(LocalityInfo.newBuilder()
                            .build()
                    )
                    .withAddressLine(CITY_NAME).build()
            )
            .withBoundary(Boundary.newBuilder()
                    .build()
            )
            .build();

    @Autowired
    private OutletUploadDao outletUploadDao;

    @Autowired
    private DbOutletProcessor dbOutletProcessor;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private GeoClient geoClient;

    @Autowired
    @Qualifier("checkPendingFilesExecutorService")
    private ScheduledExecutorService checkPendingFilesExecutorService;

    @TempDir
    public File tempFolder;

    private DbOutletProcessingService outletProcessingService;

    private ExecutorService executorService;

    @BeforeEach
    void setUp() {
        when(geoClient.find(eq(CITY_NAME), any())).thenReturn(List.of(GEO_OBJECT));
        when(geoClient.find(eq(WRONG_CITY), any())).thenThrow(new QueryTimeoutException("Ошибка получение данных"));

        executorService = mock(ExecutorService.class);
        outletProcessingService = new DbOutletProcessingService(
                outletUploadDao, dbOutletProcessor, transactionTemplate, executorService,
                checkPendingFilesExecutorService, tempFolder);

        doAnswer(args -> {
            ((Runnable) args.getArgument(0)).run();
            return null;
        }).when(executorService).execute(any());
    }

    @Test
    @DisplayName("Обработка корректного файла аутлетов")
    @DbUnitDataSet(before = "processor/csv/outletProcessing.before.csv", after = "processor/csv/outletProcessing.noError.after.csv")
    void testProcessFileNoError() throws IOException {
        processFile("outlets-noError.xlsx");
    }

    @Test
    @DisplayName("Обработка файла аутлетов с часами без нулей в начале(например 8:00 вместо 08:00)")
    @DbUnitDataSet(before = "processor/csv/outletProcessing.before.csv",
            after = "processor/csv/outletProcessing.testProcessHoursWithoutLeadingZeroes.after.csv")
    void testProcessHoursWithoutLeadingZeroes() throws IOException {
        processFile("testProcessHoursWithoutLeadingZeroes.xlsx");
    }

    @Test
    @DisplayName("Обработка корректного файла аутлетов с незаполненным полем storagePeriod")
    @DbUnitDataSet(before = "processor/csv/outletProcessing.before.csv", after = "processor/csv/outletProcessing.emptyStoragePeriod.after.csv")
    void testProcessEmptyStoragePeriod() throws IOException {
        processFile("outlets-emptyStoragePeriod.xlsx");
    }

    @Test
    @DisplayName("Проверка проставления поля storagePeriod")
    @DbUnitDataSet(before = "processor/csv/outletProcessing.before.csv", after = "processor/csv/outletProcessing.storagePeriod.after.csv")
    void testProcessStoragePeriod() throws IOException {
        processFile("outlets-storagePeriod.xlsx");
    }

    @Test
    @DisplayName("Обработка файла аутлетов с одной ошибкой")
    @DbUnitDataSet(before = "processor/csv/outletProcessing.before.csv", after = "processor/csv/outletProcessing.withOneError.after.csv")
    void testProcessFileWithOneError() throws IOException {
        processFile("outlets-withOneError.xlsx");
    }

    @Test
    @DisplayName("Обработка файла аутлетов с несколькими ошибками")
    @DbUnitDataSet(before = "processor/csv/outletProcessing.before.csv", after = "processor/csv/outletProcessing.withErrors.after.csv")
    void testProcessFileWithErrors() throws IOException {
        processFile("outlets-withErrors.xlsx");
    }

    @Test
    @DisplayName("Обработка корректного файла аутлетов для оффлайн-магазина")
    @DbUnitDataSet(before = "processor/csv/outletProcessing.before.offline.csv", after = "processor/csv/outletProcessing.noError.offline.after.csv")
    void testProcessFileOfflineShop() throws IOException {
        processFile("outlets-noErrorOffline.xlsx");
    }

    @Test
    @DisplayName("Обработка файла аутлетов с ошибкой на уровне выполнения jdbc транзакции")
    @DbUnitDataSet(before = "processor/csv/outletProcessing.before.csv", after = "processor/csv/outletProcessing.jdbcError.after.csv")
    void run_jdbcError_exceptionStatus() throws IOException {
        processFile("outlets-jdbcException.xlsx");
    }

    @Test
    @DisplayName("Обработка файла с неожиданной ошибкой при проверке валидности аутлетов")
    @DbUnitDataSet(before = "processor/csv/outletProcessing.before.csv", after = "processor/csv/outletProcessing.unexpectedError.after.csv")
    void run_unexpectedError_exceptionStatus() throws IOException {
        final String unexpectedErrorCity = "Новосибирск";

        when(geoClient.find(eq(unexpectedErrorCity), any())).thenThrow(new RuntimeException());
        processFile("outlets-unexpectedError.xlsx");
    }

    @Test
    @DisplayName("Обработка файла с ошибкой отсутствующего расписания")
    @DbUnitDataSet(before = "processor/csv/outletProcessing.before.csv", after = "processor/csv/outletProcessing.scheduleIsMissing.after.csv")
    void run_scheduleIsMissingError_exceptionStatus() throws IOException {
        processFile("outlets-scheduleIsMissing.xlsx");
    }

    @Test
    @DisplayName("Обработка файла аутлетов с ошибкой в номере телефона")
    @DbUnitDataSet(before = "processor/csv/outletProcessing.before.csv", after = "processor/csv/outletProcessing.wrongPhone.after.csv")
    void run_wrongPhone_errorStatus() throws IOException {
        processFile("outlets-wrongPhone.xlsx");
    }

    @Test
    @DisplayName("Обработка файла аутлетов, который не получилось отправить в обработку в момент запроса")
    @DbUnitDataSet(before = "processor/csv/outletProcessing.before.csv", after = "processor/csv/outletProcessing.noError.after.csv")
    void testProcessPendingFileNoError() {
        // после загрузки файла на диск не запускаем обработку, для это выбрасываем исключение
        doThrow(RuntimeException.class).when(executorService).execute(any());
        assertThatThrownBy(() -> processFile("outlets-noError.xlsx")).isInstanceOf(RuntimeException.class);

        doAnswer(args -> {
            ((Runnable) args.getArgument(0)).run();
            return null;
        }).when(executorService).execute(any());
        // файл должен найтись и обработаться
        outletProcessingService.checkPendingFiles();
    }

    private void processFile(String fileName) throws IOException {
        MockMultipartFile file = getExcelFile(fileName);
        outletProcessingService.processFile(1001, -2, true, file);
        assertThat(outletProcessingService.uploadIdsInProgressSize()).isEqualTo(0);
    }

    private MockMultipartFile getExcelFile(String file) throws IOException {
        try (InputStream stream = getClass().getResourceAsStream("processor/xls/" + file)) {
            return new MockMultipartFile(file, file, ContentType.APPLICATION_XML.getMimeType(), stream);
        }
    }
}
