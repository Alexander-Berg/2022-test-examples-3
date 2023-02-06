const { NAVIGATION } = require('../config').consts;
const clientPhotoPage = require('../page-objects/client-photo2-page');
const touchPopups = require('../page-objects/client-popups').touch;

describe('Переключение вида в фотосрезе -> ', () => {
    describe('Отображение переключалки', () => {
        beforeEach(async function() {
            const bro = this.browser;
            await bro.yaClientLoginFast('yndx-ufo-test-144');
            await bro.url(NAVIGATION.photo.url);
            await bro.yaWaitPhotoSliceItemsInViewportLoad();
        });

        hermione.only.in('chrome-phone-6.0');
        it('diskclient-4600: [Вау-сетка] Открытие попапа выбора режимов вау-сетки в горизонтальной ориентации', async function() {
            const bro = this.browser;
            this.testpalmId = 'diskclient-4600';
            await bro.orientation('landscape');

            await bro.yaWaitForVisible(clientPhotoPage.common.photoHeader.menuButton());
            await bro.click(clientPhotoPage.common.photoHeader.menuButton());
            await bro.yaWaitForVisible(clientPhotoPage.common.photoHeaderMenu());
            await bro.pause(200); // because of `animation-duration: .2s`
            await bro.yaScroll(clientPhotoPage.common.photoHeaderMenu.filterFoldersRadio());
            await bro.pause(500); // чтобы успел скрыться скроллбар

            // проскроллили вниз - первого пункта меню не видно
            await bro.yaAssertView('diskclient-photo-view-menu-in-landscape-1', touchPopups.mobilePaneVisible());

            await bro.click(clientPhotoPage.common.photoHeaderMenu.filterFoldersRadio());
            await bro.yaWaitForHidden(clientPhotoPage.common.photoHeaderMenu());
            await bro.click(clientPhotoPage.common.photoHeader.menuButton());
            await bro.yaWaitForVisible(clientPhotoPage.common.photoHeaderMenu());
            await bro.pause(200); // because of `animation-duration: .2s`

            // поосле переткрытия первый пункт снова видно (но не видно последний)
            await bro.yaAssertView('diskclient-photo-view-menu-in-landscape-2', touchPopups.mobilePaneVisible());
        });
    });

    describe('Переключение вида', () => {
        const getActiveView = async function(bro) {
            const { value } = await bro.execute(() => {
                const photo2 = document.querySelector('.js-photo');
                if (photo2.classList.contains('photo_tile')) {
                    return 'tile';
                } else if (photo2.classList.contains('photo_wow')) {
                    return 'wow';
                }
            });
            return value;
        };

        const maybeSwitchToView = async function(bro, view) {
            const shouldSwitch = (await getActiveView(bro)) !== view;
            if (shouldSwitch) {
                await bro.yaSetPhotoSliceListingType(view);
                await bro.refresh();
                await bro.yaWaitPhotoSliceItemsInViewportLoad();
            }
        };

        it('diskclient-4730: [Вау-сетка] [Тач] Отображение вау-сетки в разных режимах', async function() {
            const bro = this.browser;
            this.testpalmId = 'diskclient-4730';
            await bro.yaClientLoginFast('yndx-ufo-test-145');
            await bro.url(NAVIGATION.photo.url);
            await bro.yaWaitPhotoSliceItemsInViewportLoad();

            // если предыдущий запуск теста упал, мог сохраниться неправильный вид в настройках пользователя
            await maybeSwitchToView(bro, 'wow');

            await bro.yaWaitForVisible(clientPhotoPage.common.photo.wow());

            await bro.yaSetPhotoSliceListingType('tile');
            await bro.yaWaitForVisible(clientPhotoPage.common.photo.tile());
            await bro.yaWaitPhotoSliceItemsInViewportLoad();
            await bro.yaAssertView(this.testpalmId + '-1', clientPhotoPage.common.photo());

            // после рефреша вид сохраняется
            await bro.refresh();
            await bro.yaWaitPhotoSliceItemsInViewportLoad();
            await bro.yaWaitForVisible(clientPhotoPage.common.photo.tile());

            // переключим обратно
            await bro.yaSetPhotoSliceListingType('wow');
            await bro.yaWaitForVisible(clientPhotoPage.common.photo.wow());
            await bro.yaWaitPhotoSliceItemsInViewportLoad();
            await bro.yaAssertView(this.testpalmId + '-2', clientPhotoPage.common.photo());

            // после рефреша вид сохраняется
            await bro.refresh();
            await bro.yaWaitPhotoSliceItemsInViewportLoad();
            await bro.yaWaitForVisible(clientPhotoPage.common.photo.wow());
        });
    });
});
