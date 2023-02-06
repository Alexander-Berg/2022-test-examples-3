package ru.yandex.market.tpl.core.service.feedback;

import lombok.RequiredArgsConstructor;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.tpl.api.model.feedback.pro.FeedbackCreateRequestDto;
import ru.yandex.market.tpl.api.model.feedback.pro.FeedbackDto;
import ru.yandex.market.tpl.core.domain.feedback.CourierFeedback;
import ru.yandex.market.tpl.core.domain.feedback.CourierFeedbackRepository;
import ru.yandex.market.tpl.core.domain.feedback.CourierFeedbackService;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.test.TplAbstractTest;

@RequiredArgsConstructor
public class CourierFeedbackServiceTest extends TplAbstractTest {
    private final CourierFeedbackService courierFeedbackService;
    private final TestUserHelper testUserHelper;
    private final CourierFeedbackRepository courierFeedbackRepository;

    @Test
    void saveCourierFeedbackByPro() {
        User user = testUserHelper.findOrCreateUser(7586398756L);
        float rating = 5.34f;
        String appVersion = "17";
        String comment = "goood";
        FeedbackDto result = courierFeedbackService.saveCourierFeedback(new FeedbackCreateRequestDto(
                rating, appVersion, comment
        ), user);

        Assertions.assertThat(result.getComment()).isEqualTo(comment);
        Assertions.assertThat(result.getRating()).isEqualTo(rating);

        CourierFeedback feedback =
                courierFeedbackRepository.getAllByUserId(user.getId()).stream().findAny().orElseThrow();
        Assertions.assertThat(feedback.getComment()).isEqualTo(comment);
        Assertions.assertThat(feedback.getRating()).isEqualTo(rating);
        Assertions.assertThat(feedback.getAppVersion()).isEqualTo(appVersion);
    }
}
