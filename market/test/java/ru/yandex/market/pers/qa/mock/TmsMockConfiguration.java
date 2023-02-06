package ru.yandex.market.pers.qa.mock;

import java.util.Collections;
import java.util.Map;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.client.RestTemplate;

import ru.yandex.common.framework.user.blackbox.BlackBoxService;
import ru.yandex.market.cataloger.CatalogerClient;
import ru.yandex.market.common.mds.s3.client.service.api.NamedHistoryMdsS3Client;
import ru.yandex.market.core.expimp.storage.QueryToStorageExtractor;
import ru.yandex.market.pers.qa.CoreMockConfiguration;
import ru.yandex.market.pers.qa.client.st.StartrekClient;
import ru.yandex.market.pers.qa.tms.export.yt.DayOfWeekProvider;
import ru.yandex.market.pers.yt.YtClient;
import ru.yandex.market.pers.yt.tm.TransferManagerClient;
import ru.yandex.market.shopinfo.ShopInfoService;
import ru.yandex.market.telegram.TelegramBotClient;

@Configuration
public class TmsMockConfiguration extends CoreMockConfiguration {

    @Bean
    public YtClient ytClientHahn() {
        return PersQaTmsMockFactory.ytClientHahnMock();
    }

    @Bean
    public YtClient ytClientArnold() {
        return PersQaTmsMockFactory.ytClientArnoldMock();
    }

    @Bean
    @Qualifier("yqlJdbcTemplate")
    public JdbcTemplate yqlJdbcTemplate() {
        return PersQaTmsMockFactory.yqlJdbcTemplateMock();
    }

    @Bean
    public TelegramBotClient telegramBotClient() {
        return PersQaTmsMockFactory.telegramBotClientMock();
    }

    @Bean
    public StartrekClient startrekClient() {
        return PersQaTmsMockFactory.startrekClientMock();
    }

    @Bean
    public DayOfWeekProvider dayOfWeekProvider() {
        return PersQaTmsMockFactory.dayOfWeekProvider();
    }

    @Bean
    public BlackBoxService blackBoxService() {
        return PersQaTmsMockFactory.blackBoxMock();
    }

    @Bean
    public ShopInfoService shopInfoService() {
        return PersQaTmsMockFactory.shopInfoServiceMock();
    }

    @Bean
    public CatalogerClient catalogerClient() {
        return PersQaTmsMockFactory.catalogerClientMock();
    }

    @Bean
    @Qualifier("saasPushRestTemplate")
    public RestTemplate saasPushRestTemplate() {
        return PersQaTmsMockFactory.saasPushRestTemplateMock();
    }

    @Bean
    @Qualifier("preservedTablesCountMap")
    Map<String, Integer> preservedTablesCountMap() {
        return Collections.singletonMap("new_questions", 2);
    }

    @Bean
    @Qualifier("mboCmsRestTemplate")
    RestTemplate mboCmsRestTemplate() {
        return PersQaTmsMockFactory.mboCmsRestTemplate();
    }

    @Bean
    QueryToStorageExtractor queryToStorageExtractor() {
        return PersQaTmsMockFactory.queryToStorageExtractorMock();
    }

    @Bean
    NamedHistoryMdsS3Client namedHistoryMdsS3Client() {
        return PersQaTmsMockFactory.namedHistoryMdsS3ClientMock();
    }

    @Bean
    TransferManagerClient transferManagerClient() {
        return PersQaTmsMockFactory.transferManagerClientMock();
    }

}
