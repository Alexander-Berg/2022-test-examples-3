package ru.yandex.market.core.client.remove;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Проверяем {@link RemoveClientMigrationService}
 */
class RemoveClientMigrationServiceTest extends FunctionalTest {
    @Autowired
    private RemoveClientMigrationService removeClientMigrationService;

    public static Stream<Arguments> dataCheckGetClientsRelatedToBalanceByCampaignIds() {
        return Stream.of(
                Arguments.of("Пустой список партнеров", List.of(), Set.of()),
                Arguments.of("Ид кампаний партнеров на взаимозачете", List.of(11L, 12L), Set.of()),
                Arguments.of("Ид кампаний партнеров не на взаимозачете", List.of(21L), Set.of(210L)),
                Arguments.of("Ид кампаний магазинов (ADV)", List.of(31L, 32L), Set.of(310L, 320L)),
                Arguments.of("Ид кампаний бизнесов c партнерам на взаимозачете", List.of(101L), Set.of()),
                Arguments.of("Ид кампаний бизнесов c партнерам не на взаимозачете", List.of(201L, 202L), Set.of(200L,
                        210L,
                        220L)),
                Arguments.of("Ид кампаний бизнесов c разными партнерам", List.of(101L, 202L, 301L), Set.of(220L,
                        310L, 320L)),
                Arguments.of("Ид кампаний партнеров и бизнесов", List.of(11L, 201L, 31L), Set.of(200L, 210L, 310L))
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("dataCheckGetClientsRelatedToBalanceByCampaignIds")
    @DbUnitDataSet(before = "RemoveClientMigrationServiceTest.before.csv")
    void checkGetClientsRelatedToBalanceByCampaignIds(String description,
                                                      List<Long> campaignIds,
                                                      Set<Long> expectedClients) {
        Set<Long> clientsRelatedToBalanceByCampaignIds =
                removeClientMigrationService.getClientsRelatedToBalanceByCampaignIds(campaignIds);
        assertThat(clientsRelatedToBalanceByCampaignIds).isEqualTo(expectedClients);
    }
}
