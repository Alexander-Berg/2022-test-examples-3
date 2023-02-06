package ru.yandex.autotests.innerpochta.testopithecus.lizaapp.features;

import com.yandex.xplat.testopithecus.WYSIWIG;
import org.jetbrains.annotations.NotNull;
import ru.yandex.autotests.innerpochta.rules.InitStepsRule;

/**
 * @author a-zoshchuk
 */
public class LizaWysiwyg implements WYSIWIG {

    private InitStepsRule steps;

    public LizaWysiwyg(InitStepsRule steps) {
        this.steps = steps;
    }

    @Override
    public void setStrong(int from, int to) {
        steps.user().composeSteps().setTextBold(from, to);
    }

    @Override
    public void setItalic(int from, int to) {
        steps.user().composeSteps().setTextItalic(from, to);
    }

    @Override
    public void clearFormatting(int from, int to) {
        steps.user().composeSteps().clearTextFormatting(from, to);
    }

    @Override
    public void appendText(int index, @NotNull String text) {
        steps.user().composeSteps().appendTextToIndex(index, text);
    }
}
