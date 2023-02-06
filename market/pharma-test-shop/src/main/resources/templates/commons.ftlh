<#-- @ftlvariable name="pharmacy" type="ru.yandex.market.pharmatestshop.domain.pharmacy.Pharmacy" -->
<#-- @ftlvariable name="message" type="java.lang.String" -->
<#import "/spring.ftl" as spring/>
<#setting number_format="computer">
<#macro page>
    <!DOCTYPE html>
    <html lang="en">
    <head>
        <meta charset="UTF-8">
        <title>Codeforces</title>
        <link rel="icon" type="image/png" href="<@spring.url '/favicon.png'/>" />
        <link rel="stylesheet" type="text/css" href="<@spring.url '/css/normalize.css'/>">
        <link rel="stylesheet" type="text/css" href="<@spring.url '/css/style.css'/>">
        <link rel="stylesheet" type="text/css" href="<@spring.url '/css/form.css'/>">
        <link rel="stylesheet" type="text/css" href="<@spring.url '/css/datatable.css'/>">
        <link rel="stylesheet" type="text/css" href="<@spring.url '/css/article.css'/>">
        <script src="<@spring.url '/js/jquery-3.4.1.min.js'/>"></script>
        <script src="<@spring.url '/js/notify.js'/>"></script>
    </head>
    <body>
    <header>
        <nav>
            <ul>
                <li><a href="/">Home</a></li>
                <li><a href="/templates/all_pharmacies.ftlh">Show list of pharmacies</a></li>
            </ul>
            <ul>
                <li></li>
            </ul>

        </nav>
    </header>
    <div class="middle">
        <main>
            <#nested/>
        </main>
    </div>

    </body>
    <script>
        <#--noinspection JSUnresolvedFunction-->
        $(function () {
            <#if message??>
            $.notify("${message?js_string}", {
                className: 'success',
                position: 'bottom right'
            });
            </#if>
        });
    </script>
    </html>
</#macro>

<#macro error field>
    <@spring.bind field/>
    <#if spring.status.errorMessage??>
        <div class="error">${spring.status.errorMessage}</div>
    </#if>
</#macro>

<#macro pharmacy pharmacy>
<#-- @ftlvariable name="pharmacy" type="ru.yandex.market.pharmatestshop.domain.pharmacy.Pharmacy" -->
    <article>
        <div class="title"><a style="text-decoration: none"
                href="/pharmacy/${pharmacy.getShopId()?long}">${pharmacy.getShopId()}</a></div>
        <div class="information">By ${pharmacy.message}, ${pharmacy.deliveryTypes}</div>
    </article>
</#macro>
