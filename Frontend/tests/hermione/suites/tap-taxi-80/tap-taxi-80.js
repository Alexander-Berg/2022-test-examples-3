const assert = require('assert');
const URL = require('url');

const indexPage = require('../../page-objects/index');

// eslint-disable-next-line mocha/no-skipped-tests
describe.skip('tap-taxi-80: Главная. Переход по ссылке "Пользовательское соглашение" в шторке "Детали поездки', function() {
    it('В новой вкладке должна открыться ссылка пользовательского соглашения', async function() {
        const bro = this.browser;
        await bro.auth('taxi-80');
        await indexPage.open(bro);
        await bro.waitForVisible(indexPage.tariff.buttonActivePriceNonEmpty, 30000);

        // Ожидаем завершения анимации появления стоимости тарифа
        // await bro.pause(1000);

        await bro.click(indexPage.orderFormButton);
        await bro.waitForVisible(indexPage.orderFormHidden, 5000, true);
        await bro.waitForVisible(indexPage.orderStatus.containerHidden, 5000, true);
        await bro.waitForVisible(indexPage.orderStatus.container, 30000);
        await bro.swipeUp(indexPage.orderStatus.container, 1000);
        await bro.waitForVisible(indexPage.orderStatus.details, 5000);

        await bro.click(indexPage.orderStatus.details);
        await bro.waitForVisible(indexPage.orderStatus.detailsModalRules, 5000);

        await bro.click(indexPage.orderStatus.detailsModalRules);
        await bro.switchTabByIndex(1);

        const currentUrl = await bro.getCurrentUrl();
        const url = URL.parse(currentUrl, true);
        const urlLinkExpected = 'https://yandex.com/legal/yandexgo_termsofuse/#index__russia_ru';
        assert.strictEqual(url.href, urlLinkExpected, `По клику на ссылку открывается урл "${url.href}", а должен "${urlLinkExpected}"`);

        await bro.switchTabByIndex(0);
        await bro.waitForVisible(indexPage.orderStatus.detailsModalRules, 5000);
    });
});
