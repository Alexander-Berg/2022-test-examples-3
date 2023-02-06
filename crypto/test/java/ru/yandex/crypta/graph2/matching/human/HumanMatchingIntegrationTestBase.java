package ru.yandex.crypta.graph2.matching.human;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.function.Function;
import ru.yandex.crypta.graph2.dao.Dao;
import ru.yandex.crypta.graph2.dao.yt.local.fastyt.LocalYtFactory;
import ru.yandex.crypta.graph2.dao.yt.local.fastyt.client.YtTablesLocalImpl;
import ru.yandex.crypta.graph2.dao.yt.local.fastyt.fs.FileBasedLocalYtDataLayer;
import ru.yandex.crypta.graph2.dao.yt.local.fastyt.fs.LocalYtDataLayer;
import ru.yandex.crypta.graph2.dao.yt.local.fastyt.testdata.SortInfoHelper;
import ru.yandex.crypta.graph2.matching.human.config.HumanMatchingConfig;
import ru.yandex.inside.yt.kosher.cypress.CypressNodeType;
import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.inside.yt.kosher.tables.YTableEntryTypes;
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;
import ru.yandex.misc.io.file.File2;
import ru.yandex.misc.lang.DefaultToString;

abstract class HumanMatchingIntegrationTestBase {

    protected Dao localDao;
    protected SortInfoHelper sortInfoHelper;

    protected Dao setUpLocalYt(Path workdir) throws IOException {
        File2.wrap(workdir.toFile()).deleteRecursiveQuietly();
        Files.createDirectories(workdir);

        LocalYtDataLayer tablesDataLayer = new FileBasedLocalYtDataLayer(workdir);
        return LocalYtFactory.createLocalDao(tablesDataLayer, workdir);
    }

    protected void prepareMinRequiredTestData(HumanMatchingConfig config) {

        localDao.yt().cypress().create(
                YPath.simple(config.soupEdgesTable),
                CypressNodeType.TABLE,
                true
        );

        localDao.yt().cypress().create(
                YPath.simple(config.soupVerticesPropertiesTable),
                CypressNodeType.TABLE
        );

        sortInfoHelper.setSortedBy(
                YPath.simple(config.soupVerticesPropertiesTable),
                Cf.list("id", "id_type")
        );
    }

    public void setUp(Path workdir, HumanMatchingConfig config) throws Exception {
        localDao = setUpLocalYt(workdir);
        sortInfoHelper = new SortInfoHelper(((YtTablesLocalImpl) localDao.yt().tables()).getDataLayer());
        prepareMinRequiredTestData(config);
    }

    public void tearDown(Path workdir) throws Exception {
        File2.wrap(workdir.toFile()).deleteRecursiveQuietly();
    }

    protected RunStats runNIterations(HumanMatchingConfig config, HumanMatchingMain humanMatchingMain, int iterations) {

        YPath outputDir = YPath.simple(config.outputDir);

        RunStats runStats = new RunStats();
        for (int iter = 0; iter < iterations; iter++) {
            humanMatchingMain.run(localDao, config, false, iter);

            IterationStats countStats = new IterationStats();
            countStats.iterN = iter;
            countStats.componentsCount = countTableRecs(outputDir.child("crypta_components_stats"));
            countStats.verticesCount = countTableRecs(outputDir.child("vertices_no_multi_profile"));
            countStats.edgesCount = countTableRecs(outputDir.child("edges_by_crypta_id"));
            countStats.edgesBetweenCount = countTableRecs(outputDir.child("edges_between_components"));
            runStats.iterations.add(countStats);

        }

        return runStats;

    }

    protected int countTableRecs(YPath table) {
        return localDao.yt().tables().read(table, YTableEntryTypes.YSON,
                (Function<Iterator<YTreeMapNode>, Integer>) i -> { return Cf.x(i).count(); }
        );
    }

    protected static class RunStats {
        List<IterationStats> iterations = new ArrayList<>();

        @Override
        public String toString() {
            String iterationsStr = iterations
                    .stream()
                    .map(Object::toString)
                    .collect(Collectors.joining("\n"));
            return "Components merging progress\n" + iterationsStr;
        }

        IterationStats lastIteration() {
            return iterations.get(iterations.size() - 1);
        }
    }

    protected static class IterationStats extends DefaultToString {
        int iterN;
        int componentsCount;
        int verticesCount;
        int edgesCount;
        int edgesBetweenCount;
    }
}
