package ru.yandex.chemodan.app.psbilling.core.mail.sending;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.util.EntityUtils;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.app.psbilling.core.dao.groups.GroupServicePriceOverrideDao;
import ru.yandex.chemodan.app.psbilling.core.dao.mail.EmailTemplateDao;
import ru.yandex.chemodan.app.psbilling.core.dao.mail.SentEmailInfoDao;
import ru.yandex.chemodan.app.psbilling.core.mail.MailContext;
import ru.yandex.chemodan.app.psbilling.core.mocks.Blackbox2MockConfiguration;
import ru.yandex.chemodan.app.psbilling.core.tasks.execution.TaskScheduler;
import ru.yandex.chemodan.app.psbilling.core.utils.EmailHelper;
import ru.yandex.inside.passport.PassportUid;
import ru.yandex.inside.passport.blackbox2.protocol.response.BlackboxCorrectResponse;
import ru.yandex.misc.email.Email;
import ru.yandex.misc.io.http.UrlUtils;
import ru.yandex.misc.regex.Pattern2;
import ru.yandex.misc.test.Assert;

@SuppressWarnings("SameParameterValue")
public class SendLocalizedEmailTest extends BaseEmailSendTest {
    private final PassportUid userUid = PassportUid.MAX_VALUE;
    private final String userFirstName = "First Name";

    @Autowired
    GroupServicePriceOverrideDao groupServicePriceOverrideDao;
    @Autowired
    EmailTemplateDao emailTemplateDao;
    @Autowired
    TaskScheduler taskScheduler;
    @Autowired
    SentEmailInfoDao sentEmailInfoDao;
    @Autowired
    EmailHelper emailHelper;

    @Test
    public void simpleSend() {
        String templateKey = emailHelper.createEmailTemplate("key", "desc", Option.empty());
        emailHelper.createLocalizedEmailTemplate(templateKey, "ru", "Disk:RussianTemplateCode");
        emailHelper.createLocalizedEmailTemplate(templateKey, "en", "Disk:EnglishTemplateCode");
        setUserLanguageInBlackBox(userUid, "ru");

        executeEmailTask(templateKey, userUid);

        HttpUriRequest request = mailSenderMockConfig.verifyEmailSent();
        Assert.assertContains(request.getURI().toString(), "Disk");
        Assert.assertContains(request.getURI().toString(), "RussianTemplateCode");
        Assert.assertContains(request.getURI().toString(), userUid.toString());

        Assert.isTrue(sentEmailInfoDao.find(userUid.toString(), "key").isPresent());
    }

    @Test
    public void sendWithError() {
        String templateKey = emailHelper.createEmailTemplate("key", "desc", Option.empty());
        emailHelper.createLocalizedEmailTemplate(templateKey, "ru", "Disk:RussianTemplateCode");
        setUserLanguageInBlackBox(userUid, "ru");

        mailSenderMockConfig.mockHttpClientResponse(400, "error");
        executeEmailTask(templateKey, userUid);
        Assert.isFalse(sentEmailInfoDao.find(userUid.toString(), "key").isPresent());
    }

    @Test
    public void sendWithArgs() throws Exception {
        ListF<String> args = Cf.list("public_display_name");
        String templateKey = emailHelper.createEmailTemplate("key", "desc", Option.of(args));
        emailHelper.createLocalizedEmailTemplate(templateKey, "ru", "Disk:RussianTemplateCode");
        emailHelper.createLocalizedEmailTemplate(templateKey, "en", "Disk:EnglishTemplateCode");
        setUserLanguageInBlackBox(userUid, "ru");

        executeEmailTask(templateKey, userUid);

        HttpPost request = mailSenderMockConfig.verifyEmailSent();
        Assert.assertContains(request.getURI().toString(), "Disk");
        Assert.assertContains(request.getURI().toString(), "RussianTemplateCode");
        Assert.assertContains(request.getURI().toString(), userUid.toString());

        String requestBody = EntityUtils.toString(request.getEntity());
        ObjectNode node = new ObjectMapper().readValue(requestBody, ObjectNode.class);
        Assert.isTrue(node.has("args"));
        Assert.isTrue(node.get("args").has("public_display_name"));
        Assert.equals(node.get("args").get("public_display_name").textValue(), userFirstName);
    }

    @Test
    public void sendToEmailAddress() {
        String templateKey = emailHelper.createEmailTemplate("key", "desc", Option.empty());
        emailHelper.createLocalizedEmailTemplate(templateKey, "ru", "Disk:RussianTemplateCode");
        setUserLanguageInBlackBox(userUid, "ru");

        Email email = new Email("user@somewhere.com");

        MailContext context = MailContext.builder().email(Option.of(email)).to(userUid).build();
        executeEmailTask(templateKey, context);

        String uri = mailSenderMockConfig.verifyEmailSent().getURI().toString();
        Assert.assertContains(uri, UrlUtils.urlEncode(email.getEmail()));
        Assert.isFalse(uri.contains(userUid.toString()));

        Assert.some(sentEmailInfoDao.find(email.getEmail(), templateKey));
        Assert.none(sentEmailInfoDao.find(userUid.toString(), templateKey));
    }

    @Test
    public void sendWithNoEmailLocale() {
        String templateKey = emailHelper.createEmailTemplate("key", "desc", Option.empty());
        emailHelper.createLocalizedEmailTemplate(templateKey, "en", "Disk:EnglishTemplateCode");
        setUserLanguageInBlackBox(userUid, "ru");

        executeEmailTask(templateKey, userUid);
        mailSenderMockConfig.verifyNoEmailSent();
    }

    @Test
    public void sendToBelarusianWithRu() {
        setUserLanguageInBlackBox(userUid, "be");
        Assert.equals("ru", createAndSendLocalizedEmailReturnLang("ru", "en"));
    }

    @Test
    public void sendToFrenchmanWithEn() {
        setUserLanguageInBlackBox(userUid, "fr");
        Assert.equals("en", createAndSendLocalizedEmailReturnLang("ru", "en"));
    }

    @Test
    public void sendToTurkWithNative() {
        setUserLanguageInBlackBox(userUid, "tr");
        Assert.equals("tr", createAndSendLocalizedEmailReturnLang("tr", "ru", "en"));
    }

    private String createAndSendLocalizedEmailReturnLang(String... locales) {
        String templateKey = emailHelper.createEmailTemplate("key", "desc", Option.empty());

        for (String locale : locales) {
            emailHelper.createLocalizedEmailTemplate(templateKey, locale, "Disk:SenderKeyFor" + locale);
        }
        executeEmailTask(templateKey, userUid);

        String requestUri = mailSenderMockConfig.verifyEmailSent().getURI().toString();
        return Pattern2.compile("SenderKeyFor([a-z]+)").findNthGroup(requestUri, 1).get();
    }

    private void executeEmailTask(String templateKey, PassportUid uid) {
        executeEmailTask(templateKey, MailContext.builder().to(uid).promoId(Option.empty()).build());
    }

    private void executeEmailTask(String templateKey, MailContext context) {
        taskScheduler.schedulePromoEmailTask(templateKey, context);
        bazingaTaskManagerStub.executeTasks(applicationContext);
    }

    private void setUserLanguageInBlackBox(PassportUid uid, String language) {
        BlackboxCorrectResponse resp = Blackbox2MockConfiguration.getBlackboxResponse("login", userFirstName,
                Option.empty(),
                Option.empty(), Option.of(language), Option.empty(), Option.empty());
        blackbox2MockConfig.mockUserInfo(uid, resp);
    }
}

