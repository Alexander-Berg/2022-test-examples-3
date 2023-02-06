package ru.yandex.market.partner.mvc.controller.campaign;

import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.mbi.util.MbiMatchers;
import ru.yandex.market.mbi.util.MoreMbiMatchers;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.partner.util.FunctionalTestHelper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static ru.yandex.market.core.partner.placement.PartnerPlacementProgramType.DROPSHIP_BY_SELLER;

/**
 * Проверяем работу {@link PartnerRecommendedRegistrationController}.
 */
@DbUnitDataSet(before = "../../../campaign/PartnerCheckReplicationServiceTest.before.csv")
class PartnerRecommendedRegistrationControllerTest extends FunctionalTest {

    @Test
    void testGetRecommendationToRegistrationNotFound() {
        assertThatThrownBy(() -> FunctionalTestHelper.get(
                baseUrl + "/campaigns/{campaignId}/recommendation?_user_id={userId}",
                10666L, DROPSHIP_BY_SELLER, 1010L))
                .isInstanceOfSatisfying(HttpClientErrorException.class, exception -> {
                    assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                });

    }

    @Test
    void testGetRecommendationToRegistration() {
        ResponseEntity<String> response = FunctionalTestHelper.get(
                baseUrl + "/campaigns/{campaignId}/recommendation?_user_id={userId}",
                4100L, DROPSHIP_BY_SELLER, 1010L);
        String expectedResponse = "" +
                "{\n" +
                "  \"cpaOnly\": true,\n" +
                "  \"recommendations\": [\n" +
                "    {\n" +
                "      \"placementType\": \"DROPSHIP_BY_SELLER\",\n" +
                "      \"recommendationType\": \"MIGRATION\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"placementType\": \"DROPSHIP\",\n" +
                "      \"recommendationType\": \"MIGRATION\"\n" +
                "    }\n" +
                "  ]\n" +
                "}";
        MatcherAssert.assertThat(response, MoreMbiMatchers.responseBodyMatches(MbiMatchers.jsonPropertyEquals("result",
                expectedResponse)));
    }
}
