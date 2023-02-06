package ru.yandex.chemodan.app.psbilling.core.dao.email;

import java.util.UUID;

import org.joda.time.Instant;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.app.psbilling.core.AbstractPsBillingCoreTest;
import ru.yandex.chemodan.app.psbilling.core.dao.mail.EmailTemplateDao;
import ru.yandex.chemodan.app.psbilling.core.dao.mail.SentEmailInfoDao;
import ru.yandex.chemodan.app.psbilling.core.entities.mail.EmailTemplateEntity;
import ru.yandex.chemodan.app.psbilling.core.entities.mail.SentEmailInfoEntity;
import ru.yandex.chemodan.app.psbilling.core.utils.DateUtils;
import ru.yandex.misc.test.Assert;

public class SentEmailInfoDaoTests extends AbstractPsBillingCoreTest {
    @Autowired
    private EmailTemplateDao emailTemplateDao;
    @Autowired
    private SentEmailInfoDao sentEmailInfoDao;

    private EmailTemplateEntity emailTemplate;
    private final String userUid = UUID.randomUUID().toString();

    @Test
    public void create() {
        SentEmailInfoDao.InsertData data = SentEmailInfoDao.InsertData.builder()
                .emailTemplateKey(emailTemplate.getKey())
                .uid(userUid)
                .build();

        SentEmailInfoEntity actual = sentEmailInfoDao.createOrUpdate(data);
        Assert.equals(data.getEmailTemplateKey(), actual.getEmailTemplateKey());
        Assert.equals(data.getUid(), actual.getUid());
        Assert.equals(Instant.now(), actual.getSentDate());
    }

    @Test
    public void update() {
        SentEmailInfoDao.InsertData data = SentEmailInfoDao.InsertData.builder()
                .emailTemplateKey(emailTemplate.getKey())
                .uid(userUid)
                .build();

        sentEmailInfoDao.createOrUpdate(data);
        DateUtils.freezeTime(DateUtils.futureDate());

        SentEmailInfoEntity actual = sentEmailInfoDao.createOrUpdate(data);
        Assert.equals(data.getEmailTemplateKey(), actual.getEmailTemplateKey());
        Assert.equals(data.getUid(), actual.getUid());
        Assert.equals(Instant.now(), actual.getSentDate());
    }

    @Test
    public void find() {
        SentEmailInfoDao.InsertData data = SentEmailInfoDao.InsertData.builder()
                .emailTemplateKey(emailTemplate.getKey())
                .uid(userUid)
                .build();

        sentEmailInfoDao.createOrUpdate(data);

        Option<SentEmailInfoEntity> sentEmail = sentEmailInfoDao.find(userUid, emailTemplate.getKey());
        Assert.isTrue(sentEmail.isPresent());
    }

    @Before
    public void setup() {
        DateUtils.freezeTime();
        EmailTemplateDao.InsertData data = EmailTemplateDao.InsertData.builder()
                .key("key")
                .description("desc")
                .args(Option.of(Cf.list("arg1", "arg2")))
                .build();

        emailTemplate = emailTemplateDao.create(data);
    }

    @After
    public void cleanup() {
        DateUtils.unfreezeTime();
    }
}
