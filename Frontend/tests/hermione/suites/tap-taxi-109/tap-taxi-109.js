const indexPage = require('../../page-objects/index');

describe('tap-taxi-109:  Главная. Переход по ссылкам в модалке "Информация о сервисе', function() {
    it('Должна открываться ссылки по тапу на пункты информационной модалки', async function() {
        const bro = this.browser;

        await bro.auth('taxi');

        await indexPage.open(bro);
        await bro.waitForVisible(indexPage.tariff.buttonTitleActive, 30000);
        await bro.waitForVisible(indexPage.menuModal.button, 5000);

        await indexPage.menuModal.open(bro);

        await bro.click(indexPage.menuModal.promo);
        await assertLink(bro, 'Установите Яндекс Go', 'https://go.yandex/ru_ru/?from=turbomenu');

        await clickLinkAndAssertUrl(bro, 'Обратиться в поддержку', 'https://ya-authproxy.taxi.tst.yandex.ru/webview/yaproxy/help/ru_int/taxi?isIframeSupported=true&id=86b3ad46098346a78724fb7e6861e341&orderid=');
        await clickLinkAndAssertUrl(bro, 'Узнать о безопасности', 'https://go.yandex/ru_ru/lp/safety/rider-safety');
        await clickLinkAndAssertUrl(bro, 'Тарифы', 'https://taxi.yandex.ru/moscow/tariff/');
        await clickLinkAndAssertUrl(bro, 'Партнёры', 'https://taxi.yandex.ru/moscow/parks/');
        await clickLinkAndAssertUrl(bro, 'Пользовательское соглашение', 'https://yandex.ru/legal/rules/');
        await clickLinkAndAssertUrl(bro, 'Политика конфиденциальности', 'https://yandex.ru/legal/confidential/');
    });

    async function clickLinkAndAssertUrl(bro, nameLink, urlLinkExpected) {
        await indexPage.menuModal.clickLink(bro, nameLink);
        await assertLink(bro, nameLink, urlLinkExpected);
    }

    async function assertLink(bro, nameLink, urlLinkExpected) {
        await bro.switchTabByIndex(1);
        await bro.assertCurrentUrl(nameLink, urlLinkExpected);

        await bro.close();
        await bro.switchTabByIndex(0);
        await bro.waitForVisible(indexPage.menuModal.content, 5000);
    }
});
