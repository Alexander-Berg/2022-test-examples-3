package ru.yandex.market.mbo.cms.core.utils.schema.changeset;

import java.util.Collections;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.market.mbo.cms.core.models.Changeset;
import ru.yandex.market.mbo.cms.core.models.ChangesetInfo;
import ru.yandex.market.mbo.cms.core.models.DocumentDescription;
import ru.yandex.market.mbo.cms.core.models.NodeType;

public class ChangesetOperationsPlusTest {

    @Test(expected = IllegalArgumentException.class)
    public void testNullPlusNull() {
        ChangesetOperations.plus(null, null, new ChangesetInfo());
    }

    @Test
    public void testNullPlusEmpty() {
        Changeset changeset = ChangesetOperations.plus(null, new Changeset(), new ChangesetInfo());
        Assert.assertNotNull(changeset);
        assertChangesetIsEmpty(changeset);
    }

    @Test
    public void testEmptyPlusNull() {
        Changeset changeset = ChangesetOperations.plus(new Changeset(), null, new ChangesetInfo());
        Assert.assertNotNull(changeset);
        assertChangesetIsEmpty(changeset);
    }

    @Test
    public void testEmptyPlusEmpty() {
        Changeset changeset = ChangesetOperations.plus(new Changeset(), new Changeset(), new ChangesetInfo());
        Assert.assertNotNull(changeset);
        assertChangesetIsEmpty(changeset);
    }

    @Test
    public void testEmptyPlusDocument() {
        Changeset changeset = ChangesetOperations.plus(
                new Changeset(), changesetWithDocument("doc"), new ChangesetInfo()
        );
        Assert.assertEquals(1, changeset.getDocuments().size());
    }

    @Test
    public void testDocumentPlusEmpty() {
        Changeset changeset = ChangesetOperations.plus(
                changesetWithDocument("doc"), new Changeset(), new ChangesetInfo()
        );
        Assert.assertEquals(1, changeset.getDocuments().size());
    }

    @Test
    public void testDocumentPlusSameDocument() {
        Changeset changeset = ChangesetOperations.plus(
                changesetWithDocument("doc"), changesetWithDocument("doc"), new ChangesetInfo()
        );
        Assert.assertEquals(1, changeset.getDocuments().size());
    }

    @Test
    public void testDocumentPlusAnotherDocument() {
        Changeset changeset = ChangesetOperations.plus(
                changesetWithDocument("doc1"), changesetWithDocument("doc2"), new ChangesetInfo()
        );
        Assert.assertEquals(2, changeset.getDocuments().size());
    }

    @Test
    public void testEmptyPlusNodeType() {
        Changeset changeset = ChangesetOperations.plus(
                new Changeset(), changesetWithNodeType("doc"), new ChangesetInfo()
        );
        Assert.assertEquals(1, changeset.getNodeTypes().size());
    }

    @Test
    public void testNodeTypePlusEmpty() {
        Changeset changeset = ChangesetOperations.plus(
                changesetWithNodeType("doc"), new Changeset(), new ChangesetInfo()
        );
        Assert.assertEquals(1, changeset.getNodeTypes().size());
    }

    @Test
    public void testNodeTypePlusSameNodeType() {
        Changeset changeset = ChangesetOperations.plus(
                changesetWithNodeType("doc"), changesetWithNodeType("doc"), new ChangesetInfo()
        );
        Assert.assertEquals(1, changeset.getNodeTypes().size());
    }

    @Test
    public void testNodeTypePlusAnotherNodeType() {
        Changeset changeset = ChangesetOperations.plus(
                changesetWithNodeType("doc1"), changesetWithNodeType("doc2"), new ChangesetInfo()
        );
        Assert.assertEquals(2, changeset.getNodeTypes().size());
    }

    private void assertChangesetIsEmpty(Changeset changeset) {
        Assert.assertTrue(changeset.getDocuments().isEmpty());
        Assert.assertTrue(changeset.getNodeTypes().isEmpty());
    }

    private Changeset changesetWithDocument(String documentType) {
        Changeset result = new Changeset();
        result.setDocuments(
                Collections.singletonMap(documentType, new DocumentDescription("namespace", documentType))
        );
        return result;
    }

    private Changeset changesetWithNodeType(String nodeTypeName) {
        Changeset result = new Changeset();
        result.setNodeTypes(
                Collections.singletonMap(nodeTypeName, new NodeType(nodeTypeName))
        );
        return result;
    }
}
