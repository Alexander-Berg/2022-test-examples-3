package ru.yandex.market.core.id.service;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.campaign.model.CampaignType;
import ru.yandex.market.id.LegalInfo;

@DbUnitDataSet(before = "LegalInfoServiceTest.before.csv")
class LegalInfoServiceTest extends FunctionalTest {

    @Autowired
    private LegalInfoService legalInfoService;

    @Test
    void testLegalInfoFromPrepayRequest() {
        var legalInfo = legalInfoService.getLegalInfo(1, CampaignType.SUPPLIER);
        Assertions.assertThat(legalInfo)
                .usingRecursiveComparison()
                .ignoringAllOverriddenEquals()
                .isEqualTo(
                        LegalInfo.newBuilder()
                            .setLegalName("orgName")
                            .setType("OOO")
                            .setRegistrationNumber("12345")
                            .setLegalAddress("jurAddrr")
                            .setPhysicalAddress("factAddr")
                            .setInn("7743880975")
                            .setKpp("123456789")
                            .build()
                );
    }

    @Test
    void testLegalInfoFromPrepayRequestTwoRequests() {
        var legalInfo = legalInfoService.getLegalInfo(2, CampaignType.SUPPLIER);
        Assertions.assertThat(legalInfo)
                .usingRecursiveComparison()
                .ignoringAllOverriddenEquals()
                .isEqualTo(
                        LegalInfo.newBuilder()
                                .setLegalName("orgName")
                                .setType("OOO")
                                .setRegistrationNumber("123456")
                                .setLegalAddress("jurAddrr")
                                .setPhysicalAddress("factAddr")
                                .setInn("7743880975")
                                .setKpp("123456789")
                                .build()
                );
    }
}
