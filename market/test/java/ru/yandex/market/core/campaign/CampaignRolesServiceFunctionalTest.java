package ru.yandex.market.core.campaign;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.contact.InnerRole;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static ru.yandex.market.core.contact.InnerRole.SHOP_ADMIN;
import static ru.yandex.market.core.contact.InnerRole.SHOP_OPERATOR;
import static ru.yandex.market.core.contact.InnerRole.SHOP_TECHNICAL;

/**
 * Функциональные тесты для {@link CampaignRolesService}.
 *
 * @author Vadim Lyalin
 */
@DbUnitDataSet(before = "CampaignRolesServiceFunctionalTest.before.csv")
public class CampaignRolesServiceFunctionalTest extends FunctionalTest {
    @Autowired
    private CampaignRolesService campaignRolesService;

    private static Stream<Arguments> testHasAuthorityInCampaignArgs() {
        return Stream.of(
                Arguments.of(999, 100, SHOP_ADMIN, false),  // несуществующий пользователь
                Arguments.of(1, 999, SHOP_ADMIN, false),    // несуществующая кампания
                Arguments.of(5, 100, SHOP_ADMIN, false),     // роль не выдана

                Arguments.of(1, 100, SHOP_ADMIN, true),     // администратор бизнеса
                Arguments.of(3, 100, SHOP_ADMIN, true),     // суперадмин
                Arguments.of(5, 100, SHOP_TECHNICAL, true)  // роль явно выдана
        );
    }

    private static Stream<Arguments> testHasAuthorityOnHisClientArgs() {
        return Stream.of(
                Arguments.of(5, SHOP_ADMIN, false),    // проверяем роль, которая не выдана
                Arguments.of(7, SHOP_ADMIN, false),    // uid с линком без ролей
                Arguments.of(999, null, false),        // несуществующий контакт

                Arguments.of(5, SHOP_TECHNICAL, true), // роль явно задана
                Arguments.of(5, null, true),           // роль null, у uid'а есть линк с какой-то ролью
                Arguments.of(3, SHOP_ADMIN, true),     // бизнес админ
                Arguments.of(3, SHOP_OPERATOR, true),  // бизнес админ
                Arguments.of(1, SHOP_ADMIN, true)      // бизнес админ
        );
    }

    @ParameterizedTest
    @MethodSource("testHasAuthorityInCampaignArgs")
    void testHasAuthorityInCampaign(long uid, long campaignId, InnerRole innerRole, boolean result) {
        assertEquals(result, campaignRolesService.hasAuthorityInCampaign(uid, campaignId, innerRole));
    }

    @ParameterizedTest
    @MethodSource("testHasAuthorityOnHisClientArgs")
    void testHasAuthorityOnHisClient(long uid, InnerRole role, boolean result) {
        assertEquals(result, campaignRolesService.hasAuthorityWithRole(uid, role));
    }
}
