const { NAVIGATION } = require('../config').consts;
const { photo } = require('../page-objects/client-photo2-page').common;
const slider = require('../page-objects/slider').common;
const popups = require('../page-objects/client-popups').common;
const { assert } = require('chai');
const { clientDesktopBrowsersList } = require('@ps-int/ufo-hermione/browsers');

/**
 * @param {string} filename
 * @returns {Promise<void>}
 */
async function closeAndWaitForScroll(filename) {
    const bro = this.browser;
    await bro.click(slider.sliderButtons.closeButton());
    await bro.yaWaitForHidden(slider.contentSlider.previewImage());

    const fileAfterScrollSelector = photo.itemByName().replace(':title', filename);
    await bro.yaWaitForVisible(fileAfterScrollSelector);
    await bro.yaAssertInViewport(fileAfterScrollSelector);
}

/**
 * @param {string} startFilename - название файла, который нужно открыть в слайдере
 * @param {string} endFliename - название файла, к которому должен подскролиться фотосрез после закрытия слайдера
 * @param {('left'|'right')} [direction='right'] - направление в котором нужно листать слайдер
 * @returns {Promise<void>}
 */
async function testScrollAfterClose(startFilename, endFliename, direction = 'right') {
    const bro = this.browser;
    const startFile = await bro.$(photo.itemByName().replace(':title', startFilename));

    await startFile.waitForDisplayed({ timeout: 10000 });
    await startFile.click();

    const previewImage = await bro.$(slider.contentSlider.previewImage());

    await previewImage.waitForExist({ timeout: 10000 });
    await bro.yaChangeSliderActiveImage(50, direction);

    assert.equal(await bro.yaGetActiveSliderImageName(), endFliename);

    await closeAndWaitForScroll.call(this, endFliename);
}

/**
 * @param {Object} bro
 */
async function waitPreviewImageDisplayed(bro) {
    await bro.waitUntil(async () => {
        const images = await bro.$$(slider.contentSlider.previewImage());
        const visibleImages = await Promise.all(images.map((image) => image.isDisplayed()));

        return visibleImages.some(Boolean);
    });
}

describe('Слайдер нового фотосреза ->', () => {
    it('diskclient-4335, diskclient-4528: Открытие фото в слайдере фотосреза', async function() {
        const bro = this.browser;
        await bro.yaClientLoginFast('yndx-ufo-test-07');
        this.testpalmId = await bro.yaIsMobile() ? 'diskclient-4335' : 'diskclient-4528';

        await bro.url(NAVIGATION.photo.url);
        await bro.yaWaitForVisible(photo.item(), 10000);
        await bro.click(photo.item());
        await bro.yaWaitForVisible(slider.contentSlider.previewImage());
        await bro.assertView(this.testpalmId, slider.contentSlider());
    });

    it('diskclient-4338: Закрытие слайдера по крестику из Фотосреза', async function() {
        const bro = this.browser;
        await bro.yaClientLoginFast('yndx-ufo-test-07');
        this.testpalmId = 'diskclient-4338';

        await bro.url(NAVIGATION.photo.url);
        await bro.yaWaitForVisible(photo.item(), 10000);
        await bro.click(photo.item());
        await bro.yaWaitForVisible(slider.contentSlider.previewImage());
        await bro.click(slider.sliderButtons.closeButton());
        await bro.yaWaitForHidden(slider.contentSlider.previewImage());
    });

    it('diskclient-4331, diskclient-4270: Листание ресурсов в слайдере фотосреза', async function() {
        const bro = this.browser;
        await bro.yaClientLoginFast('yndx-ufo-test-07');
        const isMobile = await bro.yaIsMobile();
        this.testpalmId = isMobile ? 'diskclient-4331' : 'diskclient-4270';

        await bro.url(NAVIGATION.photo.url);
        await bro.yaWaitForVisible(photo.item(), 10000);
        await bro.click(photo.item());
        await bro.yaWaitForVisible(slider.contentSlider.items());

        await bro.yaChangeSliderActiveImage(20);

        await bro.assertView(this.testpalmId, slider.contentSlider());
    });

    it('diskclient-4542, diskclient-4541: Подскролл к фото после закрытие слайдера при листании большого количества фото в слайдере', async function() {
        // листание большого числа файлов может занимать существенное время
        this.browser.executionContext.timeout(90000);
        const bro = this.browser;
        await bro.yaClientLoginFast('yndx-ufo-test-138');
        const isMobile = await bro.yaIsMobile();
        this.testpalmId = isMobile ? 'diskclient-4542' : 'diskclient-4541';

        await bro.url(NAVIGATION.photo.url);
        await bro.yaWaitForVisible(photo.item(), 10000);

        await bro.click(photo.item());
        await bro.yaWaitForVisible(slider.contentSlider.previewImage());

        await bro.yaChangeSliderActiveImage(50);

        await bro.click(slider.sliderButtons.closeButton());
        await bro.yaWaitForHidden(slider.contentSlider.previewImage());
        // файл, до которого напереключались слайдером и к которому должен был сработать подскролл
        const fileAfterScrollSelector = photo.itemByName().replace(':title', '2018-12-30 20-38-00.JPG');
        await bro.yaWaitForVisible(fileAfterScrollSelector);
        await bro.yaAssertInViewport(fileAfterScrollSelector);
    });
});

describe('Слайдер нового фотосреза с вау-сеткой ->', () => {
    beforeEach(async function() {
        const bro = this.browser;
        await bro.yaClientLoginFast('yndx-ufo-test-143');
        await bro.url(NAVIGATION.photo.url);
        await bro.yaWaitForVisible(photo.item(), 10000);
    });

    it('diskclient-4638, diskclient-4640: [Вау-сетка] Открытие слайдера фото в режиме вау-сетки', async function() {
        const bro = this.browser;
        this.testpalmId = await bro.yaIsMobile() ? 'diskclient-4640' : 'diskclient-4638';

        await bro.click(photo.item());
        await bro.yaWaitForVisible(slider.contentSlider.previewImage());
        await bro.assertView(this.testpalmId, slider.contentSlider());
    });

    it('diskclient-4639, diskclient-4641: [Вау-сетка] Слайдер видео в режиме вау-сетки', async function() {
        const bro = this.browser;
        this.testpalmId = await bro.yaIsMobile() ? 'diskclient-4641' : 'diskclient-4639';

        await bro.click(photo.videoItem());
        await bro.yaWaitForVisible(slider.contentSlider.videoPlayer());
    });

    it('diskclient-4654, diskclient-4658: [Вау-сетка] Закрытые слайдера фото в режиме вау-сетки', async function() {
        const bro = this.browser;
        const isMobile = await bro.yaIsMobile();
        this.testpalmId = isMobile ? 'diskclient-4658' : 'diskclient-4654';

        await bro.click(photo.item());
        await bro.yaWaitForVisible(slider.contentSlider.previewImage());
        await bro.click(slider.sliderButtons.closeButton());
        await bro.yaWaitForHidden(slider.contentSlider.previewImage());
    });

    it('diskclient-4642, diskclient-4643: [Вау-сетка] Листание в слайдере в режиме вау-сетки', async function() {
        const bro = this.browser;
        const isMobile = await bro.yaIsMobile();
        this.testpalmId = isMobile ? 'diskclient-4643' : 'diskclient-4642';

        await bro.click(photo.item());
        await bro.yaWaitForVisible(slider.contentSlider.items());

        await bro.yaChangeSliderActiveImage(25);

        assert.equal(await bro.yaGetActiveSliderImageName(), '2019-06-04 13-08-46.JPG');

        await bro.yaChangeSliderActiveImage(5, 'left');

        assert.equal(await bro.yaGetActiveSliderImageName(), '2019-06-04 13-09-23.JPG');

        await bro.assertView(this.testpalmId, slider.contentSlider());
    });

    it('diskclient-4646, diskclient-4647: [Вау-сетка] Автопроскролливание к ресурсу после листания в слайдере', async function() {
        const bro = this.browser;
        const isMobile = await bro.yaIsMobile();
        this.testpalmId = isMobile ? 'diskclient-4647' : 'diskclient-4646';

        await testScrollAfterClose.call(this, '13-10.jpg', '2019-06-04 13-05-54.JPG');
    });
});

describe('Слайдер в альбомах-срезах ->', () => {
    hermione.only.notIn('chrome-phone-6.0');
    it('diskclient-5252, diskclient-5558: Открытие слайдера в автоальбоме', async function() {
        const bro = this.browser;
        const isMobile = await this.browser.yaIsMobile();
        this.testpalmId = isMobile ? 'diskclient-5558' : 'diskclient-5252';
        await bro.yaClientLoginFast('yndx-ufo-test-170');

        await bro.url(`${NAVIGATION.photo.url}?filter=unbeautiful`);
        await bro.yaWaitForVisible(photo.item(), 10000);
        await bro.click(photo.item());
        await bro.yaWaitForVisible(slider.contentSlider.previewImage());
        assert.equal(await bro.yaGetActiveSliderImageName(), '2017-07-30 21-07-00.JPG');

        await bro.yaAssertView(`${this.testpalmId}-toolbar`, slider.sliderButtons());

        await bro.click(slider.sliderButtons.moreButton());
        await bro.yaWaitForVisible(popups.actionBarMorePopup());
        await bro.pause(500);
        await bro.yaAssertView(`${this.testpalmId}-popup`, popups.actionBarMorePopup.menu());
    });

    it('diskclient-5253, diskclient-5559: Открытие слайдера с безлимитным файлом в автоальбоме', async function() {
        const bro = this.browser;
        await bro.yaClientLoginFast('yndx-ufo-test-229');

        await bro.url(`${NAVIGATION.photo.url}?filter=unbeautiful`);
        await bro.yaWaitForVisible(photo.item(), 10000);
        await bro.click(photo.item());
        await bro.yaWaitForVisible(slider.contentSlider.previewImage());
        assert.equal(await bro.yaGetActiveSliderImageName(), '2019-10-22 18-38-44.JPG');
    });

    it('diskclient-5254, diskclient-5560: Подскролл при закрытии слайдера автоальбома', async function() {
        const bro = this.browser;
        await bro.yaClientLoginFast('yndx-ufo-test-170');
        await bro.url(`${NAVIGATION.photo.url}?filter=unbeautiful`);
        await testScrollAfterClose.call(this, '2017-07-30 21-07-00.JPG', '2017-10-12 15-12-05.JPG');
    });

    it('diskclient-5255, diskclient-5561: Подскролл при закрытии слайдера автоальбома при листании с последнего файла', async function() {
        const bro = this.browser;
        await bro.yaClientLoginFast('yndx-ufo-test-170');
        await bro.url(`${NAVIGATION.photo.url}?filter=unbeautiful`);
        await bro.yaWaitForVisible(photo.item(), 10000);
        await bro.yaScrollToEnd();

        await testScrollAfterClose.call(this, '2000-01-01 09-36-05.JPG', '2004-08-10 04-37-15.JPG', 'left');
    });

    hermione.only.in(clientDesktopBrowsersList);
    it('diskclient-5256, diskclient-5562: Открытие слайдера в автоальбоме по прямому URL', async function() {
        const bro = this.browser;
        await bro.yaClientLoginFast('yndx-ufo-test-170');
        const FILE_NAME = '2012-06-29 10-47-32.JPG';
        // eslint-disable-next-line max-len
        await bro.url(`${NAVIGATION.photo.url}?filter=unbeautiful&dialog=slider&idDialog=%2Fdisk%2FЗагрузки%2F10gb%2F2012%2F${FILE_NAME}`);

        await waitPreviewImageDisplayed(bro);

        assert.equal(await bro.yaGetActiveSliderImageName(), FILE_NAME);

        await bro.yaWaitForVisible(slider.contentSlider.nextImage());

        await bro.yaChangeSliderActiveImage(1);

        await waitPreviewImageDisplayed(bro);

        const NEXT_FILENAME = '2012-05-17 17-06-02.JPG';
        assert.equal(await bro.yaGetActiveSliderImageName(), NEXT_FILENAME);

        await closeAndWaitForScroll.call(this, NEXT_FILENAME);
    });

    it('diskclient-5257, diskclient-5563: Открытие слайдера в автоальбоме по прямому URL для удаленного файла', async function() {
        const bro = this.browser;
        await bro.yaClientLoginFast('yndx-ufo-test-170');
        const FILE_NAME = '2017-04-06 00-38-51.JPG';
        // eslint-disable-next-line max-len
        await bro.url(`${NAVIGATION.photo.url}?filter=unbeautiful&dialog=slider&idDialog=%2Fdisk%2FЗагрузки%2F10gb%2F2017%2F${FILE_NAME}`);

        await bro.yaWaitForVisible(slider.contentSlider.previewImage());
        assert.equal(await bro.yaGetActiveSliderImageName(), FILE_NAME);

        await bro.click(slider.sliderButtons.closeButton());
        await bro.yaWaitForHidden(slider.contentSlider.previewImage());

        await bro.yaAssertScrollEquals(0);
    });
});
