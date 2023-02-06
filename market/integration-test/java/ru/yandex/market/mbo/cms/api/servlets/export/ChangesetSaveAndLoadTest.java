package ru.yandex.market.mbo.cms.api.servlets.export;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mbo.cms.AbstractTest;
import ru.yandex.market.mbo.cms.api.servlets.export.util.changeset.ChangesetsEqualsTestHelper;
import ru.yandex.market.mbo.cms.core.dao.SchemaDao;
import ru.yandex.market.mbo.cms.core.models.Changeset;
import ru.yandex.market.mbo.cms.core.models.ChangesetActionType;
import ru.yandex.market.mbo.cms.core.models.ChangesetInfo;
import ru.yandex.market.mbo.cms.core.models.ChangesetSource;
import ru.yandex.market.mbo.cms.core.models.Constants;
import ru.yandex.market.mbo.cms.core.models.DocumentDescription;
import ru.yandex.market.mbo.cms.core.models.DocumentExport;
import ru.yandex.market.mbo.cms.core.models.FieldType;
import ru.yandex.market.mbo.cms.core.models.KeyTemplate;
import ru.yandex.market.mbo.cms.core.models.NodeType;

public class ChangesetSaveAndLoadTest extends AbstractTest {
    private static final long USER_ID = 764301093; //market-cms-test-user
    private static final String NAMESPACE = "test_namespace";

    @Autowired
    private SchemaDao schemaDao;

    @Test
    public void testChangesetInfo() {
        Changeset toSave;
        Changeset loaded;

        toSave = new Changeset(new ChangesetInfo(ChangesetSource.EDITOR, ChangesetActionType.PLUS));
        loaded = saveAndLoad(toSave);

        Assert.assertEquals(ChangesetActionType.PLUS, loaded.getInfo().getActionType());
        Assert.assertEquals(ChangesetSource.EDITOR, loaded.getInfo().getSource());

        toSave = new Changeset(new ChangesetInfo(ChangesetSource.API, ChangesetActionType.MINUS));
        loaded = saveAndLoad(toSave);

        Assert.assertEquals(ChangesetActionType.MINUS, loaded.getInfo().getActionType());
        Assert.assertEquals(ChangesetSource.API, loaded.getInfo().getSource());
        Assert.assertNotNull(loaded.getDocuments());
        Assert.assertNotNull(loaded.getNodeTypes());
    }

    @Test
    public void testDocuments() {
        Changeset toSave;
        Changeset loaded;

        toSave = createChangeset();
        toSave.setDocuments(createDocuments());
        loaded = saveAndLoad(toSave);

        ChangesetsEqualsTestHelper.assertEquals(toSave, loaded);
    }

    @Test
    public void testNodeTypes() {
        Changeset toSave;
        Changeset loaded;

        toSave = createChangeset();
        toSave.setNodeTypes(createNodeTypes());
        loaded = saveAndLoad(toSave);

        ChangesetsEqualsTestHelper.assertEquals(toSave, loaded);
    }

    private Map<String, NodeType> createNodeTypes() {
        Map<String, NodeType> result = new HashMap<>();

        NodeType nt = new NodeType("nt1");
        result.put(nt.getName(), nt);

        nt = new NodeType("nt2");
        nt.addParentNamesImmediate("pname");
        nt.addField("fi", new FieldType("fi"));
        nt.addTemplate(Constants.Device.DESKTOP, Constants.Format.JSON, "template");
        nt.addProperty("prop", Collections.singletonList("val"));
        nt.addPropertyBranch("path", "prop", Collections.singletonList("val"));
        nt.addPropertyBranch("path2", null, null);
        result.put(nt.getName(), nt);

        return result;
    }

    private Map<String, DocumentDescription> createDocuments() {
        Map<String, DocumentDescription> result = new HashMap<>();

        DocumentDescription dd = new DocumentDescription(NAMESPACE, "dd1");
        result.put(dd.getType(), dd);

        DocumentExport de = new DocumentExport();
        de.setView("test_view");
        de.setFormat(Constants.Format.JSON);
        de.setDevice(Constants.Device.DESKTOP);
        dd = new DocumentDescription(NAMESPACE, "dd2");
        dd.setRootTemplate("rt");
        dd.setSimilarDomain("si");
        dd.setLabel("la");
        dd.setExports(Collections.singletonList(de));
        result.put(dd.getType(), dd);

        KeyTemplate kt = new KeyTemplate();
        de = new DocumentExport();
        de.setView("test_view");
        de.setFormat(Constants.Format.XML);
        de.setDevice(Constants.Device.PHONE);
        de.setUrlPrefix("prefix");
        de.setClient("client");
        de.setResponsibles(Collections.singletonList("resp"));
        de.setIdentityFields(Collections.singletonList("ident"));
        de.setKeyTemplates(Collections.singletonList(kt));
        de.setSimilarKeyTemplates(Collections.singletonList(kt));
        dd = new DocumentDescription(NAMESPACE, "dd3");
        dd.setExports(Collections.singletonList(de));
        result.put(dd.getType(), dd);

        kt = new KeyTemplate();
        kt.setTemplate(Collections.singletonList("templ"));
        kt.setUniq(false);
        kt.setRequiredGroup("grp");
        kt.setRequired(false);
        de = new DocumentExport();
        de.setView("test_view");
        de.setFormat(Constants.Format.XML);
        de.setDevice(Constants.Device.PHONE);
        de.setKeyTemplates(Collections.singletonList(kt));
        de.setSimilarKeyTemplates(Collections.singletonList(kt));
        dd = new DocumentDescription(NAMESPACE, "dd4");
        dd.setExports(Collections.singletonList(de));
        result.put(dd.getType(), dd);

        kt = new KeyTemplate();
        kt.setTemplate(Collections.singletonList("templ"));
        kt.setUniq(false);
        kt.setRequiredGroup("grp");
        kt.setRequired(false);
        de = new DocumentExport();
        de.setView("test_view");
        de.setFormat(Constants.Format.XML);
        de.setDevice(Constants.Device.PHONE);
        de.setKeyTemplates(Collections.singletonList(kt));
        de.setSimilarKeyTemplates(Collections.singletonList(kt));
        dd = new DocumentDescription(NAMESPACE, "dd5");
        dd.setExports(Collections.singletonList(de));
        result.put(dd.getType(), dd);

        return result;
    }

    private Changeset createChangeset() {
        return new Changeset(new ChangesetInfo(ChangesetSource.EDITOR, ChangesetActionType.PLUS));
    }

    private Changeset saveAndLoad(Changeset changeset) {
        long changesetId = schemaDao.saveChangeset(NAMESPACE, changeset, USER_ID);
        Changeset loaded = schemaDao.loadFullChangeset(changesetId);
        Assert.assertEquals(USER_ID, loaded.getInfo().getCreatorId());
        return loaded;
    }
}
