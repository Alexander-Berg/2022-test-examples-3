package ru.yandex.chemodan.app.psbilling.core.mail.keyProviders;

import java.util.UUID;

import org.joda.time.Instant;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.app.psbilling.core.AbstractPsBillingCoreTest;
import ru.yandex.chemodan.app.psbilling.core.dao.promos.UserPromoDao;
import ru.yandex.chemodan.app.psbilling.core.entities.promos.PromoTemplateEntity;
import ru.yandex.chemodan.app.psbilling.core.entities.promos.UserPromoEntity;
import ru.yandex.chemodan.app.psbilling.core.mail.MailContext;
import ru.yandex.chemodan.app.psbilling.core.mail.keydataproviders.UserPromoKeyProvider;
import ru.yandex.chemodan.app.psbilling.core.utils.DateUtils;
import ru.yandex.chemodan.app.psbilling.core.utils.PromoHelper;
import ru.yandex.inside.passport.PassportUid;
import ru.yandex.inside.utils.Language;
import ru.yandex.misc.test.Assert;

public class UserPromoKeyProviderTest extends AbstractPsBillingCoreTest {
    @Autowired
    private UserPromoKeyProvider userPromoKeyProvider;
    @Autowired
    private UserPromoDao userPromoDao;
    @Autowired
    private PromoHelper promoHelper;

    @Test
    public void positive_to_date() {
        Instant toDate = Instant.parse("2020-10-21T12:34:56");
        DateUtils.freezeTime(toDate.minus(1000));
        UserPromoEntity userPromo = userPromoDao.createOrUpdate(getUserPromoData().toDate(Option.of(toDate)).build());

        MailContext context = MailContext.builder().userPromoId(Option.of(userPromo.getId()))
                .language(Option.of(Language.RUSSIAN)).build();
        Option<String> promoToDate = userPromoKeyProvider.getKeyData("user_promo_to_date", context);
        Assert.assertTrue(promoToDate.isPresent());
        Assert.equals("21 октября 2020 года", promoToDate.get());
    }

    @Test
    public void differentTimeZonesWithUser() {
        PassportUid uid =  PassportUid.MAX_VALUE;
        blackbox2MockConfig.mockUserInfo(uid, blackbox2MockConfig.getBlackboxResponse("login", "firstName",
                Option.empty(), Option.empty(), Option.of("ru"), Option.of("Asia/Tokyo"), Option.empty())); // UTC+9
        Instant toDate = Instant.parse("2021-04-06T19:00:00+0300");// 2021-04-07T01:00:00+0900
        DateUtils.freezeTime(toDate.minus(1000));
        UserPromoEntity userPromo = userPromoDao.createOrUpdate(getUserPromoData().toDate(Option.of(toDate)).build());

        MailContext context = MailContext.builder()
                .userPromoId(Option.of(userPromo.getId()))
                .to(uid)
                .language(Option.of(Language.RUSSIAN)).build();
        Option<String> promoToDate = userPromoKeyProvider.getKeyData("user_promo_to_date", context);
        Assert.assertTrue(promoToDate.isPresent());
        Assert.equals("7 апреля 2021 года", promoToDate.get());
    }

    @Test
    public void differentTimeZonesWithUser_Midnight() {
        PassportUid uid =  PassportUid.MAX_VALUE;
        blackbox2MockConfig.mockUserInfo(uid, blackbox2MockConfig.getBlackboxResponse("login", "firstName",
                Option.empty(), Option.empty(), Option.of("ru"), Option.of("Asia/Tokyo"), Option.empty())); // UTC+9
        Instant toDate = Instant.parse("2021-04-06T18:00:00+0300");// 2021-04-07T00:00:00+0900
        DateUtils.freezeTime(toDate.minus(1000));
        UserPromoEntity userPromo = userPromoDao.createOrUpdate(getUserPromoData().toDate(Option.of(toDate)).build());

        MailContext context = MailContext.builder()
                .userPromoId(Option.of(userPromo.getId()))
                .to(uid)
                .language(Option.of(Language.RUSSIAN)).build();
        Option<String> promoToDate = userPromoKeyProvider.getKeyData("user_promo_to_date", context);
        Assert.assertTrue(promoToDate.isPresent());
        Assert.equals("6 апреля 2021 года", promoToDate.get());
    }
    @Test
    public void noLanguage() {
        Instant toDate = Instant.parse("2020-10-21T12:34:56");
        DateUtils.freezeTime(toDate.minus(1000));
        UserPromoEntity userPromo = userPromoDao.createOrUpdate(getUserPromoData().toDate(Option.of(toDate)).build());

        MailContext context = MailContext.builder().userPromoId(Option.of(userPromo.getId())).build();
        Option<String> promoToDate = userPromoKeyProvider.getKeyData("user_promo_to_date", context);
        Assert.assertTrue(promoToDate.isPresent());
        Assert.equals("21.10.2020", promoToDate.get());
    }

    @Test
    public void no_end_date() {
        UserPromoEntity userPromo = userPromoDao.createOrUpdate(getUserPromoData().toDate(Option.empty()).build());

        MailContext context = MailContext.builder().userPromoId(Option.of(userPromo.getId()))
                .language(Option.of(Language.RUSSIAN)).build();
        Option<String> promoToDate = userPromoKeyProvider.getKeyData("user_promo_to_date", context);
        Assert.assertTrue(promoToDate.isPresent());
        Assert.equals("", promoToDate.get());
    }

    @Test
    public void noKey() {
        MailContext context = MailContext.builder().build();
        Option<String> promoName = userPromoKeyProvider.getKeyData(UUID.randomUUID().toString(), context);
        Assert.assertFalse(promoName.isPresent());
    }

    @Test
    public void noPromo() {
        MailContext context = MailContext.builder().build();
        Option<String> promoName = userPromoKeyProvider.getKeyData("user_promo_to_date", context);
        Assert.assertFalse(promoName.isPresent());
    }

    private UserPromoDao.InsertData.InsertDataBuilder getUserPromoData() {
        PromoTemplateEntity promo = promoHelper.createUserPromo();
        return UserPromoDao.InsertData.builder()
                .uid(PassportUid.MAX_VALUE)
                .promoTemplateId(promo.getId())
                .fromDate(DateUtils.pastDate());
    }
}
