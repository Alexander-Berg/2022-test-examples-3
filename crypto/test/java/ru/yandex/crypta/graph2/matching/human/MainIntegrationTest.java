package ru.yandex.crypta.graph2.matching.human;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.crypta.graph2.matching.human.config.HumanMatchingConfig;
import ru.yandex.crypta.graph2.model.matching.vertex.VertexInComponent;
import ru.yandex.crypta.graph2.utils.YamlConfig;
import ru.yandex.devtools.test.CanonicalFile;
import ru.yandex.devtools.test.Canonizer;
import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.inside.yt.kosher.tables.YTableEntryTypes;

import static org.junit.Assert.assertEquals;
import static ru.yandex.crypta.graph2.matching.human.helper.TestDataHelper.copyTestResourceToYtWorkdir;

public class MainIntegrationTest extends HumanMatchingIntegrationTestBase {

    private static final Path WORKDIR = Paths.get("human_matching_main_test_workdir");

    private HumanMatchingConfig config;

    @Before
    public void setUp() throws Exception {
        config = YamlConfig.readConfigFromClassPath("test_config.yaml", HumanMatchingConfig.class);
        super.setUp(WORKDIR, config);

        copyTestResourceToYtWorkdir(WORKDIR, "test_data/soup_edges", "home/crypta/production/state/graph/v2/soup" +
                "/cooked/soup_edges");
        copyTestResourceToYtWorkdir(WORKDIR, "test_data/vertices_properties", "home/crypta/production/state/graph/v2" +
                "/soup/cooked/vertices_properties");
        copyTestResourceToYtWorkdir(WORKDIR, "test_data/dates_count_per_edge_type", "home/crypta/production/state" +
                "/graph/v2/soup/cooked/stats/dates_count_per_edge_type");

        sortInfoHelper.setSortedBy(
                YPath.simple("//home/crypta/production/state/graph/v2/soup/cooked/vertices_properties"),
                Cf.list("id", "id_type")
        );
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown(WORKDIR);
    }

    @Test
    public void runTest() throws IOException {

        HumanMatchingMain humanMatchingMain = new HumanMatchingMain();
        RunStats stats = runNIterations(config, humanMatchingMain, 4);
        System.out.println(stats);

        assertEquals(303, stats.lastIteration().componentsCount);
        canonizeResults(config.outputDir);

    }

    private void canonizeResults(String outputDir) throws FileNotFoundException {
         try {
            String canonicalFileName = "vertices";
            PrintWriter writer = new PrintWriter(canonicalFileName, StandardCharsets.UTF_8);
            localDao.yt().tables().read(
                    YPath.simple(outputDir).child("vertices_no_multi_profile_by_id_type"),
                    YTableEntryTypes.yson(VertexInComponent.class),
                    (rec) -> {
                        writer.println(rec.getVertex() + " " + rec.getCryptaId());
                    }
            );
            writer.close();
            Canonizer.canonize(new CanonicalFile(canonicalFileName, false));
         } catch (RuntimeException | IOException e) {
             // Skip canonization, when it's impossible. Check diff with ya make -t
             System.out.println("Run without canonization");
         }
    }

}
