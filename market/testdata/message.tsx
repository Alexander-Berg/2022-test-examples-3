export default {
    /**
     * Виды тестовых уведомлений с идентификатором события и данными
     */

    criticalMessage: {
        templateId: 125,
        body: `
<local-order>
    <order>
        <id>9000</id>
        <statusExpiryDate>2018-03-01 13:00</statusExpiryDate>
        <status>PENDING</status>
    </order>
</local-order>`.trim(),
    },

    regularMessage: {
        templateId: 63,
        body: `
<alert-info>
    <shop-name>(используется автотестами)</shop-name>
    <balance-usd>999999</balance-usd>
    <balance-positive>true</balance-positive>
</alert-info>`.trim(),
    },

    qualityCheckMessage: {
        // TODO: подставить данные реального оффера магазина
        templateId: 16,
        body: `
<abo-info>
    <offers>
        <offer>
            <title>Детские ботиночки из натуральной кожи Котофей 052055 - 20, синий-белый (22)</title>
            <url>http://www.kidmaster.ru/Kupit-s-Dostavkoi/00011233-Kotofei-Detskie-botinochki-052055/addmarketfeature/16736</url>
        </offer>
        <offer>
            <title>Отпариватель MIE Gianna A</title>
            <url>http://ya.ru</url>
        </offer>
    </offers>
    <problems>
        <problem>Неверно указан срок или стоимость доставки</problem>
        <problem>Неверная информация о гарантии</problem>
    </problems>
</abo-info>`.trim(),
    },

    managementMessage: {
        // TODO: прописать данные для шаблона
        templateId: 127,
        body: ''.trim(),
    },
};
