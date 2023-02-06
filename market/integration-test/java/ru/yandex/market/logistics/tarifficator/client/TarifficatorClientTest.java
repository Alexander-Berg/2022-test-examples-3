package ru.yandex.market.logistics.tarifficator.client;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.annotation.Nonnull;

import com.google.common.collect.ImmutableSet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.client.ResponseActions;
import org.springframework.test.web.client.ResponseCreator;

import ru.yandex.market.logistics.tarifficator.model.dto.DeliveryOptionDto;
import ru.yandex.market.logistics.tarifficator.model.dto.DeliveryOptionsSearchRequestDto;
import ru.yandex.market.logistics.tarifficator.model.dto.MdsFileDto;
import ru.yandex.market.logistics.tarifficator.model.dto.PriceListFileDto;
import ru.yandex.market.logistics.tarifficator.model.dto.PriceListRestrictionsDto;
import ru.yandex.market.logistics.tarifficator.model.dto.ServiceCostDto;
import ru.yandex.market.logistics.tarifficator.model.dto.TagDto;
import ru.yandex.market.logistics.tarifficator.model.dto.TariffDto;
import ru.yandex.market.logistics.tarifficator.model.dto.TariffSearchFilter;
import ru.yandex.market.logistics.tarifficator.model.dto.TariffUpdateDto;
import ru.yandex.market.logistics.tarifficator.model.dto.WithdrawPriceListItemDto;
import ru.yandex.market.logistics.tarifficator.model.dto.WithdrawPriceListItemSearchDto;
import ru.yandex.market.logistics.tarifficator.model.dto.shop.PickupPointDeliveryRuleCreateRequest;
import ru.yandex.market.logistics.tarifficator.model.dto.shop.PickupPointDeliveryRuleResponse;
import ru.yandex.market.logistics.tarifficator.model.dto.shop.PickupPointDeliveryRuleUpdateRequest;
import ru.yandex.market.logistics.tarifficator.model.dto.shop.ShopLocalRegionUpdateRequest;
import ru.yandex.market.logistics.tarifficator.model.dto.tpl.CourierTariffDto;
import ru.yandex.market.logistics.tarifficator.model.dto.tpl.CourierTariffOptionDto;
import ru.yandex.market.logistics.tarifficator.model.dto.tpl.CourierTariffPriceDto;
import ru.yandex.market.logistics.tarifficator.model.enums.DeliveryMethod;
import ru.yandex.market.logistics.tarifficator.model.enums.FileExtension;
import ru.yandex.market.logistics.tarifficator.model.enums.FileType;
import ru.yandex.market.logistics.tarifficator.model.enums.PriceListStatus;
import ru.yandex.market.logistics.tarifficator.model.enums.PricingType;
import ru.yandex.market.logistics.tarifficator.model.enums.ServiceType;
import ru.yandex.market.logistics.tarifficator.model.enums.TariffType;
import ru.yandex.market.logistics.tarifficator.model.enums.shop.PickupPointDeliveryRuleStatus;
import ru.yandex.market.logistics.tarifficator.model.enums.tpl.CourierTariffStatus;
import ru.yandex.market.logistics.util.client.HttpTemplate;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.ClassLoader.getSystemResourceAsStream;
import static org.apache.commons.io.IOUtils.toByteArray;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.MULTIPART_FORM_DATA;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static ru.yandex.market.logistics.tarifficator.model.enums.FileType.TPL_COURIER_TARIFF;

@DisplayName("Интеграционный тест клиента тарификатора")
class TarifficatorClientTest extends AbstractClientTest {

    private static final String TEST_SERVICE_TICKET = "test-service-ticket";
    private static final String TEST_USER_TICKET = "test-user-ticket";

    @Autowired
    private TarifficatorClient tarifficatorClient;

    @DisplayName("Получение всех тарифов")
    @Test
    void getTariffs() throws IOException {
        prepareMockGetRequest("/tariffs", "response/tariffs.json");
        List<TariffDto> tariffs = tarifficatorClient.getTariffs();

        checkAllTariffs(tariffs);
    }

    @DisplayName("Поиск тарифов")
    @Test
    void searchTariffs() throws IOException {
        prepareMockRequest("/tariffs", "response/tariffs.json", HttpMethod.PUT)
            .andExpect(content().json(getFileContent("request/search_tariffs_filter.json"), true));
        TariffSearchFilter filter = new TariffSearchFilter()
            .setPartnerIds(ImmutableSet.of(12L, 32L));
        filter
            .setTariffId(1L)
            .setDeliveryMethod(DeliveryMethod.COURIER)
            .setEnabled(true)
            .setName("Курьер Василий")
            .setType(TariffType.OWN_DELIVERY);
        List<TariffDto> tariffs = tarifficatorClient.searchTariffs(filter);

        checkAllTariffs(tariffs);
    }

    @DisplayName("Поиск тарифов по пустому списку партнеров")
    @Test
    void searchTariffsEmptyPartners() {
        List<TariffDto> tariffs = tarifficatorClient.searchTariffs(
            new TariffSearchFilter().setPartnerIds(Set.of())
        );
        softly.assertThat(tariffs).isEmpty();
    }

    private void checkAllTariffs(List<TariffDto> tariffs) {
        softly.assertThat(tariffs).hasSize(2);
        softly.assertThat(tariffs)
            .usingRecursiveFieldByFieldElementComparator()
            .isEqualTo(List.of(mockTariff(100L), mockTariff(200L)));
    }

    @DisplayName("Получение тарифа по идентификатору")
    @Test
    void getTariff() throws IOException {
        prepareMockGetRequest("/tariffs/2", "response/tariff_2.json");
        TariffDto tariff = tarifficatorClient.getTariff(2);
        softly.assertThatObject(tariff).isEqualToComparingFieldByFieldRecursively(mockTariff(200L));
    }

    @DisplayName("Получение существующего тарифа по идентификатору через Optional")
    @Test
    void getTariffFromOptionalPresent() throws IOException {
        prepareMockGetRequest("/tariffs/2", "response/tariff_2.json");
        Optional<TariffDto> optionalTariff = tarifficatorClient.getOptionalTariff(2);
        softly.assertThat(optionalTariff).hasValueSatisfying(
            tariff -> softly.assertThat(tariff).isEqualToComparingFieldByFieldRecursively(mockTariff(200L))
        );
    }

    @DisplayName("Получение несуществующего тарифа по идентификатору через Optional")
    @Test
    void getTariffFromOptionalAbsent() {

        mock.expect(requestTo(uri + "/tariffs/3"))
            .andExpect(method(HttpMethod.GET))
            .andExpect(content().contentType(APPLICATION_JSON))
            .andExpect(header(HttpTemplate.SERVICE_TICKET_HEADER, TEST_SERVICE_TICKET))
            .andExpect(header(HttpTemplate.USER_TICKET_HEADER, TEST_USER_TICKET))
            .andRespond(withStatus(NOT_FOUND));

        Optional<TariffDto> tariff = tarifficatorClient.getOptionalTariff(3);
        softly.assertThat(tariff).isEmpty();
    }

    @DisplayName("Создание нового тарифа")
    @Test
    void createTariff() throws IOException {
        expectedRequest(
            "/tariffs",
            HttpMethod.POST,
            "request/create_tariff.json",
            okJsonResponse("response/created_tariff.json")
        );

        TariffDto tariff = tarifficatorClient.createTariff(
            TariffDto.builder()
                .partnerId(300L)
                .deliveryMethod(DeliveryMethod.POST)
                .currency("RUB")
                .type(TariffType.GENERAL)
                .name("Почта России")
                .code("Почта России КОД")
                .enabled(true)
                .description("Почта РФ")
                .build()
        );

        softly.assertThatObject(tariff)
            .isEqualToComparingFieldByFieldRecursively(
                TariffDto.builder()
                    .id(1L)
                    .partnerId(100L)
                    .deliveryMethod(DeliveryMethod.COURIER)
                    .currency("RUB")
                    .type(TariffType.GENERAL)
                    .name("Курьерская доставка СДЭК")
                    .code("Курьерская доставка СДЭК КОД")
                    .enabled(true)
                    .description("Только для МКАД")
                    .build()
            );
    }

    @DisplayName("Обновление тарифа")
    @Test
    void updateTariff() throws IOException {
        expectedRequest(
            "/tariffs/1",
            HttpMethod.PUT,
            "request/update_tariff.json",
            okJsonResponse("response/updated_tariff.json")
        );

        TariffDto tariff = tarifficatorClient.updateTariff(
            1L,
            TariffUpdateDto.builder()
                .name("ПВЗ доставка СДЭК")
                .code("ПВЗ доставка СДЭК КОД")
                .enabled(true)
                .description("Пригород")
                .build()
        );

        softly.assertThatObject(tariff)
            .isEqualToComparingFieldByFieldRecursively(
                TariffDto.builder()
                    .id(1L)
                    .partnerId(101L)
                    .deliveryMethod(DeliveryMethod.PICKUP)
                    .currency("RUB")
                    .type(TariffType.GENERAL)
                    .name("ПВЗ для СДЭК")
                    .code("ПВЗ для СДЭК КОД")
                    .enabled(true)
                    .description("Пригород")
                    .build()
            );
    }

    @DisplayName("Получение всех тегов тарифа")
    @Test
    void getTags() throws IOException {
        prepareMockGetRequest("/tariffs/1/tags", "response/tariff_tags.json");

        List<TagDto> tags = tarifficatorClient.getTags(1L);
        assertTariffTags(tags);
    }

    @DisplayName("Получение всех тегов тарифа")
    @Test
    void updateTags() throws IOException {
        expectedRequest(
            "/tariffs/1/tags",
            HttpMethod.PUT,
            "request/update_tariff_tags.json",
            okJsonResponse("response/tariff_tags.json")
        );

        List<TagDto> tags = tarifficatorClient.updateTags(1L, ImmutableSet.of("DAAS", "BERU"));
        assertTariffTags(tags);
    }

    @DisplayName("Получить загруженные файлы прайс-листа")
    @Test
    void getPriceListFile() throws IOException {
        prepareMockGetRequest("/price-list/files/1", "response/price_list_file_1.json");
        PriceListFileDto priceListFile = tarifficatorClient.getPriceListFile(1L);

        softly.assertThatObject(priceListFile)
            .isEqualToComparingFieldByFieldRecursively(
                new PriceListFileDto()
                    .setTariffId(1L)
                    .setId(1L)
                    .setFile(createMdsFile(1L))
                    .setCreatedAt(newUtcInstant("2019-07-22T11:00:00"))
                    .setUpdatedAt(newUtcInstant("2019-07-22T12:00:00"))
                    .setStatus(PriceListStatus.SUCCESS)
            );
    }

    @DisplayName("Активация прайс-листа")
    @Test
    void activatePriceListFile() throws IOException {
        prepareMockRequest("/price-list/files/1/activate", "response/price_list_file_1.json", HttpMethod.POST);
        PriceListFileDto priceListFile = tarifficatorClient.activatePriceListFile(1L);

        softly.assertThatObject(priceListFile)
            .isEqualToComparingFieldByFieldRecursively(
                new PriceListFileDto()
                    .setTariffId(1L)
                    .setId(1L)
                    .setFile(createMdsFile(1L))
                    .setCreatedAt(newUtcInstant("2019-07-22T11:00:00"))
                    .setUpdatedAt(newUtcInstant("2019-07-22T12:00:00"))
                    .setStatus(PriceListStatus.SUCCESS)
            );
    }

    @DisplayName("Получить загруженные файлы прайс-листов тарифа")
    @Test
    void getPriceListFiles() throws IOException {
        prepareMockGetRequest("/price-list/files/tariff/1", "response/price_list_files.json");
        List<PriceListFileDto> priceListFiles = tarifficatorClient.getPriceListFiles(1L);

        softly.assertThatObject(priceListFiles)
            .isEqualToComparingFieldByFieldRecursively(
                List.of(
                    new PriceListFileDto()
                        .setTariffId(1L)
                        .setId(2L)
                        .setFile(createMdsFile(2L))
                        .setCreatedAt(newUtcInstant("2019-07-22T11:00:00"))
                        .setUpdatedAt(newUtcInstant("2019-07-22T12:00:00"))
                        .setStatus(PriceListStatus.SUCCESS),
                    new PriceListFileDto()
                        .setTariffId(1L)
                        .setId(1L)
                        .setFile(createMdsFile(1L))
                        .setCreatedAt(newUtcInstant("2019-07-22T11:00:00"))
                        .setUpdatedAt(newUtcInstant("2019-07-22T12:00:00"))
                        .setStatus(PriceListStatus.SUCCESS)
                )
            );
    }

    @Test
    @DisplayName("Загрузка файла прайс-листа")
    void uploadPriceListFile() throws IOException {
        //  todo проверить контент запроса, после возможности в спринге DELIVERY-17017
        //  https://github.com/spring-projects/spring-framework/pull/23772
        mock.expect(requestTo(uri + "/price-list/files/tariff/1"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(content().contentTypeCompatibleWith(MULTIPART_FORM_DATA))
            .andExpect(header(HttpTemplate.SERVICE_TICKET_HEADER, TEST_SERVICE_TICKET))
            .andExpect(header(HttpTemplate.USER_TICKET_HEADER, TEST_USER_TICKET))
            .andRespond(okJsonResponse("response/price_list_file_1.json"));

        try (InputStream file = getSystemResourceAsStream("request/price-list.xlsx")) {
            PriceListFileDto priceList = tarifficatorClient.uploadPriceListFile(
                1L,
                "my_file.xlsx",
                toByteArray(checkNotNull(file))
            );

            softly.assertThatObject(priceList)
                .isEqualToComparingFieldByFieldRecursively(
                    new PriceListFileDto()
                        .setTariffId(1L)
                        .setFile(createMdsFile(1L))
                        .setId(1L)
                        .setStatus(PriceListStatus.SUCCESS)
                        .setLogFile(null)
                        .setCreatedAt(Instant.parse("2019-07-22T11:00:00Z"))
                        .setUpdatedAt(Instant.parse("2019-07-22T12:00:00Z"))
                );
        }
    }

    @Test
    @DisplayName("Загрузка файла прайс-листа с ограничением на количество направлений в тарифе")
    void uploadPriceListFileWithDirectionCountRestriction() throws IOException {
        //  todo проверить контент запроса, после возможности в спринге DELIVERY-17017
        //  https://github.com/spring-projects/spring-framework/pull/23772
        mock.expect(requestTo(
            uri + "/price-list/files/tariff/1"
                + "?directionCountRestriction=1"
                + "&weightBreaksCountRestriction=10"
        ))
            .andExpect(method(HttpMethod.POST))
            .andExpect(content().contentTypeCompatibleWith(MULTIPART_FORM_DATA))
            .andExpect(header(HttpTemplate.SERVICE_TICKET_HEADER, TEST_SERVICE_TICKET))
            .andExpect(header(HttpTemplate.USER_TICKET_HEADER, TEST_USER_TICKET))
            .andRespond(okJsonResponse("response/price_list_file_1.json"));

        try (InputStream file = getSystemResourceAsStream("request/price-list.xlsx")) {
            PriceListFileDto priceList = tarifficatorClient.uploadPriceListFile(
                1L,
                "my_file.xlsx",
                toByteArray(checkNotNull(file)),
                new PriceListRestrictionsDto()
                    .setDirectionCountRestriction(1)
                    .setWeightBreaksCountRestriction(10)
            );

            softly.assertThatObject(priceList)
                .isEqualToComparingFieldByFieldRecursively(
                    new PriceListFileDto()
                        .setTariffId(1L)
                        .setFile(createMdsFile(1L))
                        .setId(1L)
                        .setStatus(PriceListStatus.SUCCESS)
                        .setLogFile(null)
                        .setCreatedAt(Instant.parse("2019-07-22T11:00:00Z"))
                        .setUpdatedAt(Instant.parse("2019-07-22T12:00:00Z"))
                );
        }
    }

    @DisplayName("Найти варианты доставки")
    @Test
    void findDeliveryOptions() throws IOException {
        expectedRequest(
            "/delivery-options/search",
            HttpMethod.PUT,
            "request/get_options.json",
            okJsonResponse("response/get_options.json")
        );

        final List<DeliveryOptionDto> deliveryOptions = tarifficatorClient.findDeliveryOptions(
            DeliveryOptionsSearchRequestDto.builder()
                .tariffIds(ImmutableSet.of(1L, 100L, 200L, 300L))
                .locationFrom(213)
                .locationTo(197)
                .date(LocalDateTime.of(2019, 8, 22, 11, 0, 0).toInstant(ZoneOffset.UTC))
                .weight(new BigDecimal("19"))
                .length(50)
                .width(30)
                .height(10)
                .isPublic(false)
                .build()
        );

        softly.assertThatObject(deliveryOptions)
            .isEqualToComparingFieldByFieldRecursively(
                List.of(
                    DeliveryOptionDto.builder()
                        .tariffId(1)
                        .minDays(4)
                        .maxDays(7)
                        .deliveryCost(new BigDecimal("178.00"))
                        .servicesCost(Set.of(
                            ServiceCostDto.builder()
                                .code(ServiceType.INSURANCE)
                                .pricingType(PricingType.PERCENT_COST)
                                .priceValue(new BigDecimal("0.005000"))
                                .minCost(new BigDecimal("0.00"))
                                .maxCost(new BigDecimal("1500.00"))
                                .build()
                        ))
                        .build()
                )
            );
    }

    @Test
    @DisplayName("Найти элемент заборного прайс-листа")
    void findWithdrawPriceListItem() throws IOException {
        expectedRequest(
            "/withdraw-price-list-item",
            HttpMethod.PUT,
            "request/get_withdraw_price_list_item.json",
            okJsonResponse("response/get_withdraw_price_list_item.json")
        );

        Optional<WithdrawPriceListItemDto> actualOptionalResponse = tarifficatorClient.searchWithdrawPriceListItemDto(
            new WithdrawPriceListItemSearchDto()
                .setLocationZoneId(1L)
                .setVolume(BigDecimal.valueOf(0.1))
        );

        softly.assertThat(actualOptionalResponse).hasValueSatisfying(
            actualResponse -> softly.assertThatObject(actualResponse).isEqualToComparingFieldByFieldRecursively(
                new WithdrawPriceListItemDto()
                    .setId(2L)
                    .setLocationZoneId(1L)
                    .setMinVolume(BigDecimal.valueOf(0.0))
                    .setMaxVolume(BigDecimal.valueOf(0.3))
                    .setCost(BigDecimal.valueOf(220))
            )
        );
    }

    @Test
    @DisplayName("Элемент заборного прайс-листа не найден")
    void findWithdrawPriceListItemIsEmpty() throws IOException {
        expectedRequest(
            "/withdraw-price-list-item",
            HttpMethod.PUT,
            "request/get_withdraw_price_list_item.json",
            withStatus(NOT_FOUND)
        );

        Optional<WithdrawPriceListItemDto> actualOptionalResponse = tarifficatorClient.searchWithdrawPriceListItemDto(
            new WithdrawPriceListItemSearchDto()
                .setLocationZoneId(1L)
                .setVolume(BigDecimal.valueOf(0.1))
        );

        softly.assertThat(actualOptionalResponse).isEmpty();
    }

    @Test
    @DisplayName("Добавление новой партнерской службы ПВЗ")
    void addTariffDestinationPartner() throws IOException {
        mock.expect(requestTo(uri + "/tariffs/destinationPartners/4"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(header(HttpTemplate.SERVICE_TICKET_HEADER, TEST_SERVICE_TICKET))
            .andExpect(header(HttpTemplate.USER_TICKET_HEADER, TEST_USER_TICKET))
            .andRespond(withStatus(OK));

        tarifficatorClient.addTariffDestinationPartner(4L);
    }

    @Test
    @DisplayName("Получение актуального курьерского тарифа")
    void getActualCourierTariff() throws IOException {
        prepareMockGetRequest(
            "/tpl-courier-tariff/actual?date=2020-01-01",
            "response/get_actual_tariff.json"
        );

        CourierTariffDto expectedCourierTariff = new CourierTariffDto()
            .setId(1L)
            .setStatus(CourierTariffStatus.SUCCESS)
            .setFile(
                new MdsFileDto(
                    1L,
                    "http://localhost:8080/tpl_courier_tariff_document_1.xlsx",
                    "originalFileName",
                    TPL_COURIER_TARIFF,
                    FileExtension.EXCEL
                )
            )
            .setCourierTariffOptions(List.of(
                new CourierTariffOptionDto()
                    .setFromDistance(131L)
                    .setToDistance(300L)
                    .setSortingCenterId(1L)
                    .setCourierCompanyId(2L)
                    .setPrice(
                        new CourierTariffPriceDto()
                            .setMinTariff(3500L)
                            .setStandardTariff(BigDecimal.valueOf(170.0))
                            .setBusinessTariff(BigDecimal.valueOf(85.0))
                            .setLockerTariff(BigDecimal.valueOf(170.0))
                            .setPvzTariff(BigDecimal.valueOf(170.0))
                            .setLockerBoxTariff(BigDecimal.valueOf(20.0))
                            .setPvzBoxTariff(BigDecimal.valueOf(20.0))
                    ),
                new CourierTariffOptionDto()
                    .setFromDistance(0L)
                    .setToDistance(130L)
                    .setSortingCenterId(1L)
                    .setCourierCompanyId(2L)
                    .setPrice(
                        new CourierTariffPriceDto()
                            .setMinTariff(3000L)
                            .setStandardTariff(BigDecimal.valueOf(150.0))
                            .setBusinessTariff(BigDecimal.valueOf(75.0))
                            .setLockerTariff(BigDecimal.valueOf(160.0))
                            .setPvzTariff(BigDecimal.valueOf(170.0))
                            .setLockerBoxTariff(BigDecimal.valueOf(20.0))
                            .setPvzBoxTariff(BigDecimal.valueOf(40.0))
                    )
            ));
        Optional<CourierTariffDto> actualCourierTariffDto = tarifficatorClient.getActualTplCourierTariff(
            LocalDate.of(2020, 1, 1)
        );

        softly.assertThat(actualCourierTariffDto).hasValueSatisfying(
            actualCourierTariff -> softly.assertThat(actualCourierTariff)
                .usingRecursiveComparison()
                .isEqualTo(expectedCourierTariff)
        );
    }

    @Test
    @DisplayName("Актуальный курьерский тариф не найден")
    void actualCourierTariffNotFound() {
        mock.expect(requestTo(uri + "/tpl-courier-tariff/actual?date=2020-01-01"))
            .andExpect(method(HttpMethod.GET))
            .andExpect(content().contentType(APPLICATION_JSON))
            .andExpect(header(HttpTemplate.SERVICE_TICKET_HEADER, TEST_SERVICE_TICKET))
            .andExpect(header(HttpTemplate.USER_TICKET_HEADER, TEST_USER_TICKET))
            .andRespond(withStatus(NOT_FOUND));

        softly.assertThat(tarifficatorClient.getActualTplCourierTariff(LocalDate.of(2020, 1, 1)))
            .isEmpty();
    }

    private void assertTariffTags(List<TagDto> tags) {
        softly.assertThat(tags).hasSize(2);
        softly.assertThatObject(tags.get(0))
            .isEqualToComparingFieldByFieldRecursively(
                TagDto.builder()
                    .tagName("BERU")
                    .description("Программа доставки на маркете BERU")
                    .build()
            );

        softly.assertThatObject(tags.get(1))
            .isEqualToComparingFieldByFieldRecursively(
                TagDto.builder()
                    .tagName("DAAS")
                    .description("Программа доставки как сервиса")
                    .build()
            );
    }

    @DisplayName("Создание правила доставки ПВЗ магазина")
    @Test
    void createPickupPointDeliveryRule() throws IOException {
        expectedRequest(
            "/shops/pickup-points/delivery-rule",
            HttpMethod.POST,
            "request/create_pickupPointDeliveryRule.json",
            okJsonResponse("response/created_pickupPointDeliveryRule.json")
        );
        PickupPointDeliveryRuleResponse response = tarifficatorClient.createPickupPointDeliveryRule(
            (PickupPointDeliveryRuleCreateRequest) new PickupPointDeliveryRuleCreateRequest()
                .setShopId(774L)
                .setLmsLogisticsPointId(2L)
                .setStatus(PickupPointDeliveryRuleStatus.ACTIVE)
                .setPickupPointType("PICKUP_POINT")
        );

        softly.assertThat(response)
            .usingRecursiveComparison()
            .ignoringFields("createdAt", "updatedAt")
            .isEqualTo(
                new PickupPointDeliveryRuleResponse()
                    .setId(1L)
                    .setShopId(774L)
                    .setLmsLogisticsPointId(2L)
                    .setStatus(PickupPointDeliveryRuleStatus.ACTIVE)
                    .setPickupPointType("PICKUP_POINT")
            );
    }

    @DisplayName("Обновление правила доставки ПВЗ магазина")
    @Test
    void updatePickupPointDeliveryRule() throws IOException {
        expectedRequest(
            "/shops/pickup-points/delivery-rule/1",
            HttpMethod.PUT,
            "request/update_pickupPointDeliveryRule.json",
            okJsonResponse("response/updated_pickupPointDeliveryRule.json")
        );
        PickupPointDeliveryRuleResponse response = tarifficatorClient.updatePickupPointDeliveryRule(
            1L,
            new PickupPointDeliveryRuleUpdateRequest()
                .setStatus(PickupPointDeliveryRuleStatus.ACTIVE)
                .setPickupPointType("TERMINAL")
                .setDaysFrom(0)
                .setDaysTo(2)
                .setOrderBeforeHour(2)
        );

        softly.assertThat(response)
            .usingRecursiveComparison()
            .ignoringFields("createdAt", "updatedAt")
            .isEqualTo(
                new PickupPointDeliveryRuleResponse()
                    .setId(1L)
                    .setShopId(774L)
                    .setLmsLogisticsPointId(1L)
                    .setStatus(PickupPointDeliveryRuleStatus.ACTIVE)
                    .setPickupPointType("TERMINAL")
                    .setDaysFrom(0)
                    .setDaysTo(2)
                    .setOrderBeforeHour(2)
            );
    }

    @Test
    @DisplayName("Удаление правила доставки ПВЗ магазина")
    void deletePickupPointDeliveryRule() {

        mock.expect(requestTo(uri + "/shops/pickup-points/delivery-rule/1"))
            .andExpect(method(HttpMethod.DELETE))
            .andExpect(header(HttpTemplate.SERVICE_TICKET_HEADER, TEST_SERVICE_TICKET))
            .andExpect(header(HttpTemplate.USER_TICKET_HEADER, TEST_USER_TICKET))
            .andRespond(withStatus(OK));

        softly.assertThatCode(() -> tarifficatorClient.deletePickupPointDeliveryRule(1))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Обновление локального региона магазина")
    void updateLocalDeliveryRegion() throws IOException {
        long shopId = 1L;
        long userId = 11L;
        expectedRequest(
            "/v2/shops/" + shopId + "/local-region?_user_id=" + userId,
            HttpMethod.PUT,
            "request/update_local_region.json",
            withStatus(OK).contentType(APPLICATION_JSON)
        );
        tarifficatorClient.updateShopLocalRegion(
            shopId,
            userId,
            new ShopLocalRegionUpdateRequest().setLocalRegion(213L)
        );
    }

    @Nonnull
    private MdsFileDto createMdsFile(long id) {
        return new MdsFileDto(
            id,
            "http://prce-list",
            "price-name",
            FileType.PRICE_LIST,
            FileExtension.EXCEL
        );
    }

    private Instant newUtcInstant(String datetime) {
        return LocalDateTime.parse(datetime).toInstant(ZoneOffset.UTC);
    }

    void prepareMockGetRequest(String path, String responseFile) throws IOException {
        prepareMockRequest(path, responseFile, HttpMethod.GET);
    }

    @Nonnull
    ResponseActions prepareMockRequest(String path, String responseFile, HttpMethod method) throws IOException {
        ResponseActions actions = mock.expect(requestTo(uri + path))
            .andExpect(method(method))
            .andExpect(content().contentType(APPLICATION_JSON))
            .andExpect(header(HttpTemplate.SERVICE_TICKET_HEADER, TEST_SERVICE_TICKET))
            .andExpect(header(HttpTemplate.USER_TICKET_HEADER, TEST_USER_TICKET));
        actions
            .andRespond(okJsonResponse(responseFile));
        return actions;
    }

    private void expectedRequest(
        String path,
        HttpMethod method,
        String requestFile,
        ResponseCreator taskResponseCreator
    ) throws IOException {
        ResponseActions actions = mock.expect(requestTo(uri + path))
            .andExpect(method(method))
            .andExpect(content().contentType(APPLICATION_JSON))
            .andExpect(content().json(getFileContent(requestFile)))
            .andExpect(header(HttpTemplate.SERVICE_TICKET_HEADER, TEST_SERVICE_TICKET))
            .andExpect(header(HttpTemplate.USER_TICKET_HEADER, TEST_USER_TICKET));

        if (taskResponseCreator != null) {
            actions.andRespond(taskResponseCreator);
        }
    }

    private ResponseCreator okJsonResponse(String responseFile) throws IOException {
        return withStatus(OK)
            .contentType(APPLICATION_JSON)
            .body(getFileContent(responseFile));
    }

    private TariffDto mockTariff(long id) {

        Map<Long, TariffDto> tariffDtoMap = Map.of(
            100L,
            TariffDto.builder()
                .id(100L)
                .partnerId(100L)
                .deliveryMethod(DeliveryMethod.COURIER)
                .currency("RUB")
                .type(TariffType.GENERAL)
                .name("Курьерская доставка СДЭК")
                .code("Курьерская доставка СДЭК КОД")
                .enabled(true)
                .description("Только для МКАД")
                .build(),
            200L,
            TariffDto.builder()
                .id(200L)
                .partnerId(200L)
                .deliveryMethod(DeliveryMethod.PICKUP)
                .currency("RUB")
                .type(TariffType.GENERAL)
                .name("ПВЗ СДЭК")
                .code("ПВЗ СДЭК КОД")
                .enabled(true)
                .description("Только для МКАД")
                .build()
        );
        return tariffDtoMap.get(id);
    }
}
