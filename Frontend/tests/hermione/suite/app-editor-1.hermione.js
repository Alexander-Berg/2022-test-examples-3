const indexPage = require('../page-objects');

const itemPosition = [0, 9, 10, 15];

describe('app-editor-1: Главная. Автоматическое выставление активной ячейки по переданному значению get-параметра "selectedItemPosition"', function() {
    for (let item of itemPosition) {
        it(`Должна быть активна ячейка, расположенная в позиции ${item}`, async function() {
            const bro = this.browser;

            await indexPage.openAndCheckPage(bro, { selectedItemPosition: item });
            const page = await indexPage.carouselPage(bro, item > 9 ? 1 : 0);
            if (item >= 10) {
                await bro.swipeLeft(page.selector);

                const paginatorActive = await indexPage.pinnedPaginatorActive(2);
                await bro.waitForVisible(paginatorActive, 5000);
            }

            await bro.waitForVisible(page.cellActive);
            await bro.assertView(`app-editor-1-cellActive-${item}`, indexPage.pinnedAppsPanel);
        });
    }
});
