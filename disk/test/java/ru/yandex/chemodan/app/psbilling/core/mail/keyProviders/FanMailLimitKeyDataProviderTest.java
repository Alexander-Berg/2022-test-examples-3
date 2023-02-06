package ru.yandex.chemodan.app.psbilling.core.mail.keyProviders;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.app.psbilling.core.AbstractPsBillingCoreTest;
import ru.yandex.chemodan.app.psbilling.core.entities.features.GroupServiceFeature;
import ru.yandex.chemodan.app.psbilling.core.entities.products.FeatureType;
import ru.yandex.chemodan.app.psbilling.core.entities.texts.TankerKeyEntity;
import ru.yandex.chemodan.app.psbilling.core.entities.texts.TankerTranslationEntity;
import ru.yandex.chemodan.app.psbilling.core.mail.GroupServiceData;
import ru.yandex.chemodan.app.psbilling.core.mail.MailContext;
import ru.yandex.chemodan.app.psbilling.core.mail.keydataproviders.FanMailLimitKeyDataProvider;
import ru.yandex.chemodan.app.psbilling.core.texts.TankerTranslation;
import ru.yandex.inside.utils.Language;
import ru.yandex.misc.test.Assert;

import static ru.yandex.chemodan.app.psbilling.core.products.UserProductFeatureRegistry.FAN_MAIL_LIMIT_FEATURE_CODE;

public class FanMailLimitKeyDataProviderTest extends AbstractPsBillingCoreTest {

    @Autowired
    private FanMailLimitKeyDataProvider fanMailLimitKeyDataProvider;

    @Test
    public void getFanMailLimit() {
        TankerKeyEntity valueTankerKey = psBillingTextsFactory.create("test_fan_mail_limit");
        TankerTranslationEntity ruTranslation = new TankerTranslationEntity(valueTankerKey.getId(), "ru", "1234");
        TankerTranslationEntity enTranslation = new TankerTranslationEntity(valueTankerKey.getId(), "en", "1234");
        TankerTranslation valueTranslation
                = new TankerTranslation(valueTankerKey.getId(), Cf.list(ruTranslation, enTranslation));
        textsManagerMockConfig.mockFindTranslation(valueTranslation);

        GroupServiceFeature feature = psBillingGroupsFactory.createGroupServiceFeature(
                psBillingGroupsFactory.createGroup(),
                psBillingProductsFactory.createFeature(FeatureType.ADDITIVE),
                b -> b.code(FAN_MAIL_LIMIT_FEATURE_CODE)
                        .valueTankerKeyId(Option.of(valueTankerKey.getId()))
        );
        GroupServiceData service = new GroupServiceData(feature.getGroupServiceId().toString());

        Assert.some("1234", getFanMailLimitKeyData(service, Language.RUSSIAN));
        Assert.some("1234", getFanMailLimitKeyData(service, Language.ENGLISH));
    }

    private Option<String> getFanMailLimitKeyData(GroupServiceData service, Language language) {
        MailContext context = MailContext.builder()
                .groupServices(Cf.list(service))
                .language(Option.of(language))
                .build();

        return fanMailLimitKeyDataProvider.getKeyData("fan_mail_limit_service", context);
    }

    @Before
    public void initialize() {
        super.initialize();
        textsManagerMockConfig.reset();
    }
}
