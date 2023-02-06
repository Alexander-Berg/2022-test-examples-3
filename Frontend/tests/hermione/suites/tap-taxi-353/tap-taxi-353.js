const indexPage = require('../../page-objects/index');

describe('tap-taxi-353: Главная. Отображение модалки "Бизнесу" с последующим переходам по ссылкам', function() {
    it('По нажатию на пункт "Бизнесу" должна появиться модалка со ссылками для бизнеса', async function() {
        const bro = this.browser;

        await bro.auth('taxi');

        await indexPage.open(bro);
        await bro.waitForVisible(indexPage.tariff.buttonTitleActive, 30000);
        await bro.waitForVisible(indexPage.menuModal.button, 5000);

        await indexPage.menuModal.open(bro);

        const link = await bro.findElements(indexPage.menuModal.link);
        await bro.click(link[4]);
        await bro.waitForVisible(indexPage.menuModal.subModal, 5000);
        await bro.waitModalOpen();
        await bro.hideElement(indexPage.map.container);
        await bro.assertView('subModal', indexPage.menuModal.subModal);

        await clickLinkAndAssertUrl(bro, 'Подключить поездки', 'https://taxi.yandex.ru/business/');
        await clickLinkAndAssertUrl(bro, 'Подключить доставку', 'https://taxi.yandex.ru/action/business/delivery/');
        await clickLinkAndAssertUrl(bro, 'Виджет на ваш сайт', 'https://taxi.yandex.ru/action/tools/taxiapi_touch');
    });

    async function clickLinkAndAssertUrl(bro, nameLink, urlLinkExpected) {
        await indexPage.menuModal.clickSubModelLink(bro, nameLink);

        await bro.switchTabByIndex(1);
        await bro.assertCurrentUrl(nameLink, urlLinkExpected);

        await bro.close();
        await bro.switchTabByIndex(0);
        await bro.waitForVisible(indexPage.menuModal.content, 5000);
    }
});
