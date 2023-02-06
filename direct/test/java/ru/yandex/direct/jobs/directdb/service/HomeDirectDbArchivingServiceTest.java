package ru.yandex.direct.jobs.directdb.service;

import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.context.ApplicationContext;

import ru.yandex.bolts.collection.impl.ArrayListF;
import ru.yandex.bolts.collection.impl.DefaultMapF;
import ru.yandex.bolts.collection.impl.EmptyMap;
import ru.yandex.direct.ytwrapper.YtPathUtil;
import ru.yandex.direct.ytwrapper.client.YtClusterConfig;
import ru.yandex.direct.ytwrapper.client.YtProvider;
import ru.yandex.direct.ytwrapper.model.YtCluster;
import ru.yandex.direct.ytwrapper.model.YtDynamicOperator;
import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.inside.yt.kosher.common.DataSize;
import ru.yandex.inside.yt.kosher.common.GUID;
import ru.yandex.inside.yt.kosher.cypress.Cypress;
import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.inside.yt.kosher.impl.ytree.YTreeBooleanNodeImpl;
import ru.yandex.inside.yt.kosher.impl.ytree.YTreeDoubleNodeImpl;
import ru.yandex.inside.yt.kosher.impl.ytree.YTreeIntegerNodeImpl;
import ru.yandex.inside.yt.kosher.impl.ytree.YTreeListNodeImpl;
import ru.yandex.inside.yt.kosher.impl.ytree.YTreeMapNodeImpl;
import ru.yandex.inside.yt.kosher.impl.ytree.YTreeStringNodeImpl;
import ru.yandex.inside.yt.kosher.operations.Operation;
import ru.yandex.inside.yt.kosher.operations.OperationStatus;
import ru.yandex.inside.yt.kosher.operations.YtOperations;
import ru.yandex.inside.yt.kosher.operations.specs.JobIo;
import ru.yandex.inside.yt.kosher.operations.specs.MergeMode;
import ru.yandex.inside.yt.kosher.operations.specs.MergeSpec;
import ru.yandex.inside.yt.kosher.tables.TableWriterOptions;
import ru.yandex.inside.yt.kosher.transactions.YtTransactions;
import ru.yandex.inside.yt.kosher.ytree.YTreeNode;
import ru.yandex.inside.yt.kosher.ytree.YTreeStringNode;
import ru.yandex.yt.ytclient.proxy.ApiServiceTransaction;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.jobs.util.yt.YtEnvPath.relativePart;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;

@ParametersAreNonnullByDefault
@DisplayName("Архивирование таблиц для более эффективного хранения")
class HomeDirectDbArchivingServiceTest {

    private static final String YT_HOME = "//home/direct";

    @Mock
    private YtProvider ytProvider;

    @Mock
    private YtClusterConfig clusterConfig;

    @Mock
    private Yt yt;

    @Mock
    private Cypress cypress;

    @Mock
    private YtOperations operations;

    @Mock
    private YtTransactions transactions;

    @Mock
    private ApplicationContext applicationContext;

    @Mock
    private HomeDirectDbFullWorkPropObtainerService fullWorkPropObtainerService;

    @Mock
    private Operation operation;

    @Mock
    private YtDynamicOperator ytDynamicOperator;

    @Mock
    private ApiServiceTransaction transaction;

    @InjectMocks
    private HomeDirectDbArchivingService service;

    @Captor
    private ArgumentCaptor<MergeSpec> mergeSpecArgumentCaptor;

    private GUID guid = GUID.create();

    @BeforeEach
    void setUp() {
        initMocks(this);

        given(fullWorkPropObtainerService.isFullWorkEnabled()).willReturn(true);
        given(ytProvider.getClusterConfig(any())).willReturn(clusterConfig);
        given(clusterConfig.getHome()).willReturn(YT_HOME);
        given(ytProvider.get(any())).willReturn(yt);
        given(yt.cypress()).willReturn(cypress);
        given(yt.operations()).willReturn(operations);
        given(yt.transactions()).willReturn(transactions);
        given(applicationContext.getBean(eq(HomeDirectDbArchivingService.class))).willReturn(service);
        given(operations.mergeAndGetOp(any(), anyBoolean(), any())).willReturn(operation);
        given(operation.getStatus()).willReturn(OperationStatus.COMPLETED);

        given(transaction.getId()).willReturn(guid);
        doAnswer(invocation -> {
            Consumer<ApiServiceTransaction> consumer = invocation.getArgument(0);
            consumer.accept(transaction);
            return null;
        }).when(ytDynamicOperator).runInTransaction(any(), any());
        given(ytProvider.getDynamicOperator(any())).willReturn(ytDynamicOperator);
    }

    @Test
    @DisplayName("Должен архивировать таблицы из папок которые старше, чем сколько-то дней")
    void shouldArchiveTablesFromFoldersOlderThanSomeHowMuchDays() {
        var folders = new ArrayListF<YTreeStringNode>();
        folders.add(new YTreeStringNodeImpl("2020-01-01", new EmptyMap<>()));
        var sortedBy = new YTreeListNodeImpl(new EmptyMap<>());
        sortedBy.add(new YTreeStringNodeImpl("cid", new EmptyMap<>()));
        var tables = new ArrayListF<YTreeStringNode>();
        tables.add(new YTreeStringNodeImpl("banners", new EmptyMap<>()));
        given(cypress.list(any(YPath.class))).willReturn(folders).willReturn(tables);
        var table = new YTreeMapNodeImpl(new EmptyMap<>());
        table.put("sorted_by", sortedBy);
        table.put("sorted", new YTreeBooleanNodeImpl(true, new EmptyMap<>()));
        table.put("compression_codec", new YTreeStringNodeImpl("zstd_5", new EmptyMap<>()));
        table.put("erasure_codec", new YTreeStringNodeImpl("none", new EmptyMap<>()));
        table.put("compression_ratio", new YTreeDoubleNodeImpl(0.4217797723489037, new EmptyMap<>()));
        var attrPath = YPath.simple(YtPathUtil.generatePath(
                YT_HOME, relativePart(), "db-archive/2020-01-01/banners/@"));
        given(cypress.get(eq(attrPath))).willReturn(table);

        service.archive(YtCluster.HAHN, LocalDate.of(2020, 1, 17));

        var compressionCodecAttributePath = YtPathUtil.generatePath(
                YT_HOME,
                relativePart(),
                "db-archive/2020-01-01/banners/@compression_codec"
        );
        verify(cypress).set(
                eq(Optional.of(guid)),
                eq(false),
                eq(YPath.simple(compressionCodecAttributePath)),
                eq(new YTreeStringNodeImpl("brotli_8", new EmptyMap<>()))
        );
        var erasureCodecAttributePath = YtPathUtil.generatePath(
                YT_HOME,
                relativePart(),
                "db-archive/2020-01-01/banners/@erasure_codec"
        );
        verify(cypress).set(
                eq(Optional.of(guid)),
                eq(false),
                eq(YPath.simple(erasureCodecAttributePath)),
                eq(new YTreeStringNodeImpl("lrc_12_2_2", new EmptyMap<>()))
        );
        var mergeSpec = new MergeSpec(
                singletonList(YtPathUtil.generatePath(YT_HOME, relativePart(), "db-archive/2020-01-01/banners")),
                YtPathUtil.generatePath(YT_HOME, relativePart(), "db-archive/2020-01-01/banners"))
          .toBuilder()
                .setMergeMode(MergeMode.SORTED)
                .setCombineChunks(true)
                .setAdditionalSpecParameters(DefaultMapF.wrap(Map.of(
                        "force_transform", new YTreeBooleanNodeImpl(true, new EmptyMap<>()),
                        "data_size_per_job", new YTreeIntegerNodeImpl(false, 2147483647, new EmptyMap<>())
                )))
                .setJobIo(new JobIo(new TableWriterOptions().withDesiredChunkSize(DataSize.fromBytes(68719476736L))))
                .build();

        verify(operations).mergeAndGetOp(eq(Optional.of(guid)), eq(false), mergeSpecArgumentCaptor.capture());
        assertThat(mergeSpec).is(matchedBy(beanDiffer(mergeSpecArgumentCaptor.getValue())));
    }

    @Test
    @DisplayName("Должен оставить статус сортировки при трансформации (таблица не была отсортирована)")
    void shouldLeaveUnsortedWhenWasUnsorted() {
        var folders = new ArrayListF<YTreeStringNode>();
        folders.add(new YTreeStringNodeImpl("2020-01-01", new EmptyMap<>()));
        var tables = new ArrayListF<YTreeStringNode>();
        tables.add(new YTreeStringNodeImpl("banners", new EmptyMap<>()));
        given(cypress.list(any(YPath.class))).willReturn(folders).willReturn(tables);
        var table = new YTreeMapNodeImpl(new EmptyMap<>());
        table.put("sorted", new YTreeBooleanNodeImpl(false, new EmptyMap<>()));
        table.put("compression_codec", new YTreeStringNodeImpl("zstd_5", new EmptyMap<>()));
        table.put("erasure_codec", new YTreeStringNodeImpl("none", new EmptyMap<>()));
        table.put("compression_ratio", new YTreeDoubleNodeImpl(0.4217797723489037, new EmptyMap<>()));
        var attrPath = YPath.simple(YtPathUtil.generatePath(
                YT_HOME, relativePart(), "db-archive/2020-01-01/banners/@"));
        given(cypress.get(eq(attrPath))).willReturn(table);

        service.archive(YtCluster.HAHN, LocalDate.of(2020, 1, 17));

        var compressionCodecAttributePath = YtPathUtil.generatePath(
                YT_HOME,
                relativePart(),
                "db-archive/2020-01-01/banners/@compression_codec"
        );
        verify(cypress).set(
                eq(Optional.of(guid)),
                eq(false),
                eq(YPath.simple(compressionCodecAttributePath)),
                eq(new YTreeStringNodeImpl("brotli_8", new EmptyMap<>()))
        );
        var erasureCodecAttributePath = YtPathUtil.generatePath(
                YT_HOME,
                relativePart(),
                "db-archive/2020-01-01/banners/@erasure_codec"
        );
        verify(cypress).set(
                eq(Optional.of(guid)),
                eq(false),
                eq(YPath.simple(erasureCodecAttributePath)),
                eq(new YTreeStringNodeImpl("lrc_12_2_2", new EmptyMap<>()))
        );
        var mergeSpec = new MergeSpec(
                singletonList(YtPathUtil.generatePath(YT_HOME, relativePart(), "db-archive/2020-01-01/banners")),
                YtPathUtil.generatePath(YT_HOME, relativePart(), "db-archive/2020-01-01/banners"))
          .toBuilder()
                .setMergeMode(MergeMode.UNORDERED)
                .setCombineChunks(true)
                .setAdditionalSpecParameters(DefaultMapF.wrap(Map.of(
                        "force_transform", new YTreeBooleanNodeImpl(true, new EmptyMap<>()),
                        "data_size_per_job", new YTreeIntegerNodeImpl(false, 2147483647, new EmptyMap<>())
                )))
                .setJobIo(new JobIo(new TableWriterOptions().withDesiredChunkSize(DataSize.fromBytes(68719476736L))))
                .build();

        verify(operations).mergeAndGetOp(eq(Optional.of(guid)), eq(false), mergeSpecArgumentCaptor.capture());
        assertThat(mergeSpec).is(matchedBy(beanDiffer(mergeSpecArgumentCaptor.getValue())));
    }

    @Test
    @DisplayName("Должен взять стандартное значение для data_size_per_job если compression_ratio слишком большой")
    void shouldGetDefaultDataSizePerJobIfCompressionRationIs1000() {
        var folders = new ArrayListF<YTreeStringNode>();
        folders.add(new YTreeStringNodeImpl("2020-01-01", new EmptyMap<>()));
        var sortedBy = new YTreeListNodeImpl(new EmptyMap<>());
        sortedBy.add(new YTreeStringNodeImpl("cid", new EmptyMap<>()));
        var tables = new ArrayListF<YTreeStringNode>();
        tables.add(new YTreeStringNodeImpl("banners", new EmptyMap<>()));
        given(cypress.list(any(YPath.class))).willReturn(folders).willReturn(tables);
        var table = new YTreeMapNodeImpl(new EmptyMap<>());
        table.put("sorted_by", sortedBy);
        table.put("sorted", new YTreeBooleanNodeImpl(true, new EmptyMap<>()));
        table.put("compression_codec", new YTreeStringNodeImpl("zstd_5", new EmptyMap<>()));
        table.put("erasure_codec", new YTreeStringNodeImpl("none", new EmptyMap<>()));
        table.put("compression_ratio", new YTreeDoubleNodeImpl(1000., new EmptyMap<>()));
        var attrPath = YPath.simple(YtPathUtil.generatePath(
                YT_HOME, relativePart(), "db-archive/2020-01-01/banners/@"));
        given(cypress.get(eq(attrPath))).willReturn(table);

        service.archive(YtCluster.HAHN, LocalDate.of(2020, 1, 17));

        var compressionCodecAttributePath = YtPathUtil.generatePath(
                YT_HOME,
                relativePart(),
                "db-archive/2020-01-01/banners/@compression_codec"
        );
        verify(cypress).set(
                eq(Optional.of(guid)),
                eq(false),
                eq(YPath.simple(compressionCodecAttributePath)),
                eq(new YTreeStringNodeImpl("brotli_8", new EmptyMap<>()))
        );
        var erasureCodecAttributePath = YtPathUtil.generatePath(
                YT_HOME,
                relativePart(),
                "db-archive/2020-01-01/banners/@erasure_codec"
        );
        verify(cypress).set(
                eq(Optional.of(guid)),
                eq(false),
                eq(YPath.simple(erasureCodecAttributePath)),
                eq(new YTreeStringNodeImpl("lrc_12_2_2", new EmptyMap<>()))
        );
        var mergeSpec = new MergeSpec(
                singletonList(YtPathUtil.generatePath(YT_HOME, relativePart(), "db-archive/2020-01-01/banners")),
                YtPathUtil.generatePath(YT_HOME, relativePart(), "db-archive/2020-01-01/banners"))
          .toBuilder()
                .setMergeMode(MergeMode.SORTED)
                .setCombineChunks(true)
                .setAdditionalSpecParameters(DefaultMapF.wrap(Map.of(
                        "force_transform", new YTreeBooleanNodeImpl(true, new EmptyMap<>()),
                        "data_size_per_job", new YTreeIntegerNodeImpl(false, 268435456, new EmptyMap<>())
                )))
                .setJobIo(new JobIo(new TableWriterOptions().withDesiredChunkSize(DataSize.fromBytes(68719476736L))))
                .build();

        verify(operations).mergeAndGetOp(eq(Optional.of(guid)), eq(false), mergeSpecArgumentCaptor.capture());
        assertThat(mergeSpec).is(matchedBy(beanDiffer(mergeSpecArgumentCaptor.getValue())));
    }

    @Test
    @DisplayName("Если таблица уже сжата - ничего не делаем")
    void shouldDoNothingIfTableAlreadyCompressed() {
        var folders = new ArrayListF<YTreeStringNode>();
        folders.add(new YTreeStringNodeImpl("2020-01-01", new EmptyMap<>()));
        var sortedBy = new YTreeListNodeImpl(new EmptyMap<>());
        sortedBy.add(new YTreeStringNodeImpl("cid", new EmptyMap<>()));
        var tables = new ArrayListF<YTreeStringNode>();
        tables.add(new YTreeStringNodeImpl("banners", new EmptyMap<>()));
        given(cypress.list(any(YPath.class))).willReturn(folders).willReturn(tables);
        var table = new YTreeMapNodeImpl(new EmptyMap<>());
        table.put("sorted_by", sortedBy);
        table.put("sorted", new YTreeBooleanNodeImpl(true, new EmptyMap<>()));
        table.put("compression_codec", new YTreeStringNodeImpl("brotli_8", new EmptyMap<>()));
        table.put("erasure_codec", new YTreeStringNodeImpl("lrc_12_2_2", new EmptyMap<>()));
        table.put("compression_ratio", new YTreeDoubleNodeImpl(0.4217797723489037, new EmptyMap<>()));
        var attrPath = YPath.simple(YtPathUtil.generatePath(
                YT_HOME, relativePart(), "db-archive/2020-01-01/banners/@"));
        given(cypress.get(eq(attrPath))).willReturn(table);

        service.archive(YtCluster.HAHN, LocalDate.of(2020, 1, 17));

        verify(cypress, never()).set(any(YPath.class), any(YTreeNode.class));

        verify(operations, never()).merge(any());
    }

    @Test
    @DisplayName("Если папка - не дата, ничего не делаем, значит это либо ссылка, либо служебная таблица")
    void shouldDoNothingIfFolderNameIsNotADate() {
        var folders = new ArrayListF<YTreeStringNode>();
        folders.add(new YTreeStringNodeImpl("current", new EmptyMap<>()));

        given(cypress.list(any(YPath.class))).willReturn(folders);

        service.archive(YtCluster.HAHN, LocalDate.of(2020, 1, 17));

        verify(cypress, never()).set(any(YPath.class), any(YTreeNode.class));

        verify(operations, never()).merge(any());
    }

    @Test
    @DisplayName("Рано архивировать - ничего не делаем")
    void shouldDoNothingIfArchivingDaysNotGone() {
        var folders = new ArrayListF<YTreeStringNode>();
        folders.add(new YTreeStringNodeImpl("2020-01-16", new EmptyMap<>()));

        given(cypress.list(any(YPath.class))).willReturn(folders);

        service.archive(YtCluster.HAHN, LocalDate.of(2020, 1, 17));

        verify(cypress, never()).set(any(YPath.class), any(YTreeNode.class));

        verify(operations, never()).merge(any());
    }
}
