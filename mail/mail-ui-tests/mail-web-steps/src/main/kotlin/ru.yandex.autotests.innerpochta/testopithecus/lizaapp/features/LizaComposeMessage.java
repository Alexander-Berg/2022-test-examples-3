package ru.yandex.autotests.innerpochta.testopithecus.lizaapp.features;

import com.yandex.xplat.common.YSSet;
import com.yandex.xplat.testopithecus.ComposeMessage;
import com.yandex.xplat.testopithecus.DraftView;
import com.yandex.xplat.testopithecus.WysiwygView;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.interactions.Actions;
import ru.yandex.autotests.innerpochta.rules.InitStepsRule;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.collection.IsEmptyCollection.empty;

/**
 * @author pavponn
 */
public class LizaComposeMessage implements ComposeMessage {

    private InitStepsRule steps;

    public LizaComposeMessage(InitStepsRule steps) {
        this.steps = steps;
    }

    @Override
    public void goToMessageReply() {
        steps.user().defaultSteps()
            .clicksOn(steps.pages().mail().msgView().toolbar().replyButton())
            .shouldSee(steps.pages().mail().compose().composeFieldsBlock().fieldTo());
    }

    @Override
    public void addTo(@NotNull String to) {
        steps.user().composeSteps().inputsAddressInFieldTo(to);
        new Actions(steps.getDriver()).sendKeys(Keys.ENTER).perform();
    }

    @Override
    public void removeTo(int order) {
        steps.user().defaultSteps()
            .clicksOn(
                steps.pages().mail().compose().composeFieldsBlock().yabbleToList().waitUntil(not(empty())).get(order)
            )
            .clicksOn(steps.pages().mail().compose().composeYabbleDropdown().removeFromRecipients());
    }

    @Override
    public void setSubject(@NotNull String subject) {
        steps.user().composeSteps().inputsSubject(subject);
    }

    @Override
    public void clearSubject() {
        steps.user().composeSteps().clearInputsSubjectField();
    }

    @Override
    public void setBody(@NotNull String body) {
        steps.user().composeSteps().inputsSendText(body);
    }

    @Override
    public void clearBody() {
        steps.user().composeSteps().clearInputsSendTextField();
    }

    @NotNull
    @Override
    public YSSet<String> getTo() {
        return new YSSet<>(
            steps.pages().mail().compose().composeFieldsBlock().yabbleToList().stream()
                .map(MailElement::getText)
                .collect(Collectors.toSet())
        );
    }

    @NotNull
    @Override
    public DraftView getDraft() {
        return new DraftView() {

            @NotNull
            @Override
            public WysiwygView getWysiwyg() {
                return new WysiwygView() {
                    @NotNull
                    @Override
                    public String getText() {
                        return steps.pages().mail().compose().textareaBlock().formattedText().getText();
                    }

                    @NotNull
                    @Override
                    public YSSet<String> getStyles(int i) {
                        return LizaComposeMessage.this.getStyles(getRichBody(), i);
                    }

                    @NotNull
                    @Override
                    public String getRichBody() {
                            return ((String) ((JavascriptExecutor) steps.getDriver()).executeScript(
                                "return arguments[0].innerHTML",
                                steps.pages().mail().compose().textareaBlock().formattedTextLines().get(0).getWrappedElement()
                            )).replaceFirst("<br>", "").replaceAll("&lt;", "<").replaceAll("&gt;", ">");
                    }
                };
            }

            @NotNull
            @Override
            public String tostring() {
                return String.format(
                    "body=%s, subject=%s, to=`%s",
                    getWysiwyg().getRichBody(),
                    getSubject(),
                    getTo().values().toString()
                );
            }

            @Override
            public void setSubject(@Nullable String subject) {

            }

            @Nullable
            @Override
            public String getSubject() {
                return steps.user().pages().ComposePage().composeFieldsBlock().fieldSubject().getAttribute("value");
            }

            @Override
            public void setTo(@NotNull YSSet<String> to) {

            }

            @NotNull
            @Override
            public YSSet<String> getTo() {
                YSSet<String> to = new YSSet<>();
                steps.user().pages().ComposePage().composeFieldsBlock().yabbleToList()
                    .forEach(toElement -> to.add(toElement.getText()));
                return to;
            }
        };
    }

    @Override
    public void addToUsingSuggest(@NotNull String to) {
        steps.user().defaultSteps().clicksOn(steps.pages().mail().compose().composeFieldsBlock().fieldTo());
        steps.user().composeSteps().inputsAddressInFieldTo(to);
        steps.user().defaultSteps()
            .clicksOn(steps.pages().mail().compose().suggestList().waitUntil(not(empty())).get(0).avatar());
    }

    private YSSet<String> getStyles(String richBody, int i){
        ArrayList<YSSet<String>> styles = new ArrayList<>();
        Pattern startTagPattern = Pattern.compile("<([a-z]*)>");
        Pattern finishTagPattern = Pattern.compile("<\\/([a-z]*)>");
        Matcher matcherStart;
        Matcher matcherFinish;
        String newString = richBody;
        Set<String> currentStyles = new HashSet<>();
        while (newString.length() > 0) {
            matcherStart = startTagPattern.matcher(newString);
            matcherFinish = finishTagPattern.matcher(newString);
            boolean textNext = false;
            while (!textNext) {
                textNext = false;
                if (matcherStart.find() && matcherStart.start() == 0 && matcherStart.groupCount() > 0) {
                    currentStyles.add(matcherStart.group(1));
                    newString = newString.replaceFirst("<[a-z]*>", "");
                }
                else if (matcherFinish.find() && matcherFinish.start() == 0 && matcherFinish.groupCount() > 0) {
                    currentStyles.remove(matcherFinish.group(1));
                    newString = newString.replaceFirst("<\\/[a-z]*>", "");
                }
                else {
                    if (newString.equals("")) {
                        break;
                    }
                    styles.add(new YSSet<>(currentStyles));
                    textNext = true;
                    newString = newString.substring(1);
                }
            }
        }
        return styles.get(i);
    }
}
