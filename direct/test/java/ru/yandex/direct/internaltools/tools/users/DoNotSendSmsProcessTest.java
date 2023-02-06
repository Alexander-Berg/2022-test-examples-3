package ru.yandex.direct.internaltools.tools.users;

import java.util.Set;

import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.user.repository.UserRepository;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.internaltools.configuration.InternalToolsTest;
import ru.yandex.direct.internaltools.tools.users.model.DoNotSendSmsAction;
import ru.yandex.direct.internaltools.tools.users.model.DoNotSendSmsInput;

import static ru.yandex.direct.core.entity.user.model.UsersOptionsOptsValues.DO_NOT_SEND_SMS;
import static ru.yandex.direct.core.entity.user.model.UsersOptionsOptsValues.FOLD_INFOBLOCK;
import static ru.yandex.direct.core.entity.user.model.UsersOptionsOptsValues.NOTIFY_ABOUT_NEW_DOMAINS;

@InternalToolsTest
@RunWith(SpringJUnit4ClassRunner.class)
public class DoNotSendSmsProcessTest {
    public static final String FORBIDDEN = "запрещена";
    public static final String ALLOWED = "разрешена";
    @Autowired
    private DoNotSendSmsTool tool;

    @Autowired
    private Steps steps;

    @Autowired
    private UserRepository repository;

    private UserInfo user1;
    private UserInfo user2;

    @Before
    public void setup() {
        user1 = steps.userSteps().createDefaultUser();
        user2 = steps.userSteps().createDefaultUser();
    }

    @Test
    public void forbidByUid() {
        repository.setOpts(user1.getShard(), user1.getUid(), Set.of(FOLD_INFOBLOCK));
        var input = new DoNotSendSmsInput()
                .withAction(DoNotSendSmsAction.FORBID)
                .withUid(user1.getUid());
        var result = tool.process(input);

        var actualForUser1 = repository.getOpts(user1.getShard(), user1.getUid());
        var actualForUser2 = repository.getOpts(user2.getShard(), user2.getUid());
        var softly = new SoftAssertions();
        softly.assertThat(actualForUser1)
                .isEqualTo(Set.of(DO_NOT_SEND_SMS, FOLD_INFOBLOCK));
        softly.assertThat(result.getMessage())
                .contains(FORBIDDEN);
        softly.assertThat(actualForUser2)
                .doesNotContain(DO_NOT_SEND_SMS);
        softly.assertAll();
    }

    // потому что в реальности падало, когда пытался добавить значение в Collections.emptySet()
    @Test
    public void forbidByLoginFromEmptySet() {
        repository.setOpts(user1.getShard(), user1.getUid(), Set.of());
        var input = new DoNotSendSmsInput()
                .withAction(DoNotSendSmsAction.FORBID)
                .withLogin(user1.getLogin());
        var result = tool.process(input);

        var actualForUser1 = repository.getOpts(user1.getShard(), user1.getUid());
        var actualForUser2 = repository.getOpts(user2.getShard(), user2.getUid());
        var softly = new SoftAssertions();
        softly.assertThat(actualForUser1)
                .isEqualTo(Set.of(DO_NOT_SEND_SMS));
        softly.assertThat(result.getMessage())
                .contains("запрещена");
        softly.assertThat(actualForUser2)
                .doesNotContain(DO_NOT_SEND_SMS);
        softly.assertAll();
    }

    @Test
    public void allowByLogin() {
        repository.setOpts(user1.getShard(), user1.getUid(), Set.of(DO_NOT_SEND_SMS, NOTIFY_ABOUT_NEW_DOMAINS));
        repository.setOpts(user2.getShard(), user2.getUid(), Set.of(DO_NOT_SEND_SMS, NOTIFY_ABOUT_NEW_DOMAINS));
        var input = new DoNotSendSmsInput()
                .withAction(DoNotSendSmsAction.ALLOW)
                .withLogin(user2.getLogin());
        var result = tool.process(input);

        var actualForUser1 = repository.getOpts(user1.getShard(), user1.getUid());
        var actualForUser2 = repository.getOpts(user2.getShard(), user2.getUid());
        var softly = new SoftAssertions();
        softly.assertThat(actualForUser2)
                .isEqualTo(Set.of(NOTIFY_ABOUT_NEW_DOMAINS));
        softly.assertThat(result.getMessage())
                .contains(ALLOWED);
        softly.assertThat(actualForUser1)
                .isEqualTo(Set.of(DO_NOT_SEND_SMS, NOTIFY_ABOUT_NEW_DOMAINS));
        softly.assertAll();
    }

    // проверяем, что чтение не пишет
    @Test
    public void readByUid() {
        repository.setOpts(user1.getShard(), user1.getUid(), Set.of(DO_NOT_SEND_SMS, NOTIFY_ABOUT_NEW_DOMAINS));
        repository.setOpts(user2.getShard(), user2.getUid(), Set.of(NOTIFY_ABOUT_NEW_DOMAINS));
        var input1 = new DoNotSendSmsInput()
                .withAction(DoNotSendSmsAction.READ)
                .withLogin(user1.getLogin());
        var result1 = tool.process(input1);
        var input2 = new DoNotSendSmsInput()
                .withAction(DoNotSendSmsAction.READ)
                .withLogin(user2.getLogin());
        var result2 = tool.process(input2);

        var actualForUser1 = repository.getOpts(user1.getShard(), user1.getUid());
        var actualForUser2 = repository.getOpts(user2.getShard(), user2.getUid());
        var softly = new SoftAssertions();
        softly.assertThat(actualForUser1)
                .isEqualTo(Set.of(DO_NOT_SEND_SMS, NOTIFY_ABOUT_NEW_DOMAINS));
        softly.assertThat(result1.getMessage())
                .contains(FORBIDDEN);
        softly.assertThat(actualForUser2)
                .isEqualTo(Set.of(NOTIFY_ABOUT_NEW_DOMAINS));
        softly.assertThat(result2.getMessage())
                .contains(ALLOWED);
        softly.assertAll();
    }
}
