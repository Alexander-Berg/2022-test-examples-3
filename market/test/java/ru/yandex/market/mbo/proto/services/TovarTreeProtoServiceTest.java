package ru.yandex.market.mbo.proto.services;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import ru.yandex.market.mbo.db.RedisCachedGetTovarTreeResponseService;
import ru.yandex.market.mbo.db.RedisCachedTreeService;
import ru.yandex.market.mbo.db.TovarTreeService;
import ru.yandex.market.mbo.db.TovarTreeServiceMock;
import ru.yandex.market.mbo.db.category_wiki.CachedCategoryWikiService;
import ru.yandex.market.mbo.db.params.CategoryParametersExtractorService;
import ru.yandex.market.mbo.db.tovartree.TovarCategoryDataFilter;
import ru.yandex.market.mbo.export.MboParameters;
import ru.yandex.market.mbo.gwt.models.visual.TovarCategory;
import ru.yandex.market.mbo.tree.ExportTovarTree;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author galaev
 * @since 2019-01-28
 */
@SuppressWarnings("checkstyle:MagicNumber")
public class TovarTreeProtoServiceTest {

    private TovarTreeProtoService service;
    private TovarTreeProtoService cachedService;
    private TovarTreeService tovarTreeService;
    private RedisCachedTreeService redisCachedTreeService;
    private RedisCachedGetTovarTreeResponseService redisCachedGetTovarTreeResponseService;


    @Before
    public void setUp() throws Exception {
        tovarTreeService = mock(TovarTreeService.class);
        redisCachedTreeService = mock(RedisCachedTreeService.class);
        redisCachedGetTovarTreeResponseService =
                mock(RedisCachedGetTovarTreeResponseService.class);

        CachedCategoryWikiService categoryWikiService = mock(CachedCategoryWikiService.class);
        CategoryParametersExtractorService extractorService = mock(CategoryParametersExtractorService.class);

        when(extractorService.extractCategory(any(TovarCategory.class), any(TovarCategoryDataFilter.class)))
            .thenAnswer(invocation -> {
                TovarCategory category = invocation.getArgument(0);
                return MboParameters.Category.newBuilder()
                    .setHid(category.getHid())
                    .build();
            });

        service = new TovarTreeProtoService(
            extractorService,
            categoryWikiService,
            tovarTreeService,
            redisCachedTreeService,
            redisCachedGetTovarTreeResponseService,
            false
        );

        cachedService = new TovarTreeProtoService(
            extractorService,
            categoryWikiService,
            tovarTreeService,
            redisCachedTreeService,
            redisCachedGetTovarTreeResponseService,
            true
        );

        when(redisCachedGetTovarTreeResponseService.getTovarTree())
                .thenAnswer(invocation -> {
                    when(tovarTreeService.loadTovarTree())
                            .thenReturn(initTovarTree().loadTovarTree());
                    return service.getTovarTree(
                            ExportTovarTree.GetTovarTreeRequest
                                    .newBuilder()
                                    .build());
                });
    }

    @Test
    public void testGetTovarTree() {
        when(tovarTreeService.loadTovarTree())
            .thenReturn(initTovarTree().loadTovarTree());
        testLoadTovarTree(service);
        verify(tovarTreeService, times(1)).loadTovarTree();
        verify(redisCachedTreeService, never()).getTovarTree();
    }

    @Test
    @Ignore
    public void testGetTovarTreeCached() {
        when(redisCachedTreeService.getTovarTree())
                .thenReturn(initTovarTree().loadTovarTree());
        testLoadTovarTree(cachedService);
        verify(tovarTreeService, never()).loadTovarTree();
        verify(redisCachedTreeService, times(1)).getTovarTree();
    }

    private void testLoadTovarTree(TovarTreeProtoService service) {
        ExportTovarTree.GetTovarTreeRequest request = ExportTovarTree.GetTovarTreeRequest.newBuilder()
            .setLoadWiki(true)
            .build();

        ExportTovarTree.GetTovarTreeResponse tovarTree = service.getTovarTree(request);

        Assertions.assertThat(tovarTree.getCategoriesList()).hasSize(3);
        Assertions.assertThat(tovarTree.getCategoriesList()).extracting(MboParameters.Category::getHid)
            .containsExactlyInAnyOrder(1L, 2L, 3L);
    }

    private TovarTreeServiceMock initTovarTree() {
        TovarCategory root = new TovarCategory();
        root.setHid(1);
        TovarCategory department = new TovarCategory();
        department.setHid(2);
        department.setParentHid(root.getHid());
        TovarCategory leaf = new TovarCategory();
        leaf.setHid(3);
        leaf.setParentHid(department.getHid());

        TovarTreeServiceMock tovarTreeServiceMock = new TovarTreeServiceMock();
        tovarTreeServiceMock.addCategory(root);
        tovarTreeServiceMock.addCategory(department);
        tovarTreeServiceMock.addCategory(leaf);
        return tovarTreeServiceMock;
    }
}
