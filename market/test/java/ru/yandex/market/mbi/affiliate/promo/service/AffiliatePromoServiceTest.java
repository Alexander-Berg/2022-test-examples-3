package ru.yandex.market.mbi.affiliate.promo.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import Market.DataCamp.DataCampPromo;
import Market.DataCamp.SyncAPI.SyncGetPromo;
import NMarketIndexer.Common.Common;
import okhttp3.MediaType;
import okhttp3.ResponseBody;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.support.TransactionTemplate;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.loyalty.api.exception.MarketLoyaltyException;
import ru.yandex.market.loyalty.api.model.MarketLoyaltyErrorCode;
import ru.yandex.market.loyalty.api.model.promocode.PromocodeCheckResponse;
import ru.yandex.market.loyalty.client.MarketLoyaltyClient;
import ru.yandex.market.mbi.affiliate.promo.TestResourceUtils;
import ru.yandex.market.mbi.affiliate.promo.api.server.dto.CreatePromoDescriptionRequestDto;
import ru.yandex.market.mbi.affiliate.promo.api.server.dto.DeviceType;
import ru.yandex.market.mbi.affiliate.promo.api.server.dto.Discount;
import ru.yandex.market.mbi.affiliate.promo.api.server.dto.DiscountType;
import ru.yandex.market.mbi.affiliate.promo.api.server.dto.OfferMatching;
import ru.yandex.market.mbi.affiliate.promo.api.server.dto.PartnerPromoDto;
import ru.yandex.market.mbi.affiliate.promo.api.server.dto.PromoDescriptionDetailedDto;
import ru.yandex.market.mbi.affiliate.promo.api.server.dto.PromoDescriptionDto;
import ru.yandex.market.mbi.affiliate.promo.api.server.dto.PromoDescriptionStatus;
import ru.yandex.market.mbi.affiliate.promo.api.server.dto.PromoDescriptionWithPromocodes;
import ru.yandex.market.mbi.affiliate.promo.api.server.dto.PromoDescriptionsResponseDto;
import ru.yandex.market.mbi.affiliate.promo.api.server.dto.PromoListResponseDto;
import ru.yandex.market.mbi.affiliate.promo.api.server.dto.PromoOffers;
import ru.yandex.market.mbi.affiliate.promo.common.AffiliatePromoException;
import ru.yandex.market.mbi.affiliate.promo.common.AffiliatePromoUtils;
import ru.yandex.market.mbi.affiliate.promo.common.ErrorCode;
import ru.yandex.market.mbi.affiliate.promo.config.FunctionalTestConfig;
import ru.yandex.market.mbi.affiliate.promo.dao.PromoDao;
import ru.yandex.market.mbi.affiliate.promo.dao.VarsDao;
import ru.yandex.market.mbi.affiliate.promo.model.Partner;
import ru.yandex.market.mbi.affiliate.promo.model.PromoDescription;
import ru.yandex.market.mbi.affiliate.promo.model.PromoDescriptionBudgetData;
import ru.yandex.market.mbi.affiliate.promo.model.Promocode;
import ru.yandex.market.mbi.affiliate.promo.regions.RegionService;
import ru.yandex.market.mbi.affiliate.promo.stroller.DataCampStrollerClient;
import ru.yandex.market.mbi.affiliate.promo.stroller.StrollerJsonApi;
import ru.yandex.market.mbi.affiliate.promo.stroller.StrollerProtoApi;
import ru.yandex.market.mbi.affiliate.promo.stroller.StrollerProtoHelper;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.emptyIterable;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.iterableWithSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = FunctionalTestConfig.class)
public class AffiliatePromoServiceTest {
    private static final long ID = 44L;
    private static final long CLID = 3333L;
    private static final String STRING_ID = AffiliatePromoUtils.toStringParentId(ID);

    @Autowired
    private DataCampStrollerClient strollerClient;
    @Autowired
    private StrollerJsonApi strollerJsonApi;
    @Autowired
    private StrollerProtoApi strollerProtoApi;

    @Autowired
    private PromoDao promoDao;
    private PromoDao promoDaoSpy;

    private VarsDao varsDaoMock;

    @Autowired
    private TransactionTemplate transactionTemplate;

    private PartnerService partnerServiceMock;
    private MarketLoyaltyClient loyaltyClientMock;
    private RegionService regionServiceMock;

    @Autowired
    private Clock clock;

    private AffiliatePromoService service;

    @Before
    public void init() {
        partnerServiceMock = mock(PartnerService.class);
        loyaltyClientMock = mock(MarketLoyaltyClient.class);
        varsDaoMock = mock(VarsDao.class);
        regionServiceMock = mock(RegionService.class);
        promoDaoSpy = Mockito.spy(promoDao);
        service = new AffiliatePromoService(
              strollerClient, promoDaoSpy, transactionTemplate, partnerServiceMock,
                new LoyaltyClientWrapper(loyaltyClientMock),
                clock,
                varsDaoMock, new OfferMatchingRuleMapper(regionServiceMock), 2);

        when(partnerServiceMock.getUserLoginByClid(CLID)).thenReturn("user_login_1");
        when(varsDaoMock.getVar(AffiliatePromoService.VAR_TIMESTAMP_END_DATE_UPDATE))
                .thenReturn(String.valueOf(
                        LocalDateTime.of(2022, Month.JUNE, 6, 12, 0, 0)
                            .toInstant(ZoneOffset.UTC)
                                .getEpochSecond()));
    }

    @After
    public void after() {
        Mockito.reset(promoDaoSpy, strollerJsonApi, strollerProtoApi);
    }

    @Test
    public void testCreateDescription() throws Exception {
        var expectedRequestProto = DataCampPromo.PromoDescription.newBuilder();
        TestResourceUtils.loadProto("ru/yandex/market/mbi/affiliate/promo/service/create_description_proto.json", expectedRequestProto);

        var dto = TestResourceUtils.loadDto(
                "ru/yandex/market/mbi/affiliate/promo/service/create_description_dto.json",
                CreatePromoDescriptionRequestDto.class);

        when(strollerJsonApi.addPromo(eq(expectedRequestProto.build())).execute())
                .thenReturn(Response.success(expectedRequestProto.build()));

        // Подменяем ответ, чтобы иметь фиксированное значение id, а настоящий сохраняем,
        // чтоб потом проверить, что он есть в базе
        AtomicLong realInsertedIdRef = mockDaoInsertResponse(ID);

        var result = service.createPromoDescription(dto);

        assertThat(result.getPromoDescriptionId(), is(STRING_ID));

        var inserted = promoDaoSpy.findDescriptionById(realInsertedIdRef.get());
        assertThat(inserted, notNullValue());
        assertThat(inserted.isMayContainExcludedOffers(), is(false));
        assertThat(inserted.isOneOrderPromocode(), is(false));
        assertThat(inserted.isFirstMarketOrderPromocode(), is(false));
        assertThat(inserted.getDeviceType(), nullValue());
        assertThat(inserted.getPartnerGroups(), is(List.of("vip1", "vip2")));
        assertThat(inserted.getManagerEmail(), is("yuliachr@yandex-team.ru"));
        assertThat(inserted.getBudgetNotificationThreshold(), is(5));
        assertThat(inserted.isGeneratedPromoParent(), is(false));
        verify(strollerJsonApi).addPromo(eq(expectedRequestProto.build()));
        verify(strollerJsonApi).addPromo(eq(expectedRequestProto.build()));
    }

    @Test
    public void testCreateDescriptionWithFlags() throws Exception {
        var expectedRequestProto = DataCampPromo.PromoDescription.newBuilder();
        TestResourceUtils.loadProto("ru/yandex/market/mbi/affiliate/promo/service/create_description_proto.json", expectedRequestProto);

        var dto = TestResourceUtils.loadDto(
                "ru/yandex/market/mbi/affiliate/promo/service/create_description_with_flags_dto.json",
                CreatePromoDescriptionRequestDto.class);

        when(strollerJsonApi.addPromo(eq(expectedRequestProto.build())).execute())
                .thenReturn(Response.success(expectedRequestProto.build()));

        // Подменяем ответ, чтобы иметь фиксированное значение id, а настоящий сохраняем,
        // чтоб потом проверить, что он есть в базе
        AtomicLong realInsertedIdRef = mockDaoInsertResponse(ID);

        var result = service.createPromoDescription(dto);

        assertThat(result.getPromoDescriptionId(), is(STRING_ID));

        var inserted = promoDaoSpy.findDescriptionById(realInsertedIdRef.get());
        assertThat(inserted, notNullValue());
        assertThat(inserted.isMayContainExcludedOffers(), is(true));
        assertThat(inserted.isOneOrderPromocode(), is(false));
        assertThat(inserted.isFirstMarketOrderPromocode(), is(true));
        assertThat(inserted.getDeviceType(), is(DeviceType.APPLICATION));
        verify(strollerJsonApi).addPromo(eq(expectedRequestProto.build()));
    }

    @Test
    public void testCreateDescriptionEmptyMatching() throws Exception {
        var dto = TestResourceUtils.loadDto(
                "ru/yandex/market/mbi/affiliate/promo/service/create_description_empty_dto.json",
                CreatePromoDescriptionRequestDto.class);

        when(strollerJsonApi.addPromo(any()).execute())
                .thenReturn(Response.success(null));

        var result = service.createPromoDescription(dto);
        assertThat(result, notNullValue());
    }

    @Test
    public void testCreateWithExcludeRestrictions() throws Exception {
        var expectedRequestProto = DataCampPromo.PromoDescription.newBuilder();
        TestResourceUtils.loadProto("ru/yandex/market/mbi/affiliate/promo/service/create_description_with_exclude_proto.json", expectedRequestProto);

        var dto = TestResourceUtils.loadDto(
                "ru/yandex/market/mbi/affiliate/promo/service/create_description_with_exclude_dto.json",
                CreatePromoDescriptionRequestDto.class);

        when(strollerJsonApi.addPromo(eq(expectedRequestProto.build())).execute())
                .thenReturn(Response.success(expectedRequestProto.build()));


        AtomicLong realInsertedIdRef = mockDaoInsertResponse(ID);

        var result = service.createPromoDescription(dto);

        verify(strollerJsonApi).addPromo(eq(expectedRequestProto.build()));

        assertThat(result.getPromoDescriptionId(), is(STRING_ID));

        var inserted = promoDaoSpy.findDescriptionById(realInsertedIdRef.get());
        assertThat(inserted, notNullValue());
        assertThat(inserted.isMayContainExcludedOffers(), is(true));
    }

    @Test
    public void testUpdateBudget() throws Exception {
        var expectedFindRequestProto = SyncGetPromo.GetPromoBatchRequest.newBuilder();
        TestResourceUtils.loadProto("ru/yandex/market/mbi/affiliate/promo/service/update_request_proto.json", expectedFindRequestProto);
        var expectedFindResponse = SyncGetPromo.GetPromoBatchResponse.newBuilder();
        TestResourceUtils.loadProto("ru/yandex/market/mbi/affiliate/promo/service/update_find_response.json", expectedFindResponse);
        var expectedRequestProto = DataCampPromo.PromoDescription.newBuilder();
        TestResourceUtils.loadProto("ru/yandex/market/mbi/affiliate/promo/service/update_budget_proto.json", expectedRequestProto);
        var expectedResponse = DataCampPromo.PromoDescription.newBuilder();
        TestResourceUtils.loadProto("ru/yandex/market/mbi/affiliate/promo/service/update_budget_response.json", expectedResponse);

        when(strollerProtoApi.getPromosByIds(eq(expectedFindRequestProto.build())).execute())
                .thenReturn(Response.success(expectedFindResponse.build()));
        when(strollerJsonApi.addPromo(eq(expectedRequestProto.build())).execute())
                .thenReturn(Response.success(expectedResponse.build()));
        service.updatePromoDescription(STRING_ID, 2000000, null, null, null);

        verify(strollerProtoApi).getPromosByIds(eq(expectedFindRequestProto.build()));
        verify(strollerJsonApi).addPromo(eq(expectedRequestProto.build()));

        verify(promoDaoSpy, never()).updateStatus(any(), any());
    }

    @Test
    public void testUpdateNotFound() throws Exception {
        var expectedFindRequestProto = SyncGetPromo.GetPromoBatchRequest.newBuilder();
        TestResourceUtils.loadProto("ru/yandex/market/mbi/affiliate/promo/service/update_request_proto.json", expectedFindRequestProto);
        when(strollerProtoApi.getPromosByIds(eq(expectedFindRequestProto.build())).execute())
                .thenReturn(Response.success(null));
        var e = assertThrows(AffiliatePromoException.class,
                () -> service.updatePromoDescription(STRING_ID, null, PromoDescriptionStatus.ACTIVE, null, null)
        );
        assertThat(e.getErrorCode(), is(ErrorCode.PROMO_DESCRIPTION_NOT_FOUND));
        verify(strollerProtoApi).getPromosByIds(eq(expectedFindRequestProto.build()));
    }

    @Test
    public void testEnablePromoDescription() throws Exception {
        var expectedFindRequestProto = SyncGetPromo.GetPromoBatchRequest.newBuilder();
        TestResourceUtils.loadProto("ru/yandex/market/mbi/affiliate/promo/service/update_request_proto.json", expectedFindRequestProto);
        var expectedFindResponse = SyncGetPromo.GetPromoBatchResponse.newBuilder();
        TestResourceUtils.loadProto("ru/yandex/market/mbi/affiliate/promo/service/update_find_response.json", expectedFindResponse);
        when(strollerProtoApi.getPromosByIds(eq(expectedFindRequestProto.build())).execute())
                .thenReturn(Response.success(expectedFindResponse.build()));

        service.updatePromoDescription(STRING_ID, null, PromoDescriptionStatus.ACTIVE, null, null);

        verify(promoDaoSpy).updateStatus(List.of(ID), PromoDescriptionStatus.ACTIVE);
        verify(strollerProtoApi).getPromosByIds(eq(expectedFindRequestProto.build()));
    }

    @Test
    public void testListAvailablePromoDescriptions() throws Exception {
        var expectedResponse = SyncGetPromo.GetPromoBatchResponse.newBuilder();
        TestResourceUtils.loadProto("ru/yandex/market/mbi/affiliate/promo/service/list_available_response.json", expectedResponse);

        when(strollerProtoApi.getPromos(eq("v1/partners/"
                + StrollerProtoHelper.PARENT_PROMO_BUSINESS_ID + "/promos"
                + "?businessId=" + StrollerProtoHelper.PARENT_PROMO_BUSINESS_ID
                + "&only_unfinished=true"
                + "&type=" + DataCampPromo.PromoType.PARENT_PROMO_VALUE)).execute())
                .thenReturn(Response.success(expectedResponse.build()));

        when(promoDaoSpy.findPromoDescriptionsByIdsAndStatuses(
                List.of(1000L, 1012L, 1888L, 1002L),
                List.of(PromoDescriptionStatus.ACTIVE, PromoDescriptionStatus.BROKEN), null))
        .thenReturn(
                Map.of(1002L, PromoDescription.builder()
                        .withId(1002)
                        .withPromoOffers(new PromoOffers()
                                        .url("https://market.yandex.ru/catalog--televizory/18040671")
                                        .description("Телевизоры")
                                        .matchingInclude(new OfferMatching()
                                                .categoryList(List.of(18040671L))))
                        .withDiscount(new Discount().discountType(DiscountType.PERCENT).discountValue(7)
                                .bucketMinPrice(20000)
                                .bucketMaxPrice(2000000)
                        )
                        .withStatus(PromoDescriptionStatus.ACTIVE)
                        .withMayContainExcludedOffers(false)
                        .build(),
                1000L, PromoDescription.builder()
                        .withId(1000L)
                        .withPromoOffers(new PromoOffers()
                                        .url("https://market.yandex.ru/")
                                        .description("На всё")
                        )
                        .withDiscount(new Discount().discountType(DiscountType.PERCENT).discountValue(1))
                        .withStatus(PromoDescriptionStatus.BROKEN)
                    .build(),
                1888L, PromoDescription.builder()
                         .withId(1888L)
                         .withPromoOffers(new PromoOffers()
                                        .url("https://market.yandex.ru/")
                                        .description("На всё")
                         )
                         .withDiscount(new Discount().discountType(DiscountType.PERCENT).discountValue(1))
                         .withStatus(PromoDescriptionStatus.ACTIVE)
                         .withGeneratedPromoParent(true)
                         .build()
                ));

        var response = service.listAvailablePromoDescriptions("aaa", null, null, null);
        var expectedDto = TestResourceUtils.loadDto(
                "ru/yandex/market/mbi/affiliate/promo/service/list_available_result_dto.json",
                PromoDescriptionsResponseDto.class);

        assertThat(response, iterableWithSize(2));
        Optional<PromoDescriptionDto> result1002 =
                response.stream().filter(d -> d.getPromoDescriptionId().equals("aff_parent_1002"))
                        .findFirst();
        assertThat(result1002.isPresent(), is(true));
        assertThat(result1002.get(), is(expectedDto.getPromoDescriptions().get(0)));

        verify(promoDaoSpy).updateStatus(List.of(1000L), PromoDescriptionStatus.ACTIVE);
        verify(strollerProtoApi).getPromos(anyString());
    }

    @Test
    public void testListAvailableEmptyResponse() throws Exception {
        when(strollerProtoApi.getPromos(eq("v1/partners/"
                + StrollerProtoHelper.PARENT_PROMO_BUSINESS_ID + "/promos"
                + "?businessId=" + StrollerProtoHelper.PARENT_PROMO_BUSINESS_ID
                + "&only_unfinished=true"
                + "&type=" + DataCampPromo.PromoType.PARENT_PROMO_VALUE)).execute())
                .thenReturn(Response.success(SyncGetPromo.GetPromoBatchResponse.newBuilder().build()));

        var response = service.listAvailablePromoDescriptions("aaa", null, null, null);
        assertThat(response, emptyIterable());
        verify(strollerProtoApi).getPromos(anyString());
    }

    @Test
    public void testListAvailableAllowInactive() throws Exception {
        var expectedResponse = SyncGetPromo.GetPromoBatchResponse.newBuilder();
        TestResourceUtils.loadProto("ru/yandex/market/mbi/affiliate/promo/service/list_available_response.json", expectedResponse);

        when(strollerProtoApi.getPromos(eq("v1/partners/"
                + StrollerProtoHelper.PARENT_PROMO_BUSINESS_ID + "/promos"
                + "?businessId=" + StrollerProtoHelper.PARENT_PROMO_BUSINESS_ID
                + "&only_unfinished=true"
                + "&type=" + DataCampPromo.PromoType.PARENT_PROMO_VALUE)).execute())
                .thenReturn(Response.success(expectedResponse.build()));

        when(promoDaoSpy.findPromoDescriptionsByIdsAndStatuses(
                List.of(1000L, 1012L, 1888L, 1002L, 1003L),
                List.of(PromoDescriptionStatus.ACTIVE, PromoDescriptionStatus.INACTIVE,
                        PromoDescriptionStatus.BROKEN, PromoDescriptionStatus.BROKEN_INACTIVE), null))
                .thenReturn(Map.of(1002L, PromoDescription.builder()
                                .withId(1002)
                                .withPromoOffers(new PromoOffers()
                                                .url("https://market.yandex.ru/catalog--televizory/18040671")
                                                .description("Телевизоры")
                                                .matchingInclude(new OfferMatching()
                                                        .categoryList(List.of(18040671L))))
                                .withDiscount(new Discount().discountType(DiscountType.PERCENT).discountValue(7)
                                        .bucketMinPrice(20000)
                                        .bucketMaxPrice(2000000)
                                )
                                .withStatus(PromoDescriptionStatus.INACTIVE)
                                .withMayContainExcludedOffers(false)
                                .build(),
                        1000L, PromoDescription.builder()
                                .withId(1000L)
                                .withPromoOffers(new PromoOffers()
                                        .url("https://market.yandex.ru/")
                                        .description("На всё")
                                )
                                .withDiscount(new Discount().discountType(DiscountType.PERCENT).discountValue(1))
                                .withStatus(PromoDescriptionStatus.BROKEN_INACTIVE)
                                .build(),
                        1888L, PromoDescription.builder()
                                .withId(1888L)
                                .withPromoOffers(new PromoOffers()
                                        .url("https://market.yandex.ru/")
                                        .description("На всё")
                                )
                                .withDiscount(new Discount().discountType(DiscountType.PERCENT).discountValue(1))
                                .withStatus(PromoDescriptionStatus.ACTIVE)
                                .withGeneratedPromoParent(true)
                                .build()));
        var response = service.listAvailablePromoDescriptions("distr-test-selt-1", null, null, null);
        assertThat(response, iterableWithSize(3));
        verify(strollerProtoApi).getPromos(anyString());
        verify(promoDaoSpy).findPromoDescriptionsByIds(any());
    }

    @Test
    public void testStrollerFailureOnCreate() throws Exception {
        var dto = TestResourceUtils.loadDto(
                "ru/yandex/market/mbi/affiliate/promo/service/create_description_dto.json",
                CreatePromoDescriptionRequestDto.class);

        when(strollerJsonApi.addPromo(any()).execute()).thenReturn(Response.error(500,
                ResponseBody.create(MediaType.get("application/json"), "{message: internal-error}")));

        AtomicLong realInsertedIdRef = mockDaoInsertResponse(33L);

        assertThrows(AffiliatePromoException.class,
                () -> service.createPromoDescription(dto));

        var inserted = promoDaoSpy.findPromoDescriptionsByIds(List.of(realInsertedIdRef.get()));
        assertThat(inserted.entrySet(), empty());

        verify(strollerJsonApi, times(2)).addPromo(any());
    }

    @Test
    public void testCreatePromocode() throws Exception {
        long parentId = promoDaoSpy.insertPromoDescription(PromoDescription.builder()
                .withDiscount(new Discount()
                        .discountType(DiscountType.PERCENT)
                        .discountValue(5)
                        .bucketMinPrice(5000)
                )
                .withPromoOffers(new PromoOffers()
                        .url("https://market.yandex.ru/catalog--televizory/2121")
                        .description("Телевизоры")
                        .matchingInclude(new OfferMatching().categoryList(List.of(2121L)))
                        .matchingExclude(new OfferMatching()))
                .withStatus(PromoDescriptionStatus.ACTIVE)
                .withDeviceType(DeviceType.APPLICATION).build());

        var expectedFindResponseBuilder = SyncGetPromo.GetPromoBatchResponse.newBuilder();
        TestResourceUtils.loadProto("ru/yandex/market/mbi/affiliate/promo/service/update_find_response.json", expectedFindResponseBuilder);
        var expectedFindResponse = withAnotherIdParent(parentId, expectedFindResponseBuilder.build());

        var createPromocodeRequest = DataCampPromo.PromoDescription.newBuilder();
        TestResourceUtils.loadProto("ru/yandex/market/mbi/affiliate/promo/service/create_promocode_proto.json", createPromocodeRequest);
        var pk = createPromocodeRequest.getPrimaryKey().toBuilder()
                .setPromoId(AffiliatePromoUtils.toStringChildId(parentId + 1)).build();
        var additionalInfo =
                createPromocodeRequest.getAdditionalInfo().toBuilder().setParentPromoId(AffiliatePromoUtils.toStringParentId(parentId));
        var bluePromocode = createPromocodeRequest.getMechanicsData().getBluePromocode().toBuilder()
                .setPromocodeDescription("Affiliate promo by " + CLID + " based on " + AffiliatePromoUtils.toStringParentId(parentId));

        createPromocodeRequest
                .setPrimaryKey(pk)
                .setAdditionalInfo(additionalInfo)
                .setMechanicsData(createPromocodeRequest.getMechanicsData().toBuilder().setBluePromocode(bluePromocode).build());

        var createPromocodeResponseBuilder = DataCampPromo.PromoDescription.newBuilder();
        TestResourceUtils.loadProto("ru/yandex/market/mbi/affiliate/promo/service/create_promocode_response.json", createPromocodeResponseBuilder);
        var createPromocodeResponse = withOtherIds(createPromocodeResponseBuilder.build(), parentId, parentId + 1);

        when(strollerProtoApi.getPromosByIds(any()).execute()).thenReturn(Response.success(expectedFindResponse));

        doReturn(new PromocodeCheckResponse(true, null)).when(loyaltyClientMock).checkPromocode(any());
        doReturn(ResponseEntity.ok().build()).when(loyaltyClientMock).reservePromocode(any());

        when(strollerJsonApi.addPromo(eq(createPromocodeRequest.build())).execute())
                .thenReturn(Response.success(createPromocodeResponse));

        service.createPromocode(CLID, "BEST-DEAL-AF", AffiliatePromoUtils.toStringParentId(parentId));

        var inDb = promoDao.getPromosByParent(parentId);
        assertThat(inDb, iterableWithSize(1));
        assertThat(inDb.get(0).getParentId(), is(parentId));
        assertThat(inDb.get(0).getClid(), is(CLID));
        assertThat(inDb.get(0).getId(), is(parentId + 1));

        verify(strollerJsonApi).addPromo(eq(createPromocodeRequest.build()));
        verify(loyaltyClientMock).checkPromocode(any());
        verify(loyaltyClientMock).reservePromocode("BEST-DEAL-AF");
    }

    @Test
    public void testCreatePromocodeNoSuffix() throws Exception {
        var expectedFindResponse = SyncGetPromo.GetPromoBatchResponse.newBuilder();
        TestResourceUtils.loadProto("ru/yandex/market/mbi/affiliate/promo/service/update_find_response.json", expectedFindResponse);

        doReturn(PromoDescription.builder()
                .withId(ID)
                .withStatus(PromoDescriptionStatus.ACTIVE).build())
                .when(promoDaoSpy).findDescriptionById(ID);
        when(strollerProtoApi.getPromosByIds(any()).execute()).thenReturn(Response.success(expectedFindResponse.build()));

        var e = assertThrows(AffiliatePromoException.class, () ->
                service.createPromocode(CLID, "BEST-DEAL", STRING_ID)
        );
        assertThat(e.getErrorCode(), is(ErrorCode.BAD_PROMOCODE_VALUE));
    }

    @Test
    public void testCreatePromocodeFailedToReserve() throws Exception {
        var expectedFindResponse = SyncGetPromo.GetPromoBatchResponse.newBuilder();
        TestResourceUtils.loadProto("ru/yandex/market/mbi/affiliate/promo/service/update_find_response.json", expectedFindResponse);

        doReturn(PromoDescription.builder()
                .withId(ID)
                .withStatus(PromoDescriptionStatus.ACTIVE).build())
                .when(promoDaoSpy).findDescriptionById(ID);
        when(strollerProtoApi.getPromosByIds(any()).execute()).thenReturn(Response.success(expectedFindResponse.build()));

        doReturn(new PromocodeCheckResponse(true, null))
                .when(loyaltyClientMock).checkPromocode(any());
        doThrow(new MarketLoyaltyException("error"))
                .when(loyaltyClientMock).reservePromocode(any());


        var e = assertThrows(AffiliatePromoException.class, () ->
                service.createPromocode(CLID, "BEST-DEAL-AF", STRING_ID)
        );
        assertThat(e.getErrorCode(), is(ErrorCode.FAILED_TO_RESERVE_PROMOCODE_VALUE));
        verify(loyaltyClientMock).checkPromocode(any());
        verify(loyaltyClientMock).reservePromocode(any());
    }

    @Test
    public void testCreatePromocodeBadValue() throws Exception {
        var expectedFindResponse = SyncGetPromo.GetPromoBatchResponse.newBuilder();
        TestResourceUtils.loadProto("ru/yandex/market/mbi/affiliate/promo/service/update_find_response.json", expectedFindResponse);

        doReturn(PromoDescription.builder()
                .withId(ID)
                .withStatus(PromoDescriptionStatus.ACTIVE).build())
                .when(promoDaoSpy).findDescriptionById(ID);
        when(strollerProtoApi.getPromosByIds(any()).execute()).thenReturn(Response.success(expectedFindResponse.build()));

        doReturn(new PromocodeCheckResponse(false, MarketLoyaltyErrorCode.PROMOCODE_IS_OCCUPIED_NOW))
                .when(loyaltyClientMock).checkPromocode(any());

        var e = assertThrows(AffiliatePromoException.class, () ->
            service.createPromocode(CLID, "BEST-DEAL-AF", STRING_ID)
        );
        assertThat(e.getErrorCode(), is(ErrorCode.PROMOCODE_IN_USE));
        verify(loyaltyClientMock).checkPromocode(any());
    }

    @Test
    public void testCreatePromocodeParentNotExists() {
        var e = assertThrows(AffiliatePromoException.class,
                () -> service.createPromocode(CLID, "best_deal", STRING_ID)
        );
        assertThat(e.getErrorCode(), is(ErrorCode.PROMO_DESCRIPTION_NOT_FOUND));
    }

    @Test
    public void testCreatePromocodeParentNotActive() {
        doReturn(PromoDescription.builder().withStatus(PromoDescriptionStatus.CANCELLED).build())
                .when(promoDaoSpy).findDescriptionById(ID);
        var e = assertThrows(AffiliatePromoException.class,
                () -> service.createPromocode(CLID, "best_deal", STRING_ID)
        );
        assertThat(e.getErrorCode(), is(ErrorCode.PROMO_DESCRIPTION_NOT_ACTIVE));
    }

    @Test
    public void testCreatePromocodeLowBudget() throws Exception {
        var expectedFindResponse = SyncGetPromo.GetPromoBatchResponse.newBuilder();
        TestResourceUtils.loadProto("ru/yandex/market/mbi/affiliate/promo/service/update_find_response.json", expectedFindResponse);

        // В файле бюджет достаточный, подменяем его на 1
        DataCampPromo.PromoDescription promoDescription = expectedFindResponse.getPromos().getPromo(0).toBuilder()
                .setCurrentBudget(DataCampPromo.CurrentBudget.newBuilder()
                        .setMoneyLimit(Common.PriceExpression.newBuilder().setPrice(1).build()).build()).build();
        expectedFindResponse = SyncGetPromo.GetPromoBatchResponse.newBuilder()
                .setPromos(DataCampPromo.PromoDescriptionBatch.newBuilder()
                        .addPromo(promoDescription).build());

        doReturn(PromoDescription.builder()
                .withId(ID)
                .withStatus(PromoDescriptionStatus.ACTIVE).build())
                .when(promoDaoSpy).findDescriptionById(ID);
        when(strollerProtoApi.getPromosByIds(any()).execute()).thenReturn(Response.success(expectedFindResponse.build()));

        var e = assertThrows(AffiliatePromoException.class,
                () -> service.createPromocode(CLID, "best_deal", STRING_ID)
        );
        assertThat(e.getErrorCode(), is(ErrorCode.LOW_BUDGET));
    }

    @Test
    public void testCancelPromoDescription() throws Exception {
        long parentId = promoDaoSpy.insertPromoDescription(
                somePromoDescription()
                .withStatus(PromoDescriptionStatus.ACTIVE).build());
        long childId = promoDaoSpy.insertPromocode("best_deal", CLID, parentId);

        var expectedFindResponseBuilder = SyncGetPromo.GetPromoBatchResponse.newBuilder();
        TestResourceUtils.loadProto("ru/yandex/market/mbi/affiliate/promo/service/update_find_response.json", expectedFindResponseBuilder);
        var expectedFindResponse = withAnotherIdParent(parentId, expectedFindResponseBuilder.build());

        var descriptionResponse = DataCampPromo.PromoDescription.newBuilder();
        TestResourceUtils.loadProto("ru/yandex/market/mbi/affiliate/promo/service/create_description_proto.json", descriptionResponse);
        var constraints = descriptionResponse.getConstraints().toBuilder().setEnabled(false);
        descriptionResponse.setConstraints(constraints);

        var expectedChildrenFindResponse = SyncGetPromo.GetPromoBatchResponse.newBuilder();
        TestResourceUtils.loadProto("ru/yandex/market/mbi/affiliate/promo/service/children_find_response.json", expectedChildrenFindResponse);
        var descr = withOtherIds(
                expectedChildrenFindResponse.getPromos().getPromo(0),
                parentId, childId);
        expectedChildrenFindResponse = SyncGetPromo.GetPromoBatchResponse.newBuilder().setPromos(
                DataCampPromo.PromoDescriptionBatch.newBuilder().addPromo(descr).build());


        when(strollerProtoApi.getPromosByIds(any()).execute()).thenReturn(Response.success(expectedFindResponse));

        when(strollerJsonApi.addPromo(any()).execute()).thenReturn(Response.success(descriptionResponse.build()));

        when(strollerProtoApi.getPromosByIds(any()).execute()).thenReturn(Response.success(expectedChildrenFindResponse.build()));

        var childDescriptionResponse = withAnotherIdChild(childId, descriptionResponse.build());
        when(strollerJsonApi.addPromo(any()).execute()).thenReturn(Response.success(childDescriptionResponse));

        service.updatePromoDescription(
                AffiliatePromoUtils.toStringParentId(parentId),
                null,
                PromoDescriptionStatus.CANCELLED,
                null, null);

        var inDb = promoDao.findDescriptionById(parentId);
        assertThat(inDb, notNullValue());
        assertThat(inDb.getStatus(), is(PromoDescriptionStatus.CANCELLED));
    }

    @Test
    public void testUpdateEndDate() throws Exception {
        long parentId = promoDaoSpy.insertPromoDescription(
                somePromoDescription()
                        .withStatus(PromoDescriptionStatus.ACTIVE).build());
        long childId = promoDaoSpy.insertPromocode("best_deal", CLID, parentId);

        var newEndDate = LocalDate.of(2021, 12, 31);

        var expectedFindResponseBuilder = SyncGetPromo.GetPromoBatchResponse.newBuilder();
        TestResourceUtils.loadProto("ru/yandex/market/mbi/affiliate/promo/service/update_find_response.json", expectedFindResponseBuilder);
        var expectedFindResponse = withAnotherIdParent(parentId, expectedFindResponseBuilder.build());

        var descriptionResponse = DataCampPromo.PromoDescription.newBuilder();
        TestResourceUtils.loadProto("ru/yandex/market/mbi/affiliate/promo/service/create_description_proto.json", descriptionResponse);
        var constraints = descriptionResponse.getConstraints().toBuilder()
                .setEndDate(AffiliatePromoUtils.toSecondsEndDate(newEndDate, false));
        descriptionResponse.setConstraints(constraints);

        var expectedChildrenFindResponse = SyncGetPromo.GetPromoBatchResponse.newBuilder();
        TestResourceUtils.loadProto("ru/yandex/market/mbi/affiliate/promo/service/children_find_response.json", expectedChildrenFindResponse);
        var descr = withOtherIds(
                expectedChildrenFindResponse.getPromos().getPromo(0),
                parentId, childId);
        expectedChildrenFindResponse = SyncGetPromo.GetPromoBatchResponse.newBuilder().setPromos(
                DataCampPromo.PromoDescriptionBatch.newBuilder().addPromo(descr).build());


        when(strollerProtoApi.getPromosByIds(any()).execute()).thenReturn(Response.success(expectedFindResponse));

        when(strollerJsonApi.addPromo(any()).execute()).thenReturn(Response.success(descriptionResponse.build()));

        when(strollerProtoApi.getPromosByIds(any()).execute()).thenReturn(Response.success(expectedChildrenFindResponse.build()));

        var childDescriptionResponse = withAnotherIdChild(childId, descriptionResponse.build());
        when(strollerJsonApi.addPromo(any()).execute()).thenReturn(Response.success(childDescriptionResponse));

        service.updatePromoDescription(
                AffiliatePromoUtils.toStringParentId(parentId),
                null, null, newEndDate, null);
    }

    @Test
    public void testUpdateEndDateForFinishedPromo() throws Exception {
        long parentId = promoDaoSpy.insertPromoDescription(
                somePromoDescription()
                        .withStatus(PromoDescriptionStatus.ACTIVE).build());

        var newEndDate = LocalDate.of(2021, 12, 31);

        var expectedFindResponseBuilder = SyncGetPromo.GetPromoBatchResponse.newBuilder();
        TestResourceUtils.loadProto("ru/yandex/market/mbi/affiliate/promo/service/update_find_response.json", expectedFindResponseBuilder);
        var finishedResponse = SyncGetPromo.GetPromoBatchResponse.newBuilder().setPromos(DataCampPromo.PromoDescriptionBatch.newBuilder()
                .addPromo(expectedFindResponseBuilder.getPromos().getPromo(0)
                        .toBuilder()
                        .setConstraints(
                                expectedFindResponseBuilder.getPromos().getPromo(0).getConstraints()
                                        .toBuilder()
                                        .setEndDate(clock.instant().getEpochSecond() - 24 * 60 * 60).build())
                ).build());

        when(strollerProtoApi.getPromosByIds(any()).execute()).thenReturn(Response.success(finishedResponse.build()));
        var e = assertThrows(AffiliatePromoException.class, () ->
            service.updatePromoDescription(
                    AffiliatePromoUtils.toStringParentId(parentId),
                    null, null, newEndDate, null));
        assertThat(e.getErrorCode(), is(ErrorCode.BAD_END_DATE));
    }

    @Test
    public void testUpdateEndDateForYesterday() throws Exception {
        long parentId = promoDaoSpy.insertPromoDescription(
                somePromoDescription()
                        .withStatus(PromoDescriptionStatus.ACTIVE).build());

        var newEndDate = AffiliatePromoUtils.today(clock).minusDays(1);

        var expectedFindResponseBuilder = SyncGetPromo.GetPromoBatchResponse.newBuilder();
        TestResourceUtils.loadProto("ru/yandex/market/mbi/affiliate/promo/service/update_find_response.json", expectedFindResponseBuilder);

        when(strollerProtoApi.getPromosByIds(any()).execute()).thenReturn(Response.success(expectedFindResponseBuilder.build()));
        var e = assertThrows(AffiliatePromoException.class, () ->
                service.updatePromoDescription(
                        AffiliatePromoUtils.toStringParentId(parentId),
                        null,
                        null,
                        newEndDate,
                        null));
        assertThat(e.getErrorCode(), is(ErrorCode.BAD_END_DATE));
    }

    @Test
    public void testGetDescriptionsWithPromosBadUserLogin() {
        when(partnerServiceMock.getClidsByUserLogins(List.of("some_user_login")))
                .thenReturn(List.of());

        var e = assertThrows(AffiliatePromoException.class,
                () -> service.getPromoDescriptionsWithPromocodes("some_user_login", null, null, null, null));
        assertThat(e.getErrorCode(), is(ErrorCode.BAD_USER_LOGIN));
    }

    @Test
    public void testGetDescriptionsWithPromos() throws Exception {
        long parentId = promoDao.insertPromoDescription(
                PromoDescription.builder().withDiscount(new Discount()
                        .discountType(DiscountType.PERCENT)
                        .discountValue(12)
                ).withPromoOffers(new PromoOffers()
                        .description("Samsung")
                        .url("https://market.yandex.ru/brands--samsung/153061")
                        .matchingInclude(new OfferMatching().brandList(List.of(153061L)))
                        .matchingExclude(new OfferMatching()))
                .withStatus(PromoDescriptionStatus.ACTIVE)
                .build()
        );
        long id3333 = promoDao.insertPromocode("samsung_v1", CLID, parentId);
        long id4444 = promoDao.insertPromocode("samsung_v2", 4444L, parentId);
        promoDao.insertPromocode("samsung_v2", 5555L, parentId);

        when(partnerServiceMock.getClidsByUserLogins(List.of("some_user_login")))
                .thenReturn(List.of(
                        new Partner(CLID, "some_user_login", "pd1"),
                        new Partner(4444L, "som_user_login", "pd2")));

        var childrenResponse3333Builder = SyncGetPromo.GetPromoBatchResponse.newBuilder();
        TestResourceUtils.loadProto("ru/yandex/market/mbi/affiliate/promo/service/list_children_response.json", childrenResponse3333Builder);
        var childrenResponse3333= SyncGetPromo.GetPromoBatchResponse.newBuilder().setPromos(DataCampPromo.PromoDescriptionBatch.newBuilder()
                .addPromo(withOtherIds(childrenResponse3333Builder.getPromos().getPromo(0), parentId, id3333))
                .build()).build();

        var pk4444 = childrenResponse3333.getPromos().getPromo(0).getPrimaryKey().toBuilder()
                .setPromoId(AffiliatePromoUtils.toStringChildId(id4444))
                .setBusinessId(4444).build();
        var bluePromocode4444 =
                childrenResponse3333.getPromos().getPromo(0).getMechanicsData()
                        .getBluePromocode().toBuilder().setClid(4444L).setPromoCode("samsung_v2");
        var childrenResponse4444 =
                SyncGetPromo.GetPromoBatchResponse.newBuilder().setPromos(DataCampPromo.PromoDescriptionBatch.newBuilder().addPromo(
                childrenResponse3333.getPromos().getPromo(0).toBuilder()
                        .setPrimaryKey(pk4444)
                        .setMechanicsData(DataCampPromo.PromoMechanics.newBuilder()
                                .setBluePromocode(bluePromocode4444))
                        .clearLoyaltyKey()
        ).build());

        @SuppressWarnings("unchecked")
        Call<SyncGetPromo.GetPromoBatchResponse> call1 = mock(Call.class);
        @SuppressWarnings("unchecked")
        Call<SyncGetPromo.GetPromoBatchResponse> call2 = mock(Call.class);
        when(strollerProtoApi.getPromos("v1/partners/"
                        + 3333L + "/promos"
                        + "?businessId=" + 3333L
                        + "&only_unfinished=false"
                        + "&type=" + DataCampPromo.PromoType.BLUE_PROMOCODE_VALUE)).thenReturn(call1);
        when(strollerProtoApi.getPromos("v1/partners/"
                + 4444L + "/promos"
                + "?businessId=" + 4444L
                + "&only_unfinished=false"
                + "&type=" + DataCampPromo.PromoType.BLUE_PROMOCODE_VALUE)).thenReturn(call2);

        doAnswer(invocation -> {
               Callback<SyncGetPromo.GetPromoBatchResponse> callback = invocation.getArgument(0);
               callback.onResponse(call1, Response.success(childrenResponse3333));
               return null;
        }).when(call1).enqueue(any());
        doAnswer(invocation -> {
                    Callback<SyncGetPromo.GetPromoBatchResponse> callback = invocation.getArgument(0);
                    callback.onResponse(call2, Response.success(childrenResponse4444.build()));
                    return null;
        }).when(call2).enqueue(any());

        var serviceSpy = spy(service);
        doReturn(30).when(serviceSpy).getBudgetPercent(any());

        var result = serviceSpy.getPromoDescriptionsWithPromocodes("some_user_login", null, null, null, null);

        var expectedDto = TestResourceUtils.loadDto(
                "ru/yandex/market/mbi/affiliate/promo/service/list_promos_result_dto.json",
                PromoListResponseDto.class);
        expectedDto.getPromos().get(0).getPromoDescription().promoDescriptionId(AffiliatePromoUtils.toStringParentId(parentId));
        Map<Long, PartnerPromoDto> byClidExpectedDto = expectedDto.getPromos().get(0).getPartnerPromoList().stream().collect(Collectors.toMap(PartnerPromoDto::getClid, p -> p));
        byClidExpectedDto.get(CLID).setPromoId(AffiliatePromoUtils.toStringChildId(id3333));
        byClidExpectedDto.get(4444L).setPromoId(AffiliatePromoUtils.toStringChildId(id4444));

        assertThat(result, iterableWithSize(1));
        assertThat(result, is(expectedDto.getPromos()));
    }

    @Test
    public void testGetBudget() throws Exception {
        var expectedFindResponse = SyncGetPromo.GetPromoBatchResponse.newBuilder();
        TestResourceUtils.loadProto("ru/yandex/market/mbi/affiliate/promo/service/update_find_response.json", expectedFindResponse);

        when(strollerProtoApi.getPromosByIds(any()).execute()).thenReturn(Response.success(expectedFindResponse.build()));

        int budgetPercent = service.getBudgetPercent(STRING_ID);
        assertThat(budgetPercent, is(10));
    }

    @Test
    public void testSetExpired() throws Exception {
        long id1 = promoDaoSpy.insertPromoDescription(somePromoDescription()
                .withStatus(PromoDescriptionStatus.ACTIVE).build());
        long id2 = promoDaoSpy.insertPromoDescription(somePromoDescription()
                .withStatus(PromoDescriptionStatus.ACTIVE).build());
        long id3 = promoDaoSpy.insertPromoDescription(somePromoDescription()
                .withStatus(PromoDescriptionStatus.EXPIRED).build());

        var strollerResponse = SyncGetPromo.GetPromoBatchResponse.newBuilder();
        TestResourceUtils.loadProto("ru/yandex/market/mbi/affiliate/promo/service/set_expired_stroller_response.json", strollerResponse);
        var strollerResponseWithIds = withOtherIdsParent(List.of(id1, id2, id3), strollerResponse.build());

        when(strollerProtoApi.getPromos(any()).execute()).thenReturn(Response.success(strollerResponseWithIds));

        service.setExpiredStatuses();

        assertThat(Objects.requireNonNull(promoDao.findDescriptionById(id1)).getStatus(), is(PromoDescriptionStatus.ACTIVE));
        assertThat(Objects.requireNonNull(promoDao.findDescriptionById(id2)).getStatus(), is(PromoDescriptionStatus.EXPIRED));
        assertThat(Objects.requireNonNull(promoDao.findDescriptionById(id3)).getStatus(), is(PromoDescriptionStatus.EXPIRED));
    }


    @Test
    public void testSetExpiredWithShift() throws Exception {
        long id = promoDaoSpy.insertPromoDescription(somePromoDescription()
                .withStatus(PromoDescriptionStatus.INACTIVE).build());
        LocalDateTime updateDay = LocalDate.parse("2022-06-08").atStartOfDay();
        when(varsDaoMock.getVar(AffiliatePromoService.VAR_TIMESTAMP_END_DATE_UPDATE))
                .thenReturn(String.valueOf(updateDay.toEpochSecond(ZoneOffset.UTC)));

        var strollerResponse =
                SyncGetPromo.GetPromoBatchResponse.newBuilder()
                .setPromos(
                        DataCampPromo.PromoDescriptionBatch.newBuilder()
                                .addPromo(DataCampPromo.PromoDescription.newBuilder()
                                    .setPrimaryKey(
                                            DataCampPromo.PromoDescriptionIdentifier.newBuilder()
                                                    .setPromoId("aff_parent_" + id))
                                    .setConstraints(
                                            DataCampPromo.PromoConstraints.newBuilder()
                                                .setEndDate(1654722000L))
                        ));
        when(strollerProtoApi.getPromos(any()).execute()).thenReturn(Response.success(strollerResponse.build()));

        var clock = new TestableClock();
        clock.setFixed(LocalDateTime.of(2022, Month.JUNE, 9, 13, 0, 0).toInstant(ZoneOffset.UTC), ZoneId.of("UTC"));
        var serviceWithClock = new AffiliatePromoService(
                strollerClient, promoDaoSpy, transactionTemplate, partnerServiceMock,
                new LoyaltyClientWrapper(loyaltyClientMock),
                clock,
                varsDaoMock,
                new OfferMatchingRuleMapper(regionServiceMock), 2);
        serviceWithClock.setExpiredStatuses();

        assertThat(Objects.requireNonNull(promoDao.findDescriptionById(id)).getStatus(), is(PromoDescriptionStatus.EXPIRED));
    }

    @Test
    public void testGetDescriptionFromDaoForNewPromocode() {
        long parentId = promoDaoSpy.insertPromoDescription(somePromoDescription()
                .withStatus(PromoDescriptionStatus.ACTIVE).build());
        assertThat(service.getDescriptionFromDaoForNewPromocode(parentId, CLID),
                notNullValue());
    }

    @Test
    public void testGetDescriptionFromDaoForNewPromocodeInactive() {
        long parentId = promoDaoSpy.insertPromoDescription(somePromoDescription()
                .withStatus(PromoDescriptionStatus.INACTIVE).build());
        var e = assertThrows(AffiliatePromoException.class,
                () -> service.getDescriptionFromDaoForNewPromocode(parentId, CLID));
        assertThat(e.getErrorCode(), is(ErrorCode.PROMO_DESCRIPTION_NOT_ACTIVE));
    }

    @Test
    public void testGetDescriptionFromDaoForNewPromocodeInactiveSpecialLogin() {
        when(partnerServiceMock.getUserLoginByClid(444L))
                .thenReturn(AffiliatePromoService.MANAGER_LOGINS.iterator().next());
        long parentId = promoDaoSpy.insertPromoDescription(somePromoDescription()
                .withStatus(PromoDescriptionStatus.INACTIVE).build());
        assertThat(service.getDescriptionFromDaoForNewPromocode(parentId, 444L),
                notNullValue());
    }

    @Test
    public void testGetDescriptionFromDaoForNewPromocodeExpired() {
        when(partnerServiceMock.getUserLoginByClid(444L))
                .thenReturn(AffiliatePromoService.MANAGER_LOGINS.iterator().next());
        long parentId = promoDaoSpy.insertPromoDescription(somePromoDescription()
                .withStatus(PromoDescriptionStatus.EXPIRED).build());
        var e = assertThrows(AffiliatePromoException.class,
                () -> service.getDescriptionFromDaoForNewPromocode(parentId, 444L));
        assertThat(e.getErrorCode(), is(ErrorCode.PROMO_DESCRIPTION_NOT_ACTIVE));
    }

    @Test
    public void testGetDescriptionFromDaoForNewPromocodeClidNotFound() {
        when(partnerServiceMock.getUserLoginByClid(888L)).thenReturn(null);
        long parentId = promoDaoSpy.insertPromoDescription(somePromoDescription()
                .withStatus(PromoDescriptionStatus.ACTIVE).build());
        var e = assertThrows(AffiliatePromoException.class,
                () -> service.getDescriptionFromDaoForNewPromocode(parentId, 444L));
        assertThat(e.getErrorCode(), is(ErrorCode.BAD_CLID));
    }

    @Test
    public void testIsVisiblePartnerGroup() {
        PromoDescription noGroup = PromoDescription.builder().build();
        assertTrue(service.isVisiblePromoDescription("user", noGroup, List.of()));
        assertTrue(service.isVisiblePromoDescription("user", noGroup, List.of("group1")));

        PromoDescription withGroups = PromoDescription.builder()
                .withPartnerGroups(List.of("g1", "g2"))
                .build();
        assertTrue(service.isVisiblePromoDescription("user", withGroups, List.of("g4", "g2")));
        assertFalse(service.isVisiblePromoDescription("user", withGroups, List.of()));
        assertFalse(service.isVisiblePromoDescription("user", withGroups, List.of("g5")));

        assertTrue(service.isVisiblePromoDescription(
                AffiliatePromoService.MANAGER_LOGINS.iterator().next(),
                withGroups,
                List.of("g5")));
    }

    private static PromoDescription.Builder somePromoDescription() {
        return PromoDescription.builder()
                .withDiscount(new Discount()
                        .discountType(DiscountType.PERCENT)
                        .discountValue(5)
                )
                .withPromoOffers(new PromoOffers()
                        .url("https://market.yandex.ru/catalog--televizory/18040671")
                        .matchingInclude(new OfferMatching().categoryList(List.of(18040671L)))
                        .matchingExclude(new OfferMatching())
                        .description("Телевизоры")
                );
    }

    @Test
    public void testFindAndCancelPromocodes() {
        var serviceSpy = spy(service);
        doNothing().when(serviceSpy).cancelPromocodes(any(), any());

        when(partnerServiceMock.getPartnersByGroups(List.of("vip1"))).thenReturn(List.of("user1"));
        when(partnerServiceMock.getClidsByUserLogins(List.of("user1"))).thenReturn(
                List.of(
                        new Partner(11L, "user1", null),
                        new Partner(12L, "user1", null)));
        doReturn(List.of(
                new Promocode(1000, 4L, "P-AF", 11L),
                new Promocode(1001, 4L, "P1-AF", 11L),
                new Promocode(2000, 4L, "222P-AF", 21L),
                new Promocode(2001, 4L, "223P-AF", 22L)
        )).when(promoDaoSpy).getPromosByParent(4L);

        var result = serviceSpy.findAndCancelPromocodes(List.of("vip1"), 4L);
        assertThat(result, containsInAnyOrder(21L, 22L));
    }

    @Test
    public void testFindAndCancelPromocodesSetRestrictionOnPublicPromo() {
        var serviceSpy = spy(service);
        doNothing().when(serviceSpy).cancelPromocodes(any(), any());

        when(partnerServiceMock.getPartnersByGroups(List.of("vip1"))).thenReturn(List.of("user1"));
        when(partnerServiceMock.getClidsByUserLogins(List.of("user1"))).thenReturn(
                List.of(
                        new Partner(11L, "user1", null),
                        new Partner(12L, "user1", null)));
        doReturn(List.of(
                new Promocode(1000, 4L, "P-AF", 11L),
                new Promocode(1001, 4L, "P1-AF", 11L),
                new Promocode(2000, 4L, "222P-AF", 21L)
        )).when(promoDaoSpy).getPromosByParent(4L);

        var result = serviceSpy.findAndCancelPromocodes(List.of("vip1"), 4L);
        assertThat(result, containsInAnyOrder(21L));
    }

    @Test
    public void testFindAndCancelPromocodesSetEmptyGroupRestriction() {
        var serviceSpy = spy(service);
        doNothing().when(serviceSpy).cancelPromocodes(any(), any());

        var result = serviceSpy.findAndCancelPromocodes(List.of(), 4L);
        assertThat(result, emptyIterable());
    }

    @Test
    public void testMapToMatchingFilters() throws Exception{
        var dto = TestResourceUtils.loadDto(
                "ru/yandex/market/mbi/affiliate/promo/service/map_to_matching_filters_input.json",
                PromoDescriptionWithPromocodes.class);
        var noFilter = AffiliatePromoService.mapToMatchingFilters(dto, null, null, null);
        assertThat(noFilter, is(dto));

        var searchNoMatch = AffiliatePromoService.mapToMatchingFilters(dto, "aaaa", null, null);
        assertThat(searchNoMatch, nullValue());

        var datesNoMatch =
                AffiliatePromoService.mapToMatchingFilters(dto, null, LocalDate.parse("2022-01-01"), LocalDate.parse("2022-01-31"));
        assertThat(datesNoMatch, nullValue());

        var searchMatch = AffiliatePromoService.mapToMatchingFilters(dto, "august", null, null);
        assertThat(searchMatch.getPartnerPromoList(), iterableWithSize(1));
        assertThat(searchMatch.getPartnerPromoList().get(0).getPromocodeValue(), is("samsung_august_v2"));

        var datesMatch = AffiliatePromoService.mapToMatchingFilters(dto, null, LocalDate.parse("2021-07-01"), LocalDate.parse("2021-08-03"));
        assertThat(datesMatch.getPartnerPromoList(), iterableWithSize(2));
        assertThat(datesMatch.getPartnerPromoList().stream()
                        .map(p -> p.getPromocodeValue())
                        .collect(Collectors.toList()),
                containsInAnyOrder("samsung_august_v2", "promo_v2"));


        var datesMatch2 = AffiliatePromoService.mapToMatchingFilters(dto, null, LocalDate.parse("2021-08-27"), LocalDate.parse("2021-09-30"));
        assertThat(datesMatch2.getPartnerPromoList(), iterableWithSize(2));
        assertThat(datesMatch2.getPartnerPromoList().stream()
                        .map(p -> p.getPromocodeValue())
                        .collect(Collectors.toList()),
                containsInAnyOrder("samsung_august_v2", "promo_v1"));

    }

    @Test
    public void testGetPromoDescriptionById() throws Exception {
        when(promoDaoSpy.findDescriptionById(1000)).thenReturn(
                somePromoDescription()
                        .withId(1000L)
                        .withStatus(PromoDescriptionStatus.ACTIVE)
                        .build());
        var expectedFindResponseBuilder = SyncGetPromo.GetPromoBatchResponse.newBuilder();
        TestResourceUtils.loadProto("ru/yandex/market/mbi/affiliate/promo/service/stroller_get_by_id_response.json", expectedFindResponseBuilder);
        var expectedFindResponse = withAnotherIdParent(1000, expectedFindResponseBuilder.build());
        when(strollerProtoApi.getPromosByIds(any()).execute()).thenReturn(Response.success(expectedFindResponse));

        var result = service.getPromoDescriptionDetailed("aff_parent_1000");

        var expectedDto = TestResourceUtils.loadDto(
                "ru/yandex/market/mbi/affiliate/promo/service/get_description_by_id_dto.json",
                PromoDescriptionDetailedDto.class);
        assertThat(result, is(expectedDto));
    }

    @Test
    public void testGetBudgetData() throws Exception {
        when(promoDaoSpy.findPromoDescriptionsByIdsAndStatuses(
                List.of(1000L, 1001L), List.of(PromoDescriptionStatus.ACTIVE), null)).thenReturn(
                Map.of(1000L, somePromoDescription()
                                .withId(1000L)
                                .withStatus(PromoDescriptionStatus.ACTIVE)
                                .build(),
                        1001L, somePromoDescription()
                                .withId(1001L)
                                .withStatus(PromoDescriptionStatus.ACTIVE)
                                .withManagerEmail("yuliachr@yandex-team.ru")
                                .withBudgetNotificationThreshold(5)
                                .build()));
        var strollerResponseBuilder = SyncGetPromo.GetPromoBatchResponse.newBuilder();
        TestResourceUtils.loadProto("ru/yandex/market/mbi/affiliate/promo/service/stroller_budget_response.json", strollerResponseBuilder);
        when(strollerProtoApi.getPromos(any()).execute()).thenReturn(Response.success(strollerResponseBuilder.build()));

        var result = service.getAllBudgetsAndLimits();
        assertThat(result, containsInAnyOrder(
                new PromoDescriptionBudgetData(
                        "aff_parent_1000", 30, 300000,
                        LocalDate.of(2021, 8, 31), null, null),
                new PromoDescriptionBudgetData(
                        "aff_parent_1001", 100, 2000000,
                        LocalDate.of(2021, 8, 31), 5, "yuliachr@yandex-team.ru")
        ));
    }

    private AtomicLong mockDaoInsertResponse(long toReturn) {
        final AtomicLong realInsertedId = new AtomicLong();
        Mockito.doAnswer(invocationOnMock -> {
            long id = (Long) invocationOnMock.callRealMethod();
            realInsertedId.set(id);
            return toReturn;
        }).when(promoDaoSpy).insertPromoDescription(any());
        return realInsertedId;
    }

    private static SyncGetPromo.GetPromoBatchResponse withAnotherIdParent(long id, SyncGetPromo.GetPromoBatchResponse response) {
        return SyncGetPromo.GetPromoBatchResponse.newBuilder().setPromos(DataCampPromo.PromoDescriptionBatch.newBuilder()
                .addPromo(withAnotherIdParent(id, response.getPromos().getPromo(0)))
                .build()).build();
    }

    private static SyncGetPromo.GetPromoBatchResponse withOtherIdsParent(List<Long> ids, SyncGetPromo.GetPromoBatchResponse response) {
        Iterator<Long> it = ids.iterator();
        List<DataCampPromo.PromoDescription> promos = response.getPromos().getPromoList().stream()
                .map(promo -> withAnotherIdParent(it.next(), promo))
                .collect(Collectors.toList());
        return SyncGetPromo.GetPromoBatchResponse.newBuilder().setPromos(DataCampPromo.PromoDescriptionBatch.newBuilder()
                .addAllPromo(promos)
                .build()).build();
    }

    private static DataCampPromo.PromoDescription withAnotherIdChild(long childId, DataCampPromo.PromoDescription description) {
        var identifier = description.getPrimaryKey().toBuilder()
                .setPromoId(AffiliatePromoUtils.toStringChildId(childId)).build();
        return description.toBuilder().setPrimaryKey(identifier).build();
    }

    private static DataCampPromo.PromoDescription withAnotherIdParent(long childId, DataCampPromo.PromoDescription description) {
        var identifier = description.getPrimaryKey().toBuilder()
                .setPromoId(AffiliatePromoUtils.toStringParentId(childId)).build();
        return description.toBuilder().setPrimaryKey(identifier).build();
    }

    private static DataCampPromo.PromoDescription withOtherIds(DataCampPromo.PromoDescription description, long parentId, long childId) {
        var additionalInfo = description.getAdditionalInfo().toBuilder()
                .setParentPromoId(AffiliatePromoUtils.toStringParentId(parentId)).build();
        var identifier = description.getPrimaryKey().toBuilder()
                .setPromoId(AffiliatePromoUtils.toStringChildId(childId)).build();
        return description.toBuilder().setPrimaryKey(identifier).setAdditionalInfo(additionalInfo).build();
    }
}
