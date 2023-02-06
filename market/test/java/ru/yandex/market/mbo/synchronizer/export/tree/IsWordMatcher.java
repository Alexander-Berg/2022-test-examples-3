package ru.yandex.market.mbo.synchronizer.export.tree;

import org.hamcrest.CustomTypeSafeMatcher;
import org.hamcrest.Description;

import ru.yandex.market.mbo.gwt.models.visual.Word;

import java.util.Objects;

/**
 * @author Alexander Kramarev (pochemuto@yandex-team.ru)
 * @date 25.01.2018
 */
public class IsWordMatcher extends CustomTypeSafeMatcher<Word> {

    private final String word;
    private final int langId;

    public IsWordMatcher(String word, int langId) {
        super("should match word {langId='" + langId + "', word='" + word + "', id=any}");
        this.word = word;
        this.langId = langId;
    }

    public IsWordMatcher(Word word) {
        this(word.getWord(), word.getLangId());
    }

    @Override
    protected boolean matchesSafely(Word item) {
        return Objects.equals(item.getWord(), word) && item.getLangId() == langId;
    }

    @Override
    protected void describeMismatchSafely(Word item, Description mismatchDescription) {
        boolean mismatch = false;
        if (!Objects.equals(item.getWord(), word)) {
            mismatchDescription.appendText("word ")
                .appendValue(item.getWord())
                .appendText(" not matched ")
                .appendValue(word);
            mismatch = true;
        }
        if (item.getLangId() != langId) {
            if (mismatch) {
                mismatchDescription.appendText(" and ");
            }
            mismatchDescription.appendText("langId ")
                .appendValue(item.getLangId())
                .appendText(" not matched ")
                .appendValue(langId);
        }
    }

    public static IsWordMatcher likeWord(Word word) {
        return new IsWordMatcher(word);
    }

    public static IsWordMatcher isWord(String word, int langId) {
        return new IsWordMatcher(word, langId);
    }
}
