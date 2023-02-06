package ru.yandex.market.delivery.mdbapp.util;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

import org.assertj.core.api.JUnitSoftAssertions;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import ru.yandex.market.delivery.mdbapp.components.storage.domain.ReturnRequest;
import ru.yandex.market.delivery.mdbapp.components.storage.domain.ReturnRequestItem;
import ru.yandex.market.delivery.mdbapp.components.storage.domain.type.ReturnRequestState;

import static ru.yandex.market.delivery.mdbapp.integration.utils.ReturnRequestTestUtils.newPickupPoint;

@RunWith(Parameterized.class)
public class CheckCreatingRequestStateTest {

    @Rule
    public JUnitSoftAssertions softly = new JUnitSoftAssertions();

    @Parameter
    public ReturnRequest returnRequest;

    @Parameter(1)
    public ReturnRequestState expectedState;

    @Parameters
    public static Collection<Object[]> data() {
        return List.of(new Object[][]{
            {
                returnRequest(),
                ReturnRequestState.CREATING_REQUESTS
            },
            {
                returnRequest().setReturnId(null),
                ReturnRequestState.AWAITING_FOR_DATA
            },
            {
                returnRequest().setBarcode(null),
                ReturnRequestState.CREATING_REQUESTS
            },
            {
                returnRequest().setExternalOrderId(null),
                ReturnRequestState.AWAITING_FOR_DATA
            },
            {
                returnRequest().setRequestDate(null),
                ReturnRequestState.AWAITING_FOR_DATA
            },
            {
                returnRequestWithoutItems(),
                ReturnRequestState.AWAITING_FOR_DATA
            },
            {
                returnRequestWithouPickupPoint(),
                ReturnRequestState.AWAITING_FOR_DATA
            }
        });
    }

    @Test
    public void test() {
        // when:
        ReturnRequestUtil.checkCreatingRequestState(returnRequest);

        // then:
        softly.assertThat(returnRequest.getState()).isEqualTo(expectedState);
    }

    private static ReturnRequest returnRequest() {
        final String returnId = "7342";
        final ReturnRequest returnRequest = new ReturnRequest()
            .setReturnId(returnId)
            .setBarcode("VOZVRAT_SF_PVZ_" + returnId)
            .setExternalOrderId(7342001L)
            .setBuyerName("Константин Вячеславович Воронцов")
            .setRequestDate(LocalDate.now())
            .setState(ReturnRequestState.AWAITING_FOR_DATA);
        returnRequest.addReturnRequestItem(new ReturnRequestItem().setId(7342011L));
        newPickupPoint().addReturnRequest(returnRequest);

        return returnRequest;
    }

    private static ReturnRequest returnRequestWithoutItems() {
        final ReturnRequest returnRequest = returnRequest();
        returnRequest.getItems()
            .forEach(returnRequest::removeReturnRequestItem);
        return returnRequest;
    }

    private static ReturnRequest returnRequestWithouPickupPoint() {
        final ReturnRequest returnRequest = returnRequest();
        returnRequest.getPickupPoint().removeReturnRequest(returnRequest);
        return returnRequest;
    }
}
