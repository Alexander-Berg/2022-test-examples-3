package ru.yandex.market.mbo.synchronizer.export.navigation;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.market.mbo.db.navigation.FastFilterService;
import ru.yandex.market.mbo.db.navigation.FilterConfigService;
import ru.yandex.market.mbo.db.navigation.ModelListService;
import ru.yandex.market.mbo.db.navigation.NavigationTreeNodeRedirectService;
import ru.yandex.market.mbo.db.navigation.NavigationTreeService;
import ru.yandex.market.mbo.gwt.models.navigation.NavigationTree;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author kravchenko-aa
 * @date 13.11.18
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class NavigationTreeProviderTest {
    private static final Long DEFAULT_TREE_ID = 42L;
    private static final Long NEW_TREE_ID = 4200L;
    private static final int INSTANCE_COUNT = 1000;
    private static final String FIRST_SESSION = "first";
    private static final String SECOND_SESSION = "second";

    @Mock
    private NavigationTreeService navigationTreeService;
    @Mock
    private NavigationTreeNodeRedirectService navigationTreeNodeRedirectService;
    private NavigationTreeProvider navigationTreeProvider;

    @Before
    public void setUp() {
        ModelListService modelListService = Mockito.mock(ModelListService.class);
        when(navigationTreeService.getModelListService()).thenReturn(modelListService);
        when(navigationTreeService.getModelListService().getAllModelLists()).thenReturn(Collections.emptyList());

        FilterConfigService filterConfigService = Mockito.mock(FilterConfigService.class);
        when(navigationTreeService.getFilterConfigService()).thenReturn(filterConfigService);
        when(navigationTreeService.getFilterConfigService().getAllFilterConfigs()).thenReturn(Collections.emptyList());


        FastFilterService fastFilterService = Mockito.mock(FastFilterService.class);
        when(navigationTreeService.getFastFilterService()).thenReturn(fastFilterService);
        when(navigationTreeService.getFastFilterService().getAllFilters()).thenReturn(Collections.emptyList());

        when(navigationTreeService.getNavigationTrees()).thenReturn(createTreeList(DEFAULT_TREE_ID));
        navigationTreeProvider = new NavigationTreeProvider();
        navigationTreeProvider.setNavigationTreeService(navigationTreeService);
        navigationTreeProvider.setNavigationTreeNodeRedirectService(navigationTreeNodeRedirectService);
    }

    @Test
    public void testCallWithoutSession() {
        StubCallable callable = new StubCallable(navigationTreeProvider, null);
        Long result = callable.call();
        assertEquals(DEFAULT_TREE_ID, result);

        when(navigationTreeService.getNavigationTrees()).thenReturn(createTreeList(NEW_TREE_ID));
        result = callable.call();
        assertEquals(NEW_TREE_ID, result);
        verify(navigationTreeService, times(2)).getNavigationTrees();
    }

    @Test
    public void testCallWithSession() {
        StubCallable first = new StubCallable(navigationTreeProvider, FIRST_SESSION);
        Long firstResult = first.call();
        assertEquals(DEFAULT_TREE_ID, firstResult);

        when(navigationTreeService.getNavigationTrees()).thenReturn(createTreeList(NEW_TREE_ID));
        firstResult = first.call();
        assertEquals(DEFAULT_TREE_ID, firstResult);
        verify(navigationTreeService, times(1)).getNavigationTrees();

        StubCallable second = new StubCallable(navigationTreeProvider, SECOND_SESSION);
        Long secondResult = second.call();
        assertEquals(NEW_TREE_ID, secondResult);
        verify(navigationTreeService, times(2)).getNavigationTrees();
    }

    @Test
    public void testCallInManyThreads() throws ExecutionException, InterruptedException {
        ExecutorService service = Executors.newCachedThreadPool();
        List<Future<Long>> result = new ArrayList<>();
        for (int i = 0; i < INSTANCE_COUNT; i++) {
            StubCallable c = new StubCallable(navigationTreeProvider, FIRST_SESSION);
            result.add(service.submit(c));
        }

        int count = 0;
        for (Future<Long> future : result) {
            if (!future.get().equals(DEFAULT_TREE_ID)) {
                count++;
            }
        }
        assertEquals(0, count);
        verify(navigationTreeService, times(1)).getNavigationTrees();
    }

    private List<NavigationTree> createTreeList(long id) {
        NavigationTree t = new NavigationTree();
        t.setId(id);
        return Collections.singletonList(t);
    }

    private static class StubCallable implements Callable<Long> {
        private final NavigationTreeProvider navigationTreeProvider;
        private final String id;

        StubCallable(NavigationTreeProvider navigationTreeProvider, String id) {
            this.navigationTreeProvider = navigationTreeProvider;
            this.id = id;
        }

        @Override
        public Long call() {
            return navigationTreeProvider.getNavigationTrees(id).get(0).getId();
        }
    }
}
