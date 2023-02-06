package ru.yandex.market.partner.security.checker;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.contact.InnerRole;
import ru.yandex.market.core.security.DefaultBusinessUidable;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.security.model.Authority;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static ru.yandex.market.core.contact.InnerRole.SHOP_ADMIN;
import static ru.yandex.market.core.contact.InnerRole.SHOP_OPERATOR;

/**
 * Тестирует {@link CampaignContactInBusinessChecker}.
 *
 * @author Vadim Lyalin
 */
@DbUnitDataSet(before = "businessOwnerRoleCheckerTest.before.csv")
public class CampaignContactInBusinessCheckerTest extends FunctionalTest {
    @Autowired
    private CampaignContactInBusinessChecker campaignContactInBusinessChecker;

    private static Stream<Arguments> testCheckArgs() {
        return Stream.of(
                // Проверки с любой ролью
                Arguments.of(999, 999, null, false),  // несуществующие бизнес и уид
                Arguments.of(31, 4, null, false),  // у контакта нет линков в кампании бизнеса
                Arguments.of(32, 10, null, true),  // уид имеет контакт линк в кампании бизнеса
                Arguments.of(31, 50, null, true),  // шоп админ
                Arguments.of(32, 50, null, false),  // ГП в другом клиенте
                // Проверки SHOP_ADMIN
                Arguments.of(32, 10, SHOP_ADMIN, false),  // уид имеет контакт линк в кампании бизнеса, с другой ролью
                Arguments.of(32, 10, SHOP_OPERATOR, true),
                Arguments.of(31, 50, SHOP_ADMIN, true)  // шоп админ
        );
    }

    @ParameterizedTest
    @MethodSource("testCheckArgs")
    void testCheck(long businessId, long uid, InnerRole role, boolean result) {
        assertEquals(result, campaignContactInBusinessChecker.checkTyped(
                new DefaultBusinessUidable(businessId, uid, uid),
                new Authority(null, role == null ? null : role.name())));
    }
}
