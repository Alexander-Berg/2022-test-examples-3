package ru.yandex.market.logistics.management.facade;

import java.util.Set;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.management.AbstractContextualAspectValidationTest;
import ru.yandex.market.logistics.management.entity.request.support.ChangePartnerCargoTypesFilter;
import ru.yandex.market.logistics.management.entity.request.support.ChangePartnerCargoTypesRequest;
import ru.yandex.market.logistics.management.entity.type.PartnerType;

@DatabaseSetup("/data/facade/partner/before/partners_with_cargo_types.xml")
public class PartnerForbiddenCargoTypesFacadeTest extends AbstractContextualAspectValidationTest {
    @Autowired
    private PartnerFacade partnerFacade;

    @Test
    @ExpectedDatabase(value = "/data/facade/partner/after/added_cargo_type.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    void addCargoTypesToBlackListToMultiplePartnersTest_byId() {
        ChangePartnerCargoTypesRequest request = ChangePartnerCargoTypesRequest.newBuilder()
            .changePartnerCargoTypesFilter(
                ChangePartnerCargoTypesFilter.newBuilder().partnerIds(Set.of(1L, 3L)).build()
            )
            .cargoTypes(Set.of(101, 102))
            .build();
        partnerFacade.addCargoTypesToBlackListToMultiplePartners(request);
    }

    @Test
    @ExpectedDatabase(value = "/data/facade/partner/after/added_cargo_type.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    void addCargoTypesToBlackListToMultiplePartnersTest_byType() {
        ChangePartnerCargoTypesRequest request = ChangePartnerCargoTypesRequest.newBuilder()
            .changePartnerCargoTypesFilter(
                ChangePartnerCargoTypesFilter.newBuilder().partnerTypes(Set.of(PartnerType.DELIVERY)).build()
            )
            .cargoTypes(Set.of(101, 102))
            .build();
        partnerFacade.addCargoTypesToBlackListToMultiplePartners(request);
    }

    @Test
    @ExpectedDatabase(value = "/data/facade/partner/after/added_cargo_type_sort_by_type.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    void addCargoTypesToBlackListToMultiplePartnersTest_byIdAndType() {
        ChangePartnerCargoTypesRequest request = ChangePartnerCargoTypesRequest.newBuilder()
            .changePartnerCargoTypesFilter(
                ChangePartnerCargoTypesFilter
                    .newBuilder()
                    .partnerIds(Set.of(1L))
                    .partnerTypes(Set.of(PartnerType.SORTING_CENTER))
                    .build()
            )
            .cargoTypes(Set.of(104))
            .build();
        partnerFacade.addCargoTypesToBlackListToMultiplePartners(request);
    }

    @Test
    @ExpectedDatabase(value = "/data/facade/partner/after/added_cargo_type_sort_by_type.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    void addCargoTypesToBlackListToMultiplePartnersTest_byIdAndSubType() {
        ChangePartnerCargoTypesRequest request = ChangePartnerCargoTypesRequest.newBuilder()
            .changePartnerCargoTypesFilter(
                ChangePartnerCargoTypesFilter
                    .newBuilder()
                    .partnerIds(Set.of(1L))
                    .partnerSubtypeIds(Set.of(2L))
                    .build()
            )
            .cargoTypes(Set.of(104))
            .build();
        partnerFacade.addCargoTypesToBlackListToMultiplePartners(request);
    }

    @Test
    @ExpectedDatabase(value = "/data/facade/partner/after/removed_cargo_types_for_all.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    void removeCargoTypesFromPartners_byId() {
        ChangePartnerCargoTypesRequest request = ChangePartnerCargoTypesRequest.newBuilder()
            .changePartnerCargoTypesFilter(
                ChangePartnerCargoTypesFilter
                    .newBuilder()
                    .partnerIds(Set.of(1L, 2L))
                    .build()
            )
            .cargoTypes(Set.of(101, 102))
            .build();
        partnerFacade.removeCargoTypesFromPartners(request);
    }

    @Test
    @ExpectedDatabase(value = "/data/facade/partner/after/removed_cargo_types.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    void removeCargoTypesFromPartners_byType() {
        ChangePartnerCargoTypesRequest request = ChangePartnerCargoTypesRequest.newBuilder()
            .changePartnerCargoTypesFilter(
                ChangePartnerCargoTypesFilter
                    .newBuilder()
                    .partnerTypes(Set.of(PartnerType.DELIVERY))
                    .build()
            )
            .cargoTypes(Set.of(101, 102))
            .build();
        partnerFacade.removeCargoTypesFromPartners(request);
    }

    @Test
    @ExpectedDatabase(value = "/data/facade/partner/after/removed_cargo_types_for_all.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    void removeCargoTypesFromPartners_byIdAndType() {
        ChangePartnerCargoTypesRequest request = ChangePartnerCargoTypesRequest.newBuilder()
            .changePartnerCargoTypesFilter(
                ChangePartnerCargoTypesFilter
                    .newBuilder()
                    .partnerIds(Set.of(1L))
                    .partnerTypes(Set.of(PartnerType.SORTING_CENTER))
                    .build()
            )
            .cargoTypes(Set.of(101, 102))
            .build();
        partnerFacade.removeCargoTypesFromPartners(request);
    }
}
