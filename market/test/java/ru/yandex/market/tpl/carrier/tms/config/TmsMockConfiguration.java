package ru.yandex.market.tpl.carrier.tms.config;

import java.time.Clock;

import com.pengrad.telegrambot.TelegramBot;
import org.mockito.Answers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.market.logistics.les.client.producer.LesProducer;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.tms.quartz2.group.GroupManager;
import ru.yandex.market.tms.quartz2.util.QrtzLogTableCleaner;
import ru.yandex.market.tpl.carrier.core.adapter.ConfigurationProviderAdapter;
import ru.yandex.market.tpl.carrier.core.domain.run.RunRepository;
import ru.yandex.market.tpl.carrier.core.domain.usershift.RoutePointRepository;
import ru.yandex.market.tpl.carrier.core.domain.usershift.UserShiftRepository;
import ru.yandex.market.tpl.carrier.tms.executor.telegram.RunNotAssignedTelegramNotificationsExecutor;
import ru.yandex.market.tpl.carrier.tms.executor.telegram.ShiftNotStartedTelegramNotificationsExecutor;
import ru.yandex.market.tpl.carrier.tms.executor.telegram.TelegramUserHaveNotTapMovingButtonNotificationExecutor;
import ru.yandex.market.tpl.carrier.tms.service.telegram.TelegramBotUpdateHandler;
import ru.yandex.market.tpl.common.maps.client.MapsClient;
import ru.yandex.market.tpl.common.web.monitoring.juggler.JugglerClient;
import ru.yandex.market.tpl.common.xiva.send.XivaSendTvmClient;
import ru.yandex.mj.generated.client.taxi_client_notify.api.TaxiClientNotifyApiClient;
import ru.yandex.mj.generated.client.taxi_driver_trackstory.api.TaxiDriverTrackstoryApiClient;
import ru.yandex.mj.generated.client.taximeter_v2.api.TaximeterV2ApiClient;

@MockBean(
        classes = {
                XivaSendTvmClient.class,
                LMSClient.class,
                QrtzLogTableCleaner.class,
                GroupManager.class,
                JugglerClient.class,
                TelegramBot.class,
                MapsClient.class,
                TaximeterV2ApiClient.class,
                TaxiClientNotifyApiClient.class,
                TaxiDriverTrackstoryApiClient.class,
                Yt.class,
        },
        answer = Answers.RETURNS_DEEP_STUBS
)
@Configuration
public class TmsMockConfiguration {

    @Bean
    public TelegramBotUpdateHandler telegramBotUpdateHandlerMock() {
        TelegramBotUpdateHandler telegramBotUpdateHandlerMock = Mockito.mock(TelegramBotUpdateHandler.class);

        Mockito.doReturn("Mocked bean").when(telegramBotUpdateHandlerMock).toString();
        return telegramBotUpdateHandlerMock;
    }

    @Bean
    public RunNotAssignedTelegramNotificationsExecutor runNotAssignedTelegramNotificationsExecutorMock(
            RunRepository runRepository,
            @Qualifier("telegramBotUpdateHandlerMock") TelegramBotUpdateHandler telegramBotUpdateHandler,
            Clock clock,
            ConfigurationProviderAdapter configurationProviderAdapter) {

        return new RunNotAssignedTelegramNotificationsExecutor(runRepository, telegramBotUpdateHandler,
                configurationProviderAdapter, clock);
    }

    @Bean
    public TelegramUserHaveNotTapMovingButtonNotificationExecutor telegramUserHaveNotTapMovingButtonExecutorMock(
            RunRepository runRepository,
            RoutePointRepository routePointRepository,
            @Qualifier("telegramBotUpdateHandlerMock") TelegramBotUpdateHandler telegramBotUpdateHandler,
            TransactionTemplate transactionTemplate,
            Clock clock,
            ConfigurationProviderAdapter configurationProviderAdapter) {

        return new TelegramUserHaveNotTapMovingButtonNotificationExecutor(
                clock,
                routePointRepository,
                configurationProviderAdapter,
                transactionTemplate,
                telegramBotUpdateHandler,
                runRepository
        );
    }

    @Bean
    public ShiftNotStartedTelegramNotificationsExecutor shiftNotStartedTelegramNotificationsExecutorMock(
            UserShiftRepository userShiftRepository,
            RunRepository runRepository,
            @Qualifier("telegramBotUpdateHandlerMock") TelegramBotUpdateHandler telegramBotUpdateHandler,
            TransactionTemplate transactionTemplate,
            Clock clock,
            ConfigurationProviderAdapter configurationProviderAdapter) {

        return new ShiftNotStartedTelegramNotificationsExecutor(
                userShiftRepository,
                runRepository,
                telegramBotUpdateHandler,
                transactionTemplate,
                configurationProviderAdapter,
                clock);
    }

    @Bean
    public LesConfiguration lesConfiguration() {
        return new LesConfiguration();
    }

    @Bean
    public LesProducer lesProducer() {
        return Mockito.mock(LesProducer.class);
    }


}
