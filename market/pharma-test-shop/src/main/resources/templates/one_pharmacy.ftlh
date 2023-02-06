<#import "commons.ftlh" as c>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Show</title>
</head>
<body>

<fieldset>
    <legend>General information</legend>
    <table id="timelineTable" class="timeline" align="left" width="100%">
        <tr>
            <td><label>${'shopId'}</label></td>
            <td><label>${pharmacy.getShopId()}</label></td>
        </tr>

        <tr>
            <td><label>${'campaignId'}</label></td>
            <td><label>${pharmacy.getCampaignId()}</label></td>
        </tr>
        <tr>
            <td><label>${'oauthToken'}</label></td>
            <td><label>${pharmacy.getOauthToken()}</label></td>
        </tr>
        <tr>
            <td><label>${'oauthClientId'}</label></td>
            <td><label>${pharmacy.getOauthClientId()}</label></td>
        </tr>
        <tr>
            <td><label>${'salesModel'}</label></td>
            <td><label>${pharmacy.getSalesModel()}</label></td>
        </tr>

        <tr>
            <td><label>${'deliveryTypes'}</label></td>
            <td><label>${pharmacy.getDeliveryTypes()}</label></td>
        </tr>
        <tr>
            <td><label>${'paymentMethodExpress'}</label></td>
            <td><label>${pharmacy.getPaymentMethodExpress()}</label></td>
        </tr>
        <tr>
            <td><label>${'paymentMethodDelivery '}</label></td>
            <td><label>${pharmacy.getPaymentMethodDelivery()}</label></td>
        </tr>
        <tr>
            <td><label>${'paymentMethodPickup '}</label></td>
            <td><label>${pharmacy.getPaymentMethodPickup()}</label></td>
        </tr>

        <tr>
            <td><label>${'outletIds '}</label></td>
            <td><label>${pharmacy.getOutletIds()}</label></td>
        </tr>

    </table>


</fieldset>

<p>
    <a  style="background-color: rgba(232,255,246,0.35); color: #009a00;font-weight: bold; font-size: larger"  href="/pharmacies/${pharmacy.getShopId()}/edit"> Edit </a>

<form method="POST" action="/pharmacies/delete/${pharmacy.getShopId()}">
    <input style="background-color: #d26a01; color: honeydew; font-weight: bold;" type="submit" value="Delete" />
</form>
</body>
</html>
