package ru.yandex.market.mbo.synchronizer.export;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.springframework.jdbc.core.JdbcTemplate;
import ru.yandex.bolts.collection.MapF;
import ru.yandex.inside.yt.kosher.cypress.CypressNodeType;
import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.inside.yt.kosher.ytree.YTreeNode;
import ru.yandex.market.mbo.synchronizer.export.modelstorage.CategoryModelsExtractor.Output;
import ru.yandex.market.mbo.synchronizer.export.modelstorage.yt.ModelsYtExportService;
import ru.yandex.market.mbo.synchronizer.export.tree.TovarTreeYtExportService;
import ru.yandex.market.mbo.yt.TestYtWrapper;

public class YtCategoryModelsSwitcherTest {
    private static final Long CATEGORY_ID = 90401L;

    private TestYtWrapper yt;
    private JdbcTemplate yqlTemplate;
    private YtCategoryModelsSwitcher switcher;
    private YPath basePath;
    private YPath recent;

    @Before
    public void setup() {
        basePath = YPath.simple("//base");

        yt = new TestYtWrapper();
        yqlTemplate = Mockito.mock(JdbcTemplate.class);
        switcher = new YtCategoryModelsSwitcher(yqlTemplate, yt.pool(), yt);

        recent = createTables("recent", "");
    }

    @Test
    public void testSimpleQuery() {
        YPath session = createTables("session", "");

        Answer<Void> queryWithoutFilter = invocation -> {
            String query = (String) invocation.getArguments()[0];
            Assert.assertFalse(query.contains(CATEGORY_ID.toString()));
            return null;
        };

        Mockito.doAnswer(queryWithoutFilter).when(yqlTemplate).execute(Mockito.anyString());
        switcher.switchFiles(recent, session);

        // assert current sessions are exist
        for (Output output : Output.values()) {
            YPath fullTableName = getFullTableName("session", output, "");
            Assert.assertTrue("Expected " + fullTableName + " to exist", yt.cypress().exists(fullTableName));
        }

        // assert temp tables are deleted
        for (Output output : Output.values()) {
            YPath fullTableName = getFullTableName("session", output, "_temp");
            Assert.assertFalse("Expected " + fullTableName + " to be deleted", yt.cypress().exists(fullTableName));
        }
    }

    @Test
    public void testFilteredQuery() {
        YPath session = createTables("session", "");

        Answer<Void> queryWithFilter = invocation -> {
            String query = invocation.getArgument(0);
            Assert.assertTrue(query.contains(CATEGORY_ID.toString()));
            return null;
        };

        Mockito.doAnswer(queryWithFilter).when(yqlTemplate).execute(Mockito.anyString());

        switcher.addFailedCategory(CATEGORY_ID);
        switcher.switchFiles(recent, session);

        // assert current sessions are exist
        for (Output output : Output.values()) {
            YPath fullTableName = getFullTableName("session", output, "");
            Assert.assertTrue("Expected " + fullTableName + " to exist", yt.cypress().exists(fullTableName));
        }

        // assert temp tables are deleted
        for (Output output : Output.values()) {
            YPath fullTableName = getFullTableName("session", output, "_temp");
            Assert.assertFalse("Expected " + fullTableName + " to be deleted", yt.cypress().exists(fullTableName));
        }
    }

    private YPath getFullTableName(String sessionFolderName, Output output, String tableSuffix) {
        return basePath.child(sessionFolderName).child("models").child(output.getName() + tableSuffix);
    }

    private YPath createTables(String sessionFolderName, String tableSuffix) {
        YPath fullSessionPath = basePath.child(sessionFolderName);
        for (Output output : Output.values()) {
            createTable(fullSessionPath, tableSuffix, output);
        }
        return fullSessionPath;
    }

    private void createTable(YPath fullSessionPath, String tableSuffix, Output output) {
        MapF<String, YTreeNode> map = output.equals(Output.PARAMETERS) ?
                TovarTreeYtExportService.tableAttrs() : ModelsYtExportService.tableAttrs();

        yt.cypress().create(fullSessionPath.child("models").child(output.getName() + tableSuffix),
            CypressNodeType.TABLE, true, false, map);
    }
}
