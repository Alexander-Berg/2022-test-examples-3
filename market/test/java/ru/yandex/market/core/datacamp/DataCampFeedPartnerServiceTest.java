package ru.yandex.market.core.datacamp;

import java.util.stream.Stream;

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
@DbUnitDataSet(before = "dataCampFeedPartnerServiceTest.before.csv")
class DataCampFeedPartnerServiceTest extends FunctionalTest {

    @Autowired
    private PushSettingsService pushSettingsService;

    private static Stream<Arguments> args() {
        return Stream.of(
                Arguments.of(1L, PartnerChangeSchemaType.PUSH_TO_PULL),
                Arguments.of(2L, PartnerChangeSchemaType.PULL_TO_PUSH),
                Arguments.of(3L, PartnerChangeSchemaType.PULL_TO_PUSH),
                Arguments.of(4L, PartnerChangeSchemaType.PUSH_TO_PULL),
                Arguments.of(5L, PartnerChangeSchemaType.PUSH_TO_PULL)
        );
    }

    @ParameterizedTest
    @MethodSource("args")
    void getPartnerSchemaType(long partnerId, PartnerChangeSchemaType expected) {
        assertEquals(expected, pushSettingsService.getPartnerSchemaType(partnerId));
    }


}
