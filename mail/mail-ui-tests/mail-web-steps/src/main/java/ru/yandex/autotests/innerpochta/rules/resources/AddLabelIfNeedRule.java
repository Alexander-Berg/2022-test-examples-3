package ru.yandex.autotests.innerpochta.rules.resources;

import org.glassfish.jersey.internal.util.Producer;
import org.junit.rules.ExternalResource;
import ru.yandex.autotests.innerpochta.steps.AllureStepStorage;
import ru.yandex.autotests.innerpochta.steps.beans.label.Label;
import ru.yandex.autotests.innerpochta.util.Utils;

import static ru.yandex.autotests.innerpochta.util.handlers.LabelsConstants.LABELS_PARAM_GREEN_COLOR;

/**
 * @author cosmopanda on 23.07.2016.
 */
public class AddLabelIfNeedRule extends ExternalResource {

    private AllureStepStorage user;
    private Producer<AllureStepStorage> producer;

    private AddLabelIfNeedRule(Producer<AllureStepStorage> producer) {
        this.producer = producer;
    }

    public static AddLabelIfNeedRule addLabelIfNeed(Producer<AllureStepStorage> userProducer) {
        return new AddLabelIfNeedRule(userProducer);
    }

    public Label getFirstLabel() {
        return user.apiLabelsSteps().getAllUserLabels().get(0);
    }

    @Override
    protected void before() throws Throwable {
        user = producer.call();
        if (user.apiLabelsSteps().getAllUserLabels().isEmpty())
            user.apiLabelsSteps().addNewLabel(Utils.getRandomString(), LABELS_PARAM_GREEN_COLOR);
    }
}
