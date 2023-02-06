package ru.yandex.autotests.market.billing.backend.barc;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringRunner;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.aqua.annotations.project.Feature;
import ru.yandex.autotests.market.billing.backend.core.dao.yt.YtClient;
import ru.yandex.autotests.market.billing.backend.core.dao.yt.configuration.MbiDataArchiverYTConfiguration;
import ru.yandex.autotests.market.billing.backend.steps.BarcDaoSteps;
import ru.yandex.autotests.market.billing.backend.steps.archiver.ArchivingSteps;
import ru.yandex.autotests.market.billing.backend.steps.archiver.SchemaUpgradeTestExecutionListener;
import ru.yandex.autotests.market.common.spring.CommonSpringTest;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.inside.yt.kosher.ytree.YTreeNode;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Title;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.context.TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS;
import static ru.yandex.autotests.market.billing.backend.core.dao.yt.YtClient.SCHEMA;
import static ru.yandex.autotests.market.billing.backend.steps.archiver.SchemaUpgradeTestExecutionListener.SCHEMA_UPGRADE_EXECUTOR;
import static ru.yandex.autotests.market.billing.backend.steps.archiver.SchemaUpgradeTestExecutionListener.SCHEMA_UPGRADE_TEST_TABLE;

/**
 * @author Dmitriy Poluyanov <a href="mailto:neiwick@yandex-team.ru">Dmitriy Poluyanov</a>
 * @since 23.05.17
 */
@Aqua.Test(title = "Тест архиваторной джобы schemaUpgradeExecutor")
@Feature("barc")
@Issue("AUTOTESTMARKET-6159")
@CommonSpringTest(classes = {
        ArchivingSteps.class,
        BarcDaoSteps.class,
        MbiDataArchiverYTConfiguration.class})
@TestExecutionListeners(listeners = SchemaUpgradeTestExecutionListener.class, mergeMode = MERGE_WITH_DEFAULTS)
@RunWith(SpringRunner.class)
public class SchemaUpgradeExecutorTest {

    @Autowired
    private ArchivingSteps archivingSteps;

    @Autowired
    private YtClient mainArchiveYTClient;

    @Autowired
    private YtClient replicaArchiveYTClient;

    @Test
    @Title("Джоба schemaUpgradeExecutor отработала без ошибок")
    public void testCorrectSchemaUpgrade() throws IOException {
        archivingSteps.checkArchiverJob(SCHEMA_UPGRADE_EXECUTOR);
    }

    @Test
    @Title("Джоба schemaUpgradeExecutor обновила схему всех таблиц на основном кластере")
    public void testSchemaUpgradeOnPrimaryCluster() throws IOException {
        Map<String, String> notUpgradedEntities = new LinkedHashMap<>();

        SchemaUpgradeTestExecutionListener.getMainTables()
                .forEach((entity, columnName) -> {
                    YTreeNode node = mainArchiveYTClient.get(mainArchiveYTClient.getRootPath(), entity, SCHEMA_UPGRADE_TEST_TABLE);

                    ListF<YTreeNode> tableSchema = node.getAttributeOrThrow(SCHEMA).asList();
                    Option<YTreeNode> nameOptional = tableSchema.find(yTreeNode -> yTreeNode
                            .asMap().getOrThrow("name")
                            .stringValue().equals(columnName));

                    if (!nameOptional.isPresent()) {
                        notUpgradedEntities.put(entity, columnName);
                    }
                });

        assertThat(notUpgradedEntities)
                .withFailMessage("Новые колонки должны добавиться во всех сущностях")
                .isEmpty();
    }

    @Test
    @Title("Джоба schemaUpgradeExecutor обновила схему всех таблиц на реплике")
    public void testSchemaUpgradeOnReplicaCluster() throws IOException {
        Map<String, String> notUpgradedEntities = new LinkedHashMap<>();

        SchemaUpgradeTestExecutionListener.getReplicaTables()
                .forEach((entity, columnName) -> {
                    YTreeNode node = replicaArchiveYTClient.get(replicaArchiveYTClient.getRootPath(), entity, SCHEMA_UPGRADE_TEST_TABLE);

                    ListF<YTreeNode> tableSchema = node.getAttributeOrThrow(SCHEMA).asList();
                    Option<YTreeNode> nameOptional = tableSchema.find(yTreeNode -> yTreeNode
                            .asMap().getOrThrow("name")
                            .stringValue().equals(columnName));

                    if (!nameOptional.isPresent()) {
                        notUpgradedEntities.put(entity, columnName);
                    }
                });

        assertThat(notUpgradedEntities)
                .withFailMessage("Новые колонки должны добавиться во всех сущностях")
                .isEmpty();
    }
}
