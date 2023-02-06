package ru.yandex.market.b2bcrm.module.ticket.test;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.b2bcrm.module.ticket.B2bLeadTicket;
import ru.yandex.market.b2bcrm.module.ticket.test.config.B2bTicketTests;
import ru.yandex.market.b2bcrm.module.ticket.test.utils.B2bTicketTestUtils;
import ru.yandex.market.jmf.bcp.BcpService;
import ru.yandex.market.jmf.entity.EntityStorageService;
import ru.yandex.market.jmf.module.geo.Region;
import ru.yandex.market.jmf.module.geo.RegionType;

import static org.assertj.core.api.Assertions.assertThat;

@B2bTicketTests
public class B2bLeadTicketPromoFederalDisctrictTest {
    @Inject
    EntityStorageService entityStorageService;
    @Inject
    private B2bTicketTestUtils b2bTicketTestUtils;
    @Inject
    private BcpService bcpService;

    private RegionType cityRegionType;
    private RegionType federalRegionType;

    @BeforeEach
    public void init() {
        cityRegionType = entityStorageService.getByNaturalId(RegionType.FQN, "5");
        federalRegionType = entityStorageService.getByNaturalId(RegionType.FQN, "3");
    }

    @Test
    @DisplayName("Создание лида с пустым promoCity")
    public void setNullPromoRegionTest() {
        var ticket = b2bTicketTestUtils.createB2bLead(Map.of());

        assertThat(ticket.getPromoCity()).isNull();
        assertThat(ticket.getPromoFederalDistrict()).isNull();
    }

    @Test
    @DisplayName("Создание лида с заполненным promoCity")
    public void setPromoCityTest() {
        var region = getRegion("123", "124");
        var ticket = b2bTicketTestUtils.createB2bLead(Map.of(B2bLeadTicket.PROMO_CITY, region));

        assertThat(ticket.getPromoCity()).isEqualTo(region);
        assertThat(ticket.getPromoFederalDistrict()).isEqualTo(region.getFederalDistrict());
    }

    @Test
    @DisplayName("Обновление promoCity в лиде, где он уже есть")
    public void updatePromoCityTest() {
        var region1 = getRegion("123", "124");
        var ticket = b2bTicketTestUtils.createB2bLead(Map.of(B2bLeadTicket.PROMO_CITY, region1));

        var region2 = getRegion("234", "235");
        bcpService.edit(ticket, Map.of(B2bLeadTicket.PROMO_CITY, region2));

        assertThat(ticket.getPromoCity()).isEqualTo(region2);
        assertThat(ticket.getPromoFederalDistrict()).isEqualTo(region2.getFederalDistrict());
    }

    public Region getRegion(String cityCode, String federalDisctrictCode) {
        Map<String, Object> federalDistrictAttributes = new HashMap<>();
        federalDistrictAttributes.put(Region.CODE, federalDisctrictCode);
        federalDistrictAttributes.put(Region.TITLE, federalDisctrictCode);
        federalDistrictAttributes.put(Region.REGION_TYPE, federalRegionType);
        federalDistrictAttributes.put(Region.FEDERAL_DISTRICT, null);
        Region federalDistrict = bcpService.create(Region.FQN, federalDistrictAttributes);

        Map<String, Object> cityAttributes = new HashMap<>();
        cityAttributes.put(Region.CODE, cityCode);
        cityAttributes.put(Region.TITLE, cityCode);
        cityAttributes.put(Region.REGION_TYPE, cityRegionType);
        cityAttributes.put(Region.FEDERAL_DISTRICT, federalDistrict);

        return bcpService.create(Region.FQN, cityAttributes);
    }
}
