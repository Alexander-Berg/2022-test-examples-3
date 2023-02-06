package ru.yandex.market.delivery.mdbapp.integration.enricher.delivery.json;

import com.fasterxml.jackson.annotation.JsonProperty;

import ru.yandex.market.delivery.mdbapp.integration.payload.CreateIntakeInLgw;
import ru.yandex.market.logistic.gateway.common.model.common.Partner;
import ru.yandex.market.logistic.gateway.common.model.delivery.Intake;

public class CreateIntakeInLgwMixIn extends CreateIntakeInLgw {

    public CreateIntakeInLgwMixIn(@JsonProperty("intake") Intake intake,
                                  @JsonProperty("partner") Partner partner) {
        super(intake, partner);
    }
}
