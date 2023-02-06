package ru.yandex.travel.hotels.administrator;

import java.util.List;
import java.util.UUID;

import ru.yandex.travel.hotels.administrator.entity.HotelConnection;
import ru.yandex.travel.hotels.administrator.entity.HotelConnectionUpdate;
import ru.yandex.travel.hotels.administrator.entity.LegalDetails;
import ru.yandex.travel.hotels.administrator.workflow.proto.EHotelConnectionState;
import ru.yandex.travel.hotels.administrator.workflow.proto.EHotelConnectionUpdateState;
import ru.yandex.travel.hotels.administrator.workflow.proto.ELegalDetailsState;
import ru.yandex.travel.hotels.common.partners.travelline.model.ContactType;
import ru.yandex.travel.hotels.common.partners.travelline.model.HotelDetails;
import ru.yandex.travel.hotels.common.partners.travelline.model.HotelDetailsAddress;
import ru.yandex.travel.hotels.common.partners.travelline.model.HotelDetailsResponse;
import ru.yandex.travel.hotels.common.partners.travelline.model.HotelOfferStatus;
import ru.yandex.travel.hotels.common.partners.travelline.model.HotelRef;
import ru.yandex.travel.hotels.common.partners.travelline.model.TaxType;
import ru.yandex.travel.hotels.proto.EPartnerId;

public class EntityCreatingUtils {

    public static final String INN = "INN123456789";
    public static final String NEW_INN = "NEW_INN12345";
    public static final String KPP = "kpp";
    public static final String BIC = "bic";
    public static final String HOTEL_CODE = "hotel_code";
    public static final String EMAIL = "mail@example.com";
    public static final String LEGAL_NAME = "Legal Name";
    public static final String NEW_LEGAL_NAME = "New Legal Name";
    public static final String FULL_LEGAL_NAME = "Full Legal Name";
    public static final String FACT_POST_CODE = "Fact Post Code";
    public static final String POST_CODE = "111111";
    public static final String FACT_ADDRESS = "Address Line";
    public static final String NEW_ADDRESS = "new address";
    public static final String PHONE = "74959999999";
    public static final String NEW_PHONE = "7495111111";
    public static final String CURRENT_ACCOUNT = "account";
    public static final String NEW_CURRENT_ACCOUNT = "new account";
    public static final String ADDRESS = "some address";

    public static HotelConnection hotelConnection() {
        HotelConnection connection = new HotelConnection();
        connection.setId(UUID.randomUUID());
        connection.setHotelCode(HOTEL_CODE);
        connection.setPartnerId(EPartnerId.PI_TRAVELLINE);
        connection.setState(EHotelConnectionState.CS_NEW);
        connection.setAccountantEmail(EMAIL);
        return connection;
    }

    public static HotelConnectionUpdate.HotelConnectionUpdateBuilder hotelConnectionUpdate() {
        return HotelConnectionUpdate.builder()
                .id(UUID.randomUUID())
                .state(EHotelConnectionUpdateState.HCU_NEW)
                .partnerId(EPartnerId.PI_TRAVELLINE)
                .hotelCode(HOTEL_CODE)
                .accountantEmail(EMAIL)
                .inn(INN)
                .kpp(KPP)
                .bic(BIC)
                .paymentAccount(CURRENT_ACCOUNT)
                .legalName(LEGAL_NAME)
                .fullLegalName(FULL_LEGAL_NAME)
                .legalPostCode(POST_CODE)
                .legalAddress(POST_CODE + ", " + ADDRESS)
                .legalAddressUnified(true)
                .legalPhone(PHONE)
                .postCode(POST_CODE)
                .postAddress(POST_CODE + ", " + ADDRESS);
    }

    public static HotelDetailsResponse hotelDetailsResponse(boolean newInn, boolean hasMinorChange,
                                                            boolean hasMajorChange, boolean newRequisites) {
        HotelDetails details = HotelDetails.builder()
                .offerStatus(HotelOfferStatus.ACCEPTED)
                .addressDetails(HotelDetailsAddress.builder()
                        .postalCode(FACT_POST_CODE)
                        .fullAddress(POST_CODE + ", " + FACT_ADDRESS)
                        .cityName("City Name")
                        .build())
                .bankAccountDetails(HotelDetails.BankAccountDetails.builder()
                        .inn(newInn ? NEW_INN : INN)
                        .kpp(KPP)
                        .bic(BIC)
                        .currentAccount(newRequisites ? NEW_CURRENT_ACCOUNT : CURRENT_ACCOUNT)
                        .personLegalName(LEGAL_NAME)
                        .branchName(FULL_LEGAL_NAME)
                        .personLegalName(hasMajorChange ? NEW_LEGAL_NAME : LEGAL_NAME)
                        .addressDetails(HotelDetailsAddress.builder()
                                .postalCode(POST_CODE)
                                .fullAddress(hasMajorChange ? POST_CODE + ", " + NEW_ADDRESS : POST_CODE + ", " + ADDRESS)
                                .cityName("City Name")
                                .build())
                        .phone(hasMinorChange ? NEW_PHONE : PHONE)
                        .tax(TaxType.COMMON)
                        .build())
                .contactInfo(List.of(
                        HotelDetails.HotelContactInfo.builder()
                                .contactType(ContactType.ACCOUNTANT)
                                .email(EMAIL)
                                .build(),
                        HotelDetails.HotelContactInfo.builder()
                                .contactType(ContactType.CONTRACT)
                                .name("Name")
                                .position("Position")
                                .phone("Phone")
                                .email("Contract Email")
                                .build(),
                        HotelDetails.HotelContactInfo.builder()
                                .contactType(ContactType.RESERVATION)
                                .phone("ReservationPhone")
                                .build()
                ))
                .hotelRef(HotelRef.builder().code(HOTEL_CODE).build())
                .build();
        return new HotelDetailsResponse(details);
    }

    public static LegalDetails legalDetails(boolean newInn) {
        return legalDetails(newInn, true);
    }

    public static LegalDetails legalDetails(boolean newInn, boolean managedByHotelsAdministrator) {
        return LegalDetails.builder()
                .id(UUID.randomUUID())
                .inn(newInn ? NEW_INN : INN)
                .kpp(KPP)
                .bic(BIC)
                .paymentAccount(CURRENT_ACCOUNT)
                .legalName(LEGAL_NAME)
                .fullLegalName(FULL_LEGAL_NAME)
                .legalPostCode(POST_CODE)
                .legalAddress(POST_CODE + ", " + ADDRESS)
                .legalAddressUnified(true)
                .postCode(POST_CODE)
                .postAddress(POST_CODE + ", " + ADDRESS)
                .phone(PHONE)
                .state(ELegalDetailsState.DS_NEW)
                .partnerId(EPartnerId.PI_TRAVELLINE)
                .managedByAdministrator(managedByHotelsAdministrator)
                .build();
    }
}
