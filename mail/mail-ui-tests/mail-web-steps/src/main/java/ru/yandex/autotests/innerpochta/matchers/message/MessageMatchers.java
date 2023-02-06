package ru.yandex.autotests.innerpochta.matchers.message;

import ch.lambdaj.function.argument.InvocationException;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import org.hamcrest.FeatureMatcher;
import org.hamcrest.Matcher;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import ru.yandex.autotests.innerpochta.matchers.RegExpMatcher;
import ru.yandex.autotests.innerpochta.ns.pages.messages.blocks.MessageBlock;
import ru.yandex.autotests.innerpochta.rules.WebDriverRule;
import ru.yandex.autotests.innerpochta.ns.pages.GetPagesSteps;

import java.util.List;

import static ch.lambdaj.Lambda.extract;
import static ch.lambdaj.Lambda.having;
import static ch.lambdaj.Lambda.on;
import static ch.lambdaj.collection.LambdaCollections.with;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.autotests.innerpochta.util.Utils.withWaitFor;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;

public class MessageMatchers {

    public static Matcher<MessageBlock> hasSubject(String subj) {
        return new FeatureMatcher<MessageBlock, MailElement>(hasText(subj), "exp subject", "actual subject") {
            @Override
            protected MailElement featureValueOf(MessageBlock messageBlock) {
                return messageBlock.subject();
            }
        };
    }

    public static Matcher<MessageBlock> hasDate(String subj) {
        return new FeatureMatcher<MessageBlock, MailElement>(hasText(subj), "exp date", "actual date") {
            @Override
            protected MailElement featureValueOf(MessageBlock messageBlock) {
                return messageBlock.date();
            }
        };
    }

    public static Matcher<MessageBlock> hasDate(RegExpMatcher regExpMatcher) {
        return new FeatureMatcher<MessageBlock, String>(regExpMatcher, "has Date ", "actual Date") {
            @Override
            protected String featureValueOf(MessageBlock messageBlock) {
                return messageBlock.date().getText();
            }
        };
    }

    public static Matcher<MessageBlock> hasLabel(String label) {
        return new FeatureMatcher<MessageBlock, List<String>>(
                withWaitFor(hasItem(equalTo(label.toLowerCase()))), "", "actual label is:") {
            @Override
            protected List<String> featureValueOf(MessageBlock messageBlock) {
                try {
                    return with(messageBlock.labels()).extract(on(MailElement.class).getText());
                } catch (StaleElementReferenceException e) {
                    return with(e.getMessage());
                }
            }
        };
    }


    public static Matcher<MessageBlock> hasFolder(String folder) {
        return new FeatureMatcher<MessageBlock, String>(equalTo(folder), "folder", "actual") {
            @Override
            protected String featureValueOf(MessageBlock messageBlock) {
                try {
                    return messageBlock.folder().getText();
                } catch (StaleElementReferenceException | NoSuchElementException e) {
                    return e.getMessage();
                }
            }
        };
    }

    public static Matcher<WebDriverRule> messageShould(Matcher<MessageBlock> matcher, final int number) {
        return new FeatureMatcher<WebDriverRule, MessageBlock>(matcher, "message " + number + " should has:",
                "") {
            @Override
            protected MessageBlock featureValueOf(WebDriverRule webDriverRule) {
                return new GetPagesSteps(webDriverRule).MessagePage().displayedMessages().list()
                        .get(number);
            }
        };
    }

    public static Matcher<WebDriverRule> messagesInThreadShould(Matcher<MessageBlock> matcher, final int number) {
        return new FeatureMatcher<WebDriverRule, MessageBlock>(matcher, "message in thread " + number + " should has:",
                "") {
            @Override
            protected MessageBlock featureValueOf(WebDriverRule webDriverRule) {
                return new GetPagesSteps(webDriverRule).MessagePage().displayedMessages().messagesInThread().get(number);
            }
        };
    }

    public static Matcher<WebDriverRule> messageCountOnPage(Matcher<Integer> count) {
        return new FeatureMatcher<WebDriverRule, Integer>(count, "message count ", "actual ") {
            @Override
            protected Integer featureValueOf(WebDriverRule webDriverRule) {
                try {
                    return new GetPagesSteps(webDriverRule).MessagePage().displayedMessages().list().size();
                } catch (StaleElementReferenceException | NoSuchElementException e) {
                    return -1;
                }
            }

        };
    }

    public static Matcher<WebDriverRule> existMessageWith(Matcher<List<MessageBlock>> matcher) {
        return new FeatureMatcher<WebDriverRule, List<MessageBlock>>(matcher,
                "should see message", "") {
            @Override
            protected List<MessageBlock> featureValueOf(WebDriverRule webDriverRule) {
                return new GetPagesSteps(webDriverRule).MessagePage().displayedMessages().list();
            }
        };
    }

    public static Matcher<List<MessageBlock>> subject(String name) {
        return new FeatureMatcher<List<MessageBlock>, List<String>>(hasItem(equalTo(name)),
                "with subject:", "actual:") {
            @Override
            protected List<String> featureValueOf(List<MessageBlock> messages) {
                try {
                    return extract(messages, on(MessageBlock.class).subject().getText());
                } catch (StaleElementReferenceException | InvocationException e) {
                    return with(e.getMessage());
                }
            }
        };
    }

    public static Matcher<List<MessageBlock>> subjectCount(final String name, int count) {
        return new FeatureMatcher<List<MessageBlock>, List<String>>(hasSize(count),
                "with subject:", "actual:") {
            @Override
            protected List<String> featureValueOf(List<MessageBlock> messages) {
                try {
                    return with(extract(messages, on(MessageBlock.class).subject()))
                            .retain(having(on(MessageBlock.class), hasText(equalTo(name))))
                            .extract(on(MailElement.class).getText());
                } catch (StaleElementReferenceException | InvocationException e) {
                    return with(e.getMessage());
                }
            }
        };
    }

    public static Matcher<List<MessageBlock>> subjects(String... name) {
        return new FeatureMatcher<List<MessageBlock>, List<String>>(hasItems(name),
                "with subject:", "actual:") {
            @Override
            protected List<String> featureValueOf(List<MessageBlock> messages) {
                try {
                    return extract(messages, on(MessageBlock.class).subject().getText());
                } catch (StaleElementReferenceException | InvocationException e) {
                    return with(e.getMessage());
                }
            }
        };
    }



}
