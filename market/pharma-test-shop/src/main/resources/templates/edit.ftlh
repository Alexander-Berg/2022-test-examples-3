<#import "commons.ftlh" as c>

<!DOCTYPE html >
<html lang="en" xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <title>Update pharmacy</title>
</head>
<body>


<form th:object="${pharmacy}" name="pharmacy" method="POST" action="/pharmacies/${pharmacy.getShopId()}">

    <fieldset>
        <legend>Edit information</legend>
        <p>
            <label>shopId: </label>
            <label> ${pharmacy.getShopId()}</label>

        <p>
            <label>*Enter campaignId: </label>
        <p>
            <label>
                <input type="text" name="campaignId" value="${pharmacy.getCampaignId()}" size="150" />
            </label>
        <p>
            <label>*Enter oauthToken: </label>
        <p>
            <label>
                <input type="text" name="oauthToken" value="${pharmacy.getOauthToken()}" size="150" />
            </label>
        <p>
            <label>*Enter oauthClientId: </label>
        <p>
            <label>
                <input type="text" name="oauthClientId" value="${pharmacy.getOauthClientId()}" size="150" />
            </label>
        <p>
            <label for="salesModel">*Enter salesModel: </label>

            <#if  pharmacy_error?? >
                <label style="color: red; font-size: small"> ${pharmacy_error.getSalesModelError()} </label>
            </#if>

        <p>
            <label>
                <input name="salesModel" value="${pharmacy.getSalesModel()}" size="150" />
            </label>
        <p style="font-weight: lighter; color: dimgrey; font-size: small">
            Write one option from enum:
            [dbs,fbs]

        </p>


        <p>
            <label>*Enter deliveryTypes: </label>

            <#if  pharmacy_error?? >
                <label style="color: red; font-size: small"> ${pharmacy_error.getDeliveryTypesError()} </label>
            </#if>

            <#--    <script type="text/javascript">-->
            <#--        $('#deliveryTypesSelect').val(${pharmacy.getDeliveryTypes().toString()});-->
            <#--    </script>-->
        <p>
            <label>
                <input type="text" name="deliveryTypes" value="${pharmacy.getDeliveryTypes()! }"
                    size="150" />
            </label>

        <p style="font-weight: lighter; color: dimgrey; font-size: small">
            Write ONE option from enum:
            [EXPRESS_DELIVERY_PICKUP,
            EXPRESS_DELIVERY,
            DELIVERY_PICKUP,
            EXPRESS_PICKUP,
            EXPRESS,
            DELIVERY,
            PICKUP]
        </p>


        <p>
            <label>Enter paymentMethodExpress: </label>
            <#if  pharmacy_error??>
                <label style="color: red; font-size: small"> ${pharmacy_error.getPaymentMethodExpressError()} </label>
            </#if>
        <p>
            <label>
                <input type="text" name="paymentMethodExpress" value="${pharmacy.getPaymentMethodExpress()}"
                    size="150" />
            </label>


        <p style="font-weight: lighter; color: dimgrey; font-size: small">
            Write options separated by commas [,] as they
            are
            written in Enum:
            [YANDEX,APPLE_PAY,GOOGLE_PAY]

        </p>


        <p>
            <label>Enter paymentMethodDelivery: </label>
            <#if  pharmacy_error?? >
                <label style="color: red; font-size: small"> ${pharmacy_error.getPaymentMethodDeliveryError()} </label>
            </#if>
        <p>
            <label>
                <input type="text" name="paymentMethodDelivery" value="${pharmacy.getPaymentMethodDelivery()}"
                    size="150" />
            </label>

        <p style=" font-weight: lighter; color: dimgrey; font-size: small">
            Write options separated by commas [,]
            as they
            are
            written in Enum:
            [YANDEX,APPLE_PAY,GOOGLE_PAY]

        </p>


        <p>
            <label>Enter paymentMethodPickup: </label>
            <#if  pharmacy_error?? >
                <label style="color: red; font-size: small"> ${pharmacy_error.getPaymentMethodPickupError()} </label>
            </#if>
        <p>
            <label>
                <input type="text" name="paymentMethodPickup" value="${pharmacy.getPaymentMethodPickup()}" size="150" />
            </label>

        <p style="font-weight: lighter; color: dimgrey; font-size: small">
            Write options separated by commas [,] as they
            are
            written in Enum:
            [CARD_ON_DELIVERY,CASH_ON_DELIVERY]

        </p>
        <p>
            <label>Enter outlet ids: </label>
            <#if  pharmacy_error?? >
                <label style="color: red; font-size: small"> ${pharmacy_error.getOutletIdsError()} </label>
            </#if>
        <p>
            <label>
                <input type="text" name="outletIds" value="${pharmacy.getOutletIds()}" size="150" />
            </label>

        <p style="font-weight: lighter; color: dimgrey; font-size: small">
            Write options separated by commas [,] as they:
            [1,1232, 5234]

        </p>
        <p>
            <input style="background-color: #00bfb7; color: honeydew; font-weight: bold;" type="submit" value="Update!" />
    </fieldset>
    <p style="font-weight: lighter; color: red; font-size: small">* Obligatory fields</p>
</form>

</body>
</html>
