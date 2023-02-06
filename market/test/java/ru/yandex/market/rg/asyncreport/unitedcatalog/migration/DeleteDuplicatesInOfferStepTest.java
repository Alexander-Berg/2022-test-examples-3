package ru.yandex.market.rg.asyncreport.unitedcatalog.migration;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.business.migration.BusinessMigration;
import ru.yandex.market.common.test.util.ProtoTestUtil;

/**
 * Тесты для {@link DeleteDuplicatesInOfferStep}
 *
 * @author Kirill Batalin (batalin@yandex-team.ru)
 */
class DeleteDuplicatesInOfferStepTest {

    private DeleteDuplicatesInOfferStep deleteDuplicatesInOfferStep;

    @BeforeEach
    void init() {
        deleteDuplicatesInOfferStep = new DeleteDuplicatesInOfferStep();
    }

    @ParameterizedTest(name = "{0}")
    @DisplayName("Проверка шага удаления дубликатов")
    @MethodSource("testData")
    void test(String name, String beforeProto, String afterProto) {
        BusinessMigration.MergeOffersRequestItem before = ProtoTestUtil.getProtoMessageByJson(
                BusinessMigration.MergeOffersRequestItem.class,
                "proto/" + beforeProto,
                getClass()
        );
        BusinessMigration.MergeOffersRequestItem after = ProtoTestUtil.getProtoMessageByJson(
                BusinessMigration.MergeOffersRequestItem.class,
                "proto/" + afterProto,
                getClass()
        );

        BusinessMigration.MergeOffersRequestItem.Builder toUpdate = before.toBuilder();
        deleteDuplicatesInOfferStep.accept(List.of(toUpdate));

        ProtoTestUtil.assertThat(toUpdate.build())
                .isEqualTo(after);
    }

    private static Stream<Arguments> testData() {
        return Stream.of(
                Arguments.of(
                        "Пустой список офферов",
                        "empty.before.json",
                        "empty.before.json"
                ),
                Arguments.of(
                        "Оффер без дублей",
                        "withoutDuplicates.before.json",
                        "withoutDuplicates.before.json"
                ),
                Arguments.of(
                        "Дубликаты сложных структур убираются",
                        "withDuplicatesOfMessage.before.json",
                        "withDuplicatesOfMessage.after.json"
                ),
                Arguments.of(
                        "Дубликаты простых структур убираются",
                        "withDuplicatesOfPrimitive.before.json",
                        "withDuplicatesOfPrimitive.after.json"
                ),
                Arguments.of(
                        "Дубликаты в значениях мапы убираются",
                        "withDuplicatesInMap.before.json",
                        "withDuplicatesInMap.after.json"
                ),
                Arguments.of(
                        "Вложенные дубликаты внутри репитед-поля",
                        "withNestedDuplicates.before.json",
                        "withNestedDuplicates.after.json"
                )
        );
    }
}
