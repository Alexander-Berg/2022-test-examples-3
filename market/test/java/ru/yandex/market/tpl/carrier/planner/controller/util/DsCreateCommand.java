package ru.yandex.market.tpl.carrier.planner.controller.util;

import lombok.Builder;
import lombok.Value;

import ru.yandex.market.tpl.carrier.core.domain.company.Company;
import ru.yandex.market.tpl.core.domain.base.Command;

@Builder
@Value
public class DsCreateCommand implements Command<Company> {

    @Override
    public Class getEntityType() {
        return Company.class;
    }

    Long companyId;
    Long dsId;
    String name;
    String token;
    String url;

}
