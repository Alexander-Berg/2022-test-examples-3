package ru.yandex.market.partner.campaign;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.partner.mvc.controller.campaign.model.registration.RecommendationDataResponse;
import ru.yandex.market.partner.mvc.controller.campaign.model.registration.RecommendationTypeResponse;
import ru.yandex.market.partner.test.context.FunctionalTest;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.core.partner.placement.PartnerPlacementProgramType.DROPSHIP;
import static ru.yandex.market.core.partner.placement.PartnerPlacementProgramType.DROPSHIP_BY_SELLER;
import static ru.yandex.market.partner.mvc.controller.campaign.model.registration.RecommendationDataResponse.EMPTY_DBS;
import static ru.yandex.market.partner.mvc.controller.campaign.model.registration.RecommendationDataResponse.EMPTY_FBS;
import static ru.yandex.market.partner.mvc.controller.campaign.model.registration.RecommendationToRegistrationType.MIGRATION;
import static ru.yandex.market.partner.mvc.controller.campaign.model.registration.RecommendationToRegistrationType.REGISTRATION;

/**
 * Проверяем подходит ли партнер под условия регистрации на основе ADV магазина.
 */
@DbUnitDataSet(before = "PartnerCheckReplicationServiceTest.before.csv")
class PartnerCheckReplicationServiceTest extends FunctionalTest {

    private static final RecommendationDataResponse DBS_MIGRATION =
            new RecommendationDataResponse(MIGRATION, DROPSHIP_BY_SELLER);
    private static final RecommendationDataResponse DBS_REGISTRATION =
            new RecommendationDataResponse(REGISTRATION, DROPSHIP_BY_SELLER);
    private static final RecommendationDataResponse FBS_MIGRATION =
            new RecommendationDataResponse(MIGRATION, DROPSHIP);
    @Autowired
    private PartnerCheckReplicationService partnerCheckReplicationService;

    private static Stream<Arguments> checkPartnerTestData() {
        return Stream.of(
                Arguments.of("нормальный магазин", 100L, 100100L, prepExpResp(DBS_MIGRATION, FBS_MIGRATION
                )),
                Arguments.of("субагентский магазин и субагентский пользователь", 101L, 100101L,
                        prepExpResp(DBS_MIGRATION, FBS_MIGRATION)),
                Arguments.of("субагентский магазин и агентский пользователь", 101L, 100111L,
                        prepExpResp(EMPTY_DBS, EMPTY_FBS)),
                Arguments.of("уже реплицированный магазин", 102L, 100102L, prepExpResp(EMPTY_DBS, FBS_MIGRATION
                )),
                Arguments.of("быстрая репликация, хотя магазин выключен", 103L, 100103L,
                        prepExpResp(DBS_MIGRATION, FBS_MIGRATION)),
                Arguments.of("недоступна быстрая репликация:магазин SMB", 104L, 100104L,
                        prepExpResp(DBS_REGISTRATION, FBS_MIGRATION)),
                Arguments.of("недоступна быстрая репликация: магазин c подключенным алкоголем", 105L, 100105L,
                        prepExpResp(DBS_REGISTRATION, FBS_MIGRATION)),
                Arguments.of("магазин c домашним регионом во Владивостоке", 106L, 100106L,
                        prepExpResp(DBS_MIGRATION, EMPTY_FBS)),
                Arguments.of("не ADV магазин", 102102L, 100102L, prepExpResp(EMPTY_DBS, EMPTY_FBS)),
                Arguments.of("нормальный магазин, но из Минска - без FBS", 200L, 100200L,
                        prepExpResp(DBS_MIGRATION, EMPTY_FBS)),
                Arguments.of("нормальный магазин из Северо-Кавказского ФО", 205L, 100200L,
                        prepExpResp(DBS_MIGRATION, FBS_MIGRATION))
        );
    }

    private static Stream<Arguments> canSkipModerationData() {
        return Stream.of(
                Arguments.of("Реплицированный магазин, у которого родитель в белом списке", 102102, true),
                Arguments.of("Реплицированный магазин, у которого родитель не в белом списке", 102107, false),
                Arguments.of("Родительский магазин в белом списке", 102, false),
                Arguments.of("Несуществующий магазин", 99999, false)
        );
    }

    private static RecommendationTypeResponse prepExpResp(RecommendationDataResponse dbs,
                                                          RecommendationDataResponse fbs) {
        return new RecommendationTypeResponse(List.of(dbs, fbs));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("checkPartnerTestData")
    void checkPartnerTest(String description, long partnerId, long userId,
                          RecommendationTypeResponse expectedResponse) {
        assertThat(partnerCheckReplicationService.checkPartner(partnerId, -1, userId))
                .isEqualTo(expectedResponse);
    }


    @ParameterizedTest(name = "{0}")
    @MethodSource("canSkipModerationData")
    void canSkipModerationTest(String description, long partnerId, boolean canSkip) {
        assertThat(partnerCheckReplicationService.canSkipModeration(partnerId)).isEqualTo(canSkip);
    }
}
