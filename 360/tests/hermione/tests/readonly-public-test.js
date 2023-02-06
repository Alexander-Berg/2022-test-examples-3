const { publicReadOnly } = require('../config');
const PageObjects = require('../page-objects/public');
const { videoPlayer } = require('@ps-int/ufo-hermione/page-objects/video-player').common;

const assertDownloadAndSaveNotVisible = async(bro) => {
    const isMobile = await bro.yaIsMobile();
    if (isMobile) {
        await bro.yaWaitForHidden(PageObjects.toolbar.downloadButton());
        await bro.yaWaitForHidden(PageObjects.toolbar.saveButton());
    } else {
        await bro.yaWaitForHidden(PageObjects.desktopToolbar.downloadButton());
        await bro.yaWaitForHidden(PageObjects.desktopToolbar.saveButton());
    }
};

describe('ReadOnly паблик -> ', () => {
    it('diskpublic-3010, diskpublic-3025: Паблик изображения: отсутствие кнопок сохранения и скачивания', async function() {
        const bro = this.browser;
        const isMobile = await bro.yaIsMobile();
        this.testpalmId = isMobile ? 'diskpublic-3025' : 'diskpublic-3010';
        const { url, name } = publicReadOnly.image;

        await bro.url(url);
        await bro.yaWaitForVisibleImagePreview(name);
        await bro.yaAssertFileName(name);

        await assertDownloadAndSaveNotVisible(bro);

        await bro.assertView(this.testpalmId, PageObjects.publicMain());

        await bro.yaOpenSlider(name);
        await bro.yaWaitForHidden(PageObjects.slider.sliderButtonDownload());
        await bro.yaWaitForHidden(PageObjects.slider.sliderButtonSave());
    });

    it('diskpublic-3011, diskpublic-3026: Паблик видео: отсутствие кнопок сохранения и скачивания', async function() {
        const bro = this.browser;
        const isMobile = await bro.yaIsMobile();
        this.testpalmId = isMobile ? 'diskpublic-3026' : 'diskpublic-3011';
        const { url, name } = publicReadOnly.video;

        await bro.url(url);
        await bro.yaAssertFileName(name);

        await assertDownloadAndSaveNotVisible(bro);
    });

    it('diskpublic-3012, diskpublic-3027: Паблик документа: отсутствие кнопок сохранения и скачивания', async function() {
        const bro = this.browser;
        const isMobile = await bro.yaIsMobile();
        this.testpalmId = isMobile ? 'diskpublic-3027' : 'diskpublic-3012';
        const { url, name } = publicReadOnly.document;

        await bro.url(url);
        await bro.yaWaitForVisibleDocPreview(name);
        await bro.yaAssertFileName(name);

        await assertDownloadAndSaveNotVisible(bro);

        const banner = await bro.$(PageObjects.appPromoBanner());

        if (await banner.isDisplayed()) {
            await bro.yaClick(PageObjects.appPromoBanner.closeButton());
            await banner.waitForDisplayed({ reverse: true });
        }

        await bro.assertView(this.testpalmId, PageObjects.publicMain(), {
            ignoreElements: PageObjects.moreButton() // мигает, собака
        });
    });

    it('diskpublic-3013, diskpublic-3028: Паблик видео: проигрывание видео при запрете скачивания', async function() {
        const bro = this.browser;
        const isMobile = await bro.yaIsMobile();
        this.testpalmId = isMobile ? 'diskpublic-3028' : 'diskpublic-3013';
        const { url, name } = publicReadOnly.video;

        await bro.url(url);
        await bro.yaAssertFileName(name);

        const preview = await bro.$(videoPlayer.preview());
        await preview.waitForDisplayed();

        await bro.yaClick(videoPlayer.overlayButton());
        await bro.yaAssertVideoIsPlaying();
    });

    it('diskpublic-3015, diskpublic-3029: Паблик архива: отсутствие кнопок сохранения и скачивания', async function() {
        const bro = this.browser;
        const isMobile = await bro.yaIsMobile();
        this.testpalmId = isMobile ? 'diskpublic-3029' : 'diskpublic-3015';
        const { url, name } = publicReadOnly.archive;

        await bro.url(url);
        await bro.yaWaitForVisibleIcon(name, 'zip');
        await bro.yaAssertFileName(name);

        await assertDownloadAndSaveNotVisible(bro);
    });

    it('diskpublic-3016, diskpublic-3030: Паблик книги: отсутствие кнопок сохранения и скачивания', async function() {
        const bro = this.browser;
        const isMobile = await bro.yaIsMobile();
        this.testpalmId = isMobile ? 'diskpublic-3030' : 'diskpublic-3016';
        const { url, name } = publicReadOnly.book;

        await bro.url(url);
        await bro.yaWaitForVisibleDocPreview(name);
        await bro.yaAssertFileName(name);

        await assertDownloadAndSaveNotVisible(bro);
    });

    it('diskpublic-3017, diskpublic-3031: Паблик папки: отсутствие кнопок сохранения и скачивания', async function() {
        const bro = this.browser;
        const isMobile = await bro.yaIsMobile();
        this.testpalmId = isMobile ? 'diskpublic-3031' : 'diskpublic-3017';
        const { url } = publicReadOnly.folder;

        await bro.url(url);
        await bro.yaWaitForVisible(PageObjects.listing());

        await assertDownloadAndSaveNotVisible(bro);

        const banner = await bro.$(PageObjects.appPromoBanner());

        if (await banner.isDisplayed()) {
            await bro.yaClick(PageObjects.appPromoBanner.closeButton());
            await bro.yaWaitForHidden(PageObjects.appPromoBanner());
        }

        await bro.assertView(this.testpalmId, PageObjects.publicMain());
    });

    it('diskpublic-3018, diskpublic-3019, diskpublic-3032, diskpublic-3033: Слайдер: отсутствие кнопок сохранения и скачивания', async function() {
        const bro = this.browser;
        const isMobile = await bro.yaIsMobile();
        this.testpalmId = isMobile ? 'diskpublic-3032' : 'diskpublic-3018';
        const { url } = publicReadOnly.folder;

        await bro.url(url);
        await bro.yaWaitForVisible(PageObjects.listing());

        const image = await bro.$(PageObjects.listingItemXpath().replace(/:titleText/g, '2-1.jpg'));
        await image.doubleClick();
        await bro.yaWaitForVisible(PageObjects.slider(), 'Слайдер не открылся');

        await bro.yaWaitForHidden(PageObjects.slider.sliderButtonDownload());
        await bro.yaWaitForHidden(PageObjects.slider.sliderButtonSave());

        await bro.yaClick(PageObjects.slider.sliderButtonMore());
        if (isMobile) {
            await bro.yaWaitForVisible(PageObjects.fileMenuPane());
        } else {
            await bro.yaWaitForVisible(PageObjects.fileMenu());
        }
        await bro.yaWaitForHidden(PageObjects.fileMenuDownload());

        await bro.assertView(this.testpalmId, PageObjects.slider());
    });

    it('diskpublic-3020, diskpublic-3034: Топбар: отсутствие кнопок сохранения и скачивания', async function() {
        const bro = this.browser;
        const isMobile = await bro.yaIsMobile();
        this.testpalmId = isMobile ? 'diskpublic-3032' : 'diskpublic-3018';
        const { url } = publicReadOnly.folder;

        await bro.url(url);
        await bro.yaWaitForVisible(PageObjects.listing());

        const imageSelector = PageObjects.listingItemXpath().replace(/:titleText/g, '2-1.jpg');
        if (isMobile) {
            await bro.yaLongPress(imageSelector);
        } else {
            await bro.yaClick(imageSelector);
        }
        await bro.yaWaitForVisible(PageObjects.actionBar(), 'Топбар не появился');

        // рендерятся по 2 штуки каждой кнопки - для широких и узких экранов
        // проверяем, что не показались ни те, ни другие
        await bro.yaWaitForHidden(PageObjects.actionBar.saveButtonClearInverse());
        await bro.yaWaitForHidden(PageObjects.actionBar.downloadButtonClearInverse());
        await bro.yaWaitForHidden(PageObjects.actionBar.saveButtonTransparent());
        await bro.yaWaitForHidden(PageObjects.actionBar.downloadButtonTransparent());

        await bro.yaAssertView(this.testpalmId, PageObjects.actionBar(), { ignoreElements: [] });
    });

    it('diskpublic-3021, diskpublic-3035: Паблик папки: заглушка аудиофайла', async function() {
        const bro = this.browser;
        const isMobile = await bro.yaIsMobile();
        this.testpalmId = isMobile ? 'diskpublic-3032' : 'diskpublic-3018';
        const { url } = publicReadOnly.folder;

        await bro.url(url);
        await bro.yaWaitForVisible(PageObjects.listing());

        const audio = await bro.$(PageObjects.listingItemXpath().replace(/:titleText/g, 'sample.mp3'));
        await audio.doubleClick();

        await bro.yaWaitForVisible(PageObjects.slider.activeItem.icon());
        await bro.yaWaitForHidden(PageObjects.slider.audioPlayer());

        await bro.yaAssertView(this.testpalmId, PageObjects.slider());
    });

    it('diskpublic-3024, diskpublic-3036: Паблик с запретом скачивания: отсутствие счетчика скачивания', async function() {
        const bro = this.browser;
        const isMobile = await bro.yaIsMobile();
        this.testpalmId = isMobile ? 'diskpublic-3027' : 'diskpublic-3012';
        const { url, name } = publicReadOnly.document;

        await bro.url(url);
        await bro.yaWaitForVisibleDocPreview(name);
        await bro.yaAssertFileName(name);

        await bro.yaClick(PageObjects.moreButton());
        await bro.yaWaitForVisible(PageObjects.fileMenuFileInfo());
        await bro.yaClick(PageObjects.fileMenuFileInfo());
        await bro.yaWaitForVisible(PageObjects.infoBlock());
        await bro.yaWaitForHidden(PageObjects.infoBlock.downloads());
    });
});
