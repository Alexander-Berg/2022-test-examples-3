const indexPage = require('../../page-objects/index');

describe('tap-taxi-272: Выбор адреса. Переход к экрану "Выбор адреса на карте" с главного экрана с последующим переход к шторке выбора маршрута по тапу на поля с адресом "Откуда/Куда', function() {
    it('Должен произойти возврат к главному экрану после открытия экрана "Выбор адреса на карте" точки "Откуда" с последующим переходом к саджесту', async function() {
        const bro = this.browser;
        await openPageAndSelectAddressTo(bro);

        await bro.click(indexPage.map.addressFromPlacemark);
        await bro.waitForVisible(indexPage.addressInteractiveScreen.container, 5000);
        await bro.waitForVisible(indexPage.addressInteractiveScreen.from, 5000);

        await bro.click(indexPage.addressInteractiveScreen.from);
        await bro.waitForVisible(indexPage.addressSuggest.container, 5000);

        await bro.back();
        await bro.waitForVisible(indexPage.addressSuggest.container, 5000, true);
        await bro.waitForVisible(indexPage.addressInteractiveScreen.container, 5000, true);
        await bro.waitForVisible(indexPage.addressPoint.porchButton, 5000);
    });

    it('Должен произойти возврат к главному экрану после открытия экрана "Выбор адреса на карте" точки "Куда" с последующим переходом к саджесту', async function() {
        const bro = this.browser;
        await openPageAndSelectAddressTo(bro);

        await bro.click(indexPage.map.addressToPlacemark);
        await bro.waitForVisible(indexPage.addressInteractiveScreen.container, 5000);
        await bro.waitForVisible(indexPage.addressInteractiveScreen.from, 5000);

        await bro.click(indexPage.addressInteractiveScreen.to);
        await bro.waitForVisible(indexPage.addressSuggest.container, 5000);

        await bro.back();
        await bro.waitForVisible(indexPage.addressSuggest.container, 5000, true);
        await bro.waitForVisible(indexPage.addressInteractiveScreen.container, 5000, true);
        await bro.waitForVisible(indexPage.addressPoint.porchButton, 5000);
    });

    async function openPageAndSelectAddressTo(bro) {
        await bro.auth('taxi');
        await indexPage.open(bro);
        await indexPage.addressPoint.clickPositionFirst(bro);
        await bro.waitForVisible(indexPage.map.addressToPlacemark, 5000);
    }
});
