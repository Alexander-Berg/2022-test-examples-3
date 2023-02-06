package ru.yandex.market.partner.mvc.controller.business;

import java.util.List;
import java.util.Set;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.core.contact.InnerRole;
import ru.yandex.market.partner.mvc.controller.business.model.BusinessContactRequest;
import ru.yandex.market.partner.test.context.FunctionalTest;

/**
 * Тесты для {@link BusinessContactHelper}
 */
public class BusinessContactHelperTest extends FunctionalTest {

    @Test
    void expandAllCampaignsRolesTest() {
        BusinessContactRequest businessContactRequest = new BusinessContactRequest();
        businessContactRequest.setCampaignRoles(List.of(
                new BusinessContactRequest.CampaignRolesDto(5L, Set.of(InnerRole.SHOP_TECHNICAL))));
        businessContactRequest.setAllCampaignRoles(Set.of(InnerRole.SHOP_ADMIN));

        List<BusinessContactRequest.CampaignRolesDto> expected = List.of(
                new BusinessContactRequest.CampaignRolesDto(5L, Set.of(InnerRole.SHOP_TECHNICAL, InnerRole.SHOP_ADMIN)),
                new BusinessContactRequest.CampaignRolesDto(6L, Set.of(InnerRole.SHOP_ADMIN)),
                new BusinessContactRequest.CampaignRolesDto(7L, Set.of(InnerRole.SHOP_ADMIN))
        );

        BusinessContactHelper.expandAllCampaignsRoles(Set.of(5L, 6L, 7L), businessContactRequest);
        Assertions.assertThat(businessContactRequest.getAllCampaignRoles()).isEmpty();
        Assertions.assertThat(businessContactRequest.getCampaignRoles()).hasSameElementsAs(expected);
    }
}
