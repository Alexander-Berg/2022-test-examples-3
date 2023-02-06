package ru.yandex.mail.things.utils;

import ru.yandex.mail.common.credentials.UserCredentials;
import ru.yandex.mail.common.rules.BeforeAfterOptionalRule;
import ru.yandex.mail.tests.hound.HoundApi;
import ru.yandex.mail.tests.hound.HoundProperties;
import ru.yandex.mail.tests.hound.HoundResponses;
import ru.yandex.mail.tests.hound.Labels;
import ru.yandex.mail.tests.mops.Mops;
import ru.yandex.mail.tests.mops.MopsResponses;
import ru.yandex.qatools.allure.annotations.Step;

import java.util.List;

import static ru.yandex.mail.common.api.CommonApiSettings.shouldBe;
import static ru.yandex.mail.common.properties.CoreProperties.props;

public class DeleteLabelsMopsRule extends BeforeAfterOptionalRule<DeleteLabelsMopsRule> {
    private UserCredentials rule;

    public DeleteLabelsMopsRule(UserCredentials rule) {
        this.rule = rule;
    }

    @Override
    protected void after() {
        call();
    }

    @Step("[RULE]: Удаляем пользовательские метки")
    @Override
    public void call() {
        try {
            List<String> lids = Labels.labels(
                    HoundApi.apiHound(HoundProperties.properties().houndUri(), props().getCurrentRequestId()).labels()
                            .withUid(this.rule.account().uid())
                            .post(shouldBe(HoundResponses.ok200()))
            ).userLids();

            for (String lid : lids) {
                Mops.deleteLabel(rule, lid).post(shouldBe(MopsResponses.ok()));
            }
        } catch (Exception e) {
            throw new RuntimeException("Невозможно выполнить удаление меток", e);
        }
    }
}
