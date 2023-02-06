const { clientDesktopBrowsersList } = require('@ps-int/ufo-hermione/browsers');
const { NAVIGATION } = require('../config').consts;
const { photo } = require('../page-objects/client-photo2-page').common;

describe('Фотосрез 2 -> ', () => {
    describe('восстановление прокрутки страницы', () => {
        beforeEach(async function() {
            const bro = this.browser;
            await bro.yaClientLoginFast('yndx-ufo-test-44');
            await bro.url(NAVIGATION.photo.url);
            await bro.yaSetPhotoSliceListingType('tile');
        });

        hermione.only.in(clientDesktopBrowsersList);
        it('diskclient-4538: видимый элемент должен оставаться на экране после изменения ширины экрана', async function() {
            const bro = this.browser;
            const { height, width } = await bro.windowHandleSize();

            await bro.yaWaitPhotoSliceItemsInViewportLoad();
            const itemSelector = photo.itemByName().replace(/:title/, '18-4.jpg');
            await bro.yaScroll(1580);

            await bro.yaAssertInViewport(itemSelector);

            await bro.windowHandleSize({ width: width / 2, height });
            await bro.yaAssertInViewport(itemSelector);

            await bro.windowHandleSize({ width: width * 2 / 3, height });
            await bro.yaAssertInViewport(itemSelector);
        });

        hermione.only.in('chrome-phone-6.0');
        it('diskclient-4679: видимый элемент должен оставаться на экране после смены ориентации телефона', async function() {
            const bro = this.browser;
            await bro.yaWaitPhotoSliceItemsInViewportLoad();
            const itemSelector = photo.itemByName().replace(/:title/, '3-8.jpg');
            await bro.yaScroll(1560);
            await bro.yaAssertInViewport(itemSelector);

            await bro.orientation('landscape');
            await bro.yaAssertInViewport(itemSelector);
        });
    });

    describe('восстановление прокрутки страницы в вау-сетке', () => {
        beforeEach(async function() {
            const bro = this.browser;
            await bro.yaClientLoginFast('yndx-ufo-test-143');
            await bro.url(NAVIGATION.photo.url);
        });

        hermione.only.in(clientDesktopBrowsersList);
        it('diskclient-4728: [Вау-сетка] Видимость фото при ресайзе окна', async function() {
            const bro = this.browser;
            const { height, width } = await bro.windowHandleSize();

            await bro.yaWaitPhotoSliceItemsInViewportLoad();
            const itemSelector = photo.itemByName().replace(/:title/, '2019-06-04 13-11-03.JPG');
            await bro.yaScroll(1880);

            await bro.yaAssertInViewport(itemSelector);

            await bro.windowHandleSize({ width: width / 2, height });
            await bro.yaAssertInViewport(itemSelector);

            await bro.windowHandleSize({ width: width * 2 / 3, height });
            await bro.yaAssertInViewport(itemSelector);
        });

        hermione.only.in('chrome-phone-6.0');
        it('diskclient-4727: [Вау-сетка] Видимость фото при смене ориентации экрана', async function() {
            const bro = this.browser;
            await bro.yaWaitPhotoSliceItemsInViewportLoad();
            const itemSelector = photo.itemByName().replace(/:title/, '2019-06-04 13-09-58.JPG');
            await bro.yaScroll(1580);
            await bro.yaAssertInViewport(itemSelector);

            await bro.orientation('landscape');
            await bro.yaAssertInViewport(itemSelector);
        });
    });
});
