package ru.yandex.autotests.innerpochta.testopithecus.lizaapp.features;

import com.yandex.xplat.testopithecus.WriteMessage;
import org.jetbrains.annotations.NotNull;
import ru.yandex.autotests.innerpochta.rules.InitStepsRule;

/**
 * @author pavponn
 */
public class LizaWriteMessage implements WriteMessage {

    private InitStepsRule steps;

    public LizaWriteMessage(InitStepsRule steps) {
        this.steps = steps;
    }

    @Override
    public void sendMessage(@NotNull String to, @NotNull String subject) {
        steps.user().composeSteps()
            .inputsAddressInFieldTo(to)
            .inputsSubject(subject)
            .clicksOnSendButtonInHeader()
            .waitForMessageToBeSend();

    }

    @Override
    public void replyMessage() {
        // TODO: Скорее всего этот метод должен просто отправлять ответ, но кто ж его знает
        steps.user().composeSteps().clicksOnSendButtonInHeader();
    }

    @Override
    public void sendPrepared() {
        steps.user().composeSteps().clicksOnSendButtonInHeader();
    }

    @Override
    public void openCompose() {

    }
}
