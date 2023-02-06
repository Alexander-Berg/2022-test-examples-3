const indexPage = require('../page-objects');

const nameAppIncorrect = 'Тестирование';

describe('app-editor-30: Поиск. Отображение сообщения "Приложения не найдены" после повторного открытия попапа поиска', function() {
    it('Должно отображаться сообщение "Приложения не найдены"', async function() {
        const bro = this.browser;

        await indexPage.openAndCheckPage(bro, { selectedItemPosition: 12 });
        await indexPage.openAndCheckSearchPopup(bro);
        await indexPage.fillingInputAndCheckErrorResult(bro, nameAppIncorrect);

        await indexPage.closeSearchPopup(bro);
        await indexPage.openAndCheckSearchPopup(bro);
        await bro.waitForVisible(indexPage.appsSearchPopupErrorMessage, 5000);

        await indexPage.assertCurrentTextInSearchInput(bro, nameAppIncorrect);
        await bro.assertView('app-editor-30-appsSearchPopupErrorMessage', indexPage.appPage);
    });
});
