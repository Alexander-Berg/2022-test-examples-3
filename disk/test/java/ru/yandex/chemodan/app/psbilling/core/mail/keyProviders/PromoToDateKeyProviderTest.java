package ru.yandex.chemodan.app.psbilling.core.mail.keyProviders;

import java.util.UUID;

import org.joda.time.Instant;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.app.psbilling.core.AbstractPsBillingCoreTest;
import ru.yandex.chemodan.app.psbilling.core.entities.promos.PromoTemplateEntity;
import ru.yandex.chemodan.app.psbilling.core.mail.MailContext;
import ru.yandex.chemodan.app.psbilling.core.mail.keydataproviders.PromoFieldsKeyProvider;
import ru.yandex.chemodan.app.psbilling.core.utils.DateUtils;
import ru.yandex.chemodan.app.psbilling.core.utils.PromoHelper;
import ru.yandex.inside.passport.PassportUid;
import ru.yandex.inside.utils.Language;
import ru.yandex.misc.test.Assert;

public class PromoToDateKeyProviderTest extends AbstractPsBillingCoreTest {
    @Autowired
    private PromoHelper promoHelper;
    @Autowired
    private PromoFieldsKeyProvider promoFieldsKeyProvider;

    @Test
    public void positive() {
        Instant toDate = Instant.parse("2020-10-21T12:34:56");
        DateUtils.freezeTime(toDate.minus(1000));
        PromoTemplateEntity promo = promoHelper.createGlobalPromo(b -> b.toDate(Option.of(toDate)));

        MailContext context =
                MailContext.builder().promoId(Option.of(promo.getId())).language(Option.of(Language.RUSSIAN)).build();
        Option<String> promoToDate = promoFieldsKeyProvider.getKeyData("promo_to_date", context);
        Assert.assertTrue(promoToDate.isPresent());
        Assert.equals("21 октября 2020 года", promoToDate.get());
    }

    @Test
    public void positiveMidnight() {
        Instant toDate = Instant.parse("2020-10-22T00:00:00");
        DateUtils.freezeTime(toDate.minus(1000));
        PromoTemplateEntity promo = promoHelper.createGlobalPromo(b -> b.toDate(Option.of(toDate)));

        MailContext context =
                MailContext.builder().promoId(Option.of(promo.getId())).language(Option.of(Language.RUSSIAN)).build();
        Option<String> promoToDate = promoFieldsKeyProvider.getKeyData("promo_to_date", context);
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
        PromoTemplateEntity promo = promoHelper.createGlobalPromo(b -> b.toDate(Option.of(toDate)));

        MailContext context = MailContext.builder()
                .promoId(Option.of(promo.getId()))
                .to(uid)
                .language(Option.of(Language.RUSSIAN)).build();
        Option<String> promoToDate = promoFieldsKeyProvider.getKeyData("promo_to_date", context);
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
        PromoTemplateEntity promo = promoHelper.createGlobalPromo(b -> b.toDate(Option.of(toDate)));

        MailContext context = MailContext.builder()
                .promoId(Option.of(promo.getId()))
                .to(uid)
                .language(Option.of(Language.RUSSIAN)).build();
        Option<String> promoToDate = promoFieldsKeyProvider.getKeyData("promo_to_date", context);
        Assert.assertTrue(promoToDate.isPresent());
        Assert.equals("6 апреля 2021 года", promoToDate.get());
    }

    @Test
    public void positive_no_language() {
        Instant toDate = Instant.parse("2020-10-21T12:34:56");
        DateUtils.freezeTime(toDate.minus(1000));
        PromoTemplateEntity promo = promoHelper.createGlobalPromo(b -> b.toDate(Option.of(toDate)));

        MailContext context = MailContext.builder().promoId(Option.of(promo.getId())).build();
        Option<String> promoToDate = promoFieldsKeyProvider.getKeyData("promo_to_date", context);
        Assert.assertTrue(promoToDate.isPresent());
        Assert.equals("21.10.2020", promoToDate.get());
    }

    @Test
    public void noToDate() {
        PromoTemplateEntity promo = promoHelper.createGlobalPromo(b -> b.toDate(Option.empty()));

        MailContext context = MailContext.builder().promoId(Option.of(promo.getId())).build();
        Option<String> promoToDate = promoFieldsKeyProvider.getKeyData("promo_to_date", context);
        Assert.assertTrue(promoToDate.isPresent());
        Assert.equals("", promoToDate.get());
    }

    @Test
    public void noKey() {
        MailContext context = MailContext.builder().build();
        Option<String> promoToDate = promoFieldsKeyProvider.getKeyData(UUID.randomUUID().toString(), context);
        Assert.assertFalse(promoToDate.isPresent());
    }

    @Test
    public void noPromo() {
        MailContext context = MailContext.builder().build();
        Option<String> promoToDate = promoFieldsKeyProvider.getKeyData("promo_to_date", context);
        Assert.assertFalse(promoToDate.isPresent());
    }
}
