package ru.yandex.market.core.ds;

import java.util.function.Function;

import javax.annotation.Nonnull;

import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.core.ds.model.DatasourceInfo;

/**
 * Наследник {@link DbDatasourceLockService} с отключенными блокировками для H2 для обхода несовместимостей
 * синтаксиса SQL с ораклом.
 *
 * @author Georgiy Klimov gaklimov@yandex-team.ru
 */
public class MockDatasourceLockService implements DatasourceLockService {

    private final DatasourceService datasourceService;


    @Autowired
    public MockDatasourceLockService(@Nonnull final DatasourceService datasourceService) {
        this.datasourceService = datasourceService;
    }

    @Override
    public <T> T lockDatasourceForUpdate(long id, int timeoutSeconds, Function<DatasourceInfo, T> handler) {
        final DatasourceInfo info = datasourceService.getDatasource(id);
        return handler.apply(info);
    }

}
