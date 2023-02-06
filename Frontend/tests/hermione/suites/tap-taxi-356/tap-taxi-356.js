const indexPage = require('../../page-objects/index');

describe('tap-taxi-356: Главная. Переход по ссылкам "Тарифы" и "Партнеры", расположенные в информационной модалке', function() {
    it('Должны открыться ссылка по нажатию на пункт "Партнёры" и "Тарифы" в городе "Москва"', async function() {
        const bro = this.browser;

        await bro.auth('taxi');

        await indexPage.open(bro);
        await bro.waitForVisible(indexPage.tariff.buttonTitleActive, 30000);
        await bro.waitForVisible(indexPage.menuModal.button, 5000);

        await indexPage.menuModal.open(bro);

        await clickLinkAndAssertUrl(bro, 'Партнёры', 'https://taxi.taxi.tst.yandex.ru/moscow/parks/');
        await clickLinkAndAssertUrl(bro, 'Тарифы', 'https://taxi.taxi.tst.yandex.ru/moscow/tariff/');
    });

    it('Должны открыться ссылка по нажатию на пункт "Партнёры" и "Тарифы" в городе "Тюмень"', async function() {
        const bro = this.browser;

        await bro.auth('taxi');

        // Координаты города "Тюмень"
        await bro.setGeoLocation({
            latitude: 57.1522,
            longitude: 65.5272,
            altitude: 0,
        });

        await indexPage.open(bro);
        await bro.waitForVisible(indexPage.tariff.buttonTitleActive, 30000);
        await bro.waitForVisible(indexPage.menuModal.button, 5000);

        await indexPage.menuModal.open(bro);

        await clickLinkAndAssertUrl(bro, 'Партнёры', 'https://taxi.taxi.tst.yandex.ru/tumen/parks/');
        await clickLinkAndAssertUrl(bro, 'Тарифы', 'https://taxi.taxi.tst.yandex.ru/tumen/tariff/');
    });

    async function clickLinkAndAssertUrl(bro, nameLink, urlLinkExpected) {
        await indexPage.menuModal.clickLink(bro, nameLink);

        await bro.switchTabByIndex(1);
        await bro.assertCurrentUrl(nameLink, urlLinkExpected);

        await bro.close();
        await bro.switchTabByIndex(0);
        await bro.pause(5000);
        await bro.waitForVisible(indexPage.menuModal.content, 5000);
    }
});
