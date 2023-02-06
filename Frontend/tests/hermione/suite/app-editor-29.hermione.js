const indexPage = require('../page-objects');

const nameApp = 'Чаты';
const nameAppIncorrect = 'Тестирование';

describe('app-edtor-29: Поиск. Добавление сервиса после отображения сообщения "Сервисы не найдены"', function() {
    it('Сервис должен добавиться в пустую ячейку', async function() {
        const bro = this.browser;

        await indexPage.openAndCheckPage(bro, { selectedItemPosition: 12 });
        await indexPage.openAndCheckSearchPopup(bro);
        await indexPage.fillingInputAndCheckErrorResult(bro, nameAppIncorrect);

        await indexPage.clearInputAndCheckResult(bro);
        await indexPage.fillInputAndCheckResult(bro, nameApp);
        await indexPage.searchAppAndAddInSearchPopup(bro, nameApp);
        await bro.assertView('app-editor-29-pinnedAppsPanel', indexPage.pinnedAppsPanel);

        await indexPage.closeAndCheckClosingPage(bro);
    });
});
