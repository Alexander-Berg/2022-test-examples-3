package ru.yandex.market.logistics.management.domain.entity.validation;

import java.util.stream.Stream;

import javax.annotation.Nonnull;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.logistics.management.AbstractTest;
import ru.yandex.market.logistics.management.domain.entity.Partner;
import ru.yandex.market.logistics.management.domain.entity.PartnerRelation;
import ru.yandex.market.logistics.management.domain.entity.PartnerSubtype;
import ru.yandex.market.logistics.management.domain.entity.type.PartnerType;

import static ru.yandex.market.logistics.management.util.TestPartnerSubtypes.SUB_TYPE_1;
import static ru.yandex.market.logistics.management.util.TestPartnerSubtypes.SUB_TYPE_103;
import static ru.yandex.market.logistics.management.util.TestPartnerSubtypes.SUB_TYPE_2;
import static ru.yandex.market.logistics.management.util.TestPartnerSubtypes.SUB_TYPE_3;
import static ru.yandex.market.logistics.management.util.TestPartnerSubtypes.SUB_TYPE_34;
import static ru.yandex.market.logistics.management.util.TestPartnerSubtypes.SUB_TYPE_4;
import static ru.yandex.market.logistics.management.util.TestPartnerSubtypes.SUB_TYPE_5;
import static ru.yandex.market.logistics.management.util.TestPartnerSubtypes.SUB_TYPE_6;
import static ru.yandex.market.logistics.management.util.TestPartnerSubtypes.SUB_TYPE_67;
import static ru.yandex.market.logistics.management.util.TestPartnerSubtypes.SUB_TYPE_68;
import static ru.yandex.market.logistics.management.util.TestPartnerSubtypes.SUB_TYPE_69;
import static ru.yandex.market.logistics.management.util.TestPartnerSubtypes.SUB_TYPE_7;
import static ru.yandex.market.logistics.management.util.TestPartnerSubtypes.SUB_TYPE_70;
import static ru.yandex.market.logistics.management.util.TestPartnerSubtypes.SUB_TYPE_8;

class EnabledPartnerRelationHasAcceptedToPartnerSubtypeValidatorTest extends AbstractTest {

    EnabledPartnerRelationHasAcceptedToPartnerSubtypeValidator
        validator = new EnabledPartnerRelationHasAcceptedToPartnerSubtypeValidator();

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource
    void testHandle(
        @SuppressWarnings("unused") String name,
        PartnerRelation partnerRelation,
        boolean expected
    ) {
        // when:
        boolean actual = validator.isValid(partnerRelation, null);

        // then:
        softly.assertThat(actual).isEqualTo(expected);
    }

    @SuppressWarnings({"unused", "MethodLength"})
    @Nonnull
    private static Stream<Arguments> testHandle() {
        return Stream.of(
            Arguments.of(
                "Cвязь без явного указания состояния",
                partnerRelation(null, null),
                true
            ),

            /* disabled partner relations */
            Arguments.of(
                "Выключенная связь с партнёром назначения без подтипа",
                partnerRelation(false, null),
                true
            ),
            Arguments.of(
                "Выключенная связь с партнёром назначения с подтипом 1 (Партнерская доставка (контрактная))",
                partnerRelation(false, SUB_TYPE_1),
                true
            ),
            Arguments.of(
                "Выключенная связь с партнёром назначения с подтипом 2 (Маркет Курьер)",
                partnerRelation(false, SUB_TYPE_2),
                true
            ),
            Arguments.of(
                "Выключенная связь с партнёром назначения с подтипом 3 (Маркет свои ПВЗ)",
                partnerRelation(false, SUB_TYPE_3),
                true
            ),
            Arguments.of(
                "Выключенная связь с партнёром назначения с подтипом 4 (Партнерские ПВЗ (ИПэшники))",
                partnerRelation(false, SUB_TYPE_4),
                true
            ),
            Arguments.of(
                "Выключенная связь с партнёром назначения с подтипом 5 (Маркет Локеры)",
                partnerRelation(false, SUB_TYPE_5),
                true
            ),
            Arguments.of(
                "Выключенная связь с партнёром назначения с подтипом 6 (СЦ для МК)",
                partnerRelation(false, SUB_TYPE_6),
                true
            ),
            Arguments.of(
                "Выключенная связь с партнёром назначения с подтипом 7 (Партнерский СЦ)",
                partnerRelation(false, SUB_TYPE_7),
                true
            ),
            Arguments.of(
                "Выключенная связь с партнёром назначения с подтипом 8 (Такси-Лавка)",
                partnerRelation(false, SUB_TYPE_8),
                true
            ),
            Arguments.of(
                "Выключенная связь с партнёром назначения с подтипом 34 (Такси-Экспресс)",
                partnerRelation(false, SUB_TYPE_34),
                true
            ),
            Arguments.of(
                "Выключенная связь с партнёром назначения с подтипом 67 (Такси-Авиа)",
                partnerRelation(false, SUB_TYPE_67),
                true
            ),
            Arguments.of(
                "Выключенная связь с партнёром назначения с подтипом 68 (Дроп-офф)",
                partnerRelation(false, SUB_TYPE_68),
                true
            ),
            Arguments.of(
                "Выключенная связь с партнёром назначения с подтипом 69 (Локеры (sandbox))",
                partnerRelation(false, SUB_TYPE_69),
                true
            ),
            Arguments.of(
                "Выключенная связь с партнёром назначения с подтипом 70 (Даркстор)",
                partnerRelation(false, SUB_TYPE_70),
                true
            ),
            Arguments.of(
                "Выключенная связь с партнёром назначения с подтипом 103 (Go Платформа)",
                partnerRelation(false, SUB_TYPE_103),
                true
            ),

            /* enabled partner relations */
            Arguments.of(
                "Включенная связь с партнёром назначения без подтипа",
                partnerRelation(true, null),
                true
            ),
            Arguments.of(
                "Включенная связь с партнёром назначения с подтипом 1 (Партнерская доставка (контрактная))",
                partnerRelation(true, SUB_TYPE_1),
                true
            ),
            Arguments.of(
                "Включенная связь с партнёром назначения с подтипом 2 (Маркет Курьер)",
                partnerRelation(true, SUB_TYPE_2),
                true
            ),
            Arguments.of(
                "Включенная связь с партнёром назначения с подтипом 3 (Маркет свои ПВЗ)",
                partnerRelation(true, SUB_TYPE_3),
                false
            ),
            Arguments.of(
                "Включенная связь с партнёром назначения с подтипом 4 (Партнерские ПВЗ (ИПэшники))",
                partnerRelation(true, SUB_TYPE_4),
                false
            ),
            Arguments.of(
                "Включенная связь с партнёром назначения с подтипом 5 (Маркет Локеры)",
                partnerRelation(true, SUB_TYPE_5),
                false
            ),
            Arguments.of(
                "Включенная связь с партнёром назначения с подтипом 6 (СЦ для МК)",
                partnerRelation(true, SUB_TYPE_6),
                true
            ),
            Arguments.of(
                "Включенная связь с партнёром назначения с подтипом 7 (Партнерский СЦ)",
                partnerRelation(true, SUB_TYPE_7),
                true
            ),
            Arguments.of(
                "Включенная связь с партнёром назначения с подтипом 8 (Такси-Лавка)",
                partnerRelation(true, SUB_TYPE_8),
                true
            ),
            Arguments.of(
                "Включенная связь с партнёром назначения с подтипом 34 (Такси-Экспресс)",
                partnerRelation(true, SUB_TYPE_34),
                true
            ),
            Arguments.of(
                "Включенная связь с партнёром назначения с подтипом 67 (Такси-Авиа)",
                partnerRelation(true, SUB_TYPE_67),
                true
            ),
            Arguments.of(
                "Включенная связь с партнёром назначения с подтипом 68 (Дроп-офф)",
                partnerRelation(true, SUB_TYPE_68),
                true
            ),
            Arguments.of(
                "Включенная связь с партнёром назначения с подтипом 69 (Локеры (sandbox))",
                partnerRelation(true, SUB_TYPE_69),
                true
            ),
            Arguments.of(
                "Включенная связь с партнёром назначения с подтипом 70 (Даркстор)",
                partnerRelation(true, SUB_TYPE_70),
                true
            ),
            Arguments.of(
                "Включенная связь с партнёром назначения с подтипом 103 (Go Платформа)",
                partnerRelation(true, SUB_TYPE_103),
                true
            ),
            Arguments.of(
                "Включенная связь от DROPSHIP с партнёром назначения с подтипом 3 (Маркет свои ПВЗ)",
                partnerRelation(true, SUB_TYPE_3)
                    .setFromPartner(new Partner().setPartnerType(PartnerType.DROPSHIP)),
                true
            ),
            Arguments.of(
                "Включенная связь от DROPSHIP с партнёром назначения с подтипом 4 (Партнерские ПВЗ (ИПэшники))",
                partnerRelation(true, SUB_TYPE_4)
                    .setFromPartner(new Partner().setPartnerType(PartnerType.DROPSHIP)),
                true
            ),
            Arguments.of(
                "Включенная связь от DROPSHIP с партнёром назначения с подтипом 5 (Маркет Локеры)",
                partnerRelation(true, SUB_TYPE_5)
                    .setFromPartner(new Partner().setPartnerType(PartnerType.DROPSHIP)),
                true
            ),
            Arguments.of(
                "Включенная связь от DROPSHIP с партнёром назначения с подтипом 103 (Go Платформа)",
                partnerRelation(true, SUB_TYPE_103)
                    .setFromPartner(new Partner().setPartnerType(PartnerType.DROPSHIP)),
                true
            )
        );
    }

    private static PartnerRelation partnerRelation(Boolean enabled, PartnerSubtype toPartnerSubtype) {
        return new PartnerRelation()
            .setEnabled(enabled)
            .setToPartner(
                new Partner().setPartnerSubtype(toPartnerSubtype)
            );
    }
}
