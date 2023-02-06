package ru.yandex.market.pers.notify.mock;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.jetbrains.annotations.NotNull;

import ru.yandex.common.cache.memcached.MemCachedAgent;
import ru.yandex.market.loyalty.client.MarketLoyaltyClient;
import ru.yandex.market.loyalty.client.RestMarketLoyaltyClient;
import ru.yandex.market.monitoring.ComplicatedMonitoring;
import ru.yandex.market.pers.list.PersBasketClient;
import ru.yandex.market.pers.list.model.BasketItem;
import ru.yandex.market.pers.list.model.BasketItemType;
import ru.yandex.market.pers.list.model.BasketLabel;
import ru.yandex.market.pers.notify.ems.event.NotificationEvent;
import ru.yandex.market.pers.notify.model.NotificationSubtype;
import ru.yandex.market.pers.notify.model.NotificationTransportType;
import ru.yandex.market.pers.notify.model.event.NotificationEventDataName;
import ru.yandex.market.pers.notify.model.event.NotificationEventSource;
import ru.yandex.market.pers.notify.model.event.NotificationEventStatus;
import ru.yandex.market.pers.notify.model.push.MobileAppInfo;
import ru.yandex.market.pers.notify.model.push.MobilePlatform;
import ru.yandex.market.pers.notify.passport.model.UserInfo;
import ru.yandex.market.pers.notify.push.PusherResponse;
import ru.yandex.market.pers.notify.push.XivaPusherService;
import ru.yandex.market.report.ReportService;
import ru.yandex.market.report.model.Model;
import ru.yandex.market.sdk.userinfo.domain.AggregateUserInfo;
import ru.yandex.market.sdk.userinfo.domain.Options;
import ru.yandex.market.sdk.userinfo.domain.SberlogInfo;
import ru.yandex.market.sdk.userinfo.domain.Sex;
import ru.yandex.market.sdk.userinfo.domain.Uid;
import ru.yandex.market.sdk.userinfo.service.UidConstants;
import ru.yandex.market.sdk.userinfo.service.UserInfoService;
import ru.yandex.market.sdk.userinfo.util.Result;
import ru.yandex.market.shopinfo.OrganizationInfoService;
import ru.yandex.market.shopinfo.ShopInfo;
import ru.yandex.market.shopinfo.ShopInfoService;
import ru.yandex.market.shopinfo.ShopReturnAddress;
import ru.yandex.market.shopinfo.SupplierInfo;
import ru.yandex.market.shopinfo.SupplierInfoService;
import ru.yandex.passport.tvmauth.TvmClient;

import static org.mockito.Matchers.anyCollectionOf;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyCollection;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.anyObject;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.anyVararg;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.market.pers.notify.mock.HttpClientMockHelpers.getHttpResponseMock;
import static ru.yandex.market.pers.notify.mock.HttpClientMockHelpers.withPath;

/**
 * @author Ivan Anisimov
 *         valter@yandex-team.ru
 *         17.08.15
 */
public class MockFactory {

    public static final long SBER_ID = UidConstants.SBER_ID_RANGE.upperEndpoint();
    private static final ThreadLocalRandom RND = ThreadLocalRandom.current();

    public static MobileAppInfo generateMobileAppInfo() {
        MobileAppInfo info = new MobileAppInfo();
        info.setUuid(UUID.randomUUID().toString());
        info.setYandexUid(UUID.randomUUID().toString());
        info.setUid(RND.nextBoolean() ? null : (long) RND.nextInt(10_000));
        info.setAppName("myApp");
        info.setCreationTime(new Date());
        info.setGeoId(RND.nextBoolean() ? null : (long) RND.nextInt());
        info.setLoginTime(new Date());
        info.setModificationTime(new Date());
        info.setPlatform(MobilePlatform.values()[RND.nextInt(MobilePlatform.values().length)]);
        info.setUnregistered(RND.nextBoolean());
        info.setPushToken(UUID.randomUUID().toString());
        return info;
    }

    public static NotificationEventSource generateNotificationPushEventSource() {
        List<NotificationSubtype> notificationSubtypes = Arrays.stream(NotificationSubtype.values())
            .filter(t -> t.getTransportType() == NotificationTransportType.PUSH)
            .collect(Collectors.toList());

        NotificationSubtype subtype = notificationSubtypes.get(RND.nextInt(notificationSubtypes.size()));

        NotificationEventSource.Builder source;
        source = NotificationEventSource.fromUuid(UUID.randomUUID().toString(), subtype);

        source.setSendTime(new Date());

        if (RND.nextBoolean()) {
            source.setUid((long) Math.abs(RND.nextInt()));
        }

        source.setSourceId((long) RND.nextInt());

        if (RND.nextBoolean()) {
            source.setData(generateNotificationEventData());
        }
        return source.build();
    }

    public static NotificationEventSource generateNotificationEventSource() {
        NotificationSubtype subtype = NotificationSubtype.values()[RND.nextInt(NotificationSubtype.values().length)];

        NotificationEventSource.Builder source;
        if (subtype.getTransportType() == NotificationTransportType.PUSH) {
            source = NotificationEventSource.fromUuid(UUID.randomUUID().toString(), subtype);
        } else {
            source = NotificationEventSource.fromEmail("valeter@rambler.ru", subtype);
        }

        source.setSendTime(new Date());

        if (RND.nextBoolean()) {
            source.setUid((long) Math.abs(RND.nextInt()));
        }

        source.setSourceId((long) RND.nextInt());

        if (RND.nextBoolean()) {
            source.setData(generateNotificationEventData());
        }
        return source.build();
    }

    public static NotificationEvent generateNotificationPushEvent() {
        NotificationEvent event = new NotificationEvent();
        event.setSendTime(new Date());

        event.setUuid(UUID.randomUUID().toString());
        event.setAddress("");

        if (RND.nextBoolean()) {
            event.setUid(RND.nextLong());
        }

        if (RND.nextBoolean()) {
            event.setSourceId(RND.nextLong());
        }

        List<NotificationSubtype> notificationSubtypes = Arrays.stream(NotificationSubtype.values())
            .filter(t -> t.getTransportType() == NotificationTransportType.PUSH)
            .collect(Collectors.toList());

        event.setNotificationSubtype(notificationSubtypes.get(RND.nextInt(notificationSubtypes.size())));
        event.setCreationTime(new Date());
        event.setId(RND.nextLong());
        event.setStatus(NotificationEventStatus.values()[RND.nextInt(NotificationEventStatus.values().length)]);
        event.setModificationTime(new Date());
        event.setRepeatPostAction(RND.nextBoolean());

        if (RND.nextBoolean()) {
            event.setData(generateNotificationEventData());
        }

        return event;
    }

    public static NotificationEvent generateNotificationEvent() {
        NotificationEvent event = new NotificationEvent();
        event.setSendTime(new Date());

        if (RND.nextBoolean()) {
            event.setUuid(UUID.randomUUID().toString());
            event.setAddress("");
        } else {
            event.setAddress(UUID.randomUUID().toString() + '@' + UUID.randomUUID().toString());
        }

        if (RND.nextBoolean()) {
            event.setUid(RND.nextLong());
        }

        if (RND.nextBoolean()) {
            event.setSourceId(RND.nextLong());
        }

        event.setNotificationSubtype(NotificationSubtype.values()[RND.nextInt(NotificationSubtype.values().length)]);
        event.setCreationTime(new Date());
        event.setId(RND.nextLong());
        event.setStatus(NotificationEventStatus.values()[RND.nextInt(NotificationEventStatus.values().length)]);
        event.setModificationTime(new Date());
        event.setRepeatPostAction(RND.nextBoolean());

        if (RND.nextBoolean()) {
            event.setData(generateNotificationEventData());
        }

        return event;
    }

    public static Map<String, String> generateNotificationEventData() {
        List<String> dataNames = new ArrayList<>();

        Field[] fields = NotificationEventDataName.class.getFields();
        for (Field field : fields) {
            try {
                dataNames.add((String) field.get(NotificationEventDataName.class));
            } catch (IllegalAccessException ignored) {
            }
        }

        Map<String, String> result = new HashMap<>();

        int count = RND.nextInt(dataNames.size());
        List<Integer> dataNameIndecies = new ArrayList<>();
        for (int i = 0; i < dataNames.size(); i++) {
            dataNameIndecies.add(i);
        }
        Collections.shuffle(dataNameIndecies, RND);
        for (int i = 0; i < count; i++) {
            String value = RND.nextBoolean() ? String.valueOf(RND.nextLong()) : UUID.randomUUID().toString();
            String dataName = dataNames.get(dataNameIndecies.get(i));
            value = Objects.equals(dataName, NotificationEventDataName.UID) ? String.valueOf(RND.nextLong()) : value;
            result.put(dataName, value);
        }

        return result;
    }


    private static UserInfo generateUserInfo() {
        return new UserInfo(RND.nextInt(), UUID.randomUUID().toString(), generateEmail(),
            UUID.randomUUID().toString(), UUID.randomUUID().toString(), UUID.randomUUID().toString());
    }

    private static String generateEmail() {
        return UUID.randomUUID().toString() + "@" + UUID.randomUUID().toString() + ".ru";
    }

    private static List<BasketLabel> generateLabels(int count) {
        List<BasketLabel> result = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            result.add(generateLabel());
        }
        return result;
    }

    private static BasketLabel generateLabel() {
        BasketLabel result = new BasketLabel();
        result.setCreateDate(new Date(RND.nextLong()));
        result.setDeleteDate(new Date(RND.nextLong()));
        result.setDisplayName(UUID.randomUUID().toString());
        result.setId(RND.nextLong());
        result.setNote(UUID.randomUUID().toString());
        result.setOwnerId(RND.nextLong());
        result.setUid(RND.nextLong());
        return result;
    }

    public static List<BasketItem> generateItems(int count) {
        return IntStream.range(0, count).mapToObj(i -> generateItem()).collect(Collectors.toList());
    }

    @NotNull
    public static BasketItem generateItem() {
        BasketItem item = generateTypedItem();
        item.setCreateDate(new Date(RND.nextLong()));
        if (RND.nextBoolean()) {
            item.setDeleteDate(new Date(RND.nextLong()));
        }
        item.setDisplayName(UUID.randomUUID().toString());
        item.setId(RND.nextLong());

        int countLabels = RND.nextInt(10);
        List<Long> labelIds = new ArrayList<>();
        for (int i = 0; i < countLabels; i++) {
            labelIds.add(RND.nextLong());
        }
        item.setLabelIds(labelIds);

        item.setNote(UUID.randomUUID().toString());
        item.setOwnerId(RND.nextLong());

        item.setReferer(UUID.randomUUID().toString());

        item.setUid(RND.nextLong());
        return item;
    }
    
    private static BasketItem generateTypedItem() {
        BasketItemType type = BasketItemType.values()[RND.nextInt(BasketItemType.values().length)];
        switch (type) {
            case MODEL: {
                BasketItem basketItemModel = new BasketItem(BasketItemType.MODEL);
                basketItemModel.setModelId(RND.nextInt());
                basketItemModel.setHid(RND.nextLong());
                return basketItemModel;
            }
            case OFFER: {
                BasketItem basketItemOffer = new BasketItem(BasketItemType.OFFER);
                basketItemOffer.setModelId(RND.nextLong());
                basketItemOffer.setHid(RND.nextLong());
                basketItemOffer.setOfferId(RandomStringUtils.random(RND.nextInt(10)));
                return basketItemOffer;
            }
            case GROUP: {
                BasketItem basketItemGroup = new BasketItem(BasketItemType.GROUP);
                basketItemGroup.setGroupId(RND.nextLong());
                basketItemGroup.setHid(RND.nextLong());
                return basketItemGroup;
            }
            case CLUSTER: {
                BasketItem basketItemCluster = new BasketItem(BasketItemType.CLUSTER);
                basketItemCluster.setClusterId(RND.nextLong());
                basketItemCluster.setHid(RND.nextLong());
                if (RND.nextBoolean()) {
                    basketItemCluster.setDeleteDate(new Date(RND.nextLong()));
                }
                return basketItemCluster;
            }
            default:
                return new BasketItem();
        }
    }


    public PersBasketClient getPersBasketClientMock() {
        PersBasketClient result = mock(PersBasketClient.class);
        initPersBasketClientMock(result);
        return result;
    }

    public synchronized void initPersBasketClientMock(PersBasketClient result) {
        when(result.getItems(anyString(), any())).thenReturn(generateItems(RND.nextInt(100)));
    }

    public MemCachedAgent getMemCachedAgentMock() {
        MemCachedAgent result = mock(MemCachedAgent.class);
        initMemCachedAgentMock(result);
        return result;
    }

    public synchronized void initMemCachedAgentMock(MemCachedAgent result) {
        final Map<String, Object> cache = Collections.synchronizedMap(new HashMap<>());

        when(result.addInCache(anyString(), anyObject(), anyObject())).thenAnswer(invocation -> {
            String key = (String) invocation.getArguments()[0];
            Object value = invocation.getArguments()[1];
            cache.put(key, value);
            return true;
        });

        when(result.getFromCache(anyCollection())).thenAnswer(invocation -> {
            Map<String, Object> map = new HashMap<>();
            Collection<String> keys = (Collection<String>) invocation.getArguments()[0];
            for (String next : keys) {
                if (cache.containsKey(next)) {
                    map.put(next, cache.get(next));
                }
            }
            return map;
        });

        when(result.getFromCache(anyString())).thenAnswer(invocation ->
            cache.get(invocation.getArguments()[0]));

        doAnswer(invocation -> {
            cache.remove(invocation.getArguments()[0]);
            return null;
        }).when(result).deleteFromCache(anyString());

        doAnswer(invocation -> {
            String key = (String) invocation.getArguments()[0];
            Object value = invocation.getArguments()[1];
            cache.put(key, value);
            return null;
        }).when(result).putInCache(anyString(), anyObject(), anyObject());

        doAnswer(invocation -> {
            String key = (String) invocation.getArguments()[0];
            Object value = invocation.getArguments()[1];
            cache.put(key, value);
            return null;
        }).when(result).putInCache(anyString(), anyObject());
    }

    public XivaPusherService getPusherServiceMock() {
        XivaPusherService result = mock(XivaPusherService.class);
        initPusherServiceMock(result);
        return result;
    }

    public synchronized void initPusherServiceMock(XivaPusherService result) {
        when(result.push(anyLong(), anyString(), any(NotificationSubtype.class), anyString(), anyString(), anyVararg()))
            .thenReturn(PusherResponse.success("some_id"));
        when(result.register(anyLong(), anyString(), anyString(), any(MobilePlatform.class), anyString())).thenReturn(true);
        when(result.unregister(anyLong(), anyString())).thenReturn(true);
    }

    public ShopInfoService getShopInfoServiceMock() {
        ShopInfoService result = mock(ShopInfoService.class);
        initShopInfoServiceMock(result);
        return result;
    }

    private void initGetShopNames(OrganizationInfoService<?> result, String baseName) {
        when(result.getShopNames(any()))
            .then(invocation -> {
                if (invocation.getArguments() == null) {
                    return null;
                }
                String[] result1 = new String[invocation.getArguments().length];
                for (int i = 0; i < result1.length; i++) {
                    result1[i] = baseName + " " + invocation.getArguments()[i];
                }
                return result1;
            });
        when(result.getShopName(anyLong())).thenReturn(Optional.of("returned_"+ baseName + "_name"));
    }

    public synchronized void initShopInfoServiceMock(ShopInfoService result) {
        initGetShopNames(result, "shop");

        ShopInfo info = new ShopInfo();
        info.setName("name");

        when(result.getShopInfo(anyLong())).thenReturn(Optional.of(info));

        when(result.getShopReturnAddress(anyLong())).thenReturn(
            Optional.of(new ShopReturnAddress("my_address"))
        );
    }

    public SupplierInfoService getSupplierInfoServiceMock() {
        SupplierInfoService result = mock(SupplierInfoService.class);
        initSupplierInfoServiceMock(result);
        return result;
    }

    public synchronized void initSupplierInfoServiceMock(SupplierInfoService result) {
        initGetShopNames(result, "supplier");

        SupplierInfo info = new SupplierInfo();
        info.setName("name");

        when(result.getShopInfo(anyLong())).thenReturn(Optional.of(info));
    }

    public MarketLoyaltyClient getMarketLoyaltyClientMock() {
        return mock(RestMarketLoyaltyClient.class);
    }

    public ComplicatedMonitoring getComplicatedMonitoring() {
        return mock(ComplicatedMonitoring.class);
    }

    public HttpClient getQaHttpClientMock() throws Exception {
        HttpClient mock = mock(HttpClient.class);
        initQaHttpClient(mock);
        return mock;
    }

    public void initQaHttpClient(HttpClient httpClientMock) throws IOException {
        HttpResponse responseMock = getHttpResponseMock("{\"count\": 0}", 200);
        when(httpClientMock.execute(argThat(withPath("/question/model/\\d+/count"))))
                .thenReturn(responseMock);
    }

    public ReportService getReportService() {
        ReportService reportService = mock(ReportService.class);
        Model model = new Model();
        model.setSlug("slug");
        model.setName("name");
        model.setPictureUrl("http://yandex.ru");

        when(reportService.getModelById(anyLong())).thenReturn(Optional.of(model));
        return reportService;
    }

    public TvmClient getMockTvmClient() {
        return mock(TvmClient.class);
    }

    public UserInfoService getUserInfoServiceMock() {
        UserInfoService result = mock(UserInfoService.class);
        final SberlogInfo info = mock(SberlogInfo.class);
        when(info.getUid()).thenReturn(Uid.ofSberlog(SBER_ID));
        when(info.getFirstName()).thenReturn(Optional.of("first_name_sber_id"));
        when(info.getFatherName()).thenReturn(Optional.empty());
        when(info.getLastName()).thenReturn(Optional.of("last_name_sber_id"));
        when(info.getSex()).thenReturn(Optional.of(Sex.UNKNOWN));
        when(info.getBirthDate()).thenReturn(Optional.empty());
        when(info.getEmails()).thenReturn(Arrays.asList("first@email.com", "second@email.com"));
        when(result.getUserInfoRaw(anyCollectionOf(Long.class), any(Options.class)))
            .thenReturn(Result.ofValue(Collections.singletonList(new AggregateUserInfo(info))));
        return result;
    }
}
