const { clientDesktopBrowsersList } = require('@ps-int/ufo-hermione/browsers');
const { NAVIGATION } = require('../config').consts;
const { photo } = require('../page-objects/client-photo2-page').common;
const popups = require('../page-objects/client-popups');
const assert = require('chai').assert;

const photoItem = (n) => `${photo.item()}:nth-child(${n})`;

describe('Фотосрез 2 -> ', () => {
    hermione.only.in(clientDesktopBrowsersList, 'Актуально только для десктопной версии');
    describe('вызов контекстного меню', () => {
        beforeEach(async function() {
            const bro = this.browser;
            await bro.yaClientLoginFast('yndx-ufo-test-103');
            await bro.url(NAVIGATION.photo.url);
            await bro.yaWaitPhotoSliceItemsInViewportLoad();
        });

        it('diskclient-4401: Контекстное меню в фотосрезе. Обычный ресурс', async function() {
            const bro = this.browser;

            await bro.rightClick(photoItem(2));
            await bro.yaWaitForVisible(popups.common.actionPopup(), 'контекстное меню не отобразилось');
            await bro.pause(500);
            await bro.yaAssertView('diskclient-4401', photo());
        });

        it('diskclient-4632: клик по вне закрывает контекстное меню', async function() {
            const bro = this.browser;

            await bro.rightClick(photoItem(2));
            await bro.yaWaitForVisible(popups.common.actionPopup(), 'контекстное меню не отобразилось');
            await bro.click(photo.title());
            await bro.yaWaitForHidden(popups.common.actionPopup(), 'контекстное меню не скрылось');
        });

        it('diskclient-4669: клик правой кнопкной мыши по другому элементу открывает соответствующее контекстное меню', async function() {
            const bro = this.browser;

            await bro.rightClick(photoItem(2));
            await bro.yaWaitForVisible(popups.common.actionPopup(), 'контекстное меню не отобразилось');
            await bro.pause(200);

            await bro.rightClick(photoItem(3));
            await bro.yaWaitForVisible(popups.common.actionPopup());
            const isVisible = await bro.yaIsActionBarDisplayed();
            assert(isVisible === false);
            await bro.yaAssertView('diskclient-4669', photo());
        });

        it('diskclient-4670: клик правой кнопкной мыши по невыделенному элементу снимает выделение', async function() {
            const bro = this.browser;

            for (let i = 2; i <= 4; ++i) {
                const item = photoItem(i);
                await bro.moveToObject(item);
                await bro.click(`${item} .lite-checkbox`);
            }

            await bro.yaWaitActionBarDisplayed();
            await bro.rightClick(photoItem(5));
            await bro.yaWaitForVisible(popups.common.actionPopup(), 'контекстное меню не отобразилось');
            await bro.yaWaitActionBarHidden();
        });

        it('diskclient-4671: клик правой кнопкной мыши по выделенному элементу не снимает выделение', async function() {
            const bro = this.browser;

            for (let i = 2; i <= 4; ++i) {
                const item = photoItem(i);
                await bro.moveToObject(item);
                await bro.click(`${item} .lite-checkbox`);
            }

            await bro.yaWaitActionBarDisplayed();
            await bro.rightClick(photoItem(4));
            await bro.yaWaitForVisible(popups.common.actionPopup(), 'контекстное меню не отобразилось');
            const toolbarIsStillVisible = await bro.yaIsActionBarDisplayed();
            assert(toolbarIsStillVisible === true, 'скрылся тулбар');
        });
    });

    hermione.only.in(clientDesktopBrowsersList, 'Актуально только для десктопной версии');
    it('diskclient-4726: [Вау-сетка] Вызов контекстного меню', async function() {
        const bro = this.browser;
        this.testpalmId = 'diskclient-4726';

        await bro.yaClientLoginFast('yndx-ufo-test-143');
        await bro.url(NAVIGATION.photo.url);
        await bro.yaWaitPhotoSliceItemsInViewportLoad();

        await bro.rightClick(photoItem(2));
        await bro.yaWaitForVisible(popups.common.actionPopup(), 'контекстное меню не отобразилось');
        await bro.pause(200);
        await bro.yaAssertView(this.testpalmId, photo());
    });
});
