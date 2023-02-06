const indexPage = require('../../page-objects/index');

describe('tap-taxi-111: Главная. Закрытие модалки "Информация о сервисе"', function() {
    it('Модалка должна закрыться по нажатию на системную кнопку back', async function() {
        const bro = this.browser;
        await openPageAndmenuModal(bro);

        await bro.back();
        await bro.waitForVisible(indexPage.menuModal.content, 5000, true);
        await bro.waitForVisible(indexPage.menuModal.button, 5000);
    });

    it('Модалка должна закрыться смахиванием вниз', async function() {
        const bro = this.browser;
        await openPageAndmenuModal(bro);

        await bro.swipeDown(indexPage.menuModal.content, 1000);
        await bro.waitForVisible(indexPage.menuModal.content, 5000, true);
        await bro.waitForVisible(indexPage.menuModal.button, 5000);
    });

    async function openPageAndmenuModal(bro) {
        await bro.auth('taxi');

        await indexPage.open(bro);
        await bro.waitForVisible(indexPage.tariff.buttonTitleActive, 30000);
        await bro.waitForVisible(indexPage.menuModal.button, 5000);

        await indexPage.menuModal.open(bro);
    }
});
