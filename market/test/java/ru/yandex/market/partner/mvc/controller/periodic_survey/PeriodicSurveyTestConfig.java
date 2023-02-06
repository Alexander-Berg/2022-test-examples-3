package ru.yandex.market.partner.mvc.controller.periodic_survey;

import org.springframework.context.annotation.Bean;

import ru.yandex.market.core.periodic_survey.yt.PeriodicSurveyYtDao;

import static org.mockito.Mockito.mock;

public class PeriodicSurveyTestConfig {
    @Bean
    public PeriodicSurveyYtDao periodicSurveyYtDao() {
        return mock(PeriodicSurveyYtDao.class);
    }
}
