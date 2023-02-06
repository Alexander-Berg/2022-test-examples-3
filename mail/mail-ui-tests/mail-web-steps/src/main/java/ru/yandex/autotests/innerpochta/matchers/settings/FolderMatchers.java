package ru.yandex.autotests.innerpochta.matchers.settings;

import ch.lambdaj.collection.LambdaList;
import org.hamcrest.FeatureMatcher;
import org.hamcrest.Matcher;
import org.openqa.selenium.StaleElementReferenceException;
import ru.yandex.autotests.innerpochta.ns.pages.GetPagesSteps;
import ru.yandex.autotests.innerpochta.ns.pages.settings.blocks.folderslabels.elements.CustomFolderBlock;
import ru.yandex.autotests.innerpochta.rules.WebDriverRule;

import java.util.List;

import static ch.lambdaj.Lambda.extract;
import static ch.lambdaj.Lambda.having;
import static ch.lambdaj.Lambda.on;
import static ch.lambdaj.Lambda.select;
import static ch.lambdaj.collection.LambdaCollections.with;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;

public class FolderMatchers {

    public static Matcher<CustomFolderBlock> withName(final Matcher<String> matcher) {
        return new FeatureMatcher<CustomFolderBlock, String>(matcher, "имя папки", "существующая папка") {
            @Override
            protected String featureValueOf(CustomFolderBlock customFolderBlock) {
                return customFolderBlock.name().getText();
            }
        };
    }

    public static Matcher<WebDriverRule> customFolder(Matcher<List<CustomFolderBlock>> matcher) {
        return new FeatureMatcher<WebDriverRule, List<CustomFolderBlock>>(matcher,
                "обычная папка", "") {
            @Override
            protected List<CustomFolderBlock> featureValueOf(WebDriverRule webDriverRule) {
                return new GetPagesSteps(webDriverRule).FoldersAndLabelsSettingPage().setupBlock().folders()
                        .blockCreatedFolders().customFolders();
            }
        };
    }

    public static Matcher<WebDriverRule> inboxFolderCount(Matcher<String> matcher) {
        return new FeatureMatcher<WebDriverRule, String>(matcher,
                "Inbox folder count", "") {
            @Override
            protected String featureValueOf(WebDriverRule webDriverRule) {
                try {
                    return new GetPagesSteps(webDriverRule).FoldersAndLabelsSettingPage().setupBlock().folders()
                            .inboxFolderCounter().getText();
                } catch (StaleElementReferenceException e) {
                    return e.getMessage();
                }
            }
        };
    }

    public static Matcher<WebDriverRule> customFolderCount(Matcher<Integer> matcher) {
        return new FeatureMatcher<WebDriverRule, Integer>(matcher,
                "Custom folder count", "") {
            @Override
            protected Integer featureValueOf(WebDriverRule webDriverRule) {
                try {
                    return new GetPagesSteps(webDriverRule).FoldersAndLabelsSettingPage().setupBlock().folders()
                            .blockCreatedFolders().customFolders().size();
                } catch (StaleElementReferenceException e) {
                    return -1;
                }
            }
        };
    }

    public static Matcher<List<CustomFolderBlock>> withFolderName(String count) {
        return new FeatureMatcher<List<CustomFolderBlock>, LambdaList<String>>(hasItem(equalTo(count)),
                "имя", "") {
            @Override
            protected LambdaList<String> featureValueOf(List<CustomFolderBlock> customFoldersThreads) {
                try {
                    return with(customFoldersThreads).extract(on(CustomFolderBlock.class).name().getText());
                } catch (StaleElementReferenceException e) {
                    return with(e.getMessage());
                }
            }
        };
    }

    public static Matcher<List<CustomFolderBlock>> withFolderNameAndCount(final String name, final String count) {
        return new FeatureMatcher<List<CustomFolderBlock>, List<String>>(hasItem(count),
                "имя и каунтер", "") {
            @Override
            protected List<String> featureValueOf(List<CustomFolderBlock> customFoldersThreads) {
                try {
                    return extract(select(customFoldersThreads, having(on(CustomFolderBlock.class).name(),
                                    hasText(name))),
                            on(CustomFolderBlock.class).info().getText());

                } catch (StaleElementReferenceException e) {
                    return with(e.getMessage());
                }
            }
        };
    }

    public static Matcher<WebDriverRule> customFolderNames(String... name) {
        return new FeatureMatcher<WebDriverRule, List<String>>(hasItems(name),
            "Custom folders names on right table is", "") {
            @Override
            protected List<String> featureValueOf(WebDriverRule webDriverRule) {
                try {
                    return with(new GetPagesSteps(webDriverRule).MessagePage().foldersNavigation()
                        .customFolders()).extract(on(ru.yandex.autotests.innerpochta.ns.pages.folderblocks.CustomFolderBlock.class).customFolderName().getText());
                } catch (StaleElementReferenceException e) {
                    return with(e.getMessage());
                }
            }
        };
    }
}
