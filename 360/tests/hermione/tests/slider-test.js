const { clientDesktopBrowsersList } = require('@ps-int/ufo-hermione/browsers');
const listing = require('../page-objects/client-content-listing').common;
const slider = require('../page-objects/slider');
const { videoPlayer, streamPlayer } = require('@ps-int/ufo-hermione/page-objects/video-player').common;
const popups = require('../page-objects/client-popups');
const { photo } = require('../page-objects/client-photo2-page').common;
const { consts } = require('../config');
const { assert } = require('chai');
const { photoItemByName } = require('../page-objects/client');
const publicPageObjects = require('../page-objects/public');
const { retriable } = require('@ps-int/ufo-hermione/helpers/retries');
const navigation = require('../page-objects/client-navigation');
const { NAVIGATION } = require('../config').consts;

/**
 * @typedef { import('@ps-int/ufo-hermione/types').Browser } Browser
 */

const IMAGE_FILE_NAME = '222.jpg';
const AUDIO_FILE_NAME = 'Artillery.mp3';
const VIDEO_FILE_NAME = 'Крошки.mp4';
const PHTOSTREAM_VIDEO_FILE_NAME = '2019-05-10 12-47-17.MP4';
const UNLIM_VIDEO_FILE_NAME = '2019-05-10 13-28-55.MP4';
const UNLIM_IMAGE_FILE_NAME = '2019-05-10 13-27-35.JPG';
const ATTACH_VIDEO_FILE_NAME = 'attach.mov';
const ATTACH_IMAGE_FILE_NAME = 'output-0 copy.jpg';

describe('Слайдер ->', () => {
    beforeEach(async function() {
        const bro = this.browser;

        await bro.yaClientLoginFast('yndx-ufo-test-01');
    });

    it('diskclient-1225, 1171: Смоук: assertView: открытие слайдера', async function() {
        const bro = this.browser;
        const testpalmId = await bro.yaIsMobile() ? 'diskclient-1225' : 'diskclient-1171';
        this.testpalmId = testpalmId;

        await bro.yaWaitForHidden(listing.listingSpinner());
        await bro.yaOpenListingElement(IMAGE_FILE_NAME);
        await bro.waitForVisible(slider.common.contentSlider.previewImage());
        await bro.assertView(testpalmId, slider.common.contentSlider());
    });

    it('diskclient-1515, 1152: Смоук: assertView: закрытие слайдера', async function() {
        const bro = this.browser;
        const isMobile = await bro.yaIsMobile();
        const testpalmId = isMobile ? 'diskclient-1515' : 'diskclient-1152';
        this.testpalmId = testpalmId;

        await bro.yaOpenListingElement(IMAGE_FILE_NAME);
        await bro.waitForVisible(slider.common.contentSlider.previewImage());
        await bro.click(slider.common.sliderButtons.closeButton());
        await bro.yaWaitForHidden(slider.common.contentSlider.previewImage());
        await bro.waitForVisible(listing.listing());
        if (!isMobile) {
            await bro.yaWaitActionBarDisplayed();
        }
    });

    /**
     * @param {Browser} bro
     * @param {string} exitType
     */
    const checkBackButton = async function(bro, exitType = 'click') {
        // откроем слайдер
        await bro.yaOpenListingElement(IMAGE_FILE_NAME);
        await bro.waitForVisible(slider.common.contentSlider.previewImage());
        // закроем слайдер кликом или кнопкой Esc
        if (exitType === 'esc') {
            await bro.keys('Escape');
        } else {
            await bro.click(slider.common.sliderButtons.closeButton());
        }
        await bro.yaWaitForHidden(slider.common.contentSlider());
        // нажали назад
        await bro.back();
        //  слайдер не должен открываться по кнопке "назад" после закрытия
        await bro.yaWaitForHidden(slider.common.contentSlider());
    };

    it('diskclient-3386, 3385: Работа браузерной кнопки Назад в слайдере', async function() {
        const bro = this.browser;
        const isMobile = await bro.yaIsMobile();
        this.testpalmId = isMobile ? 'diskclient-3386' : 'diskclient-3385';

        // откроем слайдер
        await bro.yaOpenListingElement(IMAGE_FILE_NAME);
        await bro.waitForVisible(slider.common.contentSlider.previewImage());
        // нажали назад
        await bro.back();
        // сладер должен скрываться по кнопке "назад"
        await bro.yaWaitForHidden(slider.common.contentSlider());
        //  слайдер не должен открываться по кнопке "назад" после закрытия кликом
        await checkBackButton(bro);
    });

    hermione.only.in(clientDesktopBrowsersList);
    it('diskclient-5101: Работа браузерной кнопки Назад в слайдере по esc', async function() {
        const bro = this.browser;
        this.testpalmId = 'diskclient-5101';
        //  слайдер не должен открываться по кнопке "назад" после закрытия по esc
        await checkBackButton(bro, 'esc');
    });

    it('diskclient-1231, 1146: Смоук: assertView: Превью аудиофайла', async function() {
        const bro = this.browser;
        const testpalmId = await bro.yaIsMobile() ? 'diskclient-1231' : 'diskclient-1146';
        this.testpalmId = testpalmId;

        await bro.yaOpenListingElement(AUDIO_FILE_NAME);
        await bro.waitForVisible(slider.common.contentSlider.audioPlayer());
        await bro.assertView(testpalmId, slider.common.contentSlider());
    });

    hermione.skip.in('chrome-phone', 'Мигает дифф: https://st.yandex-team.ru/CHEMODAN-69145');
    it('diskclient-1230, 1145: Смоук: assertView: Превью видеофайла', async function() {
        const bro = this.browser;
        const testpalmId = await bro.yaIsMobile() ? 'diskclient-1230' : 'diskclient-1145';
        this.testpalmId = testpalmId;

        // при открытии слайдера по прямому урлу автоплей не должен срабатывать
        await this.browser.url('/client/disk?dialog=slider&idDialog=%2Fdisk%2FКрошки.mp4');
        await bro.waitForVisible(videoPlayer.preview(), 1000);
        await bro.yaResetPointerPosition();
        await bro.assertView(testpalmId, slider.common.contentSlider());
    });

    it('diskclient-6117: Автовоспроизведение видео', async function() {
        const bro = this.browser;

        await bro.yaOpenListingElement(VIDEO_FILE_NAME);
        await bro.yaAssertVideoIsPlaying();

        await bro.keys('Left arrow');
        // Слева от видео у этого пользователя лежит аудио-файл. Нужно проверить что слайдер действительно переклюсился
        await bro.yaWaitForVisible(slider.common.contentSlider.audioPlayer());
        await bro.keys('Right arrow');

        // При переходе к видеофайлу автовоспроизведение не должно начинаться
        await bro.waitForVisible(videoPlayer.preview());
        await bro.waitForVisible(slider.common.contentSlider.videoPlayer.overlayButton());
        await bro.yaAssertVideoIsPlaying(false);
    });

    it('diskclient-1444, 1495: Смоук: assertView: Создание публичной ссылки на файл', async function() {
        const bro = this.browser;
        const testpalmId = await bro.yaIsMobile() ? 'diskclient-1444' : 'diskclient-1495';
        this.testpalmId = testpalmId;

        await bro.yaOpenListingElement(IMAGE_FILE_NAME);
        await bro.waitForVisible(slider.common.sliderButtons.shareButton());
        await bro.click(slider.common.sliderButtons.shareButton());
        await bro.waitForVisible(popups.common.shareDialog());
        await bro.pause(500); // анимация нотифайки
        await bro.assertView(testpalmId, slider.common.contentSlider(), {
            ignoreElements: popups.common.shareDialog.textInput()
        });
    });
});

describe('Слайдер ->', () => {
    const testData = {
        user: 'yndx-ufo-test-526',
        firstFileName: '2013-01-05 19-15-08.JPG',
        lastFileName: 'last.JPG',
        filesCount: 20
    };

    it('diskclient-1513, diskclient-1511: Смоук: assertView: листание слайдера влево', async function() {
        const bro = this.browser;
        const isMobile = await bro.yaIsMobile();
        this.testpalmId = isMobile ? 'diskclient-1511' : 'diskclient-1513';

        await bro.yaClientLoginFast(testData.user);
        await bro.yaScrollToEnd();

        await bro.yaOpenListingElement(testData.lastFileName);

        await bro.yaChangeSliderActiveImage(testData.filesCount - 1, 'left');

        assert.equal(await bro.yaGetActiveSliderImageName(), testData.firstFileName);
        await bro.assertView(this.testpalmId, slider.common.contentSlider());
    });

    it('diskclient-1514, diskclient-1512: Смоук: assertView: листание слайдера вправо', async function() {
        const bro = this.browser;
        const isMobile = await bro.yaIsMobile();
        this.testpalmId = isMobile ? 'diskclient-1511' : 'diskclient-1513';

        await bro.yaClientLoginFast(testData.user);

        await bro.yaOpenListingElement(testData.firstFileName);

        await bro.yaChangeSliderActiveImage(testData.filesCount - 1, 'right');

        assert.equal(await bro.yaGetActiveSliderImageName(), testData.lastFileName);
        await bro.assertView(this.testpalmId, slider.common.contentSlider());
    });

    hermione.skip.notIn('', 'https://st.yandex-team.ru/CHEMODAN-72025');
    it('diskclient-1224, diskclient-1254: [Слайдер] Доскролливание к файлу', async function() {
        const bro = this.browser;
        const isMobile = await bro.yaIsMobile();
        this.testpalmId = isMobile ? 'diskclient-1254' : 'diskclient-1224';

        const testData = {
            user: 'yndx-ufo-test-523',
            startFile: {
                name: '2013-01-05 19-15-08.JPG',
                nth: 1
            },
            stopFile: {
                name: '2013-01-09 06-10-39.JPG',
                nth: 18
            }
        };

        const stopFileSelecor = listing.listing.item().concat(`:nth-child(${testData.stopFile.nth})`);

        await bro.yaClientLoginFast(testData.user);

        await bro.yaWaitForVisible(stopFileSelecor);
        await bro.yaAssertInViewport(stopFileSelecor, false);

        await bro.yaOpenListingElement(testData.startFile.name);

        await bro.yaChangeSliderActiveImage(Math.abs(testData.startFile.nth - testData.stopFile.nth), 'right');
        assert.equal(await bro.yaGetActiveSliderImageName(), testData.stopFile.name);

        await bro.click(slider.common.sliderButtons.closeButton());
        await bro.yaWaitForHidden(slider.common.contentSlider.previewImage());

        await bro.yaWaitForVisible(stopFileSelecor);
        await bro.yaAssertInViewport(stopFileSelecor, true);
    });

    it('diskclient-4271, diskclient-4332: Листание ресурсов в слайдере фотосреза. Безлимитные файлы', async function() {
        const bro = this.browser;
        const isMobile = await bro.yaIsMobile();
        this.testpalmId = isMobile ? 'diskclient-4332' : 'diskclient-4271';

        const testDataUnlim = {
            user: 'yndx-ufo-test-27',
            firstFileName: '2019-01-29 16-18-06.JPG',
            lastFileName: '2015-08-05 12-04-32.PNG',
            unlimFilesCount: 32
        };

        await bro.yaClientLoginFast(testDataUnlim.user);

        await bro.url(`${NAVIGATION.photo.url}?filter=photounlim`);
        await bro.yaWaitPhotoSliceItemsInViewportLoad();

        await bro.click(photo.itemByName().replace(':title', testDataUnlim.firstFileName));

        await bro.yaChangeSliderActiveImage(testDataUnlim.unlimFilesCount - 1, 'right');
        assert.equal(await bro.yaGetActiveSliderImageName(), testDataUnlim.lastFileName);
    });

    const testDataCheckSclider = {
        user: 'yndx-ufo-test-519',
        fileName: '1998-07-03 14-56-45.JPG'
    };

    it('diskclient-6122, diskclient-6125: [Слайдер] Открытие слайдера в разделе Последние', async function() {
        const bro = this.browser;

        await bro.yaClientLoginFast(testDataCheckSclider.user);

        await bro.url(NAVIGATION.recent.url);
        await bro.pause(500);
        await bro.yaOpenListingElement(testDataCheckSclider.fileName);

        await bro.waitForVisible(slider.common.contentSlider.previewImage());
        assert.equal(await bro.yaGetActiveSliderImageName(), testDataCheckSclider.fileName);
    });

    it('diskclient-6123, diskclient-6126: [Слайдер] Открытие слайдера в разделе Общий доступ', async function() {
        const bro = this.browser;

        await bro.yaClientLoginFast(testDataCheckSclider.user);

        await bro.url(NAVIGATION.published.url);
        await bro.yaOpenListingElement(testDataCheckSclider.fileName);

        await bro.waitForVisible(slider.common.contentSlider.previewImage());
        assert.equal(await bro.yaGetActiveSliderImageName(), testDataCheckSclider.fileName);
    });

    it('diskclient-6124, diskclient-6127: [Слайдер] Открытие слайдера в папке Загрузки', async function() {
        const bro = this.browser;

        await bro.yaClientLoginFast(testDataCheckSclider.user);

        await bro.url(NAVIGATION.folder('Загрузки').url);
        await bro.yaOpenListingElement(testDataCheckSclider.fileName);

        await bro.waitForVisible(slider.common.contentSlider.previewImage());
        assert.equal(await bro.yaGetActiveSliderImageName(), testDataCheckSclider.fileName);
    });

    it('diskclient-6128, diskclient-6129: [Слайдер] Открытие слайдера в разделе Архив', async function() {
        const bro = this.browser;

        await bro.yaClientLoginFast(testDataCheckSclider.user);

        await bro.url(NAVIGATION.archive.url);
        await bro.yaOpenListingElement(testDataCheckSclider.fileName);

        await bro.waitForVisible(slider.common.contentSlider.previewImage());
        assert.equal(await bro.yaGetActiveSliderImageName(), testDataCheckSclider.fileName);
    });
});

describe('Слайдер ->', () => {
    beforeEach(async function() {
        await this.browser.yaClientLoginFast('yndx-ufo-test-43');
    });

    it('diskclient-6118: [Последние] Автовоспроизведение видео', async function() {
        const bro = this.browser;

        await bro.yaOpenSection('recent');
        await bro.yaOpenListingElement(UNLIM_VIDEO_FILE_NAME);
        await bro.yaAssertVideoIsPlaying();
    });

    hermione.only.notIn('chrome-phone-6.0');
    it('diskclient-6119: [Фото] Автовоспроизведение видео', async function() {
        const bro = this.browser;
        await bro.yaOpenSection('photo');

        const photoSliceVideoResource = photo.itemByName().replace(/:title/, PHTOSTREAM_VIDEO_FILE_NAME);
        await bro.yaWaitForVisible(photoSliceVideoResource);
        await bro.click(photoSliceVideoResource);
        await bro.yaAssertVideoIsPlaying();
    });

    it('diskclient-3604, diskclient-6104, diskclient-6038: [Фото] Автовоспроизведение безлимитного видео и ' +
        'перелистывание к видео в слайдере фотосреза', async function() {
        const bro = this.browser;
        await bro.yaOpenSection('photo');

        const photoSliceVideoResource = photo.itemByName().replace(/:title/, UNLIM_VIDEO_FILE_NAME);
        await bro.yaWaitForVisible(photoSliceVideoResource);
        await bro.click(photoSliceVideoResource);
        await bro.yaAssertVideoIsPlaying();

        await bro.keys('Escape');
        await bro.yaWaitForHidden(slider.common.contentSlider());

        await bro.click(photo.itemByName().replace(/:title/, UNLIM_IMAGE_FILE_NAME));
        await bro.yaWaitForVisible(slider.common.contentSlider());

        await bro.keys('Left arrow');
        await bro.waitForVisible(videoPlayer.preview());
        await bro.waitForVisible(slider.common.contentSlider.videoPlayer.overlayButton());
        await bro.yaAssertVideoIsPlaying(false);
    });

    it('diskclient-6120: [Альбомы] Автовоспроизведение видео', async function() {
        const bro = this.browser;
        await bro.url('/client/albums/5cd6d5012db82947e1aeb4fc');
        await bro.yaWaitAlbumItemsInViewportLoad();

        await bro.click(photoItemByName().replace(':title', '2019-05-10 13-28-55.MP4'));
        await bro.yaAssertVideoIsPlaying();

        await bro.keys('Escape');
        await bro.yaWaitForHidden(slider.common.contentSlider());

        await bro.click(photoItemByName().replace(':title', '2019-05-10 13-27-35.JPG'));
        await bro.yaWaitForVisible(slider.common.contentSlider());

        await bro.keys('Left arrow');
        await bro.waitForVisible(videoPlayer.preview());
        await bro.waitForVisible(slider.common.contentSlider.videoPlayer.overlayButton());
        await bro.yaAssertVideoIsPlaying(false);
    });

    it('diskclient-3601, 3605: [Архив] Автовоспроизведение видео', async function() {
        const bro = this.browser;
        this.testpalmId = await bro.yaIsMobile() ? 'diskclient-3601' : 'diskclient-3605';

        // 1. проверим что при открытии видео оно автовоспроизводится
        await bro.url('/client/attach');
        await bro.yaOpenListingElement(ATTACH_VIDEO_FILE_NAME);
        await bro.yaAssertVideoIsPlaying();

        await bro.keys('Escape');
        await bro.yaWaitForHidden(slider.common.contentSlider());

        // 2. проверим что если открыть слайдер для не-видео и долистать до видео, то тогда видео НЕ автовоспроизводится
        await bro.yaOpenListingElement(ATTACH_IMAGE_FILE_NAME);
        await bro.waitForVisible(slider.common.contentSlider.previewImage());
        await bro.keys('Right arrow');
        await bro.waitForVisible(videoPlayer.preview());
        await bro.waitForVisible(slider.common.contentSlider.videoPlayer.overlayButton());
        await bro.yaAssertVideoIsPlaying(false);
    });
});

describe('Cлайдер ->', () => {
    afterEach(async function() {
        const resourcesToBeRemoved = this.currentTest.ctx.resourcesToBeRemoved || [];
        if (resourcesToBeRemoved.length) {
            await this.browser.url('/client/disk');
            await this.browser.yaDeleteCompletely(resourcesToBeRemoved, { fast: true, safe: true });
        }
    });

    /**
     *
     * @param {boolean} isSingle
     * @returns {Promise<{firstTestFileName: string, secondTestFileName: string, startFolderName: string}>}
     */
    const moveTestFunction = async function(isSingle) {
        const startFolderName = `tmp-${Date.now()}-start-folder`;
        const finishFolderName = `tmp-${Date.now()}-finish-folder`;
        this.currentTest.ctx.resourcesToBeRemoved = [startFolderName, finishFolderName];
        const bro = this.browser;

        await bro.yaClientLoginFast('yndx-ufo-test-403');

        await bro.yaCreateFolders([finishFolderName, startFolderName]); // создаем папки для перемещения
        await bro.yaOpenListingElement(startFolderName);

        const [firstTestFileName, secondTestFileName] = await bro.yaUploadFiles(
            // загружаем файл, который будем перемещать
            isSingle ? ['test-file.jpg'] : ['test-file.jpg', 'test-file.jpg'], { uniq: true }
        );

        await bro.yaOpenListingElement(firstTestFileName);
        await bro.yaWaitForVisible(slider.common.contentSlider(), 'Слайдер не появился на странице.');

        await bro.yaCallActionInSliderToolbar('more');
        await bro.yaCallActionInMoreButtonPopup('move');

        await bro.yaWaitForVisible(slider.common.contentSlider(), 'Слайдер закрылся.');
        await bro.yaSelectFolderInDialogAndApply(finishFolderName);

        // проверяем нотификацию на предмет того, что файл успешно перемещен
        await bro.yaWaitNotificationForResource(
            { name: firstTestFileName, folder: finishFolderName },
            consts.TEXT_NOTIFICATION_FILE_MOVED_TO_FOLDER
        );

        return { firstTestFileName, secondTestFileName, startFolderName };
    };

    it('diskclient-1165, diskclient-1237: копирование файла.', async function() {
        const startFolderName = `tmp-${Date.now()}-start-folder`;
        const finishFolderName = `tmp-${Date.now()}-finish-folder`;
        this.currentTest.ctx.resourcesToBeRemoved = [startFolderName, finishFolderName];
        const bro = this.browser;

        await bro.yaClientLoginFast('yndx-ufo-test-404');

        await bro.yaCreateFolders([finishFolderName, startFolderName]);
        await bro.yaOpenListingElement(startFolderName);

        const testFileName = await bro.yaUploadFiles('test-file.jpg', { uniq: true });
        await bro.yaOpenListingElement(testFileName);
        await bro.yaWaitForVisible(slider.common.contentSlider(), 'Слайдер не появился на странице.');

        await bro.yaCallActionInSliderToolbar('more');
        await bro.yaCallActionInMoreButtonPopup('copy');

        await bro.yaWaitForVisible(slider.common.contentSlider(), 'Слайдер закрылся.');

        await bro.yaSelectFolderInDialogAndApply(finishFolderName);

        // проверяем нотификацию на предмет того, что файл успешно перемещен
        await bro.yaWaitNotificationForResource(
            { name: testFileName, folder: finishFolderName },
            consts.TEXT_NOTIFICATION_FILE_COPIED_TO_FOLDER
        );

        await bro.yaWaitForVisible(slider.common.contentSlider(), 'Слайдер закрылся.');
    });

    hermione.only.in(clientDesktopBrowsersList);
    it('diskclient-1162: редактирование документа.', async function() {
        const bro = this.browser;
        const startFolderName = 'Папка-для-проверки-редактирования-документа';

        await bro.yaClientLoginFast('yndx-ufo-test-243');

        await bro.yaOpenListingElement(startFolderName); // переходим в рабочую папку
        await bro.yaWaitForHidden(listing.listingSpinner());

        const ImageTestFileName = 'test-file-1.jpg';
        const DocumentTestFileName = 'test-file-2.docx';

        await bro.yaOpenListingElement(ImageTestFileName);
        await bro.yaWaitForVisible(slider.common.contentSlider(), 'Слайдер не появился на странице.');

        // кликаем по кнопке перехода к следующему файлу (документу)
        await bro.click(slider.common.contentSlider.nextImage());

        await bro.waitUntil(async() => {
            return await bro.getText(slider.common.contentSlider.activeItem.resourceName()) === DocumentTestFileName;
        }); // ожидаем перехода к документу

        await bro.click(slider.common.contentSlider.activeItem.openButton()); // кликаем по кнопке "Редактировать"

        await bro.waitUntil(async() => {
            const tabs = await bro.getTabIds();
            return tabs.length === 2;
        }); // проверяем, что открывается новое окно

        await bro.yaWaitForVisible(slider.common.contentSlider(), 'Слайдер закрылся.');
    });

    it('diskclient-1161, diskclient-1243: открыть оригинал.', async function() {
        const bro = this.browser;
        const startFolderName = 'Папка-для-проверки-функции-открытия-оригинала-изображения';
        const testFileName = 'test-file-1.jpg';

        await bro.yaClientLoginFast('yndx-ufo-test-243');

        await bro.yaOpenListingElement(startFolderName); // переходим в рабочую папку
        await bro.yaWaitForHidden(listing.listingSpinner());

        await bro.yaOpenListingElement(testFileName);
        await bro.yaWaitForVisible(slider.common.contentSlider(), 'Слайдер не появился на странице.');

        await bro.yaCallActionInSliderToolbar('more');
        await bro.yaCallActionInMoreButtonPopup('showFullsize', false);

        await bro.waitUntil(async() => {
            const tabs = await bro.getTabIds();
            return tabs.length === 2;
        }); // проверяем, что открывается новое окно

        await bro.yaWaitForVisible(slider.common.contentSlider(), 'Слайдер закрылся.');
    });

    it('diskclient-1153, diskclient-1250: поделение файлом.', async function() {
        const bro = this.browser;
        const startFolderName = `tmp-${Date.now()}-start-folder`;
        this.currentTest.ctx.resourcesToBeRemoved = [startFolderName];

        await bro.yaClientLoginFast('yndx-ufo-test-405');
        await bro.yaCreateFolder(startFolderName);

        await bro.yaOpenListingElement(startFolderName); // переходим в рабочую папку
        const testFileName = await bro.yaUploadFiles('test-file.jpg', { uniq: true });

        await bro.yaOpenListingElement(testFileName);
        await bro.yaWaitForVisible(slider.common.contentSlider(), 'Слайдер не появился на странице.');

        await bro.yaCallActionInSliderToolbar('share');
        await bro.yaWaitForVisible(popups.common.shareDialog());
        await bro.pause(500); // задержка для анимации

        await bro.yaWaitForVisible(slider.common.contentSlider(), 'Слайдер закрылся.');

        const publicUrl = await bro.getValue(popups.common.shareDialog.textInput());
        await bro.newWindow(publicUrl); // переходим по ссылке на файл
        await bro.yaWaitForVisible(publicPageObjects.fileName());
        assert.equal(await bro.getText(publicPageObjects.fileName()), testFileName);
    });

    it('diskclient-1151, diskclient-1236: кнопка "More".', async function() {
        const bro = this.browser;
        const isMobile = await bro.yaIsMobile();
        this.testpalmId = isMobile ? 'diskclient-1236' : 'diskclient-1151';
        const startFolderName = 'Папка-для-проверки-кнопки-more';
        const testFileNames = ['test-file.jpg', 'test-file.mp3'];

        await bro.yaClientLoginFast('yndx-ufo-test-243');

        await bro.yaWaitForHidden(listing.listingSpinner());

        await bro.yaOpenListingElement(startFolderName); // переходим в рабочую папку
        await bro.yaWaitForHidden(listing.listingSpinner());

        for (let i = 0; i < testFileNames.length; ++i) {
            await bro.yaOpenListingElement(testFileNames[i]);
            await bro.yaWaitForVisible(slider.common.sliderButtons.moreButton());
            await bro.click(slider.common.sliderButtons.moreButton());
            await bro.yaWaitForVisible(slider.common.sliderMoreButtonPopup());
            await bro.pause(500); // задержка для анимации
            await bro.yaAssertView(`${this.testpalmId}-${i}`, slider.common.sliderMoreButtonPopup());
            await bro.yaExecuteClick(slider.common.sliderButtons.closeButton());
            await bro.yaWaitForHidden(slider.common.contentSlider());
        }
    });

    // TODO: убрать скип после того, как починят CHEMODAN-58941
    // Пускай есть три файла - file01, file03, file05.
    // Если переименовать file03 в file04 (новое название лексикографически больше, порядок тот же),
    // слайдер останется на этом же файле.
    // Если переименовать file03 в file02 (новое название лексикографически меньше, порядок тот же),
    // слайдер перелиснется на file05.
    // Пускай есть один файл - file02.
    // Если переименовать file02 в file03 (новое название лексикографически больше), слайдер останется на этом же файле.
    // Если переименовать file02 в file01 (новое название лексикографически меньше), слайдер закроется.
    hermione.skip.notIn('');
    it('diskclient-3387, diskclient-3388: переименование файла.', async function() {
        const startFolderName = `tmp-${Date.now()}-start-folder`;
        this.currentTest.ctx.resourcesToBeRemoved = [startFolderName];
        const bro = this.browser;
        const isMobile = await bro.yaIsMobile();
        if (await bro.yaIsAndroid()) {
            bro.executionContext.timeout(100000);
        }

        /**
         * @param {string} currentFileName
         * @param {string} newFileName
         * @param {boolean} closeActionBar
         * @returns {Promise<void>}
         */
        const chooseAndRenameFile = async(currentFileName, newFileName, closeActionBar = true) => {
            if (closeActionBar && !isMobile) {
                await bro.yaWaitActionBarDisplayed();
                await bro.yaCloseActionBar();
                await bro.yaWaitActionBarHidden();
            }

            await retriable(
                async() => {
                    await bro.yaOpenListingElement(currentFileName);
                    await bro.yaWaitForVisible(slider.common.contentSlider(), 'Слайдер не появился на странице.');
                },
                2,
                500,
                async() => !(await bro.isVisible(slider.common.contentSlider()))
            );

            await bro.yaCallActionInSliderToolbar('more');
            await bro.yaCallActionInMoreButtonPopup('rename');
            await bro.yaSetResourceNameAndApply(newFileName);
        };

        await bro.yaClientLoginFast('yndx-ufo-test-176');

        await bro.yaCreateFolder(startFolderName);
        await bro.yaOpenListingElement(startFolderName); // переходим в рабочую папку
        await bro.yaWaitForHidden(listing.listingSpinner());

        const testFileNames = await bro.yaUploadFiles(
            ['test-file.jpg', 'test-file.jpg', 'test-file.jpg'], { uniq: true }
        );
        let firstTestFileName = testFileNames[0];
        let secondTestFileName = testFileNames[1];
        const thirdTestFileName = testFileNames[2];

        // проверка 1. [x1, x2, x3] -> [d(2), x1, x3]
        await chooseAndRenameFile(secondTestFileName, 'd.jpg', false);
        secondTestFileName = 'd.jpg';
        assert.equal(await bro.yaGetActiveSliderImageName(), thirdTestFileName);
        await bro.yaCallActionInSliderToolbar('close');
        await bro.yaWaitForHidden(slider.common.contentSlider());

        // проверка 3. [d(2), x1, x3] -> [d(2), t(1), x3]
        await chooseAndRenameFile(firstTestFileName, 't.jpg');
        firstTestFileName = 't.jpg';
        assert.equal(await bro.yaGetActiveSliderImageName(), firstTestFileName);
        await bro.yaCallActionInSliderToolbar('close');
        await bro.yaWaitForHidden(slider.common.contentSlider());

        // проверка 2. [d(2), t(1), x3] -> [a(1), d(2), x3]
        await chooseAndRenameFile(firstTestFileName, 'a.jpg');
        firstTestFileName = 'a.jpg';
        assert.equal(await bro.yaGetActiveSliderImageName(), thirdTestFileName);
        await bro.yaCallActionInSliderToolbar('close');
        await bro.yaWaitForHidden(slider.common.contentSlider());

        // проверка 4. [a(1), d(2), x3] -> [a(1), c(2), x3]
        await chooseAndRenameFile(secondTestFileName, 'c.jpg');
        secondTestFileName = 'a.jpg';
        assert.equal(await bro.yaGetActiveSliderImageName(), secondTestFileName);
        await bro.yaCallActionInSliderToolbar('close');
        await bro.yaWaitForHidden(slider.common.contentSlider());

        // проверка 5. [a(1), d(2), x3] -> [a(1), c(2), x3]
        await chooseAndRenameFile(thirdTestFileName, 'y.jpg');
        await bro.yaWaitForHidden(slider.common.contentSlider(), 'Слайдер не появился закрылся.');
    });

    // TODO: убрать скип после того, как починят CHEMODAN-64372
    hermione.skip.notIn('', 'После удаления файла происходит бесконечная перезагрузка');
    it('diskclient-1149, diskclient-1252: удаление файла.', async function() {
        const startFolderName = `tmp-${Date.now()}-start-folder`;
        this.currentTest.ctx.resourcesToBeRemoved = [startFolderName];
        const bro = this.browser;

        await bro.yaClientLoginFast('yndx-ufo-test-176');

        await bro.yaCreateFolder(startFolderName);
        await bro.yaOpenListingElement(startFolderName); // переходим в рабочую папку

        const [firstTestFileName, secondTestFileName] = await bro.yaUploadFiles(
            ['test-file.jpg', 'test-file.jpg'], { uniq: true }
        );

        await bro.yaWaitForHidden(listing.listingSpinner());

        await bro.yaOpenListingElement(firstTestFileName);
        await bro.yaWaitForVisible(slider.common.contentSlider(), 'Слайдер не появился на странице.');

        await bro.yaCallActionInSliderToolbar('delete');

        await bro.yaWaitNotificationForResource(firstTestFileName, consts.TEXT_NOTIFICATION_FILE_MOVED_TO_TRASH);
        await bro.yaWaitForVisible(slider.common.contentSlider(), 'Слайдер закрыт.');

        // проверяем, что в слайдере открывается следующий файл
        assert.equal(await bro.yaGetActiveSliderImageName(), secondTestFileName);

        await bro.yaCallActionInSliderToolbar('delete');

        await bro.yaWaitNotificationForResource(secondTestFileName, consts.TEXT_NOTIFICATION_FILE_MOVED_TO_TRASH);
        await bro.yaWaitForHidden(slider.common.contentSlider());
    });

    it('diskclient-1160, diskclient-1242: перейти к файлу.', async function() {
        const bro = this.browser;
        const testFileName = 'ывыввы.JPG';
        const selector = listing.listingBody.items() + ':nth-child(117)';

        await bro.yaClientLoginFast('yndx-ufo-test-243');

        /**
         *
         * @param {string} section
         * @param {string} testFileName
         * @param {string} selector
         * @returns {Promise<void>}
         */
        const goToFileTestFunction = async(section, testFileName, selector) => {
            await bro.yaOpenSection(section);
            await bro.yaWaitForHidden(listing.listingSpinner());

            await bro.yaOpenListingElement(testFileName);

            await bro.yaCallActionInSliderToolbar('more');
            await bro.yaCallActionInMoreButtonPopup('goToFile');

            await bro.yaWaitActionBarDisplayed();
            await bro.yaWaitForVisible(selector);
            await bro.pause(500); // анимация скролла
            await bro.yaAssertInViewport(selector);
        };

        // проверяем работоспособность в разделе "Последние"
        await goToFileTestFunction('recent', testFileName, selector);
        if (await bro.yaIsMobile()) {
            await bro.yaCloseActionBar();
        }

        // проверяем работоспособность в разделе "Общий доступ"
        await goToFileTestFunction('shared', testFileName, selector);
    });

    it('diskclient-1148, diskclient-1238: перемещение не единственного файла в папке.', async function() {
        const { secondTestFileName } = await moveTestFunction.call(this, false);
        const bro = this.browser;

        await bro.yaWaitForVisible(slider.common.contentSlider(), 'Слайдер закрылся.');
        assert.equal(await bro.yaGetActiveSliderImageName(), secondTestFileName);
    });

    it('diskclient-3557, diskclient-5068: перемещение единственного в папке файла.', async function() {
        const { firstTestFileName, startFolderName } = await moveTestFunction.call(this, true);
        const bro = this.browser;

        await bro.yaWaitForHidden(slider.common.contentSlider());
        await bro.yaAssertFolderOpened(startFolderName);
        await bro.yaAssertListingHasNot(firstTestFileName);
    });

    it('diskclient-1163, diskclient-1251: скачивание файла.', async function() {
        const startFolderName = 'Папка-для-проверки-функции-скачивания-файла';
        const testFileName = 'test-file-1.jpg';
        const bro = this.browser;

        await bro.yaClientLoginFast('yndx-ufo-test-243');
        await bro.yaOpenSection('disk');
        await bro.yaWaitForHidden(listing.listingSpinner());

        await bro.yaOpenListingElement(startFolderName); // переходим в рабочую папку
        await bro.yaWaitForHidden(listing.listingSpinner());

        await bro.yaOpenListingElement(testFileName);
        await bro.yaWaitForVisible(slider.common.contentSlider(), 'Слайдер не появился на странице.');

        await bro.yaCallActionInSliderToolbar('download');
        const url = await bro.retrieveUrlFromIframe(false);

        assert.isOk(/downloader\.disk\.yandex\.ru.+&filename=test-file-1\.jpg/.test(url));
    });

    hermione.only.in(clientDesktopBrowsersList);
    it('diskclient-5139: Листание в слайдере свежезагруженных ресурсов.', async function() {
        const bro = this.browser;
        await bro.yaClientLoginFast('yndx-ufo-test-281');

        const tempFolderName = 'tmp-' + Date.now();
        this.currentTest.ctx.resourcesToBeRemoved = [tempFolderName];
        await bro.yaCreateFolder(tempFolderName);
        await bro.yaOpenListingElement(tempFolderName);

        const testImageFiles = ['test-image1.jpg', 'test-image2.jpg', 'test-image3.jpg'];
        await bro.yaUploadFiles(testImageFiles);

        await bro.yaOpenListingElement('test-image1.jpg');
        await bro.waitForVisible(slider.common.contentSlider());

        for (const fileName of testImageFiles) {
            await bro.click(slider.common.sliderButtons.infoButton());
            await bro.yaWaitForVisible(popups.common.resourceInfoDropdownContent());

            const fileNameInDropdown = await bro.getText(popups.common.resourceInfoDropdownContent.fileName());

            assert(fileName === fileNameInDropdown, 'filename does not match');

            await bro.keys('Right arrow');
        }
    });

    hermione.skip.notIn('', 'https://st.yandex-team.ru/CHEMODAN-72804');
    it('diskclient-1800, diskclient-5311: [Слайдер] Скрытие контролов по тапу.', async function() {
        const bro = this.browser;
        const testFileName = 'test-file.jpg';
        this.testpalmId = await bro.yaIsMobile() ? 'diskclient-5311' : 'diskclinet-1800';

        await bro.yaClientLoginFast('yndx-ufo-test-283');
        await bro.yaWaitForHidden(listing.listingSpinner());

        await bro.yaOpenListingElement(testFileName);
        await bro.yaWaitForVisible(slider.common.contentSlider());
        const coordsOfImage = await bro.getLocation(slider.common.contentSlider.activePreview.image());

        await bro.yaAssertView(`${this.testpalmId}-1`, slider.common.contentSlider());

        await bro.yaTapOnScreen(Math.floor(coordsOfImage.x), Math.floor(coordsOfImage.y));
        await bro.yaWaitForHidden(slider.common.sliderButtons());
        await bro.yaAssertView(`${this.testpalmId}-2`, slider.common.contentSlider());
    });
});

describe('Просмотр файлов в слайдере -> ', () => {
    const IMAGE_FILE_NAME_PANO = 'panorama.jpg';
    const VIRUS_FILE_NAME = 'eicar.zip';
    const VIRUS_FILE_WORNNING = 'Файл заражён вирусом.';
    const BLOKED_FILE_NAME = 'hehehe123.HEIC';
    // eslint-disable-next-line max-len
    const BLOKED_FILE_WORNNING = 'Файл заблокирован за нарушение условий размещения пользовательского контента на сервисах Яндекса.';
    const EXE_FILE_NAME = 'file.exe';
    const BOOK_FILE_NAME = 'book.fb2';
    const ARCHIVE_FILE_NAME = 'archive.zip';
    const DOCUMENT_FILE_NAME = 'document.txt';
    const VIDEO_FILE_NAME = 'video.mp4';
    const AUDIO_FILE_NAME = 'audio.mp3';
    const PDF_FILE_PREVIEW = 'preview.pdf';
    const PDF_FILE_NO_PREVIEW = 'nopreview.pdf';
    const SLIDER = '/client/disk?dialog=slider&idDialog=/disk/';
    const openButtonSlider = slider.common.contentSlider.activeItem.openButton();
    const openButtonSliderText = openButtonSlider + ' span';
    const editButtonSlider = slider.common.sliderButtons.editButton();
    const editButtonMenu = popups.common.actionPopup.editButton();
    beforeEach(async function() {
        const bro = this.browser;
        await bro.yaClientLoginFast('yndx-ufo-test-160');
    });

    /**
     * @param {Browser} bro
     * @param {string} testpalmId
     * @returns {Promise<void>}
     */
    async function yaAssertViewSlider(bro, testpalmId) {
        await bro.yaWaitForHidden(slider.common.contentSlider.activeItem.spin());
        await bro.yaAssertView(testpalmId, slider.common.contentSlider());
        if (await bro.yaIsAndroid()) {
            await bro.orientation('landscape');
            await bro.yaAssertView(testpalmId + '-landscape', slider.common.contentSlider());
        }
    }

    /**
     * @param {Browser} bro
     * @param {string} fileName
     * @param {string} testpalmId
     * @returns {Promise<void>}
     */
    async function checkFileInSlider(bro, fileName, testpalmId) {
        this.testpalmId = testpalmId;
        await bro.url(`${SLIDER}${fileName}`);
        await yaAssertViewSlider(bro, testpalmId);
        const isMobile = await bro.yaIsMobile();
        const shouldOpenInDocs = !isMobile && fileName.split('.').pop() === 'pdf';
        await bro.yaClickAndAssertNewTabUrl(
            `//button[../../../div[contains(@class, 'resource-name') and text()="${fileName}"]]`, {
                linkShouldContain: shouldOpenInDocs ?
                    'docs/view?url=ya-disk' :
                    'docviewer'
            }
        );
    }

    /**
     * @param {Browser} bro
     * @param {string} fileName
     * @returns {Promise<void>}
     */
    async function assertAudio(bro, fileName) {
        await bro.yaWaitForVisible(slider.common.activeItemTitle().replace(':title', fileName));
        await bro.yaWaitForVisible(slider.common.contentSlider.audioPlayerPlay());
        await bro.yaAssertAudioIsPlaying(fileName);
    }

    /**
     * Проверка fullscreen в видеоплеере
     *
     * @param {Browser} bro
     * @returns {boolean}
     */
    const isVideoFullScreen = async function(bro) {
        return (await bro.execute((selector) => {
            // Берем для ширины document.body.clientWidth вместо window.innerWidth, потому что вертикальный скролл
            // отъедает место
            return $(selector)[0].clientWidth === document.body.clientWidth &&
                $(selector)[0].clientHeight === window.innerHeight;
        }, slider.common.contentSlider.videoPlayer()));
    };

    it('diskclient-1241, 1154: Слайдер. Вирусный файл', async function() {
        const bro = this.browser;
        const isMobile = await bro.yaIsMobile();
        this.testpalmId = isMobile ? 'diskclient-1241' : 'diskclient-1154';
        await bro.url(`${SLIDER}${VIRUS_FILE_NAME}`);
        await bro.waitForVisible(`.ufo-resource-info-dropdown__warning=${VIRUS_FILE_WORNNING}`);
        await bro.yaElementIsNotDisplayed(slider.common.sliderButtons.shareButton());
        await bro.yaElementIsNotDisplayed(
            `//button[../../../div[contains(@class, 'resource-name') and text()="${VIRUS_FILE_NAME}"]]`
        );
    });
    it('diskclient-1240, 1221: Слайдер. Заблокированный файл', async function() {
        const bro = this.browser;
        const isMobile = await bro.yaIsMobile();
        this.testpalmId = isMobile ? 'diskclient-1240' : 'diskclient-1221';
        await bro.url(`${SLIDER}${BLOKED_FILE_NAME}`);
        await bro.waitForVisible(`.ufo-resource-info-dropdown__warning*=${BLOKED_FILE_WORNNING}`);
        if (isMobile) {
            await bro.yaExecuteClick(navigation.touch.modalCell());
            await bro.pause(1000);
        }
        await bro.click(slider.common.sliderButtons.shareButton(), 0, 0);
        await bro.yaElementIsNotDisplayed(popups.common.shareDialog());
        await bro.yaWaitNotificationWithText(BLOKED_FILE_NAME, { consts }.EXT_NOTIFICATION_PUBLISH_ERROR);
    });
    it('diskclient-1234, 1159: Слайдер. Исполняемого файла', async function() {
        const bro = this.browser;
        const isMobile = await bro.yaIsMobile();
        this.testpalmId = isMobile ? 'diskclient-1234' : 'diskclient-1159';
        await bro.url(`${SLIDER}${EXE_FILE_NAME}`);
        await bro.pause(500);
        await yaAssertViewSlider(bro, this.testpalmId);
        await bro.yaElementIsNotDisplayed(
            `//button[../../../div[contains(@class, 'resource-name') and text()="${EXE_FILE_NAME}"]]`
        );
    });
    it('diskclient-1233, 1157: Слайдер. Слайдер книги', async function() {
        const bro = this.browser;
        const isMobile = await bro.yaIsMobile();
        this.testpalmId = isMobile ? 'diskclient-1233' : 'diskclient-1157';
        await checkFileInSlider(bro, BOOK_FILE_NAME, this.testpalmId);
    });
    it('diskclient-4899, 1158: [Слайдер] Слайдер архивного файла', async function() {
        const bro = this.browser;
        const isMobile = await bro.yaIsMobile();
        this.testpalmId = isMobile ? 'diskclient-4899' : 'diskclient-1158';
        await checkFileInSlider(bro, ARCHIVE_FILE_NAME, this.testpalmId);
    });
    it('diskclient-4903, 1156: [Слайдер] Слайдер документа', async function() {
        const bro = this.browser;
        const isMobile = await bro.yaIsMobile();
        this.testpalmId = isMobile ? 'diskclient-4903' : 'diskclient-1156';
        await checkFileInSlider(bro, DOCUMENT_FILE_NAME, this.testpalmId);
    });
    it('diskclient-1587, 1585: Слайдер. Слайдер видеофайла', async function() {
        const bro = this.browser;
        const isMobile = await bro.yaIsMobile();
        this.testpalmId = isMobile ? 'diskclient-1587' : 'diskclient-1585';

        await bro.yaWaitForHidden(listing.listingSpinner());
        await bro.yaOpenListingElement(VIDEO_FILE_NAME);
        await bro.yaAssertVideoIsPlaying();
    });
    it('diskclient-1586, 1584: Слайдер. Слайдер аудиофайла', async function() {
        const bro = this.browser;
        const isMobile = await bro.yaIsMobile();
        this.testpalmId = isMobile ? 'diskclient-1586' : 'diskclient-1584';
        await bro.url(`${SLIDER}${AUDIO_FILE_NAME}`);
        await bro.pause(500);
        await yaAssertViewSlider(bro, this.testpalmId);

        await bro.yaWaitForVisible(slider.common.contentSlider.audioPlayer.playPauseButton());
        await bro.click(slider.common.contentSlider.audioPlayer.playPauseButton());
        await assertAudio(bro, AUDIO_FILE_NAME);
    });
    it('diskclient-936, diskclient-1018: [Слайдер] Автовоспроизведение следующего аудиофайла', async function() {
        const bro = this.browser;
        const isMobile = await bro.yaIsMobile();
        this.testpalmId = isMobile ? 'diskclient-1018' : 'diskclient-936';

        const testdata = {
            folder: 'audio',
            firstAudioName: 'first.mp3',
            firstAudioDuration: 5000,
            sedondAudioName: 'second.mp3'
        };

        await bro.url(`${NAVIGATION.disk.url}/${testdata.folder}`);

        await bro.yaOpenListingElement(testdata.firstAudioName);
        await bro.click(slider.common.contentSlider.audioPlayer.playPauseButton());
        await assertAudio(bro, testdata.firstAudioName);

        await bro.pause(testdata.firstAudioDuration);
        await assertAudio(bro, testdata.sedondAudioName);
    });
    it('diskclient-1172, diskclient-1229: [Слайдер] Сладер панорамных фото', async function() {
        const bro = this.browser;
        const isMobile = await bro.yaIsMobile();
        this.testpalmId = isMobile ? 'diskclient-1229' : 'diskclient-1172';

        await bro.yaOpenListingElement(IMAGE_FILE_NAME_PANO);
        await yaAssertViewSlider(bro, this.testpalmId);
    });
    it('diskclient-5000, 3515: Нельзя открыть документ без превью в ДВ из Слайдера', async function() {
        const bro = this.browser;
        const isMobile = await bro.yaIsMobile();
        this.testpalmId = isMobile ? 'diskclient-5000' : 'diskclient-3515';
        await bro.url(`${SLIDER}${PDF_FILE_NO_PREVIEW}`);
        await yaAssertViewSlider(bro, this.testpalmId);
        await bro.yaElementIsNotDisplayed(
            `//button[../../../div[contains(@class, 'resource-name') and text()="${PDF_FILE_NO_PREVIEW}"]]`
        );
        const sliderURL = await bro.getUrl();
        await bro.click(slider.common.fileIconPdf());
        const currentURL = await bro.getUrl();
        assert(currentURL === sliderURL, 'Срабатывает переход для документов без превью');
    });
    it('diskclient-5001, 3514: Открыть документ с превью в ДВ из Слайдера', async function() {
        const bro = this.browser;
        const isMobile = await bro.yaIsMobile();
        this.testpalmId = isMobile ? 'diskclient-5001' : 'diskclient-3514';
        await checkFileInSlider(bro, PDF_FILE_PREVIEW, this.testpalmId);
        await bro.url(`${SLIDER}${PDF_FILE_PREVIEW}`);
    });

    hermione.only.in(clientDesktopBrowsersList, 'Актуально только для десктопной версии');
    it('diskclient-3467, diskclient-3468, diskclient-3469: Открытие документа в MSO редакторе', async function() {
        const bro = this.browser;
        const testData = {
            msOfficeFiles: ['table.xlsx', 'slides.pptx', 'word.docx'],
            buttonText: 'Редактировать'
        };

        /**
         * @param {string} selector
         * @param {boolean} slider
         * @returns {Promise<void>}
         */
        const openFilesInEditor = async(selector, slider) => {
            for (const fileName of testData.msOfficeFiles) {
                if (slider) {
                    await bro.url(`${SLIDER}${fileName}`);
                    assert.equal(await bro.getText(openButtonSliderText), testData.buttonText);
                } else {
                    await bro.rightClick(`//span[text()="${fileName}"]`);
                }
                await bro.yaClickAndAssertNewTabUrl(selector, { linkShouldContain: ['/edit/', fileName] });
                await bro.close();
            }
        };

        await bro.url(NAVIGATION.disk.url);
        await bro.yaWaitForVisible(listing.listingBody.items());

        await openFilesInEditor(editButtonMenu, false);
        await openFilesInEditor(openButtonSlider, true);
        await openFilesInEditor(editButtonSlider, true);
    });

    hermione.only.in('chrome-desktop');
    it('diskclient-719: Просмотр видео. Фулл скрин', async function() {
        const bro = this.browser;

        await bro.yaWaitForHidden(listing.listingSpinner());
        await bro.yaOpenListingElement(VIDEO_FILE_NAME);

        const player = await bro.$(streamPlayer.video());

        await player.waitForExist({ timeout: 5000 });
        await player.waitForDisplayed();
        await player.doubleClick();
        await bro.waitForVisible(streamPlayer.video());

        await bro.yaWaitForVisible(streamPlayer.video());
        await bro.doubleClick(streamPlayer.video());

        await bro.frameParent();
        assert(await isVideoFullScreen(bro) === true, 'Не открывается full screen по даблклику');
    });
});

hermione.skip.in('chrome-desktop', 'Мигает на десктопе https://st.yandex-team.ru/CHEMODAN-72882');
describe('Слайдер ->', () => {
    it('diskclient-1215, diskclient-1228: [Слайдер] Зум изображений', async function() {
        const bro = this.browser;
        const isMobile = await bro.yaIsMobile();
        this.testpalmId = isMobile ? 'diskclient-1228' : 'diskclient-1215';

        await bro.yaClientLoginFast('yndx-ufo-test-205');

        await bro.yaOpenSection('disk');

        await bro.yaOpenListingElement('1.jpg');

        await bro.yaZoomIn();
        await bro.yaAssertView(`${this.testpalmId}-zoom_in`, slider.common.contentSlider());

        await bro.yaZoomOut();
        await bro.yaAssertView(`${this.testpalmId}-zoom_out`, slider.common.contentSlider());
    });
});
