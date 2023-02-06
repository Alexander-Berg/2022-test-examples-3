package ru.yandex.market.partner.security.checker;

import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.campaign.model.PartnerId;
import ru.yandex.market.core.testing.ShopProgram;
import ru.yandex.market.partner.mvc.MockPartnerRequest;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.security.model.Authority;

import static ru.yandex.market.core.campaign.model.CampaignType.SHOP;

@DbUnitDataSet(before = "ModerationInProgressTest.before.csv")
public class ModerationInProgressCheckerTest extends FunctionalTest {

    private static final int USER_ID = 123;

    @Autowired
    private ModerationInProgressChecker tested;

    @ParameterizedTest(name = "{0}")
    @MethodSource("getTestData")
    void testCheckTyped(String testName,
                        long partnerId,
                        ShopProgram shopProgram,
                        boolean expected) {
        MockPartnerRequest data = new MockPartnerRequest(USER_ID, USER_ID, PartnerId.partnerId(partnerId, SHOP));
        Authority auth = new Authority("MODERATION_IN_PROGRESS", shopProgram.name());

        Assertions.assertEquals(expected, tested.checkTyped(data, auth));
    }

    private static Stream<Arguments> getTestData() {
        return Stream.of(
                Arguments.of("Сампроверка в прогрессе", 1L, ShopProgram.SELF_CHECK, true),
                Arguments.of("Фид загружается для самопроверки", 2L, ShopProgram.SELF_CHECK, false),
                Arguments.of("Самопроверка уже пройдена", 3L, ShopProgram.SELF_CHECK, false),
                Arguments.of("Самопроверка не пройдена. Модерация в прогрессе", 4L, ShopProgram.SELF_CHECK, false),
                Arguments.of("Модерация в прогрессе", 4L, ShopProgram.CPA, true),
                Arguments.of("Магазин не найден", 5L, ShopProgram.SELF_CHECK, false)
        );
    }


}
