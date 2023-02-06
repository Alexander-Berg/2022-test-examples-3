package ru.yandex.market.pharmatestshop.domain.pharmacy;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import lombok.Data;

import static ru.yandex.market.pharmatestshop.util.StringUtils.isNaturalNumberFormat;

@Data
public class PharmacyErrors {

    private static final String OUTLET_IDS_ERROR = "Outlet ids must be natural number and non-empty";
    //
    private boolean hasErrorsFlag = false;
    private String salesModelError = "Sales model could be only fbs/dbs";
    private String deliveryTypesError = "Choose ONE delivery type from the list below";
    private String paymentMethodExpressError = "Choose payment options from the list below";
    private String paymentMethodDeliveryError = "Choose payment options from the list below";
    private String paymentMethodPickupError = "Choose payment options from the list below";
    private String shopIdError = "shopId must not be empty";
    private String campaignIdError = "campaignId must not be empty";
    private String oauthTokenError = "oauthToken must not be empty";
    private String oauthClientIdError = "oauthClientId must not be empty";
    private String outletIdsError = OUTLET_IDS_ERROR;


    private void checkPharmacy(Pharmacy pharmacy) {
        if(pharmacy.getShopId()==null){
            shopIdError="shopId must not be empty";
            hasErrorsFlag=true;
        }
       if(pharmacy.getOauthClientId().isEmpty()){
           oauthClientIdError = "oauthClientId must not be empty";
           hasErrorsFlag=true;
       }
        if(pharmacy.getOauthToken().isEmpty()){
            oauthTokenError = "oauthToken must not be empty";
            hasErrorsFlag=true;
        }
        if(pharmacy.getCampaignId().isEmpty()){
            campaignIdError = "campaignId must not be empty";
            hasErrorsFlag=true;
        }


        switch (pharmacy.getSalesModel()) {
            case "dbs":
            case "fbs":
                break;
            default:
                hasErrorsFlag = true;
                salesModelError = "Only 'fbs'/'dbs' is possible";
                break;
        }

        switch (pharmacy.getDeliveryTypes()) {
            case EXPRESS_DELIVERY_PICKUP:
                hasErrorsExpress(pharmacy);
                hasErrorsDelivery(pharmacy);
                hasErrorsPickup(pharmacy);
                break;
            case EXPRESS_DELIVERY:
                hasErrorsExpress(pharmacy);
                hasErrorsDelivery(pharmacy);
                break;
            case DELIVERY_PICKUP:
                hasErrorsDelivery(pharmacy);
                hasErrorsPickup(pharmacy);
                break;
            case EXPRESS_PICKUP:
                hasErrorsExpress(pharmacy);
                hasErrorsPickup(pharmacy);
                break;
            case EXPRESS:
                hasErrorsExpress(pharmacy);
                break;
            case PICKUP:
                hasErrorsPickup(pharmacy);
                break;
            case DELIVERY:
                hasErrorsDelivery(pharmacy);
                break;
            default:
                deliveryTypesError = "Choose ONE delivery type from the list below";
                hasErrorsFlag = true;
                break;
        }

        hasErrorsOutlets(pharmacy);

    }

    private void hasErrorsDelivery(Pharmacy pharmacy) {

        List<String> paymentMethods =
                Arrays.stream(pharmacy.getPaymentMethodDelivery().split(",")).collect(Collectors.toList());
        for (var method : paymentMethods) {
            switch (method) {
                case "YANDEX":
                case "APPLE_PAY":
                case "GOOGLE_PAY":
                    break;
                default:
                    paymentMethodDeliveryError = "Choose payment options from the list below";
                    hasErrorsFlag = true;
                    return;
            }
        }

    }

    private void hasErrorsExpress(Pharmacy pharmacy) {

        List<String> paymentMethods =
                Arrays.stream(pharmacy.getPaymentMethodExpress().split(",")).collect(Collectors.toList());
        for (var method : paymentMethods) {
            switch (method) {
                case "YANDEX":
                case "APPLE_PAY":
                case "GOOGLE_PAY":
                    break;
                default:
                    paymentMethodExpressError = "Choose payment options from the list below";
                    hasErrorsFlag = true;
                    return;
            }
        }

    }

    private void hasErrorsPickup(Pharmacy pharmacy) {

        List<String> paymentMethods =
                Arrays.stream(pharmacy.getPaymentMethodPickup().split(",")).collect(Collectors.toList());
        for (var method : paymentMethods) {
            switch (method) {
                case "YANDEX":
                case "CARD_ON_DELIVERY":
                case "CASH_ON_DELIVERY":
                    break;
                default:
                    paymentMethodPickupError = "Choose payment options from the list below";
                    hasErrorsFlag = true;
                    return;
            }

        }

    }

    private void hasErrorsOutlets(Pharmacy pharmacy) {
        if (!isNaturalNumberFormat(pharmacy.getOutletIds())) {
            hasErrorsFlag = true;
            outletIdsError = OUTLET_IDS_ERROR;
        }
    }

    private void cleanErrors() {
        hasErrorsFlag = false;
        salesModelError = "";
        deliveryTypesError = "";
        paymentMethodExpressError = "";
        paymentMethodDeliveryError = "";
        paymentMethodPickupError = "";
        oauthClientIdError = "";
        oauthTokenError = "";
        campaignIdError = "";
        shopIdError = "";
        outletIdsError = "";
    }

    public boolean hasErrors(Pharmacy pharmacy) {
        cleanErrors();
        checkPharmacy(pharmacy);
        return hasErrorsFlag;
    }

}
