package ru.yandex.market.mbo.gwt.client.pages.tovartree;

import com.google.gwt.user.client.rpc.AsyncCallback;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import ru.yandex.market.mbo.gwt.client.services.ParameterServiceRemoteAsync;
import ru.yandex.market.mbo.gwt.models.params.CategoryParam;

import java.util.Collections;
import java.util.List;

/**
 * @author danfertev
 * @since 01.04.2020
 */
public class CachingCategoryParametersProviderTest {
    private static final long CATEGORY1 = 1L;
    private static final long CATEGORY2 = 2L;

    private ParameterServiceRemoteAsync parameterService;
    private CachingCategoryParametersProvider provider;

    @Before
    public void setUp() {
        parameterService = Mockito.mock(ParameterServiceRemoteAsync.class);
        Mockito.doAnswer(args -> {
            AsyncCallback<List<CategoryParam>> callback = args.getArgument(1);
            callback.onSuccess(Collections.emptyList());
            return null;
        }).when(parameterService).loadParameters(Mockito.anyLong(), Mockito.any());
        provider = new CachingCategoryParametersProvider(parameterService);
    }

    @Test
    public void testLoadOnlyOnceForCategoryRunSingleLoadCallback() {
        AsyncCallback<List<CategoryParam>> callback = Mockito.mock(AsyncCallback.class);
        provider.onLoad(CATEGORY1, callback);
        provider.onLoad(CATEGORY1, callback);

        Mockito.verify(parameterService, Mockito.times(1))
            .loadParameters(Mockito.eq(CATEGORY1), Mockito.any());

        Mockito.verify(callback, Mockito.times(1))
            .onSuccess(Mockito.anyList());
    }

    @Test
    public void testLoadOnlyOnceForCategoryRunAllGetCallbacks() {
        AsyncCallback<List<CategoryParam>> callback = Mockito.mock(AsyncCallback.class);
        provider.onGet(CATEGORY1, callback);
        provider.onGet(CATEGORY1, callback);

        Mockito.verify(parameterService, Mockito.times(1))
            .loadParameters(Mockito.eq(CATEGORY1), Mockito.any());

        Mockito.verify(callback, Mockito.times(2))
            .onSuccess(Mockito.anyList());
    }

    @Test
    public void testLoadDifferentCategoriesRunAllLoadCallbacks() {
        AsyncCallback<List<CategoryParam>> callback = Mockito.mock(AsyncCallback.class);
        provider.onLoad(CATEGORY1, callback);
        provider.onLoad(CATEGORY2, callback);

        Mockito.verify(parameterService, Mockito.times(1))
            .loadParameters(Mockito.eq(CATEGORY1), Mockito.any());
        Mockito.verify(parameterService, Mockito.times(1))
            .loadParameters(Mockito.eq(CATEGORY1), Mockito.any());

        Mockito.verify(callback, Mockito.times(2))
            .onSuccess(Mockito.anyList());
    }

    @Test
    public void testLoadDifferentCategoriesRunAllGetCallbacks() {
        AsyncCallback<List<CategoryParam>> callback = Mockito.mock(AsyncCallback.class);
        provider.onGet(CATEGORY1, callback);
        provider.onGet(CATEGORY2, callback);

        Mockito.verify(parameterService, Mockito.times(1))
            .loadParameters(Mockito.eq(CATEGORY1), Mockito.any());
        Mockito.verify(parameterService, Mockito.times(1))
            .loadParameters(Mockito.eq(CATEGORY2), Mockito.any());

        Mockito.verify(callback, Mockito.times(2))
            .onSuccess(Mockito.anyList());
    }
}
