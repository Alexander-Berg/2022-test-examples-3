package ru.yandex.market.fulfillment.wrap.marschroute.functional;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.SpyBean;

import ru.yandex.market.fulfillment.wrap.marschroute.functional.configuration.IntegrationTest;
import ru.yandex.market.fulfillment.wrap.marschroute.model.response.BaseMarschrouteResponse;
import ru.yandex.market.fulfillment.wrap.marschroute.service.MarschroutePutReferenceItemsService;
import ru.yandex.market.logistic.api.model.fulfillment.ErrorCode;
import ru.yandex.market.logistic.api.model.fulfillment.ErrorItem;
import ru.yandex.market.logistic.api.model.fulfillment.ErrorPair;
import ru.yandex.market.logistic.api.model.fulfillment.Item;
import ru.yandex.market.logistic.api.model.fulfillment.UnitId;
import ru.yandex.market.logistic.api.model.fulfillment.exception.FulfillmentApiException;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Основная логика работы сервиса {@link MarschroutePutReferenceItemsService} тестируется в
 * {@link CreateInboundFunctionalTest}. В данном тесте проверяется непосредственная работа сервиса, на данном этапе
 * все взаимодействие со сторонними сервисами тут не проверяется.
 */
class PutReferenceItemsFunctionalTest extends IntegrationTest {

    private static final UnitId FIRST_UNIT_ID = new UnitId("1", 1L, "1");
    private static final UnitId SECOND_UNIT_ID = new UnitId("2", 2L, "2");
    private static final Item FIRST_ITEM = createItem(FIRST_UNIT_ID);
    private static final Item SECOND_ITEM = createItem(SECOND_UNIT_ID);
    private static final String EXCEPTION_MESSAGE = "Something comes wrong";
    private static final ErrorItem EXCEPTION_WITH_SECOND_ITEM = createErrorItem(SECOND_ITEM, EXCEPTION_MESSAGE);
    private static final BaseMarschrouteResponse SUCCESSFUL_RESPONSE = new BaseMarschrouteResponse().setSuccess(true);
    private static final int ERROR_CODE = 10;
    private static final String ERROR_CODE_STRING = String.valueOf(ERROR_CODE);
    private static final ErrorItem BUSINESS_ERROR_WITH_SECOND_ITEM = createErrorItem(SECOND_ITEM, ERROR_CODE_STRING);
    private static final BaseMarschrouteResponse UNSUCCESSFUL_RESPONSE =
            new BaseMarschrouteResponse().setSuccess(false).setCode(ERROR_CODE);

    @SpyBean
    private MarschroutePutReferenceItemsService putReferenceItemsService;

    @Test
    void testPutSingleItemWorks() {
        doReturn(SUCCESSFUL_RESPONSE)
                .when(putReferenceItemsService).execute(anyList());

        List<ErrorItem> errorItems = putReferenceItemsService.executeWithBatches(singletonList(FIRST_ITEM));

        softly.assertThat(errorItems).withFailMessage("There shouldn't be any errors").isEmpty();

        verify(putReferenceItemsService).execute(anyList());
    }

    @Test
    void testPutTwoItemsInTwoBatchesWithSuccessAndExceptionWorks() {

        doReturn(SUCCESSFUL_RESPONSE)
                .doThrow(new RuntimeException(EXCEPTION_MESSAGE))
                .when(putReferenceItemsService).execute(anyList());

        List<ErrorItem> errorItems =
                putReferenceItemsService.executeWithBatches(asList(FIRST_ITEM, SECOND_ITEM));

        softly.assertThat(errorItems)
            .withFailMessage("There should be only one error with seconde item")
            .containsExactly(EXCEPTION_WITH_SECOND_ITEM);

        verify(putReferenceItemsService, times(2)).execute(anyList());
    }

    @Test
    void testPutTwoItemsInTwoBatchesWithDifferentMarschrouteAnswersWorks() {

        doReturn(SUCCESSFUL_RESPONSE)
                .doReturn(UNSUCCESSFUL_RESPONSE)
                .when(putReferenceItemsService).execute(anyList());

        List<ErrorItem> errorItems =
                putReferenceItemsService.executeWithBatches(asList(FIRST_ITEM, SECOND_ITEM));

        softly.assertThat(errorItems)
            .withFailMessage("There should be only one error with second item")
            .containsExactly(BUSINESS_ERROR_WITH_SECOND_ITEM);

        verify(putReferenceItemsService, times(2)).execute(anyList());
    }

    @Test
    void testPutTwoItemsInTwoBatchesWithExceptionsInBothWorks() {

        doThrow(new RuntimeException(EXCEPTION_MESSAGE))
                .doThrow(new RuntimeException(EXCEPTION_MESSAGE))
                .when(putReferenceItemsService).execute(anyList());

        softly.assertThatThrownBy(() -> putReferenceItemsService.executeWithBatches(asList(FIRST_ITEM, SECOND_ITEM)))
                .isInstanceOf(FulfillmentApiException.class);
    }

    private static Item createItem(UnitId unitId) {
        return new Item.ItemBuilder(null, null, null).setUnitId(unitId).build();
    }

    private static ErrorItem createErrorItem(Item item, String error) {
        return new ErrorItem(item.getUnitId(), new ErrorPair(ErrorCode.UNKNOWN_ERROR, error, null, null));
    }
}
