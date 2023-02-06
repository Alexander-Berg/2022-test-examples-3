package ru.yandex.chemodan.app.psbilling.core.utils;

import lombok.RequiredArgsConstructor;
import org.junit.Assert;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.app.psbilling.core.dao.mail.EmailTemplateDao;
import ru.yandex.chemodan.app.psbilling.core.entities.mail.LocalizedEmailTemplateEntity;
import ru.yandex.chemodan.app.psbilling.core.mail.MailContext;
import ru.yandex.chemodan.app.psbilling.core.mail.dataproviders.LocalizedEmailSenderDataProvider;
import ru.yandex.chemodan.app.psbilling.core.mail.dataproviders.model.SenderContext;
import ru.yandex.chemodan.app.psbilling.core.mail.tasks.BaseEmailTask;
import ru.yandex.chemodan.app.psbilling.core.mocks.BazingaTaskManagerMock;
import ru.yandex.chemodan.app.psbilling.core.tasks.BaseTask;
import ru.yandex.chemodan.test.ReflectionUtils;

@RequiredArgsConstructor
public class EmailHelper {
    private final EmailTemplateDao emailTemplateDao;
    private final BazingaTaskManagerMock bazingaTaskManagerMock;
    private final LocalizedEmailSenderDataProvider senderDataProvider;

    public void checkSingleLocalizedEmailArgs(Class<? extends BaseTask> taskClass, String emailKey,
                                              ListF<String> args) {
        checkMultipleLocalizedEmailArgs(taskClass, emailKey, args, 1);
    }

    public void checkMultipleLocalizedEmailArgs(Class<? extends BaseTask> taskClass, String emailKey,
                                                ListF<String> args, int count) {
        AssertHelper.assertSize(bazingaTaskManagerMock.findTasks(taskClass), count);
        createTemplateWithRuLocalization(emailKey, args);
        bazingaTaskManagerMock.findTasks(taskClass).forEach(tuple -> {
            BaseTask<?> task = tuple._1;
            BaseEmailTask.Parameters params = ((BaseEmailTask.Parameters) ((Option) ReflectionUtils.getField(task, "parameters")).get());
            Assert.assertEquals(emailKey, params.getEmailKey());
            MailContext mailContext = params.getContext();

            SenderContext senderContext = senderDataProvider.buildSenderContext(params.getEmailKey(), mailContext).get();
            ListF<String> realArgs = senderContext.getArgs().keys();
            Assert.assertEquals(args, realArgs);
        });
    }
    public void createTemplateWithRuLocalization(String emailKey, ListF<String> args) {
        createEmailTemplate(emailKey, "whatever", Option.of(args));
        createLocalizedEmailTemplate(emailKey, "ru", "Yandex.Mail:whatever");
    }

    public String createEmailTemplate(String key, String desc, Option<ListF<String>> args) {
        return emailTemplateDao.create(EmailTemplateDao.InsertData.builder()
                .key(key).description(desc).args(args).build()).getKey();
    }

    public void createLocalizedEmailTemplate(String templateKey, String language, String senderKey) {
        emailTemplateDao.mergeLocalizations(Cf.list(new LocalizedEmailTemplateEntity(templateKey, language,
                senderKey)));
    }
}
