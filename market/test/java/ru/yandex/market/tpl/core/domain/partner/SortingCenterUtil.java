package ru.yandex.market.tpl.core.domain.partner;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

import lombok.experimental.UtilityClass;

import ru.yandex.market.tpl.core.domain.company.Company;

/**
 * @author valter
 */
@UtilityClass
public class SortingCenterUtil {

    public SortingCenter sortingCenter() {
        return sortingCenter(SortingCenter.DEFAULT_SC_ID);
    }

    public SortingCenter sortingCenter(long id) {
        return sortingCenter(id, null, null);
    }

    public SortingCenter sortingCenter(long id, int zoneOffset) {
        return sortingCenter(id, null, null, zoneOffset);
    }

    public SortingCenter sortingCenter(long id, Set<Company> companies) {
        return sortingCenter(id, null, companies);
    }

    public SortingCenter sortingCenter(long id, DeliveryService deliveryService) {
        return sortingCenter(id, deliveryService, null);
    }

    public SortingCenter sortingCenter(long id, DeliveryService deliveryService, Set<Company> companies) {
        return sortingCenter(id, deliveryService, companies, null);
    }

    public SortingCenter sortingCenter(long id, DeliveryService deliveryService, Set<Company> companies, Integer zoneOffset) {
        SortingCenter sc = new SortingCenter();
        sc.setId(id);
        sc.setToken("SC token " + id);
        sc.setName("SC name " + id);
        sc.setAddress("SC address " + id);
        sc.setLatitude(BigDecimal.ZERO);
        sc.setLongitude(BigDecimal.ZERO);
        sc.setStartTime(LocalTime.of(11, 0));
        sc.setEndTime(LocalTime.of(23, 0));
        sc.setDeliveryServices(deliveryService == null ? List.of() : List.of(deliveryService));
        sc.setCompanies(companies == null ? Set.of() : companies);
        if(zoneOffset != null) {
            sc.setZoneOffsetHours(zoneOffset);
        }
        sc.setRegionId(1L);
        sc.setLogisticPointId(String.valueOf(id));

        return sc;
    }

}
