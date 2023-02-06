package ru.yandex.market.core.outlet.legal.db;

import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.campaign.model.CampaignType;
import ru.yandex.market.core.campaign.model.PartnerId;
import ru.yandex.market.core.error.EntityNotFoundException;
import ru.yandex.market.core.orginfo.exception.InvalidRegNumException;
import ru.yandex.market.core.orginfo.model.OrganizationType;
import ru.yandex.market.core.outlet.OutletLegalInfo;
import ru.yandex.market.core.outlet.legal.OutletLegalInfoService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Тест для {@link DbOutletLegalInfoService}
 *
 * @author stani on 08.08.18.
 */

@DbUnitDataSet(before = "DbOutletLegalInfoServiceFunctionalTest.before.csv")
class DbOutletLegalInfoServiceFunctionalTest extends FunctionalTest {

    private static final long OUTLET_WITH_LEGAL_INFO = 101L;
    private static final long OUTLET_WITHOUT_LEGAL_INFO = 102L;
    private static final long OUTLET_NOT_FOUND = 404L;
    private static final long PARTNER_ID = 106L;
    private static final PartnerId PARTNER = PartnerId.partnerId(PARTNER_ID, CampaignType.SUPPLIER);
    private static final long ACTION_ID = 101L;

    @Autowired
    private OutletLegalInfoService outletLegalInfoService;

    @Test
    void testGetOutletLegalInfo() {
        assertThat(new OutletLegalInfo.Builder()
                .setOutletId(OUTLET_WITH_LEGAL_INFO)
                .setOrganizationType(OrganizationType.OOO)
                .setOrganizationName("GazMas")
                .setRegistrationNumber("12345")
                .setJuridicalAddress("Kemerovo ul Lenina 1")
                .setFactAddress("Kemerovo ul Lenina 2")
                .build()).isEqualTo(outletLegalInfoService.getLegalInfo(OUTLET_WITH_LEGAL_INFO, PARTNER_ID));
    }

    @Test
    void testGetOutletLegalInfos() {
        assertThat(outletLegalInfoService.getLegalInfos(Arrays.asList(101L, 104L, 105L)))
                .containsExactlyInAnyOrder(
                        new OutletLegalInfo.Builder()
                                .setOutletId(101L)
                                .setOrganizationType(OrganizationType.OOO)
                                .setOrganizationName("GazMas")
                                .setRegistrationNumber("12345")
                                .setJuridicalAddress("Kemerovo ul Lenina 1")
                                .setFactAddress("Kemerovo ul Lenina 2")
                                .build(),
                        new OutletLegalInfo.Builder()
                                .setOutletId(104L)
                                .setOrganizationType(OrganizationType.OOO)
                                .setOrganizationName("Novostroy")
                                .setRegistrationNumber("12346")
                                .setJuridicalAddress("Novosibirsk ul Chehova 1")
                                .setFactAddress("Novosibirsk ul Chehova 2")
                                .build(),
                        new OutletLegalInfo.Builder()
                                .setOutletId(105L)
                                .setOrganizationType(OrganizationType.NONE)
                                .build()
                );
    }

    @Test
    void testGetOutletLegalInfosEmpty() {
        assertThat(outletLegalInfoService.getLegalInfos(Collections.singletonList(OUTLET_NOT_FOUND))).isEmpty();
    }

    @Test
    void testInsertOutletLegalInfo() {
        assertThat(outletLegalInfoService.getLegalInfo(OUTLET_WITHOUT_LEGAL_INFO, PARTNER_ID)).isNull();
        OutletLegalInfo outlet = createOutletLegalInfo(OUTLET_WITHOUT_LEGAL_INFO, "Romashka", "5077746887312");
        outletLegalInfoService.updateLegalInfo(outlet, PARTNER, ACTION_ID);
        assertThat(outletLegalInfoService.getLegalInfo(OUTLET_WITHOUT_LEGAL_INFO, PARTNER_ID)).isEqualTo(outlet);
    }

    @Test
    void testUpdateOutletLegalInfo() {
        assertThat(outletLegalInfoService.getLegalInfo(OUTLET_WITH_LEGAL_INFO, PARTNER_ID)).isNotNull();
        OutletLegalInfo outlet = createOutletLegalInfo(OUTLET_WITH_LEGAL_INFO, "Romashka", "5077746887312");
        outletLegalInfoService.updateLegalInfo(outlet, PARTNER, ACTION_ID);
        assertThat(outletLegalInfoService.getLegalInfo(OUTLET_WITH_LEGAL_INFO, PARTNER_ID)).isEqualTo(outlet);
    }

    @Test
    void testUpdateOutletLegalInvalidOgrnInfo() {
        assertThat(outletLegalInfoService.getLegalInfo(OUTLET_WITH_LEGAL_INFO, PARTNER_ID)).isNotNull();
        OutletLegalInfo outlet = createOutletLegalInfo(OUTLET_WITH_LEGAL_INFO, "Romashka", "123455");
        assertThatExceptionOfType(InvalidRegNumException.class)
                .isThrownBy(() -> outletLegalInfoService.updateLegalInfo(outlet, PARTNER, ACTION_ID));
    }


    @Test
    void testDeleteOutletLegalInfo() {
        assertThat(outletLegalInfoService.getLegalInfo(OUTLET_WITH_LEGAL_INFO, PARTNER_ID)).isNotNull();
        outletLegalInfoService.deleteLegalInfo(OUTLET_WITH_LEGAL_INFO, ACTION_ID);
        assertThat(outletLegalInfoService.getLegalInfo(OUTLET_WITH_LEGAL_INFO, PARTNER_ID)).isNull();
    }

    @Test
    void testInsertNotFoundOutletInfo() {
        assertThatExceptionOfType(EntityNotFoundException.class)
                .isThrownBy(() -> outletLegalInfoService.updateLegalInfo(
                        createOutletLegalInfo(OUTLET_NOT_FOUND, "romashka", "5077746887312"), PARTNER, ACTION_ID)
                );

    }

    @Test
    void testDeleteNotFoundOutletLegalInfo() {
        assertThatExceptionOfType(EntityNotFoundException.class)
                .isThrownBy(() -> outletLegalInfoService.deleteLegalInfo(OUTLET_NOT_FOUND, 1L));
    }

    private OutletLegalInfo createOutletLegalInfo(long outletId, String organizationName, String registrationNumber) {
        return new OutletLegalInfo.Builder()
                .setOutletId(outletId)
                .setOrganizationType(OrganizationType.OOO)
                .setOrganizationName(organizationName)
                .setRegistrationNumber(registrationNumber)
                .setJuridicalAddress("Russia")
                .setFactAddress("Moscow")
                .build();

    }
}

