package ru.yandex.crypta.graph2.matching.human;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.crypta.graph2.matching.human.config.HumanMatchingConfig;
import ru.yandex.crypta.graph2.utils.YamlConfig;

import static org.junit.Assert.assertEquals;

public class EmptyIntegrationTest extends HumanMatchingIntegrationTestBase {

    private static final Path WORKDIR = Paths.get("empty");
    private HumanMatchingConfig config;

    @Before
    public void setUp() throws Exception {
        config = YamlConfig.readConfigFromClassPath("test_config.yaml", HumanMatchingConfig.class);
        super.setUp(WORKDIR, config);
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown(WORKDIR);
    }

    @Test
    public void runTest() {
        HumanMatchingMain humanMatchingMain = new HumanMatchingMain();
        RunStats stats = runNIterations(config, humanMatchingMain, 2);
        System.out.println(stats);

        assertEquals(0, stats.lastIteration().verticesCount);

    }


}
