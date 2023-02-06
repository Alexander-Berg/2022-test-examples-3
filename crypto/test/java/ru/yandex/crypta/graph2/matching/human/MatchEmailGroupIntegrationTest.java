package ru.yandex.crypta.graph2.matching.human;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.crypta.graph.soup.config.proto.ELogSourceType;
import ru.yandex.crypta.graph.soup.config.proto.ESourceType;
import ru.yandex.crypta.graph2.matching.human.config.HumanMatchingConfig;
import ru.yandex.crypta.graph2.model.matching.vertex.VertexInComponent;
import ru.yandex.crypta.graph2.model.soup.edge.Edge;
import ru.yandex.crypta.graph2.utils.YamlConfig;
import ru.yandex.crypta.lib.proto.identifiers.EIdType;
import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.inside.yt.kosher.tables.YTableEntryTypes;

import static org.junit.Assert.assertEquals;

public class MatchEmailGroupIntegrationTest extends HumanMatchingIntegrationTestBase {

    private static final Path WORKDIR = Paths.get("match_email");
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

    private List<Edge> emailEdgesGroup() {
        ArrayList<Edge> edges = new ArrayList<>();
        // email and hashes
        edges.add(new Edge(
                "email", EIdType.EMAIL, "email_md5", EIdType.EMAIL_MD5,
                ESourceType.MD5_HASH, ELogSourceType.SOUP_PREPROCESSING, Cf.list()
        ));
        edges.add(new Edge(
                "email", EIdType.EMAIL, "email_sha", EIdType.EMAIL_SHA256,
                ESourceType.SHA256_HASH, ELogSourceType.SOUP_PREPROCESSING, Cf.list()
        ));
        edges.add(new Edge(
                "email", EIdType.EMAIL, "avito_hash", EIdType.AVITO_HASH,
                ESourceType.AVITO, ELogSourceType.SOUP_PREPROCESSING, Cf.list()
        ));


        // login of email
        edges.add(new Edge(
                "login", EIdType.LOGIN, "email", EIdType.EMAIL,
                ESourceType.LOGIN_TO_EMAIL, ELogSourceType.SOUP_PREPROCESSING, Cf.list()
        ));
        // login of email
        edges.add(new Edge(
                "123", EIdType.PUID, "login", EIdType.LOGIN,
                ESourceType.PASSPORT_PROFILE, ELogSourceType.PASSPORT_DICT, Cf.list()
        ));

        return edges;
    }

    @Test
    public void runTest() {

        List<Edge> edges = emailEdgesGroup();
        localDao.yt().tables().write(
                YPath.simple(config.soupEdgesTable),
                YTableEntryTypes.yson(Edge.class),
                Cf.wrap(edges.iterator())
        );

        HumanMatchingMain humanMatchingMain = new HumanMatchingMain();
        RunStats stats = runNIterations(config, humanMatchingMain, 2);
        System.out.println(stats);

        YPath outputDir = YPath.simple(config.outputDir);

        List<VertexInComponent> vertices = localDao.ytTables().readYsonEntities(
                outputDir.child("vertices_no_multi_profile"),
                VertexInComponent.class
        );
        assertEquals(6, vertices.size());
        Set<String> uniqueCryptaIds = vertices.stream().map(VertexInComponent::getCryptaId).collect(Collectors.toSet());
        assertEquals(1, uniqueCryptaIds.size());

    }


}
