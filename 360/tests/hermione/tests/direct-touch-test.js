const { consts, login } = require('../config/index');
const getUser = require('@ps-int/ufo-hermione/helpers/getUser')(login);
const PageObjects = require('../page-objects/public');

describe('Проверка рекламы на мобильных устройствах --> ', () => {
    hermione.only.in('chrome-phone', 'Актуально только для мобильных устройств');
    describe('Проверка рекламы в портретном режиме', () => {
        it('Верхний и нижний директы не отображаются для платных пользователей', async function() {
            const bro = this.browser;

            await bro.url(`${consts.PUBLIC_ADS_PDF_FILE_URL}`);
            await bro.yaWaitForVisible(PageObjects.directMobileTop(), 'Верхний мобильный директ не отобразился у незалогиненного пользователя');
            await bro.yaWaitForVisible(PageObjects.mobileBottomDirect(), 'Нижний мобильный директ не отобразился у незалогиненного пользователя');

            await bro.loginFast(getUser('yndx-ufo-test-oligarh'));
            await bro.url(`${consts.PUBLIC_ADS_PDF_FILE_URL}`);
            await bro.refresh();
            await bro.yaWaitForHidden(PageObjects.directMobileTop(), 'Верхний мобильный директ отобразился у платного пользователя');
            await bro.yaWaitForHidden(PageObjects.mobileBottomDirect(), 'Нижний мобильный директ отобразился у платного пользователя');
        });

        it('Нижний директ не отображается для картинок с превью', async function() {
            const bro = this.browser;

            await bro.url(`${consts.PUBLIC_ADS_JPEG_FILE_URL}`);
            await bro.yaWaitForVisible(PageObjects.directMobileTop(), 'Верхний мобильный директ не отобразился для картинки');
            await bro.yaWaitForHidden(PageObjects.mobileBottomDirect(), 'Нижний мобильный директ отобразился для картинки');
        });

        it('Нижний директ не отображается для загруженных без ошибок видео', async function() {
            const bro = this.browser;

            await bro.url(`${consts.PUBLIC_ADS_VIDEO_FILE_URL}`);
            await bro.yaWaitForVisible(PageObjects.directMobileTop(), 'Верхний мобильный директ не отобразился для видео');
            await bro.yaWaitForHidden(PageObjects.mobileBottomDirect(), 'Нижний мобильный директ отобразился для видео');
        });

        it('Верхний и нижний директы отображаются для страницы с ошибкой', async function() {
            const bro = this.browser;

            await bro.url(`${consts.PUBLIC_404_FILE_URL}`);
            await bro.yaWaitForVisible(PageObjects.directMobileTop(), 'Верхний мобильный директ не отобразился на странице ошибки');
            await bro.yaWaitForVisible(PageObjects.mobileBottomDirect(), 'Нижний мобильный директ не отобразился на странице ошибки');
        });

        it('Верхний и нижний директы отображаются для прочих типов файлов', async function() {
            const bro = this.browser;

            await bro.url(`${consts.PUBLIC_ADS_AUDIO_FILE_URL}`);
            await bro.yaWaitForVisible(PageObjects.directMobileTop(), 'Верхний мобильный директ не отобразился на странице с аудио-файлом');
            await bro.yaWaitForVisible(PageObjects.mobileBottomDirect(), 'Нижний мобильный директ не отобразился на странице с аудио-файлом');

            await bro.url(`${consts.PUBLIC_ADS_PDF_FILE_URL}`);
            await bro.yaWaitForVisible(PageObjects.directMobileTop(), 'Верхний мобильный директ не отобразился на странице с PDF-файлом');
            await bro.yaWaitForVisible(PageObjects.mobileBottomDirect(), 'Нижний мобильный директ не отобразился на странице с PDF-файлом');
        });
    });
});
