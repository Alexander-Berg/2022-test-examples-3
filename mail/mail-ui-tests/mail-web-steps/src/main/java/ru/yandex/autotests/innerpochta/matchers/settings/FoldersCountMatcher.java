package ru.yandex.autotests.innerpochta.matchers.settings;

import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import ru.yandex.autotests.innerpochta.ns.pages.settings.blocks.folderslabels.elements.CreatedFoldersListBlock;

/**
 * Created with IntelliJ IDEA.
 * User: arttimofeev
 * Date: 12.10.12
 * Time: 19:19
 */
public class FoldersCountMatcher extends TypeSafeMatcher<CreatedFoldersListBlock> {
    private Matcher<Integer> matcher;

    public boolean matchesSafely(CreatedFoldersListBlock foldersBlock) {
        return matcher.matches(foldersBlock.customFolders().size());
    }

    public FoldersCountMatcher(Matcher<Integer> matcher) {
        this.matcher = matcher;
    }

    @Factory
    public static FoldersCountMatcher foldersCount(Matcher<Integer> matcher) {
        return new FoldersCountMatcher(matcher);
    }

    @Override
    public void describeMismatchSafely(CreatedFoldersListBlock foldersBlock, Description description) {
        description.appendText("Количество фолдеров на странице настроек: ")
                .appendValue(foldersBlock.customFolders().size());
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("Количество фолдеров на странице настроек должно быть ").appendDescriptionOf(matcher);
    }
}
