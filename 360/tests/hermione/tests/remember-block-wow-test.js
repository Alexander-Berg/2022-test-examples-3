const rememberBlock = require('../page-objects/client-remember-block').common;
const popups = require('../page-objects/client-popups');
const { photo } = require('../page-objects/client-photo2-page').common;
const slider = require('../page-objects/slider');
const albums = require('../page-objects/client-albums-page');
const listing = require('../page-objects/client-content-listing').common;
const clientNavigation = require('../page-objects/client-navigation');
const { footer } = require('../page-objects/client-footer').common;
const { header, psHeader } = require('../page-objects/client');
const consts = require('../config').consts;
const { BLOCKS } = require('../consts/remember-block-tests-consts');
const { assert } = require('chai');

describe('Блок воспоминаний с wow сеткой ->', () => {
    /**
     * @param {string} testId
     * @param {Object} userConfig
     */
    async function assertViewRememberBlock(testId, userConfig) {
        const bro = this.browser;
        await bro.yaClientLoginFast(userConfig.user);
        await bro.url(userConfig.url);

        await bro.yaWaitPreviewsLoaded(rememberBlock.wowResource.preview());

        const screenshotSelector = rememberBlock.blockInner();

        // Если заскринить весь блок, то assertView будет постоянно мигать из-за скрола
        // поэтому снимается скриншот вьюпорта в верху блока, откроливается вниз страницы и
        // ещё раз снимается скриншот низа.
        await bro.pause(500); // пауза нужна чтобы пропал скрол
        await bro.yaAssertView(`${testId}-top`, screenshotSelector, {
            ignoreElements: [header(), psHeader(), footer.copyright()]
        });
        await bro.yaScrollToEnd();
        await bro.yaAssertView(`${testId}-bottom`, screenshotSelector, {
            ignoreElements: [header(), psHeader(), footer.copyright()]
        });
    }

    hermione.only.notIn('chrome-phone-6.0');
    it('diskclient-3523, diskclient-5002: assertView: Отображение и проскролливание фото в блоке фото в ПП', async function() {
        const isMobile = await this.browser.yaIsMobile();
        this.testpalmId = isMobile ? 'diskclient-3523' : 'diskclient-5002';
        await assertViewRememberBlock.call(this, this.testpalmId, BLOCKS.exists);
    });

    hermione.only.notIn('chrome-phone-6.0');
    it('diskclient-4898, diskclient-5021: Отображение и проскролливание фото в блоке фото в ПП для годовой подборки', async function() {
        const isMobile = await this.browser.yaIsMobile();
        this.testpalmId = isMobile ? 'diskclient-4898' : 'diskclient-5021';
        await assertViewRememberBlock.call(this, this.testpalmId, BLOCKS.existsYear);
    });

    hermione.only.notIn('chrome-phone-6.0');
    it('diskclient-3518, diskclient-5003: Переход в раздел "Фото" из блока ПП', async function() {
        const bro = this.browser;
        const isMobile = await bro.yaIsMobile();
        this.testpalmId = isMobile ? 'diskclient-3518' : 'diskclient-5003';
        await this.browser.yaClientLoginFast(BLOCKS.existsBig.user);
        await this.browser.url(BLOCKS.existsBig.url);

        await bro.click(rememberBlock.photoSliceButton());
        const itemSelector = photo.itemByName().replace(':title', '2017-04-04 19-51-31.JPG');
        await bro.yaWaitForVisible(itemSelector);
        await bro.yaAssertInViewport(itemSelector);
    });

    it('diskclient-3403, diskclient-5019: Работа браузерной кнопки Назад в блоке фото в ПП', async function() {
        const bro = this.browser;

        await bro.loginAndGoToUrl('diskclient-3403', 'diskclient-5019', BLOCKS.exists);
        await bro.click(rememberBlock.resource() + ':first-child');
        await bro.yaWaitForVisible(slider.common.contentSlider());
        await bro.back();
        await bro.yaWaitForHidden(slider.common.contentSlider()); // сладер должен скрываться по кнопке "назад"

        await bro.click(rememberBlock.resource() + ':first-child');
        await bro.yaWaitForVisible(slider.common.contentSlider());
        await bro.click(slider.common.sliderButtons.closeButton());
        await bro.yaWaitForHidden(slider.common.contentSlider());
        await bro.back();
        await bro.yaWaitForHidden(slider.common.contentSlider()); //  слайдер не должен открываться по кнопке "назад"
    });

    hermione.only.in('chrome-phone');
    it('diskclient-3519: Поделение блоком ПП и создание Альбома', async function() {
        const bro = this.browser;
        await bro.loginAndGoToUrl('diskclient-3519', '', BLOCKS.exists);
        await bro.click(rememberBlock.shareAlbumButton());
        await bro.yaWaitForVisible(rememberBlock.shareAlbumButtonProgress());
        await bro.yaWaitForVisible(popups.common.shareDialog(), 15000); // создание альбома может занять какое-то время
        const albumUrl = await bro.getValue(popups.common.shareDialog.textInput());
        await bro.yaExecuteClick(clientNavigation.touch.modalCell());
        await bro.yaWaitForHidden(popups.common.shareDialog());
        await bro.pause(500); // ожидане конца анимации модалки
        await bro.yaCloseNotificationForResource('Ваши фотографии за выходные', consts.TEXT_NOTIFICATION_ALBUM_CREATED);

        await bro.click(rememberBlock.shareAlbumButton());
        await bro.yaWaitForVisible(popups.common.shareDialog());
        const albumUrlSecondShare = await bro.getValue(popups.common.shareDialog.textInput());
        assert(albumUrl === albumUrlSecondShare, 'Урл публичного альбома не должен меняться в одной сессии');

        await bro.url(albumUrl);
        await bro.yaWaitForVisible(albums.publicAlbum.avatar());
        await bro.yaWaitForVisible(albums.publicAlbum.title());

        const title = await bro.getText(albums.publicAlbum.title());
        assert.equal(title, 'Ваши фотографии за выходные');
    });

    hermione.only.in('chrome-phone');
    it('diskclient-3394: Слайдер в блоке фото в ПП', async function() {
        const bro = this.browser;
        await bro.loginAndGoToUrl('diskclient-3394', '', BLOCKS.exists);

        await bro.click(rememberBlock.resource() + ':first-child');

        const toolbar = await bro.$(slider.common.sliderButtons());
        const sliderItem = await bro.$(slider.common.contentSlider.items());

        // проверка скрытия и показа тулбара по тапу
        await bro.waitUntil(async () => await toolbar.isClickable());
        await sliderItem.moveTo();
        await sliderItem.click();
        await bro.waitUntil(async () => !(await toolbar.isClickable()));
        await sliderItem.click();
        await bro.waitUntil(async () => await toolbar.isClickable());

        // // проверка pinch-to-zoom и передвижения призумленной картинки
        await bro.yaPointerPinch(slider.common.contentSlider.items());
        await bro.yaPointerPanX(slider.common.contentSlider.items(), -1, false);
        await bro.yaPointerPanY(slider.common.contentSlider.items(), -1, false);
        await bro.yaAssertView('diskclient-3394-1', slider.common.contentSlider());

        // // отзум по дабл-тапу
        await bro.click(slider.common.contentSlider.items());
        await bro.click(slider.common.contentSlider.items());
        await bro.pause(500);
        await bro.yaAssertView('diskclient-3394-2', slider.common.contentSlider());
    });

    hermione.only.notIn('chrome-phone-6.0');
    it('diskclient-3395, diskclient-5008: Инфо попап в слайдере в блоке фото в ПП', async function() {
        const bro = this.browser;
        await bro.loginAndGoToUrl('diskclient-3395', 'diskclient-5008', BLOCKS.exists);
        await bro.click(rememberBlock.resource() + ':first-child');
        await bro.yaWaitForVisible(slider.common.contentSlider());
        await bro.click(slider.common.sliderButtons.infoButton());
        await bro.yaWaitForVisible(popups.common.resourceInfoDropdownContent());

        await bro.pause(500);
        await bro.yaAssertView('diskclient-3395', 'body');
    });

    it('diskclient-3520, diskclient-5017: Переход к файлу (не безлимитный) через слайдер в блоке фото в ПП', async function() {
        const TARGET_FILE_NAME = '2017-03-19 16-07-17.JPG';
        const bro = this.browser;

        await bro.loginAndGoToUrl('diskclient-3520', 'diskclient-5017', BLOCKS.exists);

        await bro.click(rememberBlock.resource() + ':last-child');
        await bro.yaWaitForVisible(slider.common.contentSlider());
        await bro.click(slider.common.sliderButtons.infoButton());
        await bro.yaWaitForVisible(popups.common.resourceInfoDropdownFooter.goToResource());
        await bro.pause(500);

        await bro.click(popups.common.resourceInfoDropdownFooter.goToResource());

        await bro.yaAssertListingHas(TARGET_FILE_NAME);
        await bro.yaWaitActionBarDisplayed();

        const fileSelector = listing.listingBodyItemsInfoXpath().replace(/:titleText/g, TARGET_FILE_NAME);
        const inViewport = await bro.isVisibleWithinViewport(fileSelector);

        assert(inViewport === true, 'Не произошел подскрол к ресурсу');

        await bro.yaWaitForUrlPath('/client/disk/Загрузки/Фотокамера/nice');
    });

    it('diskclient-3521, diskclient-5018: Переход к безлимитному файлу через слайдер в блоке фото в ПП', async function() {
        const bro = this.browser;

        await bro.loginAndGoToUrl('diskclient-3521', 'diskclient-5018', BLOCKS.unlim);

        await bro.click(rememberBlock.resource() + ':nth-child(5)');

        await bro.yaWaitForVisible(slider.common.contentSlider());
        await bro.click(slider.common.sliderButtons.moreButton());

        await bro.yaWaitForVisible(popups.common.actionBarMorePopup.goToFile());
        await bro.pause(500);

        await bro.click(popups.common.actionBarMorePopup.goToFile());

        const preview = await bro.$(photo.item.preview());
        await preview.waitForExist();

        await bro.click(slider.common.sliderButtons.closeButton());
        await bro.yaWaitForHidden(slider.common.contentSlider());

        const photoSelector = photo.itemByName().replace(/:title/, '2017-05-28 14-45-50.JPG');
        await bro.waitForVisible(photoSelector);
        await bro.yaAssertInViewport(photoSelector);
    });

    it('diskclient-3522, diskclient-5006: Листание в слайдере в блоке фото в ПП', async function() {
        const bro = this.browser;

        await bro.loginAndGoToUrl('diskclient-3522', 'diskclient-5006', BLOCKS.slider);

        const resources = await bro.yaRememberBlockGetResourcesNames();
        assert(resources.length > 2, 'В блоке меньше трёх ресурсов');

        await bro.click(rememberBlock.resource() + ':first-child');
        await bro.yaWaitForVisible(slider.common.contentSlider());
        for (let i = 0; i < resources.length - 1; i++) {
            const fileName = await bro.yaGetResourceNameFromInfoDropdown(slider.common.sliderButtons.infoButton());

            assert(
                fileName === resources[i],
                `Перелистывание вправо: в слайдере на позиции ${i} должен находиться ресурс с именем ${resources[i]}`
            );

            await bro.yaChangeSliderActiveImage();
        }

        for (let i = resources.length - 1; i > 0; i--) {
            const fileName = await bro.yaGetResourceNameFromInfoDropdown(slider.common.sliderButtons.infoButton());
            assert(
                fileName === resources[i],
                `Перелистывание влево: в слайдере на позиции ${i} должен находиться ресурс с именем ${resources[i]}`
            );

            await bro.yaChangeSliderActiveImage(1, 'left');
        }

        const fileName = await bro.yaGetResourceNameFromInfoDropdown(slider.common.sliderButtons.infoButton());
        assert(fileName === resources[0], 'После перелистывания влево слайдер не находится на первом ресурсе');
    });

    it('diskclient-3547, diskclient-5007: Подскролл к фото после закрытия слайдера в блоке воспоминаний с вау-сеткой', async function() {
        const bro = this.browser;
        const resourceSelector = rememberBlock.wowResource();

        await bro.loginAndGoToUrl('diskclient-3547', 'diskclient-5007', BLOCKS.existsBig);

        await bro.click(resourceSelector + ':first-child');
        await bro.yaWaitForVisible(slider.common.contentSlider());

        await bro.yaChangeSliderActiveImage(8);

        await bro.click(slider.common.sliderButtons.closeButton());
        await bro.pause(500); // ожидание подскрола
        const inViewport = await bro.isVisibleWithinViewport(resourceSelector + ':nth-child(5)');

        assert(inViewport === true, 'Не произошел подскрол к ресурсу');
    });

    it('diskclient-3397, diskclient-5010: Скачивание через слайдер в блоке фото в ПП', async function() {
        const bro = this.browser;
        await bro.loginAndGoToUrl('diskclient-3397', 'diskclient-5010', BLOCKS.exists);

        await bro.click(rememberBlock.resource() + ':nth-child(2)');
        await bro.yaWaitForVisible(slider.common.contentSlider());

        const url = await bro.yaGetDownloadUrlFromAction(async() => {
            await bro.click(slider.common.sliderButtons.downloadButton());
        });

        assert(
            /downloader\.disk\.yandex\.ru.+&filename=2017-03-18%2016-37-43\.JPG/.test(url),
            'Скачивание не произошло'
        );
    });

    hermione.only.notIn('firefox-desktop', 'https://st.yandex-team.ru/CHEMODAN-75371');
    it('diskclient-3548, diskclient-5004: Возврат в блок воспоминаний из диска', async function() {
        const bro = this.browser;

        await bro.loginAndGoToUrl('diskclient-3548', 'diskclient-5004', BLOCKS.exists);

        await bro.click(rememberBlock.photoSliceButton());

        await bro.refresh();
        await bro.back();

        await bro.waitForVisible(rememberBlock.resource(), 1000);
    });

    it('diskclient-3400, diskclient-5014: Перемещение файла через слайдер в блоке фото в ПП', async function() {
        const bro = this.browser;
        const isMobile = await bro.yaIsMobile();

        this.testpalmId = isMobile ? 'diskclient-3400' : 'diskclient-5014';

        const { user, url } = isMobile ? BLOCKS.move : BLOCKS.moveDesktop;

        await bro.yaClientLoginFast(user);

        const TARGET_FILE_NAME = 'IMG_4181.JPG';
        const TARGET_FOLDER_NAME = 'move-target';

        await bro.yaOpenListingElement(TARGET_FOLDER_NAME);
        await bro.yaWaitForHidden(listing.listingSpinner());
        const hasMovedResource = await bro.yaGetContentListingHas(TARGET_FILE_NAME);

        if (hasMovedResource) {
            await bro.yaSelectResource(TARGET_FILE_NAME);
            await bro.yaWaitActionBarDisplayed();
            if (isMobile) {
                await bro.click(popups.common.actionBar.moreButton());
                await bro.yaWaitForVisible(popups.common.actionBarMorePopup.moveButton());
                await bro.pause(500);
                await bro.click(popups.common.actionBarMorePopup.moveButton());
            } else {
                await bro.click(popups.common.actionBar.moveButton());
            }
            await bro.yaWaitForVisible(popups.common.selectFolderDialog.treeContent());
            await bro.click(popups.common.selectFolderDialogItemsXpath().replace(':titleText', 'Файлы'));
            await bro.click(popups.common.selectFolderDialog.acceptButton());

            await bro.yaWaitNotificationForResource(
                { name: TARGET_FILE_NAME, folder: 'Файлы' },
                consts.TEXT_NOTIFICATION_FILE_MOVED_TO_FOLDER
            );
        }

        await this.browser.url(url);

        await bro.click(rememberBlock.resource() + ':first-child');
        await bro.yaWaitForVisible(slider.common.contentSlider());
        await bro.click(slider.common.sliderButtons.moreButton());
        await bro.pause(500);
        await bro.click(popups.common.actionBarMorePopup.moveButton());

        await bro.yaWaitForVisible(popups.common.selectFolderDialog.treeContent());
        await bro.pause(500);
        await bro.click(popups.common.selectFolderDialogItemsXpath().replace(':titleText', TARGET_FOLDER_NAME));
        await bro.click(popups.common.selectFolderDialog.acceptButton());

        await bro.yaWaitNotificationForResource(
            { name: TARGET_FILE_NAME, folder: TARGET_FOLDER_NAME },
            consts.TEXT_NOTIFICATION_FILE_MOVED_TO_FOLDER
        );

        await bro.yaAssertRememberBlockHasResource(TARGET_FILE_NAME);

        await bro.click(slider.common.sliderButtons.infoButton());
        await bro.yaWaitForVisible(popups.common.resourceInfoDropdownContent());

        const fileNameInDropdown = await bro.getText(popups.common.resourceInfoDropdownContent.fileName());

        assert(fileNameInDropdown === TARGET_FILE_NAME, 'Название открытого в слайдере файла поменялось');
    });

    it('diskclient-3396, diskclient-5009: Поделение через слайдер в блоке фото в ПП', async function() {
        const bro = this.browser;
        await bro.loginAndGoToUrl('diskclient-3396', 'diskclient-5009', BLOCKS.publish);

        await bro.click(rememberBlock.resource() + ':first-child');
        await bro.yaWaitForVisible(slider.common.contentSlider());

        await bro.click(slider.common.sliderButtons.shareButton());
        await bro.yaWaitForVisible(popups.common.shareDialog());
        await bro.yaResetPointerPosition();
        await bro.pause(500);
        await bro.yaWaitNotificationWithText(consts.TEXT_NOTIFICATION_LINK_COPIED);
        await bro.yaDeletePublicLinkInShareDialog();
    });

    it('diskclient-3401, diskclient-5012: Отмена копирования файла через слайдер в блоке фото в ПП', async function() {
        const bro = this.browser;

        await bro.loginAndGoToUrl('diskclient-3401', 'diskclient-5012', BLOCKS.copy);

        await bro.click(rememberBlock.resource() + ':first-child');
        await bro.yaWaitForVisible(slider.common.contentSlider());

        await bro.click(slider.common.sliderButtons.moreButton());
        await bro.pause(500);
        await bro.click(popups.common.actionBarMorePopup.copyButton());
        await bro.yaWaitForHidden(popups.touch.mobilePaneVisible());
        await bro.yaWaitForVisible(popups.common.selectFolderDialog.treeContent());
        await bro.click(popups.common.selectFolderDialog.cancelButton());
        await bro.yaWaitForHidden(popups.common.selectFolderDialog());

        await bro.pause(500);
        await bro.click(slider.common.sliderButtons.moreButton());
        await bro.pause(500);
        await bro.click(popups.common.actionBarMorePopup.copyButton());
        await bro.yaWaitForHidden(popups.touch.mobilePaneVisible());
        await bro.yaWaitForVisible(popups.common.selectFolderDialog.treeContent());
        await bro.click(popups.common.selectFolderDialog.closeButton());
        await bro.yaWaitForHidden(popups.common.selectFolderDialog());
    });

    it('diskclient-3399, diskclient-5013: Копирование файла через слайдер в блоке фото в ПП', async function() {
        this.browser.executionContext.timeout(90000);
        const bro = this.browser;
        const TARGET_FOLDER_NAME = 'copy-target';
        const isMobile = await bro.yaIsMobile();

        this.testpalmId = isMobile ? 'diskclient-3399' : 'diskclient-5013';

        const { user, url } = isMobile ? BLOCKS.copy : BLOCKS.copyDesktop;

        await bro.yaClientLoginFast(user);

        await bro.yaOpenListingElement(TARGET_FOLDER_NAME);
        await bro.yaWaitForHidden(listing.listingSpinner());

        const isNotEmpty = await bro.yaListingNotEmpty();
        if (isNotEmpty) {
            await bro.yaDeleteAllResources();
        }

        await bro.url(url);

        await bro.click(rememberBlock.resource() + ':nth-child(2)');
        await bro.yaWaitForVisible(slider.common.contentSlider());

        const fileName = await bro.yaGetResourceNameFromInfoDropdown(slider.common.sliderButtons.infoButton());

        await bro.click(slider.common.sliderButtons.moreButton());
        await bro.pause(500);
        await bro.click(popups.common.actionBarMorePopup.copyButton());
        await bro.yaWaitForVisible(popups.common.selectFolderDialog.treeContent());
        await bro.pause(500); // modal animation
        await bro.click(popups.common.selectFolderDialogItemsXpath().replace(':titleText', TARGET_FOLDER_NAME));
        await bro.click(popups.common.selectFolderDialog.acceptButton());
        await bro.yaWaitForHidden(popups.common.selectFolderDialog());

        await bro.yaWaitNotificationForResource(
            { name: fileName, folder: TARGET_FOLDER_NAME },
            consts.TEXT_NOTIFICATION_FILE_COPIED_TO_FOLDER
        );

        await bro.yaAssertRememberBlockHasResource(fileName);

        const fileNameAfterCopy = await bro.yaGetResourceNameFromInfoDropdown(slider.common.sliderButtons.infoButton());

        assert(fileNameAfterCopy === fileName, 'Название открытого в слайдере файла поменялось');

        await bro.url('/client/disk/' + TARGET_FOLDER_NAME);
        await bro.yaWaitForHidden(listing.listingSpinner());
        await bro.yaAssertListingHas(fileName);

        await bro.yaDeleteAllResources();
        await bro.yaCleanTrash();
    });

    hermione.only.notIn('chrome-phone-6.0');
    it('diskclient-3402, diskclient-5015: Переименование файла через слайдер в блоке фото в ПП', async function() {
        const bro = this.browser;
        await bro.loginAndGoToUrl('diskclient-3402', 'diskclient-5015', BLOCKS.rename);

        await bro.click(rememberBlock.resource() + ':first-child');
        await bro.yaWaitForVisible(slider.common.contentSlider());

        const originalFileName = await bro.yaGetResourceNameFromInfoDropdown(slider.common.sliderButtons.infoButton());
        const newFileName = originalFileName.replace(/^([^.]+)/, '$1_RENAMED');

        await bro.click(slider.common.sliderButtons.moreButton());
        await bro.pause(500);
        await bro.click(popups.common.actionBarMorePopup.renameButton());
        await bro.yaWaitForHidden(popups.touch.mobilePaneVisible());
        await bro.yaSetResourceNameAndApply(newFileName);
        await bro.pause(500);

        const fileNameInDropdown = await bro.yaGetResourceNameFromInfoDropdown(
            slider.common.sliderButtons.infoButton()
        );
        assert(fileNameInDropdown === newFileName, 'Название открытого в слайдере файла не изменилось на ожидаемое');

        await bro.click(slider.common.sliderButtons.moreButton());
        await bro.pause(500);
        await bro.click(popups.common.actionBarMorePopup.renameButton());
        await bro.yaWaitForHidden(popups.touch.mobilePaneVisible());
        await bro.yaSetResourceNameAndApply(originalFileName.replace(/^([^.]+)(.+)$/, 'IMG_' + Date.now() + '$2'));
        await bro.yaWaitForVisible(slider.common.sliderButtons.infoButton());
    });

    it('diskclient-3404, diskclient-5016: Отмена переименование файла через слайдер в блоке фото в ПП', async function() {
        const bro = this.browser;

        await bro.loginAndGoToUrl('diskclient-3404', 'diskclient-5016', BLOCKS.rename);

        await bro.click(rememberBlock.resource() + ':nth-child(2)');
        await bro.yaWaitForVisible(slider.common.contentSlider());
        await bro.click(slider.common.sliderButtons.moreButton());
        await bro.pause(500);
        await bro.click(popups.common.actionBarMorePopup.renameButton());
        await bro.yaWaitForHidden(popups.touch.mobilePaneVisible());

        await bro.yaWaitForVisible(popups.common.renameDialog());
        await bro.click(popups.common.renameDialog.closeButton());
        await bro.yaWaitForHidden(popups.common.renameDialog());
    });

    /**
     * @param {{ user: string, url: string }} blockConfig
     * @param {string} resourceSelector
     * @returns {Promise<void>}
     */
    async function loginAndDeleteFirstPhoto(blockConfig, resourceSelector) {
        const bro = this.browser;

        await bro.yaClientLoginFast(blockConfig.user);

        await bro.yaRestoreAllFromTrash();

        await bro.url(blockConfig.url);
        await bro.yaWaitPreviewsLoaded(rememberBlock.wowResource.preview());

        await bro.click(resourceSelector + ':first-child');
        await bro.yaWaitForVisible(slider.common.contentSlider());

        const firstFileNameToRemove = await bro.yaGetResourceNameFromInfoDropdown(
            slider.common.sliderButtons.infoButton()
        );

        await bro.click(slider.common.sliderButtons.deleteButton());
        await bro.yaWaitNotificationForResource(firstFileNameToRemove, consts.TEXT_NOTIFICATION_FILE_MOVED_TO_TRASH);

        const nextFilename = await bro.yaGetResourceNameFromInfoDropdown(slider.common.sliderButtons.infoButton());
        assert(firstFileNameToRemove !== nextFilename, 'Слайдер не переключился на следующий элемент');

        await bro.click(slider.common.sliderButtons.closeButton());
        await bro.yaWaitForHidden(slider.common.contentSlider());

        const names = await bro.yaRememberBlockGetResourcesNames();

        assert.equal(names[0], nextFilename);
    }

    it('diskclient-3398, diskclient-5011: Удаление файла из блока воспоминаний с вау-сеткой', async function() {
        const bro = this.browser;
        await bro.yaRestoreAllFromTrash();

        const isMobile = await bro.yaIsMobile();
        this.testpalmId = isMobile ? 'diskclient-3398' : 'diskclient-5011';
        const block = isMobile ? BLOCKS.delete : BLOCKS.deleteDesktop;

        await loginAndDeleteFirstPhoto.call(this, block, rememberBlock.resource());

        await bro.yaRestoreAllFromTrash();
    });

    hermione.only.notIn('chrome-phone-6.0');
    it('diskclient-3525, diskclient-5020: Заглушка для недоступного блока', async function() {
        const bro = this.browser;

        const isMobile = await bro.yaIsMobile();
        this.testpalmId = isMobile ? 'diskclient-3525' : 'diskclient-5020';

        await bro.yaClientLoginFast(BLOCKS.notFound.user);
        await bro.url(BLOCKS.notFound.url);
        await bro.yaAssertView(`${this.testpalmId}`, rememberBlock.blockInner());
    });
});
