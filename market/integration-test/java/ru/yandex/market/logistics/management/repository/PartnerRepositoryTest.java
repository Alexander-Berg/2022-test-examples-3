package ru.yandex.market.logistics.management.repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.logistics.management.AbstractContextualTest;
import ru.yandex.market.logistics.management.domain.entity.CargoType;
import ru.yandex.market.logistics.management.domain.entity.Partner;
import ru.yandex.market.logistics.management.domain.entity.PartnerShop;
import ru.yandex.market.logistics.management.domain.entity.PartnerSubtype;
import ru.yandex.market.logistics.management.domain.entity.PartnerTariff;
import ru.yandex.market.logistics.management.domain.entity.TariffLocation;
import ru.yandex.market.logistics.management.domain.entity.type.PartnerType;
import ru.yandex.market.logistics.management.entity.type.PartnerExternalParamType;

@SuppressWarnings("checkstyle:MagicNumber")
class PartnerRepositoryTest extends AbstractContextualTest {

    @Autowired
    private PartnerRepository repository;

    @Test
    @DatabaseSetup("/data/repository/partner/partners.xml")
    void findAllEmptyPartners() {
        List<Partner> fulfillments = repository.findAllByPartnerType(PartnerType.FULFILLMENT);
        softly.assertThat(fulfillments).as("Fulfillments failed")
            .hasSize(2)
            .extracting(Partner::getName)
            .containsExactlyInAnyOrder("fulfillment", "fulfillment1");

        List<Partner> deliveries = repository.findAllByPartnerType(PartnerType.DELIVERY);
        softly.assertThat(deliveries).as("Deliveries failed")
            .hasSize(1)
            .extracting(Partner::getName)
            .containsExactly("delivery");

        List<Partner> sortingCenters = repository.findAllByPartnerType(PartnerType.SORTING_CENTER);
        softly.assertThat(sortingCenters).as("Sorting centers failed")
            .hasSize(1)
            .extracting(Partner::getName)
            .containsExactly("sorting_center");
    }

    @Test
    @DatabaseSetup("/data/repository/partner/partners_with_everything.xml")
    @Transactional
    void findAllWithTariffs() {
        Set<PartnerTariff> tariffs = getPartner()
            .getPartnerTariffs();
        softly.assertThat(tariffs)
            .as("Should be exactly 2 tariffs").hasSize(2);

        Set<CargoType> cargoTypes = tariffs.stream()
            .map(PartnerTariff::getCargoTypes)
            .flatMap(Set::stream)
            .collect(Collectors.toSet());
        softly.assertThat(cargoTypes)
            .as("Should be exactly 2 cargo types").hasSize(2);

        Set<TariffLocation> tariffLocations = tariffs.stream()
            .map(PartnerTariff::getTariffLocations)
            .flatMap(Set::stream)
            .collect(Collectors.toSet());
        softly.assertThat(tariffLocations)
            .as("Should be exactly tariff for location").hasSize(1);

        Set<CargoType> locationCargoTypes = tariffLocations.stream()
            .map(TariffLocation::getCargoType)
            .collect(Collectors.toSet());
        softly.assertThat(locationCargoTypes)
            .as("Should be exactly cargo type for tariff with location").hasSize(1);
    }

    @Test
    @DatabaseSetup("/data/repository/partner/partners_with_everything.xml")
    @Transactional
    void removeTariffLocation() {
        Partner partner = getPartner();

        PartnerTariff partnerTariff = getPartner().getPartnerTariffs().stream()
            .filter(pt -> pt.getTariffLocations().size() > 0)
            .findFirst()
            .orElse(null);
        softly.assertThat(partnerTariff).as("Partner tariff with location tariff should exist").isNotNull();

        TariffLocation tariffLocation = partnerTariff.getTariffLocations().stream()
            .findFirst()
            .orElse(null);
        softly.assertThat(tariffLocation).as("Tariff location should exist").isNotNull();

        partnerTariff.removeTariffLocation(tariffLocation);
        repository.saveAndFlush(partner);

        long locationTariffsCount = getPartner().getPartnerTariffs().stream()
            .map(PartnerTariff::getTariffLocations)
            .mapToLong(Set::size)
            .sum();
        softly.assertThat(locationTariffsCount)
            .as("The only location tariff should be deleted").isEqualTo(0);
    }

    @Test
    @DatabaseSetup("/data/repository/partner/partners_with_everything.xml")
    @Transactional
    void removeTariff() {
        Partner partner = getPartner();
        PartnerTariff tariff = getPartner().getPartnerTariffs().stream()
            .filter(t -> t.getTariffLocations().size() > 0)
            .findFirst()
            .orElse(null);
        softly.assertThat(tariff).as("Tariff should exist").isNotNull();

        partner.getPartnerTariffs().remove(tariff);
        repository.saveAndFlush(partner);

        Partner updatedPartner = getPartner();

        softly.assertThat(updatedPartner.getPartnerTariffs())
            .as("One tariff should be deleted").hasSize(1);

        long locationTariffsCount = updatedPartner.getPartnerTariffs().stream()
            .map(PartnerTariff::getTariffLocations)
            .mapToLong(Set::size)
            .sum();
        softly.assertThat(locationTariffsCount)
            .as("The only location tariff should be deleted").isEqualTo(0);
    }

    @Test
    @DatabaseSetup("/data/repository/partner/partners_with_everything.xml")
    @Transactional
    void removeCargoType() {
        Partner partner = getPartner();
        PartnerTariff tariff = partner.getPartnerTariffs().stream()
            .findFirst()
            .orElse(null);
        softly.assertThat(tariff).as("Tariff should exist").isNotNull();

        CargoType cargoType = tariff.getCargoTypes().stream()
            .findFirst()
            .orElse(null);
        softly.assertThat(cargoType).as("Cargo type should exist").isNotNull();

        tariff.removeCargoType(cargoType);
        repository.saveAndFlush(partner);

        Set<PartnerTariff> tariffs = getPartner().getPartnerTariffs();
        Set<CargoType> cargoTypes = tariffs.stream()
            .map(PartnerTariff::getCargoTypes)
            .flatMap(Set::stream)
            .collect(Collectors.toSet());
        softly.assertThat(cargoTypes).as("One cargo type should be deleted").hasSize(1);
    }

    @Test
    @DatabaseSetup(
        value = "/data/repository/partner/partners_with_settings_api.xml",
        connection = "dbUnitQualifiedDatabaseConnection"
    )
    void findAllBySettingsApiToken() {
        List<Partner> partners = repository.findAllBySettingsApiToken("token");
        softly.assertThat(partners)
            .as("Partners are not empty")
            .isNotEmpty()
            .as("There are 2 partners with this token")
            .hasSize(2);
    }

    @Test
    @DatabaseSetup("/data/repository/partner/partners_with_everything.xml")
    @Transactional
    void findWithPartnerShops() {
        Partner partner = getPartner();
        Set<PartnerShop> partnerShop = partner.getShops();
        softly.assertThat(partnerShop).as("Should be exactly 1 shop").hasSize(1);
    }

    @Test
    @DatabaseSetup("/data/repository/partner/partners_with_everything.xml")
    @Transactional
    void findWithPartnerSubtype() {
        Partner partner = getPartner();
        PartnerSubtype partnerSubtype = partner.getPartnerSubtype();

        softly.assertThat(partnerSubtype).as("PartnerSubtype should exist").isNotNull();
        softly
            .assertThat(partnerSubtype.getId())
            .as("PartnerSubtype for this partner should have id = 2")
            .isEqualTo(2L);
        softly
            .assertThat(partnerSubtype.getPartnerType())
            .as("PartnerType must be the same as PartnerType of the partnerSubtype")
            .isEqualTo(partner.getPartnerType());
    }

    @Test
    @DatabaseSetup("/data/repository/partner/partners_with_everything.xml")
    @Transactional
    void findByMarketIdsWithCargoTypesTest() {
        List<Long> ids = new ArrayList<>();
        ids.add(1L);
        ids.add(2L);
        ids.add(4L);
        List<Partner> withCargoTypes = repository.getAllWithForbiddenCargoTypesByIdIn(ids);

        softly.assertThat(withCargoTypes).as("Should have all three of them").hasSize(3);
        Map<Long, Set<Integer>> partnerToCargoTypes =
            withCargoTypes.stream()
                .collect(Collectors.toMap(
                    Partner::getId,
                    p -> p.getForbiddenCargoTypes().stream().map(CargoType::getCargoType).collect(Collectors.toSet())));
        softly.assertThat(partnerToCargoTypes.get(1L)).containsExactlyInAnyOrder(321);
        softly.assertThat(partnerToCargoTypes.get(2L)).containsExactlyInAnyOrder(123, 456, 321);
        softly.assertThat(partnerToCargoTypes.get(4L)).containsExactlyInAnyOrder(456);
    }

    @Test
    @DatabaseSetup("/data/repository/partner/connected_partners.xml")
    void getConnectedPartners() {
        var ids = repository.getConnectedPartners(PartnerType.DROPSHIP, PartnerExternalParamType.IS_DROPOFF);
        softly.assertThat(ids).containsOnly(11L);
        softly.assertThat(ids).as("Partner's movement segment service is not SHIPMENT").doesNotContain(12L);
        softly.assertThat(ids).as("Partner's movement segment service is inactive").doesNotContain(13L);
        softly.assertThat(ids).as("Partner is not connected to a destination").doesNotContain(14L);
        softly.assertThat(ids).as("Partner type is not dropship").doesNotContain(15L);
        softly.assertThat(ids).as("Source segment type is not warehouse").doesNotContain(16L);
        softly.assertThat(ids).as("Movement segment type is not movement").doesNotContain(17L);
        softly.assertThat(ids).as("Destination segment type is not warehouse").doesNotContain(18L);
        softly.assertThat(ids).as("Destination partner is not dropoff enabled").doesNotContain(19L);
        softly.assertThat(ids).as("Destination partner does not have dropoff capability").doesNotContain(10L);
    }

    private Partner getPartner() {
        Partner partner = repository.findById(2L).orElse(null);
        softly.assertThat(partner).as("Partner with id 2 should exist").isNotNull();
        return partner;
    }
}
