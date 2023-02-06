const { clientDesktopBrowsersList } = require('@ps-int/ufo-hermione/browsers');
const { NAVIGATION } = require('../config').consts;
const { consts } = require('../config');
const { photo } = require('../page-objects/client-photo2-page').common;
const clientNavigation = require('../page-objects/client-navigation');
const { assert } = require('chai');

/**
 * @param {string} lastPhotoName
 * @returns {Promise<void>}
 */
const photoSlicePortionsLoadTest = async function(lastPhotoName) {
    await this.browser.yaScrollToEnd();
    await this.browser.yaAssertInViewport(photo.itemByName().replace(/:title/, lastPhotoName));
};

describe('Фотосрез 2 -> ', () => {
    beforeEach(async function() {
        const bro = this.browser;
        await bro.yaClientLoginFast('yndx-ufo-test-44');
        await bro.url(NAVIGATION.photo.url);
        await bro.yaSetPhotoSliceListingType('tile');
    });

    hermione.only.notIn('chrome-phone-6.0');
    it('diskclient-4812, 1472: Смоук: assertView: отображение фотосреза', async function() {
        const bro = this.browser;
        this.testpalmId = await bro.yaIsMobile() ? 'diskclient-4812' : 'diskclient-1472';

        await bro.yaWaitPhotoSliceItemsInViewportLoad();
        await bro.yaAssertView(this.testpalmId, 'body', {
            ignoreElements: [clientNavigation.desktop.spaceInfoSection()]
        });
    });

    it('diskclient-1471, 4344: подгрузка порций в фотосрезе', async function() {
        await photoSlicePortionsLoadTest.call(this, '2015-01-01 15-18-39.JPG');
    });

    hermione.only.in(clientDesktopBrowsersList);
    it('diskclient-4260: Выбор нескольких файлов', async function() {
        const bro = this.browser;

        await bro.yaWaitForVisible(photo.item.preview());
        await bro.yaSelectPhotoItemByName('Space to roam.mp4', true);
        await bro.yaSelectPhotoItemByName('SpaceX Crew Dragon.mp4');

        await bro.click(photo.itemByName().replace(':title', '12-29.jpg'));

        await bro.yaClickWithPressedKey(photo.itemByName().replace(':title', '13-37.jpg'), consts.KEY_SHIFT);

        assert.deepEqual(
            await bro.yaGetSelectedPhotoItemsNames(),
            ['Space to roam.mp4', 'SpaceX Crew Dragon.mp4', '12-29.jpg', '11-6.jpg', '13-37.jpg']
        );

        await bro.yaClickWithPressedKey(photo.itemByName().replace(':title', 'The Talking Tree.mp4'), consts.KEY_SHIFT);

        assert.deepEqual(
            await bro.yaGetSelectedPhotoItemsNames(),
            ['The Talking Tree.mp4', 'Space to roam.mp4', 'SpaceX Crew Dragon.mp4', '1-32.jpg', '12-29.jpg']
        );

        await bro.yaClickWithPressedKey(photo.itemByName().replace(':title', '1-32.jpg'), consts.KEY_SHIFT);

        assert.deepEqual(
            await bro.yaGetSelectedPhotoItemsNames(),
            ['1-32.jpg', '12-29.jpg']
        );
    });

    it('diskclient-6160, 6159: подскролл до кластера', async function() {
        const bro = this.browser;
        await bro.yaClientLoginFast('yndx-ufo-test-103');
        await bro.url(NAVIGATION.photo.url + '?from=1474498948000');

        const itemSelector = photo.itemByName().replace(':title', '2016-09-22 02-02-27.PNG');
        await bro.yaWaitForVisible(itemSelector);
        await bro.yaAssertInViewport(itemSelector);
    });
});

describe('Фотосрез 2 -> ', () => {
    it('diskclient-4350, 4549: Мультивыделение ресурсов в сетке', async function() {
        const bro = this.browser;
        const firstPhotoName = 'first-file.JPG';
        const secondPhotoName = 'second-file.JPG';
        const thirdPhotoName = 'third-file.heic';
        this.testpalmId = await bro.yaIsMobile() ? 'diskclient-4350' : 'diskclient-4549';

        await bro.yaClientLoginFast('yndx-ufo-test-285');

        await bro.yaOpenSection('photo');
        await bro.yaWaitPhotoSliceItemsInViewportLoad();

        await bro.yaSelectPhotosliceItemByName(firstPhotoName, true);
        await bro.yaSelectPhotosliceItemByName(secondPhotoName, true, true);
        await bro.yaAssertView(`${this.testpalmId}-1`, photo.group());

        await bro.yaSelectPhotosliceItemByName(thirdPhotoName, true, true);
        await bro.pause(300); // анимация выделения
        await bro.yaAssertView(`${this.testpalmId}-2`, photo.group());
    });

    hermione.skip.notIn('', 'https://st.yandex-team.ru/CHEMODAN-81780');
    it('diskclient-4603, 5404: [Вау-сетка] Смена режима просмотра фото в фотосрезе с выделенными ресурсами', async function() {
        const bro = this.browser;
        const firstPhotoName = '1.heic';
        const secondPhotoName = '2.JPG';
        const thirdPhotoName = '3.heic';
        const fourthPhotoName = '4.heic';
        this.testpalmId = await bro.yaIsMobile() ? 'diskclient-4603' : 'diskclient-5404';

        await bro.yaClientLoginFast('yndx-ufo-test-270');

        await bro.yaOpenSection('photo');
        await bro.yaSetPhotoSliceListingType('wow');
        await bro.yaWaitPhotoSliceItemsInViewportLoad();

        await bro.yaSelectPhotosliceItemByName(firstPhotoName, true, true);
        await bro.yaSelectPhotosliceItemByName(thirdPhotoName, true, true);

        await bro.yaSetPhotoSliceListingType('tile');

        const selectedPhotosNames = await bro.yaGetSelectedPhotoItemsNames();
        assert.sameMembers(selectedPhotosNames, [firstPhotoName, thirdPhotoName]);

        await bro.yaSelectPhotosliceItemByName(secondPhotoName, true, true);
        await bro.yaSelectPhotosliceItemByName(fourthPhotoName, true, true);

        await bro.yaSetPhotoSliceListingType('wow');
        await bro.yaAssertView(`${this.testpalmId}-1`, photo.group());
    });
});

describe('Фотосрез с вау-сеткой -> ', () => {
    beforeEach(async function() {
        const bro = this.browser;
        await bro.yaClientLoginFast('yndx-ufo-test-143');
        await bro.url(NAVIGATION.photo.url);
        await bro.yaSetPhotoSliceListingType('wow');
    });

    it('diskclient-4666, diskclient-4667: [Вау-сетка] Отображение вау-сетки в фотосрезе', async function() {
        const bro = this.browser;
        this.testpalmId = await bro.yaIsMobile() ? 'diskclient-4667' : 'diskclient-4666';

        await bro.yaWaitPhotoSliceItemsInViewportLoad();
        await bro.yaAssertView(this.testpalmId, photo());
    });

    it('diskclient-4659, diskclient-4660: [Вау-сетка] Подгрузка порций в фотосрезе с вау-сеткой', async function() {
        await photoSlicePortionsLoadTest.call(this, '2018-04-19 21-22-19.JPG');
    });

    hermione.only.in(clientDesktopBrowsersList);
    it('diskclient-4668: Выделение нескольких фото с Shift в фотосрезе в режиме вау-сетки', async function() {
        const bro = this.browser;
        this.testpalmId = 'diskclient-4668';

        await bro.yaWaitForVisible(photo.item.preview());
        await bro.yaSelectPhotoItemByName('13-10.jpg', true);
        await bro.yaSelectPhotoItemByName('2019-06-17 19-48-44.JPG');

        await bro.click(photo.itemByName().replace(':title', '2019-06-17 19-48-38.JPG'));

        await bro.yaClickWithPressedKey(photo.itemByName().replace(':title', '2019-06-17 17-40-50.JPG'),
            consts.KEY_SHIFT);

        assert.deepEqual(
            await bro.yaGetSelectedPhotoItemsNames(),
            [
                '13-10.jpg', '2019-06-17 19-48-44.JPG', '2019-06-17 19-48-38.JPG',
                '2019-06-17 17-57-15.JPG', '2019-06-17 17-40-53.JPG', '2019-06-17 17-40-50.JPG'
            ]
        );

        await bro.yaClickWithPressedKey(photo.itemByName().replace(':title', '2019-06-17 17-40-53.JPG'),
            consts.KEY_SHIFT);

        assert.deepEqual(
            await bro.yaGetSelectedPhotoItemsNames(),
            [
                '13-10.jpg', '2019-06-17 19-48-44.JPG', '2019-06-17 19-48-38.JPG',
                '2019-06-17 17-57-15.JPG', '2019-06-17 17-40-53.JPG'
            ]
        );
    });

    it('diskclient-6162, 6161: подскролл до обычного кластера в вау-сетке', async function() {
        const bro = this.browser;
        await bro.url(NAVIGATION.photo.url + '?from=1524162145000');

        const itemSelector = photo.itemByName().replace(':title', '2018-04-19 21-22-51.JPG');
        await bro.yaWaitForVisible(itemSelector);
        await bro.yaAssertInViewport(itemSelector);
    });

    it('diskclient-6162, 6161: подскролл до объединённого кластера в вау-сетке', async function() {
        const bro = this.browser;
        await bro.url(NAVIGATION.photo.url + '?from=1559385778000');

        const itemSelector = photo.itemByName().replace(':title', '94-10.jpg');
        await bro.yaWaitForVisible(itemSelector);
        await bro.yaAssertInViewport(itemSelector);
    });
});

describe('Фотосрез с вау-сеткой -> ', () => {
    it('diskclient-4644, 4645: [Вау-сетка] Скролл фотосреза в режиме вау-сетки', async function() {
        const bro = this.browser;
        const photoNames = ['IMG_20191129_152928.jpg', 'IMG_20191129_152917.jpg'];

        await bro.yaClientLoginFast('yndx-ufo-test-272');

        await bro.yaOpenSection('photo');
        await bro.yaWaitPhotoSliceItemsInViewportLoad();

        for (const photoName of photoNames) {
            const photoSelector = photo.itemByName().replace(':title', photoName);

            await bro.scroll(photoSelector);
            await bro.yaAssertInViewport(photoSelector);
        }
    });
});

describe('Фотосрез -> ', () => {
    it('diskclient-6172, 6173: Отображение фотосреза на не-RU локали', async function() {
        const bro = this.browser;
        this.testpalmId = await bro.yaIsMobile() ? 'diskclient-6173' : 'diskclient-6172';

        await bro.yaClientLoginFast('yndx-ufo-test-216');

        await bro.url('/client/photo');
        await bro.yaWaitPhotoSliceItemsInViewportLoad();

        await bro.yaAssertView(this.testpalmId, photo());
    });
});
