const indexPage = require('../page-objects');

describe('app-editor-10: Главная. Закрытие режима редактирования по нажатию на кнопку "Готово"', function() {
    it('Главная страница редактора должна закрываться по нажатию на кнопку “Готово”', async function() {
        const bro = this.browser;

        await indexPage.openAndCheckPage(bro, { selectedItemPosition: 8 });
        await indexPage.closeAndCheckClosingPage(bro);
    });
});
