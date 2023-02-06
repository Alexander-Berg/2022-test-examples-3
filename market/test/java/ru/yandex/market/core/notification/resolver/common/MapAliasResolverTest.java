package ru.yandex.market.core.notification.resolver.common;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.notification.context.impl.ShopNotificationContext;

import static org.hamcrest.Matchers.empty;
import static org.junit.Assert.assertThat;

/**
 * Тесты для {@link MapAliasResolver}.
 *
 * @author Vadim Lyalin
 */
public class MapAliasResolverTest extends FunctionalTest {
    /**
     * Список разрешенных доменов адресов отправителей (from) для аккаунта market-b2b рассылятора.
     * Задается в настройках рассылятора. Редактируется через поддержку рассылятора, через тикет в очереди SENDERSUPPORT
     */
    private static final Set<String> SENDER_ALLOWED_DOMAINS = Set.of("support.yandex.ru", "market.yandex.ru",
            "delivery.yandex.ru", "beru.ru", "yandex-team.ru", "ntcn.ru");

    /**
     * Список устаревших доменов. Они отсутствуют в настройках рассылятора, но у нас еще не выпилины
     * и письма от них не ходят.
     */
    private static final Set<String> DEPRECATED_DOMAINS = Set.of("bringly.io");

    /**
     * Белый список доменов.
     */
    private static final Set<String> WHITE_LIST_DOMAINS;

    static {
        WHITE_LIST_DOMAINS = new HashSet<>(SENDER_ALLOWED_DOMAINS);
        WHITE_LIST_DOMAINS.addAll(DEPRECATED_DOMAINS);
    }

    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;
    @Autowired
    private MapAliasResolver aliasResolver;

    /**
     * Проверяет, что домены адресов from разрешены к отправке.
     */
    @Test
    @DbUnitDataSet(before = "csv/MapAliasResolverTest.testFromDomains.before.csv")
    void testFromDomains() {
        List<String> aliasList = getAliasList();

        assertThat(aliasList, Matchers.not(empty()));

        ShopNotificationContext context = new ShopNotificationContext(1);
        aliasList.stream()
                .flatMap(alias -> aliasResolver.resolveAddresses(alias, context).stream())
                .map(email -> {
                    try {
                        return new InternetAddress(email);
                    } catch (AddressException e) {
                        throw new IllegalArgumentException();
                    }
                })
                .map(InternetAddress::getAddress)
                .map(MapAliasResolverTest::getEmailDomain)
                .filter(domain -> !WHITE_LIST_DOMAINS.contains(domain))
                .findAny()
                .ifPresent(email -> {
                    throw new IllegalArgumentException("email с невалидным доменом: " + email);
                });
    }

    private List<String> getAliasList() {
        return jdbcTemplate.query(
                "select distinct alias from shops_web.nn_address where place_code = 'from'",
                (rs, rowNum) -> rs.getString("alias"));
    }

    private static String getEmailDomain(String email) {
        return email.substring(email.indexOf("@") + 1);
    }
}
