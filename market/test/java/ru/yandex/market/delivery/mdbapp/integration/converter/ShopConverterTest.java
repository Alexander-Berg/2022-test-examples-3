package ru.yandex.market.delivery.mdbapp.integration.converter;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import steps.PartnerInfoSteps;

import ru.yandex.market.mbi.api.client.entity.partner.PartnerInfoDTO;
import ru.yandex.market.mbi.api.client.entity.shops.Shop;

public class ShopConverterTest {

    @Test
    public void successfulConvert() {
        PartnerInfoDTO partnerInfoDTO = PartnerInfoSteps.getPartnerInfoDTO(123);
        Shop shop = ShopConverter.convert(partnerInfoDTO);
        Assertions.assertThat(shop.getId()).as("Shop id").isEqualTo(partnerInfoDTO.getId());
        Assertions.assertThat(shop.getName()).as("Shop name").isEqualTo(partnerInfoDTO.getName());
        Assertions.assertThat(shop.getShopName()).as("Shop shopName").isEqualTo(partnerInfoDTO.getName());
        Assertions.assertThat(shop.getPhoneNumber()).as("Shop phoneNumber")
            .isEqualTo(partnerInfoDTO.getPhoneNumber());
        Assertions.assertThat(shop.getOrganizationInfos()).as("Shop organizationInfos")
            .isEqualTo(ShopConverter.extractOrganizationInfos(partnerInfoDTO.getPartnerOrgInfo()));
    }
}
