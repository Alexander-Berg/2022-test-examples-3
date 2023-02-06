package ru.yandex.direct.jobs.recommendations;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import javax.annotation.Nullable;

import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.direct.jobs.configuration.RecommendationsYtClustersParametersSource;
import ru.yandex.direct.jobs.fatconfiguration.JobsFatTest;
import ru.yandex.direct.ytwrapper.client.YtProvider;
import ru.yandex.direct.ytwrapper.model.YtCluster;
import ru.yandex.direct.ytwrapper.model.YtOperator;
import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTree;
import ru.yandex.inside.yt.kosher.ytree.YTreeNode;
import ru.yandex.inside.yt.kosher.ytree.YTreeStringNode;
import ru.yandex.yt.ytclient.proxy.YtClient;
import ru.yandex.yt.ytclient.proxy.request.CreateNode;
import ru.yandex.yt.ytclient.proxy.request.ObjectType;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.grid.core.entity.recommendation.RecommendationTablesUtils.createRecommendationsTable;
import static ru.yandex.direct.grid.core.entity.recommendation.RecommendationTablesUtils.generateTableName;

@JobsFatTest
@ExtendWith(SpringExtension.class)
class RecommendationsYTCleanupJobTests {
    private static final String LOCK_NODE_PATH = "/recommendations_lock";
    private static final String ARCHIVE_DIR_PATH = "/archive";
    private static final String CURRENT_SYMLINK_PATH = "/current";
    private static final String TEST_NODE_PATH = "//home/direct/tmp/test_recommendations";

    @Autowired
    private YtProvider ytProvider;
    /**
     * Yt client via http proxy
     */
    private YtOperator ytOperator;
    /**
     * Yt client via rpc proxy
     */
    private YtClient ytClient;
    /**
     * Собственно, джоба, подготовленная к выполнению.
     */
    private RecommendationsYTCleanupJob job;
    /**
     * Путь к директории с рекомендациями в yt.
     */
    private static String baseDir = TEST_NODE_PATH;
    private static String lockNode = baseDir + LOCK_NODE_PATH;
    private static String archiveDir = baseDir + ARCHIVE_DIR_PATH;
    private static String currentSymlink = baseDir + CURRENT_SYMLINK_PATH;

    private long timestamp;
    private String directory;
    private String tableName = null;
    public boolean isOutdated = false;


    @BeforeEach
    void before() {
        ytOperator = ytProvider.getOperator(YtCluster.YT_LOCAL);
        ytClient = ytProvider.getDynamicOperator(YtCluster.YT_LOCAL).getYtClient();

        RecommendationsYtClustersParametersSource parametersSource =
                new RecommendationsYtClustersParametersSource(singletonList(YtCluster.YT_LOCAL));

        // Создаем базовую директорию для выполнения тестов
        ytClient.createNode(
                new CreateNode(YPath.simple(baseDir).toString(), ObjectType.MapNode)
                        .setRecursive(true)
                        .setIgnoreExisting(false)).join();

        job = new RecommendationsYTCleanupJob(ytProvider, parametersSource, new YtLastAccessTsLoader() {
            // Подменяем метод, используемый джобой для определения timestamp у файлов
            @Override
            public long getLastAccessTs(YTreeNode node) {
                return timestamp;
            }
        }, baseDir);

        initStructure();
    }

    @AfterEach
    public void after() {
        // Удаляем созданную для тестов директорию
        ytOperator.getYt().cypress().remove(YPath.simple(baseDir));
    }

    private YPath prepareDataAndRunJob() {
        YPath table = createTable(YPath.simple(directory),
                tableName != null ? YPath.simple(directory).child(tableName) : null);

        // Устанавливаем необходимое значение timestamp
        generateTimestampForDelete(isOutdated);

        // Выполним удаление
        job.executeCore(YtCluster.YT_LOCAL);

        return table;
    }

    @Test
    @DisplayName("Создаем устаревшую таблицу и проверяем, что она удалилась")
    void oldTableDeleted() {
        directory = baseDir;
        isOutdated = true;
        var table = prepareDataAndRunJob();

        assertThat(listCypressDir()).doesNotContain(YTree.stringNode(table.name()));
    }

    @Test
    @DisplayName("Создаем устаревшую таблицу в архивной директории и проверяем, что она удалилась")
    void oldArchivedTableDeleted() {
        directory = archiveDir;
        isOutdated = true;
        var table = prepareDataAndRunJob();

        assertThat(listCypressDir()).doesNotContain(YTree.stringNode(table.name()));
    }

    @Test
    @DisplayName("Создаем свежую таблицу и проверяем, что она тоже удалилась (она не current)")
    void freshTableDeleted() {
        directory = baseDir;
        var table = prepareDataAndRunJob();

        assertThat(listCypressDir()).doesNotContain(YTree.stringNode(table.name()));
    }

    @Test
    @DisplayName("Создаем свежую таблицу в архивной директории и проверяем, что она не удалилась")
    void freshArchivedTableNotDeleted() {
        directory = archiveDir;
        var table = prepareDataAndRunJob();

        assertThat(listCypressDir()).contains(YTree.stringNode(table.name()));
    }

    @Test
    @DisplayName("Создаем устаревшую таблицу с нестандартным именем и проверяем, что она не удалилась")
    void oldTableWithUnusualNameNotDeleted() {
        directory = baseDir;
        tableName = "test" + new SimpleDateFormat("yyyyMMdd_HHmmssSSS").format(new Date());
        isOutdated = true;
        var table = prepareDataAndRunJob();

        assertThat(listCypressDir()).contains(YTree.stringNode(table.name()));
    }

    @Test
    @DisplayName("Создаем свежую таблицу с нестандартным именем и проверяем, что она не удалилась")
    void freshTableWithUnusualNameNotDeleted() {
        directory = baseDir;
        tableName = "test" + new SimpleDateFormat("yyyyMMdd_HHmmssSSS").format(new Date());
        var table = prepareDataAndRunJob();

        assertThat(listCypressDir()).contains(YTree.stringNode(table.name()));
    }

    @Test
    @DisplayName("Создаем устаревшую таблицу с нестандартным именем в архивной директории и проверяем" +
            ", что она удалилась")
    void oldArchivedTableWithUnusualNameDeleted() {
        directory = archiveDir;
        tableName = "test" + new SimpleDateFormat("yyyyMMdd_HHmmssSSS").format(new Date());
        isOutdated = true;
        var table = prepareDataAndRunJob();

        assertThat(listCypressDir()).doesNotContain(YTree.stringNode(table.name()));
    }

    @Test
    @DisplayName("Создаем свежую таблицу с нестандартным именем в архивной директории и проверяем" +
            ", что она не удалилась")
    void freshArchivedTableWithUnusualNameNotDeleted() {
        directory = archiveDir;
        tableName = "test" + new SimpleDateFormat("yyyyMMdd_HHmmssSSS").format(new Date());
        var table = prepareDataAndRunJob();

        assertThat(listCypressDir()).contains(YTree.stringNode(table.name()));
    }


    private void initStructure() {
        // Проинициализируем структуру директории для тестов
        if (!ytClient.existsNode(lockNode).join()) {
            ytClient.createNode(
                    new CreateNode(lockNode, ObjectType.BooleanNode)
                            .setRecursive(true)
                            .setIgnoreExisting(false)
            ).join();
        }

        if (!ytClient.existsNode(archiveDir).join()) {
            ytClient.createNode(
                    new CreateNode(archiveDir, ObjectType.MapNode)
                            .setRecursive(true)
                            .setIgnoreExisting(false)
            ).join();
        }

        if (!ytClient.existsNode(currentSymlink).join()) {
            YPath table = YPath.simple(baseDir).child("recommendations_current");
            // используем primary_medium=default, т.к. в локальном YT ssd_blobs нет
            createRecommendationsTable(ytOperator, Optional.empty(), table, true, true, "default", emptyMap());

            ytClient.createNode(
                    new CreateNode(currentSymlink, ObjectType.Link)
                            .setAttributes(ImmutableMap.of("target_path", YTree.stringNode(table.toString())))
                            .setRecursive(true)
                            .setForce(true)
            ).join();
        }
    }

    /**
     * Генерирует актуальный или устаревший timestamp в зависимости от переданного значения isOutdated
     */
    private void generateTimestampForDelete(boolean isOutdated) {
        timestamp = Instant.now()
                .minus(isOutdated ? RecommendationsYTCleanupJob.ACTUALITY_DAYS_LIMIT : 0, ChronoUnit.DAYS)
                .truncatedTo(ChronoUnit.DAYS)
                .atZone(ZoneId.systemDefault())
                .toEpochSecond();
    }

    private List<YTreeStringNode> listCypressDir() {
        return ytOperator.getYt().cypress().list(YPath.simple(directory));
    }

    /**
     * Создает пустую таблицу рекоммендаций в заданной директории
     */
    private YPath createTable(YPath directory, @Nullable YPath tableName) {
        YPath table = tableName == null ? generateTableName(directory) : tableName;
        createRecommendationsTable(ytOperator, Optional.empty(), table, true, true, "default", emptyMap());
        return table;
    }
}
