package ru.yandex.market.logistics.tarifficator.service;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.geocoder.client.GeoClient;
import ru.yandex.common.geocoder.model.request.GeoSearchParams;
import ru.yandex.common.geocoder.model.response.AddressInfo;
import ru.yandex.common.geocoder.model.response.AreaInfo;
import ru.yandex.common.geocoder.model.response.Boundary;
import ru.yandex.common.geocoder.model.response.Component;
import ru.yandex.common.geocoder.model.response.CountryInfo;
import ru.yandex.common.geocoder.model.response.Kind;
import ru.yandex.common.geocoder.model.response.LocalityInfo;
import ru.yandex.common.geocoder.model.response.Precision;
import ru.yandex.common.geocoder.model.response.SimpleGeoObject;
import ru.yandex.common.geocoder.model.response.ToponymInfo;
import ru.yandex.market.common.mds.s3.client.content.consumer.StreamCopyContentConsumer;
import ru.yandex.market.common.mds.s3.client.exception.MdsS3Exception;
import ru.yandex.market.common.mds.s3.client.model.ResourceLocation;
import ru.yandex.market.common.mds.s3.client.service.api.MdsS3Client;
import ru.yandex.market.common.mds.s3.client.service.factory.ResourceLocationFactory;
import ru.yandex.market.logistics.tarifficator.AbstractContextualTest;
import ru.yandex.market.logistics.tarifficator.exception.GeoSearchException;
import ru.yandex.market.logistics.tarifficator.jobs.producer.ActivatingPriceListProducer;
import ru.yandex.market.logistics.tarifficator.model.pricelist.raw.LocationRaw;
import ru.yandex.market.logistics.tarifficator.service.pricelist.PriceListFileService;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static java.lang.ClassLoader.getSystemResourceAsStream;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@DisplayName("Интеграционный тест сервиса PriceListFileService")
class PriceListFileServiceTest extends AbstractContextualTest {
    private static final double DELTA = 0.1;
    private static final String MDS_FILE_URL = "http://localhost:8080/price_list_log_document_1.xlsx";
    private static final Long PRICE_LIST_FILE_ID = 1L;
    private static final List<Long> EXPECTED_PRICE_LIST_IDS = List.of(1L, 2L);
    private static final List<Long> EXPECTED_OWN_DELIVERY_PRICE_LIST_IDS = List.of(1L);

    @Autowired
    private MdsS3Client mdsS3Client;
    @Autowired
    private GeoClient geoClient;
    @Autowired
    private PriceListFileService priceListFileService;
    @Autowired
    private ResourceLocationFactory resourceLocationFactory;
    @Autowired
    private ActivatingPriceListProducer activatingPriceListProducer;

    @BeforeEach
    void beforeEach() throws MalformedURLException {
        mockGeoSearchClient();
        doNothing().when(activatingPriceListProducer).produceTasks(anyList());
        when(mdsS3Client.getUrl(any())).thenReturn(new URL(MDS_FILE_URL));
        when(resourceLocationFactory.createLocation("price_list_log_document_2.xlsx")).thenAnswer(invocation ->
            ResourceLocation.create("tarifficator", invocation.getArgument(0, String.class))
        );
    }

    @AfterEach
    void afterEach() {
        verifyNoMoreInteractions(mdsS3Client);
        verifyNoMoreInteractions(activatingPriceListProducer);
    }

    @Test
    @DisplayName("Обработать минимальный прайс-лист файл")
    @DatabaseSetup("/controller/price-list-files/db/after/price-list-upload-success.xml")
    @ExpectedDatabase(
        value = "/service/price-list-files/db/after/processed-minimal-price-list.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/history-events/process-price-list-success-history-events.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void processFileMinimalSuccess() {
        mockMdsS3Client("service/price-list-files/xlsx/minimal-price-list.xlsx");
        priceListFileService.processFile(PRICE_LIST_FILE_ID);
        verifyMdsS3Client();
        verify(mdsS3Client).getUrl(ResourceLocation.create("tarifficator", "price_list_log_document_2.xlsx"));
        verify(mdsS3Client).upload(any(), any());
        verify(activatingPriceListProducer).produceTasks(eq(EXPECTED_PRICE_LIST_IDS));
    }

    @Test
    @DisplayName("Обработать прайс-лист файл в котором не только населенные пункты")
    @DatabaseSetup("/controller/price-list-files/db/after/price-list-upload-success.xml")
    @ExpectedDatabase(
        value = "/service/price-list-files/db/after/processed-minimal-price-list-not-only-locality.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/history-events/process-price-list-success-history-events.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void processFileMinimalNotOnlyLocalitySuccess() {
        mockMdsS3Client("service/price-list-files/xlsx/minimal-price-list-not-only-locality.xlsx");
        priceListFileService.processFile(PRICE_LIST_FILE_ID);
        verifyMdsS3Client();
        verify(mdsS3Client).getUrl(ResourceLocation.create("tarifficator", "price_list_log_document_2.xlsx"));
        verify(mdsS3Client).upload(any(), any());
        verify(activatingPriceListProducer).produceTasks(eq(EXPECTED_PRICE_LIST_IDS));
    }

    @Test
    @DisplayName("Обработать минимальный прайс-лист файл без указания способа доставки и типа тарифа")
    @DatabaseSetup("/controller/price-list-files/db/after/price-list-upload-success.xml")
    @ExpectedDatabase(
        value = "/service/price-list-files/db/after/processed-minimal-price-list.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void processFileWithoutDeliveryMethodAndTargetType() {
        mockMdsS3Client("service/price-list-files/xlsx/price-list-without-delivery-method-and-target-type.xlsx");
        priceListFileService.processFile(PRICE_LIST_FILE_ID);
        verifyMdsS3Client();
        verify(mdsS3Client).getUrl(ResourceLocation.create("tarifficator", "price_list_log_document_2.xlsx"));
        verify(mdsS3Client).upload(any(), any());
        verify(activatingPriceListProducer).produceTasks(eq(EXPECTED_PRICE_LIST_IDS));
    }

    @Test
    @DisplayName("Обработать минимальный прайс-лист файл своей доставки магазина")
    @DatabaseSetup("/service/price-list-files/db/before/own-delivery-price-list-uploaded.xml")
    @ExpectedDatabase(
        value = "/service/price-list-files/db/after/processed-minimal-price-list-own-delivery.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/history-events/process-price-list-success-history-events.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void processFileMinimalOwnDeliverySuccess() {
        mockMdsS3Client("service/price-list-files/xlsx/minimal-price-list-own-delivery.xlsx");
        priceListFileService.processFile(PRICE_LIST_FILE_ID);
        verifyMdsS3Client();
        verify(mdsS3Client).getUrl(ResourceLocation.create("tarifficator", "price_list_log_document_2.xlsx"));
        verify(mdsS3Client).upload(any(), any());
        verify(activatingPriceListProducer).produceTasks(eq(EXPECTED_OWN_DELIVERY_PRICE_LIST_IDS));
    }

    @Test
    @DisplayName("Успешно обработать минимальный прайс-лист файл с ограничением направлений своей доставки магазина")
    @DatabaseSetup(
        "/service/price-list-files/db/before/own-delivery-price-list-direction-restriction-uploaded-success.xml"
    )
    @ExpectedDatabase(
        value = "/service/price-list-files/db/after/processed-minimal-price-list-own-delivery.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/history-events/process-price-list-success-history-events.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void processFileMinimalOwnDeliveryWithDirectionRestrictionSuccess() {
        mockMdsS3Client("service/price-list-files/xlsx/minimal-price-list-own-delivery.xlsx");
        priceListFileService.processFile(PRICE_LIST_FILE_ID);
        verifyMdsS3Client();
        verify(mdsS3Client).getUrl(ResourceLocation.create("tarifficator", "price_list_log_document_2.xlsx"));
        verify(mdsS3Client).upload(any(), any());
        verify(activatingPriceListProducer).produceTasks(eq(EXPECTED_OWN_DELIVERY_PRICE_LIST_IDS));
    }

    @Test
    @DisplayName("Обработать минимальный прайс-лист файл с превышенным количеством направлений своей доставки магазина")
    @DatabaseSetup(
        "/service/price-list-files/db/before/own-delivery-price-list-direction-restriction-uploaded-exceeded.xml"
    )
    @ExpectedDatabase(
        value = "/service/price-list-files/db/after/processed-own-delivery-pricelist-with-exceeded-directions.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void processFileMinimalOwnDeliveryExceededNumberOfDirections() {
        mockMdsS3Client("service/price-list-files/xlsx/minimal-price-list-own-delivery.xlsx");
        priceListFileService.processFile(PRICE_LIST_FILE_ID);
        verifyMdsS3Client();
    }

    @Test
    @DisplayName("Обработать прайс-лист файл")
    @DatabaseSetup("/service/price-list-files/db/before/own-delivery-price-list-uploaded.xml")
    @ExpectedDatabase(
        value = "/service/price-list-files/db/after/processed-price-list.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/history-events/process-price-list-success-history-events.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void processFileSuccess() {
        mockMdsS3Client("service/price-list-files/xlsx/price-list.xlsx");
        priceListFileService.processFile(PRICE_LIST_FILE_ID);
        verifyMdsS3Client();
        verify(mdsS3Client).getUrl(ResourceLocation.create("tarifficator", "price_list_log_document_2.xlsx"));
        verify(mdsS3Client).upload(any(), any());
        verify(activatingPriceListProducer).produceTasks(eq(EXPECTED_OWN_DELIVERY_PRICE_LIST_IDS));
    }

    @Test
    @DisplayName("Обработать прайс-лист файл с дублированием прайс-листа общего тарифа с одной стоимостью")
    @DatabaseSetup("/service/price-list-files/db/before/general-one-price-delivery-price-list-uploaded.xml")
    @ExpectedDatabase(
        value = "/service/price-list-files/db/after/processed-price-list-with-clone.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/history-events/process-price-list-success-history-events.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void processFileWithClone() {
        mockMdsS3Client("service/price-list-files/xlsx/price-list.xlsx");
        priceListFileService.processFile(PRICE_LIST_FILE_ID);
        verifyMdsS3Client();
        verify(mdsS3Client).getUrl(ResourceLocation.create("tarifficator", "price_list_log_document_2.xlsx"));
        verify(mdsS3Client).upload(any(), any());
        verify(activatingPriceListProducer).produceTasks(eq(EXPECTED_PRICE_LIST_IDS));
    }

    @Test
    @DisplayName("Обработать прайс-лист файл дважды")
    @DatabaseSetup("/service/price-list-files/db/before/own-delivery-price-list-uploaded.xml")
    @ExpectedDatabase(
        value = "/service/price-list-files/db/after/processed-price-list.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/history-events/process-price-list-success-history-events.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void processFileTwiceSuccess() {
        mockMdsS3Client("service/price-list-files/xlsx/price-list.xlsx");
        priceListFileService.processFile(PRICE_LIST_FILE_ID);
        priceListFileService.processFile(PRICE_LIST_FILE_ID);

        verify(mdsS3Client, times(2)).download(
            eq(createResourceLocation()),
            any(StreamCopyContentConsumer.class)
        );
        verify(mdsS3Client).getUrl(ResourceLocation.create("tarifficator", "price_list_log_document_2.xlsx"));
        verify(mdsS3Client).upload(any(), any());
        verify(activatingPriceListProducer).produceTasks(eq(EXPECTED_OWN_DELIVERY_PRICE_LIST_IDS));
    }

    @Test
    @DisplayName("Обработать прайс-лист файл c дубликатами направлений")
    @DatabaseSetup("/service/price-list-files/db/before/own-delivery-price-list-uploaded.xml")
    @ExpectedDatabase(
        value = "/service/price-list-files/db/after/processed-price-list-with-duplicates.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/history-events/process-price-list-partial-success-history-events.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void processFileWithDuplicates() {
        mockMdsS3Client("service/price-list-files/xlsx/price-list-with-duplicates.xlsx");
        priceListFileService.processFile(PRICE_LIST_FILE_ID);
        verifyMdsS3Client();
        verify(mdsS3Client).getUrl(ResourceLocation.create("tarifficator", "price_list_log_document_2.xlsx"));
        verify(mdsS3Client).upload(any(), any());
    }

    @Test
    @DisplayName("Обработать прайс-лист файл без направлений доставки")
    @DatabaseSetup("/controller/price-list-files/db/after/price-list-upload-success.xml")
    @ExpectedDatabase(
        value = "/service/price-list-files/db/after/price-list-with-no-delivery-directions.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void processFileWithoutDeliveryDirections() {
        mockMdsS3Client("service/price-list-files/xlsx/price-list-without-delivery-directions.xlsx");
        priceListFileService.processFile(PRICE_LIST_FILE_ID);
        verifyMdsS3Client();
    }

    @Test
    @DisplayName("Обработать прайс-лист файл c отсутствующими обязательными колонками")
    @DatabaseSetup("/service/price-list-files/db/before/own-delivery-price-list-uploaded.xml")
    @ExpectedDatabase(
        value = "/service/price-list-files/db/after/processed-invalid-price-list.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/history-events/process-price-list-error-history-events.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void processInvalidFile() {
        mockMdsS3Client("service/price-list-files/xlsx/error-price-list.xlsx");
        priceListFileService.processFile(PRICE_LIST_FILE_ID);
        verifyMdsS3Client();
    }

    @Test
    @DisplayName("Обработать прайс-лист файл c ошибками на странице тарифа")
    @DatabaseSetup("/service/price-list-files/db/before/own-delivery-price-list-uploaded.xml")
    @ExpectedDatabase(
        value = "/service/price-list-files/db/after/processed-price-list-with-invalid-tariff-sheet.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/history-events/process-price-list-error-history-events.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void processFileWithInvalidTariffSheet() {
        mockMdsS3Client("service/price-list-files/xlsx/price-list-with-errors-in-tariff-sheet.xlsx");
        priceListFileService.processFile(PRICE_LIST_FILE_ID);
        verifyMdsS3Client();
    }

    @Test
    @DisplayName("Обработать прайс-лист файл c ошибками в некоторых строках")
    @DatabaseSetup("/service/price-list-files/db/before/own-delivery-price-list-uploaded.xml")
    @ExpectedDatabase(
        value = "/service/price-list-files/db/after/processed-price-list-with-errors-in-rows.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/history-events/process-price-list-partial-success-history-events.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void processFilePartialSuccess() {
        mockMdsS3Client("service/price-list-files/xlsx/price-list-with-errors-in-rows.xlsx");
        priceListFileService.processFile(PRICE_LIST_FILE_ID);
        verifyMdsS3Client();
        verify(mdsS3Client).getUrl(ResourceLocation.create("tarifficator", "price_list_log_document_2.xlsx"));
        verify(mdsS3Client).upload(any(), any());
    }

    @Test
    @DisplayName("Обработать прайс-лист без некоторых ячеек")
    @DatabaseSetup("/controller/price-list-files/db/after/price-list-upload-success.xml")
    @ExpectedDatabase(
        value = "/service/price-list-files/db/after/price-list-without-some-cells.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/history-events/process-price-list-partial-success-history-events.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void processFileWithoutSomeCells() {
        mockMdsS3Client("service/price-list-files/xlsx/price-list-without-some-cells.xlsx");
        priceListFileService.processFile(PRICE_LIST_FILE_ID);
        verifyMdsS3Client();
        verify(mdsS3Client).getUrl(ResourceLocation.create("tarifficator", "price_list_log_document_2.xlsx"));
        verify(mdsS3Client).upload(any(), any());
    }

    @Test
    @DisplayName("Ошибка при скачивании файла из хранилища прокидывается выше")
    @DatabaseSetup("/service/price-list-files/db/before/own-delivery-price-list-uploaded.xml")
    @ExpectedDatabase(
        value = "/history-events/price-list-file-failed-attempt-history-event.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void unableToDownloadPriceList() {
        doThrow(new MdsS3Exception("Could not process operation")).when(mdsS3Client).download(any(), any());
        softly.assertThatThrownBy(() -> priceListFileService.processFile(PRICE_LIST_FILE_ID))
            .isInstanceOf(MdsS3Exception.class);
        verifyMdsS3Client();
    }

    @Test
    @DisplayName("Ошибка при получении geo идентификатора")
    @DatabaseSetup("/service/price-list-files/db/before/own-delivery-price-list-uploaded.xml")
    @ExpectedDatabase(
        value = "/history-events/price-list-file-failed-attempt-history-event.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void unableToFindGeoLocation() {
        doThrow(new GeoSearchException("Could not process operation", new Throwable()))
            .when(mdsS3Client).download(any(), any());

        softly.assertThatThrownBy(() -> priceListFileService.processFile(PRICE_LIST_FILE_ID))
            .isInstanceOf(GeoSearchException.class);

        verifyMdsS3Client();
    }

    private void mockLocation(
        LocationRaw locationRaw,
        String address,
        GeoSearchParams geoSearchParams,
        int geoId,
        List<Component> components
    ) {
        when(geoClient.find(eq(address), argThat(argument -> geoSearchParamsEquals(geoSearchParams, argument))))
            .thenReturn(List.of(
                SimpleGeoObject.newBuilder()
                    .withToponymInfo(ToponymInfo.newBuilder()
                        .withGeoid(String.valueOf(geoId))
                        .withKind(components.get(components.size() - 1).getKinds().get(0))
                        .withComponents(components)
                        .build()
                    )
                    .withAddressInfo(AddressInfo.newBuilder()
                        .withCountryInfo(CountryInfo.newBuilder().build())
                        .withAreaInfo(AreaInfo.newBuilder().build())
                        .withLocalityInfo(LocalityInfo.newBuilder().build())
                        .withAddressLine(locationRaw.getAddress())
                        .build()
                    )
                    .withBoundary(Boundary.newBuilder()
                        .build()
                    )
                    .build()
                )
            );
    }

    private boolean geoSearchParamsEquals(GeoSearchParams expected, GeoSearchParams actual) {
        return actual.getLimit() == expected.getLimit()
            && actual.getMinimalPrecision() == expected.getMinimalPrecision()
            && actual.getPreferredLanguage() == expected.getPreferredLanguage()
            && distance(expected, actual, GeoSearchParams.Coordinates::getLatitude) < DELTA
            && distance(expected, actual, GeoSearchParams.Coordinates::getLongitude) < DELTA;
    }

    private double distance(
        GeoSearchParams expected,
        GeoSearchParams actual,
        Function<GeoSearchParams.Coordinates, Double> getter
    ) {
        if (expected == null && actual == null) {
            return 0;
        }
        if (expected == null || actual == null) {
            return Double.MAX_VALUE;
        }
        GeoSearchParams.Coordinates expectedCenter = expected.getSearchAreaCenter();
        GeoSearchParams.Coordinates actualCenter = actual.getSearchAreaCenter();
        if (expectedCenter == null && actualCenter == null) {
            return 0;
        }
        if (expectedCenter == null || actualCenter == null) {
            return Double.MAX_VALUE;
        }
        return Math.abs(getter.apply(actualCenter) - getter.apply(expectedCenter));
    }

    private void mockMdsS3Client(String responseFilePath) {
        doAnswer(invocation -> {
            StreamCopyContentConsumer consumer = invocation.getArgument(1);
            InputStream is = Objects.requireNonNull(getSystemResourceAsStream(responseFilePath));
            consumer.consume(is);
            return null;
        }).when(mdsS3Client).download(any(), any());
    }

    private void verifyMdsS3Client() {
        verify(mdsS3Client).download(
            eq(createResourceLocation()),
            any(StreamCopyContentConsumer.class)
        );
    }

    private ResourceLocation createResourceLocation() {
        return resourceLocationFactory.createLocation("file_document_1.xlsx");
    }

    private void mockFederalCities() {
        mockLocation(
            LocationRaw.builder().address("Россия, Москва").build(),
            "Москва",
            GeoSearchParams.FIRST_ANY,
            213,
            List.of(
                new Component("Россия", List.of(Kind.COUNTRY)),
                new Component("Центральный федеральный округ", List.of(Kind.PROVINCE)),
                new Component("Москва", List.of(Kind.PROVINCE))
            )
        );
        mockLocation(
            LocationRaw.builder().address("Россия, Санкт-Петербург").build(),
            "Санкт-Петербург",
            GeoSearchParams.FIRST_ANY,
            2,
            List.of(
                new Component("Россия", List.of(Kind.COUNTRY)),
                new Component("Северо-Западный федеральный округ", List.of(Kind.PROVINCE)),
                new Component("Санкт-Петербург", List.of(Kind.PROVINCE))
            )
        );
        mockLocation(
            LocationRaw.builder().address("Россия, Москва").latitude(33d).longitude(66d).build(),
            "Москва",
            GeoSearchParams.builder()
                .withLimit(1)
                .withMinimalPrecision(Precision.ALL)
                .withPreferredLanguage(GeoSearchParams.Language.RU)
                .withSearchAreaCenter(66.0, 33.0)
                .build(),
            213,
            List.of(
                new Component("Россия", List.of(Kind.COUNTRY)),
                new Component("Центральный федеральный округ", List.of(Kind.PROVINCE)),
                new Component("Москва", List.of(Kind.PROVINCE))
            )
        );
    }

    private void mockGeoSearchClient() {
        mockFederalCities();
        mockLocation(
            LocationRaw.builder().address("Россия, Центральный федеральный округ").build(),
            "Россия, Центральный федеральный округ",
            GeoSearchParams.FIRST_ANY,
            3,
            List.of(new Component("Россия", List.of(Kind.COUNTRY)))
        );
        mockLocation(
            LocationRaw.builder().address("Россия, Новосибирск").build(),
            "Россия, Новосибирск",
            GeoSearchParams.FIRST_ANY,
            65,
            List.of(
                new Component("Россия", List.of(Kind.COUNTRY)),
                new Component("Сибирский федеральный округ", List.of(Kind.PROVINCE)),
                new Component("Новосибирская область", List.of(Kind.PROVINCE)),
                new Component("городской округ Новосибирск", List.of(Kind.AREA)),
                new Component("Новосибирск", List.of(Kind.LOCALITY))
            )
        );
        mockLocation(
            LocationRaw.builder().address("Россия, Новосибирская область, городской округ Новосибирск").build(),
            "Россия, Сибирский федеральный округ, Новосибирская область, городской округ Новосибирск",
            GeoSearchParams.FIRST_ANY,
            121037,
            List.of(
                new Component("Россия", List.of(Kind.COUNTRY)),
                new Component("Сибирский федеральный округ", List.of(Kind.PROVINCE)),
                new Component("Новосибирская область", List.of(Kind.PROVINCE)),
                new Component("городской округ Новосибирск", List.of(Kind.AREA))
            )
        );
        mockLocation(
            LocationRaw.builder().address("Россия, Алтайский край, Бийск").build(),
            "Россия, Алтайский край, Бийск",
            GeoSearchParams.FIRST_ANY,
            975,
            List.of(
                new Component("Россия", List.of(Kind.COUNTRY)),
                new Component("Сибирский федеральный округ", List.of(Kind.PROVINCE)),
                new Component("Алтайский край", List.of(Kind.PROVINCE)),
                new Component("городской округ Бийск", List.of(Kind.AREA)),
                new Component("Бийск", List.of(Kind.LOCALITY))
            )
        );
        mockLocation(
            LocationRaw.builder().address("Россия, Алтайский край, городской округ Бийск").build(),
            "Россия, Сибирский федеральный округ, Алтайский край, городской округ Бийск",
            GeoSearchParams.FIRST_ANY,
            121086,
            List.of(
                new Component("Россия", List.of(Kind.COUNTRY)),
                new Component("Сибирский федеральный округ", List.of(Kind.PROVINCE)),
                new Component("Алтайский край", List.of(Kind.PROVINCE)),
                new Component("городской округ Бийск", List.of(Kind.AREA))
            )
        );
        mockLocation(
            LocationRaw.builder().address("Россия, Амурская область, Благовещенск").build(),
            "Россия, Амурская область, Благовещенск",
            GeoSearchParams.FIRST_ANY,
            77,
            List.of(
                new Component("Россия", List.of(Kind.COUNTRY)),
                new Component("Дальневосточный федеральный округ", List.of(Kind.PROVINCE)),
                new Component("Амурская область", List.of(Kind.PROVINCE)),
                new Component("городской округ Благовещенск", List.of(Kind.AREA)),
                new Component("Благовещенск", List.of(Kind.DISTRICT))
            )
        );
        mockLocation(
            LocationRaw.builder().address("Россия, Амурская область, городской округ Благовещенск").build(),
            "Россия, Дальневосточный федеральный округ, Амурская область, городской округ Благовещенск",
            GeoSearchParams.FIRST_ANY,
            121173,
            List.of(
                new Component("Россия", List.of(Kind.COUNTRY)),
                new Component("Дальневосточный федеральный округ", List.of(Kind.PROVINCE)),
                new Component("Амурская область", List.of(Kind.PROVINCE)),
                new Component("городской округ Благовещенск", List.of(Kind.AREA))
            )
        );
        mockLocation(
            LocationRaw.builder().address("Россия, Алтайский край, Барнаул").build(),
            "Россия, Алтайский край, Барнаул",
            GeoSearchParams.FIRST_ANY,
            197,
            List.of(
                new Component("Россия", List.of(Kind.COUNTRY)),
                new Component("Сибирский федеральный округ", List.of(Kind.PROVINCE)),
                new Component("Алтайский край", List.of(Kind.PROVINCE)),
                new Component("муниципальное образование Город Барнаул", List.of(Kind.AREA)),
                new Component("Барнаул", List.of(Kind.LOCALITY))
            )
        );
        mockLocation(
            LocationRaw.builder().address("Россия, Алтайский край, муниципальное образование Город Барнаул").build(),
            "Россия, Сибирский федеральный округ, Алтайский край, муниципальное образование Город Барнаул",
            GeoSearchParams.FIRST_ANY,
            121085,
            List.of(
                new Component("Россия", List.of(Kind.COUNTRY)),
                new Component("Сибирский федеральный округ", List.of(Kind.PROVINCE)),
                new Component("Алтайский край", List.of(Kind.PROVINCE)),
                new Component("муниципальное образование Город Барнаул", List.of(Kind.AREA))
            )
        );
        mockLocation(
            LocationRaw.builder().address("Россия, Центральный федеральный округ").latitude(33d).longitude(66d).build(),
            "Россия, Центральный федеральный округ",
            GeoSearchParams.FIRST_ANY,
            3,
            List.of(new Component("Россия", List.of(Kind.COUNTRY)))
        );
    }
}
