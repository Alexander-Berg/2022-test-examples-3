const {
    promoBanner,
    mobilePromoBanner,
    appPromoBanner,
    b2cBanner,
    stubDownloads,
    root
} = require('../page-objects/client');
const clientPopups = require('../page-objects/client-popups');
const clientNavigation = require('../page-objects/client-navigation');

const { NAVIGATION } = require('../config').consts;
const { ONE_DAY, DESKTOP_APP_CLOSED_BANNER_TIMEOUT } = require('../../../components/consts');
const { assert } = require('chai');

const MOBILE_BANNER_TEST_ID = '142343';

hermione.only.in('chrome-desktop-win', 'Баннеры скачивания ПО есть только на десктопах');
describe('Проверка промо-баннеров скачивания ПО -> ', () => {
    it('diskclient-4404: Отсутствие баннера при отображении welcome-попапа', async function() {
        const bro = this.browser;
        this.testpalmId = 'diskclient-4404';
        await bro.yaClientLoginFast('yndx-ufo-test-97');
        // Сбрасываем настройки показа welcome-popup, чтобы он однозначно отобразился
        await bro.yaResetWelcomePopupSettings();
        await bro.url(`${NAVIGATION.disk.url}?test-id=${MOBILE_BANNER_TEST_ID}`);
        await bro.yaWaitForHidden(promoBanner(), 'Промо-баннер отобразился');

        await bro.url(NAVIGATION.disk.url);
        await bro.yaWaitForHidden(promoBanner(), 'Промо-баннер отобразился');
        await bro.yaResetWelcomePopupSettings();
    });

    hermione.auth.createAndLogin({ tus_consumer: 'disk-front-client' });
    it('diskclient-4403: Отсутствие баннера для нового пользователя', async function() {
        const bro = this.browser;
        await bro.yaSkipWelcomePopup();
        this.testpalmId = 'diskclient-4403';
        await bro.yaOpenSection('disk');
        await bro.yaWaitForHidden(appPromoBanner(), 'Промо-баннер отобразился');
    });

    it('diskclient-4419: Отображение баннера', async function() {
        const bro = this.browser;
        this.testpalmId = 'diskclient-4419';
        await bro.yaClientLoginFast('yndx-ufo-test-746');
        await bro.url(NAVIGATION.disk.url);
        await bro.yaWaitForVisible(appPromoBanner(), 'Промо-баннер на скачивание ПО не отобразился');
        await bro.yaAssertView(this.testpalmId, appPromoBanner());
    });

    it('diskclient-4383: Отображение баннера при переходах в Диске', async function() {
        const bro = this.browser;
        this.testpalmId = 'diskclient-4383';
        await bro.yaClientLoginFast('yndx-ufo-test-746');
        await bro.url(NAVIGATION.disk.url);
        await bro.yaWaitForVisible(appPromoBanner(), 'Промо-баннер на скачивание ПО не отобразился');

        await bro.click(clientNavigation.desktop.navigationItemDownloads());
        await bro.yaWaitForVisible(stubDownloads());
        await bro.yaWaitForVisible(appPromoBanner(), 'Промо-баннер на скачивание ПО не отобразился в загрузках');
        await bro.yaOpenSection('disk');
        await bro.yaWaitForVisible(appPromoBanner(), 'Промо-баннер на скачивание ПО не отобразился после перехода');
    });

    it('diskclient-4399: Отображение рекламного блока за скроллом', async function() {
        const bro = this.browser;
        this.testpalmId = 'diskclient-4399';
        await bro.yaClientLoginFast('yndx-ufo-test-746');
        await bro.url(NAVIGATION.disk.url);
        await bro.yaWaitForVisible(appPromoBanner(), 'Промо-баннер на скачивание ПО не отобразился');

        assert(await bro.isVisibleWithinViewport(root.content.bottomAd()),
            'Рекламный блок не во вьюпорте');
        await bro.setViewportSize({ width: 900, height: 799 });
        assert(!(await bro.isVisibleWithinViewport(root.content.bottomAd.frame())),
            'Рекламный блок не скрылся из вьюпорта при высоте вьюпорта < 800');
        await bro.scroll(root.content.bottomAd());
        assert(await bro.isVisibleWithinViewport(root.content.bottomAd()),
            'Рекламный блок не показался во вьюпорте после скролла');
    });

    it('diskclient-4115: проверка открытия лендинга ПО при нажатии скачать на баннере', async function() {
        const bro = this.browser;
        this.testpalmId = 'diskclient-4115';
        await bro.yaClientLoginFast('yndx-ufo-test-747');
        await bro.url(NAVIGATION.disk.url);
        await bro.yaSetUserSettings('timestampLastDownloadedDesktopPromo');
        await bro.yaWaitForVisible(appPromoBanner(), 'Промо-баннер на скачивание ПО не отобразился');
        await bro.click(appPromoBanner.installButton());
        await bro.yaSetUserSettings('timestampLastDownloadedDesktopPromo');
        await bro.yaSwitchTab();
        await bro.yaAssertUrlInclude('/download');
    });

    it(`diskclient-4414: Отображение баннера через ${DESKTOP_APP_CLOSED_BANNER_TIMEOUT / ONE_DAY} 
        дней после закрытия крестиком`, async function() {
        const bro = this.browser;
        this.testpalmId = 'diskclient-4414';
        await bro.yaClientLoginFast('yndx-ufo-test-748');
        await bro.url(NAVIGATION.disk.url);
        await bro.yaSetUserSettings('timestampLastClosedDesktopPromo');
        await bro.yaWaitForVisible(appPromoBanner(), 'Промо-баннер на скачивание ПО не отобразился');

        // Закрытие промо-баннера по кресту
        await bro.click(appPromoBanner.closeButton());
        await bro.yaWaitForHidden(appPromoBanner(), 'Промо-баннер отобразился после нажатия на кнопку закрытия');
        await bro.refresh();
        await bro.yaWaitForHidden(appPromoBanner(),
            'Промо-баннер на скачивание ПО отобразился, несмотря не закрытие до обновления страницы');

        await bro.yaSetUserSettings('timestampLastClosedDesktopPromo',
            String(Date.now() - DESKTOP_APP_CLOSED_BANNER_TIMEOUT));
        await bro.yaWaitForVisible(appPromoBanner(),
            'Промо-баннер на скачивание ПО не отобразился через 30 дней после закрытия');
    });

    it('diskclient-4840: Отображение баннера через 2 дня после скачивания ПО', async function() {
        const bro = this.browser;
        this.testpalmId = 'diskclient-4840';
        await bro.yaClientLoginFast('yndx-ufo-test-749');
        await bro.url(NAVIGATION.disk.url);
        await bro.yaSetUserSettings('timestampLastDownloadedDesktopPromo');
        await bro.yaWaitForVisible(appPromoBanner(), 'Промо-баннер на скачивание ПО не отобразился');

        // Закрытие промо-баннера после скачивания и обновления страницы
        await bro.click(appPromoBanner.installButton());
        await bro.pause(1000);
        await bro.url(NAVIGATION.disk.url);
        await bro.yaWaitForHidden(
            appPromoBanner(),
            'Промо-баннер отобразился, несмотря на скачивание и перезагрузку страницы'
        );

        await bro.yaSetUserSettings('timestampLastDownloadedDesktopPromo',
            String(Date.now() - ONE_DAY * 2));
        await bro.yaWaitForVisible(appPromoBanner(),
            'Промо-баннер на скачивание ПО не отобразился через 2 дня после скачивания');
    });

    hermione.skip.notIn('', 'Эксперимент был отклонен, но будет другой похожий');
    it('Промо-баннер на скачивание мобильного приложения отображается под экспериментом', async function() {
        const bro = this.browser;

        await bro.yaClientLoginFast('yndx-ufo-test-98');
        // Редактируем настройки, чтобы welcome-попап не отобразился
        await bro.yaSetUserSettings('timestampLastDisplayedDialogWelcome', String(Date.now()));
        // Сбрасываем дату последнего закрытия баннера
        await bro.yaSetUserSettings('timestampLastClosedMobilePromo');

        await bro.url(`${NAVIGATION.disk.url}?test-id=${MOBILE_BANNER_TEST_ID}`);
        await bro.yaWaitForVisible(
            mobilePromoBanner(),
            'Промо-баннер на скачивание мобильного приложения не отобразился'
        );

        await bro.click(promoBanner.closeButton());
        await bro.yaWaitForHidden(mobilePromoBanner(), 'Промо-баннер отобразился после нажатия на кнопку закрытия');
        await bro.refresh();
        await bro.yaWaitForHidden(
            mobilePromoBanner(),
            'Промо-баннер отобразился, несмотря не закрытие баннера в момент до обновления страницы'
        );

        // Сбрасываем дату последнего закрытия баннера
        await bro.yaSetUserSettings('timestampLastDisplayedDialogWelcome');
        await bro.yaSetUserSettings('timestampLastClosedMobilePromo');
    });
});

describe('Проверка промо-баннеров -> ', () => {
    afterEach(async function() {
        const { items } = this.currentTest.ctx;
        await this.browser.yaDeleteCompletely(items, { fast: true, safe: true });
    });

    it('diskclient-4782, 4758: Скрытие уведомления, предлагающего скачать ПО в загрузчике ', async function() {
        const bro = this.browser;
        this.testpalmId = await bro.yaIsMobile() ? 'diskclient-4782' : 'diskclient-4758';
        await bro.yaClientLoginFast('yndx-ufo-test-279');

        const firstTestFileName = await bro.yaUploadFiles('test-file.txt', { uniq: true, closeUploader: false });
        this.currentTest.ctx.items = [firstTestFileName];
        await bro.yaWaitForVisible(clientPopups.common.uploader.uploaderNotifications.uploaderPromoNotification());

        await bro.click(clientPopups.common.uploader.uploaderNotifications.uploaderPromoNotification.closeButton());
        await bro.yaWaitForHidden(clientPopups.common.uploader.uploaderNotifications.uploaderPromoNotification());
        await bro.yaAssertView(this.testpalmId, clientPopups.common.uploader(), {
            ignoreElements: clientPopups.common.uploader.listingItem.itemTitle()
        });

        await bro.refresh();

        const secondTestFileName = await bro.yaUploadFiles('test-file.txt', { uniq: true, closeUploader: false });
        this.currentTest.ctx.items.push(secondTestFileName);
        await bro.yaWaitForVisible(clientPopups.common.uploader.uploaderNotifications.uploaderPromoNotification());

        await bro.click(clientPopups.common.uploader.closeButton());
        await bro.yaWaitForHidden(clientPopups.common.uploader());
    });

    it('diskclient-4781, 4757: Клик по уведомлению, предлагающему скачать ПО в загрузчике', async function() {
        const bro = this.browser;
        await bro.yaClientLoginFast('yndx-ufo-test-279');

        const testFileName = await bro.yaUploadFiles('test-file.txt', { uniq: true, closeUploader: false });
        this.currentTest.ctx.items = [testFileName];
        await bro.click(clientPopups.common.uploader.uploaderNotifications.uploaderPromoNotification());

        const tabs = await bro.getTabIds();
        await bro.window(tabs[0]);
        await assert.equal(tabs.length, 2);

        await bro.yaWaitForVisible(clientPopups.common.uploader.uploaderNotifications.uploaderPromoNotification());

        await bro.click(clientPopups.common.uploader.closeButton());
        await bro.yaWaitForHidden(clientPopups.common.uploader());
    });
});

hermione.only.in('chrome-desktop-win');
describe('Проверка баннера b2c', () => {
    it('b2c-1-client-promo-banner: Проверка отображения промо-баннера b2c-1 в клиенте', async function() {
        const bro = this.browser;
        await bro.yaClientLoginFast('yndx-ufo-test-771');
        await bro.yaSetUserSettings('timestampLastClosedB2CBanner');
        await bro.url(`${NAVIGATION.disk.url}?show-b2c-1-banner=1`);
        await bro.yaWaitForVisible(b2cBanner(), 'Промо-баннер b2c не отобразился');
        await bro.yaAssertView('b2c-1-client-promo-banner', b2cBanner());
    });
});
