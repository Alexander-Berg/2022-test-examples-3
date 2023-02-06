package ru.yandex.market.mbo.catalogue.navigation;

import com.google.common.base.Supplier;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import ru.yandex.common.framework.core.AbstractServRequest;
import ru.yandex.common.framework.pager.Pager;
import ru.yandex.common.util.xml.XmlConvertable;
import ru.yandex.market.mbo.catalogue.templates.MarketPropertyTemplate;
import ru.yandex.market.mbo.core.kdepot.api.Entity;
import ru.yandex.market.mbo.core.kdepot.api.KnowledgeDepotService;
import ru.yandex.market.mbo.core.kdepot.api.KnownEntityTypes;
import ru.yandex.market.mbo.core.kdepot.impl.EntityImpl;
import ru.yandex.market.mbo.core.metadata.legacy.PropertyTemplateInterface;
import ru.yandex.market.mbo.core.metadata.legacy.PropertyTemplatesProvider;
import ru.yandex.market.mbo.core.navigation.ReportRequestParams;
import ru.yandex.market.mbo.core.report.builder.SortOrder;
import ru.yandex.market.mbo.db.params.guru.GuruVendorsReader;
import ru.yandex.market.mbo.gwt.models.visual.TovarTree;
import ru.yandex.market.mbo.user.UserManager;
import ru.yandex.market.mbo.utils.web.MarketServRequest;
import ru.yandex.market.security.SecManager;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * @author york
 * @since 15.06.2017
 */
@RunWith(MockitoJUnitRunner.StrictStubs.class)
@SuppressWarnings("checkstyle:magicNumber")
public class ExtendedCategoriesInfoTest {

    private static final Long ADMIN_ID = 1L;

    private final Multimap<Long, Long> canDo = ArrayListMultimap.create();

    @Mock
    private GuruVendorsReader vendorsReader;

    @Mock
    private PropertyTemplatesProvider<PropertyTemplateInterface> propertyTemplatesService;

    @Mock
    private KnowledgeDepotService knowledgeDepotService;

    @Mock
    private UserManager userManager;

    @Mock
    private SecManager secManager;

    class ReportableMock extends ExtendedCategoriesInfoKdepotReportable {

        ReportableMock(Supplier<Map<Long, CategoryUsersInfo>> userCategoriesCache, long entityTypeId) {
            super(ExtendedCategoriesInfoTest.this.knowledgeDepotService,
                ExtendedCategoriesInfoTest.this.propertyTemplatesService,
                ExtendedCategoriesInfoTest.this.vendorsReader,
                userCategoriesCache,
                entityTypeId,
                ExtendedCategoriesInfoTest.this.userManager,
                ExtendedCategoriesInfoTest.this.secManager);

            setTovarTree(new TovarTree());
        }

        @Override
        protected Map<String, PropertyTemplateInterface> getPropertyTemplates() {
            return Collections.singletonMap("name", new MarketPropertyTemplate("name"));
        }

        @Override
        protected List<Entity> searchEntities(ReportRequestParams params, Map<String, PropertyTemplateInterface> t) {
            return new ArrayList<>(generateEnities(entityTypeId));
        }
    }

    @Before
    public void init() {
        canDo.put(1L, 2000L);
        canDo.put(10L, 1000L);
        canDo.put(10L, 1003L);
        canDo.put(11L, 1001L);
        when(userManager.isAdmin(anyLong())).thenAnswer(
            invocation -> {
                Long uid = invocation.getArgument(0);
                return ADMIN_ID.equals(uid);
            }
        );
        when(secManager.canDo(anyString(), any())).thenAnswer(
            invocation -> {
                MarketServRequest data = invocation.getArgument(1);
                Long userId = data.getUserId();
                Long categoryId = data.getParamAsLong("category_id");
                return canDo.get(userId).contains(categoryId);
            }
        );
        when(vendorsReader.getGuruCategoryId(anyLong())).thenReturn(1000L);
    }

    @Test
    public void testReportableCategories() throws Exception {
        final List<Entity> entities = generateEnities(KnownEntityTypes.MARKET_CATEGORY);
        ReportableMock reportableForTest = new ReportableMock(() -> Collections.emptyMap(),
            KnownEntityTypes.MARKET_CATEGORY) {
            @Override
            protected List<Entity> searchEntities(ReportRequestParams params,
                                                  Map<String, PropertyTemplateInterface> t) {
                return new ArrayList<>(entities);
            }
        };
        List<? extends XmlConvertable> conv = reportableForTest.getItems(new ReportRequestParams()
            .setOrderedColumn("name")
            .setOrder(SortOrder.ASC)
            .setAttributeFilters(Collections.emptyMap())
            .setPager(new Pager(0, 100))
            .setReq(requestForUser(10L))
            .setOrderSet(true));

        Mockito.verify(secManager, Mockito.times(entities.size())).canDo(anyString(), any());
        Assert.assertEquals(canDo.get(10L).size(), conv.size());

        conv = reportableForTest.getItems(new ReportRequestParams()
            .setOrderedColumn("name")
            .setOrder(SortOrder.ASC)
            .setAttributeFilters(Collections.emptyMap())
            .setPager(new Pager(0, 100))
            .setReq(requestForUser(ADMIN_ID))
            .setOrderSet(true));
        Mockito.verify(userManager, Mockito.times(2)).isAdmin(anyLong());
        Assert.assertEquals(conv.size(), entities.size());

        for (XmlConvertable convertable : conv) {
            StringBuilder sb = new StringBuilder();
            convertable.toXml(sb);
            getDoc(sb.toString());
        }
    }

    @Test
    public void testReportableGoodVendors() throws Exception {
        final List<Entity> entities = generateEnities(KnownEntityTypes.MARKET_LOCAL_VENDOR);

        ReportableMock reportableForTest = new ReportableMock(Collections::emptyMap,
            KnownEntityTypes.MARKET_LOCAL_VENDOR) {
            @Override
            protected List<Entity> searchEntities(ReportRequestParams params,
                                                  Map<String, PropertyTemplateInterface> t) {
                return new ArrayList<>(entities);
            }
        };
        List<? extends XmlConvertable> conv = reportableForTest.getItems(new ReportRequestParams()
            .setOrderedColumn("name")
            .setOrder(SortOrder.ASC)
            .setAttributeFilters(Collections.emptyMap())
            .setPager(new Pager(0, 100))
            .setReq(requestForUser(10L))
            .setOrderSet(true));

        Assert.assertEquals(entities.size(), conv.size());
    }

    @Test (expected = Exception.class)
    public void testReportableDadVendors() throws Exception {
        final List<Entity> entities = generateEnities(KnownEntityTypes.MARKET_LOCAL_VENDOR);

        ReportableMock reportableForTest = new ReportableMock(Collections::emptyMap,
            KnownEntityTypes.MARKET_LOCAL_VENDOR) {
            @Override
            protected List<Entity> searchEntities(ReportRequestParams params,
                                                  Map<String, PropertyTemplateInterface> t) {
                return new ArrayList<>(entities);
            }
        };
        reportableForTest.getItems(new ReportRequestParams()
            .setOrderedColumn("name")
            .setOrder(SortOrder.ASC)
            .setAttributeFilters(Collections.emptyMap())
            .setPager(new Pager(0, 100))
            .setReq(requestForUser(11L))
            .setOrderSet(true));
    }

    private List<Entity> generateEnities(Long typeId) {
        return Arrays.asList(
            entity(1000L, typeId, "entity1", 2),
            entity(1001L, typeId, "entity2", 1),
            entity(1002L, typeId, "entity3", 2),
            entity(1003L, typeId, "entity4", 2)
        );
    }

    private Entity entity(Long id, Long typeId, String name, int publishLevel) {
        return new EntityImpl(id, typeId)
            .compress("name", name)
            .compress("publish_level", String.valueOf(publishLevel));
    }

    private MarketServRequest<?> requestForUser(Long userId) {
        return new MarketServRequest<>(new AbstractServRequest<Object>(userId, "", "navigation") {
        });
    }

    private static Document getDoc(String xmlStr) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.parse(new InputSource(new StringReader(xmlStr)));
    }
}
