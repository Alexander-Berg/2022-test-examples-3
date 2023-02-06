package ru.yandex.chemodan.app.psbilling.core.mail.keyProviders;

import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.app.psbilling.core.AbstractPsBillingCoreTest;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.Group;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.GroupService;
import ru.yandex.chemodan.app.psbilling.core.entities.products.UserProductEntity;
import ru.yandex.chemodan.app.psbilling.core.entities.users.UserServiceEntity;
import ru.yandex.chemodan.app.psbilling.core.mail.GroupServiceData;
import ru.yandex.chemodan.app.psbilling.core.mail.MailContext;
import ru.yandex.chemodan.app.psbilling.core.mail.keydataproviders.TariffNameKeyDataProvider;
import ru.yandex.chemodan.app.psbilling.core.products.GroupProduct;
import ru.yandex.inside.passport.PassportUid;
import ru.yandex.inside.utils.Language;
import ru.yandex.misc.test.Assert;

public class TariffNameKeyDataProviderTests extends AbstractPsBillingCoreTest {
    @Autowired
    private TariffNameKeyDataProvider tariffNameKeyDataProvider;

    @Test
    public void positive_user_tariff() {
        UserProductEntity product = psBillingProductsFactory.createUserProduct();
        UserServiceEntity userService = psBillingUsersFactory.createUserService(product);

        MailContext context = MailContext.builder().language(Option.of(Language.RUSSIAN))
                .userServiceId(Option.of(userService.getId().toString())).build();
        Option<String> tariffName = tariffNameKeyDataProvider.getKeyData("tariff_name", context);
        Assert.assertTrue(tariffName.isPresent());
        Assert.equals("Тестовый ключ", tariffName.get());
    }

    @Test
    public void positive_group_tariff() {
        Group group = psBillingGroupsFactory.createGroup();
        GroupProduct product = psBillingProductsFactory.createGroupProduct();
        GroupService service = psBillingGroupsFactory.createGroupService(group, product);

        MailContext context = MailContext.builder().language(Option.of(Language.RUSSIAN))
                .groupServices(Cf.list(GroupServiceData.fromGroupService(service)))
                .build();
        Option<String> tariffName = tariffNameKeyDataProvider.getKeyData("tariff_name", context);
        Assert.assertTrue(tariffName.isPresent());
        Assert.equals("Тестовый ключ", tariffName.get());
    }

    @Test
    public void positive_user_product() {
        UserProductEntity product = psBillingProductsFactory.createUserProduct();

        MailContext context = MailContext.builder().language(Option.of(Language.RUSSIAN))
                .userProductId(Option.of(product.getId())).build();
        Option<String> tariffName = tariffNameKeyDataProvider.getKeyData("tariff_name", context);
        Assert.assertTrue(tariffName.isPresent());
        Assert.equals("Тестовый ключ", tariffName.get());
    }

    @Test
    public void noKey() {
        PassportUid uid = PassportUid.MAX_VALUE;
        MailContext context = MailContext.builder().to(uid).build();

        Option<String> userName = tariffNameKeyDataProvider.getKeyData(UUID.randomUUID().toString(), context);
        Assert.isFalse(userName.isPresent());
    }

    @Before
    public void initialize() {
        super.initialize();
        textsManagerMockConfig.reset();
    }
}
