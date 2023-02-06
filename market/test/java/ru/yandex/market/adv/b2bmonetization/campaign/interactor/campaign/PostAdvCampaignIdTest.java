package ru.yandex.market.adv.b2bmonetization.campaign.interactor.campaign;

import javax.annotation.ParametersAreNonnullByDefault;

import org.hamcrest.number.OrderingComparison;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;

import ru.yandex.market.adv.b2bmonetization.AbstractMonetizationTest;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

/**
 * Date: 18.02.2022
 * Project: b2bmarketmonetization
 *
 * @author alexminakov
 */
@DisplayName("Тесты на endpoint POST /v1/adv/campaign/id.")
@ParametersAreNonnullByDefault
class PostAdvCampaignIdTest extends AbstractMonetizationTest {

    @DisplayName("Запросили следующий идентификатор и получили его больше или равным 1.")
    @Test
    void v1AdvCampaignIdPost_nextId_greaterThanOrEqualOne() throws Exception {
        mvcPerform(
                HttpMethod.POST,
                "/v1/adv/campaign/id",
                200,
                null,
                null,
                true
        )
                .andExpect(
                        jsonPath("$.id")
                                .value(OrderingComparison.greaterThanOrEqualTo(1))
                );
    }
}
