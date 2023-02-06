package ru.yandex.market.mbo.cms.core.service;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import io.qameta.allure.Issue;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.common.util.collections.Pair;
import ru.yandex.market.mbo.cms.core.models.CmsAppContext;
import ru.yandex.market.mbo.cms.core.models.CmsPageKey;
import ru.yandex.market.mbo.cms.core.models.CmsSchema;
import ru.yandex.market.mbo.cms.core.models.Constants;
import ru.yandex.market.mbo.cms.core.models.DocumentDescription;
import ru.yandex.market.mbo.cms.core.models.DocumentDescriptionDto;
import ru.yandex.market.mbo.cms.core.models.DocumentExport;
import ru.yandex.market.mbo.cms.core.models.Key;
import ru.yandex.market.mbo.cms.core.models.KeyTemplate;
import ru.yandex.market.mbo.cms.core.models.KeyTemplates;
import ru.yandex.market.mbo.cms.core.models.PageIdAndType;
import ru.yandex.market.mbo.cms.core.models.Zoom;

/**
 * @author ayratgdl
 * @date 18.05.17
 */
public class CmsPageKeyMakerTest {
    private static final long PAGE_ID = 1;
    private static final long VIEW_ID = 100;
    private static final long NID = 1;
    private static final long SIMILAR_PAGE_ID = 2;
    private static final int POSITION_5 = 5;
    private static final int WEAKEST_POSITION = 9;
    private static final long SCHEMA_ID = 1;
    private static final long SCHEMA_REVISION_ID = 0;
    private static final boolean MASTER_SCHEMA = false;

    private CmsPageKeyMaker keyMaker;
    private CmsPageKeyMaker keyMakerMultiple;

    @Before
    public void setUp() {
        //similar
        keyMaker = buildKeyMaker(getTestedDocumentVersions(
                Arrays.asList(
                        Arrays.asList("ds", "device", "domain", "format", "zoom", "page_id", "strategy")
                                .stream().sorted().collect(Collectors.toList()),
                        Arrays.asList("ds", "device", "domain", "format", "zoom", "nid", "strategy")
                                .stream().sorted().collect(Collectors.toList())
                )
        ));
        keyMakerMultiple = buildKeyMaker(getTestedDocumentVersionsMultiple(
                Arrays.asList(
                        Arrays.asList("ds", "device", "domain", "format", "zoom", "page_id", "strategy")
                                .stream().sorted().collect(Collectors.toList()),
                        Arrays.asList("ds", "device", "domain", "format", "zoom", "nid", "strategy")
                                .stream().sorted().collect(Collectors.toList())
                )
        ));
    }

    private CmsPageKeyMaker buildKeyMaker(DocumentDescription documentDescription) {
        CmsSchema schema = Mockito.mock(CmsSchema.class);
        Mockito.when(schema.getId()).thenReturn(SCHEMA_ID);
        Mockito.when(schema.getName()).thenReturn(String.valueOf(SCHEMA_ID));
        Mockito.when(schema.getRevisionId()).thenReturn(SCHEMA_REVISION_ID);
        Mockito.when(schema.getDocument(Mockito.anyString())).thenReturn(documentDescription);

        CmsAppContext cmsAppContext = Mockito.mock(CmsAppContext.class);
        Mockito.when(cmsAppContext.getSchema()).thenReturn(schema);

        SchemaService schemaService = Mockito.mock(SchemaService.class);
        Mockito.when(schemaService.getSchema(Mockito.anyLong())).thenReturn(schema);

        CmsService cmsService = Mockito.mock(CmsService.class);
        Mockito.when(cmsService.getAppContext(Mockito.anyString())).thenReturn(cmsAppContext);
        Mockito
                .when(cmsService.retrieveViewId(
                        Mockito.anyLong(),
                        Mockito.anyLong(),
                        Mockito.anyString(),
                        Mockito.any(Constants.Device.class),
                        Mockito.any(Constants.Format.class)
                ))
                .thenReturn(VIEW_ID);
        Mockito
                .when(cmsService.findSimilarPages(
                        Mockito.any(),
                        Mockito.anyLong(),
                        Mockito.anyBoolean(),
                        Mockito.eq(Constants.SIMILAR_DOMAIN_ARTICLES_OLD)
                ))
                .thenReturn(
                        Collections.singletonList(new PageIdAndType(SIMILAR_PAGE_ID, "article"))
                );
        Mockito
                .when(cmsService.findSimilarPages(
                        Mockito.anyLong(),
                        Mockito.anyBoolean(),
                        Mockito.eq(Constants.SIMILAR_DOMAIN_ARTICLES_OLD),
                        Mockito.anySet()
                ))
                .thenReturn(
                        Collections.singletonList(new PageIdAndType(SIMILAR_PAGE_ID, "article"))
                );

        Mockito.when(cmsService.getDocSchemas(Mockito.anyString(), Mockito.anyString()))
                .thenReturn(getTestedDocumentVersionsAsMap());

        CmsPageKeyMaker result = new CmsPageKeyMaker();
        result.setCmsService(cmsService);
        result.setSchemaService(schemaService);
        return result;
    }

    @Test
    public void makePageKeysTest() {
        long pageId = PAGE_ID;
        Map<String, Set<String>> exportParams = new ExportParamsBuilder()
                .add(Constants.PAGE_ID_PARAM_NAME, Long.toString(pageId))
                .add(Constants.DOMAIN_PARAM_NAME, "ru")
                .add(KeyTemplates.DS_PARAM_NAME, "1")
                .build();
        boolean archived = false;

        CmsPageKeyBuilder keyBuilder = new CmsPageKeyBuilder()
                .setPageId(PAGE_ID).setViewId(VIEW_ID).setKeyType(CmsPageKey.KeyType.PAGE).setPosition(0);
        List<CmsPageKey> expectedKeys = Arrays.asList(
                // DEVICE_PARAM_NAME, DS_PARAM_NAME, FORMAT_PARAM_NAME, PAGE_ID_PARAM_NAME, VIEW_PARAM_NAME
                keyBuilder.setKey("device=desktop#ds=1#format=xml#page_id=1#zoom=entrypoints").build()
        );

        Map<Zoom, Map<KeyTemplate, List<CmsPageKey>>> actualKeys = keyMaker.makePageKeys(
                pageId, archived, exportParams, false, false, getTestedDocumentVersionsAsMap(
                        Arrays.asList(
                                Arrays.asList("ds", "device", "format", "zoom", "page_id")
                                        .stream().sorted().collect(Collectors.toList())
                        )
                ));

        Assert.assertEquals(expectedKeys, toList(actualKeys));
    }

    @Test
    public void makePageKeysFranchiseTest() {
        long franchiseId = 1L;
        long pageId = 1L;
        String pageType = "franchise";
        Map<String, Set<String>> exportParams = new ExportParamsBuilder()
                .add(Constants.FRANCHISE_PARAM_NAME, Long.toString(franchiseId))
                .add(Constants.DOMAIN_PARAM_NAME, "ru")
                .add(Constants.TYPE_PARAM_NAME, pageType)
                .add(Constants.HAS_CONTEXT_PARAMS, "true")
                .build();
        boolean archived = false;

        CmsPageKeyBuilder keyBuilder = new CmsPageKeyBuilder()
                .setPageId(PAGE_ID).setViewId(VIEW_ID).setKeyType(CmsPageKey.KeyType.PAGE).setPosition(0);
        CmsPageKeyBuilder keyBuilder2 = new CmsPageKeyBuilder()
                .setPageId(PAGE_ID).setViewId(VIEW_ID).setKeyType(CmsPageKey.KeyType.PAGE).setPosition(0);
        Set<CmsPageKey> expectedKeys = new HashSet<>(Arrays.asList(
                keyBuilder.setKey("device=phone#domain=ru#ds=1#format=xml#franchise_id=1" +
                        "#type=franchise#zoom=entrypoints").build(),
                keyBuilder2.setKey("device=desktop#domain=ru#ds=1#format=xml#franchise_id=1" +
                        "#type=franchise#zoom=entrypoints").build()
        ));

        Map<Zoom, Map<KeyTemplate, List<CmsPageKey>>> actualKeys = keyMakerMultiple
                .makePageKeys(pageId, archived, exportParams, false, false,
                        getTestedDocumentVersionsMuiltipleAsMap(
                                Arrays.asList(
                                        Arrays.asList("ds", "device", "domain", "format", "zoom", "type",
                                                "franchise_id")
                                                .stream().sorted().collect(Collectors.toList())
                                )
                        ));

        MatcherAssert.assertThat("equality without order",
                toList(actualKeys),
                Matchers.containsInAnyOrder(expectedKeys.toArray()));
    }

    @Test
    public void makePageSimilarKeys() {

        long pageId = PAGE_ID;
        Map<String, Set<String>> exportParams = new HashMap<>();
        boolean draft = true;

        CmsPageKeyBuilder key = new CmsPageKeyBuilder()
                .setPageId(pageId).setViewId(VIEW_ID).setKeyType(CmsPageKey.KeyType.SIMILAR)
                .setPosition(0);
        List<CmsPageKey> expectedKeys = Arrays.asList(
                // DEVICE_PARAM_NAME, DS_PARAM_NAME, DOMAIN_PARAM_NAME, FORMAT_PARAM_NAME, PAGE_ID_PARAM_NAME,
                // STRATEGY_PARAM_NAME, VIEW_PARAM_NAME
                key.setKey("device=desktop#domain=ru#ds=1#format=xml#page_id=1#strategy=similar#zoom=entrypoints")
                    .build(),
                key.setKey("device=desktop#domain=ua#ds=1#format=xml#page_id=1#strategy=similar#zoom=entrypoints")
                    .build(),
                key.setKey("device=desktop#domain=by#ds=1#format=xml#page_id=1#strategy=similar#zoom=entrypoints")
                    .build(),
                key.setKey("device=desktop#domain=kz#ds=1#format=xml#page_id=1#strategy=similar#zoom=entrypoints")
                    .build()
        );

        Map<Pair<Long, Long>, List<CmsPageKey>> actualKeys = keyMaker
                .makePageSimilarKeys(pageId, exportParams, draft, getTestedDocumentVersionsAsMap(
                        Arrays.asList(
                                Arrays.asList("ds", "device", "domain", "format", "zoom", "page_id", "strategy")
                                        .stream().sorted().collect(Collectors.toList())
                        )
                ));
        Assert.assertEquals(new HashSet<>(expectedKeys),
                new HashSet<>(actualKeys.get(Pair.of(SCHEMA_ID, SCHEMA_REVISION_ID))));
    }

    @Test
    public void makeNidSimilarKeys() {
        long nid = NID;
        boolean draft = true;

        CmsPageKeyBuilder key = new CmsPageKeyBuilder()
                .setPageId(0).setViewId(VIEW_ID).setKeyType(CmsPageKey.KeyType.SIMILAR).setPosition(0);
        List<CmsPageKey> expectedKeys = Arrays.asList(
                // DEVICE_PARAM_NAME, DS_PARAM_NAME, DOMAIN_PARAM_NAME, FORMAT_PARAM_NAME, PAGE_ID_PARAM_NAME,
                // STRATEGY_PARAM_NAME, VIEW_PARAM_NAME
                key.setKey("device=desktop#domain=ru#ds=1#format=xml#nid=1#strategy=similar#zoom=entrypoints").build(),
                key.setKey("device=desktop#domain=ua#ds=1#format=xml#nid=1#strategy=similar#zoom=entrypoints").build(),
                key.setKey("device=desktop#domain=by#ds=1#format=xml#nid=1#strategy=similar#zoom=entrypoints").build(),
                key.setKey("device=desktop#domain=kz#ds=1#format=xml#nid=1#strategy=similar#zoom=entrypoints").build()
        );

        Map<Pair<Long, Long>, List<CmsPageKey>> actualKeys = keyMaker.makeNidSimilarKeys(nid, draft,
                Constants.SIMILAR_DOMAIN_ARTICLES_OLD,
                SCHEMA_ID, Collections.emptySet());
        Assert.assertEquals(new HashSet<>(expectedKeys),
                new HashSet<>(actualKeys.get(Pair.of(SCHEMA_ID, SCHEMA_REVISION_ID))));
    }

    @Test
    @Issue("MBO-14883")
    public void makePageKeysWithPosition() {
        long pageId = PAGE_ID;
        Map<String, Set<String>> exportParams = new ExportParamsBuilder()
                .add(Constants.NID_PARAM_NAME, "10", "11", "12")
                .add(Constants.PAGE_PRIORITY_NID_PARAM_NAME, "10:5", "11:0")
                .add(Constants.HID_PARAM_NAME, "20")
                .add(Constants.PAGE_PRIORITY_HID_PARAM_NAME, "20:3")
                .add(Constants.TAG_PARAM_NAME, "a", "b", "c")
                .add(Constants.PAGE_PRIORITY_TAG_PARAM_NAME, "a:5", "b:0")
                .add(Constants.DOMAIN_PARAM_NAME, "ru")
                .add(KeyTemplates.DS_PARAM_NAME, "1")
                .build();

        boolean archived = false;
        String pageType = "article";

        CmsPageKeyBuilder key = new CmsPageKeyBuilder()
                .setPageId(pageId).setViewId(VIEW_ID).setKeyType(CmsPageKey.KeyType.PAGE);

        // На каждый nid и hid формируются по два ключа.
        // Первый ключ формируются по шаблону ключа:
        //   для nid:
        //     DEVICE_PARAM_NAME, DS_PARAM_NAME, FORMAT_PARAM_NAME, PAGE_ID_PARAM_NAME, VIEW_PARAM_NAME
        //   для hid:
        //     DEVICE_PARAM_NAME, DS_PARAM_NAME, FORMAT_PARAM_NAME, HID_PARAM_NAME, VIEW_PARAM_NAME
        List<CmsPageKey> expectedKeys = Arrays.asList(
                key.setKey("device=desktop#domain=ru#ds=1#format=xml#nid=10#zoom=entrypoints")
                        .setPosition(POSITION_5).build(),
                key.setKey("device=desktop#domain=ru#ds=1#format=xml#nid=11#zoom=entrypoints")
                        .setPosition(WEAKEST_POSITION).build(),
                key.setKey("device=desktop#domain=ru#ds=1#format=xml#nid=12#zoom=entrypoints")
                        .setPosition(WEAKEST_POSITION).build(),
                key.setKey("device=desktop#domain=ru#ds=1#format=xml#tag=a#zoom=entrypoints")
                        .setPosition(POSITION_5).build(),
                key.setKey("device=desktop#domain=ru#ds=1#format=xml#tag=b#zoom=entrypoints")
                        .setPosition(WEAKEST_POSITION).build(),
                key.setKey("device=desktop#domain=ru#ds=1#format=xml#tag=c#zoom=entrypoints")
                        .setPosition(WEAKEST_POSITION).build()
        );

        Map<Zoom, Map<KeyTemplate, List<CmsPageKey>>> actualKeys = keyMaker.makePageKeys(
                pageId, archived, exportParams, false, false, getTestedDocumentVersionsAsMap(
                        Arrays.asList(
                                Arrays.asList("ds", "device", "domain", "format", "zoom", "nid")
                                        .stream().sorted().collect(Collectors.toList()),
                                Arrays.asList("ds", "device", "domain", "format", "zoom", "tag")
                                        .stream().sorted().collect(Collectors.toList())
                        )
                ));

        MatcherAssert.assertThat("equality without order",
                toList(actualKeys),
                Matchers.containsInAnyOrder(expectedKeys.toArray()));
    }

    @Test
    @Issue("MBO-14883")
    public void makePageKeysMakeOnlyUniqueKeys() {
        long pageId = PAGE_ID;
        Map<String, Set<String>> exportParams = new ExportParamsBuilder()
                .add(Constants.PAGE_ID_PARAM_NAME, Long.toString(pageId))
                .add(Constants.HAS_CONTEXT_PARAMS, "true")
                .add(Constants.DOMAIN_PARAM_NAME, "ru")
                .add(Constants.NID_PARAM_NAME, "10")
                .add(Constants.NIDTYPE_PARAM_NAME, "ntype")
                .add(Constants.TYPE_PARAM_NAME, "type")
                .add(KeyTemplates.DS_PARAM_NAME, "1")
                .build();

        boolean archived = false;
        String pageType = "context_page";

        // Первый ключ формируется по шаблону:
        // DEVICE_PARAM_NAME, DOMAIN_PARAM_NAME, FORMAT_PARAM_NAME, NID_PARAM_NAME, NIDTYPE_PARAM_NAME,
        // TYPE_PARAM_NAME, VIEW_PARAM_NAME
        // Второй ключ формируется по шаблону:
        // DEVICE_PARAM_NAME, DS_PARAM_NAME, DOMAIN_PARAM_NAME, FORMAT_PARAM_NAME, NID_PARAM_NAME,
        // NIDTYPE_PARAM_NAME, TYPE_PARAM_NAME, VIEW_PARAM_NAME
        // На выходе имеем только два ключа
        CmsPageKeyBuilder key = new CmsPageKeyBuilder()
                .setPageId(pageId).setViewId(VIEW_ID).setKeyType(CmsPageKey.KeyType.PAGE).setPosition(WEAKEST_POSITION);
        CmsPageKeyBuilder key2 = new CmsPageKeyBuilder()
                .setPageId(pageId).setViewId(VIEW_ID).setKeyType(CmsPageKey.KeyType.PAGE);
        List<CmsPageKey> expectedKeys = Arrays.asList(
                key.setKey("device=desktop#domain=ru#ds=1#format=xml#nid=10#nodetype=ntype#type=type#zoom=entrypoints")
                        .build(),
                key2.setKey("device=desktop#ds=1#format=xml#page_id=1#zoom=entrypoints")
                        .build()
        );

        Map<Zoom, Map<KeyTemplate, List<CmsPageKey>>> actualKeys = keyMaker.makePageKeys(
                pageId, archived, exportParams, false, false, getTestedDocumentVersionsAsMap(
                        Arrays.asList(
                                Arrays.asList("ds", "device", "domain", "format", "zoom", "type", "nid", "nodetype")
                                        .stream().sorted().collect(Collectors.toList()),
                                Arrays.asList("ds", "device", "format", "zoom", "page_id")
                                        .stream().sorted().collect(Collectors.toList())
                        )
                ));
        MatcherAssert.assertThat("equality without order", toList(actualKeys),
                Matchers.containsInAnyOrder(expectedKeys.toArray()));
    }

    private static List<CmsPageKey> toList(Map<Zoom, Map<KeyTemplate, List<CmsPageKey>>> actualKeys) {
        return actualKeys
                .values()
                .stream()
                .map(m -> m.values())
                .flatMap(k -> k.stream())
                .flatMap(k -> k.stream())
                .collect(Collectors.toList());
    }

    @Test
    @Issue("MBO-15029")
    public void makeKeyWithProductIdWithoutNid() {
        long pageId = PAGE_ID;
        Map<String, Set<String>> exportParams = new ExportParamsBuilder()
                .add(Constants.PAGE_ID_PARAM_NAME, Long.toString(pageId))
                .add(Constants.HAS_CONTEXT_PARAMS, "true")
                .add(Constants.DOMAIN_PARAM_NAME, "ru")
                .add(Constants.TYPE_PARAM_NAME, "product")
                .add(KeyTemplates.DS_PARAM_NAME, "1")
                .add(Constants.PRODUCT_ID_PARAM_NAME, "101")
                .build();

        boolean archived = false;
        String pageType = "product";

        // Первый ключ будет формироваться по шаблону:
        // DEVICE_PARAM_NAME, DOMAIN_PARAM_NAME, DS_PARAM_NAME, FORMAT_PARAM_NAME,
        // PRODUCT_ID_PARAM_NAME, TYPE_PARAM_NAME, VIEW_PARAM_NAME
        // Второй ключ формируется по шаблону:
        // DEVICE_PARAM_NAME, DS_PARAM_NAME, FORMAT_PARAM_NAME, PAGE_ID_PARAM_NAME, VIEW_PARAM_NAME
        CmsPageKeyBuilder keyBuilder = new CmsPageKeyBuilder()
                .setPageId(pageId).setViewId(VIEW_ID).setKeyType(CmsPageKey.KeyType.PAGE);
        CmsPageKey expectedKey1 = keyBuilder
                .setKey("device=desktop#domain=ru#ds=1#format=xml#product_id=101#type=product#zoom=entrypoints")
            .build();
        CmsPageKey expectedKey2 = keyBuilder
                .setKey("device=desktop#ds=1#format=xml#page_id=1#zoom=entrypoints").build();
        Map<Zoom, Map<KeyTemplate, List<CmsPageKey>>> actualKeys = keyMaker
                .makePageKeys(pageId, archived, exportParams, false, false,
                    getTestedDocumentVersionsAsMap(
                        Arrays.asList(
                                Arrays.asList("ds", "device", "domain", "format", "zoom", "type", "product_id")
                                        .stream().sorted().collect(Collectors.toList()),
                                Arrays.asList("ds", "device", "format", "zoom", "page_id")
                                        .stream().sorted().collect(Collectors.toList())
                        )
                ));
        MatcherAssert.assertThat("equality without order",
                toList(actualKeys),
                Matchers.containsInAnyOrder(expectedKey1, expectedKey2));
    }

    @Test
    @Issue("MBO-15029")
    public void makeTwoKeyWithProductId() {
        long pageId = PAGE_ID;
        Map<String, Set<String>> exportParams = new ExportParamsBuilder()
                .add(Constants.PAGE_ID_PARAM_NAME, Long.toString(pageId))
                .add(Constants.HAS_CONTEXT_PARAMS, "true")
                .add(Constants.DOMAIN_PARAM_NAME, "ru")
                .add(Constants.TYPE_PARAM_NAME, "product")
                .add(KeyTemplates.DS_PARAM_NAME, "1")
                .add(Constants.PRODUCT_ID_PARAM_NAME, "101")
                .add(Constants.BRAND_ID_PARAM_NAME, "201")
                .add(Constants.NID_PARAM_NAME, "301")
                .build();

        boolean archived = false;
        String pageType = "product";

        // Первый ключ будет формироваться по шаблону:
        // DEVICE_PARAM_NAME, DOMAIN_PARAM_NAME, DS_PARAM_NAME, FORMAT_PARAM_NAME,
        // PRODUCT_ID_PARAM_NAME, TYPE_PARAM_NAME, VIEW_PARAM_NAME
        // Второй ключ формируется по шаблону:
        // BRAND_ID_PARAM_NAME, DEVICE_PARAM_NAME, DOMAIN_PARAM_NAME, DS_PARAM_NAME,
        // FORMAT_PARAM_NAME, NID_PARAM_NAME, PRODUCT_ID_PARAM_NAME, TYPE_PARAM_NAME, VIEW_PARAM_NAME
        // Третий ключ формируется по шаблону:
        // DEVICE_PARAM_NAME, DS_PARAM_NAME, FORMAT_PARAM_NAME, PAGE_ID_PARAM_NAME, VIEW_PARAM_NAME
        CmsPageKeyBuilder keyBuilder = new CmsPageKeyBuilder()
                .setPageId(pageId).setViewId(VIEW_ID).setKeyType(CmsPageKey.KeyType.PAGE);
        CmsPageKey expectedKey1 = keyBuilder
                .setPosition(0)
                .setKey("device=desktop#domain=ru#ds=1#format=xml#product_id=101#type=product#zoom=entrypoints")
            .build();
        CmsPageKey expectedKey2 = keyBuilder
                .setPosition(WEAKEST_POSITION)
                .setKey("brand_id=201#device=desktop#domain=ru#ds=1#format=xml#nid=301#product_id=101#type=product" +
                        "#zoom=entrypoints")
                .build();
        CmsPageKey expectedKey3 = keyBuilder
                .setPosition(0)
                .setKey("device=desktop#ds=1#format=xml#page_id=1#zoom=entrypoints").build();
        Map<Zoom, Map<KeyTemplate, List<CmsPageKey>>> actualKeys = keyMaker.makePageKeys(
                pageId, archived, exportParams, false, false, getTestedDocumentVersionsAsMap(
                        Arrays.asList(
                                Arrays.asList("ds", "device", "domain", "format", "zoom", "type", "product_id")
                                        .stream().sorted().collect(Collectors.toList()),
                                Arrays.asList("ds", "device", "domain", "format", "zoom", "brand_id", "nid",
                                        "product_id", "type")
                                        .stream().sorted().collect(Collectors.toList()),
                                Arrays.asList("ds", "device", "format", "zoom", "page_id")
                                        .stream().sorted().collect(Collectors.toList())
                        )
                ));
        MatcherAssert.assertThat("equality without order",
                toList(actualKeys),
                Matchers.containsInAnyOrder(expectedKey1, expectedKey2, expectedKey3));
    }

    @NotNull
    private DocumentDescription getTestedDocumentVersions(Collection<List<String>> keyTemplatesRaw) {
        DocumentExport export = new DocumentExport();
        export.setDevice(Constants.Device.DESKTOP);
        export.setFormat(Constants.Format.XML);
        export.setView("entrypoints");
        export.setKeyTemplates(makeKeyTemplates(keyTemplatesRaw));
        export.setSimilarKeyTemplates(makeKeyTemplates(keyTemplatesRaw));

        DocumentDescription version = new DocumentDescription();
        version.setExports(Collections.singletonList(export));
        version.setSimilarDomain(Constants.SIMILAR_DOMAIN_ARTICLES_OLD);
        return version;
    }

    private static List<KeyTemplate> makeKeyTemplates(Collection<List<String>> keyTemplatesRaw) {
        return keyTemplatesRaw.stream().map(k -> new KeyTemplate(k, false, false))
            .collect(Collectors.toList());
    }

    @NotNull
    private List<DocumentDescriptionDto> getTestedDocumentVersionsAsMap() {
        return getTestedDocumentVersionsAsMap(Collections.emptyList());
    }

    @NotNull
    private List<DocumentDescriptionDto> getTestedDocumentVersionsAsMap(Collection<List<String>> keyTemplatesRaw) {
        return Collections.singletonList(new DocumentDescriptionDto(
                SCHEMA_ID,
                SCHEMA_REVISION_ID,
                String.valueOf(SCHEMA_ID),
                MASTER_SCHEMA,
                getTestedDocumentVersions(keyTemplatesRaw)
        ));
    }

    @NotNull
    private List<DocumentDescriptionDto> getTestedDocumentVersionsMuiltipleAsMap(
            Collection<List<String>> keyTemplatesRaw) {
        return Collections.singletonList(new DocumentDescriptionDto(
                SCHEMA_ID,
                SCHEMA_REVISION_ID,
                String.valueOf(SCHEMA_ID),
                MASTER_SCHEMA,
                getTestedDocumentVersionsMultiple(keyTemplatesRaw))
        );
    }

    @NotNull
    private DocumentDescription getTestedDocumentVersionsMultiple(Collection<List<String>> keyTemplatesRaw) {
        DocumentExport export1 = new DocumentExport();
        export1.setDevice(Constants.Device.DESKTOP);
        export1.setFormat(Constants.Format.XML);
        export1.setView("entrypoints");
        export1.setKeyTemplates(makeKeyTemplates(keyTemplatesRaw));

        DocumentExport export2 = new DocumentExport();
        export2.setDevice(Constants.Device.PHONE);
        export2.setFormat(Constants.Format.XML);
        export2.setView("entrypoints");
        export2.setKeyTemplates(makeKeyTemplates(keyTemplatesRaw));

        DocumentDescription version = new DocumentDescription();
        version.setExports(Arrays.asList(export1, export2));
        version.setSimilarDomain(Constants.SIMILAR_DOMAIN_ARTICLES_OLD);
        return version;
    }

    private static class ExportParamsBuilder {
        private final Map<String, Set<String>> exportParams = new HashMap<>();

        private ExportParamsBuilder add(String paramName, String... values) {
            exportParams.put(paramName, new HashSet<>(Arrays.asList(values)));
            return this;
        }

        private Map<String, Set<String>> build() {
            return exportParams;
        }
    }

    private static class CmsPageKeyBuilder {
        private long pageId;
        private String key;
        private long viewId;
        private CmsPageKey.KeyType keyType;
        private int position;

        CmsPageKeyBuilder() {
        }

        public CmsPageKeyBuilder setPageId(long pageId) {
            this.pageId = pageId;
            return this;
        }

        public CmsPageKeyBuilder setKey(String key) {
            this.key = key;
            return this;
        }

        public CmsPageKeyBuilder setViewId(long viewId) {
            this.viewId = viewId;
            return this;
        }

        public CmsPageKeyBuilder setKeyType(CmsPageKey.KeyType keyType) {
            this.keyType = keyType;
            return this;
        }

        public CmsPageKeyBuilder setPosition(int position) {
            this.position = position;
            return this;
        }

        public CmsPageKey build() {
            return new CmsPageKey(pageId, new Key(key), viewId, keyType, position);
        }
    }
}
