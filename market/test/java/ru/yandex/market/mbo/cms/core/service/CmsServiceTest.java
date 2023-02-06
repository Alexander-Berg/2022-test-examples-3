package ru.yandex.market.mbo.cms.core.service;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.mbo.cms.core.dao.CmsPageDao;
import ru.yandex.market.mbo.cms.core.dao.CmsServiceDao;
import ru.yandex.market.mbo.cms.core.dao.node.processor.NodeProcessorHelper;
import ru.yandex.market.mbo.cms.core.models.ClientContext;
import ru.yandex.market.mbo.cms.core.models.ClientContextDefault;
import ru.yandex.market.mbo.cms.core.models.ClientContextJournal;
import ru.yandex.market.mbo.cms.core.models.CmsSchema;
import ru.yandex.market.mbo.cms.core.models.Constants;
import ru.yandex.market.mbo.cms.core.models.ContentContext;
import ru.yandex.market.mbo.cms.core.models.Node;
import ru.yandex.market.mbo.cms.core.models.PageContext;
import ru.yandex.market.mbo.cms.core.utils.CmsTemplateUtil;
import ru.yandex.market.mbo.cms.core.utils.OldTemplatesUtil;

import static org.junit.Assert.assertEquals;

/**
 * Created by chervotkin on 07.04.17.
 */
public class CmsServiceTest {

    private static final ClientContext CLIENT_CONTEXT_DEFAULT = new ClientContextDefault(1);
    private static final ClientContext CLIENT_CONTEXT_JOURNAL = new ClientContextJournal(1);
    public static final int SCHEMA_ID = 0;
    public static final String NAMESPACE = "ns";

    private CmsService cmsService;
    private CmsServiceDao cmsServiceDao;
    private TypografService typografService;
    private MboDataService mboDataService;
    private SchemaService schemaService;
    private CmsPageDao pageDao;
    private SimilarTagInfoProvider similarTagInfoProvider;

    @Before
    public void init() {
        typografService = Mockito.mock(TypografService.class);
        cmsServiceDao = Mockito.mock(CmsServiceDao.class);
        mboDataService = Mockito.mock(MboDataService.class);
        schemaService = Mockito.mock(SchemaService.class);
        similarTagInfoProvider = Mockito.mock(SimilarTagInfoProvider.class);
        cmsService = new CmsService(new CmsTemplateUtil(typografService), typografService);
        cmsService.setCmsServiceDao(cmsServiceDao);
        cmsService.setMboDataService(mboDataService);
        cmsService.setSchemaService(schemaService);
        cmsService.setSimilarTagInfoProvider(similarTagInfoProvider);
        pageDao = Mockito.mock(CmsPageDao.class);
        cmsService = Mockito.spy(cmsService);
        Mockito.when(mboDataService.getContentContext()).thenReturn(new ContentContext());
        Mockito.doReturn(new CmsSchema(NAMESPACE)).when(schemaService).getSchema(Mockito.anyInt());
    }

    @Test
    public void urlAsParamsToString() {
        Map<String, String> params = new HashMap<>();
        params.put("nid", "12345");
        Node w = makeUrlWidget("catalog", params);

        String s = NodeProcessorHelper.urlAsParamsToString(w, Collections.emptyMap());
        assertEquals("Bad url conversion!", "/catalog/12345/list", s);
    }

    public Node makeNode(String name, Constants.Device device, Constants.Format format, String template) {
        Node result = new Node(OldTemplatesUtil.makeBadNodeType(name, device, format, template, null), 0, 0);
        return result;
    }

    private Node makeUrlWidget(String target, Map<String, String> params) {
        String urlTemplateAsString = "{\n" +
                "  \"entity\": \"url\",\n" +
                "  \"target\": __URL_TARGET{1}__,\n" +
                "  \"params\": [__PARAMS(PARAM)*__]\n" +
                "}";

        String paramTemplateAsString = "{\n" +
                "  \"entity\": \"param\",\n" +
                "  \"name\": __NAME{1}__,\n" +
                "  \"value\": __VALUE{1}__\n" +
                "}";

        Node result = makeNode("", Constants.Device.DESKTOP, Constants.Format.JSON, urlTemplateAsString);
        result.setParameterValue("URL_TARGET", "catalog");

        for (Map.Entry<String, String> param : params.entrySet()) {
            result.addWidgetValue("PARAMS",
                    makeNode("", Constants.Device.DESKTOP, Constants.Format.JSON, paramTemplateAsString)
                            .setParameterValue("NAME", param.getKey())
                            .setParameterValue("VALUE", param.getValue()));
        }

        return result;
    }

    @Test
    public void processValue() {
        Assert.assertNull(CmsService.processValue(null, "rich_text", CLIENT_CONTEXT_DEFAULT));
        Assert.assertNull(CmsService.processValue(null, "rich_text", CLIENT_CONTEXT_JOURNAL));
        Assert.assertNull(CmsService.processValue(null, "other", CLIENT_CONTEXT_JOURNAL));

        Assert.assertEquals("<p>ttt</p>", CmsService.processValue("<p>ttt</p>",
                "rich_text", CLIENT_CONTEXT_DEFAULT));
        Assert.assertEquals("<p>ttt</p>", CmsService.processValue("<p id='1'>ttt</p>",
                "rich_text", CLIENT_CONTEXT_JOURNAL));
        Assert.assertEquals("<p>ttt</p><h3>ee</h3>",
                CmsService.processValue("<p id='1'>ttt</p><h3>ee</h3>", "rich_text", CLIENT_CONTEXT_JOURNAL));

        Assert.assertEquals("ttt",
                CmsService.processValue("<a onClick='f();'>ttt</a><script>rr</script>", "rich_text",
                        CLIENT_CONTEXT_JOURNAL));

        Assert.assertEquals("ttt",
                CmsService.processValue("<a onClick='f();'>ttt</a><script>rr</script>", "rich_text",
                        CLIENT_CONTEXT_JOURNAL));

        Assert.assertEquals("<a onClick='f();'>ttt</a><script>rr</script>",
                CmsService.processValue("<a onClick='f();'>ttt</a><script>rr</script>", "other",
                        CLIENT_CONTEXT_JOURNAL));

        Assert.assertEquals("<a onClick='f();'>ttt</a><script>rr</script>",
                CmsService.processValue("<a onClick='f();'>ttt</a><script>rr</script>", "rich_text",
                        CLIENT_CONTEXT_DEFAULT));

        Assert.assertEquals("<p class=\"v\">tttc</p>",
                CmsService.processValue("<p class=\"v\" style=\"s\"><a onClick='f();'>ttt</a><script>rr</script>c</p>",
                        "rich_text", CLIENT_CONTEXT_JOURNAL));
    }

    @Test
    @SuppressWarnings("magicnumber")
    public void loadPageContextTest() {
        /*
         * Должен искать последнюю ревизию, если revisionId = 0
         */
        long lastSchemaId = 10L;
        Mockito.doReturn(lastSchemaId)
                .when(cmsServiceDao).getLatestRevisionForContainer(Mockito.anyLong(), Mockito.any());
        CmsSchema schema = new CmsSchema();
        schema.setId(lastSchemaId);
        Mockito.doReturn(schema).when(schemaService).getSchema(Mockito.eq(1L));
        PageContext pageContext = cmsService.loadPageContext(1L, 0, 1L, false);
        Assert.assertEquals(lastSchemaId, pageContext.getAppContext().getSchema().getId());

        /*
         * Не должно падать, если page == null
         */
        cmsService.loadPageContext(null, 0, 1L, false);
    }
}
