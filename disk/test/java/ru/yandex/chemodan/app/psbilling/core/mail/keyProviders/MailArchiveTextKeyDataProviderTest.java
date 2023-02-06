package ru.yandex.chemodan.app.psbilling.core.mail.keyProviders;

import java.math.BigDecimal;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.app.psbilling.core.AbstractPsBillingCoreTest;
import ru.yandex.chemodan.app.psbilling.core.dao.texts.TankerKeyDao;
import ru.yandex.chemodan.app.psbilling.core.entities.features.GroupServiceFeature;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.Group;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.GroupService;
import ru.yandex.chemodan.app.psbilling.core.entities.products.FeatureEntity;
import ru.yandex.chemodan.app.psbilling.core.entities.products.FeatureType;
import ru.yandex.chemodan.app.psbilling.core.mail.GroupServiceData;
import ru.yandex.chemodan.app.psbilling.core.mail.MailContext;
import ru.yandex.chemodan.app.psbilling.core.mail.keydataproviders.MailArchiveTextKeyDataProvider;
import ru.yandex.chemodan.app.psbilling.core.products.GroupProduct;
import ru.yandex.chemodan.app.psbilling.core.products.UserProductFeatureRegistry;
import ru.yandex.chemodan.app.psbilling.core.texts.PredefinedTankerKey;
import ru.yandex.inside.utils.Language;
import ru.yandex.misc.test.Assert;

public class MailArchiveTextKeyDataProviderTest extends AbstractPsBillingCoreTest {

    @Autowired
    private MailArchiveTextKeyDataProvider mailArchiveTextKeyDataProvider;
    @Autowired
    private TankerKeyDao tankerKeyDao;

    @Before
    public void initialize() {
        super.initialize();
        textsManagerMockConfig.reset();
    }

    @Test
    public void sensibleText() {
        Group group = psBillingGroupsFactory.createGroup();
        tankerKeyDao.create(TankerKeyDao.InsertData.builder()
                .project(PredefinedTankerKey.MailArchiveText.getTankerProject())
                .keySet(PredefinedTankerKey.MailArchiveText.getTankerKeySet())
                .key(PredefinedTankerKey.MailArchiveText.getTankerKey())
                .build());
        GroupServiceFeature feature = psBillingGroupsFactory.createGroupServiceFeature(
                group,
                psBillingProductsFactory.createFeature(FeatureType.ADDITIVE),
                b -> b.code(UserProductFeatureRegistry.LETTER_ARCHIVE_AVAILABLE_FEATURE_CODE).amount(BigDecimal.valueOf(12435))
        );

        MailContext context = MailContext.builder()
                .groupServices(Cf.list(new GroupServiceData(feature.getGroupServiceId().toString())))
                .language(Option.of(Language.RUSSIAN))
                .build();
        Option<String> mailArchiveText = mailArchiveTextKeyDataProvider.getKeyData("mail_archive_text_service", context);
        Assert.assertTrue(mailArchiveText.isPresent());
        Assert.equals("А у вас — доступ к защищенному архиву рабочих писем сотрудников.", mailArchiveText.get());
    }

    @Test
    public void noText() {
        Group group = psBillingGroupsFactory.createGroup();
        GroupProduct product = psBillingProductsFactory.createGroupProduct();
        GroupService service = psBillingGroupsFactory.createGroupService(group, product);

        MailContext context = MailContext.builder()
                .groupServices(Cf.list(new GroupServiceData(service.getId().toString())))
                .language(Option.of(Language.RUSSIAN))
                .build();
        Option<String> mailArchiveText = mailArchiveTextKeyDataProvider.getKeyData("mail_archive_text_service", context);
        Assert.assertTrue(mailArchiveText.isPresent());
        Assert.assertTrue(mailArchiveText.get().isEmpty());
    }

    @Test
    public void mailArchiveSwitched() {
        GroupProduct product = psBillingProductsFactory.createGroupProduct();
        GroupProduct oldProduct = psBillingProductsFactory.createGroupProduct();
        FeatureEntity feature = psBillingProductsFactory.createFeature(FeatureType.ADDITIVE);
        psBillingProductsFactory.createProductFeature(product.getUserProductId(), feature,
                f -> f.code(UserProductFeatureRegistry.LETTER_ARCHIVE_AVAILABLE_FEATURE_CODE));

        MailContext context = MailContext.builder()
                .userProductId(Option.of(product.getUserProductId()))
                .oldUserProductId(Option.of(oldProduct.getUserProductId()))
                .language(Option.of(Language.RUSSIAN))
                .build();
        Option<String> mailArchiveText = mailArchiveTextKeyDataProvider.getKeyData("is_mail_archive_switched_on", context);
        Assert.assertTrue(mailArchiveText.isPresent());
        Assert.equals(Boolean.TRUE.toString(), mailArchiveText.get());
    }

    @Test
    public void mailArchiveNotSwitched() {
        GroupProduct product = psBillingProductsFactory.createGroupProduct();
        GroupProduct oldProduct = psBillingProductsFactory.createGroupProduct();
        FeatureEntity feature = psBillingProductsFactory.createFeature(FeatureType.ADDITIVE);
        psBillingProductsFactory.createProductFeature(product.getUserProductId(), feature,
                f -> f.code(UserProductFeatureRegistry.LETTER_ARCHIVE_AVAILABLE_FEATURE_CODE));
        psBillingProductsFactory.createProductFeature(oldProduct.getUserProductId(), feature,
                f -> f.code(UserProductFeatureRegistry.LETTER_ARCHIVE_AVAILABLE_FEATURE_CODE));

        MailContext context = MailContext.builder()
                .userProductId(Option.of(product.getUserProductId()))
                .oldUserProductId(Option.of(oldProduct.getUserProductId()))
                .language(Option.of(Language.RUSSIAN))
                .build();
        Option<String> mailArchiveText = mailArchiveTextKeyDataProvider.getKeyData("is_mail_archive_switched_on", context);
        Assert.assertTrue(mailArchiveText.isPresent());
        Assert.equals(Boolean.FALSE.toString(), mailArchiveText.get());
    }
}
