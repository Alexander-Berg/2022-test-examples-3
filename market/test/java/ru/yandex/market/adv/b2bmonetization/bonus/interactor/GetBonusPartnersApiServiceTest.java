package ru.yandex.market.adv.b2bmonetization.bonus.interactor;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;

import ru.yandex.market.adv.b2bmonetization.AbstractMonetizationTest;
import ru.yandex.market.common.test.db.DbUnitDataSet;

@DisplayName("Тесты на endpoint GET /v1/bonus/partners")
@ParametersAreNonnullByDefault
class GetBonusPartnersApiServiceTest extends AbstractMonetizationTest {

    @DisplayName("Вернули бонусы по списку партнеров")
    @DbUnitDataSet(
            before = "Get/csv/v1BonusPartnersGet_partnerIds_response.before.csv"
    )
    @Test
    void v1BonusPartnersGet_partnerIds_response() {
        mvcPerform(
                HttpMethod.GET,
                "/v1/bonus/info?partner_ids=1,2",
                200,
                "Get/json/response/v1BonusPartnersGet_partnerIds_response.json",
                null,
                true
        );
    }

    @DisplayName("Вернули бонусы по списку партнеров и дате действия")
    @DbUnitDataSet(
            before = "Get/csv/v1BonusPartnersGet_partnerIdsAndDate_response.before.csv"
    )
    @Test
    void v1BonusPartnersGet_partnerIdsAndDate_response() {
        mvcPerform(
                HttpMethod.GET,
                "/v1/bonus/info?partner_ids=1,2&date=2022-01-10",
                200,
                "Get/json/response/v1BonusPartnersGet_partnerIdsAndDate_response.json",
                null,
                true
        );
    }

    @DisplayName("Не передан список партнеров, ответили 400")
    @Test
    void v1BonusPartnersGet_noPartnerIds_badRequest() {
        mvcPerform(
                HttpMethod.GET,
                "/v1/bonus/info?date=2022-01-10",
                400,
                "Get/json/response/v1BonusPartnersGet_noPartnerIds_badRequest.json",
                null,
                true
        );
    }
}
