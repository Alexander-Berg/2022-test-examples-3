package ru.yandex.crypta.graph2.matching.human.workflow.component.ops.indevice;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.Test;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.Option;
import ru.yandex.crypta.graph.soup.config.Soup;
import ru.yandex.crypta.graph.soup.config.proto.ELogSourceType;
import ru.yandex.crypta.graph.soup.config.proto.ESourceType;
import ru.yandex.crypta.graph.soup.config.proto.TEdgeProps;
import ru.yandex.crypta.graph.soup.config.proto.TEdgeRecord;
import ru.yandex.crypta.graph2.matching.human.workflow.component.ops.gen.SingleLineComponentGenerator;
import ru.yandex.crypta.graph2.model.matching.component.Component;
import ru.yandex.crypta.graph2.model.matching.component.GraphInfo;
import ru.yandex.crypta.graph2.model.soup.edge.Edge;
import ru.yandex.crypta.graph2.model.soup.edge.weight.EdgeInfoProvider;
import ru.yandex.crypta.graph2.model.soup.props.DeviceId;
import ru.yandex.crypta.graph2.model.soup.props.VertexPropertiesCollector;
import ru.yandex.crypta.graph2.model.soup.props.Yandexuid;
import ru.yandex.crypta.graph2.model.soup.vertex.Vertex;
import ru.yandex.crypta.lib.proto.identifiers.EIdType;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class MarkOutIndeviceEdgesTest {

    private static final String FAKE_CC = "fake";
    private static final String SAME_UA_PROFILE = "m|x|x|x|x";

    private static final String SOME_INDEV_SOURCE_TYPE_NAME = Soup.CONFIG.name(ESourceType.APP_URL_REDIR);
    private static final ESourceType SOME_INDEV_SOURCE_TYPE = ESourceType.APP_URL_REDIR;

    private static final TEdgeRecord INDEV_PROPS = TEdgeRecord.newBuilder()
            .setProps(TEdgeProps.newBuilder()
                    .setDeviceBounds(TEdgeProps.EDeviceBounds.INDEVICE)
            ).build();

    private static final TEdgeRecord USUAL_PROPS = TEdgeRecord.newBuilder().build();

    private static final EdgeInfoProvider EDGE_TYPE_PROVIDER = new EdgeInfoProvider() {
        @Override
        public double getEdgeWeight(Edge edge) {
            return 1.0;
        }

        @Override
        public double getEdgeWeight(ru.yandex.crypta.graph2.model.soup.proto.Edge edge) {
            return 1.0;
        }

        @Override
        public TEdgeRecord getEdgeTypeConfig(Edge edge) {
            if (edge.calculateEdgeType().getSourceType() == SOME_INDEV_SOURCE_TYPE) {
                return INDEV_PROPS;
            } else {
                return USUAL_PROPS;
            }
        }

        @Override
        public TEdgeRecord getEdgeTypeConfig(ru.yandex.crypta.graph2.model.soup.proto.Edge edge) {
            if (edge.getSourceType().equals(SOME_INDEV_SOURCE_TYPE_NAME)) {
                return INDEV_PROPS;
            } else {
                return USUAL_PROPS;
            }
        }
    };

    private static final EdgeInfoProvider ALL_INDEVICE_EDGE_TYPE_PROVIDER = new EdgeInfoProvider() {
        @Override
        public double getEdgeWeight(Edge edge) {
            return 1.0;
        }

        @Override
        public double getEdgeWeight(ru.yandex.crypta.graph2.model.soup.proto.Edge edge) {
            return 1.0;
        }

        @Override
        public TEdgeRecord getEdgeTypeConfig(Edge edge) {
            return INDEV_PROPS;
        }

        @Override
        public TEdgeRecord getEdgeTypeConfig(ru.yandex.crypta.graph2.model.soup.proto.Edge edge) {
            return INDEV_PROPS;
        }
    };

    @Test
    public void onlyProperVerticesMakeIndevice() {
        Yandexuid yandexuid = new Yandexuid(new Vertex("y1", EIdType.YANDEXUID), FAKE_CC, "", Option.empty(), "",
                false);
        DeviceId deviceId = new DeviceId(new Vertex("d1", EIdType.GAID), FAKE_CC, "", "", "", Option.empty(), "",
                false);

        VertexPropertiesCollector vpsNoActivity = new VertexPropertiesCollector();
        singleEdgeIndevice(vpsNoActivity, false);

        VertexPropertiesCollector vpsHasActivity = new VertexPropertiesCollector();
        vpsHasActivity.addYandexuid(yandexuid);
        vpsHasActivity.addDeviceId(deviceId);
        singleEdgeIndevice(vpsHasActivity, false);

        yandexuid.setUaProfile("m|x|x|x|x");
        deviceId.setUaProfile("m|x|x|x|x");
        singleEdgeIndevice(vpsHasActivity, true);

    }

    private void singleEdgeIndevice(VertexPropertiesCollector vps, boolean isIndevice) {
        GraphInfo graphInfo = new GraphInfo(Cf.map(), Cf.map(), vps, Cf.list());

        Component component = new Component();
        component.addInnerEdge(new Edge(
                "y1", EIdType.YANDEXUID,
                "d1", EIdType.GAID,
                ESourceType.APP_URL_REDIR, ELogSourceType.ACCESS_LOG, Cf.list()
        ));

        MarkOutIndeviceEdges marker = new MarkOutIndeviceEdges(graphInfo, component, EDGE_TYPE_PROVIDER);
        List<IndeviceLink> indeviceLinks = marker.markOutIndeviceEdges().collect(Collectors.toList());

        assertEquals(isIndevice, !indeviceLinks.isEmpty());
        assertTrue(indeviceLinks.stream().allMatch(l ->
                l.getMatchType().equals(IndeviceLink.INDEVICE_MATCH_TYPE)
        ));
    }

    @Test
    public void inDeviceVsHeuristicCrossDeviceMatch() {
        // edge type is defined by EDGE_TYPE_PROVIDER
        Edge inDeviceEdge = new Edge(
                "e1", EIdType.EMAIL,
                "e2", EIdType.EMAIL,
                ESourceType.APP_URL_REDIR, ELogSourceType.ACCESS_LOG, Cf.list()
        );

        // edge type is defined by EDGE_TYPE_PROVIDER
        Edge crossDeviceEdge = new Edge(
                "e1", EIdType.EMAIL,
                "e2", EIdType.EMAIL,
                ESourceType.ACCOUNT_MANAGER, ELogSourceType.ACCESS_LOG, Cf.list()
        );

        crossDevicePathMarkedByHeuristicIndeviceInCaseOfUaMatch(inDeviceEdge, IndeviceLink.INDEVICE_MATCH_TYPE);
        crossDevicePathMarkedByHeuristicIndeviceInCaseOfUaMatch(crossDeviceEdge, IndeviceLink.HEURISTIC_MATCH_TYPE);
    }

    private void crossDevicePathMarkedByHeuristicIndeviceInCaseOfUaMatch(Edge centralEdge, String expectedMatchType) {
        Yandexuid yandexuid = new Yandexuid(
                new Vertex("y1", EIdType.YANDEXUID),
                FAKE_CC, SAME_UA_PROFILE, Option.empty(), "", false
        );
        DeviceId deviceId = new DeviceId(
                new Vertex("d1", EIdType.GAID),
                FAKE_CC, SAME_UA_PROFILE, "", "", Option.empty(), "", false
        );
        VertexPropertiesCollector vps = new VertexPropertiesCollector();
        vps.addYandexuid(yandexuid);
        vps.addDeviceId(deviceId);

        GraphInfo graphInfo = new GraphInfo(Cf.map(), Cf.map(), vps, Cf.list());

        Component component = new Component();
        component.addInnerEdge(new Edge(
                "y1", EIdType.YANDEXUID,
                "e1", EIdType.EMAIL,
                ESourceType.APP_URL_REDIR, ELogSourceType.ACCESS_LOG, Cf.list()
        ));
        // cross device
        component.addInnerEdge(centralEdge);
        component.addInnerEdge(new Edge(
                "e2", EIdType.EMAIL,
                "d1", EIdType.GAID,
                ESourceType.APP_URL_REDIR, ELogSourceType.ACCESS_LOG, Cf.list()
        ));

        MarkOutIndeviceEdges marker = new MarkOutIndeviceEdges(graphInfo, component, EDGE_TYPE_PROVIDER);
        List<IndeviceLink> indeviceLinks = marker.markOutIndeviceEdges().collect(Collectors.toList());

        assertEquals(1, indeviceLinks.size());
        assertTrue(indeviceLinks.stream().allMatch(l ->
                l.getMatchType().equals(expectedMatchType)
        ));
    }

    @Test
    public void testBFSDepthLimit() {
        SingleLineComponentGenerator gen = new SingleLineComponentGenerator(500);
        SingleLineComponentGenerator.SingleLineComponent testComponent = gen.generateSingleLineComponent();

        Vertex vertexFromChainStart = testComponent.getVerticesLine().first();
        Vertex vertexFromChainMiddle = testComponent.getVerticesLine().get(300);

        Component overlimitComponent = testComponent.getComponent().asComponent();

        GraphInfo graphInfo = new GraphInfo();
        graphInfo.components.put(overlimitComponent.getCryptaId(), overlimitComponent);

        // set the same user agent to all vertices
        for (Vertex vertex : overlimitComponent.getVertices()) {
            graphInfo.verticesProperties.addYandexuid(
                    new Yandexuid(vertex, FAKE_CC, SAME_UA_PROFILE, Option.empty(), "", true)
            );
        }

        testIndeviceMarkStartingFrom(vertexFromChainStart, overlimitComponent.copy(), graphInfo,
                MarkOutIndeviceEdges.MAX_IN_DEVICE_DEPTH);
        // vertexFromChainMiddle is at the path from device_id twice, thus total count is "-1"
        testIndeviceMarkStartingFrom(vertexFromChainMiddle, overlimitComponent.copy(), graphInfo,
                MarkOutIndeviceEdges.MAX_IN_DEVICE_DEPTH * 2 - 1);
    }

    private void testIndeviceMarkStartingFrom(Vertex vertexFromChainStart, Component overlimitComponent,
                                              GraphInfo graphInfo, int expectedLinksCount) {

        // connect device_id to the specified part of the chain
        Vertex deviceId = new Vertex(vertexFromChainStart.getId(), EIdType.IDFA);
        graphInfo.verticesProperties.addDeviceId(
                new DeviceId(deviceId, FAKE_CC, SAME_UA_PROFILE, "", "", Option.empty(), "", true)
        );

        Edge connectingEdge = new Edge(
                deviceId.getId(), deviceId.getIdType(),
                vertexFromChainStart.getId(), vertexFromChainStart.getIdType(),
                ESourceType.APP_METRICA, ELogSourceType.ACCESS_LOG,
                Cf.list()
        );

        overlimitComponent.addInnerEdge(connectingEdge);

        // test
        MarkOutIndeviceEdges marker =
                new MarkOutIndeviceEdges(graphInfo, overlimitComponent, ALL_INDEVICE_EDGE_TYPE_PROVIDER);
        List<IndeviceLink> indeviceLinks = marker.markOutIndeviceEdges().collect(Collectors.toList());

        assertEquals(expectedLinksCount, indeviceLinks.size());
    }
}
