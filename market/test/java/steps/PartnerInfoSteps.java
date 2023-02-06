package steps;

import ru.yandex.market.core.campaign.model.CampaignType;
import ru.yandex.market.core.manager.dto.ManagerInfoDTO;
import ru.yandex.market.core.orginfo.model.OrganizationInfoSource;
import ru.yandex.market.core.orginfo.model.OrganizationType;
import ru.yandex.market.mbi.api.client.entity.partner.PartnerInfoDTO;
import ru.yandex.market.mbi.api.client.entity.partner.PartnerOrgInfoDTO;

public class PartnerInfoSteps {
    private static final String PHONE = "+71234567890";

    private PartnerInfoSteps() {
    }

    public static PartnerInfoDTO getPartnerInfoDTO(long id) {
        return getPartnerInfoDTO(id, PHONE);
    }

    public static PartnerInfoDTO getEmptyOrgInfoPartnerInfoDTO(long id) {
        return new PartnerInfoDTO(
            id,
            0L,
            CampaignType.SHOP,
            "dummy_shop",
            "domain",
            PHONE,
            "City, Street, Building",
            null,
            false,
            null
        );
    }

    public static PartnerInfoDTO getPartnerInfoDTO(long id, String phoneNumber) {
        return new PartnerInfoDTO(
            id,
            0L,
            CampaignType.SHOP,
            "dummy_shop",
            "domain",
            phoneNumber,
            "City, Street, Building",
            getPartnerOrgInfo(),
            false,
            getManagerInfoDTO(id)
        );
    }

    private static PartnerOrgInfoDTO getPartnerOrgInfo() {
        return new PartnerOrgInfoDTO(
            OrganizationType.OAO,
            "dummy_co",
            "ogrn",
            "fact addddr",
            "juric adddr",
            OrganizationInfoSource.PARTNER_INTERFACE,
            "reg num",
            "info_url"
        );
    }

    private static ManagerInfoDTO getManagerInfoDTO(long id) {
        return new ManagerInfoDTO(id, "FullName", "e@mail.com", "+71234567890", "staff-e@mail.com");
    }
}
