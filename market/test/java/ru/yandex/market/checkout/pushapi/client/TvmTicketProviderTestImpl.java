package ru.yandex.market.checkout.pushapi.client;

import java.util.Optional;

import ru.yandex.market.checkout.common.rest.TvmTicketProvider;

/**
 * @author ifilippov5
 */
public class TvmTicketProviderTestImpl implements TvmTicketProvider {

    @Override
    public Optional<String> getServiceTicket() {
        return Optional.of("suchTvmTicket");
    }

}
