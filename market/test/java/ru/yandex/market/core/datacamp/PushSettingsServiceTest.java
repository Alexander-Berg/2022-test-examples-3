package ru.yandex.market.core.datacamp;

import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.mbi.datacamp.stroller.model.PartnerChangeSchemaType;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Тесты для {@link PushSettingsService}.
 */
@DbUnitDataSet(before = "pushSettingsServiceTest.before.csv")
class PushSettingsServiceTest extends FunctionalTest {

    @Autowired
    private PushSettingsService pushSettingsService;

    private static Stream<Arguments> argsUsePush() {
        return Stream.of(
                Arguments.of(1, false),//NO
                Arguments.of(2, true),//PULL_TO_PUSH
                Arguments.of(3, false),//PUSH_TO_PULL
                Arguments.of(4, true),//REAL
                Arguments.of(5, false)
        );
    }

    private static Stream<Arguments> argsPartnerSchemaType() {
        return Stream.of(
                Arguments.of(1, PartnerChangeSchemaType.PUSH_TO_PULL),//NO
                Arguments.of(2, PartnerChangeSchemaType.PULL_TO_PUSH),//PULL_TO_PUSH
                Arguments.of(3, PartnerChangeSchemaType.PUSH_TO_PULL),//PUSH_TO_PULL
                Arguments.of(4, PartnerChangeSchemaType.PULL_TO_PUSH),//REAL
                Arguments.of(5, PartnerChangeSchemaType.PUSH_TO_PULL)
        );
    }

    @ParameterizedTest
    @MethodSource("argsUsePush")
    void usePushScheme(long partnerId, boolean expected) {
        assertEquals(expected, pushSettingsService.usePushScheme(partnerId));
    }

    @ParameterizedTest
    @MethodSource("argsPartnerSchemaType")
    void getPartnerSchemaType(long partnerId, PartnerChangeSchemaType expected) {
        assertEquals(expected, pushSettingsService.getPartnerSchemaType(partnerId));
    }

    @Test
    @DbUnitDataSet(before = "pushSettingsServiceGetPushPartnersTest.before.csv")
    void checkGetPushPartnerList() {
        Set<Long> pushPartnerList = pushSettingsService.getPushPartnerList(Set.of(91L, 92L, 93L, 94L, 97L));
        assertEquals(3, pushPartnerList.size());
        assertEquals(Set.of(92L, 94L, 97L), pushPartnerList);
    }
}
