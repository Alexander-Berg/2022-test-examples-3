const getUser = require('@ps-int/ufo-hermione/helpers/getUser')(require('../config/index').login);
const pubDocs = require('../config/index').publicArchives;
const consts = require('../config/index').consts;
const PageObjects = require('../page-objects/public');
const { getUrlWithControlTestId } = require('../helpers/url');
const { assert } = require('chai');

const doTest = ({ type, name, url, icon, diskpublic }) => {
    describe(`${diskpublic} Паблик архива ${type} -> `, () => {
        it('Просмотр архива в неавторизованном состоянии', async function() {
            const bro = this.browser;

            await bro.url(url);
            await bro.yaWaitForVisibleIcon(type, icon);
            await bro.yaAssertFileName(name);
            await bro.yaWaitForVisibleToolbarButtons(type);
            await bro.yaClickAndAssertNewTabUrl(
                PageObjects.docPreview(), {
                    linkShouldContain: 'docviewer',
                    message: type + ' - Не открылся DV при клике на иконку архива'
                }
            );
        });
    });
};

pubDocs.forEach(doTest);

describe('Паблик аудио -> ', () => {
    it('diskpublic-531: diskpublic-2302: AssertView: Просмотр аудиофайла в неавторизованном состоянии', async function() {
        const bro = this.browser;

        await bro.url(getUrlWithControlTestId(consts.PUBLIC_AUDIO_FILE_URL));
        await bro.yaWaitForVisibleIcon(consts.PUBLIC_AUDIO_FILE_TYPE, 'audio');
        await bro.yaAssertFileName(consts.PUBLIC_AUDIO_FILE_NAME, PageObjects.audioFileName());
        await bro.yaWaitForVisibleToolbarButtons(consts.PUBLIC_AUDIO_FILE_TYPE);
        await bro.yaWaitForVisible(PageObjects.audioPlayer.duration());
        await bro.assertView('audio-file', 'body');
    });
    it('diskpublic-2304: diskpublic-2303: Сохранение на Диск аудиофайла в неавторизованном состоянии', async function() {
        await this.browser.url(consts.PUBLIC_AUDIO_FILE_URL);
        await this.browser.yaSaveToDiskWithAuthorization(getUser('test'));
    });
});

describe('Паблик книги -> ', () => {
    hermione.skip.in('chrome-phone', 'https://st.yandex-team.ru/CHEMODAN-77646');
    it('diskpublic-597: diskpublic-423: Попытка Сохранения файла на Диск под переполненным юзером', async function() {
        const bro = this.browser;
        const isMobile = await bro.yaIsMobile();

        await bro.url(consts.PUBLIC_FB2_FILE_URL);
        await bro.yaClick((isMobile ? PageObjects.toolbar.saveButton() : PageObjects.desktopToolbar.saveButton()));
        await bro.login(getUser('fullOfUser'));
        await bro.yaWaitForVisible(PageObjects.toolbar.snackbarError(), 'Сообщение об ошибке не отобразилось');
    });

    it('diskpublic-529: diskpublic-176: Просмотр книги в неавторизованном состоянии', async function() {
        const bro = this.browser;

        await bro.url(consts.PUBLIC_FB2_FILE_URL);
        await bro.yaWaitForVisibleDocPreview(consts.PUBLIC_FB2_FILE_TYPE);
        await bro.yaAssertFileName(consts.PUBLIC_FB2_FILE_NAME);
        await bro.yaWaitForVisibleToolbarButtons(consts.PUBLIC_FB2_FILE_TYPE);
        await bro.yaClickAndAssertNewTabUrl(
            PageObjects.docPreview(), {
                linkShouldContain: 'docviewer',
                message: consts.PUBLIC_FB2_FILE_TYPE + ' - Не открылся DV при клике на превью'
            }
        );
    });
});

describe('Паблик видео -> ', () => {
    it('diskpublic-1664: diskpublic-173: AssertView: Просмотр видеофайла в неавторизованном состоянии', async function() {
        const bro = this.browser;

        await bro.url(consts.PUBLIC_VIDEO_FILE_URL);
        await bro.yaScroll(0);
        await bro.yaAssertFileName(consts.PUBLIC_VIDEO_FILE_NAME);
        await bro.yaWaitForVisibleToolbarButtons(consts.PUBLIC_VIDEO_FILE_TYPE);
        await bro.yaAssertView('video-file', 'body');
    });

    it('diskpublic-1813: diskpublic-2305: Cкачивание видеофайла в неавторизованном состоянии', async function() {
        const bro = this.browser;
        const isMobile = await bro.yaIsMobile();

        await bro.url(consts.PUBLIC_VIDEO_FILE_URL);

        const url = await bro.yaGetDownloadUrlFromAction(async() => {
            const button = await bro.$(isMobile ?
                PageObjects.toolbar.downloadButton() :
                PageObjects.desktopToolbar.downloadButton());

            await button.click();
        });

        assert(
            /downloader\.disk\.yandex\.ru\/disk\/.+&filename=[^\/]+\.MOV&/.test(url),
            'Некорректный url для скачивания'
        );
    });
});

describe('Паблик залимитированного исполняемого файла -> ', () => {
    it('diskpublic-537: diskpublic-223: Смоук: Просмотр залимитированного исполняемого файла в неавторизованном состоянии', async function() {
        const bro = this.browser;
        const isMobile = await bro.yaIsMobile();
        const saveButtonTitle = isMobile ? 'Сохранить и скачать с Яндекс.Диска' : 'Сохранить на Яндекс.Диск';

        await bro.url(consts.PUBLIC_EXE_ANTIFO_FILE_URL);
        await bro.yaWaitForVisibleIcon(consts.PUBLIC_EXE_ANTIFO_FILE_TYPE);
        await bro.yaAssertFileName(consts.PUBLIC_EXE_ANTIFO_FILE_NAME);
        await bro.yaWaitForVisible(
            isMobile ? PageObjects.toolbar.snackbarAntiFo() : PageObjects.mail360AntiFOTooltip(),
            `Сообщение для залимитированного файла ${consts.PUBLIC_EXE_ANTIFO_FILE_TYPE} не отобразилось`
        );
        await bro.yaWaitForVisible(
            isMobile ? PageObjects.toolbar.saveAndDownloadButton() : PageObjects.desktopToolbar.saveButton(),
            `Кнопка "${saveButtonTitle}" для файла ${consts.PUBLIC_EXE_ANTIFO_FILE_TYPE} не отобразилось`
        );
        await bro.yaWaitForHidden(
            PageObjects.toolbar.downloadButton(),
            `Для залимитированного файла на мобилах ${consts.PUBLIC_EXE_ANTIFO_FILE_TYPE} отображается кнопка "Скачать"`
        );
    });

    it('diskpublic-1666: diskpublic-1816: Смоук: Сохранить и скачать', async function() {
        const bro = this.browser;
        const isMobile = await bro.yaIsMobile();

        await bro.url(consts.PUBLIC_EXE_ANTIFO_FILE_URL);
        await bro.yaWaitForVisibleIcon(consts.PUBLIC_EXE_ANTIFO_FILE_TYPE);

        if (isMobile) {
            await bro.yaSaveAndDownloadWithAuthorization(getUser('test'));
        } else {
            await bro.yaSaveToDiskWithAuthorization(getUser('test'));
        }
    });

    it('AssertView: отображение залимитированного исполняемого файла для безлогина из РФ', async function() {
        const bro = this.browser;
        const isMobile = await bro.yaIsMobile();
        await bro.url(consts.PUBLIC_EXE_ANTIFO_FILE_URL);
        await bro.yaWaitForVisibleIcon(consts.PUBLIC_EXE_ANTIFO_FILE_TYPE);
        await bro.yaWaitForVisible(isMobile ?
            PageObjects.toolbar.snackbarAntiFo() : PageObjects.mail360AntiFOTooltip());
        await bro.assertView('antifo-file-ru', 'body');
    });

    it('AssertView: отображение залимитированного исполняемого файла для безлогина из заграницы', async function() {
        const bro = this.browser;
        const isMobile = await bro.yaIsMobile();
        await bro.setCookies({ name: 'yandex_gid', value: '10502', domain: '.yandex.ru' }); // Paris
        await bro.url(consts.PUBLIC_EXE_ANTIFO_FILE_URL);
        await bro.yaWaitForVisibleIcon(consts.PUBLIC_EXE_ANTIFO_FILE_TYPE);
        if (isMobile) {
            await bro.yaWaitForVisible(PageObjects.toolbar.snackbarAntiFo());
        } else {
            await bro.yaWaitForVisible(PageObjects.antiFoTooltip());
        }
        await bro.assertView('antifo-file-abroad', 'body');
    });
});

describe('Паблик залимитированного файла с промо Почты 360 -> ', () => {
    /**
     * Проверка тултипа с промо Почты 360 для АнтиФО
     */
    async function testMail360AntiFOTooltip() {
        const bro = this.browser;
        const isMobile = await bro.yaIsMobile();
        await bro.yaWaitForVisible(PageObjects.mail360AntiFOTooltip(),
            'Промо тултип Почты 360 для антифо не отобразился в 1ый раз');
        await bro.yaClick(PageObjects.mail360AntiFOTooltip.close());
        await bro.yaWaitForHidden(PageObjects.mail360AntiFOTooltip());
        await bro.yaClick(isMobile ? PageObjects.toolbar.downloadButton() : PageObjects.desktopToolbar.downloadButton());
        await bro.yaWaitForVisible(PageObjects.mail360AntiFOTooltip(),
            'Промо тултип Почты 360 для антифо не отобразился после нажатия кнопки скачать');

        await bro.yaClickAndAssertNewTabUrl(PageObjects.mail360AntiFOTooltip.content(), {
            linkShouldContain: 'mail360.yandex.'
        });
    }

    hermione.only.in('chrome-desktop', 'Промка только для десктопа');
    it('diskpublic-2931: Промо залимитированного медиаресурса', async function() {
        const bro = this.browser;
        this.testpalmId = 'diskpublic-2931';

        await bro.url(consts.PUBLIC_IMG_ANTIFO_FILE_URL);
        await testMail360AntiFOTooltip.call(this);
    });

    hermione.only.in('chrome-desktop', 'Промка только для десктопа');
    it('diskpublic-2932: Промо залимитированного ресурса', async function() {
        const bro = this.browser;
        this.testpalmId = 'diskpublic-2932';

        await bro.url(consts.PUBLIC_DOC_ANTIFO_FILE_URL);
        await testMail360AntiFOTooltip.call(this);
    });

    hermione.only.in('chrome-desktop', 'Промка только для десктопа');
    it('diskpublic-2933: Промо залимитированного видео', async function() {
        const bro = this.browser;
        this.testpalmId = 'diskpublic-2933';

        await bro.url(consts.PUBLIC_VIDEO_ANTIFO_FILE_URL);
        await testMail360AntiFOTooltip.call(this);
    });
});
