package ru.yandex.market.mbi.partner.registration.util;

import java.util.Optional;

import ru.yandex.market.mbi.open.api.client.model.PartnerNotificationContact;
import ru.yandex.market.mbi.open.api.client.model.PartnerPlacementType;
import ru.yandex.market.mbi.open.api.client.model.PartnerRegistrationRequest;

public class ModelConversions {

    private ModelConversions() {
    }

    public static ru.yandex.mj.generated.server.model.PartnerRegistrationRequest from(
            ru.yandex.mj.generated.client.mbi_partner_registration.model
                    .PartnerRegistrationRequest request
    ) {
        return new ru.yandex.mj.generated.server.model.PartnerRegistrationRequest()
                .businessId(request.getBusinessId())
                .partnerName(request.getPartnerName())
                .businessName(request.getBusinessName())
                .regionId(request.getRegionId())
                .domain(request.getDomain())
                .partnerPlacementType(from(request.getPartnerPlacementType()));
    }

    public static PartnerRegistrationRequest fromRegistrationClient(
        ru.yandex.mj.generated.client.mbi_partner_registration.model
            .PartnerRegistrationRequest request,
        long uid
    ) {
        return new PartnerRegistrationRequest()
                .businessId(request.getBusinessId())
                .partnerName(request.getPartnerName())
                .businessName(request.getBusinessName())
                .regionId(request.getRegionId())
                .domain(request.getDomain())
                .shopOwnerId(Optional.ofNullable(request.getShopOwnerId()).orElse(uid))
                .partnerPlacementType(fromRegistrationClient(request.getPartnerPlacementType()))
                .partnerNotificationContact(fromNotificationContactClient(request.getPartnerNotificationContact()))
                .isSelfEmployed(Optional.ofNullable(request.getIsSelfEmployed()).orElse(false))
                .isB2BSeller(Optional.ofNullable(request.getIsB2BSeller()).orElse(false));
    }

    private static PartnerNotificationContact fromNotificationContactClient(
            ru.yandex.mj.generated.client.mbi_partner_registration.model
                    .PartnerNotificationContact contact
    ) {
        if (contact == null) {
            return null;
        }
        return new PartnerNotificationContact()
                .firstName(contact.getFirstName())
                .lastName(contact.getLastName())
                .phone(contact.getPhone())
                .email(contact.getEmail());
    }

    public static ru.yandex.mj.generated.server.model.PartnerPlacementType from(
            ru.yandex.mj.generated.client.mbi_partner_registration.model
                    .PartnerPlacementType placementType
    ) {
        switch (placementType) {
            case FBY:
                return ru.yandex.mj.generated.server.model.PartnerPlacementType.FBY;
            case FBY_PLUS:
                return ru.yandex.mj.generated.server.model.PartnerPlacementType.FBY_PLUS;
            case FBS:
                return ru.yandex.mj.generated.server.model.PartnerPlacementType.FBS;
            case DBS:
                return ru.yandex.mj.generated.server.model.PartnerPlacementType.DBS;
            case FOREIGN_SHOP:
                return ru.yandex.mj.generated.server.model.PartnerPlacementType.FOREIGN_SHOP;
            default:
                throw new UnsupportedOperationException();
        }
    }

    public static PartnerPlacementType fromRegistrationClient(
            ru.yandex.mj.generated.client.mbi_partner_registration.model
                    .PartnerPlacementType placementType
    ) {
        switch (placementType) {
            case FBY:
                return PartnerPlacementType.FBY;
            case FBY_PLUS:
                return PartnerPlacementType.FBY_PLUS;
            case FBS:
                return PartnerPlacementType.FBS;
            case DBS:
                return PartnerPlacementType.DBS;
            case FOREIGN_SHOP:
                return PartnerPlacementType.FOREIGN_SHOP;
            default:
                throw new UnsupportedOperationException();
        }
    }
}
