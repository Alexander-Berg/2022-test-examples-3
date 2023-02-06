package ru.yandex.autotests.innerpochta.imap.matchers;

import java.util.Collection;

import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.TypeSafeMatcher;

import ru.yandex.autotests.innerpochta.imap.core.imap.ImapClient;
import ru.yandex.autotests.innerpochta.imap.structures.ListItem;

import static ch.lambdaj.collection.LambdaCollections.with;
import static java.lang.String.format;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static ru.yandex.autotests.innerpochta.imap.converters.ToStringConverter.wrap;
import static ru.yandex.autotests.innerpochta.imap.requests.LsubRequest.lsub;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 10.04.14
 * Time: 13:31
 */
public class FolderSubscribedMatcher extends TypeSafeMatcher<ImapClient> {

    private String folderName;

    public FolderSubscribedMatcher(String folderName) {
        this.folderName = folderName;
    }

    @Factory
    public static FolderSubscribedMatcher hasSubscribedFolder(String folderName) {
        return new FolderSubscribedMatcher(folderName);
    }

    @Override
    public void describeTo(Description description) {
        description.appendText(format("наличие подписки на папку <%s>", folderName));
    }

    @Override
    protected void describeMismatchSafely(ImapClient client, Description mismatchDescription) {
        mismatchDescription.appendValueList("подписаны на папки: \n----->", "\n----->", "",
                with(client.request(lsub("\"\"", "*")).getItems()).convert(wrap()));
    }

    @Override
    protected boolean matchesSafely(ImapClient item) {
        Collection<ListItem> items = item.request(lsub("\"\"", format(("\"%s\""), folderName))).shouldBeOk().getItems();
        return isNotEmpty(items);
    }
}
