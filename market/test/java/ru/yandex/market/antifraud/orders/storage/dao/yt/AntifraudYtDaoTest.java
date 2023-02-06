package ru.yandex.market.antifraud.orders.storage.dao.yt;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.netty.channel.nio.NioEventLoopGroup;
import lombok.SneakyThrows;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import ru.yandex.bolts.collection.impl.EmptyMap;
import ru.yandex.inside.yt.kosher.impl.ytree.YTreeIntegerNodeImpl;
import ru.yandex.inside.yt.kosher.impl.ytree.YTreeListNodeImpl;
import ru.yandex.market.antifraud.orders.storage.entity.antifraud.AccountState;
import ru.yandex.market.antifraud.orders.storage.entity.antifraud.PassportFeatures;
import ru.yandex.market.request.trace.Module;
import ru.yandex.market.volva.utils.trace.TracingYtClient;
import ru.yandex.yt.ytclient.bus.DefaultBusConnector;
import ru.yandex.yt.ytclient.proxy.YtClient;
import ru.yandex.yt.ytclient.proxy.YtCluster;
import ru.yandex.yt.ytclient.rpc.RpcCredentials;
import ru.yandex.yt.ytclient.rpc.RpcOptions;
import ru.yandex.yt.ytclient.tables.ColumnSchema;
import ru.yandex.yt.ytclient.tables.ColumnSortOrder;
import ru.yandex.yt.ytclient.tables.ColumnValueType;
import ru.yandex.yt.ytclient.tables.TableSchema;
import ru.yandex.yt.ytclient.wire.UnversionedRow;
import ru.yandex.yt.ytclient.wire.UnversionedRowset;
import ru.yandex.yt.ytclient.wire.UnversionedValue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * @author Alexander Novikov <a href="mailto:hronos@yandex-team.ru"></a>
 * @date 10.02.2020
 */
public class AntifraudYtDaoTest {
    private final YtClient antifraudYtClient = Mockito.mock(YtClient.class);

    private final AntifraudYtDao antifraudYtDao = new AntifraudYtDao(antifraudYtClient);

    @SneakyThrows
    @Before
    public void init() {
        when(antifraudYtClient.waitProxies()).thenReturn(CompletableFuture.completedFuture(null));
    }

    @Test
    public void getAccountStateByUidTest() {
        TableSchema tableSchema = new TableSchema.Builder()
                .addAll(List.of(
                        new ColumnSchema("uid", ColumnValueType.UINT64, ColumnSortOrder.ASCENDING),
                        new ColumnSchema("karma", ColumnValueType.UINT64),
                        new ColumnSchema("created_rules", ColumnValueType.ANY)
                ))
                .setStrict(true)
                .setUniqueKeys(true)
                .build();

        YTreeListNodeImpl createdRules = new YTreeListNodeImpl(new EmptyMap<>());
        createdRules.add(new YTreeIntegerNodeImpl(false, 1, new EmptyMap<>()));
        createdRules.add(new YTreeIntegerNodeImpl(false, 2, new EmptyMap<>()));
        createdRules.add(new YTreeIntegerNodeImpl(false, 3, new EmptyMap<>()));
        List<UnversionedRow> unversionedRows = List.of(
                new UnversionedRow(List.of(
                        new UnversionedValue(0, ColumnValueType.UINT64, false, 433348306L),  //uid
                        new UnversionedValue(1, ColumnValueType.UINT64, false, 6000L),       //karma
                        new UnversionedValue(2, ColumnValueType.ANY, false,
                                UnversionedValue.convertValueTo(createdRules, ColumnValueType.ANY))
                        //created_rules
                )),
                new UnversionedRow(List.of(
                        new UnversionedValue(0, ColumnValueType.UINT64, false, 433348305L),  //uid
                        new UnversionedValue(1, ColumnValueType.UINT64, false, 5000L),       //karma
                        new UnversionedValue(2, ColumnValueType.ANY, false,
                                UnversionedValue.convertValueTo(createdRules, ColumnValueType.ANY))
                        //created_rules
                ))
        );

        UnversionedRowset unversionedRowset = new UnversionedRowset(tableSchema, unversionedRows);

        CompletableFuture<UnversionedRowset> completableFuture = CompletableFuture.completedFuture(unversionedRowset);
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        when(antifraudYtClient.selectRows(captor.capture())).thenReturn(completableFuture);

        Optional<AccountState> accountStateO = antifraudYtDao.getAccountStateByUid(433348306L).join();
        assertThat(captor.getValue()).isEqualTo("uid, karma, created_rules " +
                "FROM [//home/market/production/mstat/antifraud/accounts/accounts_state] " +
                "WHERE uid=433348306");

        assertThat(accountStateO.isPresent()).isTrue();
        AccountState accountState = accountStateO.get();
        assertThat(accountState.getKarma()).isEqualTo(6000L);
        assertThat(accountState.getUid()).isEqualTo(433348306L);
        assertThat(accountState.getHasRules()).isTrue();

        verify(antifraudYtClient).waitProxies();
        verify(antifraudYtClient).selectRows(any(String.class));
        verifyNoMoreInteractions(antifraudYtClient);
    }

    @Test
    @SneakyThrows
    public void getPassportFeaturesByUidTest() {
        TableSchema tableSchema = new TableSchema.Builder()
                .addAll(List.of(
                        new ColumnSchema("uid", ColumnValueType.UINT64, ColumnSortOrder.ASCENDING),
                        new ColumnSchema("karma", ColumnValueType.STRING),
                        new ColumnSchema("account_created", ColumnValueType.BOOLEAN),
                        new ColumnSchema("rules", ColumnValueType.ANY)

                ))
                .setStrict(true)
                .setUniqueKeys(true)
                .build();

        YTreeListNodeImpl rules = new YTreeListNodeImpl(new EmptyMap<>());
        rules.add(new YTreeIntegerNodeImpl(false, 1, new EmptyMap<>()));
        rules.add(new YTreeIntegerNodeImpl(false, 2, new EmptyMap<>()));
        rules.add(new YTreeIntegerNodeImpl(false, 3, new EmptyMap<>()));

        List<UnversionedRow> unversionedRows = List.of(new UnversionedRow(List.of(
                new UnversionedValue(0, ColumnValueType.UINT64, false, 433348306L),     //uid
                new UnversionedValue(1, ColumnValueType.STRING, false, "6000".getBytes()),    //karma
                new UnversionedValue(2, ColumnValueType.BOOLEAN, false, true),    //account_created
                new UnversionedValue(3, ColumnValueType.ANY, false,
                        UnversionedValue.convertValueTo(rules, ColumnValueType.ANY))
                ))
        );

        UnversionedRowset unversionedRowset = new UnversionedRowset(tableSchema, unversionedRows);

        CompletableFuture<UnversionedRowset> completableFuture = CompletableFuture.completedFuture(unversionedRowset);
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        when(antifraudYtClient.selectRows(captor.capture())).thenReturn(completableFuture);

        Optional<PassportFeatures> passportFeaturesO = antifraudYtDao.getPassportFeaturesByUid(433348306L).join();
        assertThat(captor.getValue()).isEqualTo("uid, karma, account_created, rules, " +
                "is_hosting_ip, is_suggested_login, age, is_proxy_ip, is_mobile_ip, is_vpn_ip " +
                "FROM [//home/market/production/mstat/antifraud/accounts/passport_features_fast] " +
                "WHERE uid=433348306");

        assertThat(passportFeaturesO.isPresent()).isTrue();
        PassportFeatures passportFeatures = passportFeaturesO.get();
        assertThat(passportFeatures.getKarma()).isEqualTo(6000L);
        assertThat(passportFeatures.getUid()).isEqualTo(433348306L);
        assertThat(passportFeatures.getAccountCreated()).isTrue();
        assertThat(passportFeatures.getHasRules()).isTrue();

        verify(antifraudYtClient).waitProxies();
        verify(antifraudYtClient).selectRows(any(String.class));
        verifyNoMoreInteractions(antifraudYtClient);
    }

    @Test
    @Ignore
    public void smokeTest() {
        YtClient client = new TracingYtClient(new DefaultBusConnector(new NioEventLoopGroup(0), true),
            Stream.of("hahn").map(YtCluster::new).collect(Collectors.toList()),
            System.getProperty("datacenter", "sas"),
            new RpcCredentials("robot-mrkt-antfrd", "secret"),
            new RpcOptions(),
            Module.MSTAT_ANTIFRAUD_ORDERS);
        AntifraudYtDao ytDao = new AntifraudYtDao(client);
        System.out.println(ytDao.getAccountStateByUid(311870044L).join());
        System.out.println(ytDao.getPassportFeaturesByUid(311870044L).join());
    }
}
