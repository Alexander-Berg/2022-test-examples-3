package ru.yandex.autotests.innerpochta.rules.resources;

import org.glassfish.jersey.internal.util.Producer;
import org.junit.rules.ExternalResource;
import ru.yandex.autotests.innerpochta.objstruct.base.misc.Account;
import ru.yandex.autotests.innerpochta.steps.AllureStepStorage;
import ru.yandex.autotests.innerpochta.steps.beans.message.Message;
import ru.yandex.autotests.innerpochta.util.Utils;

/**
 * @author cosmopanda on 28.07.2016.
 */
public class AddMessageIfNeedRule extends ExternalResource {

    private AllureStepStorage user;
    private Producer<AllureStepStorage> producer;
    private Producer<Account> accProducer;

    private AddMessageIfNeedRule(Producer<AllureStepStorage> producer, Producer<Account> accProducer) {
        this.producer = producer;
        this.accProducer = accProducer;
    }

    public static AddMessageIfNeedRule addMessageIfNeed(Producer<AllureStepStorage> producer,
                                                        Producer<Account> accProducer) {
        return new AddMessageIfNeedRule(producer, accProducer);
    }

    public Message getFirstMessage() {
        return user.apiMessagesSteps().getAllMessages().get(0);
    }

    @Override
    protected void before() throws Throwable {
        user = producer.call();
        if (user.apiMessagesSteps().getAllMessages().isEmpty()) {
            user.apiMessagesSteps()
                .sendMailWithNoSave(accProducer.call(), Utils.getRandomName(), Utils.getRandomString());
        }
    }
}
