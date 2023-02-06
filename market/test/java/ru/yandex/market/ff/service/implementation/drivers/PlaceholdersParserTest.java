package ru.yandex.market.ff.service.implementation.drivers;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.jetbrains.annotations.NotNull;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.core.supplier.model.SupplierType;
import ru.yandex.market.ff.base.IntegrationTest;
import ru.yandex.market.ff.client.enums.RequestStatus;
import ru.yandex.market.ff.client.enums.RequestType;
import ru.yandex.market.ff.model.TypeSubtype;
import ru.yandex.market.ff.model.entity.RequestSubTypeEntity;
import ru.yandex.market.ff.model.entity.ShopRequest;
import ru.yandex.market.ff.model.entity.Supplier;
import ru.yandex.market.ff.model.entity.SupplierBusinessType;
import ru.yandex.market.ff.repository.BookedTimeSlotsRepository;
import ru.yandex.market.ff.service.CalendaringServiceClientWrapperService;
import ru.yandex.market.ff.service.ConcreteEnvironmentParamService;
import ru.yandex.market.ff.service.RequestSubTypeService;
import ru.yandex.market.ff.service.implementation.drivers.parsers.PlaceholdersParser;
import ru.yandex.market.logistics.calendaring.client.dto.BookingResponseV2;
import ru.yandex.market.logistics.calendaring.client.dto.enums.BookingStatus;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

public class PlaceholdersParserTest extends IntegrationTest {

    private BookedTimeSlotsRepository bookedTimeSlotsRepository = mock(BookedTimeSlotsRepository.class);
    private ConcreteEnvironmentParamService concreteEnvironmentParamService =
            mock(ConcreteEnvironmentParamService.class);
    private RequestSubTypeService requestSubTypeService = mock(RequestSubTypeService.class);
    private CalendaringServiceClientWrapperService calendaringServiceClientWrapperService =
            mock(CalendaringServiceClientWrapperService.class);

    private static final String DRIVER_BOOKLET_TEXT =
            "Вы сотрудник [[${parcel.supplier.name}]]. " +
                    "Номер заявки на поставку [[${parcel.application.id}]].[[${remove}]] " +
                    "Склад примет поставку [[${parcel.date}]]. " +
                    "Примерное время приёма — [[${parcel.time}]]. " +
                    "Приезжайте, сотрудник [[${parcel.supplier.name}]], " +
                    "в [[${parcel.recommended.time}]].[[${remove}]][[${remove.withdraw.list}]] " +
                    "Также вы можете забрать изъятия с номерами: " +
                    "[[${parcel.withdraw.list}]].[[${remove.withdraw.list}]]";

    private static final String PARSED_DRIVER_BOOKLET_TEXT =
            "Вы сотрудник ООО «Ромашка». " +
                    "Номер заявки на поставку 12345. " +
                    "Склад примет поставку 6 июля. " +
                    "Примерное время приёма — 12:00. " +
                    "Приезжайте, сотрудник ООО «Ромашка», в 11:30.";

    private static final String PARSED_DRIVER_BOOKLET_TEXT_WITH_NO_TIME_SLOTS =
            "Вы сотрудник ООО «Ромашка». " +
                    "Номер заявки на поставку 12345.";

    private static final String PARSED_DRIVER_BOOKLET_TEXT_WITH_ADDITIONAL_WITHDRAWS_AND_NO_TIME_SLOTS =
            "Вы сотрудник ООО «Ромашка». Номер заявки на поставку 12345. Также вы можете забрать изъятия с номерами: " +
                    "666-1, 666-2.";

    public static final String TEMPLATE_TEXT = "Заголовок " +
            "[[${remove.withdraw.list}]]тут список изъятий: [[${parcel.withdraw.list}]] " +
            "если он есть (удаляем, если список пуст)[[${remove.withdraw.list}]] тут ещё какой-то текст" +
            "[[${remove.withdraw.list}]] а это тоже удаляем[[${remove.withdraw.list}]]";

    @BeforeEach
    public void beforeEach() {
        reset(bookedTimeSlotsRepository, concreteEnvironmentParamService, requestSubTypeService,
                calendaringServiceClientWrapperService);
        resetMocks();
    }

    @Test
    public void getParsedHtml() {
        when(shopRequestFetchingService.findAllBySupplierIdAndServiceIdAndStatus(anyLong(), anyLong(),
                eq(RequestStatus.READY_TO_WITHDRAW)))
                .thenReturn(List.of());

        PlaceholdersParser placeholdersParser =
                new PlaceholdersParser(calendaringServiceClientWrapperService,
                        shopRequestFetchingService, requestSubTypeService);

        ShopRequest shopRequest = new ShopRequest();
        shopRequest.setId(12345L);
        shopRequest.setServiceId(147L);
        shopRequest.setServiceRequestId("12345");
        shopRequest.setType(RequestType.SUPPLY);

        Supplier supplier = new Supplier();
        supplier.setOrganizationName("ООО «Ромашка»");
        shopRequest.setSupplier(supplier);

        ZonedDateTime from = ZonedDateTime.of(2020, 7, 6, 12, 0, 0, 0, ZoneId.of("Europe/Moscow"));
        ZonedDateTime to = ZonedDateTime.of(2020, 7, 6, 13, 0, 0, 0, ZoneId.of("Europe/Moscow"));

        BookingResponseV2 bookingResponse = new BookingResponseV2(1, "FFWF", "12345", null, 1,
                from, to, BookingStatus.ACTIVE, LocalDateTime.of(2010, 1, 1, 1, 0),
                1);

        when(calendaringServiceClientWrapperService.findByExternalIdsAndStatusV2(any(), any())).thenReturn(
                List.of(bookingResponse));

        Document parsedDocument = placeholdersParser.getParsedHtml(Jsoup.parse(DRIVER_BOOKLET_TEXT), shopRequest,
                false);
        assertEquals(PARSED_DRIVER_BOOKLET_TEXT, parsedDocument.text());
    }

    @Test
    public void getParsedHtmlWithNoTimeSlots() {
        when(shopRequestFetchingService.findAllBySupplierIdAndServiceIdAndStatus(anyLong(), anyLong(),
                eq(RequestStatus.READY_TO_WITHDRAW)))
                .thenReturn(List.of());
        PlaceholdersParser placeholdersParser =
                new PlaceholdersParser(calendaringServiceClientWrapperService,
                        shopRequestFetchingService, requestSubTypeService);

        ShopRequest shopRequest = new ShopRequest();
        shopRequest.setId(12345L);
        shopRequest.setServiceId(147L);
        shopRequest.setServiceRequestId("12345");
        shopRequest.setType(RequestType.SUPPLY);

        Supplier supplier = new Supplier();
        supplier.setOrganizationName("ООО «Ромашка»");
        shopRequest.setSupplier(supplier);

        Document parsedDocument = placeholdersParser.getParsedHtml(Jsoup.parse(DRIVER_BOOKLET_TEXT), shopRequest,
                false);
        Assertions.assertEquals(PARSED_DRIVER_BOOKLET_TEXT_WITH_NO_TIME_SLOTS, parsedDocument.text());
    }

    @Test
    public void getParsedHtmlWithAdditionalWithdrawsAndNoTimeSlots() {
        ShopRequest withdraw1 = new ShopRequest();
        withdraw1.setId(7771L);
        withdraw1.setServiceRequestId("666-1");
        withdraw1.setType(RequestType.WITHDRAW);

        ShopRequest withdraw2 = new ShopRequest();
        withdraw2.setId(7772L);
        withdraw2.setServiceRequestId("666-2");
        withdraw2.setType(RequestType.WITHDRAW);

        ShopRequest withdraw3 = new ShopRequest();
        withdraw3.setId(7773L);
        withdraw3.setServiceRequestId("999-3");
        withdraw3.setType(RequestType.WITHDRAW);
        withdraw3.setSubtype("not to withdraw");

        ShopRequest shopRequest = new ShopRequest();
        shopRequest.setId(12345L);
        shopRequest.setServiceId(147L);
        shopRequest.setServiceRequestId("12345");
        shopRequest.setType(RequestType.SUPPLY);

        Supplier supplier = new Supplier();
        supplier.setOrganizationName("ООО «Ромашка»");
        shopRequest.setSupplier(supplier);

        when(shopRequestFetchingService.findAllBySupplierIdAndServiceIdAndStatus(anyLong(), anyLong(),
                eq(RequestStatus.READY_TO_WITHDRAW)))
                .thenReturn(List.of(withdraw1, withdraw2, shopRequest));
        mockRequestSubtypeService();

        PlaceholdersParser placeholdersParser =
                new PlaceholdersParser(calendaringServiceClientWrapperService,
                        shopRequestFetchingService, requestSubTypeService);

        Document parsedDocument = placeholdersParser.getParsedHtml(Jsoup.parse(DRIVER_BOOKLET_TEXT), shopRequest,
                false);
        Assertions.assertEquals(PARSED_DRIVER_BOOKLET_TEXT_WITH_ADDITIONAL_WITHDRAWS_AND_NO_TIME_SLOTS,
                parsedDocument.text());
    }

    @Test
    @DatabaseSetup("classpath:service/drivers/multiply-withdraw.xml")
    public void processWithdrawListPatternsWithAdditionalWithdrawList() {
        mockRequestSubtypeService();

        PlaceholdersParser placeholdersParser =
                new PlaceholdersParser(calendaringServiceClientWrapperService,
                        shopRequestFetchingService, requestSubTypeService);

        Assertions.assertEquals("Заголовок тут список изъятий: 14702, 14701, 14700 если он есть (удаляем, если" +
                        " список пуст) тут ещё какой-то текст а это тоже удаляем",
                placeholdersParser.processWithdrawListPatterns(getShopRequest(), TEMPLATE_TEXT));
    }

    private void mockRequestSubtypeService() {
        RequestSubTypeEntity subTypeEntity = new RequestSubTypeEntity(1, RequestType.WITHDRAW, "DEFAULT");
        subTypeEntity.setIncludeWithdrawIntoDriverBooklet(true);
        when(requestSubTypeService.getEntityByRequestTypesAndSubtypes(any()))
                .thenReturn(Map.of(new TypeSubtype(RequestType.WITHDRAW, "DEFAULT"), subTypeEntity));
    }

    @Test
    public void processWithdrawListPatternsWithoutAdditionalWithdrawList() {
        when(shopRequestFetchingService.findAllBySupplierIdAndServiceIdAndStatus(anyLong(), anyLong(),
               eq(RequestStatus.READY_TO_WITHDRAW)))
               .thenReturn(List.of());

        PlaceholdersParser placeholdersParser =
                new PlaceholdersParser(calendaringServiceClientWrapperService,
                        shopRequestFetchingService, requestSubTypeService);

       Assertions.assertEquals("Заголовок  тут ещё какой-то текст",
               placeholdersParser.processWithdrawListPatterns(getShopRequest(), TEMPLATE_TEXT));
   }

    @NotNull
    private ShopRequest getShopRequest() {
        ShopRequest shopRequest = new ShopRequest();
        shopRequest.setId(12345L);
        shopRequest.setSupplier(new Supplier(2L, "", "", 0L,
                SupplierType.THIRD_PARTY, new SupplierBusinessType()));
        shopRequest.setServiceId(147L);
        shopRequest.setType(RequestType.WITHDRAW);
        return shopRequest;
    }
}
