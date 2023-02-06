package ru.yandex.direct.core.testing.stub;

import javax.annotation.ParametersAreNonnullByDefault;

import ru.yandex.direct.tvm.TvmIntegration;
import ru.yandex.direct.tvm.TvmService;
import ru.yandex.inside.passport.tvm2.exceptions.IncorrectTvmServiceTicketException;
import ru.yandex.inside.passport.tvm2.exceptions.IncorrectTvmUserTicketException;

/**
 * Тестовый стаб для {@link TvmIntegration}
 * Сгенерированный тикет и проверка тикета прозрачны: тикет всегда соответствует переданному {@link TvmService}
 */
@ParametersAreNonnullByDefault
public class TvmIntegrationTestStub implements TvmIntegration {
    private final TvmService currentService;

    public TvmIntegrationTestStub(TvmService currentService) {
        this.currentService = currentService;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public String getTicket(TvmService service) {
        return service.toString();
    }

    @Override
    public TvmService getTvmService(String ticket) throws IncorrectTvmServiceTicketException {
        return TvmService.fromStringStrict(ticket);
    }

    @Override
    public long checkUserTicket(String ticket) throws IncorrectTvmUserTicketException {
        return 0;
    }

    @Override
    public TvmService currentTvmService() {
        return currentService;
    }

    @Override
    public void close() {
    }
}
