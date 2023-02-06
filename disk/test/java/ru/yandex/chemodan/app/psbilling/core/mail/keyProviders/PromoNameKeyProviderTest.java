package ru.yandex.chemodan.app.psbilling.core.mail.keyProviders;

import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.app.psbilling.core.AbstractPsBillingCoreTest;
import ru.yandex.chemodan.app.psbilling.core.dao.promos.PromoTemplateDao;
import ru.yandex.chemodan.app.psbilling.core.dao.promos.UserPromoDao;
import ru.yandex.chemodan.app.psbilling.core.dao.promos.group.GroupPromoDao;
import ru.yandex.chemodan.app.psbilling.core.dao.texts.TankerKeyDao;
import ru.yandex.chemodan.app.psbilling.core.entities.promos.PromoTemplateEntity;
import ru.yandex.chemodan.app.psbilling.core.entities.texts.TankerTranslationEntity;
import ru.yandex.chemodan.app.psbilling.core.mail.MailContext;
import ru.yandex.chemodan.app.psbilling.core.mail.keydataproviders.PromoFieldsKeyProvider;
import ru.yandex.chemodan.app.psbilling.core.texts.TankerTranslation;
import ru.yandex.chemodan.app.psbilling.core.utils.PromoHelper;
import ru.yandex.inside.utils.Language;
import ru.yandex.misc.test.Assert;

public class PromoNameKeyProviderTest extends AbstractPsBillingCoreTest {
    @Autowired
    private PromoFieldsKeyProvider promoFieldsKeyProvider;
    @Autowired
    private PromoTemplateDao promoTemplateDao;
    @Autowired
    private TankerKeyDao tankerKeyDao;
    @Autowired
    private UserPromoDao userPromoDao;

    @Autowired
    private GroupPromoDao groupPromoDao;

    private PromoHelper promoHelper;
    private UUID promoNameTankerKey;

    @Test
    public void positive() {
        TankerTranslation translation = new TankerTranslation(promoNameTankerKey, Cf.list(
                new TankerTranslationEntity(promoNameTankerKey, "ru", "русский")));
        textsManagerMockConfig.mockFindTranslation(translation);

        PromoTemplateEntity promo =
                promoHelper.createGlobalPromo(b -> b.promoNameTankerKey(Option.of(promoNameTankerKey)));

        MailContext context = MailContext.builder().promoId(Option.of(promo.getId()))
                .language(Option.of(Language.RUSSIAN)).build();
        Option<String> promoName = promoFieldsKeyProvider.getKeyData("promo_name", context);
        Assert.assertTrue(promoName.isPresent());
        Assert.equals("русский", promoName.get());
    }

    @Test
    public void noLanguage() {
        TankerTranslation translation = new TankerTranslation(promoNameTankerKey, Cf.list(
                new TankerTranslationEntity(promoNameTankerKey, "ru", "русский")));
        textsManagerMockConfig.mockFindTranslation(translation);

        PromoTemplateEntity promo =
                promoHelper.createGlobalPromo(b -> b.promoNameTankerKey(Option.of(promoNameTankerKey)));

        MailContext context = MailContext.builder().promoId(Option.of(promo.getId()))
                .language(Option.of(Language.ENGLISH)).build();
        Option<String> promoName = promoFieldsKeyProvider.getKeyData("promoName", context);
        Assert.assertFalse(promoName.isPresent());
    }

    @Test
    public void noKey() {
        MailContext context = MailContext.builder().build();
        Option<String> promoName = promoFieldsKeyProvider.getKeyData(UUID.randomUUID().toString(), context);
        Assert.assertFalse(promoName.isPresent());
    }

    @Test
    public void noPromo() {
        MailContext context = MailContext.builder().build();
        Option<String> promoName = promoFieldsKeyProvider.getKeyData("promoName", context);
        Assert.assertFalse(promoName.isPresent());
    }

    @Test
    public void noPromoName() {
        PromoTemplateEntity promo = promoHelper.createGlobalPromo();
        MailContext context = MailContext.builder().promoId(Option.of(promo.getId())).build();
        Option<String> promoName = promoFieldsKeyProvider.getKeyData("promoName", context);
        Assert.assertFalse(promoName.isPresent());
    }

    @Before
    public void setup() {
        promoHelper = new PromoHelper(promoTemplateDao, userPromoDao, groupPromoDao);
        promoNameTankerKey = tankerKeyDao.create(TankerKeyDao.InsertData.builder()
                .key("promoKey").keySet("keySet").project("project").build()).getId();
    }
}
