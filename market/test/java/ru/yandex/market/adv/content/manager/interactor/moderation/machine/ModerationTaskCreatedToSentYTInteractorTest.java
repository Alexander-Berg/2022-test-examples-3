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
 * Date: 26.10.2021
 * Project: adv-content-manager
 *
 * @author alexminakov
 */
class ModerationTaskCreatedToSentYTInteractorTest extends AbstractContentManagerTest {

    @Autowired
    @Qualifier("tmsModerationTaskCreatedToSentExecutor")
    private Executor tmsModerationTaskCreatedToSentExecutor;

    @DisplayName("Проверка работоспособности job tmsModerationTaskCreatedToSentExecutor.")
    @DbUnitDataSet(
            before = "ModerationTaskCreatedToSentYTInteractor/csv/" +
                    "tmsModerationTaskCreatedToSentExecutor_threeModerationTask_completeTwoTasksAndOneException" +
                    ".before.csv",
            after = "ModerationTaskCreatedToSentYTInteractor/csv/" +
                    "tmsModerationTaskCreatedToSentExecutor_threeModerationTask_completeTwoTasksAndOneException.after" +
                    ".csv"
    )
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = ModerationRequestRecord.class,
                    path = "//tmp/uw-moderation-request-523_3123_5132_BUSINESS",
                    isDynamic = false
            ),
            create = false,
            after = "ModerationTaskCreatedToSentYTInteractor/json/yt/moderationRequestRecord/" +
                    "523_3123_5132_BUSINESS.after.json"
    )
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = ModerationRequestRecord.class,
                    path = "//tmp/uw-moderation-request-423_4123_5232_EXPRESS",
                    isDynamic = false
            ),
            create = false,
            after = "ModerationTaskCreatedToSentYTInteractor/json/yt/moderationRequestRecord/" +
                    "423_4123_5232_EXPRESS.after.json"
    )
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = ModerationRequestRecord.class,
                    path = "//tmp/uw-moderation-request-443_4123_5732_BUSINESS",
                    isDynamic = false
            ),
            create = false,
            exist = false
    )
    @Test
    void tmsModerationTaskCreatedToSentExecutor_threeModerationTask_completeTwoTasksAndOneException() {
        Assertions.assertThatThrownBy(() -> tmsModerationTaskCreatedToSentExecutor.doJob(mockContext()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Empty moderation content for task with id 443_4123_5732_BUSINESS");
    }
}
