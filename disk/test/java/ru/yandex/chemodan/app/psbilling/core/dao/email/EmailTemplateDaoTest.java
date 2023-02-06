package ru.yandex.chemodan.app.psbilling.core.dao.email;

import java.util.NoSuchElementException;

import org.joda.time.Instant;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.app.psbilling.core.AbstractPsBillingCoreTest;
import ru.yandex.chemodan.app.psbilling.core.dao.mail.EmailTemplateDao;
import ru.yandex.chemodan.app.psbilling.core.entities.mail.EmailTemplateEntity;
import ru.yandex.chemodan.app.psbilling.core.entities.mail.LocalizedEmailTemplateEntity;
import ru.yandex.chemodan.app.psbilling.core.utils.DateUtils;
import ru.yandex.misc.test.Assert;

public class EmailTemplateDaoTest extends AbstractPsBillingCoreTest {

    @Autowired
    private EmailTemplateDao emailTemplateDao;

    @Test
    public void create() {
        EmailTemplateDao.InsertData data = EmailTemplateDao.InsertData.builder()
                .key("key")
                .description("desc")
                .args(Option.of(Cf.list("arg1", "arg2")))
                .build();

        EmailTemplateEntity actual = emailTemplateDao.create(data);
        Assert.equals(actual.getKey(), data.getKey());
        Assert.equals(actual.getDescription(), data.getDescription());
        Assert.assertListsEqual(actual.getArgs(), data.getArgs().get());
        Assert.equals(actual.getCreatedAt(), Instant.now());
    }

    @Test
    public void createWithNoArgs() {
        EmailTemplateDao.InsertData data = EmailTemplateDao.InsertData.builder()
                .key("key")
                .description("desc")
                .args(Option.of(Cf.list()))
                .build();

        EmailTemplateEntity actual = emailTemplateDao.create(data);
        Assert.equals(actual.getKey(), data.getKey());
        Assert.equals(actual.getDescription(), data.getDescription());
        Assert.assertEmpty(actual.getArgs());
        Assert.equals(actual.getCreatedAt(), Instant.now());
    }

    @Test
    public void findByKey() {
        Assert.assertThrows(() -> emailTemplateDao.findByKey("key"), NoSuchElementException.class);
        EmailTemplateEntity createdEntity = emailTemplateDao.create(
                EmailTemplateDao.InsertData.builder().key("key").description("description").build());
        Assert.equals(createdEntity, emailTemplateDao.findByKey("key"));
    }

    @Test
    public void findByKeyO() {
        Assert.isFalse(emailTemplateDao.findByKeyO("key").isPresent());
        EmailTemplateEntity createdEntity = emailTemplateDao.create(
                EmailTemplateDao.InsertData.builder().key("key").description("description").build());
        Assert.equals(createdEntity, emailTemplateDao.findByKeyO("key").get());
    }

    @Test
    public void mergeLocalizations() {
        String templateKey = emailTemplateDao.create(EmailTemplateDao.InsertData.builder()
                .key("key").description("desc").build()).getKey();

        ListF<LocalizedEmailTemplateEntity> expectedLocalizations = Cf.list(
                new LocalizedEmailTemplateEntity(templateKey, "ru", "ruSenderCode"),
                new LocalizedEmailTemplateEntity(templateKey, "en", "enSenderCode"));

        emailTemplateDao.mergeLocalizations(expectedLocalizations);
        ListF<LocalizedEmailTemplateEntity> actualLocalizations = emailTemplateDao.findLocalizations(templateKey);

        Assert.assertListsEqual(expectedLocalizations, actualLocalizations);
    }

    @Test
    public void mergeLocalizationsWithConflict() {
        String templateKey = emailTemplateDao.create(EmailTemplateDao.InsertData.builder()
                .key("key").description("desc").build()).getKey();

        LocalizedEmailTemplateEntity ruLocalization =
                new LocalizedEmailTemplateEntity(templateKey, "ru", "ruSenderCode");
        LocalizedEmailTemplateEntity enLocalization =
                new LocalizedEmailTemplateEntity(templateKey, "en", "enSenderCode");

        emailTemplateDao.mergeLocalizations(Cf.list(ruLocalization, enLocalization));

        LocalizedEmailTemplateEntity newRuLocalization =
                new LocalizedEmailTemplateEntity(templateKey, "ru", "newRuSenderCode");

        emailTemplateDao.mergeLocalizations(Cf.list(newRuLocalization));

        ListF<LocalizedEmailTemplateEntity> actualLocalizations = emailTemplateDao.findLocalizations(templateKey);
        Assert.assertListsEqual(Cf.list(newRuLocalization, enLocalization), actualLocalizations);
    }

    @Before
    public void setup(){
        DateUtils.freezeTime();
    }
    @After
    public void cleanup(){
        DateUtils.unfreezeTime();
    }
}
