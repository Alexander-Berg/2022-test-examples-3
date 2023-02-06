package ru.yandex.chemodan.app.psbilling.core.promos;

import java.util.UUID;

import org.joda.time.Duration;
import org.joda.time.Instant;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.app.psbilling.core.dao.mail.EmailTemplateDao;
import ru.yandex.chemodan.app.psbilling.core.dao.products.ProductLineDao;
import ru.yandex.chemodan.app.psbilling.core.dao.products.ProductSetDao;
import ru.yandex.chemodan.app.psbilling.core.dao.promos.PsBillingPromoCoreTest;
import ru.yandex.chemodan.app.psbilling.core.dao.promos.UserPromoDao;
import ru.yandex.chemodan.app.psbilling.core.dao.texts.TankerKeyDao;
import ru.yandex.chemodan.app.psbilling.core.entities.CustomPeriod;
import ru.yandex.chemodan.app.psbilling.core.entities.CustomPeriodUnit;
import ru.yandex.chemodan.app.psbilling.core.entities.products.ProductLineEntity;
import ru.yandex.chemodan.app.psbilling.core.entities.products.ProductSetEntity;
import ru.yandex.chemodan.app.psbilling.core.entities.products.UserProductEntity;
import ru.yandex.chemodan.app.psbilling.core.entities.products.UserProductPriceEntity;
import ru.yandex.chemodan.app.psbilling.core.entities.promos.PromoApplicationType;
import ru.yandex.chemodan.app.psbilling.core.entities.promos.PromoPayloadEntity;
import ru.yandex.chemodan.app.psbilling.core.entities.promos.PromoStatusType;
import ru.yandex.chemodan.app.psbilling.core.entities.promos.PromoTemplateEntity;
import ru.yandex.chemodan.app.psbilling.core.entities.promos.UserPromoEntity;
import ru.yandex.chemodan.app.psbilling.core.mail.tasks.BaseEmailTask;
import ru.yandex.chemodan.app.psbilling.core.mail.tasks.SendLocalizedEmailTask;
import ru.yandex.chemodan.app.psbilling.core.mocks.Blackbox2MockConfiguration;
import ru.yandex.chemodan.app.psbilling.core.utils.DateUtils;
import ru.yandex.chemodan.util.exception.BadRequestException;
import ru.yandex.chemodan.util.exception.NotFoundException;
import ru.yandex.inside.passport.PassportUid;
import ru.yandex.misc.test.Assert;

public class PromoServiceTests extends PsBillingPromoCoreTest {
    private final PassportUid userUid = PassportUid.MAX_VALUE;

    @Autowired
    ProductLineDao productLineDao;

    @Autowired
    protected ApplicationContext applicationContext;

    @Autowired
    ProductSetDao productSetDao;

    @Autowired
    private TankerKeyDao tankerKeyDao;

    private String promoKey;
    private String promoPayloadType;
    private int promoPayloadVersion;
    private String tankerProject;
    private String tankerKeySet;
    private String tankerKey;
    private String promoPayloadContent;

    @Test
    public void activatePromoForUser_Global() {
        PromoTemplateEntity promoTemplate = promoHelper.createGlobalPromo();

        Assert.assertThrows(() -> promoService.activatePromoForUser(userUid, promoTemplate.getId()),
                BadRequestException.class);
    }

    @Test
    public void activatePromoForUser_Simple() {
        PromoTemplateEntity promoTemplate = promoHelper.createUserPromo();

        promoService.activatePromoForUser(userUid, promoTemplate.getId());

        ListF<UserPromoEntity> userPromos = userPromoDao.findUserPromos(userUid);
        Assert.assertEquals(1, userPromos.length());
        UserPromoEntity userPromo = userPromos.single();
        Assert.equals(userPromo.getUid(), userUid);
        Assert.equals(userPromo.getPromoTemplateId(), promoTemplate.getId());
        Assert.equals(userPromo.getFromDate(), Instant.now());
        Assert.equals(userPromo.getToDate(), Option.empty());
        Assert.equals(userPromo.getStatus(), PromoStatusType.ACTIVE);
    }

    @Test
    public void activatePromoForUser_PromoNotExist() {
        Assert.assertThrows(() -> promoService.activatePromoForUser(userUid, UUID.randomUUID()),
                NotFoundException.class);
    }

    @Test
    public void activatePromoForUser_NotStarted() {
        datesTest(DateUtils.futureDate(), null, DateUtils.futureDate(), null);
    }

    @Test
    public void activatePromoForUser_Expired() {
        PromoTemplateEntity promoTemplate =
                promoHelper.createUserPromo(b -> b.fromDate(DateUtils.farPastDate()).toDate(Option.of(DateUtils.pastDate())));

        Assert.assertThrows(() -> promoService.activatePromoForUser(userUid, promoTemplate.getId()),
                BadRequestException.class);
    }

    @Test
    public void activatePromoForUser_Actual_EndDate() {
        DateUtils.freezeTime(Instant.parse("2021-04-05T12:00:00+0900"));
        datesTest("2021-04-01T12:00:00+0900", "2021-04-06T12:00:00+0900",
                "2021-04-05T12:00:00+0900", "2021-04-07T00:00:00+0900");
    }

    @Test
    public void activatePromoForUser_Actual_NoEndDate() {
        datesTest(DateUtils.pastDate(), null, Instant.now(), null);
    }

    @Test
    public void activatePromoForUser_Duration_EndDateIsFar() {
        DateUtils.freezeTime(Instant.parse("2021-04-05T12:00:00+0900"));
        datesTest("2021-04-05T12:00:00+0900", "2021-04-10T12:00:00+0900",
                CustomPeriod.fromDays(2),
                "2021-04-05T12:00:00+0900", "2021-04-08T00:00:00+0900");
    }

    @Test
    public void activatePromoForUser_Duration_EndDateIsClose() {
        DateUtils.freezeTime(Instant.parse("2021-04-05T12:00:00+0900"));
        datesTest("2021-01-01", "2021-04-06T12:00:00+0900",
                CustomPeriod.fromDays(2),
                "2021-04-05T12:00:00+0900", "2021-04-07T00:00:00+0900");
    }

    @Test
    public void activatePromoForUser_Date_Midnight() {
        DateUtils.freezeTime(Instant.parse("2021-04-05T00:00:00+0900"));
        datesTest("2021-04-01T12:00:00+0900", "2021-04-06T00:00:00+0900",
                "2021-04-05T00:00:00+0900", "2021-04-06T00:00:00+0900");
    }

    @Test
    public void activatePromoForUser_Duration_Midnight() {
        DateUtils.freezeTime(Instant.parse("2021-04-05T00:00:00+0900"));
        datesTest("2021-04-01T12:00:00+0900", "2021-04-15T00:00:00+0900", CustomPeriod.fromDays(1),
                "2021-04-05T00:00:00+0900", "2021-04-06T00:00:00+0900");
    }

    @Test
    public void activatePromoForUser_DifferentTimeZonesWithUser() {
        DateUtils.freezeTime(Instant.parse("2021-04-05T12:00:00+0300"));
        datesTest("2021-04-01T12:00:00+0300", "2021-04-06T19:00:00+0300",
                "2021-04-05T12:00:00+0300", "2021-04-08T00:00:00+0900");
    }

    @Test
    public void activatePromoForUser_DifferentTimeZonesWithUser_Midnight() {
        DateUtils.freezeTime(Instant.parse("2021-04-05T12:00:00+0300"));
        datesTest("2021-04-01T12:00:00+0300", "2021-04-06T18:00:00+0300",
                "2021-04-05T12:00:00+0300", "2021-04-07T00:00:00+0900");
    }

    @Test
    public void activatePromoForUser_ActivateIfUsed_False() {
        PromoTemplateEntity promoTemplate = promoHelper.createUserPromo();
        userPromoDao.createOrUpdate(UserPromoDao.InsertData.builder()
                .uid(userUid)
                .fromDate(Instant.now())
                .promoTemplateId(promoTemplate.getId())
                .promoStatusType(PromoStatusType.USED)
                .build());

        boolean activated = promoService.activatePromoForUser(userUid, promoTemplate.getCode(), false, false);
        Assert.isFalse(activated);
        ListF<UserPromoEntity> userPromos = userPromoDao.findUserPromos(userUid);
        Assert.equals(1, userPromos.length());
        Assert.equals(PromoStatusType.USED, userPromos.single().getStatus());
    }

    @Test
    public void activatePromoForUser_ActivateIfUsed_True() {
        PromoTemplateEntity promoTemplate = promoHelper.createUserPromo();
        userPromoDao.createOrUpdate(UserPromoDao.InsertData.builder()
                .uid(userUid)
                .fromDate(Instant.now())
                .promoTemplateId(promoTemplate.getId())
                .promoStatusType(PromoStatusType.USED)
                .build());

        boolean activated = promoService.activatePromoForUser(userUid, promoTemplate.getCode(), true, false);
        Assert.isTrue(activated);
        ListF<UserPromoEntity> userPromos = userPromoDao.findUserPromos(userUid);
        Assert.equals(1, userPromos.length());
        Assert.equals(PromoStatusType.ACTIVE, userPromos.single().getStatus());
    }

    @Test
    public void activatePromoForUser_SendEmail_WithPromoEmail() {
        String emailTemplateKey = emailTemplateDao.create(EmailTemplateDao.InsertData.builder()
                .key("got_new_promo").description("my awesome promo").build())
                .getKey();
        PromoTemplateEntity promoTemplate =
                promoHelper.createUserPromo(b -> b.activationEmailTemplate(Option.of(emailTemplateKey)));

        promoService.activatePromoForUser(userUid, promoTemplate.getCode(), false, true);

        Assert.assertEquals(1, userPromoDao.findUserPromos(userUid).length());
        Assert.equals(1, bazingaTaskManagerStub.tasksWithParams.length());
        SendLocalizedEmailTask sentTask = (SendLocalizedEmailTask) bazingaTaskManagerStub.tasksWithParams.get(0)._1;
        BaseEmailTask.Parameters taskParams = sentTask.getParametersTyped();
        Assert.equals(taskParams.getEmailKey(), promoTemplate.getActivationEmailTemplate().get());
        Assert.equals(taskParams.getContext().getTo(), userUid);
        Assert.equals(taskParams.getContext().getPromoId().get(), promoTemplate.getId());
    }

    @Test
    public void activatePromoForUser_SendEmail_Twice_WithPromoEmail() {
        DateUtils.freezeTime(Instant.parse("2021-04-01T00:00:00+0900"));
        String emailTemplateKey = emailTemplateDao.create(EmailTemplateDao.InsertData.builder()
                .key("got_new_promo").description("my awesome promo").build())
                .getKey();
        PromoTemplateEntity promoTemplate =
                promoHelper.createUserPromo(b -> b
                        .activationEmailTemplate(Option.of(emailTemplateKey))
                        .duration(Option.of(CustomPeriod.fromDays(5))));

        promoService.activatePromoForUser(userUid, promoTemplate.getCode(), false, true);

        Assert.assertEquals(1, userPromoDao.findUserPromos(userUid).length());
        Assert.equals(Instant.parse("2021-04-06T00:00:00+0900"),
                userPromoDao.findUserPromos(userUid).get(0).getToDate().get());

        bazingaTaskManagerStub.executeTasks(applicationContext);
        DateUtils.shiftTime(Duration.standardDays(1));
        Assert.isFalse(promoService.activatePromoForUser(userUid, promoTemplate.getCode(), false, true));

        // мы не должны слать письмо второй раз, если акция еще активна
        Assert.equals(0, bazingaTaskManagerStub.tasksWithParams.length());

        DateUtils.shiftTime(Duration.standardDays(10));
        promoService.activatePromoForUser(userUid, promoTemplate.getCode(), false, true);
        // а если уже кончилась- то можно и отослать
        Assert.equals(1, bazingaTaskManagerStub.tasksWithParams.length());
    }

    @Test
    public void activatePromoForUser_SendEmail_WithoutPromoEmail() {
        PromoTemplateEntity promoTemplate = promoHelper.createUserPromo();

        promoService.activatePromoForUser(userUid, promoTemplate.getCode(), false, true);

        Assert.assertEquals(1, userPromoDao.findUserPromos(userUid).length());
        Assert.assertEmpty(bazingaTaskManagerStub.tasksWithParams);
    }

    @Test
    public void activatePromoForUser_DoNotSendEmail() {
        PromoTemplateEntity promoTemplate = promoHelper.createUserPromo();

        promoService.activatePromoForUser(userUid, promoTemplate.getId());

        Assert.assertEquals(1, userPromoDao.findUserPromos(userUid).length());
        Assert.assertEmpty(bazingaTaskManagerStub.tasksWithParams);
    }

    @Test
    public void setPromoUsedState_TwoPromosWithSameLine() {
        // CHEMODAN-77271: Пользователь со скидочным тарифом может воспользоваться скидкой при апгрейде
        // не получали отметку об использовании акции, если была активная, но неактивированная для пользователя,
        // акция с типом MULTIPLE_TIME
        PromoTemplateEntity promoTemplate1 =
                promoHelper.createUserPromo(x -> x.applicationType(PromoApplicationType.ONE_TIME));
        PromoTemplateEntity promoTemplate2 =
                promoHelper.createUserPromo(x -> x.applicationType(PromoApplicationType.MULTIPLE_TIME));
        ProductSetEntity productSet =
                productSetDao.create(ProductSetDao.InsertData.builder().key(UUID.randomUUID().toString()).build());
        ProductLineEntity productLine =
                productLineDao.create(ProductLineDao.InsertData.builder().productSetId(productSet.getId()).orderNum(1).build());
        UserProductEntity product = psBillingProductsFactory.createUserProduct();
        UserProductPriceEntity price = psBillingProductsFactory.createUserProductPrices(product,
                CustomPeriodUnit.ONE_MONTH);

        productLineDao.bindUserProducts(productLine.getId(), Cf.list(product.getId()));

        promoTemplateDao.bindProductLines(promoTemplate1.getId(), productLine.getId());
        promoTemplateDao.bindProductLines(promoTemplate2.getId(), productLine.getId());

        promoService.activatePromoForUser(userUid, promoTemplate1.getId());
        Option<String> promoUsed = promoService.setPromoUsedState(userProductManager, userUid, price.getId());

        UserPromoEntity userPromo = userPromoDao.findUserPromos(userUid).single();
        Assert.equals(userPromo.getUid(), userUid);
        Assert.equals(userPromo.getPromoTemplateId(), promoTemplate1.getId());
        Assert.some(promoTemplate1.getCode(), promoUsed);
        Assert.equals(userPromo.getStatus(), PromoStatusType.USED);
    }

    @Test
    public void setPayload_existingPromo() {
        setupPromoPayloadTest();

        PromoTemplateEntity template = promoHelper.createGlobalPromo(promoKey);
        Option<PromoPayloadEntity> payload = promoPayloadDao.get(template.getId(),
                promoPayloadType, Option.of(promoPayloadVersion));
        Assert.assertTrue(payload.isEmpty());
        Assert.assertFalse(tankerKeyDao.exists(tankerProject, tankerKeySet, tankerKey));

        promoService.setPayload(promoKey, promoPayloadType, promoPayloadVersion,
                promoPayloadContent);

        payload = promoPayloadDao.get(template.getId(), promoPayloadType,
                Option.of(promoPayloadVersion));
        Assert.assertTrue(payload.isPresent());
        Assert.equals(payload.get().getPayloadType(), promoPayloadType);
        Assert.equals(payload.get().getVersion(), promoPayloadVersion);
        Assert.equals(payload.get().getContent(), promoPayloadContent);
        Assert.assertTrue(tankerKeyDao.exists(tankerProject, tankerKeySet, tankerKey));
    }

    @Test
    public void setPayload_notExistingPromo() {
        setupPromoPayloadTest();

        Assert.assertThrows(() -> promoService.setPayload(promoKey, promoPayloadType,
                promoPayloadVersion, promoPayloadContent),
                RuntimeException.class);
    }

    @Test
    public void setPayload_alreadyExistingPayload() {
        setupPromoPayloadTest();

        PromoTemplateEntity template = promoHelper.createGlobalPromo(promoKey);
        createPayload(template, promoPayloadType, promoPayloadVersion);
        Assert.assertFalse(tankerKeyDao.exists(tankerProject, tankerKeySet, tankerKey));

        promoService.setPayload(promoKey, promoPayloadType, promoPayloadVersion,
                promoPayloadContent);

        Option<PromoPayloadEntity> payload = promoPayloadDao.get(template.getId(),
                promoPayloadType, Option.of(promoPayloadVersion));
        Assert.assertTrue(payload.isPresent());
        Assert.equals(payload.get().getPayloadType(), promoPayloadType);
        Assert.equals(payload.get().getVersion(), promoPayloadVersion);
        Assert.equals(payload.get().getContent(), promoPayloadContent);
        Assert.assertTrue(tankerKeyDao.exists(tankerProject, tankerKeySet, tankerKey));
    }

    @Before
    public void setup() {
        super.setup();
        blackbox2MockConfig.mockUserInfo(userUid, Blackbox2MockConfiguration.getBlackboxResponse("login", "firstName",
                Option.empty(), Option.empty(), Option.of("ru"), Option.of("Asia/Tokyo"), Option.empty())); // UTC+9
    }

    private void datesTest(String fromDate, String toDate, String expectedFromDate, String expectedToDate) {
        datesTest(Instant.parse(fromDate), Instant.parse(toDate),
                Instant.parse(expectedFromDate), Instant.parse(expectedToDate));
    }

    private void datesTest(Instant fromDate, Instant toDate, Instant expectedFromDate, Instant expectedToDate) {
        datesTest(fromDate, toDate, Option.empty(), expectedFromDate, expectedToDate);
    }

    private void datesTest(String fromDate, String toDate, CustomPeriod duration,
                           String expectedFromDate, String expectedToDate) {
        datesTest(Instant.parse(fromDate), Instant.parse(toDate), Option.of(duration),
                Instant.parse(expectedFromDate), Instant.parse(expectedToDate));
    }

    private void datesTest(Instant fromDate, Instant toDate, Option<CustomPeriod> duration,
                           Instant expectedFromDate, Instant expectedToDate) {
        PromoTemplateEntity promoTemplate = promoHelper.createUserPromo(b -> b.fromDate(fromDate)
                .toDate(Option.ofNullable(toDate))
                .duration(duration));

        promoService.activatePromoForUser(userUid, promoTemplate.getId());

        UserPromoEntity userPromo = userPromoDao.findUserPromos(userUid).single();
        Assert.equals(expectedFromDate, userPromo.getFromDate());
        Assert.equals(expectedToDate, userPromo.getToDate().getOrNull());
    }

    private void setupPromoPayloadTest() {
        promoKey = "test_promo";
        promoPayloadType = "web_disk";
        promoPayloadVersion = 0;
        tankerProject = "test_tanker_project";
        tankerKeySet = "test_tanker_key_set";
        tankerKey = "test_tanker_key";
        promoPayloadContent = "{\n" +
                "  \"text_tanker\": { \"tanker_project\": \"" + tankerProject + "\", " +
                "\"tanker_key_set\": \"" + tankerKeySet + "\", " +
                "\"tanker_key\": \"" + tankerKey + "\" },\n" +
                "  \"theme\": \"dark\",\n" +
                "  \"withSubtitle\": false\n" +
                "}";
    }
}
