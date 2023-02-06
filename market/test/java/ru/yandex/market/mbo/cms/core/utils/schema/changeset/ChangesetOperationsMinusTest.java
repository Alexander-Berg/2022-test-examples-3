package ru.yandex.market.mbo.cms.core.utils.schema.changeset;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.market.mbo.cms.core.models.Changeset;
import ru.yandex.market.mbo.cms.core.models.ChangesetInfo;
import ru.yandex.market.mbo.cms.core.models.DocumentDescription;
import ru.yandex.market.mbo.cms.core.models.NodeType;

public class ChangesetOperationsMinusTest {

    @Test(expected = IllegalArgumentException.class)
    public void testNullMinusNull() {
        ChangesetOperations.minus(null, null, new ChangesetInfo());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNullMinusEmpty() {
        Changeset changeset = ChangesetOperations.minus(null, new Changeset(), new ChangesetInfo());
        Assert.assertNotNull(changeset);
        assertChangesetIsEmpty(changeset);
    }

    @Test
    public void testEmptyMinusNull() {
        Changeset changeset = ChangesetOperations.minus(new Changeset(), null, new ChangesetInfo());
        Assert.assertNotNull(changeset);
        assertChangesetIsEmpty(changeset);
    }

    @Test
    public void testEmptyMinusEmpty() {
        Changeset changeset = ChangesetOperations.minus(new Changeset(), new Changeset(), new ChangesetInfo());
        Assert.assertNotNull(changeset);
        assertChangesetIsEmpty(changeset);
    }

    @Test
    public void testEmptyMinusEmptyDocument() {
        Changeset changeset = ChangesetOperations.minus(
                new Changeset(), changesetWithEmptyDocument("doc"), new ChangesetInfo()
        );
        Assert.assertEquals(0, changeset.getDocuments().size());
    }

    @Test
    public void testEmptyMinusNotEmptyDocument() {
        Changeset changeset = ChangesetOperations.minus(
                new Changeset(), changesetWithNotEmptyDocument("doc"), new ChangesetInfo()
        );
        Assert.assertEquals(0, changeset.getDocuments().size());
    }

    @Test
    public void testEmptyDocumentMinusEmpty() {
        Changeset changeset = ChangesetOperations.minus(
                changesetWithEmptyDocument("doc"), new Changeset(), new ChangesetInfo()
        );
        Assert.assertEquals(1, changeset.getDocuments().size());
    }

    @Test
    public void testNotEmptyDocumentMinusEmpty() {
        Changeset changeset = ChangesetOperations.minus(
                changesetWithEmptyDocument("doc"), new Changeset(), new ChangesetInfo()
        );
        Assert.assertEquals(1, changeset.getDocuments().size());
    }

    @Test
    public void testEmptyDocumentMinusSameEmptyDocument() {
        Changeset changeset = ChangesetOperations.minus(
                changesetWithEmptyDocument("doc"), changesetWithEmptyDocument("doc"), new ChangesetInfo()
        );
        Assert.assertEquals(0, changeset.getDocuments().size());
    }

    @Test
    public void testNotEmptyDocumentMinusSameNotEmptyDocument() {
        Changeset changeset = ChangesetOperations.minus(
                changesetWithNotEmptyDocument("doc"),
                changesetWithNotEmptyDocument("doc"),
                new ChangesetInfo()
        );
        Assert.assertEquals(1, changeset.getDocuments().size());
    }

    @Test
    public void testEmptyDocumentMinusAnotherEmptyDocument() {
        Changeset changeset = ChangesetOperations.minus(
                changesetWithEmptyDocument("doc1"), changesetWithEmptyDocument("doc2"), new ChangesetInfo()
        );
        Assert.assertEquals(1, changeset.getDocuments().size());
    }

    @Test
    public void testNotEmptyDocumentMinusAnotherNotEmptyDocument() {
        Changeset changeset = ChangesetOperations.minus(
                changesetWithNotEmptyDocument("doc1"),
                changesetWithNotEmptyDocument("doc2"),
                new ChangesetInfo()
        );
        Assert.assertEquals(1, changeset.getDocuments().size());
    }

    @Test
    public void testEmptyMinusEmptyNodeType() {
        Changeset changeset = ChangesetOperations.minus(
                new Changeset(), changesetWithEmptyNodeType("doc"), new ChangesetInfo()
        );
        Assert.assertEquals(0, changeset.getNodeTypes().size());
    }

    @Test
    public void testEmptyMinusNotEmptyNodeType() {
        Changeset changeset = ChangesetOperations.minus(
                new Changeset(), changesetWithNotEmptyNodeType("doc"), new ChangesetInfo()
        );
        Assert.assertEquals(0, changeset.getNodeTypes().size());
    }

    @Test
    public void testEmptyNodeTypeMinusEmpty() {
        Changeset changeset = ChangesetOperations.minus(
                changesetWithEmptyNodeType("doc"), new Changeset(), new ChangesetInfo()
        );
        Assert.assertEquals(1, changeset.getNodeTypes().size());
    }

    @Test
    public void testNotEmptyNodeTypeMinusEmpty() {
        Changeset changeset = ChangesetOperations.minus(
                changesetWithNotEmptyNodeType("doc"), new Changeset(), new ChangesetInfo()
        );
        Assert.assertEquals(1, changeset.getNodeTypes().size());
    }

    @Test
    public void testEmptyNodeTypeMinusSameEmptyNodeType() {
        Changeset changeset = ChangesetOperations.minus(
                changesetWithEmptyNodeType("doc"), changesetWithEmptyNodeType("doc"), new ChangesetInfo()
        );
        Assert.assertEquals(0, changeset.getNodeTypes().size());
    }

    @Test
    public void testNotEmptyNodeTypeMinusSameNotEmptyNodeType() {
        Changeset changeset = ChangesetOperations.minus(
                changesetWithNotEmptyNodeType("doc"),
                changesetWithNotEmptyNodeType("doc"),
                new ChangesetInfo()
        );
        Assert.assertEquals(1, changeset.getNodeTypes().size());
    }

    @Test
    public void testNotEmptyNodeTypeMinusAnotherNotEmptyNodeType() {
        Changeset changeset = ChangesetOperations.minus(
                changesetWithEmptyNodeType("doc1"),
                changesetWithEmptyNodeType("doc2"),
                new ChangesetInfo()
        );
        Assert.assertEquals(1, changeset.getNodeTypes().size());
    }

    private void assertChangesetIsEmpty(Changeset changeset) {
        Assert.assertTrue(changeset.getDocuments().isEmpty());
        Assert.assertTrue(changeset.getNodeTypes().isEmpty());
    }

    private Changeset changesetWithEmptyDocument(String documentType) {
        Changeset result = new Changeset();
        result.setDocuments(
                Collections.singletonMap(documentType, new DocumentDescription("namespace", documentType))
        );
        return result;
    }

    private Changeset changesetWithNotEmptyDocument(String documentType) {
        Changeset result = new Changeset();
        DocumentDescription documentDescription = new DocumentDescription("namespace", documentType);
        documentDescription.setExports(new ArrayList<>());
        result.setDocuments(
                Collections.singletonMap(documentType, documentDescription)
        );
        return result;
    }

    private Changeset changesetWithEmptyNodeType(String nodeTypeName) {
        Changeset result = new Changeset();
        result.setNodeTypes(
                Collections.singletonMap(nodeTypeName, new NodeType(nodeTypeName))
        );
        return result;
    }

    private Changeset changesetWithNotEmptyNodeType(String nodeTypeName) {
        Changeset result = new Changeset();
        NodeType nodeType = new NodeType(nodeTypeName);
        nodeType.setProperties(new HashMap<>());
        result.setNodeTypes(
                Collections.singletonMap(nodeTypeName, nodeType)
        );
        return result;
    }
}
