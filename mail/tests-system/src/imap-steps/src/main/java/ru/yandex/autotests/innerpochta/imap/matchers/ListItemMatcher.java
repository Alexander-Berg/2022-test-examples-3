package ru.yandex.autotests.innerpochta.imap.matchers;

import java.util.Set;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

import ru.yandex.autotests.innerpochta.imap.structures.ListItem;

import static com.google.common.base.Joiner.on;
import static com.sun.mail.imap.protocol.BASE64MailboxDecoder.decode;
import static java.lang.String.format;
import static ru.yandex.autotests.innerpochta.imap.utils.Utils.quoted;

public class ListItemMatcher extends TypeSafeMatcher<ListItem> {
    private final ListItem required;

    public ListItemMatcher(ListItem required) {
        this.required = required;
    }

    public static Matcher<ListItem> listItem(String reference, String name, String... flags) {
        return new ListItemMatcher(new ListItem(reference, name, flags));
    }

    public static Matcher<ListItem> listItem(ListItem listItem) {
        return new ListItemMatcher(listItem);
    }

    private boolean flagsMatch(Set<String> flags) {
        return flags.containsAll(required.getFlags());
    }

    private boolean referenceMatches(String reference) {
        return reference.equals(required.getReference()) || reference.equals(quoted(required.getReference()));
    }

    private boolean nameMatches(String name) {
        return name.equals(required.getName()) || name.equals(quoted(required.getName()));
    }

    @Override
    protected boolean matchesSafely(ListItem item) {
        return flagsMatch(item.getFlags()) && referenceMatches(item.getReference()) && nameMatches(item.getName());
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("\n\t<")
                .appendText(required.getName().startsWith("&B") || required.getName().startsWith("\"&B")
                        ? format("%s (%s)", decode(required.getName()), required.getName())
                        : required.getName())
                .appendText("> с флагами: [").appendText(on(' ').join(required.getFlags())).appendText("]");
    }

    @Override
    protected void describeMismatchSafely(ListItem item, Description mismatchDescription) {
        mismatchDescription.appendText("\n\tбыл ").appendText(item.toString());
    }
}
