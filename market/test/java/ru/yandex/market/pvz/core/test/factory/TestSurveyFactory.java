package ru.yandex.market.pvz.core.test.factory;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import lombok.Builder;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.pvz.core.domain.survey.SurveyCommandService;
import ru.yandex.market.pvz.core.domain.survey.SurveyParams;
import ru.yandex.market.pvz.core.test.factory.mapper.SurveyTestParamsMapper;

public class TestSurveyFactory {

    @Autowired
    private SurveyTestParamsMapper surveyTestParamsMapper;

    @Autowired
    private SurveyCommandService surveyCommandService;

    public SurveyParams create() {
        return create(SurveyTestParams.builder().build());
    }

    public SurveyParams create(SurveyTestParams surveyTestParams) {
        SurveyParams surveyParams = surveyTestParamsMapper.map(surveyTestParams);
        return surveyCommandService.create(surveyParams);
    }

    @Data
    @Builder
    public static class SurveyTestParams {
        public static final String DEFAULT_TITLE = "Тип сообщения";
        public static final String DEFAULT_BODY = "Вам нужно пройти опрос, ссылка ниже";
        public static final String DEFAULT_SHORT_DESCRIPTION = "Показывается в админке ЛМС";
        public static final String DEFAULT_URL = "https://forms.test.yandex-team.ru";
        public static final String DEFAULT_FREQUENCY = "* * * * * ? *";
        public static final LocalDate DEFAULT_START_DATE = LocalDate.of(2022, 4, 13);
        public static final LocalDate DEFAULT_END_DATE = LocalDate.of(2022, 4, 13);
        public static final List<String> DEFAULT_CAMPAIGN_FEATURES = Collections.emptyList();
        public static final List<Long> DEFAULT_CAMPAIGN_IDS = Collections.emptyList();

        @Builder.Default
        private String title = DEFAULT_TITLE;

        @Builder.Default
        private String body = DEFAULT_BODY;

        @Builder.Default
        private String shortDescription = DEFAULT_SHORT_DESCRIPTION;

        @Builder.Default
        private String url = DEFAULT_URL;

        @Builder.Default
        private String frequency = DEFAULT_FREQUENCY;

        @Builder.Default
        private LocalDate startDate = DEFAULT_START_DATE;

        @Builder.Default
        private LocalDate endDate = DEFAULT_END_DATE;

        @Builder.Default
        private List<String> campaignFeatures = DEFAULT_CAMPAIGN_FEATURES;

        @Builder.Default
        private List<Long> campaignIds = DEFAULT_CAMPAIGN_IDS;

        private boolean surveyByPickupPoint;

    }

}
