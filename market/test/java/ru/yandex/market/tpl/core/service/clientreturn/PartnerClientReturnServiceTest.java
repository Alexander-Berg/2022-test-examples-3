package ru.yandex.market.tpl.core.service.clientreturn;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.tpl.core.domain.clientreturn.ClientReturn;
import ru.yandex.market.tpl.core.domain.clientreturn.ClientReturnGenerator;
import ru.yandex.market.tpl.core.domain.partner.clientreturn.PartnerClientReturnService;
import ru.yandex.market.tpl.core.test.TplAbstractTest;

import static org.assertj.core.api.Assertions.assertThat;

@RequiredArgsConstructor
public class PartnerClientReturnServiceTest extends TplAbstractTest {
    private final PartnerClientReturnService partnerClientReturnService;
    private final ClientReturnGenerator clientReturnGenerator;

    private ClientReturn clientReturn;

    @BeforeEach
    void init() {
        clientReturn = clientReturnGenerator.generateReturnFromClient();
    }

    @Test
    void getRescheduleDates() {
        var dates = partnerClientReturnService.getAvailableReschedulingIntervals(clientReturn.getExternalReturnId());
        assertThat(dates).isNotNull();
        assertThat(dates).isNotEmpty();
    }
}
