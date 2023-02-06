package ru.yandex.market.api.server;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.common.collect.Lists;
import io.netty.util.concurrent.Future;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.exceptions.base.MockitoException;

import ru.yandex.market.api.ContextHolderTestHelper;
import ru.yandex.market.api.MockClientHelper;
import ru.yandex.market.api.MockContextBuilder;
import ru.yandex.market.api.MockRequestBuilder;
import ru.yandex.market.api.common.Result;
import ru.yandex.market.api.controller.v2.CommonParameters;
import ru.yandex.market.api.controller.v2.ParametersV2;
import ru.yandex.market.api.domain.v2.GeoCoordinatesV2;
import ru.yandex.market.api.error.ApiError;
import ru.yandex.market.api.error.NotFoundException;
import ru.yandex.market.api.geo.GeoRegionService;
import ru.yandex.market.api.geo.GeoUtils;
import ru.yandex.market.api.geo.domain.GeoRegion;
import ru.yandex.market.api.geo.domain.RegionType;
import ru.yandex.market.api.integration.BaseTest;
import ru.yandex.market.api.internal.common.GenericParams;
import ru.yandex.market.api.internal.common.GenericParamsBuilder;
import ru.yandex.market.api.internal.common.RearrFactor;
import ru.yandex.market.api.internal.geo.GeobaseService;
import ru.yandex.market.api.internal.geo.LbsService;
import ru.yandex.market.api.internal.geo.domain.SignalsInfo;
import ru.yandex.market.api.server.RegionalInfoResolver.RegionErrorCode;
import ru.yandex.market.api.server.RegionalInfoResolver.RegionErrorType;
import ru.yandex.market.api.server.context.Context;
import ru.yandex.market.api.server.sec.client.Client;
import ru.yandex.market.api.server.sec.client.ClientHelper;
import ru.yandex.market.api.server.sec.client.CommonClient;
import ru.yandex.market.api.test.infrastructure.prerequisites.annotations.WithMocks;
import ru.yandex.market.api.util.concurrent.Futures;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.api.server.MobileRegionalInfoResolver.FALLBACK_REGION;

/**
 * @author Kirill Sulim sulim@yandex-team.ru
 */
@WithMocks
public class RegionalInfoResolverTest extends BaseTest {

    private static class GeoCoordinatesMatcher extends ArgumentMatcher<GeoCoordinatesV2> {

        private double lat;
        private double lon;

        public GeoCoordinatesMatcher(double lat, double lon) {
            this.lat = lat;
            this.lon = lon;
        }

        @Override
        public boolean matches(Object argument) {
            if (!(argument instanceof GeoCoordinatesV2)) {
                return false;
            } else {
                GeoCoordinatesV2 coord = (GeoCoordinatesV2) argument;
                return coord.getLatitude() == lat && coord.getLongitude() == lon;
            }
        }
    }

    private static final int BOGDANOVICH_REGION_ID = 20233;
    private static final int MONTGOMERY_REGION_ID = 21285;
    private static final int MOSCOW_REGION_ID = 213;
    private static final int USA_REGION_ID = 84;

    private RegionalInfoResolverRegistry regionalInfoResolverRegistry;

    @InjectMocks
    private MobileRegionalInfoResolver mobileRegionalInfoResolver;

    @InjectMocks
    private PartnerRegionalInfoResolver partnerRegionalInfoResolver;

    @Mock
    private GeobaseService geobaseService;

    @Mock
    private GeoRegionService geoRegionService;

    @Mock
    private LbsService lbsService;

    @Mock
    private ClientHelper clientHelper;

    private MockClientHelper mockClientHelper;

    @Before
    public void setUp() throws Exception {
        when(geobaseService.getRegionIdByIp(anyString())).thenReturn(Futures.newSucceededFuture(Result.newResult(2)));

        GeoRegion russia = new GeoRegion();
        russia.setId(GeoUtils.Country.RUSSIA);
        russia.setType(RegionType.COUNTRY);

        GeoRegion bogdanovich = new GeoRegion();
        bogdanovich.setId(BOGDANOVICH_REGION_ID);
        bogdanovich.setType(RegionType.CITY);

        GeoRegion montgomery = new GeoRegion();
        montgomery.setId(MONTGOMERY_REGION_ID);
        montgomery.setType(RegionType.CITY);

        GeoRegion usa = new GeoRegion();
        usa.setId(USA_REGION_ID);
        usa.setType(RegionType.CITY);

        when(geoRegionService.getParentRegions(BOGDANOVICH_REGION_ID)).thenReturn(Lists.newArrayList(bogdanovich, russia));
        when(geoRegionService.getParentRegions(MONTGOMERY_REGION_ID)).thenReturn(Lists.newArrayList(montgomery, usa));

        regionalInfoResolverRegistry = new RegionalInfoResolverRegistry(
            mobileRegionalInfoResolver,
            partnerRegionalInfoResolver,
            clientHelper
        );

        mockClientHelper = new MockClientHelper(clientHelper);

    }

    @After
    public void tearDown() {
        ContextHolderTestHelper.destroyContext();
    }

    /**
     * Проверяем, что проставляем регион по remote_ip
     */
    @Test
    public void shouldDefineRegionByRemoteIpParameter() {
        HttpServletRequest request = MockRequestBuilder.start()
            .param(PartnerRegionalInfoResolver.REMOTE_IP_PARAM_NAME, "2.12.85.06")
            .build();
        HttpServletResponse response = mock(HttpServletResponse.class);
        Context context = MockContextBuilder.start().externalClient().build();

        when(geoRegionService.getInfoOrNull(2)).thenReturn(new GeoRegion());

        RegionalInfoResolver resolver = regionalInfoResolverRegistry.get(context);
        Futures.wait(resolver.initRegionInfo(request, response, context));

        verify(context.getRegionInfo()).setRawRegionId(eq(2));
    }

    /**
     * Проверяем, что проставляем регион по geo_id
     */
    @Test
    public void shouldDefineRegionByGeoIdParam() {
        HttpServletRequest request = MockRequestBuilder.start()
            .param(CommonParameters.REGION_PARAM_NAME, "42")
            .build();
        HttpServletResponse response = mock(HttpServletResponse.class);
        Context context = MockContextBuilder.start().externalClient().build();

        when(geoRegionService.getInfoOrNull(42)).thenReturn(new GeoRegion());

        RegionalInfoResolver resolver = regionalInfoResolverRegistry.get(context);
        Futures.wait(resolver.initRegionInfo(request, response, context));

        verify(context.getRegionInfo()).setRawRegionId(eq(42));
    }

    /**
     * Проверяем, что возвращаем ошибку, если не смогли определить регион по remoteIp
     */
    @Test
    public void shouldErrorIfRemoteIpIsUnknown() {
        HttpServletRequest request = MockRequestBuilder.start()
            .param(PartnerRegionalInfoResolver.REMOTE_IP_PARAM_NAME, "0.0.0.0")
            .build();
        HttpServletResponse response = mock(HttpServletResponse.class);
        Context context = MockContextBuilder.start().externalClient().build();

        when(geobaseService.getRegionIdByIp(anyString()))
            .thenReturn(Futures.newFailedFuture(new MockitoException("Test")));

        RegionalInfoResolver resolver = regionalInfoResolverRegistry.get(context);
        Future<Void> future = resolver.initRegionInfo(request, response, context);

        Assert.assertTrue("должны сделать fallback на Москву", future.isSuccess());
    }

    /**
     * Проверяем, что возвращаем warning о том, что регион не найден для мобильного клиента
     */
    @Test
    public void shouldAddWarningOnWrongRegionForMobile() {
        HttpServletRequest request = MockRequestBuilder.start()
            .param(CommonParameters.REGION_PARAM_NAME, "312")
            .build();
        HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
        Context context = MockContextBuilder.start().mobileClient().build();

        when(geoRegionService.getInfo(anyInt())).thenThrow(new NotFoundException("Test"));

        RegionalInfoResolver resolver = regionalInfoResolverRegistry.get(context);
        Futures.wait(resolver.initRegionInfo(request, response, context));

        verify(context).addWarning(new ApiError(
            RegionErrorType.FALLBACK_REGION,
            RegionErrorCode.REGION_NOT_FOUND,
            "Region '312' not found"
        ));
    }

    /**
     * Проверяем, что НЕ возвращаем warning о том, что регион не найден для немобильного клиента
     */
    @Test
    public void shouldNotAddWarningOnWrongRegionForNotMobile() {
        HttpServletRequest request = MockRequestBuilder.start()
            .param(CommonParameters.REGION_PARAM_NAME, "312")
            .build();
        HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
        Context context = MockContextBuilder.start().externalClient().build();

        when(geoRegionService.getInfo(anyInt())).thenThrow(new NotFoundException("Test"));

        RegionalInfoResolver resolver = regionalInfoResolverRegistry.get(context);
        Futures.wait(resolver.initRegionInfo(request, response, context));

        verify(context, times(0)).addWarning(new ApiError(
            RegionErrorType.FALLBACK_REGION,
            RegionErrorCode.REGION_NOT_FOUND,
            "Region '312' not found"
        ));
    }

    /**
     * Проверяем, что определяем регион по координатам
     */
    @Test
    public void shouldResolveRegionIdByCoords() {
        final int regionId = BOGDANOVICH_REGION_ID;
        HttpServletRequest request = MockRequestBuilder.start()
            .param(ParametersV2.LATITUDE_PARAM_NAME, "60")
            .param(ParametersV2.LONGITUDE_PARAM_NAME, "90")
            .build();
        HttpServletResponse response = mock(HttpServletResponse.class);
        Context context = MockContextBuilder.start().mobileClient().build();

        when(geobaseService.getRegion(argThat(new GeoCoordinatesMatcher(60, 90))))
            .thenReturn(Futures.newSucceededFuture(Result.newResult(regionId)));
        when(geoRegionService.getInfoOrNull(regionId))
            .thenReturn(new GeoRegion());

        when(geoRegionService.toCity(anyInt(), anyInt())).thenReturn(regionId);
        when(geoRegionService.toCountry(anyInt(), anyInt())).thenReturn(GeoUtils.Country.RUSSIA);

        RegionalInfoResolver resolver = regionalInfoResolverRegistry.get(context);
        Futures.wait(resolver.initRegionInfo(request, response, context));

        verify(context.getRegionInfo()).setRawRegionId(regionId);
        verify(context, never()).addWarning(any(ApiError.class));
    }

    /**
     * Проверяем, что определяем регион по координатам
     */
    @Test
    public void shouldResolveRegionIdByIp() {
        final int regionId = BOGDANOVICH_REGION_ID;
        HttpServletRequest request = MockRequestBuilder.start().build();
        HttpServletResponse response = mock(HttpServletResponse.class);
        Context context = MockContextBuilder.start().mobileClient().userIp("1.1.1.1").build();

        when(geoRegionService.getInfoOrNull(regionId)).thenReturn(new GeoRegion());
        when(geobaseService.getRegionIdByIp("1.1.1.1")).thenReturn(Futures.newSucceededFuture(Result.newResult(regionId)));

        when(geoRegionService.toCity(anyInt(), anyInt())).thenReturn(regionId);
        when(geoRegionService.toCountry(anyInt(), anyInt())).thenReturn(GeoUtils.Country.RUSSIA);

        RegionalInfoResolver resolver = regionalInfoResolverRegistry.get(context);
        Futures.wait(resolver.initRegionInfo(request, response, context));

        verify(context.getRegionInfo()).setRawRegionId(BOGDANOVICH_REGION_ID);
        verify(geobaseService, never()).getRegion(any());
    }

    /**
     * Проверяем, что если в заросе есть и координаты и geo_id, что geo_id предпочтительный
     */
    @Test
    public void shouldResolveRegionByGeoIdParam() {
        HttpServletRequest request = MockRequestBuilder.start()
            .param(ParametersV2.LATITUDE_PARAM_NAME, "60")
            .param(ParametersV2.LONGITUDE_PARAM_NAME, "90")
            .param(CommonParameters.REGION_PARAM_NAME, BOGDANOVICH_REGION_ID)
            .build();
        HttpServletResponse response = mock(HttpServletResponse.class);
        Context context = MockContextBuilder.start().mobileClient().build();

        when(geobaseService.getRegion(argThat(new GeoCoordinatesMatcher(60, 90))))
            .thenReturn(Futures.newSucceededFuture(Result.newResult(BOGDANOVICH_REGION_ID)));
        when(geoRegionService.getInfoOrNull(eq(BOGDANOVICH_REGION_ID)))
            .thenReturn(new GeoRegion());
        when(geoRegionService.toCountry(eq(BOGDANOVICH_REGION_ID), anyInt())).thenReturn(GeoUtils.Country.RUSSIA);

        RegionalInfoResolver resolver = regionalInfoResolverRegistry.get(context);
        Futures.wait(resolver.initRegionInfo(request, response, context));

        verify(context.getRegionInfo()).setRawRegionId(BOGDANOVICH_REGION_ID);
        verify(geobaseService, never()).getRegion(any());
    }

    /**
     * Проверяем, что если определеили регион за пределаи КУБР, то делаем фоллбек на Москву
     */
    @Test
    public void shouldResolveFallbackRegionUsUnsupportedCountry() {
        HttpServletRequest request = MockRequestBuilder.start()
            .param(CommonParameters.REGION_PARAM_NAME, MONTGOMERY_REGION_ID)
            .build();
        HttpServletResponse response = mock(HttpServletResponse.class);
        Context context = MockContextBuilder.start().build();

        mockClientHelper.is(ClientHelper.Type.BIXBY, true);

        when(geoRegionService.getInfoOrNull(eq(MONTGOMERY_REGION_ID)))
            .thenReturn(new GeoRegion());

        RegionalInfoResolver resolver = regionalInfoResolverRegistry.get(context);
        Futures.wait(resolver.initRegionInfo(request, response, context));

        verify(context.getRegionInfo()).setRawRegionId(MOSCOW_REGION_ID);
        verify(context).addWarning(new ApiError(
            RegionErrorType.FALLBACK_REGION,
            RegionErrorCode.UNSUPPORTED_COUNTRY,
            String.format("Region '%d' is from unsupported country", MONTGOMERY_REGION_ID)
        ));
    }

    /**
     * Проверяем, что возвращаем неопределенный регион, если никаких параметров для определения региона не было передано
     */
    @Test
    public void shouldReturnUndefinedRegionIfNoRegionWasPassed() {
        HttpServletRequest request = MockRequestBuilder.start().build();
        HttpServletResponse response = mock(HttpServletResponse.class);
        Context context = MockContextBuilder.start().externalClient().build();

        RegionalInfoResolver resolver = regionalInfoResolverRegistry.get(context);
        Futures.wait(resolver.initRegionInfo(request, response, context));

        verify(context.getRegionInfo()).setRawRegionId(GeoUtils.UNDEFINED_REGION);
    }

    /**
     * Проверяем, что возвращаем fallback регион для мобильного клиента, даже если никаких параметров для определения региона не было передано
     */
    @Test
    public void shouldReturnFallbackRegionIfNoRegionWasPassedForMobileClient() {
        HttpServletRequest request = MockRequestBuilder.start().build();
        HttpServletResponse response = mock(HttpServletResponse.class);
        Context context = MockContextBuilder.start().mobileClient().build();

        when(geoRegionService.toCity(anyInt(), anyInt())).thenReturn(FALLBACK_REGION);

        RegionalInfoResolver resolver = regionalInfoResolverRegistry.get(context);
        Futures.wait(resolver.initRegionInfo(request, response, context));

        verify(context.getRegionInfo()).setRawRegionId(FALLBACK_REGION);
    }

    /**
     * Проверяем, что вернем регион Москва, если сервис Lbs не отвечает
     */
    @Test
    public void shouldReturnMoscowOnLbsError() {
        HttpServletRequest request = MockRequestBuilder.start()
            .param("wifinetworks", "DEADDEADBEAF:-100")
            .build();
        HttpServletResponse response = mock(HttpServletResponse.class);
        Context context = MockContextBuilder.start().mobileClient().build();

        when(lbsService.locate(any(SignalsInfo.class), any(HttpServletRequest.class))).thenReturn(Futures.newFailedFuture(new MockitoException("Test")));

        RegionalInfoResolver resolver = regionalInfoResolverRegistry.get(context);
        Futures.wait(resolver.initRegionInfo(request, response, context));

        verify(context.getRegionInfo()).setRawRegionId(FALLBACK_REGION);
    }

    /**
     * Проверяем, что если в параметре geo_id передано значение 0 (все регионы), то корректно отрабатываем этот случай
     */
    @Test
    public void shouldAcceptAsAllRegions() {
        HttpServletRequest request = MockRequestBuilder.start()
            .param("geo_id", String.valueOf(GeoUtils.ALL_REGIONS))
            .build();

        HttpServletResponse response = mock(HttpServletResponse.class);
        Context context = MockContextBuilder.start().mobileClient().build();

        RegionalInfoResolver resolver = regionalInfoResolverRegistry.get(context);
        Futures.wait(resolver.initRegionInfo(request, response, context));

        verify(context.getRegionInfo()).setRawRegionId(GeoUtils.ALL_REGIONS);
        verify(context.getValidationErrors(), never()).add(anyString());
    }

    /**
     * Проверяем, что используем параметр geo_id вместо remote_ip для партнеров, если геобаза не ответила
     */
    @Test
    public void shouldUseGeoIdParameterForPartnerIfGeobaseIsFailed() {
        int regionId = 157;
        HttpServletRequest request = MockRequestBuilder.start()
            .param("geo_id", regionId)
            .param("remote_ip", "0.0.0.0")
            .build();

        HttpServletResponse response = mock(HttpServletResponse.class);
        Context context = MockContextBuilder.start().externalClient().build();

        when(geobaseService.getRegionIdByIp(anyString()))
            .thenReturn(Futures.newSucceededFuture(
                Result.newError(
                    new ApiError(
                        RegionErrorType.FALLBACK_REGION,
                        RegionErrorCode.ERROR_RETRIEVING_DATA,
                        "Error retrieving region data"
                    ))));
        when(geoRegionService.getInfoOrNull(regionId))
            .thenReturn(new GeoRegion());

        RegionalInfoResolver resolver = regionalInfoResolverRegistry.get(context);
        Future<Void> initRegionId = resolver.initRegionInfo(request, response, context);
        Futures.wait(initRegionId);

        Assert.assertTrue(initRegionId.isSuccess());
        verify(context.getRegionInfo()).setRawRegionId(regionId);
        verify(context.getValidationErrors(), never()).add(anyString());
    }

    /**
     * Проверяем, что передаем ошибку, если геобаза не ответила и geo_id не был передан
     */
    @Test
    public void shouldFailForPartnerIfGeobaseIsFailedAndGeoIdIsNotPresent() {
        HttpServletRequest request = MockRequestBuilder.start()
            .param("remote_ip", "0.0.0.0")
            .build();

        HttpServletResponse response = mock(HttpServletResponse.class);
        Context context = MockContextBuilder.start().externalClient().build();

        when(geobaseService.getRegionIdByIp(anyString()))
            .thenReturn(Futures.newFailedFuture(new MockitoException("Database failed test")));

        RegionalInfoResolver resolver = regionalInfoResolverRegistry.get(context);
        Future<Void> initRegionId = resolver.initRegionInfo(request, response, context);
        Futures.wait(initRegionId);

        Assert.assertTrue("Должны сделать fallback на Москву", initRegionId.isSuccess());
    }

    @Test
    public void testFallbackToMoskowIfGeobaseFailsToDetectRegion() {
        HttpServletRequest request = MockRequestBuilder.start()
            .param(ParametersV2.LATITUDE_PARAM_NAME, "60")
            .param(ParametersV2.LONGITUDE_PARAM_NAME, "90")
            .param(CommonParameters.REGION_PARAM_NAME, "4294967294") // Больше MAX_INT
            .build();

        HttpServletResponse response = mock(HttpServletResponse.class);
        Context context = MockContextBuilder.start().mobileClient().build();

        when(geobaseService.getRegion(argThat(new GeoCoordinatesMatcher(60, 90))))
            .thenReturn(Futures.newSucceededFuture(Result.newResult(-1)));

        when(geoRegionService.toCity(anyInt(), anyInt())).thenReturn(MOSCOW_REGION_ID);
        when(geoRegionService.toCountry(anyInt(), anyInt())).thenReturn(GeoUtils.Country.RUSSIA);

        RegionalInfoResolver resolver = regionalInfoResolverRegistry.get(context);
        Futures.wait(resolver.initRegionInfo(request, response, context));

        verify(context.getRegionInfo()).setRawRegionId(MOSCOW_REGION_ID);
        verify(context.getRegionInfo(), times(1)).setIpRegionId(any());
    }

    /**
     * В случае проблем с данным, геобаза может вернуть скрытый регион. Если среди его родителей нет города, то мы не
     * можем определить необходимый регион.
     *
     * @see <a href="https://st.yandex-team.ru/MARKETAPI-3423">
     * MARKETAPI-3423: Возвращать регион по умолчанию, если геобаза вернула скрытый регион
     * </a>
     */
    @Test
    public void shouldReturnDefaultAndWarningForHiddenRegion() {
        final int regionId = 100;

        HttpServletRequest request = MockRequestBuilder.start()
            .param(CommonParameters.REGION_PARAM_NAME, "100")
            .build();
        HttpServletResponse response = mock(HttpServletResponse.class);
        Context context = MockContextBuilder.start().mobileClient().build();

        when(geoRegionService.getInfo(eq(regionId)))
            .thenReturn(new GeoRegion(regionId, "Test hidden", RegionType.HIDDEN, null, null));

        RegionalInfoResolver resolver = regionalInfoResolverRegistry.get(context);
        Futures.wait(resolver.initRegionInfo(request, response, context));

        verify(context.getRegionInfo()).setRawRegionId(MOSCOW_REGION_ID);
        verify(context).addWarning(new ApiError(
            RegionErrorType.FALLBACK_REGION,
            RegionErrorCode.REGION_NOT_FOUND,
            "Region '100' not found"
        ));
    }

    @Test
    public void shouldMobileResolveGeoRegionalInExp() {
        final int regionId = 100;
        final Integer ipRegionId = 50;
        String ip = "1.2.3.4";

        HttpServletRequest request = MockRequestBuilder.start()
                .param(CommonParameters.REGION_PARAM_NAME, "100")
                .build();
        HttpServletResponse response = mock(HttpServletResponse.class);

        Context context = initWithType(CommonClient.Type.MOBILE);
        context.setUserIp(ip);

        when(geoRegionService.getInfoOrNull(eq(regionId)))
                .thenReturn(new GeoRegion(regionId, "Test country", RegionType.COUNTRY, null, null));
        when(geobaseService.getRegionIdByIp(eq(ip)))
                .thenReturn(Futures.newSucceededFuture(Result.newResult(ipRegionId)));

        RegionalInfoResolver resolver = regionalInfoResolverRegistry.get(context);
        Futures.wait(resolver.initRegionInfo(request, response, context));

        Assert.assertEquals(context.getRegionInfo().getRawRegionId(), regionId);
        Assert.assertEquals(context.getRegionInfo().getIpRegionId(), ipRegionId);
    }

    public Context initWithType(CommonClient.Type type) {
        Context context = new Context("abc");
        Client client = new Client();
        client.setType(type);
        context.setClient(client);
        return context;
    }
}
