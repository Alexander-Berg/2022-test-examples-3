package ru.yandex.market.volva.graphs.service;

import java.util.List;
import java.util.stream.Stream;

import io.netty.channel.nio.NioEventLoopGroup;
import lombok.SneakyThrows;
import org.junit.Ignore;
import org.junit.Test;

import ru.yandex.market.volva.config.ConfigurationService;
import ru.yandex.market.volva.entity.EdgeEvent;
import ru.yandex.market.volva.entity.EventCollection;
import ru.yandex.market.volva.entity.IdType;
import ru.yandex.market.volva.entity.Node;
import ru.yandex.market.volva.entity.Source;
import ru.yandex.market.volva.yt.EdgesRequestsBuilder;
import ru.yandex.market.volva.yt.UsersRequestsBuilder;
import ru.yandex.market.volva.yt.YtEdge;
import ru.yandex.market.volva.yt.YtGraphReader;
import ru.yandex.market.volva.yt.YtOptions;
import ru.yandex.market.volva.yt.YtUser;
import ru.yandex.yt.ytclient.bus.DefaultBusConnector;
import ru.yandex.yt.ytclient.proxy.YtClient;
import ru.yandex.yt.ytclient.rpc.RpcCredentials;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class EventServiceTest {
    @Test
    @Ignore
    @SneakyThrows
    public void smokeTest() {
        var ytClient = new YtClient(new DefaultBusConnector(new NioEventLoopGroup(0), true),
            "markov",
            RpcCredentials.loadFromEnvironment());
        var ytOptions = new YtOptions("//home/market/testing/antifraud/volva", "users", "edges_new", 10, 10, 100);
        ConfigurationService configurationService = mock(ConfigurationService.class);
        when(configurationService.getSemaphoreTimeout()).thenReturn(5);
        ytClient.waitProxies().join();

        var usersRequestsBuilder = new UsersRequestsBuilder(ytOptions);
        var edgesRequestsBuilder = new EdgesRequestsBuilder(ytOptions);
        var graphReader = new YtGraphReader(ytOptions, configurationService);
        var eventService = new EventService(ytClient,
            new YtGraphService(usersRequestsBuilder,
                edgesRequestsBuilder,
                graphReader,
                ytOptions
            )
        );

        var node1 = new Node("john galt", IdType.PHONE);
        var node2 = new Node("frisco", IdType.PHONE);
        var node3 = new Node("ragnar", IdType.PHONE);

        var edge1 = EdgeEvent.addTrusted(node1, node2);
        eventService.accept(EventCollection.create(List.of(edge1), Source.CHECKOUTER));
        Thread.sleep(500);
        var users = ytClient
            .lookupRows(usersRequestsBuilder.byUserIds(Stream.of(node1, node2, node3)), usersRequestsBuilder.getSerializer())
            .join();
        assertThat(users).extracting(YtUser::getNode)
            .containsOnly(node1, node2);
        var edges = graphReader.selectEdges(ytClient, List.of(node1, node2))
            .join();
        assertThat(edges).isNotEmpty();

        var edge2 = EdgeEvent.addTrusted(node2, node3);
        eventService.accept(EventCollection.create(List.of(edge2), Source.CHECKOUTER));
        Thread.sleep(500);
        users = ytClient
            .lookupRows(usersRequestsBuilder.byUserIds(Stream.of(node1, node2, node3)), usersRequestsBuilder.getSerializer())
            .join();
        assertThat(users).extracting(YtUser::getNode)
            .containsOnly(node1, node2, node3);
        edges = graphReader.selectEdges(ytClient, List.of(node1, node2, node3))
            .join();
        assertThat(edges).extracting(YtEdge::getFrom, YtEdge::getTo)
            .containsOnly(
                tuple(node2, node1),
                tuple(node2, node3)
            );

        eventService.accept(EventCollection.create(List.of(EdgeEvent.remove(node1, node2)), Source.CHECKOUTER));
        Thread.sleep(500);
        users = ytClient
            .lookupRows(usersRequestsBuilder.byUserIds(Stream.of(node1, node2, node3)), usersRequestsBuilder.getSerializer())
            .join();
        assertThat(users).extracting(YtUser::getNode)
            .containsOnly(node2, node3);
        edges = graphReader.selectEdges(ytClient, List.of(node1, node2))
            .join();
        assertThat(edges).extracting(YtEdge::getFrom, YtEdge::getTo)
            .containsOnly(
                tuple(node2, node3)
            );
    }
}
