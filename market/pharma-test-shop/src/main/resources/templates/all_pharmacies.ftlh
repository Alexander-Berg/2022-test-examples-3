<#import "commons.ftlh" as c>

<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Index</title>
</head>
<body>

<fieldset>
    <div>
        <a STYLE="font-size: larger; " href="/pharmacies/new">Create new shop</a>
    </div>
</fieldset>
<p>
    <#--Шапка таблицы -->

<table id="timelineTable" border="1" class="timeline" align="center" width="90%">

    <tr>

        <th><label>${'shopId'}</label></th>
        <th><label>${'campaignId'}</label></th>
        <th><label>${'oauthToken'}</label></th>
        <th><label>${'oauthClientId'}</label></th>
        <th><label>${'deliveryTypes'}</label></th>
        <th><label>${'salesModel'}</label></th>

    </tr>

    <#list pharmacies as pharmacy>
        <tr>
            <td><label>${pharmacy.getShopId()!}</label></td>
            <td><label>${pharmacy.getCampaignId()!}</label></td>
            <td><label>${pharmacy.getOauthToken()!}</label></td>
            <td><label>${pharmacy.getOauthClientId()!}</label></td>
            <td><label>${pharmacy.getDeliveryTypes()!}</label></td>
            <td><label>${pharmacy.getSalesModel()!}</label></td>
            <td>
                <a href="/pharmacies/${pharmacy.getShopId()!}"> More information
                </a>
            </td>

        </tr>
    </#list>
</table>


<br />
<hr />


</body>
</html>

