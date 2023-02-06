package ru.yandex.market.adv.content.manager.interactor.moderation.machine;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import ru.yandex.market.adv.content.manager.AbstractContentManagerTest;
import ru.yandex.market.adv.yt.test.annotation.YtUnitDataSet;
import ru.yandex.market.adv.yt.test.annotation.YtUnitScheme;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.tms.quartz2.model.Executor;
import ru.yandex.uw.moderation.model.ModerationRequestRecord;

/**
 * Date: 07.10.2021
 * Project: adv-content-manager
 *
 * @author alexminakov
 */
@SuppressWarnings("checkstyle:lineLength")
class ModerationTaskSentToModeratedYTInteractorTest extends AbstractContentManagerTest {

    @Autowired
    @Qualifier("tmsModerationTaskSentToModeratedExecutor")
    private Executor tmsModerationTaskSentToModeratedExecutor;

    @DisplayName("Проверка работоспособности job tmsModerationTaskSentToModeratedExecutor.")
    @DbUnitDataSet(
            before = "ModerationTaskSentToModeratedYTInteractor/csv/" +
                    "tmsModerationTaskSentToModeratedExecutor_fourModerationTask_completeTwoTasksAndOneException.before.csv",
            after = "ModerationTaskSentToModeratedYTInteractor/csv/" +
                    "tmsModerationTaskSentToModeratedExecutor_fourModerationTask_completeTwoTasksAndOneException.after.csv"
    )
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = ModerationRequestRecord.class,
                    path = "//tmp/uw-moderation-request-523_3123_5132_BUSINESS-moderated",
                    isDynamic = false
            ),
            exist = false,
            before = "ModerationTaskSentToModeratedYTInteractor/json/yt/moderationRequestRecord/result/" +
                    "523_3123_5132_BUSINESS.before.json"
    )
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = ModerationRequestRecord.class,
                    path = "//tmp/uw-moderation-request-523_3123_5132_BUSINESS",
                    isDynamic = false
            ),
            exist = false,
            before = "ModerationTaskSentToModeratedYTInteractor/json/yt/moderationRequestRecord/request/" +
                    "523_3123_5132_BUSINESS.before.json"
    )
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = ModerationRequestRecord.class,
                    path = "//tmp/uw-moderation-request-423_4123_5232_EXPRESS-moderated",
                    isDynamic = false
            ),
            exist = false,
            before = "ModerationTaskSentToModeratedYTInteractor/json/yt/moderationRequestRecord/result/" +
                    "423_4123_5232_EXPRESS.before.json"
    )
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = ModerationRequestRecord.class,
                    path = "//tmp/uw-moderation-request-427_4124_5233_BUSINESS-moderated",
                    isDynamic = false
            ),
            before = "ModerationTaskSentToModeratedYTInteractor/json/yt/moderationRequestRecord/result/" +
                    "427_4124_5233_BUSINESS.before.json",
            after = "ModerationTaskSentToModeratedYTInteractor/json/yt/moderationRequestRecord/result/" +
                    "427_4124_5233_BUSINESS.after.json"
    )
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = ModerationRequestRecord.class,
                    path = "//tmp/uw-moderation-request-427_4124_5233_BUSINESS",
                    isDynamic = false
            ),
            before = "ModerationTaskSentToModeratedYTInteractor/json/yt/moderationRequestRecord/request/" +
                    "427_4124_5233_BUSINESS.before.json",
            after = "ModerationTaskSentToModeratedYTInteractor/json/yt/moderationRequestRecord/request/" +
                    "427_4124_5233_BUSINESS.after.json"
    )
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = ModerationRequestRecord.class,
                    path = "//tmp/uw-moderation-request-443_4123_5632_BUSINESS-moderated",
                    isDynamic = false
            )
    )
    @Test
    public void tmsModerationTaskSentToModeratedExecutor_fourModerationTask_completeTwoTasksAndOneException() {
        Assertions.assertThatThrownBy(() -> tmsModerationTaskSentToModeratedExecutor.doJob(mockContext()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Wrong size of result table for task with id 427_4124_5233_BUSINESS. " +
                        "Expected: 2. Actual: 1")
                .hasSuppressedException(
                        new IllegalStateException(
                                "Table //tmp/uw-moderation-request-443_4123_5632_BUSINESS-moderated cannot be empty"
                        )
                );
    }
}
