package ru.yandex.market.b2bcrm.module.account.test;

import java.util.List;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.context.annotation.Description;

import ru.yandex.market.b2bcrm.module.account.Account;
import ru.yandex.market.b2bcrm.module.account.Shop;
import ru.yandex.market.b2bcrm.module.config.B2bAccountTests;
import ru.yandex.market.crm.util.Randoms;
import ru.yandex.market.jmf.bcp.BcpService;
import ru.yandex.market.jmf.metadata.Fqn;
import ru.yandex.market.jmf.metadata.metaclass.Metaclass;
import ru.yandex.market.jmf.search.SearchService;
import ru.yandex.market.jmf.utils.Maps;

@B2bAccountTests
public class SearchTest {
    @Inject
    SearchService searchService;
    @Inject
    BcpService bcpService;

    private static Stream<Arguments> data() {
        return Stream.of(
                Arguments.of("account$supplier", "123456"),
                Arguments.of("account$supplier", "test_url.com"),
                Arguments.of("account$shop", "2007199644"),
                Arguments.of("account$shop", "autotestshop.xyz")
        );
    }

    @BeforeEach
    public void setUp() {
        createEntity(Fqn.of("account$supplier"), 123456, "test_url.com");
        createEntity(Fqn.of("account$supplier"), 12345567, "autotestsupplier.com");
        createEntity(Fqn.of("account$shop"), 2007199644, "autotestshop.xyz");
        createEntity(Fqn.of("account$shop"), 2028199777, "justtest.xyz");
    }

    @ParameterizedTest(name = "{index} Entity: {0}, Search string: {1})")
    @MethodSource(value = "data")
    @Description("""
                Поиск supplier/shop по url/clientId
                https://testpalm.yandex-team.ru/testcase/ocrm-1552
                https://testpalm.yandex-team.ru/testcase/ocrm-1553
                https://testpalm.yandex-team.ru/testcase/ocrm-1554
                https://testpalm.yandex-team.ru/testcase/ocrm-1555
            """)
    public void findAccount(String fqn, String searchString) {
        List<Metaclass> result = searchService.search(searchString);

        Assertions.assertEquals(1, result.size(), "Должны найти 1 объект");
        Assertions.assertEquals(Fqn.of("account"), result.get(0).getFqn());
    }

    private Account createEntity(Fqn fqn, Integer clientId, String url) {
        return bcpService.create(fqn, Maps.of(
                Account.TITLE, Randoms.string(),
                Shop.CLIENT_ID, clientId,
                Shop.DOMAIN, Maps.of("href", url, "value", url)
        ));
    }
}
