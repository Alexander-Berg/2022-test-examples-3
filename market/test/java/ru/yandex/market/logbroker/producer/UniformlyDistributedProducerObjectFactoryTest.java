package ru.yandex.market.logbroker.producer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableMap;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.yandex.kikimr.persqueue.producer.AsyncProducer;
import ru.yandex.kikimr.persqueue.producer.async.AsyncProducerConfig;
import ru.yandex.kikimr.persqueue.producer.transport.message.inbound.ProducerInitResponse;
import ru.yandex.market.logbroker.model.LogbrokerCluster;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Georgiy Klimov gaklimov@yandex-team.ru
 */
@ExtendWith(MockitoExtension.class)
class UniformlyDistributedProducerObjectFactoryTest {

    ProducerPoolObjectFactory objectPoolFactory;
    GenericObjectPool<PooledSimpleAsyncProducer> pool;

    @Mock
    LogbrokerCluster vla;
    @Mock
    LogbrokerCluster sas;

    @BeforeEach
    void setUp() {
        ProducerConfig config = config();
        objectPoolFactory = new UniformlyDistributedProducerObjectFactory(config);
        pool = new GenericObjectPool<>(objectPoolFactory, config.getPoolConfig());
        objectPoolFactory.setPool(pool);
    }

    @AfterEach
    void tearDown() {
        pool.close();
    }

    @Test
    void testInit() throws Exception {
        initMock(sas);
        initMock(vla);

        pool.preparePool();

        verifyCreations(sas, "prefix_sas", 5, 2);
        verifyCreations(vla, "prefix_vla", 5, 2);
    }

    void verifyCreations(LogbrokerCluster cluster, String prefix, int partitions, int num) {
        ArgumentCaptor<AsyncProducerConfig> captor = ArgumentCaptor.forClass(AsyncProducerConfig.class);
        verify(cluster, times(partitions * num)).createAsyncProducer(captor.capture());
        verifyCreations(captor, prefix, partitions, num, 10);
    }

    void verifyCreations(ArgumentCaptor<AsyncProducerConfig> captor, String prefix, int partitions, int num, int count) {
        List<String> created = captor.getAllValues().stream()
                .map(AsyncProducerConfig::getSourceId)
                .map(String::new)
                .sorted()
                .collect(Collectors.toList());
        List<String> expected = new ArrayList<>();
        for (int p = 1; p <= partitions; p++) {
            for (int i = 0; i < num; i++) {
                expected.add(String.format("%s_%d_%d", prefix, p, i));
            }
        }
        expected = expected.subList(0, count);
        Assertions.assertEquals(
                expected,
                created
        );
    }

    @Test
    void testConnectWhenClusterIsDown() throws Exception {
        // ломаем один кластер
        doAnswer(inv -> {
            throw new CompletionException(new IllegalStateException());
        }).when(sas).createAsyncProducer(any());
        initMock(vla);

        pool.preparePool(); // пул нормально инициализируется

        InOrder inOrder = inOrder(sas, vla);
        inOrder.verify(sas, times(10)).createAsyncProducer(any());
        inOrder.verify(vla, times(10)).createAsyncProducer(any());

        // run evictor
        pool.setTimeBetweenEvictionRunsMillis(10);

        ArgumentCaptor<AsyncProducerConfig> captor = ArgumentCaptor.forClass(AsyncProducerConfig.class);
        // evictor пытается хотя бы раз переинициализировать писателей
        inOrder.verify(sas, timeout(10000).times(10)).createAsyncProducer(captor.capture());
        // stop evictor
        pool.setTimeBetweenEvictionRunsMillis(0);

        inOrder.verifyNoMoreInteractions();
        verifyCreations(captor, "prefix_sas", 5, 2, 10);
    }

    @Test
    void testReconnect() throws Exception {
        initMock(sas);
        initMock(vla);

        pool.preparePool();

        InOrder inOrder = inOrder(sas, vla);
        inOrder.verify(sas, times(10)).createAsyncProducer(any());
        inOrder.verify(vla, times(10)).createAsyncProducer(any());

        // ломаем 5 писателей
        for (int i = 0; i < 5; i++) {
            PooledSimpleAsyncProducer producer = pool.borrowObject(); // взяли и запомнили
            producer.close(); // вернули хорошим
            producer.closeFuture().complete(null); // в фоне сломали
        }
        // run evictor
        pool.setTimeBetweenEvictionRunsMillis(10);

        ArgumentCaptor<AsyncProducerConfig> captor = ArgumentCaptor.forClass(AsyncProducerConfig.class);
        // таймаут просто "для галочки" на случай сломанного теста
        // здесь мокито честно ждет 5 вызовов
        inOrder.verify(sas, timeout(10000). times(5)).createAsyncProducer(captor.capture());
        // stop evictor
        pool.setTimeBetweenEvictionRunsMillis(0);

        inOrder.verifyNoMoreInteractions();
        verifyCreations(captor, "prefix_sas", 5, 2, 5);
    }

    ProducerConfig config() {
        return new ProducerConfig.Builder()
                .setTopicName("test-topic")
                .setModuleName("test-app")
                .setSourceIdPrefix("prefix")
                .setPoolConfig(poolConfig())
                .configureUniformDistribution(
                        ImmutableMap.of(
                                "sas", sas,
                                "vla", vla),
                        5,
                        2)
                .build();
    }

    GenericObjectPoolConfig<PooledSimpleAsyncProducer> poolConfig() {
        GenericObjectPoolConfig<PooledSimpleAsyncProducer> config = new GenericObjectPoolConfig<>();
        config.setLifo(false);
        config.setTestOnBorrow(true);
        config.setTestOnReturn(true);
        config.setTestWhileIdle(true);
        return config;
    }

    void initMock(LogbrokerCluster cluster) {
        doAnswer(inv -> createMockProducer(inv.getArgument(0)))
                .when(cluster)
                .createAsyncProducer(any());
    }

    AsyncProducer createMockProducer(AsyncProducerConfig config) {
        AsyncProducer producer = mock(AsyncProducer.class);
        when(producer.init()).thenReturn(CompletableFuture.completedFuture(
                new ProducerInitResponse(1, "test-topic", config.getGroup() - 1, new String(config.getSourceId()))));
        CompletableFuture<Void> future = new CompletableFuture<>();
        when(producer.closeFuture()).thenReturn(future);
        return producer;
    }

}
