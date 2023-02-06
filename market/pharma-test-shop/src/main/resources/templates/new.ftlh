<#import "commons.ftlh" as c>

<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Create pharmacy</title>
</head>
<body>

<form method="POST" action="/pharmacies" name="pharmacy">

    <p>
        <label>*Enter shop_id: </label>
        <#if  pharmacy_error??>
            <label style="color: red; font-size: small"> ${pharmacy_error.getShopIdError()} </label>
        </#if>
    <p>
        <label>
            <input name="shopId" type="number" value="${pharmacy.getShopId()!}" />

        </label>
    <p>
        <label>*Enter campaignId: </label>
        <#if  pharmacy_error??>
            <label style="color: red; font-size: small"> ${pharmacy_error.getCampaignIdError()} </label>
        </#if>
    <p>
        <label>
            <input type="text" name="campaignId" size="150" value="${pharmacy.getCampaignId()!}" />

        </label>

    <p>
        <label>*Enter oauthToken: </label>
        <#if  pharmacy_error??>
            <label style="color: red; font-size: small"> ${pharmacy_error.getOauthTokenError()} </label>
        </#if>
    <p>
        <label>
            <input type="text" name="oauthToken" size="150" value="  ${pharmacy.getOauthToken()!}" /> >

        </label>
    <p>
        <label>*Enter oauthClientId: </label>
        <#if  pharmacy_error??>
            <label style="color: red; font-size: small"> ${pharmacy_error.getOauthClientIdError()} </label>
        </#if>
    <p>
        <label>
            <input type="text" name="oauthClientId" size="150" value="${pharmacy.getOauthClientId()!}" />

        </label>
    <p>
        <label for="salesModel">*Enter salesModel: </label>
        <label>
            <select id="salesModelSelect" name="salesModel">
                <script>
                    <#--$('#salesModelSelect').val(${pharmacy.getDeliveryTypes()});-->
                    document.getElementById("salesModelSelect").val(${pharmacy.getSalesModel()!})
                </script>
                <option>dbs</option>
                <option>fbs</option>
            </select>
        </label>

    <p>
        <label>*Enter deliveryTypes: </label>
        <label>
            <select name="deliveryTypes">
                <option>EXPRESS_DELIVERY_PICKUP</option>
                <option>EXPRESS_DELIVERY</option>
                <option>DELIVERY_PICKUP</option>
                <option>EXPRESS_PICKUP</option>
                <option>EXPRESS</option>
                <option>DELIVERY</option>
                <option>PICKUP</option>
            </select>
        </label>

    <p>
        <label>Enter paymentMethodExpress: </label>
        <#if  pharmacy_error??>
            <label style="color: red; font-size: small"> ${pharmacy_error.getPaymentMethodExpressError()} </label>
        </#if>
    <p>
        <label>
            <input type="text" name="paymentMethodExpress"
                size="150" value=" ${pharmacy.getPaymentMethodExpress()!}" />

        </label>
    <p style="font-weight: lighter; color: dimgrey; font-size: small"> Write options separated by commas [,] as they
        are
        written in Enum:
        [YANDEX,APPLE_PAY,GOOGLE_PAY]</p>

    <p>
        <label>Enter paymentMethodDelivery: </label>
        <#if  pharmacy_error??>
            <label style="color: red; font-size: small"> ${pharmacy_error.getPaymentMethodDeliveryError()} </label>
        </#if>
    <p>
        <label>
            <input type="text" name="paymentMethodDelivery"
                size="150" value="${pharmacy.getCampaignId()!}">

        </label>
    <p style="font-weight: lighter; color: dimgrey; font-size: small"> Write options separated by commas [,] as they
        are
        written in Enum:
        [YANDEX,APPLE_PAY,GOOGLE_PAY]</p>

    <p>
        <label>Enter paymentMethodPickup: </label>
        <#if  pharmacy_error??>
            <label style="color: red; font-size: small"> ${pharmacy_error.getPaymentMethodPickupError()} </label>
        </#if>
    <p>
        <label>
            <input type="text" name="paymentMethodPickup" size="150" value="${pharmacy.getPaymentMethodPickup()!}" />
        </label>
    <p style="font-weight: lighter; color: dimgrey; font-size: small"> Write options separated by commas [,] as they
        are
        written in Enum:
        [CARD_ON_DELIVERY,CASH_ON_DELIVERY]</p>

    <p>
        <label>Enter outletIds: </label>
        <#if  pharmacy_error??>
            <label style="color: red; font-size: small"> ${pharmacy_error.getOutletIdsError()} </label>
        </#if>
    <p>
        <label>
            <input type="text" name="outletIds" size="150" value="${pharmacy.getOutletIds()!}" />
        </label>
    <p style="font-weight: lighter; color: dimgrey; font-size: small"> Write options separated by commas [,]:
        [1,2,3]</p>

    <p>
        <input style="background-color: #049f04; color: honeydew; font-weight: bold;" type="submit" value="Create!" />

    <p style="font-weight: lighter; color: red; font-size: small">* Obligatory fields</p>

</form>

</body>
</html>

