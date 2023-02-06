const indexPage = require('../../page-objects/index');

describe('tap-taxi-355: Главная. Закрытие модалок, открывающихся через модалку "Информация о сервисе"', function() {
    it('Модалка должна закрыться по нажатию на системную кнопку back', async function() {
        const bro = this.browser;
        await openPageAndSuMenuModal(bro);

        await bro.back();
        await bro.waitForVisible(indexPage.menuModal.subModal, 5000, true);
        await bro.waitForVisible(indexPage.menuModal.content, 5000);
    });

    it('Модалка должна закрыться смахиванием вниз', async function() {
        const bro = this.browser;
        await openPageAndSuMenuModal(bro);

        await bro.swipeDown(indexPage.menuModal.subModal, 1000);
        await bro.waitForVisible(indexPage.menuModal.subModal, 5000, true);
        await bro.waitForVisible(indexPage.menuModal.content, 5000);
    });

    it('Модалка должна закрыться тапом в область вне модалки', async function() {
        const bro = this.browser;
        await openPageAndSuMenuModal(bro);

        await bro.leftClick(indexPage.menuModal.content, 1000);
        await bro.waitForVisible(indexPage.menuModal.subModal, 5000, true);
        await bro.waitForVisible(indexPage.menuModal.content, 5000);
    });

    async function openPageAndSuMenuModal(bro) {
        await bro.auth('taxi');

        await indexPage.open(bro);
        await bro.waitForVisible(indexPage.tariff.buttonTitleActive, 30000);
        await bro.waitForVisible(indexPage.menuModal.button, 5000);

        await indexPage.menuModal.open(bro);
        const link = await indexPage.menuModal.searchLinkByName(bro, 'ПартнёрамПодключить или расширить таксопарк');
        await bro.click(link);
        await bro.waitForVisible(indexPage.menuModal.subModal, 5000);
    }
});
