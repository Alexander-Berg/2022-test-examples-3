package ru.yandex.market.mbi.api.controller;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

import ru.yandex.market.mbi.api.client.entity.MstatClickCorrectionActionId;
import ru.yandex.market.mbi.api.config.FunctionalTest;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * Тесты для {@link MstatClicksCorrectionActionController}
 */
class MstatClicksCorrectionActionControllerTest extends FunctionalTest {
    private static final String VALID_TICKET_NAME = "MBI-45656";
    private static final String INVALID_TICKET_NAME = "test";

    @Test
    void testGetActionId() {
        MstatClickCorrectionActionId actionId = mbiApiClient.createClickCorrectionsActionId(VALID_TICKET_NAME);
        assertThat(actionId.getActionId(), is(1L));
    }

    @Test
    void testGetActionIdError() {
        var httpClientErrorException = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> mbiApiClient.createClickCorrectionsActionId(INVALID_TICKET_NAME)
        );
        assertThat(httpClientErrorException.getStatusCode(), is(HttpStatus.BAD_REQUEST));
    }
}

