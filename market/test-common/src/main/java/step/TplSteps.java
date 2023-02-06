package step;

import java.time.Instant;
import java.util.List;

import client.LesClient;
import dto.requests.les.EventDto;
import io.qameta.allure.Step;
import lombok.extern.slf4j.Slf4j;
import toolkit.Retrier;

import ru.yandex.market.logistics.les.dto.CarDto;
import ru.yandex.market.logistics.les.dto.CourierDto;
import ru.yandex.market.logistics.les.dto.PersonDto;
import ru.yandex.market.logistics.les.dto.PhoneDto;
import ru.yandex.market.logistics.les.tpl.CourierReceivedPickupReturnEvent;

@Slf4j
public class TplSteps {

    private static final String COURIER_SOURCE = "courier";
    private static final String COURIER_QUEUE = "courier_out";
    private static final String COURIER_RECEIVED_PICKUP_RETURN_EVENT = "COURIER_RECEIVED_PICKUP_RETURN";
    private static final Long COURIER_ID = 94899L;
    private static final Long COURIER_UUID = 1043756597L;
    private static final String COURIER_NAME = "Courier";
    private static final String COURIER_PHONE_NUMBER = "+7-900-000-00-00";
    private static final String COURIER_CAR_NUMBER = "a000aa";

    private static final LesClient LES = new LesClient();

    @Step("Забор возврата из ПВЗ в СЦ")
    public void receiveReturnFromPvzToSc(
        String barcode,
        Long sortingCenterId,
        Long deliveryServiceId
    ) {
        log.debug("push event about return transportation from pvz to sc...");
        Retrier.clientRetry(() -> {
            LES.addEvent(
                new EventDto(
                    COURIER_SOURCE,
                    barcode + Instant.now().getEpochSecond(),
                    Instant.now().toEpochMilli(),
                    COURIER_RECEIVED_PICKUP_RETURN_EVENT,
                    new CourierReceivedPickupReturnEvent(
                        barcode,
                        sortingCenterId,
                        List.of(sortingCenterId),
                        Instant.now(),
                        new CourierDto(
                            COURIER_ID,
                            COURIER_UUID,
                            deliveryServiceId,
                            new PersonDto(COURIER_NAME, null, null),
                            new PhoneDto(COURIER_PHONE_NUMBER, null),
                            new CarDto(COURIER_CAR_NUMBER, ""),
                            null
                        )
                    ),
                    "AUTOTEST_EVENT"
                ),
                COURIER_QUEUE
            );
        });
    }
}
