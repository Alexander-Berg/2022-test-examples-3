package ru.yandex.chemodan.app.psbilling.core.mail.keyProviders;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.app.psbilling.core.AbstractPsBillingCoreTest;
import ru.yandex.chemodan.app.psbilling.core.entities.features.GroupServiceFeature;
import ru.yandex.chemodan.app.psbilling.core.entities.products.FeatureType;
import ru.yandex.chemodan.app.psbilling.core.mail.GroupServiceData;
import ru.yandex.chemodan.app.psbilling.core.mail.MailContext;
import ru.yandex.chemodan.app.psbilling.core.mail.keydataproviders.DiskSpaceKeyDataProvider;
import ru.yandex.inside.utils.Language;
import ru.yandex.misc.test.Assert;

import static ru.yandex.chemodan.app.psbilling.core.products.UserProductFeatureRegistry.MPFS_SPACE_FEATURE_CODE;

public class DiskSpaceKeyDataProviderTest extends AbstractPsBillingCoreTest {

    @Autowired
    private DiskSpaceKeyDataProvider diskSpaceKeyDataProvider;

    @Test
    public void groupSpaces() {
        GroupServiceFeature feature = psBillingGroupsFactory.createGroupServiceFeature(
                psBillingGroupsFactory.createGroup(),
                psBillingProductsFactory.createFeature(FeatureType.ADDITIVE),
                b -> b.code(MPFS_SPACE_FEATURE_CODE)
                        .valueTankerKeyId(Option.of(psBillingTextsFactory.create("test_666_gb").getId()))
        );
        GroupServiceData service = new GroupServiceData(feature.getGroupServiceId().toString());

        Assert.some("666 ГБ", getDiskSpaceKeyData(service, Language.RUSSIAN));
        Assert.some("666 GB", getDiskSpaceKeyData(service, Language.ENGLISH));
    }

    private Option<String> getDiskSpaceKeyData(GroupServiceData service, Language language) {
        MailContext context = MailContext.builder()
                .groupServices(Cf.list(service))
                .language(Option.of(language))
                .build();

        return diskSpaceKeyDataProvider.getKeyData("disk_space_service", context);
    }

    @Before
    public void initialize() {
        super.initialize();
        textsManagerMockConfig.reset();
    }
}
