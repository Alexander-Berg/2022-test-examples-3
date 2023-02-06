package ru.yandex.autotests.innerpochta.imap.matchers;

import java.util.List;

import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.TypeSafeMatcher;

import ru.yandex.autotests.innerpochta.imap.core.imap.ImapClient;
import ru.yandex.qatools.matchers.collection.HasSameItemsAsCollectionMatcher;

import static com.google.common.collect.Lists.newArrayList;
import static java.lang.String.format;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 30.05.14
 * Time: 18:48
 */
public class FlagsExistMatcher extends TypeSafeMatcher<ImapClient> {

    private List<String> flags;
    private String id;

    public FlagsExistMatcher(String id, List<String> flags) {
        this.id = id;
        this.flags = flags;
    }

    @Factory
    public static FlagsExistMatcher hasFlags(String id, List<String> flags) {
        return new FlagsExistMatcher(id, flags);
    }

    @Factory
    public static FlagsExistMatcher hasFlags(String id, String... flags) {
        return new FlagsExistMatcher(id, newArrayList(flags));
    }

    @Override
    public void describeTo(Description description) {
        description.appendText(format("существование флагов <%s>", flags));
    }

    @Override
    protected void describeMismatchSafely(ImapClient client, Description mismatchDescription) {
        mismatchDescription.appendValueList("существуют флаги: \n----->", "\n----->", "",
                client.fetch().flags(id));
    }

    @Override
    protected boolean matchesSafely(ImapClient client) {
        return HasSameItemsAsCollectionMatcher.hasSameItemsAsCollection(flags).matches(client.fetch().flags(id));
    }
}
