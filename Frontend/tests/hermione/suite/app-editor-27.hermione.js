const indexPage = require('../page-objects');

const nameAppIncorrect = 'Тестирование';

describe('app-editor-27: Поиск. Отображение сообщения "Сервисы не найдены', function() {
    it('В попапе поиска должно отображаться сообщение "Сервисы не найдены" при вводе некорректного значения', async function() {
        const bro = this.browser;

        await indexPage.openAndCheckPage(bro, { selectedItemPosition: 12 });
        await indexPage.openAndCheckSearchPopup(bro);
        await indexPage.fillingInputAndCheckErrorResult(bro, nameAppIncorrect);
        await bro.assertView('app-editor-27-appsSearchPopupErrorMessage', indexPage.appPage);

        await indexPage.clearInputAndCheckResult(bro);
        await indexPage.closeSearchPopup(bro);

        await indexPage.openAndCheckSearchPopup(bro);
        await bro.waitForVisible(indexPage.appsSearchPopupErrorMessage, 5000, true);
    });
});
