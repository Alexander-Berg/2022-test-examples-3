package ru.yandex.market.core.business;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.ds.model.InvalidInternalPartnerNameException;

import static org.assertj.core.api.Assertions.assertThat;

class BusinessCreationServiceTest extends FunctionalTest {

    @Autowired
    private BusinessCreationService businessCreationService;

    @DisplayName("Проверяем создание бизнеса с невалидным именем")
    @Test
    void createBusinessWithInvalidName() {
        BusinessInfo newBusiness = new BusinessInfo(17, "#@%$;\\");
        Assertions.assertThatThrownBy(() -> businessCreationService.createBusiness(
                new BusinessRegistrationRequest(213L, newBusiness.getName(), null, null, false),
                1007L,
                1L,
                true
        )).isInstanceOf(InvalidInternalPartnerNameException.class);
    }

    @Test
    @DisplayName("Проверяем, что создание бизнеса с валидным именем не вызывает ошибок")
    @DbUnitDataSet(before = "BusinessCreationServiceTest.before.csv")
    void createBusiness() {
        BusinessInfo newBusiness = new BusinessInfo(17, "lavka:ru?");
        BusinessInfo createdBusiness = businessCreationService.createBusiness(
                new BusinessRegistrationRequest(213L, newBusiness.getName(), null, null, false),
                1007L,
                1L,
                true
        );
        assertThat(createdBusiness.getCampaignId()).isEqualTo(1L);
    }

    @Test
    @DbUnitDataSet(before = "BusinessCreationServiceTest.before.csv")
    void testCreateBusinessForUidByRegistration() {
        BusinessRegistrationRequest request =
                new BusinessRegistrationRequest(null, "lavka:ru?", null, null, true);
        BusinessInfo businessInfo = businessCreationService.createBusinessForUid(request, 1007L, 1L);
        assertThat(businessInfo.getName()).isEqualTo("lavka:ru?");
        assertThat(businessInfo.getCampaignId()).isEqualTo(1L);
        assertThat(businessInfo.getId()).isEqualTo(1L);
        assertThat(businessInfo.isDeleted()).isEqualTo(false);
    }
}
