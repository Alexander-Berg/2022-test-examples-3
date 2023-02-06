package ru.yandex.market.wms.common.spring.service;

import java.util.Collections;
import java.util.Set;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.wms.common.spring.IntegrationTest;
import ru.yandex.market.wms.common.spring.exception.BadRequestTitledException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class CancelledItemServiceTest extends IntegrationTest {
    private static final Set<String> UIT = Collections.singleton("SERIAL_1");
    private static final String TO_CONTAINER = "CANCELLED";
    private static final String ITRN_SOURCE_TYPE = "Cancelled item move";
    private static final String SOURCE_LOC = "NS-CONS-1";
    private static final String WRONG_LOC = "WRONG_LOC";

    @Autowired
    private CancelledItemService service;

    @Test
    @DatabaseSetup("/db/service/cancelled-item/1/before.xml")
    @ExpectedDatabase(value = "/db/service/cancelled-item/1/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void moveCancelledItemHappy() {
        service.validateAndMoveCancelledItemsByUits(UIT, TO_CONTAINER, ITRN_SOURCE_TYPE, SOURCE_LOC);
    }

    @Test
    @DatabaseSetup("/db/service/cancelled-item/2/before.xml")
    @ExpectedDatabase(value = "/db/service/cancelled-item/2/before.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void moveCancelledItem_ToContainerWithNotCancelledItems() {
        BadRequestTitledException actualException = assertThrows(
                BadRequestTitledException.class,
                () -> service.validateAndMoveCancelledItemsByUits(UIT, TO_CONTAINER, ITRN_SOURCE_TYPE, SOURCE_LOC)
        );
        //Fix flaky on different locale
        assertEquals(String.format("Контейнер CANCELLED содержит неотмененные товары: %fшт", 2.0),
                actualException.getMessage());
    }

    @Test
    @DatabaseSetup("/db/service/cancelled-item/1/before.xml")
    @ExpectedDatabase(value = "/db/service/cancelled-item/1/before.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void moveCancelledItem_incorrectSourceLoc() {
        BadRequestTitledException actualException = assertThrows(
                BadRequestTitledException.class,
                () -> service.validateAndMoveCancelledItemsByUits(UIT, TO_CONTAINER, ITRN_SOURCE_TYPE, WRONG_LOC)
        );
        assertEquals(
                "Товар (1 шт) находится в ячейках [NS-CONS-1], не совпадающих с ячейкой задания WRONG_LOC: [SERIAL_1]",
                actualException.getMessage());
    }
}
