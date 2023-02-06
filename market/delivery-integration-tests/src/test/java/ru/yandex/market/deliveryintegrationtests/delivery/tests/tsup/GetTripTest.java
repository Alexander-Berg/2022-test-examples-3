package ru.yandex.market.deliveryintegrationtests.delivery.tests.tsup;

import io.qameta.allure.Epic;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.qatools.properties.Property;
import ru.qatools.properties.Resource;

import java.time.LocalDate;

@Resource.Classpath({"delivery/delivery.properties"})
@Slf4j
@DisplayName("Tsup Test")
@Epic("Tsup")

public class GetTripTest extends AbstractTsupTest {

    @Property("delivery.sofino")
    private long sofino;

    @Property("delivery.scklimovsk")
    private long scklimovsk;

    private static final long SOFINO_LOGPOINT = 10000004403L;
    private static final long SCKLIMOVSK_LOGPOINT = 10001009454L;
    static Long transportationId;
    static Long tripId;

    @BeforeEach
    public void setUp() {
        transportationId = TM_STEPS.getTransportationIdForDay(
                scklimovsk,
                sofino,
                LocalDate.now().plusDays(1),
                null
        );
        tripId = TM_STEPS.getTripByTransportationId(transportationId);
    }

    @Test
    @DisplayName("Тsup: Поиск рейса")
    void getTripTest() {
        TSUP_STEPS.verifyRun(tripId, SCKLIMOVSK_LOGPOINT, SOFINO_LOGPOINT, LocalDate.now().plusDays(1));
    }

}
