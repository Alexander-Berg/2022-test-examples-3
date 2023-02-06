const indexPage = require('../page-objects');

const itemPosition = [-1, 100, 'test', 'тест', '!@#!#%%^&'];

describe('app-editor-2: Главная. Автоматическое отображение первой активной ячейки при условии, что в get-параметре "selectedItemPosition" было передано некорректное значение', function() {
    for (let item of itemPosition) {
        it(`Должна быть активна первая ячейка при условии, что было передано значение в selectedItemPosition=${item}`, async function() {
            const bro = this.browser;

            await indexPage.openAndCheckPage(bro, { selectedItemPosition: item });
            await bro.waitForVisible(indexPage.pinnedAppsPanelFirstCellActive, 5000);
        });
    }
});
