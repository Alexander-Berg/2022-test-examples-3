package ru.yandex.autotests.innerpochta.rules.resources;

import org.junit.rules.ExternalResource;
import ru.yandex.autotests.innerpochta.steps.AllureStepStorage;
import ru.yandex.autotests.innerpochta.steps.beans.label.Label;

import static ch.lambdaj.Lambda.having;
import static ch.lambdaj.Lambda.on;
import static ch.lambdaj.Lambda.selectFirst;
import static org.hamcrest.CoreMatchers.equalTo;

/**
 * @author mabelpines
 */
public class UnPinAllMessagesRule extends ExternalResource {
    private AllureStepStorage user;

    private UnPinAllMessagesRule(AllureStepStorage user) {
        this.user = user;
    }

    public static UnPinAllMessagesRule unPinAllMessagesRule(AllureStepStorage user) {
        return new UnPinAllMessagesRule(user);
    }

    @Override
    protected void before() throws Throwable {
        Label pinLabel = selectFirst(
            user.apiLabelsSteps().getAllLabels(),
            having(on(Label.class).getSymbolicName(), equalTo("pinned_label"))
        );
        user.apiMessagesSteps().getAllMessagesLabel(pinLabel.getName())
            .forEach(msg -> user.apiLabelsSteps().unPinLetter(msg));
    }
}
