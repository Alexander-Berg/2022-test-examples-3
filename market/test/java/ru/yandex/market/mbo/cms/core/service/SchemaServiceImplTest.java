package ru.yandex.market.mbo.cms.core.service;


import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.mbo.cms.core.dao.CmsServiceDao;
import ru.yandex.market.mbo.cms.core.dao.SchemaDao;
import ru.yandex.market.mbo.cms.core.log.MetricsLogger;
import ru.yandex.market.mbo.cms.core.models.CmsSchema;
import ru.yandex.market.mbo.cms.core.models.DocumentDescription;
import ru.yandex.market.mbo.cms.core.models.FieldType;
import ru.yandex.market.mbo.cms.core.models.Namespace;
import ru.yandex.market.mbo.cms.core.models.NodeType;
import ru.yandex.market.mbo.cms.core.models.SchemaDiffInfo;

public class SchemaServiceImplTest {
    public static final String NAMESPACE_1 = "n1";
    public static final String NAMESPACE_2 = "n2";
    public static final String NAMESPACE_1_SCHEMA_1_DOC_1 = "n1s1doc1";
    public static final String NAMESPACE_1_SCHEMA_1_DOC_2 = "n1s1doc2";
    public static final String NAMESPACE_1_SCHEMA_1_DOC_3 = "n1s1doc3";
    public static final String NAMESPACE_1_SCHEMA_2_DOC_1 = "n1s2doc1";
    public static final String NAMESPACE_1_SCHEMA_2_DOC_2 = "n1s2doc2";
    public static final String NAMESPACE_1_SCHEMA_2_DOC_3 = "n1s2doc3";
    public static final String NAMESPACE_2_SCHEMA_3_DOC_1 = "n2s3doc1";
    public static final String NAMESPACE_2_SCHEMA_3_DOC_2 = "n2s3doc2";
    public static final String NAMESPACE_2_SCHEMA_3_DOC_3 = "n2s3doc3";
    public static final String SCHEMA_1_NAME = "s1";
    public static final String SCHEMA_2_NAME = "s2";
    public static final String SCHEMA_3_NAME = "s3";

    SchemaDao schemaDao = Mockito.mock(SchemaDao.class);
    CmsServiceDao cmsServiceDao = Mockito.mock(CmsServiceDao.class);
    MetricsLogger metricsLogger = Mockito.mock(MetricsLogger.class);
    SchemaService schemaService = new SchemaServiceImpl(schemaDao, cmsServiceDao, metricsLogger);
    @Before
    public void init() {

        CmsSchema n1s1 = new CmsSchema(NAMESPACE_1);
        Map<String, DocumentDescription> n1s1docs = new HashMap<>();
        n1s1.setMaster(true);
        DocumentDescription n1s1doc1 = new DocumentDescription(NAMESPACE_1, NAMESPACE_1_SCHEMA_1_DOC_1);
        DocumentDescription n1s1doc2 = new DocumentDescription(NAMESPACE_1, NAMESPACE_1_SCHEMA_1_DOC_2);
        DocumentDescription n1s1doc3 = new DocumentDescription(NAMESPACE_1, NAMESPACE_1_SCHEMA_1_DOC_3);
        n1s1docs.put(NAMESPACE_1_SCHEMA_1_DOC_1, n1s1doc1);
        n1s1docs.put(NAMESPACE_1_SCHEMA_1_DOC_2, n1s1doc2);
        n1s1docs.put(NAMESPACE_1_SCHEMA_1_DOC_3, n1s1doc3);
        n1s1.setDocuments(n1s1docs);

        CmsSchema n1s2 = new CmsSchema(NAMESPACE_1);
        Map<String, DocumentDescription> n1s2docs = new HashMap<>();
        DocumentDescription n1s2doc1 = new DocumentDescription(NAMESPACE_1, NAMESPACE_1_SCHEMA_2_DOC_1);
        DocumentDescription n1s2doc2 = new DocumentDescription(NAMESPACE_1, NAMESPACE_1_SCHEMA_2_DOC_2);
        DocumentDescription n1s2doc3 = new DocumentDescription(NAMESPACE_1, NAMESPACE_1_SCHEMA_2_DOC_3);
        n1s2docs.put(NAMESPACE_1_SCHEMA_2_DOC_1, n1s2doc1);
        n1s2docs.put(NAMESPACE_1_SCHEMA_2_DOC_2, n1s2doc2);
        n1s2docs.put(NAMESPACE_1_SCHEMA_2_DOC_3, n1s2doc3);
        n1s2.setDocuments(n1s2docs);

        CmsSchema n2s1 = new CmsSchema(NAMESPACE_2);
        n2s1.setMaster(true);
        Map<String, DocumentDescription> n2s1docs = new HashMap<>();
        DocumentDescription n2s1doc1 = new DocumentDescription(NAMESPACE_2, NAMESPACE_2_SCHEMA_3_DOC_1);
        DocumentDescription n2s1doc2 = new DocumentDescription(NAMESPACE_2, NAMESPACE_2_SCHEMA_3_DOC_2);
        DocumentDescription n2s1doc3 = new DocumentDescription(NAMESPACE_2, NAMESPACE_2_SCHEMA_3_DOC_3);
        n2s1docs.put(NAMESPACE_2_SCHEMA_3_DOC_1, n2s1doc1);
        n2s1docs.put(NAMESPACE_2_SCHEMA_3_DOC_2, n2s1doc2);
        n2s1docs.put(NAMESPACE_2_SCHEMA_3_DOC_3, n2s1doc3);
        n2s1.setDocuments(n2s1docs);

        Mockito.when(schemaDao.getSchema(NAMESPACE_1, SCHEMA_1_NAME)).thenReturn(n1s1);
        Mockito.when(schemaDao.getSchema(NAMESPACE_1, SCHEMA_2_NAME)).thenReturn(n1s2);
        Mockito.when(schemaDao.getSchema(NAMESPACE_2, SCHEMA_3_NAME)).thenReturn(n2s1);

        Mockito.when(schemaDao.getNamespaces()).thenReturn(
            Arrays.asList(
                    new Namespace(NAMESPACE_1, "", "", "", false),
                    new Namespace(NAMESPACE_2, "", "", "", false)
            ));
        Mockito.when(schemaDao.getAllPublishedSchemas(NAMESPACE_1)).thenReturn(
            Arrays.asList(makeDocumentSchema(SCHEMA_1_NAME, n1s1doc1, true))
        );

        Mockito.when(schemaDao.getAllPublishedSchemas(NAMESPACE_2)).thenReturn(
            Arrays.asList(makeDocumentSchema(SCHEMA_3_NAME, n2s1doc1, true))
        );
    }

    private CmsSchema makeDocumentSchema(String schemaName,
                                         DocumentDescription documentDescription,
                                         boolean master) {
        CmsSchema result = new CmsSchema();
        result.setName(schemaName);
        result.setMaster(true);
        result.setDocTypes(Set.of(documentDescription.getType()));
        result.getDocuments().put(documentDescription.getType(), documentDescription);
        return result;
    }

    @Test
    public void getDocTypes() {
        Map<String, List<DocumentDescription>> result = schemaService.getDocTypes(NAMESPACE_1, "s1");
        Assert.assertEquals(1, result.size());
        Assert.assertEquals(3, result.get(NAMESPACE_1).size());
        Assert.assertTrue(result.get(NAMESPACE_1).stream()
            .anyMatch(o -> o.getType().equals(NAMESPACE_1_SCHEMA_1_DOC_1)));
        Assert.assertTrue(result.get(NAMESPACE_1).stream()
            .anyMatch(o -> o.getType().equals(NAMESPACE_1_SCHEMA_1_DOC_2)));
        Assert.assertTrue(result.get(NAMESPACE_1).stream()
            .anyMatch(o -> o.getType().equals(NAMESPACE_1_SCHEMA_1_DOC_3)));
    }

    @Test
    public void getAllDocTypes() {
        Map<String, List<DocumentDescription>> result = schemaService.getDocTypes(null, null);
        Assert.assertEquals(2, result.size());
        Assert.assertEquals(1, result.get(NAMESPACE_1).size());
        Assert.assertEquals(1, result.get(NAMESPACE_2).size());
        Assert.assertTrue(result.get(NAMESPACE_1).stream()
            .anyMatch(o -> o.getType().equals(NAMESPACE_1_SCHEMA_1_DOC_1)));
        Assert.assertTrue(result.get(NAMESPACE_2).stream()
            .anyMatch(o -> o.getType().equals(NAMESPACE_2_SCHEMA_3_DOC_1)));
    }

    @Test
    public void getSchemaDiffInfoTest() {
        CmsSchema oldSchema = new CmsSchema("1");
        CmsSchema newSchema = new CmsSchema("1");

        Map<String, NodeType> oldSchemaNodeTypes = new HashMap<>();
        Map<String, NodeType> newSchemaNodeTypes = new HashMap<>();

        NodeType notAffectedNodeType = new NodeType("no_changes");
        notAffectedNodeType.setLabel("no_changes");
        notAffectedNodeType.addField("field", new FieldType("no_changes"));

        NodeType changedNodeTypeBefore = new NodeType("changed");
        changedNodeTypeBefore.setLabel("changed");
        changedNodeTypeBefore.addField("field", new FieldType("before"));

        NodeType changedNodeTypeAfter = new NodeType("changed");
        changedNodeTypeAfter.setLabel("changed");
        changedNodeTypeAfter.addField("field", new FieldType("after"));

        NodeType removedNodeType = new NodeType("removed");
        removedNodeType.setLabel("removed");
        removedNodeType.addField("field", new FieldType("removed"));

        NodeType addedNodeType = new NodeType("added");
        addedNodeType.setLabel("added");
        addedNodeType.addField("field", new FieldType("added"));

        oldSchemaNodeTypes.put(notAffectedNodeType.getName(), notAffectedNodeType);
        oldSchemaNodeTypes.put(changedNodeTypeBefore.getName(), changedNodeTypeBefore);
        oldSchemaNodeTypes.put(removedNodeType.getName(), removedNodeType);

        newSchemaNodeTypes.put(notAffectedNodeType.getName(), new NodeType(notAffectedNodeType));
        newSchemaNodeTypes.put(changedNodeTypeAfter.getName(), changedNodeTypeAfter);
        newSchemaNodeTypes.put(addedNodeType.getName(), addedNodeType);

        oldSchema.setNodeTypes(oldSchemaNodeTypes);
        newSchema.setNodeTypes(newSchemaNodeTypes);

        SchemaDiffInfo diffInfo = schemaService.getSchemaDiffInfo(oldSchema, newSchema);

        Assert.assertEquals(1, diffInfo.getAddedNodeTypes().size());
        Assert.assertEquals(1, diffInfo.getChangedNodeTypes().size());
        Assert.assertEquals(1, diffInfo.getRemovedNodeTypes().size());

        Assert.assertTrue(diffInfo.getAddedNodeTypes().contains("added"));
        Assert.assertTrue(diffInfo.getChangedNodeTypes().contains("changed"));
        Assert.assertTrue(diffInfo.getRemovedNodeTypes().contains("removed"));
    }
}
