const URL = require('url');
const indexPage = require('../../page-objects/index');

describe('tap-taxi-352: Главная. Отображение модалки "Водителям" с последующим переходам по ссылкам модалки', function() {
    it('По нажатию на пункт "Водителям" должна появиться модалка со ссылками для водителей', async function() {
        const bro = this.browser;

        await bro.auth('taxi');

        await indexPage.open(bro);
        await bro.waitForVisible(indexPage.tariff.buttonTitleActive, 30000);
        await bro.waitForVisible(indexPage.menuModal.button, 5000);

        await indexPage.menuModal.open(bro);

        const link = await indexPage.menuModal.searchLinkByName(bro, 'ВодителямСтать водителем или курьером. Аренда авто');
        await bro.click(link);
        await bro.waitForVisible(indexPage.menuModal.subModal, 5000);
        await bro.waitModalOpen();
        await bro.hideElement(indexPage.map.container);
        await bro.assertView('subModal', indexPage.menuModal.subModal);

        await clickLinkAndAssertUrl(bro, 'Стать водителем в парке', 'https://taxi.yandex.ru/rabota/');
        await clickLinkAndAssertUrl(bro, 'Стать самозанятым', 'https://taxi.yandex.ru/smz/');
        await clickLinkAndAssertUrl(bro, 'Стать курьером', 'https://taxi.yandex.ru/rabota/delivery/');
        await clickLinkAndAssertUrl(bro, 'Арендовать авто', 'https://taxi.yandex.ru/rabota/arenda/');
        await clickLinkAndAssertUrl(bro, 'Сайт для водителей', 'https://pro.yandex/ru-ru/moskva');

        await clickLinkAndSwitchTab(bro, 'Забрендировать авто');
        const currentUrl = await bro.getCurrentUrl();
        const url = URL.parse(currentUrl, true);
        const urlLinkExpected = 'pro.yandex';
        assert.strictEqual(url.hostname, urlLinkExpected, `По клику на ссылку "Забрендировать авто" открывается урл "${url.hostname}", а должен "${urlLinkExpected}"`);
    });

    async function clickLinkAndSwitchTab(bro, nameLink) {
        await indexPage.menuModal.clickSubModelLink(bro, nameLink);
        await bro.switchTabByIndex(1);
    }

    async function clickLinkAndAssertUrl(bro, nameLink, urlLinkExpected) {
        await clickLinkAndSwitchTab(bro, nameLink);
        await bro.assertCurrentUrl(nameLink, urlLinkExpected);

        await bro.close();
        await bro.switchTabByIndex(0);
        await bro.waitForVisible(indexPage.menuModal.content, 5000);
    }
});
