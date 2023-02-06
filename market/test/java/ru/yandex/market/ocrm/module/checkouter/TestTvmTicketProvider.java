package ru.yandex.market.ocrm.module.checkouter;

import java.util.Optional;

import ru.yandex.market.checkout.common.rest.TvmTicketProvider;

public class TestTvmTicketProvider implements TvmTicketProvider {
    @Override
    public Optional<String> getServiceTicket() {
        return Optional.empty();
    }
}
