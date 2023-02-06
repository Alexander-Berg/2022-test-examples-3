const getUser = require('@ps-int/ufo-hermione/helpers/getUser')(require('../config/index').login);
const PageObjects = require('../page-objects/public');
const { publicPictures } = require('../config/index');
const { getUrlWithControlTestId } = require('../helpers/url');
const { publicDesktopBrowsersList } = require('@ps-int/ufo-hermione/browsers');
const { assert } = require('chai');

let swipeHeight = 0;

const doTest = ({ type, name, url: originalUrl, diskpublic }) => {
    const url = getUrlWithControlTestId(originalUrl);

    describe(`${diskpublic} Паблик картинки ${type} -> `, () => {
        beforeEach(async function() {
            const { height } = await this.browser.getWindowSize();

            swipeHeight = Math.trunc(height / 3);
        });

        it('Просмотр картинки в неавторизованном состоянии', async function() {
            const bro = this.browser;

            await bro.url(url);
            await bro.yaWaitForVisibleImagePreview(type);
            await bro.yaAssertFileName(name);
            await bro.yaOpenSlider(type);
            await bro.yaClick(PageObjects.slider.sliderButtonX());
            await bro.yaWaitForSliderClosed(type);
        });

        it('AssertView: Проверка отображения паблика картинки в неавторизованном состоянии', async function() {
            const bro = this.browser;

            await bro.url(url);
            await bro.yaWaitForVisibleImagePreview(type);
            await bro.yaAssertView('image-file', 'body');
        });

        it('AssertView: Проверка отображения слайдера картинки в неавторизованном состоянии', async function() {
            const bro = this.browser;

            await bro.url(url);
            await bro.yaWaitForVisibleImagePreview(type);
            await bro.yaOpenSlider(type);
            await bro.yaWaitPreviewsLoaded(PageObjects.slider.activeItem.previewInSlider());
            await bro.yaAssertView('slider-image-file', 'body');
        });
    });
};

publicPictures.forEach(doTest);

const doSwipeTest = ({ type, url }) => {
    hermione.skip.in(publicDesktopBrowsersList, 'Актуально только для мобильных браузеров');
    describe(`Мобильный паблик картинки ${type} -> `, () => {
        beforeEach(async function() {
            const { height } = await this.browser.getWindowSize();

            swipeHeight = Math.trunc(height / 3);
        });

        hermione.skip.notIn('', 'https://st.yandex-team.ru/CHEMODAN-84765');
        it('Закрытие слайдера по свайпу вниз', async function() {
            const bro = this.browser;
            await bro.url(url);
            await bro.yaOpenSlider(type);
            await bro.yaWaitPreviewsLoaded(PageObjects.slider.activeItem.previewInSlider());
            await bro.swipeDown(PageObjects.slider.activeItem.previewInSlider(), swipeHeight);
            await bro.yaWaitForSliderClosed(type);
        });

        hermione.skip.notIn('', 'https://st.yandex-team.ru/CHEMODAN-84765');
        it('Закрытие слайдера по свайпу вверх', async function() {
            const bro = this.browser;
            await bro.url(url);
            await bro.yaOpenSlider(type);
            await bro.yaWaitPreviewsLoaded(PageObjects.slider.activeItem.previewInSlider());
            await bro.swipeUp(PageObjects.slider.activeItem.previewInSlider(), swipeHeight);
            await bro.yaWaitForSliderClosed(type);
        });
    });
};

publicPictures.forEach(doSwipeTest);

const jpegPicture = publicPictures.find((picture) => picture.type === 'jpg');
describe(`Паблик картинки ${jpegPicture.type} -> `, () => {
    it('diskpublic-1650: diskpublic-1810: Смоук: Сохранение на Диск', async function() {
        const bro = this.browser;

        await bro.url(jpegPicture.url);
        await bro.yaSaveToDiskWithAuthorization(getUser('test'));
    });

    it('diskpublic-553: diskpublic-1795: Смоук: Скачивание изображения без авторизации', async function() {
        const bro = this.browser;
        const isMobile = await bro.yaIsMobile();

        await bro.url(jpegPicture.url);

        const url = await bro.yaGetDownloadUrlFromAction(async() => {
            const button = await bro.$(isMobile ?
                PageObjects.toolbar.downloadButton() :
                PageObjects.desktopToolbar.downloadButton());

            await button.click();
        });

        assert(
            /downloader\.disk\.yandex\.ru\/disk\/.+&filename=[^\/]+\.JPEG\.jpg&/.test(url),
            'Некорректный url для скачивания'
        );
    });
});
