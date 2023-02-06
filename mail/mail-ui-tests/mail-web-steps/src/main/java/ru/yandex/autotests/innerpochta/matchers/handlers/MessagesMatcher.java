package ru.yandex.autotests.innerpochta.matchers.handlers;

import edu.emory.mathcs.backport.java.util.Arrays;
import org.hamcrest.FeatureMatcher;
import org.hamcrest.Matchers;
import ru.yandex.autotests.innerpochta.rules.RestAssuredAuthRule;
import ru.yandex.autotests.innerpochta.steps.beans.folder.Folder;
import ru.yandex.autotests.innerpochta.steps.beans.message.Message;

import java.util.List;

import static ch.lambdaj.Lambda.extract;
import static ch.lambdaj.Lambda.having;
import static ch.lambdaj.Lambda.on;
import static ch.lambdaj.Lambda.selectFirst;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static ru.yandex.autotests.innerpochta.api.folders.FoldersHandler.foldersHandler;
import static ru.yandex.autotests.innerpochta.api.messages.MessagesHandler.messagesHandler;
import static ru.yandex.autotests.innerpochta.steps.api.ApiDefaultSteps.getJsonPathConfig;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.INBOX;

/**
 * @author mabelpines
 */
public class MessagesMatcher {

    public static FeatureMatcher<RestAssuredAuthRule, List<String>> hasMessageWithSubjectInList(String subject) {
        return new FeatureMatcher<RestAssuredAuthRule, List<String>>(
            hasItem(Matchers.equalTo(subject)), "заголовок письма", "существующее письмо"
        ) {
            @Override
            protected List<String> featureValueOf(RestAssuredAuthRule auth) {
                List allFolders = Arrays.asList(foldersHandler().withAuth(auth).callFoldersHandler().then()
                    .extract().jsonPath(getJsonPathConfig()).getObject("models[0].data.folder", Folder[].class));

                Folder inboxFolder = selectFirst(allFolders, having(on(Folder.class).getSymbol(), equalTo(INBOX)));
                return extract(Arrays.asList(messagesHandler().withAuth(auth)
                    .withCurrentFolder(inboxFolder.getFid()).withMessagesPerPage(inboxFolder.getCount())
                    .callMessagesHandler().then().extract().jsonPath(getJsonPathConfig())
                    .getObject("models[0].data.message", Message[].class)), on(Message.class).getSubject());
            }
        };
    }

    public static FeatureMatcher<RestAssuredAuthRule, Integer> hasThreadWithSubjectInList(String subject, int size) {
        return new FeatureMatcher<RestAssuredAuthRule, Integer>(
            Matchers.greaterThanOrEqualTo(size),
            "заголовок и тело письма", "существующее письмо") {
            @Override
            protected Integer featureValueOf(RestAssuredAuthRule auth) {
                List allFolders = Arrays.asList(foldersHandler().withAuth(auth).callFoldersHandler().then()
                    .extract().jsonPath(getJsonPathConfig()).getObject("models[0].data.folder", Folder[].class));

                Folder inboxFolder = selectFirst(allFolders, having(on(Folder.class).getSymbol(), equalTo(INBOX)));

                List<String> thread = extract(Arrays.asList(messagesHandler().withAuth(auth)
                    .withCurrentFolder(inboxFolder.getFid()).withMessagesPerPage(inboxFolder.getCount())
                    .callMessagesHandler().then().extract().jsonPath(getJsonPathConfig())
                    .getObject("models[0].data.message", Message[].class)), on(Message.class).getSubject());
                thread.removeIf(subj -> !subj.equals(subject));
                return thread.size();
            }
        };
    }
}
