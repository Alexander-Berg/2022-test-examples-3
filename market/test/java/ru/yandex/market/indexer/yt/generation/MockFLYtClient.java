package ru.yandex.market.indexer.yt.generation;

import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.jdbc.core.namedparam.EmptySqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTreeBuilder;
import ru.yandex.inside.yt.kosher.impl.ytree.object.serializers.YTreeObjectSerializer;
import ru.yandex.inside.yt.kosher.ytree.YTreeNode;
import ru.yandex.yt.ytclient.bus.DefaultBusConnector;
import ru.yandex.yt.ytclient.proxy.SelectRowsRequest;
import ru.yandex.yt.ytclient.proxy.YtClient;
import ru.yandex.yt.ytclient.rpc.RpcCompression;
import ru.yandex.yt.ytclient.rpc.RpcCredentials;
import ru.yandex.yt.ytclient.rpc.RpcOptions;

/**
 * Мок {@link YtClient}, который выполняет запросы на h2 вместо yt.
 * Используется табличка {@code shops_web.process_log_mock}.
 *
 * @author Kirill Batalin (batalin@yandex-team.ru)
 */
public class MockFLYtClient extends YtClient {

    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    public MockFLYtClient(final NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
        super(new DefaultBusConnector(),
                Collections.emptyList(),
                "",
                null,
                new RpcCredentials(),
                new RpcCompression(),
                new RpcOptions());
        this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
    }

    @Override
    public CompletableFuture<Void> waitProxies() {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public <T> CompletableFuture<List<T>> selectRows(final SelectRowsRequest request,
                                                     final YTreeObjectSerializer<T> serializer) {
        final String query = getQuery(request);

        // Данные из запроса мапаем на YTreeNode для того, чтобы потом создать объект через serializer.
        final List<YTreeNode> nodes = new ArrayList<>();
        namedParameterJdbcTemplate.query(query, EmptySqlParameterSource.INSTANCE, rs -> {
            final ResultSetMetaData metaData = rs.getMetaData();
            final int columnCount = metaData.getColumnCount();

            // Обходим строки
            while (rs.next()) {

                // Каждую строку маппам в мапу YTreeNode
                final YTreeBuilder builder = new YTreeBuilder().beginMap();
                for (int i = 0; i < columnCount; ++i) {
                    final String columnName = metaData.getColumnName(i + 1).toLowerCase();
                    final Object columnValue = ObjectUtils.defaultIfNull(rs.getObject(i + 1), StringUtils.EMPTY);
                    builder.key(columnName).value(columnValue);
                }
                final YTreeNode node = builder.endMap().build();
                nodes.add(node);
            }

            return true;
        });

        final List<T> result = nodes.stream()
                .map(serializer::deserialize)
                .collect(Collectors.toList());
        return CompletableFuture.completedFuture(result);
    }

    /**
     * Прохачить запрос. Запросы к дин.таблицам похожи на sql, но это не sql.
     * Сделаем вид, что этого метода нет.
     */
    private String getQuery(final SelectRowsRequest request) {
        String query = request.getQuery();
        query = "select " + query;
        query = query.replaceAll("\\[.*]", "shops_web.process_log_mock");
        query = query.replaceAll("if\\(([^,]*),([^,]*),([^)]*)\\)", "case when $1 then $2 else $3 end");
        return query;
    }
}
