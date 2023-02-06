package ru.yandex.market.core.post;

import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.post.model.PostRegionEntity;

/**
 * Тесты для {@link RusPostOfficeService}.
 *
 * @author Kirill Batalin (batalin@yandex-team.ru)
 */
class RusPostOfficeServiceTest extends FunctionalTest {

    @Autowired
    private RusPostOfficeService rusPostOfficeService;

    @ParameterizedTest
    @MethodSource("testGetPostOfficeRegionData")
    @DbUnitDataSet(before = "RusPostOfficeService.testGetPostOfficeRegion.before.csv")
    void testGetPostOfficeRegion(final String name, final long postCode, final String expected) {
        final Optional<PostRegionEntity> postOfficeRegion = rusPostOfficeService.getPostOfficeRegion(postCode);

        if (expected != null) {
            Assertions.assertTrue(postOfficeRegion.isPresent());
            Assertions.assertEquals(expected, postOfficeRegion.get().getPostRegionId());
        } else {
            Assertions.assertTrue(postOfficeRegion.isEmpty());
        }
    }

    static Stream<Arguments> testGetPostOfficeRegionData() {
        return Stream.of(
                Arguments.of(
                        "ОПС в Москве. Должны выбрать Москву",
                        105077L,
                        "773"
                ),
                Arguments.of(
                        "ОПС в городе Московской области. Должны выбрать Московскую область",
                        105076L,
                        "502"
                ),
                Arguments.of(
                        "ОПС в Московской области. Должны выбрать Московскую область",
                        105075L,
                        "502"
                ),
                Arguments.of(
                        "ОПС в ЦФО. Нет региона",
                        105074L,
                        null
                ),
                Arguments.of(
                        "ОПС в районе Тюменской области. Должны выбрать Тюменскую область",
                        105073L,
                        "722"
                )
        );
    }

}
