package ru.yandex.market.periodic_survey;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ru.yandex.market.core.periodic_survey.service.NetPromoterScoreNotificationService;
import ru.yandex.market.core.periodic_survey.yt.LastUserSurveyIdYtDao;
import ru.yandex.market.core.periodic_survey.yt.PeriodicSurveyYtDao;

import static org.mockito.Mockito.mock;

@Configuration
public class PeriodicSurveyTestConfig {
    @Bean
    public PeriodicSurveyYtDao periodicSurveyYtDao() {
        return mock(PeriodicSurveyYtDao.class);
    }

    @Bean
    public LastUserSurveyIdYtDao lastUserSurveyIdYtDao() {
        return mock(LastUserSurveyIdYtDao.class);
    }

    @Bean
    public NetPromoterScoreNotificationService npsNotificationService() {
        return mock(NetPromoterScoreNotificationService.class);
    }

}
