const indexPage = require('../../page-objects/index');

describe('tap-taxi-354: Главная. Отображение модалки "Партнёрам" с последующим переходам по ссылкам', function() {
    it('По нажатию на пункт "Партнёрам" должна появиться модалка со ссылками для партнеров', async function() {
        const bro = this.browser;

        await bro.auth('taxi');

        await indexPage.open(bro);
        await bro.waitForVisible(indexPage.tariff.buttonTitleActive, 30000);
        await bro.waitForVisible(indexPage.menuModal.button, 5000);

        await indexPage.menuModal.open(bro);

        const link = await indexPage.menuModal.searchLinkByName(bro, 'ПартнёрамПодключить или расширить таксопарк');
        await bro.click(link);
        await bro.waitForVisible(indexPage.menuModal.subModal, 5000);
        await bro.waitModalOpen();
        await bro.hideElement(indexPage.map.container);
        await bro.assertView('subModal', indexPage.menuModal.subModal);

        await clickLinkAndAssertUrl(bro, 'Подключить парк к Яндекс.Такси', 'https://taxi.yandex.ru/partnership/');
        await clickLinkAndAssertUrl(bro, 'Расширить таксопарк', 'https://pro.yandex/lp/ru-ru/mypark/?from=turbo');
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
