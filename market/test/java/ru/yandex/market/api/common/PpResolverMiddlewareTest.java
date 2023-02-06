package ru.yandex.market.api.common;

import it.unimi.dsi.fastutil.ints.IntLists;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.web.method.HandlerMethod;
import ru.yandex.market.api.common.arguments.PpResolverMiddleware;
import ru.yandex.market.api.integration.UnitTestBase;
import ru.yandex.market.api.internal.common.PP;
import ru.yandex.market.api.server.context.Context;
import ru.yandex.market.api.server.context.ContextHolder;
import ru.yandex.market.api.server.sec.client.Client;
import ru.yandex.market.api.server.sec.client.internal.Tariff;
import ru.yandex.market.api.server.sec.client.internal.TestTariffs;
import ru.yandex.market.api.test.infrastructure.prerequisites.annotations.WithContext;

import java.lang.reflect.Method;

/**
 * Created by vdorogin on 23.06.17.
 */
@WithContext
public class PpResolverMiddlewareTest extends UnitTestBase {

    private static final int PP_FROM_REQUEST = 100;
    private static final int PP_FROM_ANNOTATION = 200;
    private static final int PP_FROM_CLIENT = 300;

    private PpResolverMiddleware ppResolver;
    private Context context;

    @Test
    public void defaultPlaceByMobileClientWithAnnotation() throws NoSuchMethodException {
        setClient(false, Client.Type.MOBILE, PP_FROM_CLIENT, TestTariffs.CUSTOM);
        context.setPpList(IntLists.singleton(PP_FROM_REQUEST));

        ppResolver.beforeArgumentsResolved(getHandlerMethod("methodWithoutPlace"));
        Assert.assertEquals(Integer.valueOf(PP.DEFAULT), context.getPp());
    }

    @Test
    public void defaultPlaceByMobileClientWithoutAnnotation() throws NoSuchMethodException {
        setClient(false, Client.Type.MOBILE, PP_FROM_CLIENT, TestTariffs.CUSTOM);
        context.setPpList(IntLists.singleton(PP_FROM_REQUEST));

        ppResolver.beforeArgumentsResolved(getHandlerMethod("methodWithoutAnnotation"));
        Assert.assertEquals(Integer.valueOf(PP.DEFAULT), context.getPp());
    }

    @Test
    public void emptyPlaceWithClient() throws NoSuchMethodException {
        setClient(false, Client.Type.INTERNAL, null, TestTariffs.BASE);
        context.setPpList(IntLists.singleton(PP_FROM_REQUEST));

        ppResolver.beforeArgumentsResolved(getHandlerMethod("methodWithPlace"));
        Assert.assertEquals(Integer.valueOf(PP.MARKET_API), context.getPp());
    }

    @Test
    public void emptyPlaceWithoutClient() throws NoSuchMethodException {
        context.setClient(null);
        context.setPpList(IntLists.singleton(PP_FROM_REQUEST));

        ppResolver.beforeArgumentsResolved(getHandlerMethod("methodWithPlace"));
        Assert.assertNull(context.getPp());
    }

    @Test
    public void marketApiPlaceForCustomTariff() throws NoSuchMethodException {
        setClient(false, Client.Type.INTERNAL, null, TestTariffs.CUSTOM);
        context.setPpList(IntLists.singleton(PP_FROM_REQUEST));

        ppResolver.beforeArgumentsResolved(getHandlerMethod("methodWithPlace"));
        Assert.assertEquals(Integer.valueOf(PP.MARKET_API), context.getPp());
    }

    // Mockito не умеет мокать final классы, поэтому замокать java.lang.reflect.Method не получается
    @GenericParamsFactory(
        places = {
            @PlaceParam(clientType = Client.Type.MOBILE, place = PP_FROM_ANNOTATION)
        }
    )
    public void methodWithPlace() {
    }

    public void methodWithoutAnnotation() {
    }

    @GenericParamsFactory(
        places =  {
            @PlaceParam(clientType = Client.Type.MOBILE, place = PP.MOBILE_MODEL_OFFERS)
        }
    )
    public void methodWithoutPlace() {
    }

    @GenericParamsFactory(
        places =  {
            @PlaceParam(clientType = Client.Type.EXTERNAL, place = PP.EXTERNAL_MODEL_OFFERS)
        }
    )
    public void methodWithExternalClientType() {
    }

    @Test
    public void placeFromAnnotationByMobileClient() throws NoSuchMethodException {
        setClient(false, Client.Type.MOBILE, PP_FROM_CLIENT, TestTariffs.CUSTOM);
        context.setPpList(IntLists.singleton(PP_FROM_REQUEST));

        ppResolver.beforeArgumentsResolved(getHandlerMethod("methodWithPlace"));
        Assert.assertEquals(Integer.valueOf(PP_FROM_ANNOTATION), context.getPp());
    }

    @Test
    public void placeFromAnnotationWhenRequestPPIsNull() throws NoSuchMethodException {
        setClient(true, Client.Type.MOBILE, PP_FROM_CLIENT, TestTariffs.CUSTOM);
        context.setPpList(IntLists.EMPTY_LIST);

        ppResolver.beforeArgumentsResolved(getHandlerMethod("methodWithPlace"));
        Assert.assertEquals(Integer.valueOf(PP_FROM_ANNOTATION), context.getPp());
    }

    @Test
    public void placeFromClientByNotMobileClient() throws NoSuchMethodException {
        setClient(false, Client.Type.INTERNAL, PP_FROM_CLIENT, TestTariffs.CUSTOM);
        context.setPpList(IntLists.singleton(PP_FROM_REQUEST));

        ppResolver.beforeArgumentsResolved(getHandlerMethod("methodWithPlace"));
        Assert.assertEquals(Integer.valueOf(PP_FROM_CLIENT), context.getPp());
    }

    @Test
    public void placeFromExternalClient() throws NoSuchMethodException {
        setClient(false, Client.Type.EXTERNAL, null, TestTariffs.CUSTOM);
        context.setPpList(IntLists.EMPTY_LIST);

        ppResolver.beforeArgumentsResolved(getHandlerMethod("methodWithExternalClientType"));
        Assert.assertEquals(Integer.valueOf(PP.EXTERNAL_MODEL_OFFERS), context.getPp());
    }

    @Test
    public void placeFromRequest() throws NoSuchMethodException {
        setClient(true, Client.Type.MOBILE, PP_FROM_CLIENT, TestTariffs.CUSTOM);
        context.setPpList(IntLists.singleton(PP_FROM_REQUEST));

        ppResolver.beforeArgumentsResolved(getHandlerMethod("methodWithPlace"));
        Assert.assertEquals(Integer.valueOf(PP_FROM_REQUEST), context.getPp());
    }

    @Before
    public void setUp() throws Exception {
        super.setUp();
        ppResolver = new PpResolverMiddleware();
        context = ContextHolder.get();
    }

    private HandlerMethod getHandlerMethod(String methodName) throws NoSuchMethodException {
        Method method = getClass().getMethod(methodName);
        HandlerMethod handlerMethod = Mockito.mock(HandlerMethod.class);
        Mockito.when(handlerMethod.getMethod()).thenReturn(method);
        return handlerMethod;
    }

    private void setClient(boolean allowPP, Client.Type type, Integer offerPlacementId, Tariff tariff) {
        Client client = context.getClient();
        client.setAllowPP(allowPP);
        client.setType(type);
        client.setOfferPlacementId(offerPlacementId);
        client.setTariff(tariff);
    }
}
