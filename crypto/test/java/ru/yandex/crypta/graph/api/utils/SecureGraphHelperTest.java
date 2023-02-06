package ru.yandex.crypta.graph.api.utils;

import java.util.List;

import org.apache.commons.codec.digest.DigestUtils;
import org.junit.Test;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.crypta.graph.api.model.graph.Edge;
import ru.yandex.crypta.graph.api.model.graph.Graph;
import ru.yandex.crypta.graph.api.model.graph.GraphComponent;
import ru.yandex.crypta.graph.api.model.graph.Vertex;

import static java.util.Collections.emptyList;
import static org.junit.Assert.assertEquals;
import static ru.yandex.crypta.graph.api.utils.SecureGraphHelper.MASKED_TYPE;

public class SecureGraphHelperTest {

    @Test
    public void createMaskedGraph() {
        SecureGraphHelper secureGraphHelper = new SecureGraphHelper();

        Vertex yandexuid = new Vertex("12345678909876543210", "yandexuid");
        Vertex uuid = new Vertex("12344321567890098765", "uuid");
        Vertex phone = new Vertex("+7123456789", "phone");
        Vertex dit = new Vertex("102938457", "dit_id");
        List<Vertex> vertices = Cf.arrayList(yandexuid, uuid, phone, dit);

        Edge yuidUuid = new Edge(yandexuid, uuid, "type1", "log1", 0.0, emptyList());
        Edge uuidPhone = new Edge(uuid, phone, "type2", "log2", 0.0, emptyList());
        Edge phoneDit = new Edge(phone, dit, "type3", "log3", 0.0, emptyList());
        Edge ditYuid = new Edge(dit, yandexuid, "type4", "log4", 0.0, emptyList());
        List<Edge> edges = Cf.arrayList(yuidUuid, phoneDit, uuidPhone, ditYuid);

        List<GraphComponent> components = Cf.arrayList(new GraphComponent("12283456789", vertices, edges));
        Graph graph = new Graph(components);

        Graph maskedGraph = secureGraphHelper.createMaskedGraph(graph);

        Vertex maskedPhone = new Vertex(DigestUtils.md5Hex(DigestUtils.md5Hex("+7123456789")), "phone");
        Vertex maskedDit = new Vertex(DigestUtils.md5Hex(DigestUtils.md5Hex("102938457")), "external_partner");
        List<Vertex> maskedVertices = Cf.arrayList(yandexuid, uuid, maskedPhone, maskedDit);

        Edge maskedUuidPhone =
                new Edge(uuid, maskedPhone, MASKED_TYPE, MASKED_TYPE, 0.0,
                        emptyList());
        Edge maskedPhoneDit =
                new Edge(maskedPhone, maskedDit, MASKED_TYPE, MASKED_TYPE, 0.0,
                        emptyList());
        Edge maskedDitYuid =
                new Edge(maskedDit, yandexuid, MASKED_TYPE, MASKED_TYPE, 0.0,
                        emptyList());
        List<Edge> maskedEdges = Cf.arrayList(yuidUuid, maskedPhoneDit, maskedUuidPhone, maskedDitYuid);

        List<GraphComponent> maskedComponents =
                Cf.arrayList(new GraphComponent("12283456789", maskedVertices, maskedEdges));
        Graph trueMaskedGraph = new Graph(maskedComponents);

        assertEquals(maskedGraph, trueMaskedGraph);
    }
}
