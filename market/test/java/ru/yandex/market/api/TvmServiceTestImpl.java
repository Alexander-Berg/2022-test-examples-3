package ru.yandex.market.api;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import ru.yandex.market.api.internal.tvm.TvmTicket;
import ru.yandex.market.api.server.sec.tvm.TvmService;

/**
 * Created by tesseract on 15.07.17.
 */
@Profile("test")
@Component
public class TvmServiceTestImpl implements TvmService {
    @Override
    public TvmTicket getTicket(String tvmApplicationId) {
        return null;
    }
}
