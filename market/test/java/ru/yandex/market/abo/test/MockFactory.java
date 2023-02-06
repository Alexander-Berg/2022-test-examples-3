package ru.yandex.market.abo.test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.NullInputStream;
import org.joda.time.Instant;
import org.mockito.invocation.InvocationOnMock;
import retrofit2.Call;
import retrofit2.Response;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.common.framework.user.UserInfo;
import ru.yandex.common.framework.user.UserInfoField;
import ru.yandex.common.framework.user.blackbox.BlackBoxUserInfo;
import ru.yandex.common.util.currency.Currency;
import ru.yandex.common.util.date.DateUtil;
import ru.yandex.market.abo.bpmn.client.AboBpmnClient;
import ru.yandex.market.abo.core.datacamp.client.DataCampClient;
import ru.yandex.market.abo.core.deliverycalculator.client.DeliveryCalculatorClient;
import ru.yandex.market.abo.core.offer.report.Offer;
import ru.yandex.market.abo.core.offer.report.OfferService;
import ru.yandex.market.abo.core.offer.report.ReportParam;
import ru.yandex.market.abo.core.offer.report.ShopSwitchedOffException;
import ru.yandex.market.abo.core.shop.ShopInfo;
import ru.yandex.market.abo.core.shop.ShopInfoService;
import ru.yandex.market.abo.core.startrek.StartrekSessionProvider;
import ru.yandex.market.abo.core.user.BulkBlackBoxUserService;
import ru.yandex.market.abo.core.whois.WhoisClient;
import ru.yandex.market.abo.generated.client.communication_proxy.api.CallsApi;
import ru.yandex.market.abo.generated.client.personal_market.api.bulk_retrieve.DefaultApi;
import ru.yandex.market.abo.generated.client.personal_market.model.PersonalBulkRetrieveResponse;
import ru.yandex.market.abo.generated.client.personal_market.model.PersonalMultiTypeRetrieveResponse;
import ru.yandex.market.abo.generated.client.personal_market.model.PersonalRetrieveResponse;
import ru.yandex.market.abo.util.telegram.TelegramClient;
import ru.yandex.market.antifraud.orders.client.MstatAntifraudOrdersClient;
import ru.yandex.market.api.cpa.yam.dto.PrepayRequestDTO;
import ru.yandex.market.api.cpa.yam.dto.RequestsInfoDTO;
import ru.yandex.market.api.cpa.yam.entity.RequestType;
import ru.yandex.market.checkout.checkouter.cart.MultiCart;
import ru.yandex.market.checkout.checkouter.client.CheckouterAPI;
import ru.yandex.market.checkout.checkouter.client.CheckouterOrderHistoryEventsApi;
import ru.yandex.market.checkout.checkouter.client.CheckouterOrderHistoryEventsClient;
import ru.yandex.market.checkout.checkouter.client.CheckouterPaymentApi;
import ru.yandex.market.checkout.checkouter.client.CheckouterRefundApi;
import ru.yandex.market.checkout.checkouter.client.CheckouterReturnApi;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvents;
import ru.yandex.market.checkout.checkouter.event.PagedEvents;
import ru.yandex.market.checkout.checkouter.order.MultiOrder;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.OrderSearchRequest;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.PagedOrders;
import ru.yandex.market.checkout.checkouter.pay.PagedPayments;
import ru.yandex.market.checkout.checkouter.pay.PagedRefunds;
import ru.yandex.market.checkout.checkouter.pay.Payment;
import ru.yandex.market.checkout.checkouter.pay.PaymentGoal;
import ru.yandex.market.checkout.checkouter.pay.PaymentStatus;
import ru.yandex.market.checkout.checkouter.pay.RefundableItem;
import ru.yandex.market.checkout.checkouter.pay.RefundableItems;
import ru.yandex.market.checkout.checkouter.request.PagedOrderEventsRequest;
import ru.yandex.market.checkout.checkouter.request.RequestClientInfo;
import ru.yandex.market.checkout.checkouter.returns.Return;
import ru.yandex.market.checkout.checkouter.shop.PrepayType;
import ru.yandex.market.checkout.common.rest.GenericPage;
import ru.yandex.market.checkout.common.rest.Pager;
import ru.yandex.market.checkout.entity.Attachment;
import ru.yandex.market.checkout.entity.AttachmentGroup;
import ru.yandex.market.checkout.entity.Conversation;
import ru.yandex.market.checkout.entity.ConversationStatus;
import ru.yandex.market.checkout.entity.PrivacyMode;
import ru.yandex.market.checkout.entity.RefereeRole;
import ru.yandex.market.checkout.pushapi.client.PushApi;
import ru.yandex.market.checkout.referee.CheckoutReferee;
import ru.yandex.market.checkout.referee.SearchTerms;
import ru.yandex.market.common.mds.s3.client.content.ContentConsumer;
import ru.yandex.market.common.mds.s3.client.content.ContentProvider;
import ru.yandex.market.common.mds.s3.client.model.ResourceListing;
import ru.yandex.market.common.mds.s3.client.model.ResourceLocation;
import ru.yandex.market.common.mds.s3.client.service.api.MdsS3Client;
import ru.yandex.market.common.report.indexer.IdxAPI;
import ru.yandex.market.common.report.indexer.yellow.YellowIdxApi;
import ru.yandex.market.common.report.model.FeedOfferId;
import ru.yandex.market.core.application.PartnerApplicationStatus;
import ru.yandex.market.core.campaign.model.CampaignType;
import ru.yandex.market.core.cutoff.CutoffNotificationStatus;
import ru.yandex.market.core.feature.model.FeatureType;
import ru.yandex.market.core.manager.dto.ManagerInfoDTO;
import ru.yandex.market.core.orginfo.model.OrganizationInfoSource;
import ru.yandex.market.core.orginfo.model.OrganizationType;
import ru.yandex.market.core.outlet.Address;
import ru.yandex.market.core.outlet.OutletInfo;
import ru.yandex.market.core.outlet.OutletType;
import ru.yandex.market.core.outlet.moderation.OutletStatus;
import ru.yandex.market.logistics.tarificator.open.api.client.api.RegionGroupApi;
import ru.yandex.market.logistics.tarificator.open.api.client.api.RegionGroupStatusApi;
import ru.yandex.market.logistics.tarificator.open.api.client.api.model.ShopRegionGroupsDto;
import ru.yandex.market.mbi.api.client.MbiApiClient;
import ru.yandex.market.mbi.api.client.entity.CutoffActionStatus;
import ru.yandex.market.mbi.api.client.entity.GenericStatusResponse;
import ru.yandex.market.mbi.api.client.entity.abo.OpenAboCutoffResponse;
import ru.yandex.market.mbi.api.client.entity.pagedOutletsDTO.OutletInfoDTO;
import ru.yandex.market.mbi.api.client.entity.pagedOutletsDTO.PagedOutletsDTO;
import ru.yandex.market.mbi.api.client.entity.params.ShopParams;
import ru.yandex.market.mbi.api.client.entity.params.ShopsWithParams;
import ru.yandex.market.mbi.api.client.entity.partner.BusinessOwnerDTO;
import ru.yandex.market.mbi.api.client.entity.partner.PartnerIndexState;
import ru.yandex.market.mbi.api.client.entity.partner.PartnerInfoDTO;
import ru.yandex.market.mbi.api.client.entity.partner.PartnerOrgInfoDTO;
import ru.yandex.market.mbi.api.client.entity.payment.PaymentCheckStatus;
import ru.yandex.market.mbi.api.client.entity.shops.CpaShopInfo;
import ru.yandex.market.mbi.api.client.entity.shops.PagedCpaShops;
import ru.yandex.market.mbi.api.client.entity.shops.ProgramState;
import ru.yandex.market.mbi.api.client.entity.shops.Shop;
import ru.yandex.market.mbi.api.client.entity.shops.ShopFeatureInfoDTO;
import ru.yandex.market.mbi.datacamp.saas.SaasService;
import ru.yandex.market.mbi.open.api.client.MbiOpenApiClient;
import ru.yandex.startrek.client.Issues;
import ru.yandex.startrek.client.Session;
import ru.yandex.startrek.client.model.Issue;
import ru.yandex.startrek.client.model.IssueCreate;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Класс для создания моков используемых сервисов.
 *
 * @author kukabara
 */
@SuppressWarnings("unchecked")
public class MockFactory {
    private static final ThreadLocalRandom RND = ThreadLocalRandom.current();

    private static final Integer TOTAL = 10;
    private static final Integer DEFAULT_PAGE_SIZE = 50;

    private static CheckouterAPI checkouterClientMock;
    private static CheckouterOrderHistoryEventsApi historyEventsApiMock;
    private static CheckouterReturnApi returnApiMock;
    private static CheckouterRefundApi refundApiMock;
    private static CheckouterPaymentApi paymentApiMock;
    private static OfferService offerServiceMock;
    private static CheckoutReferee checkoutRefereeClientMock;
    private static MdsS3Client mdsS3Client;
    private static BulkBlackBoxUserService blackBoxServiceMock;
    private static StartrekSessionProvider sessionProviderMock;
    private static MstatAntifraudOrdersClient mstatAntifraudOrdersClient;
    private static IdxAPI idxApiService;
    private static YellowIdxApi yellowIdxApi;
    private static DataCampClient dataCampClient;
    private static DeliveryCalculatorClient deliveryCalculatorClient;
    private static TelegramClient telegramClient;
    private static IdxAPI idxApiSandboxService;
    private static WhoisClient whoisClient;
    private static PushApi pushApiClient;
    private static RegionGroupStatusApi regionGroupStatusApi;
    private static RegionGroupApi regionGroupApi;
    private static AboBpmnClient aboBpmnClient;
    private static ru.yandex.market.abo.generated.client.personal_market.api.retrieve.DefaultApi personalRetrieveApi;
    private static ru.yandex.market.abo.generated.client.personal_market.api.multi_types_retrieve.DefaultApi personalMultiTypesRetrieveApi;
    private static DefaultApi personalBulkRetrieveApi;
    private static CallsApi callsApi;
    private static SaasService saasService;

    /*--------------------------------------
     * REPORT
     *--------------------------------------*/
    public static synchronized OfferService getOfferServiceMock() throws ShopSwitchedOffException {
        if (offerServiceMock == null) {
            offerServiceMock = mock(OfferService.class);
            doAnswer(invocation -> getOffers(invocation.getArguments()))
                    .when(offerServiceMock).findWithParams(any());
            doAnswer(invocation ->
                    getOffers(Arrays.copyOfRange(invocation.getArguments(), 1, invocation.getArguments().length))
            ).when(offerServiceMock).findWithParams(/*IndexType*/ any(), any());

            doAnswer(invocation -> getOffers(invocation.getArguments()).get(0))
                    .when(offerServiceMock).findFirstWithParams(any());
            doAnswer(invocation -> {
                List<Long> modelIds = (List<Long>) invocation.getArguments()[0];
                List<ru.yandex.market.common.report.model.Model> models = new ArrayList<>();
                for (Long modelId : modelIds) {
                    ru.yandex.market.common.report.model.Model m = new ru.yandex.market.common.report.model.Model();
                    m.setId(modelId);
                    m.setName("Name-" + modelId);
                    m.setCategoryId(modelId);
                    m.setCategoryName("Category-" + modelId);
                    models.add(m);
                }
                return models;
            }).when(offerServiceMock).findModels(anyList());
            doReturn(10).when(offerServiceMock).countReportOffers(any(), any());
        }
        return offerServiceMock;
    }

    public static ShopInfoService getShopInfoServiceMock(boolean inPrdBase) {
        ShopInfoService shopInfoServiceMock = mock(ShopInfoService.class);
        when(shopInfoServiceMock.getShopInfo(anyLong())).then(inv -> {
            long shopId = (long) inv.getArguments()[0];
            ShopInfo shopInfo = new ShopInfo();
            shopInfo.setId(shopId);
            shopInfo.setInProductionBase(inPrdBase);
            shopInfo.setInTestingBase(!inPrdBase);
            return shopInfo;
        });
        return shopInfoServiceMock;
    }

    @Nonnull
    public static List<Offer> getOffers(Object[] params) {
        Long shopId = null;
        int pageSize = 10;

        for (Object o : params) {
            ReportParam param = (ReportParam) o;
            if (param == null) {
                continue;
            }
            switch (param.getType()) {
                case SHOP_ID:
                    shopId = param.getValue() == null ? null : Long.valueOf(param.getValue().toString());
                    break;
                case PAGE_SIZE:
                    if (param.getValue() != null) {
                        pageSize = Integer.parseInt(param.getValue().toString());
                    }
                    break;
            }
        }
        if (shopId == null) {
            shopId = TestHelper.generateId();
        }

        List<Offer> offers = new ArrayList<>();
        for (int i = 0; i < pageSize; i++) {
            offers.add(TestHelper.generateOffer(shopId));
        }
        return offers;
    }

    /*--------------------------------------
     * FEED DISPATCHER
     *--------------------------------------*/
    // TODO

    /*--------------------------------------
     * MBI
     *--------------------------------------*/

    public static synchronized MbiApiClient getMbiApiClientMock() {
        MbiApiClient mbiApiClientMock = mock(MbiApiClient.class);

        initOutletLogic(mbiApiClientMock);
        initCutoffLogic(mbiApiClientMock);
        initParamLogic(mbiApiClientMock);
        initFeatureLogic(mbiApiClientMock);
        initSuperAdminLogic(mbiApiClientMock);

        // /send-message-to-shop
        doReturn("MessageId: 1").when(mbiApiClientMock).sendMessageToShop(anyLong(), anyInt(), anyString());

        // /prepay-request
        doAnswer(invocation -> {
            PartnerApplicationStatus status = (PartnerApplicationStatus) invocation.getArguments()[0];
            RequestsInfoDTO info = new RequestsInfoDTO();
            RequestsInfoDTO.Data data = new RequestsInfoDTO.Data(1L, status, RequestType.MARKETPLACE);
            info.setData(Collections.singletonList(data));
            return info;
        }).when(mbiApiClientMock).getPrepayRequestIds(any(), anyLong());

        doAnswer(invocation -> {
            Long shopId = (Long) invocation.getArguments()[0];

            return getShop(shopId);
        }).when(mbiApiClientMock).getShop(anyLong());

        doAnswer(invocation -> getPartnerInfo((long) invocation.getArguments()[0]))
                .when(mbiApiClientMock).getPartnerInfo(anyLong());

        doReturn(GenericStatusResponse.OK_RESPONSE).when(mbiApiClientMock)
                .openFeatureCutoff(anyLong(), anyInt(), anyInt());
        doReturn(GenericStatusResponse.OK_RESPONSE).when(mbiApiClientMock)
                .closeFeatureCutoff(anyLong(), anyInt(), anyInt());

        doAnswer(invocation -> {
            Long shopId = (Long) invocation.getArguments()[0];
            return new OpenAboCutoffResponse(shopId, CutoffActionStatus.OK, CutoffNotificationStatus.SENT);
        }).when(mbiApiClientMock).openAboCutoff(anyLong(), any());


        doReturn(new PartnerIndexState(0, true, false)).when(mbiApiClientMock).getPartnerIndexState(anyLong());

        doReturn(new PrepayRequestDTO()).when(mbiApiClientMock).getPrepayRequest(1L, null);

        return mbiApiClientMock;
    }

    public static synchronized MbiOpenApiClient getMbiOpenApiClientMock() {
        MbiOpenApiClient mbiOpenApiClientMock = mock(MbiOpenApiClient.class);
        return mbiOpenApiClientMock;
    }

    @Nonnull
    public static Shop getShop(Long shopId) {
        Shop.PaymentStatus status = new Shop.PaymentStatus();
        status.setPaymentCheckStatus(PaymentCheckStatus.SUCCESS);
        status.setYamContractStatusCompleted(true);
        Shop.CampaignDetails campaignDetails = new Shop.CampaignDetails(shopId, shopId, shopId);
        return new Shop(shopId, "ShopName-" + shopId, "ShopName-" + shopId,
                213L, "+7912341234", Collections.emptyList(), ProgramState.ON, ProgramState.ON, false,
                true, status, campaignDetails, "FEED", false);
    }

    @Nonnull
    private static PartnerInfoDTO getPartnerInfo(long shopId) {
        var shop = getShop(shopId);
        var ogrInfo = new PartnerOrgInfoDTO(
                OrganizationType.OOO, shop.getShopName(), "", "", "", OrganizationInfoSource.PARTNER_INTERFACE, "", ""
        );
        var managerInfo = new ManagerInfoDTO(-1, "", "", "", "");
        return new PartnerInfoDTO(
                shopId, null, CampaignType.SHOP, shop.getShopName(),
                "shop.ru", shop.getPhoneNumber(), "", ogrInfo, false, managerInfo
        );
    }

    private static void initFeatureLogic(MbiApiClient mbiApiClient) {
        // /feature/check/result
        doReturn(null).when(mbiApiClient).sendFeatureModerationResult(any());

        // /feature/shops
        doAnswer(invocation -> {
            List<Long> shopIds = (List<Long>) invocation.getArguments()[0];
            List<ShopFeatureInfoDTO> l = new ArrayList<>();
            for (Long shopId : shopIds) {
                ShopFeatureInfoDTO featureInfo = new ShopFeatureInfoDTO(shopId, FeatureType.SUBSIDIES,
                        "SUCCESS", false, null, ProgramState.ON);
                l.add(featureInfo);
            }
            return l;
        }).when(mbiApiClient).getFeatureInfos(anyList(), anyList());

        // /feature/enabled/shops
        doReturn(Collections.emptyList()).when(mbiApiClient).getShopsWithEnabledFeature(anyLong());
    }

    private static void initSuperAdminLogic(MbiApiClient mbiApiClient) {
        var businessOwner = mock(BusinessOwnerDTO.class);
        when(businessOwner.getContactId()).thenReturn(1L);
        when(businessOwner.getContactUid()).thenReturn(1L);
        when(businessOwner.getLogin()).thenReturn("super.admin@yandex.ru");
        when(mbiApiClient.getPartnerSuperAdmin(anyLong())).thenReturn(businessOwner);
    }

    private static void initParamLogic(MbiApiClient mbiApiClient) {
        // /get-shop-checked-params
        doAnswer(invocation -> {
            List<Long> shopIds = (List<Long>) invocation.getArguments()[0];
            List<ShopParams> params = new ArrayList<>();
            for (Long shopId : shopIds) {
                ShopParams p = new ShopParams(shopId, emptyList());
            }
            return new ShopsWithParams(params);
        }).when(mbiApiClient).getShopCheckedParams(anyList());

        // /push-param-check
        doNothing().when(mbiApiClient).pushParamCheck(anyLong(), anyInt(), any());


    }

    public static synchronized CpaShopInfo getCpaShopInfoMock(long shopId) {
        CpaShopInfo info = mock(CpaShopInfo.class);
        when(info.getShopId()).thenReturn(shopId);
        when(info.isPartnerInterface()).thenReturn(false);
        when(info.isEnabled()).thenReturn(true);
        when(info.getOpenCutoffs()).thenReturn(emptyList());
        when(info.getCutoffsForTesting()).thenReturn(emptyList());
        when(info.getPlacementTimeInDays()).thenReturn(100L);
        when(info.getCpaPlacementTimeInDays()).thenReturn(100L);
        Set<Long> regions = Sets.newHashSet(213L);
        when(info.getOwnRegions()).thenReturn(regions);
        when(info.getCpaRegions()).thenReturn(regions.stream().map(BigDecimal::new).collect(Collectors.toSet()));
        when(info.isYandexPrepayFeasible()).thenReturn(true);
        when(info.getPaymentCheckStatus()).thenReturn("SUCCESS");
        return info;
    }

    private static void initCutoffLogic(MbiApiClient mbiApiClient) {
        // get-cpa-shops
        doAnswer(invocation -> {
            List<Long> shopIds = (List<Long>) invocation.getArguments()[0];
            if (shopIds == null || shopIds.isEmpty()) {
                shopIds = singletonList(TestHelper.generateId());
            }
            return new PagedCpaShops(
                    shopIds.stream().map(MockFactory::getCpaShopInfoMock).collect(Collectors.toList()),
                    getMbiPager((Integer) invocation.getArguments()[1], (Integer) invocation.getArguments()[2], shopIds.size())
            );
        }).when(mbiApiClient).getCpaShops(/*shopIds */anyList(), /*page*/anyInt(), /*pageSize*/anyInt(), anyBoolean());

    }

    private static ru.yandex.market.api.pager.Pager getMbiPager(Integer pageNumber, Integer pageSize,
                                                                int total) {
        Pager p = getPager(pageNumber, pageSize, total);
        return new ru.yandex.market.api.pager.Pager(
                p.getTotal(), p.getFrom(), p.getTo(), p.getPageSize(), p.getPagesCount(), p.getCurrentPage()
        );
    }

    private static ru.yandex.market.api.pager.Pager getMbiPager(Integer pageNumber, Integer pageSize) {
        return getMbiPager(pageNumber, pageSize, TOTAL);
    }

    private static Pager getPager(Integer pageNumber, Integer pageSize, int total) {
        int currentPage = pageNumber == null || pageNumber < 1 ? 1 : pageNumber;
        int realPageSize = pageSize == null ? DEFAULT_PAGE_SIZE : pageSize;

        // set pages count
        int pagesCount = total / realPageSize + (total % realPageSize > 0 ? 1 : 0);
        if (pagesCount == 0) {
            pagesCount = 1;
        }

        // set from, to
        currentPage = Math.max(Math.min(currentPage, pagesCount), 1);
        int from = Math.min((currentPage - 1) * realPageSize, total);
        int to = Math.min(currentPage * realPageSize, total);
        return new Pager(total, from, to, pageSize, pagesCount, currentPage);
    }

    private static void initOutletLogic(MbiApiClient mbiApiClient) {
        when(mbiApiClient.getOutletsV2(any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenAnswer(invocation -> {
                    Object[] arguments = invocation.getArguments();
                    Long shopId = (Long) arguments[0];
                    Long outletId = (Long) arguments[1];
                    ru.yandex.market.api.pager.Pager pager;
                    List<OutletInfoDTO> items = new ArrayList<>();
                    if (outletId != null) {
                        pager = new ru.yandex.market.api.pager.Pager(1, 1, 1, 1, 1, 1);
                        items.add(generateOutletInfo(shopId, outletId));
                    } else {
                        pager = getMbiPager((Integer) arguments[7], (Integer) arguments[8]);
                        for (int i = pager.getFrom(); i < pager.getTo(); i++) {
                            items.add(generateOutletInfo(shopId, null));
                        }
                    }
                    return new PagedOutletsDTO(pager, items);
                });
    }

    private static OutletInfoDTO generateOutletInfo(Long shopId, Long outletId) {
        OutletInfo info = mock(OutletInfo.class);
        when(info.getId()).thenReturn(outletId != null ? outletId : RND.nextInt());
        when(info.getName()).thenReturn("[name]");
        when(info.getEmails()).thenReturn(singletonList("[email]"));
        when(info.getDatasourceId()).thenReturn(shopId != null ? shopId : RND.nextInt());
        when(info.getType()).thenReturn(OutletType.DEPOT);
        when(info.getAddress()).thenReturn(new Address.Builder()
                .setCity("[city]")
                .setStreet("[street]")
                .setNumber("[number]")
                .setBuilding("[building]")
                .setEstate("[estate]")
                .setBlock("[block]")
                .setLaneKM(1)
                .setOther("[addrAdd]")
                .build());
        when(info.getStatus()).thenReturn(OutletStatus.AT_MODERATION);

        return new OutletInfoDTO(info);
    }

    /*--------------------------------------
     * CHECKOUTER
     *--------------------------------------*/
    public static synchronized CheckouterAPI getCheckouterClientMock() throws IOException {
        if (checkouterClientMock == null) {
            checkouterClientMock = mock(CheckouterAPI.class);
            doReturn(getPaymentApiMock()).when(checkouterClientMock).payments();
            doReturn(getRefundApiMock()).when(checkouterClientMock).refunds();
            doReturn(getReturnApiMock()).when(checkouterClientMock).returns();
            doReturn(getHistoryEventsApiMock()).when(checkouterClientMock).orderHistoryEvents();
            // cart
            doAnswer(invocation -> {
                Order order = TestHelper.generateOrder(TestHelper.generateId());
                MultiCart actualizedMultiCart = mock(MultiCart.class);
                when(actualizedMultiCart.getCarts()).thenReturn(singletonList(order));

                return actualizedMultiCart;
            }).when(checkouterClientMock).cart(any(), anyLong(), anyBoolean(), any(), any(), any(), any(), any(), anyBoolean());

            // checkout
            doAnswer(invocation -> {
                MultiOrder multiOrder = mock(MultiOrder.class);
                Order order = TestHelper.generateOrder(TestHelper.generateId());
                doReturn(singletonList(order)).when(multiOrder).getOrders();
                doReturn(null).when(multiOrder).getOrderFailures();
                doReturn(null).when(multiOrder).getCartFailures();
                return multiOrder;
            }).when(checkouterClientMock).checkout(any(), anyLong(), anyBoolean(), anyBoolean(), any(), any(),
                    any(), any(), any(), any()
            );

            // getOrder
            doAnswer(invocation -> {
                long orderId = (long) invocation.getArguments()[0];
                return TestHelper.generateOrder(orderId, 1L, 1L, false);
            }).when(checkouterClientMock).getOrder(anyLong(), any(), anyLong());

            // get-orders
            doAnswer(invocation -> {
                OrderSearchRequest request = (OrderSearchRequest) invocation.getArguments()[1];
                Long shopId = request.shopId != null ? request.shopId : TestHelper.generateId();
                Long userId = request.userId != null ? request.userId : TestHelper.generateId();
                boolean fake = request.fake != null ? request.fake : RND.nextBoolean();
                List<Long> orderIds = request.orderIds;

                int page = request.pageInfo != null ? request.pageInfo.getCurrentPage() : 1;
                int pageSize = request.pageInfo != null ? request.pageInfo.getPageSize() : 10;
                int total = orderIds != null && !orderIds.isEmpty() ? orderIds.size() : TOTAL;

                List<Order> orders = new ArrayList<>();
                if (orderIds != null && !orderIds.isEmpty()) {
                    for (Long orderId : orderIds) {
                        orders.add(TestHelper.generateOrder(orderId, shopId, userId, fake));
                    }
                } else {
                    long orderId = TestHelper.generateId();
                    for (int i = 0; i < total; i++) {
                        orders.add(TestHelper.generateOrder(orderId++, shopId, userId, fake));
                    }
                }
                return new PagedOrders(orders, getPager(page, pageSize, total));
            }).when(checkouterClientMock).getOrders(any(RequestClientInfo.class), any(OrderSearchRequest.class));

            when(checkouterClientMock.getOrderReceiptPdf(any(), any()))
                    .thenReturn(IOUtils.toInputStream("mock-pdf", Charset.defaultCharset()));


        }
        return checkouterClientMock;
    }

    public static synchronized CheckouterPaymentApi getPaymentApiMock() {
        if (paymentApiMock == null) {
            paymentApiMock = mock(CheckouterPaymentApi.class);
            doAnswer(invocation -> {
                Object[] arguments = invocation.getArguments();

                long orderId = (long) arguments[0];
                Integer page = (Integer) arguments[3];
                Integer pageSize = (Integer) arguments[4];

                List<Payment> items = new ArrayList<>();
                Payment p = new Payment();
                p.setType(PaymentGoal.ORDER_PREPAY);
                p.setPrepayType(PrepayType.YANDEX_MARKET);
                p.setCurrency(Currency.RUR);
                p.setOrderId(orderId);
                p.setTotalAmount(BigDecimal.valueOf(1000.5));
                p.setStatus(PaymentStatus.CLEARED);
                items.add(p);
                return new PagedPayments(getPager(page, pageSize, items.size()), items);
            }).when(paymentApiMock).getPayments(anyLong(), any(), anyLong(), anyInt(), anyInt());
        }

        return paymentApiMock;
    }

    public static synchronized CheckouterRefundApi getRefundApiMock() {
        if (refundApiMock == null) {
            refundApiMock = mock(CheckouterRefundApi.class);

            when(refundApiMock.getRefundableItems(anyLong(), any(), anyLong(), any())).then(inv -> {
                RefundableItems refundable = new RefundableItems();
                refundable.setItems(List.of(new RefundableItem(new OrderItem(
                        new FeedOfferId("foo", RND.nextLong()), new BigDecimal(RND.nextInt(42)), 1)
                )));
                return refundable;
            });

            when(refundApiMock.getRefunds(anyLong(), any(), any(), any(), any(), any(), any()))
                    .thenReturn(new PagedRefunds(new Pager(), emptyList()));
        }

        return refundApiMock;
    }

    public static synchronized CheckouterReturnApi getReturnApiMock() {
        if (returnApiMock == null) {
            returnApiMock = mock(CheckouterReturnApi.class);
            when(returnApiMock.getReturn(anyLong(), anyLong(), anyBoolean(), any(), anyLong())).thenReturn(new Return());
        }

        return returnApiMock;
    }

    public static synchronized CheckouterOrderHistoryEventsApi getHistoryEventsApiMock() {
        if (historyEventsApiMock == null) {
            historyEventsApiMock = mock(CheckouterOrderHistoryEventsClient.class);

            // /orders/%d/events
            doAnswer(invocation -> {
                Object[] arguments = invocation.getArguments();

                PagedOrderEventsRequest request = (PagedOrderEventsRequest) arguments[1];
                long orderId = request.getOrderId();
                Integer page = request.getPage();
                Integer pageSize = request.getPageSize();

                Order order = TestHelper.generateOrder(orderId);
                List<OrderHistoryEvent> events = new ArrayList<>();
                Date d = order.getCreationDate();
                int i = 0;
                events.add(TestHelper.generateEvent(order, d, null, OrderStatus.PENDING));
                events.add(TestHelper.generateEvent(order, DateUtil.addField(d, Calendar.MINUTE, ++i), OrderStatus.PENDING, OrderStatus.PROCESSING));
                events.add(TestHelper.generateEvent(order, DateUtil.addField(d, Calendar.MINUTE, ++i), OrderStatus.PROCESSING, OrderStatus.DELIVERY));
                events.add(TestHelper.generateEvent(order, DateUtil.addField(d, Calendar.MINUTE, ++i), OrderStatus.DELIVERY, OrderStatus.CANCELLED));
                return new PagedEvents(events, getPager(page, pageSize, events.size()));
            }).when(historyEventsApiMock).getOrderHistoryEvents(any(RequestClientInfo.class), any(PagedOrderEventsRequest.class));
            when(historyEventsApiMock.getOrdersHistoryEvents(any())).thenReturn(new OrderHistoryEvents(emptyList()));
        }

        return historyEventsApiMock;
    }

    /*--------------------------------------
     * CHECKOUT-REFEREE
     *--------------------------------------*/
    private static final Map<Long, Conversation> conversationMapById = new HashMap<>();
    private static Multimap<ConversationStatus, Conversation> conversationMapByStatus = ArrayListMultimap.create();

    static {
        Collection<Conversation> conversations =
                generateConv(TOTAL, new ConversationStatus[]{ConversationStatus.ESCALATED});
        conversations.addAll(generateConv(TOTAL, new ConversationStatus[]{ConversationStatus.ARBITRAGE}));
        conversations.addAll(generateConv(TOTAL, new ConversationStatus[]{ConversationStatus.CLOSED}));
        for (Conversation conv : conversations) {
            conversationMapById.put(conv.getId(), conv);
            conversationMapByStatus.put(conv.getLastStatus(), conv);
        }
    }

    public static synchronized CheckoutReferee getCheckoutRefereeClientMock() throws IOException {
        if (checkoutRefereeClientMock == null) {
            checkoutRefereeClientMock = mock(CheckoutReferee.class);

            doAnswer(invocation -> {
                Conversation conv = TestHelper.generateConv(ConversationStatus.OPEN);
                conversationMapById.put(conv.getId(), conv);
                conversationMapByStatus.put(conv.getLastStatus(), conv);
                return conv;
            }).when(checkoutRefereeClientMock).startConversation(any());

            doAnswer(invocation -> {
                Long convId = (Long) invocation.getArguments()[0];
                Conversation conv = conversationMapById.get(convId);
                if (conv == null) {
                    throw new IllegalArgumentException("Unknown conv " + convId);
                }
                return moveConversation(invocation, ConversationStatus.OPEN, ConversationStatus.ISSUE);
            }).when(checkoutRefereeClientMock).raiseIssue(anyLong(), anyLong(), any(), any(), any(), anyLong(), anyString());

            doAnswer(invocation -> {
                Long convId = (Long) invocation.getArguments()[0];
                Conversation conv = conversationMapById.get(convId);
                if (conv == null) {
                    throw new IllegalArgumentException("Unknown conv " + convId);
                }
                return moveConversation(invocation, ConversationStatus.ISSUE, ConversationStatus.ESCALATED);
            }).when(checkoutRefereeClientMock).escalateToArbiter(anyLong(), anyLong(), any(), any(), any(), anyLong(), anyString());

            doAnswer(invocation -> {
                Long convId = (Long) invocation.getArguments()[0];
                return conversationMapById.get(convId);
            }).when(checkoutRefereeClientMock).getConversation(anyLong(), anyLong(), any(), /*shop*/anyLong());

            // /arbitrage/conversations/search2
            // поиск по ESCALATED, ARBITRAGE, InquiryTypes
            doAnswer(invocation -> {
                SearchTerms terms = (SearchTerms) invocation.getArguments()[0];

                int page = terms.getPage() != null ? terms.getPage() : 1;
                int pageSize = terms.getPageSize() != null ? terms.getPageSize() : 10;
                int total = TOTAL;

                Collection<Conversation> conv = new ArrayList<>();
                ConversationStatus[] statuses = terms.getStatuses() != null ?
                        terms.getStatuses().toArray(new ConversationStatus[0]) : ConversationStatus.values();
                for (ConversationStatus status : statuses) {
                    conv.addAll(conversationMapByStatus.get(status));
                }
                return new GenericPage<>(getPager(page, pageSize, total), conv);
            }).when(checkoutRefereeClientMock).searchConversations(any());

            // /arbitrage/conversations/status-updates
            // поиск ESCALATED -> CLOSED, ARBITRAGE -> CLOSED
            doAnswer(invocation -> {
                Object[] arguments = invocation.getArguments();

                Integer page = (Integer) arguments[6];
                Integer pageSize = (Integer) arguments[7];
                int total = TOTAL;

                return new GenericPage<>(getPager(page, pageSize, total), conversationMapByStatus.get(ConversationStatus.CLOSED));
            }).when(checkoutRefereeClientMock).getStatusUpdates(anyLong(), any(), anyLong(), /*statusFrom*/any(),
                    any(), any(), anyInt(), anyInt());

            // /arbitrage/conversations/%d/arbitrage
            doAnswer(invocation -> moveConversation(invocation, ConversationStatus.ESCALATED, ConversationStatus.ARBITRAGE))
                    .when(checkoutRefereeClientMock).startArbitrage(anyLong(), anyInt(), any());

            // /arbitrage/conversations/%d/resolve
            doAnswer(invocation -> moveConversation(invocation, ConversationStatus.ARBITRAGE, ConversationStatus.CLOSED))
                    .when(checkoutRefereeClientMock).resolveIssue(anyLong(), anyLong(), any(),
                            /*resolutionType*/any(), any(), /*shop*/anyLong(), anyString(), /*user*/anyLong(), anyString()
                    );

            // /arbitrage/conversations/%d/reopen
            doAnswer(invocation -> moveConversation(invocation, ConversationStatus.CLOSED, ConversationStatus.ESCALATED))
                    .when(checkoutRefereeClientMock).reopen(anyLong(), anyInt(), any(),
                            anyLong(), any()
                    );

            // /arbitrage/conversations/%d/close
            doAnswer(invocation -> moveConversation(invocation, null, ConversationStatus.CLOSED))
                    .when(checkoutRefereeClientMock).closeConversation(anyLong(), anyInt(), any(),
                            any(), any()
                    );

            // /arbitrage/conversations/%d/attach
            doAnswer(invocation -> {
                Object[] arguments = invocation.getArguments();

                AttachmentGroup group = new AttachmentGroup();
                group.setId(TestHelper.generateId());
                group.setConversationId((Long) arguments[0]);
                group.setAuthorRole((RefereeRole) arguments[2]);
                group.setPrivacyMode((PrivacyMode) arguments[4]);
                return group;
            }).when(checkoutRefereeClientMock).addAttachmentGroup(anyLong(), anyLong(), any(),
                    anyLong(), any());

            // uploadAttachment
            doAnswer(invocation -> {
                Attachment att = new Attachment();
                att.setId(TestHelper.generateId());
                att.setContentId("contentId");
                return att;
            }).when(checkoutRefereeClientMock).uploadAttachment(anyLong(), anyLong(), anyLong(), any(),
                    anyLong(), any(), any(), (InputStream) any());

            when(checkoutRefereeClientMock.downloadAttachment(anyLong(), anyLong(), anyLong(), anyLong(), any(), any()))
                    .thenReturn(new NullInputStream(1));
        }
        return checkoutRefereeClientMock;
    }

    public static synchronized BulkBlackBoxUserService getBlackBoxServiceMock() {
        if (blackBoxServiceMock == null) {
            blackBoxServiceMock = mock(BulkBlackBoxUserService.class);
            when(blackBoxServiceMock.getUserInfo(anyLong())).then(inv -> userInfo((Long) inv.getArguments()[0]));
            when(blackBoxServiceMock.getUserInfo(anyLong(), any())).then(inv -> userInfo((Long) inv.getArguments()[0]));
            when(blackBoxServiceMock.getUserInfo(any())).thenReturn(userInfo(RND.nextInt()));
            when(blackBoxServiceMock.getUserInfo(any(), any())).thenReturn(userInfo(RND.nextInt()));
            when(blackBoxServiceMock.getUsersInfo(anySet()))
                    .then(inv -> ((Set<Long>) inv.getArguments()[0]).stream()
                            .map(MockFactory::userInfo)
                            .collect(Collectors.toMap(UserInfo::getLogin, Function.identity())
                            ));
        }
        return blackBoxServiceMock;
    }

    private static BlackBoxUserInfo userInfo(long userId) {
        BlackBoxUserInfo userInfo = new BlackBoxUserInfo(userId);
        userInfo.addField(UserInfoField.LOGIN, "mock-mvc-user");
        return userInfo;
    }

    @Nonnull
    private static Conversation moveConversation(InvocationOnMock invocation,
                                                 ConversationStatus fromStatus, ConversationStatus toStatus) {
        Long convId = (Long) invocation.getArguments()[0];
        Conversation conversation = conversationMapById.get(convId);
        if (conversation == null) {
            throw new IllegalArgumentException("Unknown conv " + convId);
        }
        if (fromStatus == null || conversation.getLastStatus() == fromStatus) {
            conversationMapByStatus.remove(conversation.getLastStatus(), conversation);

            conversation.setLastStatus(toStatus);
            conversationMapByStatus.put(conversation.getLastStatus(), conversation);
        } else {
            throw new IllegalStateException(String.format("Can't move to status %s from %s (must be %s)",
                    toStatus, conversation.getLastStatus(), fromStatus));
        }
        return conversation;
    }

    private static Collection<Conversation> generateConv(int total, ConversationStatus[] statuses) {
        if (statuses == null) {
            statuses = ConversationStatus.values();
        }
        Collection<Conversation> conversations = new ArrayList<>();
        for (int i = 0; i < total; i++) {
            conversations.add(
                    TestHelper.generateConv(statuses[RND.nextInt(statuses.length)])
            );
        }
        return conversations;
    }

    /**
     * S3
     */
    public static synchronized MdsS3Client getMdsS3Client() {
        if (mdsS3Client == null) {
            Map<String, Map<String, byte[]>> storage = new HashMap<>();
            mdsS3Client = mock(MdsS3Client.class);

            doAnswer(invocation -> {
                ResourceLocation resourceLocation = (ResourceLocation) invocation.getArguments()[0];
                Map<String, byte[]> bucket = storage.getOrDefault(resourceLocation.getBucketName(), emptyMap());
                byte[] bytes = bucket.getOrDefault(resourceLocation.getKey(), new byte[0]);
                ContentConsumer consumer = (ContentConsumer) invocation.getArguments()[1];
                return consumer.consume(new ByteArrayInputStream(bytes));
            }).when(mdsS3Client).download(any(), any());

            doAnswer(invocation -> {
                ContentProvider provider = (ContentProvider) invocation.getArguments()[1];
                byte[] bytes = IOUtils.toByteArray(provider.getInputStream());
                ResourceLocation location = (ResourceLocation) invocation.getArguments()[0];
                storage.computeIfAbsent(location.getBucketName(), s -> new HashMap<>()).put(location.getKey(), bytes);
                return null;
            }).when(mdsS3Client).upload(any(), any());

            doAnswer(invocation -> {
                ResourceLocation[] locations = (ResourceLocation[]) invocation.getArguments()[0];
                for (ResourceLocation location : locations) {
                    storage.get(location.getBucketName()).remove(location.getKey());
                }
                return null;
            }).when(mdsS3Client).delete(any());

            doAnswer(invocation -> {
                ResourceLocation location = (ResourceLocation) invocation.getArguments()[0];
                return storage.containsKey(location.getBucketName()) && storage.get(location.getKey()).containsKey(location.getKey());
            }).when(mdsS3Client).contains(any());

            doAnswer(invocation -> {
                ResourceLocation location = (ResourceLocation) invocation.getArguments()[0];
                return ResourceListing.create(
                        location.getBucketName(),
                        new ArrayList<>(storage.getOrDefault(location.getBucketName(), emptyMap()).keySet()),
                        emptyList()
                );
            }).when(mdsS3Client).list(any(), anyBoolean());
        }
        return mdsS3Client;
    }

    public static synchronized StartrekSessionProvider getSessionProviderMock() {
        if (sessionProviderMock == null) {
            sessionProviderMock = mock(StartrekSessionProvider.class);

            Session session = mock(Session.class);
            Issues issues = mock(Issues.class);
            Issue issue = mock(Issue.class);
            when(issue.getKey()).thenReturn(String.valueOf(RND.nextLong()));
            when(issue.getCreatedAt()).thenReturn(Instant.now());
            when(issues.create(any(IssueCreate.class))).thenReturn(issue);
            when(session.issues()).thenReturn(issues);
            when(issues.find(anyString())).thenReturn(Cf.emptyIterator());

            when(sessionProviderMock.session()).thenReturn(session);
        }
        return sessionProviderMock;
    }

    public static synchronized MstatAntifraudOrdersClient getMstatAntifraudOrdersClient() {
        if (mstatAntifraudOrdersClient == null) {
            mstatAntifraudOrdersClient = mock(MstatAntifraudOrdersClient.class);
        }
        return mstatAntifraudOrdersClient;
    }

    public static synchronized IdxAPI getIdxApiService() {
        if (idxApiService == null) {
            idxApiService = mock(IdxAPI.class);
        }
        return idxApiService;
    }

    public static synchronized YellowIdxApi getYellowIdxApi() {
        if (yellowIdxApi == null) {
            yellowIdxApi = mock(YellowIdxApi.class);
        }
        return yellowIdxApi;
    }

    public static synchronized DataCampClient getDataCampClient() {
        if (dataCampClient == null) {
            dataCampClient = mock(DataCampClient.class);
        }
        return dataCampClient;
    }

    public static synchronized DeliveryCalculatorClient getDeliveryCalculatorClient() {
        if (deliveryCalculatorClient == null) {
            deliveryCalculatorClient = mock(DeliveryCalculatorClient.class);
        }
        return deliveryCalculatorClient;
    }

    public static synchronized TelegramClient getTelegramClient() {
        if (telegramClient == null) {
            telegramClient = mock(TelegramClient.class);
        }
        return telegramClient;
    }

    public static synchronized IdxAPI getIdxApiSandboxService() {
        if (idxApiSandboxService == null) {
            idxApiSandboxService = mock(IdxAPI.class);
        }
        return idxApiSandboxService;
    }

    public static synchronized WhoisClient getWhoisClient() {
        if (whoisClient == null) {
            whoisClient = mock(WhoisClient.class);
            when(whoisClient.query(any())).thenReturn(Map.of());
        }
        return whoisClient;
    }

    public static synchronized PushApi getPushApiClient() {
        if (pushApiClient == null) {
            pushApiClient = mock(PushApi.class);
        }
        return pushApiClient;
    }

    public static synchronized RegionGroupStatusApi getRegionGroupStatusApi() {
        if (regionGroupStatusApi == null) {
            regionGroupStatusApi = mock(RegionGroupStatusApi.class);
        }
        return regionGroupStatusApi;
    }

    public static synchronized RegionGroupApi getRegionGroupApi() throws IOException {
        if (regionGroupApi == null) {
            regionGroupApi = mock(RegionGroupApi.class);
            doReturn(mockRetrofitCall(new ShopRegionGroupsDto())).when(regionGroupApi).getRegionGroups(anyLong());
        }
        return regionGroupApi;
    }

    public static synchronized AboBpmnClient getAboBpmnClient() {
        if (aboBpmnClient == null) {
            aboBpmnClient = mock(AboBpmnClient.class);
        }
        return aboBpmnClient;
    }

    public static synchronized ru.yandex.market.abo.generated.client.personal_market.api.retrieve.DefaultApi
    getPersonalRetrieveApi() throws IOException {
        if (personalRetrieveApi == null) {
            personalRetrieveApi =
                    mock(ru.yandex.market.abo.generated.client.personal_market.api.retrieve.DefaultApi.class);
            doReturn(mockRetrofitCall(new PersonalRetrieveResponse()))
                    .when(personalRetrieveApi).v1DataTypeRetrievePost(any(), any());
        }
        return personalRetrieveApi;
    }

    public static synchronized ru.yandex.market.abo.generated.client.personal_market.api.multi_types_retrieve.DefaultApi
    getPersonalMultiTypesRetrieveApi() throws IOException {
        if (personalMultiTypesRetrieveApi == null) {
            personalMultiTypesRetrieveApi =
                    mock(ru.yandex.market.abo.generated.client.personal_market.api.multi_types_retrieve.DefaultApi.class);
            doReturn(mockRetrofitCall(new PersonalMultiTypeRetrieveResponse()))
                    .when(personalMultiTypesRetrieveApi).v1MultiTypesRetrievePost(any());
        }
        return personalMultiTypesRetrieveApi;
    }

    public static synchronized DefaultApi getPersonalBulkRetrieveApi() throws IOException {
        if (personalBulkRetrieveApi == null) {
            personalBulkRetrieveApi = mock(DefaultApi.class);
            doReturn(mockRetrofitCall(new PersonalBulkRetrieveResponse()))
                    .when(personalBulkRetrieveApi).v1DataTypeBulkRetrievePost(any(), any());
        }
        return personalBulkRetrieveApi;
    }

    public static synchronized CallsApi getCallsApi() {
        if (callsApi == null) {
            callsApi = mock(CallsApi.class);
        }
        return callsApi;
    }

    private static <T> Call<T> mockRetrofitCall(T body) throws IOException {
        var call = mock(Call.class);
        when(call.execute()).thenReturn(Response.success(body));
        return call;
    }

    public static synchronized SaasService getSaasService() throws IOException {
        if (saasService == null) {
            saasService = mock(SaasService.class);
        }
        return saasService;
    }
}
