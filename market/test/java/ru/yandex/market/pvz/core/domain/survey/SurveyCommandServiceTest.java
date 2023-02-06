package ru.yandex.market.pvz.core.domain.survey;

import java.time.LocalDate;
import java.util.Collections;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.pvz.core.test.TransactionlessEmbeddedDbTest;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@TransactionlessEmbeddedDbTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class SurveyCommandServiceTest {

    private final TestableClock clock;
    private final SurveyCommandService surveyCommandService;

    @Test
    void whenCreateThenSuccess() {
        SurveyParams surveyParams = SurveyParams.builder()
                .title("Пройдите опрос")
                .body("Вам нужно пройти опрос, ссылка ниже")
                .shortDescription("Показывается в админке ЛМС")
                .url("Ссылка на ФОС")
                .frequency("* * * * * ? *")
                .startDate(LocalDate.now(clock))
                .endDate(LocalDate.now(clock))
                .campaignFeatures(Collections.emptyList())
                .campaignIds(Collections.emptyList())
                .surveyByPickupPoint(true)
                .build();
        SurveyParams result = surveyCommandService.create(surveyParams);
        assertThat(result).isEqualToIgnoringGivenFields(surveyParams, "id");
    }

}
