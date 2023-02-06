package ru.yandex.market.checkout.util;

import java.util.Optional;

import ru.yandex.market.checkout.common.rest.TvmTicketProvider;

/**
 * @author : poluektov
 * date: 2019-10-24.
 */
public class TvmTicketProviderTestImpl implements TvmTicketProvider {

    @Override
    public Optional<String> getServiceTicket() {
        return Optional.of("suchTvmTicket");
    }
}
