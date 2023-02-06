package ru.yandex.market.indexer.yt.generation;

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import ru.yandex.market.core.indexer.model.GenerationMeta;
import ru.yandex.market.core.yt.indexer.YtFactory;
import ru.yandex.yt.ytclient.proxy.YtClient;
import ru.yandex.yt.ytclient.rpc.RpcOptions;

/**
 * Фабрика {@link YtErrorsProvider}, которая подкладывает замоканный {@link MockFLYtClient} вместо {@link YtClient}.
 * Таким образом поведение {@link YtErrorsProvider} не изменяется в тестах.
 * Меняется лишь компонент, который делает запросы в yt. Вместо yt запросы делаются в h2.
 *
 * @author Kirill Batalin (batalin@yandex-team.ru)
 */
public class MockYtErrorsProviderFactory extends YtErrorsProviderFactory {

    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    public MockYtErrorsProviderFactory(final RpcOptions ytErrorsProviderRpcOptions,
                                       final YtErrorsProviderOptions ytErrorsProviderOptions,
                                       final YtFactory ytFactory,
                                       final NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
        super(ytErrorsProviderRpcOptions, ytErrorsProviderOptions, ytFactory);
        this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
    }

    @Override
    protected YtClient createYtClient(final GenerationMeta meta) {
        return new MockFLYtClient(namedParameterJdbcTemplate);
    }
}
