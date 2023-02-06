const getUser = require('@ps-int/ufo-hermione/helpers/getUser')(require('../config/index').login);
const consts = require('../config/index').consts;
const PageObjects = require('../page-objects/public');

describe('Платный пользователь -> ', () => {
    hermione.only.in('chrome-desktop', 'Актуально только для десктопа');
    it('diskpublic-1645: Смоук: Боковой медиа-директ', async function() {
        const bro = this.browser;

        await bro.url(consts.PUBLIC_ADS_PDF_FILE_URL);
        await bro.yaWaitForVisible(PageObjects.directDesktopRight(), 'Боковой медиа-директ не отобразился для неавторизованного');
        await bro.loginFast(getUser('yndx-ufo-test-oligarh'));
        await bro.url(consts.PUBLIC_ADS_PDF_FILE_URL);
        await bro.refresh();
        await bro.yaWaitForHidden(PageObjects.directDesktopRight(), 'Боковой медиа-директ показывается платнику');
    });

    hermione.only.in('chrome-desktop', 'Актуально только для десктопа'); // тачи проверяются в отдельном тесте
    it('diskpublic-1644: diskpublic-1798: Смоук: Полоска директа', async function() {
        const bro = this.browser;

        await bro.url(consts.PUBLIC_ADS_PDF_FILE_URL);

        const promoBanner = await bro.$(PageObjects.appPromoBanner());

        if (await promoBanner.isExisting()) {
            await bro.yaClick(PageObjects.appPromoBanner.closeButton());
        }
        await bro.yaWaitForVisible(PageObjects.directDesktopTop(), 'Полоска директа не отобразилась для неавторизованного');
        await bro.loginFast(getUser('yndx-ufo-test-oligarh'));
        await bro.url(consts.PUBLIC_ADS_PDF_FILE_URL);
        await bro.refresh();
        await bro.yaWaitForHidden(PageObjects.directDesktopTop(), 'Полоска директа показывается платнику');
    });

    it('diskpublic-1667: diskpublic-1817: Смоук: Паблик залимитированного файла под платником', async function() {
        const bro = this.browser;
        const isMobile = await bro.yaIsMobile();

        await bro.loginFast(getUser('yndx-ufo-test-oligarh'));
        await bro.url(consts.PUBLIC_EXE_ANTIFO_FILE_URL);
        await bro.refresh();
        await bro.yaWaitForVisibleIcon(consts.PUBLIC_EXE_ANTIFO_FILE_TYPE);
        await bro.yaAssertFileName(consts.PUBLIC_EXE_ANTIFO_FILE_NAME);
        await bro.yaWaitForHidden((isMobile ? PageObjects.toolbar.snackbarAntiFo() : PageObjects.antiFoTooltip()), `Сообщение для залимитированного файла ${consts.PUBLIC_EXE_ANTIFO_FILE_TYPE} отображается платнику`);
        await bro.yaWaitForHidden((isMobile ? PageObjects.toolbar.saveAndDownloadButton() : PageObjects.desktopToolbar.saveAndDownloadButton()), 'Кнопка "Сохранить и скачать с Яндекс.Диска" отображается под платником');
        await bro.yaWaitForVisible((isMobile ? PageObjects.toolbar.downloadButton() : PageObjects.desktopToolbar.downloadButton()), `Для залимитированного файла под платником ${consts.PUBLIC_EXE_ANTIFO_FILE_TYPE} неотображается кнопка "Скачать"`);
    });
});
