package ru.yandex.chemodan.app.psbilling.core.mail.keydataproviders;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.app.psbilling.core.AbstractPsBillingCoreTest;
import ru.yandex.chemodan.app.psbilling.core.PsBillingProductsFactory;
import ru.yandex.chemodan.app.psbilling.core.PsBillingUsersFactory;
import ru.yandex.chemodan.app.psbilling.core.entities.InappStore;
import ru.yandex.chemodan.app.psbilling.core.mail.MailContext;
import ru.yandex.misc.test.Assert;


public class InappStoreNameKeyDataProviderTest extends AbstractPsBillingCoreTest {

    @Autowired
    InappStoreNameKeyDataProvider provider;
    @Autowired
    PsBillingUsersFactory usersFactory;
    @Autowired
    PsBillingProductsFactory productsFactory;

    @Test
    public void googlePlayTest() {
        testImpl(InappStore.GOOGLE_PLAY, InappStore.GOOGLE_PLAY.getHumanReadable());
    }

    @Test
    public void appStorePlayTest() {
        testImpl(InappStore.APPLE_APPSTORE, InappStore.APPLE_APPSTORE.getHumanReadable());
    }

    private void testImpl(InappStore inappStore, String expectedStore) {
        testImpl(inappStore, Option.of(expectedStore));
    }
    private void testImpl(InappStore inappStore, Option<String> expectedStore) {

        MailContext context = MailContext.builder().inappStore(Option.of(inappStore)).build();
        Option<String> storeName = provider.getKeyData("inapp_store_type", context);
        Assert.equals(expectedStore, storeName);

    }
}
