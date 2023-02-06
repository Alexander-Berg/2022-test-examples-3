package ru.yandex.autotests.innerpochta.steps.api;

import com.google.common.base.Joiner;
import io.restassured.response.Response;
import org.hamcrest.Matchers;
import ru.yandex.autotests.innerpochta.api.DoSendJsonHandler;
import ru.yandex.autotests.innerpochta.objstruct.base.misc.Account;
import ru.yandex.autotests.innerpochta.rules.RestAssuredAuthRule;
import ru.yandex.autotests.innerpochta.steps.AllureStepStorage;
import ru.yandex.autotests.innerpochta.steps.beans.folder.Folder;
import ru.yandex.autotests.innerpochta.steps.beans.label.Label;
import ru.yandex.autotests.innerpochta.steps.beans.message.Message;
import ru.yandex.autotests.innerpochta.util.Utils;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Step;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static ch.lambdaj.Lambda.having;
import static ch.lambdaj.Lambda.on;
import static ch.lambdaj.Lambda.select;
import static ch.lambdaj.Lambda.selectFirst;
import static ch.lambdaj.Lambda.selectMax;
import static com.google.common.collect.ImmutableMap.of;
import static java.util.concurrent.Executors.newCachedThreadPool;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.autotests.innerpochta.api.DoSendJsonHandler.doSendJsonHandler;
import static ru.yandex.autotests.innerpochta.api.UploadAttachmentHandler.uploadAttachmentHandler;
import static ru.yandex.autotests.innerpochta.api.messages.DoMessagesHandler.doMessagesHandler;
import static ru.yandex.autotests.innerpochta.api.messages.MessagesHandler.messagesHandler;
import static ru.yandex.autotests.innerpochta.api.replyLater.DoReplyLaterCreateHandler.doReplyLaterCreateHandler;
import static ru.yandex.autotests.innerpochta.api.settings.DoSettingsHandler.doSettingsHandler;
import static ru.yandex.autotests.innerpochta.matchers.handlers.MessagesMatcher.hasMessageWithSubjectInList;
import static ru.yandex.autotests.innerpochta.matchers.handlers.MessagesMatcher.hasThreadWithSubjectInList;
import static ru.yandex.autotests.innerpochta.steps.api.ApiDefaultSteps.getJsonPathConfig;
import static ru.yandex.autotests.innerpochta.util.ProxyServerConstants.SESSION_TIMEOUT_VALUE_NUMBER;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomNumber;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomString;
import static ru.yandex.autotests.innerpochta.util.Utils.withWaitFor;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.FALSE;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.INBOX;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.REPLY_LATER;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.SENT;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.STATUS_TRUE;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.STATUS_YES;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.TRASH;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.TRUE;
import static ru.yandex.autotests.innerpochta.util.handlers.MessagesConstants.MESSAGES_PARAM_ACTION_DELETE;
import static ru.yandex.autotests.innerpochta.util.handlers.MessagesConstants.MESSAGES_PARAM_ACTION_MARK;
import static ru.yandex.autotests.innerpochta.util.handlers.MessagesConstants.MESSAGES_PARAM_ACTION_MOVE;
import static ru.yandex.autotests.innerpochta.util.handlers.MessagesConstants.MESSAGES_PARAM_ACTION_TO_SPAM;
import static ru.yandex.autotests.innerpochta.util.handlers.MessagesConstants.MESSAGES_PARAM_ACTION_UNMARK;
import static ru.yandex.autotests.innerpochta.util.handlers.SendJsonConstants.MESSAGES_PARAM_DRAFT;
import static ru.yandex.autotests.innerpochta.util.handlers.SendJsonConstants.MESSAGES_PARAM_TEMPLATE;
import static ru.yandex.autotests.innerpochta.util.handlers.SendJsonConstants.SEND_JSON_PARAM_FROM_NAME;
import static ru.yandex.autotests.innerpochta.util.handlers.SendJsonConstants.SEND_JSON_PARAM_SAVE_UNDERSCORE;
import static ru.yandex.autotests.innerpochta.util.handlers.SendJsonConstants.SEND_JSON_PARAM_SEND_TIME;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_PARAM_NOSAVE;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_SAVE_SENT;

/**
 * @author mabelpines
 */
public class ApiMessagesSteps {

    public RestAssuredAuthRule auth;
    private AllureStepStorage user;
    private String ccEmails = "";
    private String bccEmails = "";

    public ApiMessagesSteps(AllureStepStorage user) {
        this.user = user;
    }

    public ApiMessagesSteps withAuth(RestAssuredAuthRule auth) {
        this.auth = auth;
        return this;
    }

    public ApiMessagesSteps addCcEmails(String... emails) {
        ccEmails = Joiner.on(",").join(emails);
        return this;
    }

    public ApiMessagesSteps addBccEmails(String... emails) {
        bccEmails = Joiner.on(",").join(emails);
        return this;
    }

    @Step("Вызов api-метода: messages. Получаем все письма из папки “{0}“.")
    public List<Message> getAllMessagesInFolder(Folder folder) {
        return Arrays.asList(messagesHandler().withAuth(auth).withCurrentFolder(folder.getFid())
            .withMessagesPerPage(folder.getCount()).callMessagesHandler().then()
            .extract().jsonPath(getJsonPathConfig()).getObject("models[0].data.message", Message[].class));
    }

    @Step("Вызов api-метода: messages. Получаем все письма из папки “{0}“.")
    public List<Message> getAllMessagesInFolder(String folderSymbol) {
        Folder folder = user.apiFoldersSteps().getFolderBySymbol(folderSymbol);
        return getAllMessagesInFolder(folder);
    }

    @Step("Вызов api-метода: messages. Получаем все письма с меткой “{0}“.")
    public List<Message> getAllMessagesLabel(String labelName) {
        Label label = user.apiLabelsSteps().getLabelByName(labelName);
        return Arrays.asList(messagesHandler().withAuth(auth).withCurrentLid(label.getLid())
            .withMessagesPerPage(label.getCount()).callMessagesHandler().then()
            .extract().jsonPath(getJsonPathConfig()).getObject("models[0].data.message", Message[].class));
    }

    @Step("Вызов api-метода: messages. Получаем все письма.")
    public List<Message> getAllMessages() {
        return getAllMessagesInFolder(INBOX);
    }

    @Step("Вызов api-метода: messages. Получаем письмо по теме письма.")
    public Message getMessageWithSubject(String subject) {
        return selectFirst(getAllMessages(), having(on(Message.class).getSubject(), equalTo(subject)));
    }

    @Step("Вызов api-метода: messages. Получаем письмо по теме письма.")
    public List<Message> getAllMessagesWithSubject(String subject) {
        return select(getAllMessages(), having(on(Message.class).getSubject(), equalTo(subject)));
    }

    @Step("Вызов api-метода: messages. Получаем письмо по теме письма “{0}“ в папке “{1}“.")
    public Message getMessageWithSubjectInFolder(String subject, String folder) {
        return selectFirst(getAllMessagesInFolder(folder), having(on(Message.class).getSubject(), equalTo(subject)));
    }

    @Step("Вызов api-метода: messages. Получаем письма за дату “{0}“ в папке “{1}“.")
    public List<Message> getAllMessagesByDate(String date, String folder) {
        return select(
            getAllMessagesInFolder(folder),
            having(on(Message.class).getDate().toString(), containsString(date))
        );
    }

    @Step("Вызов api-метода: messages. Получаем все письма из треда с tid «0».")
    public List<Message> getAllThreadMessages(String tid) {
        return Arrays.asList(messagesHandler().withAuth(auth).withThreadId(tid).callMessagesHandler().then()
            .extract().jsonPath(getJsonPathConfig()).getObject("models[0].data.message", Message[].class));
    }

    @Step("Вызов api-метода: do-messages. Переносим все письма из папки “{0}“ в папку “{1}“.")
    public ApiMessagesSteps moveAllMessagesFromFolderToFolder(String currentFolderName, String targetFolderName) {
        Folder currentFolder = user.apiFoldersSteps().getFolderBySymbol(currentFolderName);
        Folder targetFolder = user.apiFoldersSteps().getFolderBySymbol(targetFolderName);
        if ((!getAllMessagesInFolder(currentFolder.getName()).isEmpty()) || (targetFolder.getFid() != null)) {
            doMessagesHandler().withAuth(auth).withAction(MESSAGES_PARAM_ACTION_MOVE)
                .withMessages(getAllMessagesInFolder(currentFolder.getName()))
                .withMoveFile(targetFolder.getFid()).callDoMessagesHandler();
        }
        return this;
    }

    @Step("Вызов api-метода: do-messages. Переносим письма в папку “{0}“.")
    public ApiMessagesSteps moveMessagesFromFolderToFolder(String targetFolderName, Message... messages) {
        Folder targetFolder = user.apiFoldersSteps().getFolderBySymbol(targetFolderName);
        if ((targetFolder.getFid() != null)) {
            doMessagesHandler().withAuth(auth).withAction(MESSAGES_PARAM_ACTION_MOVE)
                .withMessages(Arrays.asList(messages))
                .withMoveFile(targetFolder.getFid()).callDoMessagesHandler();
        }
        return this;
    }

    @Step("Вызов api-метода: do-messages. Отправляем письмо “{0}“ в спам.")
    public ApiMessagesSteps moveMessagesToSpam(Message... messages) {
        if (!Arrays.asList(messages).isEmpty())
            doMessagesHandler().withAuth(auth).withAction(MESSAGES_PARAM_ACTION_TO_SPAM)
                .withMessages(Arrays.asList(messages)).callDoMessagesHandler();
        return this;
    }

    @Step("Вызов api-метода: do-messages. Переносим письмо в таб")
    public ApiMessagesSteps moveMessagesToTab(String tabName, Message... messages) {
        doMessagesHandler().withAuth(auth).withAction(MESSAGES_PARAM_ACTION_MOVE)
            .withMessages(Arrays.asList(messages))
            .withMoveFile("1")
            .withTab(tabName.toLowerCase()).callDoMessagesHandler();
        return this;
    }

    @Step("Вызов api-метода: do-messages. Переносим все письма из папки в таб")
    public ApiMessagesSteps moveAllMessagesToTab(String tabName, String currentFolderName) {
        Folder currentFolder = user.apiFoldersSteps().getFolderBySymbol(currentFolderName);
        if (!getAllMessagesInFolder(currentFolder.getName()).isEmpty()) {
            doMessagesHandler().withAuth(auth).withAction(MESSAGES_PARAM_ACTION_MOVE)
                .withMessages(getAllMessagesInFolder(currentFolder.getName()))
                .withMoveFile("1")
                .withTab(tabName.toLowerCase())
                .callDoMessagesHandler();
        }
        return this;
    }

    @Step("Удаляем все сообщения в папке “{0}“")
    public ApiMessagesSteps deleteAllMessagesInFolder(Folder folder) {
        if (!getAllMessagesInFolder(folder.getName()).isEmpty())
            doMessagesHandler().withAuth(auth).withAction(MESSAGES_PARAM_ACTION_DELETE)
                .withMessages(getAllMessagesInFolder(folder.getName())).callDoMessagesHandler();
        return this;
    }

    @Step("Удаляем сообщение/я {0}")
    public ApiMessagesSteps deleteMessages(Message... messages) {
        if (!Arrays.asList(messages).isEmpty() && Arrays.asList(messages).get(0) != null)
            doMessagesHandler().withAuth(auth).withAction(MESSAGES_PARAM_ACTION_DELETE)
                .withMessages(Arrays.asList(messages)).callDoMessagesHandler();
        return this;
    }

    @Step("Вызов api-метода: do-send-json. Отправляем письмо - To: «{0}», Тема: «{1}», Текст: «{2}». " +
        "В отправленных не сохраняем.")
    public Message sendMailWithNoSave(Account acc, String subject, String messageBody) {
        doSettingsHandler()
            .withAuth(auth).withList(SETTINGS_SAVE_SENT).withParams(of(SETTINGS_SAVE_SENT, FALSE)).callDoSettings();
        doSendJsonHandler().withAuth(auth).withReceiver(acc.getSelfEmail()).withSubject(subject)
            .withMessageBody(messageBody).withPlainTextType().withParam(SETTINGS_PARAM_NOSAVE, STATUS_YES)
            .callDoSendJson();
        assertThat(
            "Новое письмо не появилось в списке писем",
            auth,
            withWaitFor(hasMessageWithSubjectInList(subject))
        );
        deleteMessages(getMessageWithSubjectInFolder(subject, SENT));
        deleteMessages(getMessageWithSubjectInFolder(subject, TRASH));
        doSettingsHandler()
            .withAuth(auth).withList(SETTINGS_SAVE_SENT).withParams(of(SETTINGS_SAVE_SENT, TRUE)).callDoSettings();
        return selectFirst(getAllMessages(), having(on(Message.class).getSubject(), equalTo(subject)));
    }

    @Step("Вызов api-метода: do-send-json. Отправляем письмо - To: «{0}», Тема: «{1}», Текст: «{2}». " +
        "В отправленных не сохраняем.")
    public Message sendMailWithNoSave(String email, String subject, String messageBody) {
        doSettingsHandler()
            .withAuth(auth).withList(SETTINGS_SAVE_SENT).withParams(of(SETTINGS_SAVE_SENT, FALSE)).callDoSettings();
        doSendJsonHandler().withAuth(auth).withReceiver(email).withSubject(subject).withMessageBody(messageBody)
            .withPlainTextType().withParam(SETTINGS_PARAM_NOSAVE, STATUS_YES).callDoSendJson();
        assertThat(
            "Новое письмо не появилось в списке писем",
            auth,
            withWaitFor(hasMessageWithSubjectInList(subject))
        );
        deleteMessages(getMessageWithSubjectInFolder(subject, SENT));
        deleteMessages(getMessageWithSubjectInFolder(subject, TRASH));
        doSettingsHandler()
            .withAuth(auth).withList(SETTINGS_SAVE_SENT).withParams(of(SETTINGS_SAVE_SENT, TRUE)).callDoSettings();
        return selectFirst(getAllMessages(), having(on(Message.class).getSubject(), equalTo(subject)));
    }

    @Step("Отправляем себе пачку сообщений в количестве {1}")
    public ApiMessagesSteps sendCoupleMessages(Account acc, int numOfMsg) {
        user.apiSettingsSteps().callWithListAndParams(SETTINGS_SAVE_SENT, of(SETTINGS_SAVE_SENT, FALSE));
        ExecutorService threadPool = newCachedThreadPool();
        List<Future> tasks = new ArrayList<>();
        for (int msg = 1; msg <= numOfMsg; ++msg) {
            String subject = "subj " + msg;
            tasks.add(
                threadPool.submit(() -> {
                    synchronized (ApiMessagesSteps.this) {
                        doSendJsonHandler().withAuth(auth).withReceiver(acc.getSelfEmail()).withSubject(subject)
                            .withMessageBody(Utils.getRandomString()).withPlainTextType()
                            .withParam(SETTINGS_PARAM_NOSAVE, STATUS_YES).callDoSendJson();
                        user.defaultSteps().waitInSeconds(0.5);
                        deleteMessages(getMessageWithSubjectInFolder(subject, SENT));
                        deleteMessages(getMessageWithSubjectInFolder(subject, TRASH));
                    }
                    assertThat(
                        "Новое письмо не появилось в списке писем",
                        auth,
                        withWaitFor(hasMessageWithSubjectInList(subject))
                    );
                })
            );
        }
        threadPool.shutdown();
        try {
            threadPool.awaitTermination(SESSION_TIMEOUT_VALUE_NUMBER, TimeUnit.SECONDS);
            for (Future task : tasks) {
                task.get();
            }
        } catch (ExecutionException e) {
            throw new AssertionError(e.getMessage());
        } catch (InterruptedException ignored) {
        } finally {
            user.apiSettingsSteps().callWithListAndParams(SETTINGS_SAVE_SENT, of(SETTINGS_SAVE_SENT, TRUE));
        }
        return this;
    }

    @Step("Вызов api-метода: do-send-json. Отправляем письмо - To: «{0}», Тема: «{1}», Текст: «{2}»." +
        " В отправленных не сохраняем.")
    public Message sendMailWithCcAndBcc(String email, String subject, String messageBody) {
        doSettingsHandler()
            .withAuth(auth).withList(SETTINGS_SAVE_SENT).withParams(of(SETTINGS_SAVE_SENT, FALSE)).callDoSettings();
        doSendJsonHandler().withAuth(auth).withReceiver(email).withSubject(subject)
            .withMessageBody(messageBody).withPlainTextType().withParam(SETTINGS_PARAM_NOSAVE, STATUS_YES)
            .withCc(ccEmails).withBcc(bccEmails).callDoSendJson();
        assertThat(
            "Новое письмо не появилось в списке писем",
            auth,
            withWaitFor(hasMessageWithSubjectInList(subject))
        );
        deleteMessages(getMessageWithSubjectInFolder(subject, SENT));
        deleteMessages(getMessageWithSubjectInFolder(subject, TRASH));
        doSettingsHandler()
            .withAuth(auth).withList(SETTINGS_SAVE_SENT).withParams(of(SETTINGS_SAVE_SENT, TRUE)).callDoSettings();
        return selectFirst(getAllMessages(), having(on(Message.class).getSubject(), equalTo(subject)));
    }

    @Step("Вызов api-метода: do-send-json. Отправляем письмо в тред- To: «{0}», Тема: «{1}», Текст: «{2}»." +
        " В отправленных не сохраняем.")
    public Message sendMessageToThreadWithCcAndBcc(String email, String subject, String messageBody) {
        doSettingsHandler()
            .withAuth(auth).withList(SETTINGS_SAVE_SENT).withParams(of(SETTINGS_SAVE_SENT, FALSE)).callDoSettings();
        doSendJsonHandler().withAuth(auth).withReceiver(email).withSubject(subject)
            .withMessageBody(messageBody).withPlainTextType().withParam(SETTINGS_PARAM_NOSAVE, STATUS_YES)
            .withCc(ccEmails).withBcc(bccEmails)
            .withThreadToMessage(getMessageWithSubject(subject).getMessageId())
            .callDoSendJson();
        assertThat(
            "Новое письмо не появилось в списке писем",
            auth,
            withWaitFor(hasMessageWithSubjectInList(subject))
        );
        deleteMessages(getMessageWithSubjectInFolder(subject, SENT));
        deleteMessages(getMessageWithSubjectInFolder(subject, TRASH));
        doSettingsHandler()
            .withAuth(auth).withList(SETTINGS_SAVE_SENT).withParams(of(SETTINGS_SAVE_SENT, TRUE)).callDoSettings();
        return selectFirst(getAllMessages(), having(on(Message.class).getSubject(), equalTo(subject)));
    }

    @Step("Вызов api-метода: do-send-json. Отправляем письмо - To: «{0}», Тема: «{1}», Текст: «{2}».")
    public Message sendMail(Account acc, String subject, String messageBody) {
        doSendJsonHandler().withAuth(auth).withReceiver(acc.getSelfEmail()).withSubject(subject)
            .withMessageBody(messageBody).withPlainTextType().callDoSendJson();
        assertThat("Новое письмо не появилось в списке писем", auth, withWaitFor(hasMessageWithSubjectInList(subject)));
        return selectFirst(getAllMessages(), having(on(Message.class).getSubject(), equalTo(subject)));
    }

    @Step("Вызов api-метода: do-send-json. Отправляем письмо - To: «{0}», Subj: «{1}», Text: «{2}». " +
        "Не ищем письмо в инбоксе")
    public ApiMessagesSteps sendMail(String email, String subject, String messageBody) {
        doSendJsonHandler().withAuth(auth).withReceiver(email).withSubject(subject).withMessageBody(messageBody)
            .withPlainTextType().callDoSendJson();
        return this;
    }

    @Step("Вызов api-метода: do-send-json. Отправляем письмо нескольким адресатам To: «{2}», Subj: «{0}», Text: «{1}». " +
        "Не ищем письмо в инбоксе")
    public ApiMessagesSteps sendMailToSeveralReceivers(String subject, String messageBody, String... email) {
        doSendJsonHandler().withAuth(auth).withReceiver(String.join(",", email)).withSubject(subject)
            .withMessageBody(messageBody).withPlainTextType().callDoSendJson();
        return this;
    }

    @Step("Вызов api-метода: do-send-json. Отправляем тред - To: «{0}» ({2} писем)")
    public Message sendThread(Account acc, String subject, int threadSize) {
        int threadId = getRandomNumber(Integer.MAX_VALUE, 1);
        int initialThreadSize = getAllMessagesWithSubject(subject).size();
        user.apiSettingsSteps().callWithListAndParams(SETTINGS_SAVE_SENT, of(SETTINGS_SAVE_SENT, FALSE));
        ExecutorService threadPool = newCachedThreadPool();
        List<Future> tasks = new ArrayList<>();
        for (int msg = 0; msg < threadSize; ++msg) {
            tasks.add(
                threadPool.submit(() -> {
                    synchronized (ApiMessagesSteps.this) {
                        doSendJsonHandler().withAuth(auth).withReceiver(acc.getSelfEmail()).withSubject(subject)
                            .withMessageBody(getRandomString()).withPlainTextType()
                            .withParam(SETTINGS_PARAM_NOSAVE, STATUS_YES).withThreadId(threadId).callDoSendJson();
                        user.defaultSteps().waitInSeconds(0.5);
                        deleteMessages(getMessageWithSubjectInFolder(subject, SENT));
                        deleteMessages(getMessageWithSubjectInFolder(subject, TRASH));
                    }
                })
            );
        }
        threadPool.shutdown();
        try {
            threadPool.awaitTermination(SESSION_TIMEOUT_VALUE_NUMBER, TimeUnit.SECONDS);
            for (Future task : tasks) {
                task.get();
            }
        } catch (ExecutionException e) {
            throw new AssertionError(e.getMessage());
        } catch (InterruptedException ignored) {
        } finally {
            user.apiSettingsSteps().callWithListAndParams(SETTINGS_SAVE_SENT, of(SETTINGS_SAVE_SENT, TRUE));
        }
        assertThat(
            "Тред отправился не полностью",
            auth,
            withWaitFor(hasThreadWithSubjectInList(subject, initialThreadSize + threadSize))
        );
        return selectMax(
            select(getAllMessages(), having(on(Message.class).getSubject(), equalTo(subject))),
            on(Message.class).getMid()
        );
    }

    @Step("Вызов api-метода: do-send-json. Отправляем письмо - To: «{0}», Subj: «{1}», Text: «{2}» From: “{3}“.")
    public Message sendMailFromName(Account acc, String subject, String messageBody, String name) {
        doSendJsonHandler().withAuth(auth).withReceiver(acc.getSelfEmail())
            .withParam(SEND_JSON_PARAM_FROM_NAME, name).withSubject(subject).withMessageBody(messageBody)
            .withPlainTextType().callDoSendJson();
        return selectFirst(getAllMessages(), having(on(Message.class).getSubject(), equalTo(subject)));
    }

    @Step("Вызов api-метода: do-send-json. Отправляем письмо - To: «{0}», Subj: «{1}», Text: «{2}» From: “{3}“.")
    public Message sendMailFromName(String email, String subject, String messageBody, String name) {
        doSendJsonHandler().withAuth(auth).withReceiver(email)
            .withParam(SEND_JSON_PARAM_FROM_NAME, name).withSubject(subject).withMessageBody(messageBody)
            .withPlainTextType().callDoSendJson();
        return selectFirst(getAllMessages(), having(on(Message.class).getSubject(), equalTo(subject)));
    }

    @Step("Вызов api-метода: do-send-json. Создаем письмо с отправкой на завтра - To: «{0}», " +
        "Тема: «{1}», Текст: «{2}» Имя отправителя: “{3}“.")
    public void sendMailWithSentTime(Account acc, String subject, String messageBody) {
        doSettingsHandler()
            .withAuth(auth).withList(SETTINGS_SAVE_SENT).withParams(of(SETTINGS_SAVE_SENT, FALSE)).callDoSettings();
        doSendJsonHandler().withAuth(auth).withReceiver(acc.getSelfEmail()).withParam(SETTINGS_PARAM_NOSAVE, STATUS_YES)
            .withParam(SEND_JSON_PARAM_SEND_TIME, Utils.getTomorrowDate("yyyy-MM-dd HH:mm:ss"))
            .withSubject(subject).withMessageBody(messageBody)
            .withPlainTextType().callDoSendJson();
        doSettingsHandler()
            .withAuth(auth).withList(SETTINGS_SAVE_SENT).withParams(of(SETTINGS_SAVE_SENT, TRUE)).callDoSettings();
    }

    @Step("Помечаем письмо {0} как прочитанное")
    public ApiMessagesSteps markLetterRead(Message message) {
        if (message.getMid() != null)
            doMessagesHandler().withAuth(auth).withAction(MESSAGES_PARAM_ACTION_MARK).withIds(message.getMid())
                .callDoMessagesHandler();
        return this;
    }

    @Step("Помечаем письмо {0} как непрочитанное")
    public ApiMessagesSteps markLetterUnRead(Message message) {
        if (message.getMid() != null)
            doMessagesHandler().withAuth(auth).withAction(MESSAGES_PARAM_ACTION_UNMARK).withIds(message.getMid())
                .callDoMessagesHandler();
        return this;
    }

    @Step("Вызов api-метода: do-send-json. Подготавливаем черновик с рандомным текстом.")
    public String createDraftMessage() {
        String randText = Utils.getRandomName();
        doSendJsonHandler(SEND_JSON_PARAM_SAVE_UNDERSCORE, STATUS_TRUE).withAuth(auth).withNoSend()
            .withSaveSymbol(MESSAGES_PARAM_DRAFT).withSend(randText).callDoSendJson();
        return randText;
    }

    @Step("Вызов api-метода: do-send-json. Подготавливаем черновик с рандомным текстом.")
    public String createDraftWithSubject(String subject) {
        String randText = Utils.getRandomName();
        doSendJsonHandler(SEND_JSON_PARAM_SAVE_UNDERSCORE, STATUS_TRUE).withAuth(auth).withNoSend()
            .withSaveSymbol(MESSAGES_PARAM_DRAFT).withSubject(subject).withSend(randText).callDoSendJson();
        return randText;
    }

    @Step("Вызов api-метода: do-send-json. Подготавливаем шаблон с рандомным текстом.")
    public String createTemplateMessage(Account acc) {
        String subj = Utils.getRandomName();
        Folder folder = user.apiFoldersSteps().createTemplateFolder();
        doSendJsonHandler(SEND_JSON_PARAM_SAVE_UNDERSCORE, STATUS_TRUE).withAuth(auth).withNoSend()
            .withSaveSymbol(MESSAGES_PARAM_TEMPLATE)
            .withTemplatesFid(folder.getFid())
            .withSend(Utils.getRandomName()).withReceiver(acc.getSelfEmail()).withSubject(subj)
            .callDoSendJson();
        return subj;
    }

    @Step("Вызов api-метода: do-send-json. Подготавливаем {2} шаблонов с рандомным текстом.")
    public ApiMessagesSteps createTemplateMessage(Account acc, int count) {
        Folder folder = user.apiFoldersSteps().createTemplateFolder();
        ExecutorService threadPool = newCachedThreadPool();
        List<Future> tasks = new ArrayList<>();
        for (int msg = 1; msg <= count; ++msg) {
            String subject = "subj " + msg;
            tasks.add(
                threadPool.submit(() -> {
                    synchronized (ApiMessagesSteps.this) {
                        doSendJsonHandler(SEND_JSON_PARAM_SAVE_UNDERSCORE, STATUS_TRUE).withAuth(auth).withNoSend()
                            .withSaveSymbol(MESSAGES_PARAM_TEMPLATE)
                            .withTemplatesFid(folder.getFid())
                            .withSend(Utils.getRandomName()).withReceiver(acc.getSelfEmail()).withSubject(subject)
                            .callDoSendJson();
                        user.defaultSteps().waitInSeconds(0.5);
                    }
                })
            );
        }
        threadPool.shutdown();
        try {
            threadPool.awaitTermination(SESSION_TIMEOUT_VALUE_NUMBER, TimeUnit.SECONDS);
            for (Future task : tasks) {
                task.get();
            }
        } catch (ExecutionException e) {
            throw new AssertionError(e.getMessage());
        } catch (InterruptedException ignored) {
        } finally {
            user.apiSettingsSteps().callWithListAndParams(SETTINGS_SAVE_SENT, of(SETTINGS_SAVE_SENT, TRUE));
        }
        return this;
    }

    @Step("Вызов api-метода: do-send-json. Подготавливаем шаблон с темой и текстом.")
    public String createTemplateMessage(Account acc, String sbj, String body) {
        Folder folder = user.apiFoldersSteps().createTemplateFolder();
        doSendJsonHandler(SEND_JSON_PARAM_SAVE_UNDERSCORE, STATUS_TRUE).withAuth(auth).withNoSend()
            .withSaveSymbol(MESSAGES_PARAM_TEMPLATE)
            .withSubject(sbj)
            .withMessageBody(body)
            .withTemplatesFid(folder.getFid())
            .withSend(Utils.getRandomName()).withReceiver(acc.getSelfEmail())
            .callDoSendJson();
        return sbj;
    }

    @Step("Вызов api-метода: do-send-json. Подготавливаем шаблон с темой и текстом.")
    public String createTemplateMessage(String email, String sbj, String body) {
        Folder folder = user.apiFoldersSteps().createTemplateFolder();
        doSendJsonHandler(SEND_JSON_PARAM_SAVE_UNDERSCORE, STATUS_TRUE).withAuth(auth).withNoSend()
            .withSaveSymbol(MESSAGES_PARAM_TEMPLATE)
            .withSubject(sbj)
            .withMessageBody(body)
            .withTemplatesFid(folder.getFid())
            .withSend(Utils.getRandomName()).withReceiver(email)
            .callDoSendJson();
        return sbj;
    }

    @Step("Вызов api-метода: do-send-json. Подготавливаем шаблон с темой и текстом и аттачами.")
    public void createTemplateWithAttachmentsAndHTMLBody(Account acc, String subject, String messageBody,
                                                         String... attachNames) {
        Folder folder = user.apiFoldersSteps().createTemplateFolder();
        DoSendJsonHandler sendJsonHandler =
            doSendJsonHandler(SEND_JSON_PARAM_SAVE_UNDERSCORE, STATUS_TRUE).withAuth(auth).withNoSend()
                .withSaveSymbol(MESSAGES_PARAM_TEMPLATE)
                .withSubject(subject)
                .withMessageBody(messageBody)
                .withTemplatesFid(folder.getFid())
                .withSend(Utils.getRandomName()).withReceiver(acc.getSelfEmail())
                .withHtmlTextType();
        Arrays.asList(attachNames).forEach(attachName -> sendJsonHandler.withAttachId(uploadAttachment(attachName)));
        sendJsonHandler.callDoSendJson();
    }

    @Step("Помечаем все письма как прочитанные")
    public ApiMessagesSteps markAllMsgRead() {
        markAllMsgs(MESSAGES_PARAM_ACTION_MARK);
        return this;
    }

    @Step("Помечаем все письма как непрочитанные")
    public ApiMessagesSteps markAllMsgUnRead() {
        markAllMsgs(MESSAGES_PARAM_ACTION_UNMARK);
        return this;
    }

    private ApiMessagesSteps markAllMsgs(String markParam) {
        List<String> fids = Arrays.asList(user.apiFoldersSteps().getAllFids().split(","));
        if (!fids.isEmpty())
            fids.forEach(fid -> doMessagesHandler().withAuth(auth)
                .withAction(markParam).withFid(fid).callDoMessagesHandler());
        return this;
    }

    @Step("Создаем черновик c To: {0}")
    public String prepareDraft(String to, String subj, String send) {
        Response via = doSendJsonHandler(SEND_JSON_PARAM_SAVE_UNDERSCORE, STATUS_TRUE).withAuth(auth).withNoSend()
            .withSaveSymbol(MESSAGES_PARAM_DRAFT).withSend(send).withReceiver(to).withSubject(subj)
            .callDoSendJson();
        String storedmid = via.getBody().jsonPath().get("storedmid");
        System.out.print(storedmid);
        return via.getBody().jsonPath().get("storedmid");
    }

    @Step("Вызов api-метода: do-send-json. Отправляем письмо - To: «{0}», Тема: «{1}», Текст: «{2}». " +
        "В отправленных не сохраняем.")
    public ApiMessagesSteps sendMailWithNoSaveWithoutCheck(String email, String subject, String messageBody) {
        doSettingsHandler()
            .withAuth(auth).withList(SETTINGS_SAVE_SENT).withParams(of(SETTINGS_SAVE_SENT, FALSE)).callDoSettings();
        doSendJsonHandler().withAuth(auth).withReceiver(email).withSubject(subject).withMessageBody(messageBody)
            .withPlainTextType().callDoSendJson();
        deleteMessages(getMessageWithSubjectInFolder(subject, SENT));
        deleteMessages(getMessageWithSubjectInFolder(subject, TRASH));
        doSettingsHandler()
            .withAuth(auth).withList(SETTINGS_SAVE_SENT).withParams(of(SETTINGS_SAVE_SENT, TRUE)).callDoSettings();
        return this;
    }

    @Step("Количество писем в папке {0} должно равняться {1}")
    public void shouldGetMsgCountViaApi(String folder, int count) {
        assertThat(
            "Количество писем в папке не совпадает с ожидаемым",
            user.apiMessagesSteps().getAllMessagesInFolder(folder).size(),
            Matchers.equalTo(count)
        );
    }

    @Step("Количество писем в метке {0} должно равняться {1}")
    public void shouldGetMsgCountInLabelViaApi(String label, int count) {
        assertThat(
            "Количество писем в метке не совпадает с ожидаемым",
            user.apiMessagesSteps().getAllMessagesLabel(label).size(),
            Matchers.equalTo(count)
        );
    }

    @Step("Вызов api-метода: messages. Получаем все непрочитанные письма.")
    private List<Message> getAllUnreadMessages() {
        return Arrays.asList(messagesHandler().withAuth(auth).withMessagesPerPage(30).withOnlyUnread()
            .callMessagesHandler().then().extract().jsonPath(getJsonPathConfig())
            .getObject("models[0].data.message", Message[].class));
    }

    @Step("Отправляем тредное письмо к уже существующему письму «{0}»")
    public Message sendMessageToThreadWithMessage(Message msg, Account acc, String body) {
        String subject = msg.getSubject();
        int messagesWithSubject = getAllMessagesWithSubject(subject).size();
        doSendJsonHandler().withAuth(auth).withReceiver(acc.getSelfEmail()).withSubject(subject).withMessageBody(body)
            .withPlainTextType().withThreadToMessage(msg.getMessageId()).callDoSendJson();
        assertThat(
            "Тред отправился не полностью",
            auth,
            withWaitFor(hasThreadWithSubjectInList(subject, messagesWithSubject + 1))
        );
        return selectMax(
            select(getAllMessages(), having(on(Message.class).getSubject(), equalTo(subject))),
            on(Message.class).getMid()
        );
    }

    @Step("Отправляем тредное письмо к уже существующему письму «{0}», не проверяем, что дошло")
    public void sendMessageToThreadWithMessageWithoutCheck(Message msg, Account acc) {
        doSendJsonHandler().withAuth(auth).withReceiver(acc.getSelfEmail()).withSubject(msg.getSubject())
            .withPlainTextType().withThreadToMessage(msg.getMessageId()).callDoSendJson();
    }

    @Step("Отправляем тредное письмо к уже существующему письму с темой «{0}», не сохраняя в отправленных")
    public Message sendMessageToThreadWithSubjectWithNoSave(String subject, Account acc, String body) {
        doSettingsHandler()
            .withAuth(auth).withList(SETTINGS_SAVE_SENT).withParams(of(SETTINGS_SAVE_SENT, FALSE)).callDoSettings();
        Message msg = sendMessageToThreadWithSubject(subject, acc, body);
        deleteMessages(getMessageWithSubjectInFolder(subject, SENT));
        deleteMessages(getMessageWithSubjectInFolder(subject, TRASH));
        doSettingsHandler()
            .withAuth(auth).withList(SETTINGS_SAVE_SENT).withParams(of(SETTINGS_SAVE_SENT, TRUE)).callDoSettings();
        return msg;
    }

    @Step("Отправляем тредное письмо к уже существующему письму с темой {0}")
    public Message sendMessageToThreadWithSubject(String subject, Account acc, String body) {
        return sendMessageToThreadWithMessage(getMessageWithSubject(subject), acc, body);
    }

    @Step("Отправляем тредное письмо к уже существующему письму с темой {0}. Не проверяем приход письма.")
    public Message sendMessageToThreadWithSubjectWithoutCheck(String subject, Account acc, String body) {
        Message msg = getMessageWithSubject(subject);
        doSendJsonHandler().withAuth(auth).withReceiver(acc.getSelfEmail()).withSubject(subject).withMessageBody(body)
            .withPlainTextType().withThreadToMessage(msg.getMessageId()).callDoSendJson();
        return selectMax(
            select(getAllMessages(), having(on(Message.class).getSubject(), equalTo(subject))),
            on(Message.class).getMid()
        );
    }

    @Step("Создаем черновик в треде с темой «{1}» и получателем «{0}»")
    public String prepareDraftToThread(String to, String subj, String send) {
        Response via = doSendJsonHandler(SEND_JSON_PARAM_SAVE_UNDERSCORE, STATUS_TRUE).withAuth(auth).withNoSend()
            .withSaveSymbol(MESSAGES_PARAM_DRAFT).withSend(send).withReceiver(to).withSubject(subj)
            .withThreadToMessage(getMessageWithSubject(subj).getMessageId()).callDoSendJson();
        return via.getBody().jsonPath().get("storedmid");
    }

    @Step("Вызов api-метода: do-send-json. Отправляем письмо - Subj: «{0}», Text: «{1}», To: «{2}» " +
        "Не ищем письмо в инбоксе")
    public ApiMessagesSteps sendMailWithSeveralReceiversToThread(String subject, String messageBody, String... email) {
        doSendJsonHandler().withAuth(auth).withReceiver(String.join(",", email)).withSubject(subject)
            .withMessageBody(messageBody).withPlainTextType()
            .withThreadToMessage(getMessageWithSubject(subject).getMessageId()).callDoSendJson();
        return this;
    }

    @Step("Загружаем аттач")
    public String uploadAttachment(String attachName) {
        Response resp = uploadAttachmentHandler()
            .withAuth(auth)
            .withFile(new File(user.defaultSteps().getAttachPath(attachName)))
            .callUploadAttachment();
        return resp.getBody().jsonPath().get("write_attachment.id");
    }

    @Step("Вызов api-метода: do-send-json. Отправляем письмо с аттачем и html телом - To: «{0}», Subj: «{1}», " +
        "Attachments: «{3}».")
    public void sendMailWithAttachmentsAndHTMLBody(String email, String subject, String messageBody,
                                                   String... attachNames) {
        DoSendJsonHandler sendJsonHandler =
            doSendJsonHandler().withAuth(auth).withReceiver(email).withSubject(subject).withMessageBody(messageBody)
                .withHtmlTextType();
        Arrays.asList(attachNames).forEach(attachName -> sendJsonHandler.withAttachId(uploadAttachment(attachName)));
        sendJsonHandler.callDoSendJson();
        assertThat("Новое письмо не появилось в списке писем", auth, withWaitFor(hasMessageWithSubjectInList(subject)));
    }

    @Step("Отправляем тредное письмо с аттачами к уже существующему письму с темой {0}. Не проверяем приход письма.")
    public void sendMailWithAttachmentsToThreadWithSubject(String email, String subject,
                                                           String messageBody,
                                                           String... attachNames) {
        doSettingsHandler()
            .withAuth(auth).withList(SETTINGS_SAVE_SENT).withParams(of(SETTINGS_SAVE_SENT, FALSE)).callDoSettings();
        Message msg = getMessageWithSubject(subject);
        DoSendJsonHandler sendJsonHandler =
            doSendJsonHandler().withAuth(auth).withReceiver(email).withSubject(subject).withMessageBody(messageBody)
                .withHtmlTextType().withThreadToMessage(msg.getMessageId());
        Arrays.asList(attachNames).forEach(attachName -> sendJsonHandler.withAttachId(uploadAttachment(attachName)));
        sendJsonHandler.callDoSendJson();
        deleteMessages(getMessageWithSubjectInFolder(subject, SENT));
        deleteMessages(getMessageWithSubjectInFolder(subject, TRASH));
    }

    @Step("Вызов api-метода: do-send-json. Отправляем письмо с аттачем и html телом - To: «{0}», Subj: «{1}», " +
        "Attachments: «{3}». Не сохраняем в отправленных.")
    public void sendMailWithAttachmentsAndHTMLBodyNoSave(String email, String subject, String messageBody,
                                                         String... attachNames) {
        doSettingsHandler()
            .withAuth(auth).withList(SETTINGS_SAVE_SENT).withParams(of(SETTINGS_SAVE_SENT, FALSE)).callDoSettings();
        DoSendJsonHandler sendJsonHandler =
            doSendJsonHandler().withAuth(auth).withReceiver(email).withSubject(subject).withMessageBody(messageBody)
                .withHtmlTextType();
        Arrays.asList(attachNames).forEach(attachName -> sendJsonHandler.withAttachId(uploadAttachment(attachName)));
        sendJsonHandler.callDoSendJson();
        deleteMessages(getMessageWithSubjectInFolder(subject, SENT));
        deleteMessages(getMessageWithSubjectInFolder(subject, TRASH));
        assertThat("Новое письмо не появилось в списке писем", auth, withWaitFor(hasMessageWithSubjectInList(subject)));
    }

    @Step("Вызов api-метода: do-reply-later-create. Создаём напоминание об ответе для письма «{0}» на завтра. " +
        "Проверяем, кол-во писем в папке «Ответить позже» равно {1}")
    public void doReplyLaterForTomorrow(Message message, int count) {
        doReplyLaterCreateHandler().withAuth(auth)
            .withMid(message.getMid())
            .withDate(String.valueOf(System.currentTimeMillis() / 1000 + 86400))
            .callDoReplyLaterCreateHandler();
        assertThat(
            "Количество писем в папке не совпадает с ожидаемым",
            user.apiMessagesSteps().getAllMessagesInFolder(REPLY_LATER).size(),
            withWaitFor(Matchers.equalTo(count), 15000)
        );
    }

    @Step("Вызов api-метода: do-reply-later-create. Создаём напоминание об ответе для письма «{0}» через 1 секунду")
    @Description("На проде напоминание на письмо можно поставить только на через 5 минут")
    public void doReplyLater(Message message) {
        doReplyLaterCreateHandler().withAuth(auth)
            .withMid(message.getMid())
            .withDate(String.valueOf(System.currentTimeMillis() / 1000 + 1))
            .callDoReplyLaterCreateHandler();
        assertThat(
            "Количество писем в папке не совпадает с ожидаемым",
            user.apiMessagesSteps().getAllMessagesInFolder(REPLY_LATER).size(),
            withWaitFor(Matchers.equalTo(0), 15000)
        );
    }
}
