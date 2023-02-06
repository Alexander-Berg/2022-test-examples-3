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
import static ru.yandex.autotests.innerpochta.imap.requests.ListRequest.list;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 05.03.14
 * Time: 20:10
 */
public class FolderExistMatcher extends TypeSafeMatcher<ImapClient> {

    private String folderName;

    public FolderExistMatcher(String folderName) {
        this.folderName = folderName;
    }

    @Factory
    public static FolderExistMatcher hasFolder(String folderName) {
        return new FolderExistMatcher(folderName);
    }

    @Override
    public void describeTo(Description description) {
        description.appendText(format("существование папки <%s>", folderName));
    }

    @Override
    protected void describeMismatchSafely(ImapClient client, Description mismatchDescription) {
        mismatchDescription.appendValueList("существуют папки: \n----->", "\n----->", "",
                with(client.request(list("\"\"", "*")).getItems()).convert(wrap()));
    }

    @Override
    protected boolean matchesSafely(ImapClient client) {
        Collection<ListItem> items = client.request(list("\"\"", format(("\"%s\""), folderName))).shouldBeOk().getItems();
        return isNotEmpty(items);
    }
}
