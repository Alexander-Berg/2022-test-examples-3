package ru.yandex.market.tpl.carrier.core.domain.partner;

import java.util.Objects;
import java.util.Set;

import lombok.experimental.UtilityClass;

import ru.yandex.market.tpl.carrier.core.domain.company.Company;

@UtilityClass
public class DeliveryServiceUtil {

    public DeliveryService deliveryService(long id) {
        return deliveryService(id, null);
    }

    public DeliveryService deliveryService(long id, Set<Company> companies) {
        DeliveryService deliveryService = new DeliveryService();
        deliveryService.setId(id);
        deliveryService.setName("DS name " + id);
        deliveryService.setToken("DS token " + id);
        deliveryService.setCompanies(Objects.requireNonNullElse(companies, Set.of()));
        return deliveryService;
    }
}
