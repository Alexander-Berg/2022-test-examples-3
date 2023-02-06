package ru.yandex.market.core.campaign;

import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.core.partner.PartnerQuery;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit-тесты на {@link PartnerQuery}.
 *
 * @author fbokovikov
 */
class PartnerQueryTest {

    static Stream<Arguments> queryStateArgs() {
        return Stream.of(
                Arguments.of("12345678", "12345678", 12345678L),
                Arguments.of("11-12345678", "12345678", 12345678L),
                Arguments.of("12345678-11", "12345678-11", null),
                Arguments.of("supp", "supp", null),
                Arguments.of("supp-12", "supp-12", null),
                Arguments.of("c3p0", "c3p0", null),
                Arguments.of("1&1", "1&1", null)
        );
    }

    @ParameterizedTest
    @DisplayName("Тесты на состояние поискового запроса")
    @MethodSource("queryStateArgs")
    void queryState(String query, String expectedNormalizedQuery, Long expectedCampaignQuery) {
        PartnerQuery partnerQuery = PartnerQuery.builder().withQuery(query).build();
        assertThat(partnerQuery.getQuery()).isEqualTo(expectedNormalizedQuery);
        assertThat(partnerQuery.asNumericQuery().orElse(-1))
                .isEqualTo(expectedCampaignQuery == null
                        ? -1
                        : expectedCampaignQuery);
    }
}
