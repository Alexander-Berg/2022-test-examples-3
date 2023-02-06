const indexPage = require('../../page-objects/index');

describe('tap-taxi-115: Главная. Отображение информационной модалки поверх плашки "Нет доступа к геопозиции"', function() {
    hermione.only.in('chrome-no-geolocation');
    hermione.also.in('chrome-no-geolocation');
    it('Плашки "Нет доступа к геопозиции" должна отображаться под паранджой информационной модалки', async function() {
        const bro = this.browser;

        await bro.auth('taxi');

        await indexPage.open(bro);

        await bro.waitForVisible(indexPage.topNotification, 5000);
        await bro.waitForVisible(indexPage.tariff.buttonActivePrice, 20000);
        await bro.waitForVisible(indexPage.map.interactiveModeOverlay, 30000, true);

        await indexPage.menuModal.open(bro);

        await bro.hideElement(indexPage.map.container);
        await bro.assertView('menuModal', indexPage.root);
    });
});
