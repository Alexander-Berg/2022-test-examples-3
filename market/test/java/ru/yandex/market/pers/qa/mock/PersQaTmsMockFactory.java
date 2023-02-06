package ru.yandex.market.pers.qa.mock;

import java.time.DayOfWeek;
import java.util.Optional;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.client.RestTemplate;

import ru.yandex.bolts.collection.impl.EmptyMap;
import ru.yandex.common.framework.user.UserInfoField;
import ru.yandex.common.framework.user.blackbox.BlackBoxService;
import ru.yandex.common.framework.user.blackbox.BlackBoxUserInfo;
import ru.yandex.market.cataloger.CatalogerClient;
import ru.yandex.market.common.mds.s3.client.service.api.NamedHistoryMdsS3Client;
import ru.yandex.market.core.expimp.storage.QueryToStorageExtractor;
import ru.yandex.market.pers.qa.client.st.StartrekClient;
import ru.yandex.market.pers.qa.tms.export.yt.DayOfWeekProvider;
import ru.yandex.market.pers.test.common.PersTestMocksHolder;
import ru.yandex.market.pers.yt.YtClient;
import ru.yandex.market.pers.yt.YtClientMocks;
import ru.yandex.market.pers.yt.YtClusterType;
import ru.yandex.market.pers.yt.tm.TransferManagerClient;
import ru.yandex.market.shopinfo.ShopInfoService;
import ru.yandex.market.telegram.TelegramBotClient;
import ru.yandex.market.telegram.TelegramResponse;
import ru.yandex.startrek.client.model.Issue;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

public class PersQaTmsMockFactory {

    public static void resetMocks() {
        PersTestMocksHolder.resetMocks();
    }

    public static YtClient ytClientHahnMock() {
        return PersTestMocksHolder.registerMock(YtClient.class, result-> {
            YtClientMocks.baseMock(YtClusterType.HAHN, result);
        });
    }

    public static YtClient ytClientArnoldMock() {
        return PersTestMocksHolder.registerMock(YtClient.class, result-> {
            YtClientMocks.baseMock(YtClusterType.ARNOLD, result);
        });
    }

    public static JdbcTemplate yqlJdbcTemplateMock() {
        return PersTestMocksHolder.registerMock(JdbcTemplate.class);
    }

    public static TelegramBotClient telegramBotClientMock() {
        return PersTestMocksHolder.registerMock(TelegramBotClient.class, PersQaTmsMockFactory::initializeTelegramBot);
    }

    private static void initializeTelegramBot(TelegramBotClient client) {
        final TelegramResponse mockResponse = new TelegramResponse();
        mockResponse.setOk(true);
        when(client.sendBotMessage(any(), any())).thenReturn(mockResponse);
    }

    public static DayOfWeekProvider dayOfWeekProvider() {
        return PersTestMocksHolder.registerMock(DayOfWeekProvider.class, PersQaTmsMockFactory::initializeDayOfWeekProvider);
    }

    private static void initializeDayOfWeekProvider(DayOfWeekProvider dayOfWeekProvider) {
        when(dayOfWeekProvider.getCurrentDayOfWeek()).thenReturn(DayOfWeek.MONDAY);
    }

    public static StartrekClient startrekClientMock() {
        return PersTestMocksHolder.registerMock(StartrekClient.class, PersQaTmsMockFactory::initializeStartrekClient);
    }

    private static void initializeStartrekClient(StartrekClient client) {
        Issue ticket = new Issue(
            UUID.randomUUID().toString(),
            null,
            "key",
            "summary",
            1,
            new EmptyMap<>(),
            null);

        when(client.openTicket(any())).thenReturn(ticket);
    }

    public static RestTemplate mboCmsRestTemplate() {
        return PersTestMocksHolder.registerMock(RestTemplate.class, PersQaTmsMockFactory::initializeRestTemplate);
    }

    private static void initializeRestTemplate(RestTemplate restTemplate) {

    }

    public static BlackBoxService blackBoxMock() {
        return PersTestMocksHolder.registerMock(BlackBoxService.class, PersQaTmsMockFactory::initializeBlackBox);
    }

    private static void initializeBlackBox(BlackBoxService source) {
        try {
            BlackBoxUserInfo value = new BlackBoxUserInfo(1);
            value.addField(UserInfoField.LOGIN, null);
            when(source.getUserInfo(anyLong(), any())).thenReturn(value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static ShopInfoService shopInfoServiceMock() {
        return PersTestMocksHolder.registerMock(ShopInfoService.class, PersQaTmsMockFactory::initializeShopInfoService);
    }

    private static void initializeShopInfoService(ShopInfoService source) {
        try {
            when(source.getShopInfo(anyLong())).thenReturn(Optional.empty());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static RestTemplate saasPushRestTemplateMock() {
        return PersTestMocksHolder.registerMock(RestTemplate.class, PersQaTmsMockFactory::initializeSaasPushRestTemplate);
    }

    private static void initializeSaasPushRestTemplate(RestTemplate source) {
        try {
            when(source.postForEntity(anyString(), any(), any())).thenReturn(ResponseEntity.ok().build());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static CatalogerClient catalogerClientMock() {
        return PersTestMocksHolder.registerMock(CatalogerClient.class, PersQaTmsMockFactory::initializeCatalogerClient);
    }

    private static void initializeCatalogerClient(CatalogerClient source) {
        try {
            when(source.getBrandName(anyLong())).thenReturn(Optional.empty());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static QueryToStorageExtractor queryToStorageExtractorMock() {
        return PersTestMocksHolder.registerMock(QueryToStorageExtractor.class);
    }

    public static NamedHistoryMdsS3Client namedHistoryMdsS3ClientMock() {
        return PersTestMocksHolder.registerMock(NamedHistoryMdsS3Client.class);
    }

    public static TransferManagerClient transferManagerClientMock() {
        return PersTestMocksHolder.registerMock(TransferManagerClient.class);
    }
}
