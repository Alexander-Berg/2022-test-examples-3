package ru.yandex.market.vendors.analytics.core.model.dto.common.socdem;

import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.vendors.analytics.core.model.common.socdem.GenderAgePair;
import ru.yandex.market.vendors.analytics.core.model.common.socdem.SocdemFilter;
import ru.yandex.market.vendors.analytics.core.model.enums.AgeSegment;
import ru.yandex.market.vendors.analytics.core.model.enums.Gender;

import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.toSet;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static ru.yandex.market.vendors.analytics.core.model.enums.AgeSegment.AGE_0_17;
import static ru.yandex.market.vendors.analytics.core.model.enums.AgeSegment.AGE_18_24;
import static ru.yandex.market.vendors.analytics.core.model.enums.AgeSegment.AGE_25_34;
import static ru.yandex.market.vendors.analytics.core.model.enums.AgeSegment.AGE_35_44;
import static ru.yandex.market.vendors.analytics.core.model.enums.AgeSegment.AGE_45_54;
import static ru.yandex.market.vendors.analytics.core.model.enums.AgeSegment.AGE_55_99;
import static ru.yandex.market.vendors.analytics.core.model.enums.Gender.FEMALE;
import static ru.yandex.market.vendors.analytics.core.model.enums.Gender.MALE;

/**
 * Тест на конвертер {@link SocdemConverter}
 *
 * @author antipov93.
 */
public class SocdemConverterTest {

    @ParameterizedTest(name = "[{index}]")
    @MethodSource("socdemConverterArguments")
    public void convert(Set<GenderAgePairDTO> socdemPairs, SocdemFilter resultFilter) {
        assertEquals(SocdemConverter.convert(socdemPairs), resultFilter);
    }

    private static Stream<Arguments> socdemConverterArguments() {
        return Stream.of(
                Arguments.of(
                        Set.of(
                                socdemPairDTO(FEMALE, AGE_0_17),
                                socdemPairDTO(FEMALE, AGE_18_24),
                                socdemPairDTO(FEMALE, AGE_25_34),
                                socdemPairDTO(FEMALE, AGE_35_44),
                                socdemPairDTO(FEMALE, AGE_45_54),
                                socdemPairDTO(FEMALE, AGE_55_99),
                                socdemPairDTO(MALE, AGE_0_17),
                                socdemPairDTO(MALE, AGE_25_34)
                        ),
                        socdemFilter(
                                Set.of(FEMALE),
                                Set.of(AGE_0_17, AGE_25_34),
                                emptySet()
                        )
                ),
                Arguments.of(
                        allSocdems(),
                        socdemFilter(
                                Stream.of(Gender.values()).collect(toSet()),
                                Stream.of(AgeSegment.values()).collect(toSet()),
                                emptySet()
                        )
                ),
                Arguments.of(
                        Set.of(
                                socdemPairDTO(FEMALE, AGE_0_17),
                                socdemPairDTO(MALE, AGE_0_17),
                                socdemPairDTO(FEMALE, AGE_35_44),
                                socdemPairDTO(MALE, AGE_18_24)
                        ),
                        socdemFilter(
                                emptySet(),
                                Set.of(AGE_0_17),
                                Set.of(
                                        socdemPair(FEMALE, AGE_35_44),
                                        socdemPair(MALE, AGE_18_24)
                                )
                        )
                ),
                Arguments.of(
                        emptySet(),
                        socdemFilter(emptySet(), emptySet(), emptySet())
                ),
                Arguments.of(
                        null,
                        socdemFilter(emptySet(), emptySet(), emptySet())
                )
        );
    }

    private static GenderAgePairDTO socdemPairDTO(Gender gender, AgeSegment ageSegment) {
        return new GenderAgePairDTO(gender, ageSegment);
    }

    private static Set<GenderAgePairDTO> allSocdems() {
        return Stream.of(Gender.values())
                .flatMap(gender ->
                        Stream.of(AgeSegment.values())
                                .map(age -> socdemPairDTO(gender, age))
                ).collect(toSet());
    }

    private static SocdemFilter socdemFilter(
            Set<Gender> fullGenders,
            Set<AgeSegment> fullAges,
            Set<GenderAgePair> otherPairs
    ) {
        return SocdemFilter.builder()
                .gendersForAllAges(fullGenders)
                .agesForAllGenders(fullAges)
                .otherGenderAgePairs(otherPairs)
                .build();
    }

    private static GenderAgePair socdemPair(Gender gender, AgeSegment ageSegment) {
        return GenderAgePair.of(gender, ageSegment);
    }
}
