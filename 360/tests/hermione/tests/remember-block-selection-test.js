const { clientDesktopBrowsersList } = require('@ps-int/ufo-hermione/browsers');
const rememberBlock = require('../page-objects/client-remember-block').common;
const popups = require('../page-objects/client-popups').common;
const clientNavigation = require('../page-objects/client-navigation');
const { BLOCKS } = require('../consts/remember-block-tests-consts');

const { assert } = require('chai');

const itemSelector = (n) => `${rememberBlock.wowResource()}:nth-child(${n})`;

describe('Выделение в блоке воспоминаний ->', () => {
    hermione.only.in(clientDesktopBrowsersList, 'Актуально только для десктопной версии');
    describe('контекстное меню', () => {
        it('diskclient-5141: Контекстное меню в блоке воспоминаний', async function() {
            const bro = this.browser;
            this.testpalmId = 'diskclient-5141';

            await bro.loginAndGoToUrl('', '', BLOCKS.exists);

            await bro.rightClick(itemSelector(2));
            await bro.yaWaitForVisible(popups.actionPopup(), 'контекстное меню не отобразилось');
            await bro.pause(300);
            await bro.yaAssertView('remember-block-context-menu', rememberBlock.blockInner());
        });

        it('diskclient-5142: Закрытие КМ по клику вне фото', async function() {
            const bro = this.browser;

            await bro.loginAndGoToUrl('', '', BLOCKS.exists);

            await bro.rightClick(itemSelector(2));
            await bro.yaWaitForVisible(popups.actionPopup(), 'контекстное меню не отобразилось');
            await bro.click(rememberBlock.title());
            await bro.yaWaitForHidden(popups.actionPopup(), 'контекстное меню не скрылось');
        });

        it('diskclient-5143: Открытие КМ при повторном нажатии на ПКМ', async function() {
            const bro = this.browser;

            await bro.loginAndGoToUrl('', '', BLOCKS.exists);

            await bro.rightClick(itemSelector(1));
            await bro.yaWaitForVisible(popups.actionPopup(), 'контекстное меню не отобразилось');
            await bro.pause(200);

            await bro.rightClick(itemSelector(2));
            await bro.yaWaitForVisible(popups.actionPopup());

            const isVisible = await bro.yaIsActionBarDisplayed();
            assert(isVisible === false);

            await bro.pause(300);
            await bro.yaAssertView('remember-block-context-menu-visible', rememberBlock.blockInner());
        });

        it('diskclient-5144: Снятие выделения фото в блоке воспоминаний при вызове КМ', async function() {
            const bro = this.browser;

            await bro.loginAndGoToUrl('', '', BLOCKS.exists);

            for (let i = 2; i <= 4; ++i) {
                const item = itemSelector(i);

                await bro.yaSelectPhotoItem(item, true, true);
            }

            await bro.yaWaitActionBarDisplayed();

            const selector = itemSelector(5);
            const item = await bro.$(selector);

            await item.scrollIntoView();
            await bro.rightClick(selector);

            await bro.yaWaitForVisible(popups.actionPopup(), 'контекстное меню не отобразилось');
            await bro.yaWaitActionBarHidden();
        });

        it('diskclient-5145: Отображение КМ после клика ПКМ по выделенному фото', async function() {
            const bro = this.browser;

            await bro.loginAndGoToUrl('', '', BLOCKS.exists);

            for (let i = 2; i <= 4; ++i) {
                const item = itemSelector(i);
                await bro.yaSelectPhotoItem(item, true, true);
            }

            await bro.yaWaitActionBarDisplayed();
            await bro.rightClick(itemSelector(4));
            await bro.yaWaitForVisible(popups.actionPopup(), 'контекстное меню не отобразилось');
            const toolbarIsStillVisible = await bro.yaIsActionBarDisplayed();
            assert(toolbarIsStillVisible === true, 'скрылся тулбар');
        });
    });

    hermione.only.in(clientDesktopBrowsersList, 'Актуально только для десктопной версии');
    describe('выделение мышкой', () => {
        it('diskclient-5147: Мультивыделение drag&drop фото в блоке воспоминаний', async function() {
            const bro = this.browser;

            await bro.loginAndGoToUrl('', '', BLOCKS.existsBig);

            const { x, y } = await bro.getLocation(rememberBlock.title());
            const { width, height } = await bro.getViewportSize();

            await bro.yaMouseSelect(rememberBlock.title(), {
                startX: x - 10,
                startY: y - 10,
                deltaX: width - x - 700,
                deltaY: height / 2,
                releaseMouse: false
            });

            await bro.yaAssertView('remember-block-mouse-selection-frame', rememberBlock.blockInner());
        });

        it('diskclient-5146: Мультивыделение фото в блоке воспоминаний', async function() {
            const bro = this.browser;

            await bro.loginAndGoToUrl('', '', BLOCKS.existsBig);

            const { x, y } = await bro.getLocation(rememberBlock.title());
            const { width, height } = await bro.getViewportSize();

            await bro.yaMouseSelect(rememberBlock.title(), {
                startX: x,
                startY: y,
                deltaX: width - x - 700,
                deltaY: height / 2
            });

            await bro.yaWaitActionBarDisplayed();
            await bro.pause(500);
            await bro.yaAssertView('remember-block-mouse-selection', 'body', {
                ignoreElements: [clientNavigation.desktop.infoSpaceButton()]
            });
        });
    });

    describe('выделение чекбоками', () => {
        hermione.only.notIn('chrome-phone');
        it('diskclient-5148, diskclient-5140: Выделение фото в блоке воспоминаний', async function() {
            const bro = this.browser;
            const isMobile = await this.browser.yaIsMobile();
            this.testpalmId = isMobile ? 'diskclient-5148' : 'diskclient-5140';

            await bro.loginAndGoToUrl('', '', BLOCKS.existsBig);

            for (let i = 1; i <= 4; ++i) {
                const item = itemSelector(i);

                await bro.yaSelectPhotoItem(item, true, false);
            }

            let selectionInfo = await bro.getText(popups.actionBar.selectionInfoText());
            assert(Number.parseInt(selectionInfo, 10) === 4);

            let item = itemSelector(1);
            await bro.yaSelectPhotoItem(item, true, false);
            selectionInfo = await bro.getText(popups.actionBar.selectionInfoText());
            assert(Number.parseInt(selectionInfo, 10) === 3);

            item = itemSelector(6);
            await bro.yaSelectPhotoItem(item, true, false);
            selectionInfo = await bro.getText(popups.actionBar.selectionInfoText());
            assert(Number.parseInt(selectionInfo, 10) === 4);
        });

        hermione.only.in('chrome-phone-6.0');
        it('diskclient-5149: [Тач] Снятие выделения с фото в блоке воспоминаний', async function() {
            const bro = this.browser;
            this.testpalmId = 'diskclient-5149';

            await bro.loginAndGoToUrl('', '', BLOCKS.existsBig);

            for (let i = 1; i <= 4; ++i) {
                const item = itemSelector(i);

                await bro.yaSelectPhotoItem(item, true, false);
            }

            const selectionInfo = await bro.getText(popups.actionBar.selectionInfoText());
            assert(Number.parseInt(selectionInfo, 10) === 4);

            for (let i = 1; i <= 4; ++i) {
                const item = itemSelector(i);

                await bro.yaSelectPhotoItem(item, true, false);
                const isSelectionInfoVisible = await bro.isVisible(popups.actionBar.selectionInfoText());
                if (isSelectionInfoVisible) {
                    const selectionInfo = await bro.getText(popups.actionBar.selectionInfoText());
                    assert(Number.parseInt(selectionInfo, 10) === 4 - i);
                }
            }

            const isSelectionInfoVisible = await bro.isVisible(popups.actionBar.selectionInfoText());

            assert(isSelectionInfoVisible === false);
        });
    });
});
