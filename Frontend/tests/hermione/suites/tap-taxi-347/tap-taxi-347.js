const indexPage = require('../../page-objects/index');

describe('tap-taxi-347:  Главная. Отображение информационного меню неавторизованным пользователем', function() {
    it('Должна открываться форма меню без аватарки и логина', async function() {
        const bro = this.browser;
        await indexPage.open(bro);
        await bro.waitForVisible(indexPage.menuModal.button, 5000);
        await indexPage.menuModal.open(bro);
        await bro.waitForVisible(indexPage.menuModal.userInfo, 5000, true);
    });
});
