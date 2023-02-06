const indexPage = require('../../page-objects/index');

describe('tap-taxi-225: Главная. Отображения разных вариантов списков тарифов', function() {
    hermione.also.in(['chrome-grid-320', 'chrome-grid-414']);
    it('Должен отображаться полный список тарифов в Москве', async function() {
        const bro = this.browser;
        await bro.auth('taxi');

        await openPage(bro);

        await bro.pause(1000);
        await bro.hideElement(indexPage.map.container);
        await bro.assertView('listTariffPartOne', indexPage.orderForm, { ignoreElements: indexPage.addressPoint.toInput });

        await bro.swipeLeft(indexPage.tariff.tariffsSelector, 600);
        await bro.assertView('listTariffPartTwo', indexPage.orderForm, { ignoreElements: indexPage.addressPoint.toInput });
    });

    hermione.also.in(['chrome-grid-320', 'chrome-grid-414']);
    it('Должна отображаться одна карточка тарифа в Тюмени', async function() {
        const bro = this.browser;
        await bro.auth('taxi');

        // Координаты города "Тюмень"
        await bro.setGeoLocation({
            latitude: 57.1522,
            longitude: 65.5272,
            altitude: 0,
        });

        await openPage(bro);
        await bro.hideElement(indexPage.map.container);
        await bro.assertView('listTariff', indexPage.orderForm, { ignoreElements: indexPage.addressPoint.toInput });
    });

    hermione.also.in(['chrome-grid-320', 'chrome-grid-414']);
    it('Должно отображаться две карточки тарифа в Ростове-на-Дону', async function() {
        const bro = this.browser;
        await bro.auth('taxi');

        // Координаты города "Ростов-на-Дону"
        await bro.setGeoLocation({
            latitude: 47.2192,
            longitude: 39.6908,
            altitude: 0,
        });

        await openPage(bro);
        await bro.hideElement(indexPage.map.container);
        await bro.assertView('listTariff', indexPage.orderForm, { ignoreElements: indexPage.addressPoint.toInput });
    });

    hermione.also.in(['chrome-grid-320', 'chrome-grid-414']);
    it('Должно отображаться три карточки тарифа в Екатеринбурге', async function() {
        const bro = this.browser;
        await bro.auth('taxi');

        // Координаты города "Екатеринбург"
        await bro.setGeoLocation({
            latitude: 56.8519,
            longitude: 60.6122,
            altitude: 0,
        });

        await openPage(bro);
        await bro.hideElement(indexPage.map.container);
        await bro.assertView('listTariff', indexPage.orderForm, { ignoreElements: indexPage.addressPoint.toInput });
    });

    async function openPage(bro) {
        await indexPage.open(bro);
        await bro.waitForVisible(indexPage.tariff.buttonActivePrice, 10000);

        // Ожидаем полной загрузки стоимости тарифов
        await bro.pause(2000);
    }
});
