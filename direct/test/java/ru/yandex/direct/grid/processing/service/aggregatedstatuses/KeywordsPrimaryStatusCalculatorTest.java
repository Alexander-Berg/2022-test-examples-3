package ru.yandex.direct.grid.processing.service.aggregatedstatuses;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.core.entity.aggregatedstatuses.GdSelfStatusEnum;
import ru.yandex.direct.core.entity.aggregatedstatuses.keyword.AggregatedStatusKeywordData;
import ru.yandex.direct.grid.core.entity.showcondition.model.GdiShowConditionPrimaryStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.entity.aggregatedstatuses.GdSelfStatusReason.ACTIVE;
import static ru.yandex.direct.core.entity.aggregatedstatuses.GdSelfStatusReason.ARCHIVED;
import static ru.yandex.direct.core.entity.aggregatedstatuses.GdSelfStatusReason.DRAFT;
import static ru.yandex.direct.core.entity.aggregatedstatuses.GdSelfStatusReason.KEYWORD_SUSPENDED_BY_USER;
import static ru.yandex.direct.core.entity.aggregatedstatuses.GdSelfStatusReason.REJECTED_ON_MODERATION;

@RunWith(Parameterized.class)
public class KeywordsPrimaryStatusCalculatorTest {

    @Parameterized.Parameter
    public String testDescription;
    @Parameterized.Parameter(1)
    public AggregatedStatusKeywordData aggregatedStatusKeywordData;
    @Parameterized.Parameter(2)
    public GdiShowConditionPrimaryStatus expectStatus;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                // Без статуса
                {"null статус конвертируется в DRAFT",
                        null,
                        GdiShowConditionPrimaryStatus.DRAFT},
                {"(null status, ARCHIVED reason) -> DRAFT",
                        new AggregatedStatusKeywordData(null, ARCHIVED),
                        GdiShowConditionPrimaryStatus.DRAFT},
                // Архивные
                {"(ARCHIVED status, ARCHIVED reason) -> ARCHIVED",
                        new AggregatedStatusKeywordData(GdSelfStatusEnum.ARCHIVED, ARCHIVED),
                        GdiShowConditionPrimaryStatus.ARCHIVED},
                // Черновики
                {"(DRAFT status, DRAFT reason) -> DRAFT",
                        new AggregatedStatusKeywordData(GdSelfStatusEnum.DRAFT, DRAFT),
                        GdiShowConditionPrimaryStatus.DRAFT},
                // Отклонена на модерации
                {"(STOP_CRIT status, REJECTED_ON_MODERATION reason) -> MODERATION_REJECTED",
                        new AggregatedStatusKeywordData(GdSelfStatusEnum.STOP_CRIT, REJECTED_ON_MODERATION),
                        GdiShowConditionPrimaryStatus.MODERATION_REJECTED},
                // Остановленные
                {"(STOP_OK status, KEYWORD_SUSPENDED_BY_USER reason) -> STOPPED",
                        new AggregatedStatusKeywordData(GdSelfStatusEnum.STOP_OK, KEYWORD_SUSPENDED_BY_USER),
                        GdiShowConditionPrimaryStatus.STOPPED},
                // Активные
                {"(RUN_OK status, ACTIVE reason) -> ACTIVE",
                        new AggregatedStatusKeywordData(GdSelfStatusEnum.RUN_OK, ACTIVE),
                        GdiShowConditionPrimaryStatus.ACTIVE},
        });
    }

    @Test
    public void convertToRetargetingBaseStatus() {
        GdiShowConditionPrimaryStatus status =
                KeywordsPrimaryStatusCalculator.convertToPrimaryStatus(aggregatedStatusKeywordData);
        assertThat(status).isEqualTo(expectStatus);
    }
}
