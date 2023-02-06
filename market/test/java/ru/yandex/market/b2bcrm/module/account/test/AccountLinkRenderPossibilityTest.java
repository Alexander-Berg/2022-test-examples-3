package ru.yandex.market.b2bcrm.module.account.test;

import java.util.Map;

import javax.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.b2bcrm.module.account.Account;
import ru.yandex.market.b2bcrm.module.account.AccountLink;
import ru.yandex.market.b2bcrm.module.account.Shop;
import ru.yandex.market.b2bcrm.module.config.B2bAccountTests;
import ru.yandex.market.jmf.bcp.BcpService;
import ru.yandex.market.jmf.metadata.Fqn;
import ru.yandex.market.jmf.utils.DomainException;

@B2bAccountTests
public class AccountLinkRenderPossibilityTest {
    @Inject
    private BcpService bcpService;

    @Test
    public void shouldBeRendered() {
        createLink(Account.FQN, "http://example.com/${obj.gid}");
        createLink(Shop.FQN, "http://example.com/${obj.gid}");
        createLink(Shop.FQN, "http://example.com/${obj.shopId}");
    }

    @Test()
    public void shouldNotBeRendered() {
        Assertions.assertThrows(DomainException.class, () -> {
            createLink(Fqn.of("unknown"), "http://example.com/${obj.gid}");
            createLink(Account.FQN, "http://example.com/${obj.shopId}");
            createLink(Shop.FQN, "http://example.com/${unknownVariable.shopId}");
        });
    }

    private void createLink(Fqn accountType, String urlTemplate) {
        bcpService.create(AccountLink.FQN, Map.of(
                AccountLink.TITLE, "title",
                AccountLink.ACCOUNT_TYPE, accountType,
                AccountLink.URL_TEMPLATE, urlTemplate
        ));
    }


}
