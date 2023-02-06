<html>
<head>
    <title>Результаты сравнения цен </title>
    <meta charset="UTF-8">
</head>
<body>
<div style="padding-left: 20px; padding-top: 20px;padding-right: 20px">
    <table style="border-collapse: collapse !important;max-width: 700px; border-radius: 25px">
        <thead>
        <tr>
            <th style="text-align: center;border: 2px solid #ddd">#</th>
            <th style="text-align: center;border: 2px solid #ddd">Info</th>
            <th style="text-align: center; border: 2px solid #ddd">Yandex.Travel</th>
            <th style="text-align: center; border: 2px solid #ddd">sletat.ru</th>
        </tr>
        </thead>

        <tbody style="border-top: 2px solid #ddd">
        <#list results as summary>

        <#if summary.success == 0>
        <tr style="background-color: #c4ffdd">
        <#elseif summary.success == 1>
        <tr style="background-color: #ffc4c4">
        <#elseif summary.success == 2>
        <tr style="background-color: antiquewhite">
        </#if>

            <th>${summary?index + 1}</th>
            <th style="padding:10px;border: 2px solid #ddd">
                From: ${summary.information.fromCity} <br>
                <#if summary.information.resort??>
                To: ${summary.information.resort} (${summary.information.toCountry}) <br>
                <#else>
                To: ${summary.information.toCountry} <br>
                </#if>
                <#if summary.information.hotel??>
                Hotel: ${summary.information.hotel} <br>
                </#if>
                From Date: ${summary.information.fromDate} <br>
                To Date: ${summary.information.toDate} <br>
                Travellers: ${summary.information.adults} adults<#if summary.information.childs?size != 0> with children</#if> <br>
                Nights: from ${summary.information.minNights} to ${summary.information.maxNights}
            </th>
            <th style="padding:10px;border: 2px solid #ddd">
                Price: ${summary.yaTour.price} <br>
                Operator: ${summary.yaTour.operator} <br>
                URL: <a target="_blank" href="${summary.yaTour.url}">url</a> <br>
                Screen: <a target="_blank" href="${summary.yaTour.screenUrl}">screenshot</a>
            </th>
            <th style="padding:10px;border: 2px solid #ddd">
                Price: ${summary.slTour.price} <br>
                Operator: ${summary.slTour.operator} <br>
                URL: <a target="_blank" href="${summary.slTour.url}">url</a> <br>
                Screen: <a target="_blank" href="${summary.slTour.screenUrl}">screenshot</a>
            </th>
        </tr>
        </#list>
        </tbody>
    </table>
</div>
</body>
</html>
