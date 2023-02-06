const consts = require('../config/index').folder;
const PageObjects = require('../page-objects/public');
const { wowGridItem, listingItem } = require('../helpers/selectors');
const { assert } = require('chai');

hermione.only.in('chrome-desktop');
describe('Управление клавиатурой', () => {
    it('diskpublic-2928: Открытие папки по Enter', async function() {
        const bro = this.browser;
        await bro.url(consts.PUBLIC_FOLDER_URL);
        await bro.yaWaitForVisible(PageObjects.listing());

        await bro.yaClick(PageObjects.listing.subfolder());

        // Закрываем инфо панель
        await bro.keys('Escape');

        await bro.keys('Enter');

        const item = await bro.$(listingItem(4));

        await bro.yaWaitForVisible(listingItem(4));

        assert.equal(
            (await item.getText(listingItem(4))),
            consts.PUBLIC_SUBFOLDER_ITEM_4
        );
    });

    it('diskpublic-2930: Выбор элемента по Tab (перемещение с помощью стрелки)', async function() {
        const bro = this.browser;
        await bro.url(consts.PUBLIC_FOLDER_URL);
        await bro.yaWaitForVisible(PageObjects.listing());

        await bro.yaClick(PageObjects.listing.subfolder());

        // Закрываем инфо панель
        await bro.keys('Escape');

        // Нажимаем стрелку вниз чтобы перейти к файлу
        await bro.keys('Down arrow');

        const item = await bro.$(listingItem(2));
        await item.scrollIntoView();

        await bro.yaAssertView('diskpublic-2930', listingItem(2));
    });

    it('diskpublic-2929: Открытие слайдера по Enter в вау-сетке', async function() {
        const bro = this.browser;
        await bro.url(consts.PUBLIC_FOLDER_WITH_10_PHOTOS_URL);
        await bro.yaWaitForVisible(PageObjects.wowGrid.item());
        await bro.yaFocus(PageObjects.wowGrid.item());

        await bro.keys('Enter');

        await bro.yaWaitForVisible(PageObjects.slider());
    });

    it('diskpublic-2927: Выбор элемента по Tab в вау-сетке', async function() {
        const bro = this.browser;
        await bro.url(consts.PUBLIC_FOLDER_WITH_10_PHOTOS_URL);
        await bro.yaWaitForVisible(PageObjects.wowGrid.item());
        await bro.yaFocus(PageObjects.wowGrid.item());

        await bro.keys('Tab');

        await bro.yaAssertView('diskpublic-2927', wowGridItem(2));
    });
});
