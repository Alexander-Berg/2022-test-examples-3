package ru.yandex.crypta.graph2.dao.yt.schema;

import org.junit.Test;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.crypta.graph2.dao.yt.schema.extractor.BenderYtSchemaExtractor;
import ru.yandex.crypta.graph2.dao.yt.schema.extractor.YTreeYtSchemaExtractor;
import ru.yandex.crypta.graph2.dao.yt.schema.extractor.YtSchemaExtractor;
import ru.yandex.crypta.graph2.model.matching.component.ComponentNeighbours;
import ru.yandex.crypta.graph2.model.matching.component.ComponentStats;
import ru.yandex.crypta.graph2.model.matching.edge.EdgeBetweenComponents;
import ru.yandex.crypta.graph2.model.matching.edge.EdgeBetweenWithNewCryptaIds;
import ru.yandex.crypta.graph2.model.matching.edge.EdgeInComponent;
import ru.yandex.crypta.graph2.model.matching.merge.MergeKeyWithNewCryptaIds;
import ru.yandex.crypta.graph2.model.matching.merge.MergeNeighbour;
import ru.yandex.crypta.graph2.model.matching.merge.MergeOffer;
import ru.yandex.crypta.graph2.model.matching.vertex.VertexInComponent;
import ru.yandex.crypta.graph2.model.soup.edge.Edge;
import ru.yandex.crypta.graph2.model.soup.props.VertexExactSocdem;
import ru.yandex.crypta.graph2.model.soup.vertex.Vertex;
import ru.yandex.inside.yt.kosher.ytree.YTreeNode;
import ru.yandex.yt.ytclient.tables.TableSchema;

public class ComponentYtSchemaExtractorTest {

    private static final ListF<Class<?>> ENTITIES = Cf.list(Vertex.class, Edge.class,
            VertexInComponent.class, EdgeInComponent.class,
            EdgeBetweenComponents.class, EdgeBetweenWithNewCryptaIds.class,
            MergeOffer.class, MergeNeighbour.class, MergeKeyWithNewCryptaIds.class,
            ComponentStats.class, ComponentNeighbours.class,
            VertexExactSocdem.class);

    @Test
    public void testBenderYtSchemaGeneratorAllEntities() throws Exception {
        YtSchemaExtractor gen = new BenderYtSchemaExtractor();

        for (Class<?> entity : ENTITIES) {
            TableSchema schema = gen.extractTableSchema(entity);
            YTreeNode yTreeNode = schema.toYTree();
            System.out.println("Class " + entity.getName() + ": " + yTreeNode);
        }
    }

    @Test
    public void testYsonYtSchemaGeneratorAllEntities() throws Exception {
        YtSchemaExtractor gen = new YTreeYtSchemaExtractor();

        for (Class<?> entity : ENTITIES) {
            TableSchema schema = gen.extractTableSchema(entity);
            YTreeNode yTreeNode = schema.toYTree();
            System.out.println("Class " + entity.getName() + ": " + yTreeNode);
        }
    }

}
