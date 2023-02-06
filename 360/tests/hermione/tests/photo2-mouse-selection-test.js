const { clientDesktopBrowsersList } = require('@ps-int/ufo-hermione/browsers');
const { NAVIGATION } = require('../config').consts;
const { common: { photo }, desktop: { fastScroll } } = require('../page-objects/client-photo2-page');
const clientNavigation = require('../page-objects/client-navigation');
const popups = require('../page-objects/client-popups');

const photoGroup = (n) => `${photo.group()}:nth-of-type(${n})`;

describe('Фотосрез 2 -> ', () => {
    hermione.only.in(clientDesktopBrowsersList, 'Актуально только для десктопной версии');
    describe('выделение мышкой', () => {
        afterEach(async function() {
            const bro = this.browser;
            await bro.yaRestoreAllFromTrash();
        });

        it('diskclient-4120: Проверка отрисовки рамки мультивыделения', async function() {
            const bro = this.browser;
            this.testpalmId = 'diskclient-4120';

            await bro.yaClientLoginFast('yndx-ufo-test-103');
            await bro.url(NAVIGATION.photo.url);
            await bro.yaWaitPhotoSliceItemsInViewportLoad();

            const { x, y } = await bro.getLocation(photoGroup(1));
            const { width, height } = await bro.getViewportSize();

            await bro.yaMouseSelect(photoGroup(1), {
                startX: x,
                startY: y,
                deltaX: width - x - 60,
                deltaY: height / 2,
                releaseMouse: false
            });

            await bro.yaAssertView('diskclient-4120', photo());
        });

        it('diskclient-4121: Мультивыделение ресурсов мышью в фотосрезе', async function() {
            const bro = this.browser;
            this.testpalmId = 'diskclient-4121';

            await bro.yaClientLoginFast('yndx-ufo-test-103');
            await bro.url(NAVIGATION.photo.url);
            await bro.yaWaitPhotoSliceItemsInViewportLoad();

            const { x, y } = await bro.getLocation(photoGroup(1));
            const { width, height } = await bro.getViewportSize();

            await bro.yaMouseSelect(photoGroup(1), {
                startX: x,
                startY: y,
                deltaX: width - x - 60,
                deltaY: height / 2
            });

            await bro.yaWaitActionBarDisplayed();
            await bro.pause(500);
            await bro.yaAssertView('diskclient-4121', 'body', {
                ignoreElements: [clientNavigation.desktop.spaceInfoSection()]
            });
        });

        it('diskclient-4122: Проверка мультивыделения после удаления ресурса', async function() {
            const bro = this.browser;
            this.testpalmId = 'diskclient-4122';

            await bro.yaClientLoginFast('yndx-ufo-test-201');
            await bro.url(NAVIGATION.photo.url);
            await bro.yaWaitPhotoSliceItemsInViewportLoad();

            await bro.moveToObject(photo.item(), 10, 10);
            await bro.click(photo.item.checkbox());

            await bro.yaWaitActionBarDisplayed();
            await bro.click(popups.common.actionBar.deleteButton());

            await bro.yaAssertProgressBarAppeared();
            await bro.yaAssertProgressBarDisappeared();

            const { x, y } = await bro.getLocation(photoGroup(1));
            const { width, height } = await bro.getViewportSize();

            await bro.yaMouseSelect(photoGroup(1), {
                startX: x,
                startY: y,
                deltaX: width - x - 60,
                deltaY: height / 2
            });

            await bro.yaWaitActionBarDisplayed();
            await bro.pause(500);
            await bro.yaAssertView('diskclient-4122', 'body', {
                invisibleElements: [clientNavigation.desktop.spaceInfoSection()]
            });
        });

        it('diskclient-4661: [Вау-сетка] Выделение файлом в вау-сетке', async function() {
            const bro = this.browser;
            this.testpalmId = 'diskclient-4661';

            await bro.yaClientLoginFast('yndx-ufo-test-143');
            await bro.url(NAVIGATION.photo.url);
            await bro.yaWaitPhotoSliceItemsInViewportLoad();

            await bro.yaScroll(570);

            const { x, y } = await bro.getLocation(photo.itemByName().replace(/:title/, '2019-06-17 19-48-41.JPG'));
            const { width, height } = await bro.getViewportSize();

            await bro.yaMouseSelect(photoGroup(1), {
                startX: x - 30,
                startY: y,
                deltaX: width - x - 60,
                deltaY: height / 2
            });

            await bro.yaWaitActionBarDisplayed();
            await bro.pause(1000);
            await bro.yaAssertView(this.testpalmId, 'body', {
                ignoreElements: [clientNavigation.desktop.spaceInfoSection()],
                hideElements: [fastScroll.pointer()]
            });
        });
    });
});
