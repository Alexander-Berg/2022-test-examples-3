package ru.yandex.chemodan.app.psbilling.core.mail.keyProviders;

import java.util.UUID;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.app.psbilling.core.dao.promos.PsBillingPromoCoreTest;
import ru.yandex.chemodan.app.psbilling.core.mail.MailContext;
import ru.yandex.chemodan.app.psbilling.core.mail.keydataproviders.UserPublicNameKeyProvider;
import ru.yandex.chemodan.app.psbilling.core.mocks.Blackbox2MockConfiguration;
import ru.yandex.inside.passport.PassportUid;
import ru.yandex.misc.test.Assert;

public class UserPublicNameKeyProviderTest extends PsBillingPromoCoreTest {
    @Autowired
    private UserPublicNameKeyProvider userPublicNameKeyProvider;

    @Test
    public void positive() {
        PassportUid uid = PassportUid.MAX_VALUE;
        blackbox2MockConfig.mockUserInfo(uid, Blackbox2MockConfiguration.getBlackboxResponse(
                "login", "firstName", Option.of("displayName"), Option.empty(), Option.empty(),
                Option.empty(), Option.empty()));

        MailContext context = MailContext.builder().to(uid).build();

        Option<String> userName = userPublicNameKeyProvider.getKeyData("public_display_name", context);
        Assert.isTrue(userName.isPresent());
        Assert.equals(userName.get(), "displayName");
    }

    @Test
    public void noKey() {
        PassportUid uid = PassportUid.MAX_VALUE;
        MailContext context = MailContext.builder().to(uid).build();

        Option<String> userName = userPublicNameKeyProvider.getKeyData(UUID.randomUUID().toString(), context);
        Assert.isFalse(userName.isPresent());
    }
}
