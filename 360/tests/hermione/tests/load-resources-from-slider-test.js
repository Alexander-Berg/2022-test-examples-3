const { clientDesktopBrowsersList } = require('@ps-int/ufo-hermione/browsers');
const { NAVIGATION } = require('../config').consts;
const listing = require('../page-objects/client-content-listing').common;
const { photo } = require('../page-objects/client-photo2-page').common;
const slider = require('../page-objects/slider').common;
const assert = require('chai').assert;

describe('Подгрузка ресурсов из слайдера ->', () => {
    beforeEach(async function() {
        const bro = this.browser;
        await bro.yaClientLoginFast('yndx-ufo-test-96');
    });

    hermione.skip.in('firefox-desktop', 'Нужен отдельный кейс');
    it('diskclient-4527, 4526: Подгрузка ресурсов в слайдере листинга', async function() {
        // листание большого числа файлов может занимать существенное время
        this.browser.executionContext.timeout(120000);
        const bro = this.browser;
        const isMobile = await bro.yaIsMobile();

        await bro.url(NAVIGATION.disk.url + '/popular3');
        await bro.yaWaitForVisible(listing.listing());

        await bro.yaWaitForHidden(listing.listingSpinner());

        const itemCount = (await bro.execute((itemSelector) => {
            return document.querySelectorAll(itemSelector).length;
        }, listing.listing.item()));
        const initialListingResources = {
            count: isMobile ? 40 : 175,
            firstNotLoaded: isMobile ? '1-46.jpg' : '12-32.jpg'
        };
        assert(itemCount === initialListingResources.count,
            // eslint-disable-next-line max-len
            `Ожидается что при первоначальном открытии прогрузится ${initialListingResources.count} ресурсов, а прогрузилось ${itemCount}`);
        await bro.yaAssertListingHasNot(initialListingResources.firstNotLoaded);

        await bro.yaOpenListingElement('1-1.jpg');
        await bro.yaWaitForVisible(slider.contentSlider.previewImage());

        await bro.yaChangeSliderActiveImage(itemCount);

        await bro.yaWaitForHidden(slider.contentSliderWait());

        assert.equal(await bro.yaGetActiveSliderImageName(bro), initialListingResources.firstNotLoaded);
        await bro.yaAssertListingHas(initialListingResources.firstNotLoaded);
    });

    hermione.only.in(clientDesktopBrowsersList, 'Стрелки переключения есть только на десктопной версии');
    it('diskclient-4535: Переход по прямой ссылке к файлу в слайдере из 2 и более порций листинга', async function() {
        const bro = this.browser;
        this.testpalmId = 'diskclient-4535';

        const fileName = '13-4.jpg';
        await bro.url(`${NAVIGATION.disk.url}/popular3?dialog=slider&idDialog=%2Fdisk%2Fpopular3%2F${fileName}`);
        await bro.yaWaitForVisible(slider.contentSlider.previewImage());

        assert.equal(await bro.yaGetActiveSliderImageName(), fileName);

        // изначально стрелок "вправо-влево" нет
        await bro.yaWaitForHidden(slider.contentSlider.nextImage(), 100);
        await bro.yaWaitForHidden(slider.contentSlider.previousImage(), 100);

        // ждём прогрузки предыдущих порций и появления стрелок
        await bro.yaWaitForVisible(slider.contentSlider.nextImage(), 10000);
        await bro.yaWaitForVisible(slider.contentSlider.previousImage(), 100);

        // проверим что работает переключение вправо
        await bro.click(slider.contentSlider.nextImage());
        assert.equal(await bro.yaGetActiveSliderImageName(), '13-40.jpg');
        // и влево
        await bro.click(slider.contentSlider.previousImage());
        await bro.click(slider.contentSlider.previousImage());
        assert.equal(await bro.yaGetActiveSliderImageName(), '13-39.jpg');
    });

    it('diskclient-4525,4524: Подгрузка ресурсов в слайдере фотосреза', async function() {
        // листание большого числа файлов может занимать существенное время
        this.browser.executionContext.timeout(120000);
        const bro = this.browser;
        const isMobile = await bro.yaIsMobile();

        await bro.url(NAVIGATION.photo.url);
        await bro.yaWaitForVisible(photo.item(), 10000);

        const itemCount = (await bro.execute((itemSelector) => {
            return document.querySelectorAll(itemSelector).length;
        }, photo.item()));
        const initialPhotoResources = {
            count: isMobile ? 77 : 79,
            firstNotLoaded: isMobile ? '5-5.jpg' : '11-4.jpg'
        };

        assert(itemCount === initialPhotoResources.count,
            // eslint-disable-next-line max-len
            `Ожидается что при первоначальном открытии прогрузится ${initialPhotoResources.count} ресурсов, а прогрузилось ${itemCount}`);
        await bro.yaWaitForHidden(photo.itemByName().replace(':title', initialPhotoResources.firstNotLoaded));

        await bro.click(photo.item());
        await bro.yaWaitForVisible(slider.contentSlider.items());

        for (let i = 0; i < itemCount; i++) {
            if (isMobile) {
                await bro.yaPointerPanX(slider.contentSlider.items(), -1);
            } else {
                await bro.click(slider.contentSlider.nextImage());
            }
        }

        await bro.yaWaitForHidden(slider.contentSliderWait());
        await bro.yaWaitForVisible(slider.contentSlider.activePreview.image());

        assert.equal(await bro.yaGetActiveSliderImageName(bro), initialPhotoResources.firstNotLoaded);
    });
});
