const clientContentListing = require('../page-objects/client-content-listing');
const clientPopups = require('../page-objects/client-popups');
const navigation = require('../page-objects/client-navigation');
const publicPageObjects = require('../page-objects/public');

const { clientDesktopBrowsersList } = require('@ps-int/ufo-hermione/browsers');

const { assert } = require('chai');
const url = require('url');
const { consts } = require('../config');
const { retriable } = require('@ps-int/ufo-hermione/helpers/retries');
const path = require('path');
const popups = require('../page-objects/client-popups');

/**
 * @typedef { import('@ps-int/ufo-hermione/types').Browser } Browser
 */

/**
 * @returns {Promise<void>}
 */
const cleanTempFiles = async function() {
    const items = this.currentTest.ctx.items;
    if (Array.isArray(items)) {
        await this.browser.url(consts.NAVIGATION.disk.url);
        await this.browser.yaDeleteCompletely([].concat(items), { safe: true, fast: true });
    }
};

describe('Листинг', () => {
    describe('Поведение', () => {
        hermione.auth.createAndLogin();
        it('diskclient-1964, 5733: [Файлы] Новый юзер', async function() {
            const bro = this.browser;
            await bro.yaSkipWelcomePopup();
            await bro.yaWaitForHidden(clientContentListing.common.listingSpinner());
            assert.sameMembers(
                [
                    'Хлебные крошки.mp4',
                    'Санкт-Петербург.jpg',
                    'Москва.jpg',
                    'Море.jpg',
                    'Мишки.jpg',
                    'Зима.jpg',
                    'Горы.jpg',
                    'Корзина'
                ],
                await bro.yaGetListingElementsTitles()
            );
        });

        hermione.auth.createAndLogin();
        hermione.only.in(clientDesktopBrowsersList, 'Актуально только для десктопной версии');
        it('diskclient-712: [Попап "Добро пожаловать!"] Показ новому пользователю', async function() {
            const bro = this.browser;
            this.testpalmId = 'diskclient-712';

            const assertWelcomePopup = async(expectedImage) => {
                await bro.yaWaitForVisible(popups.common.welcomePopup());
                await bro.yaAssertView(expectedImage, popups.common.welcomePopup.dialog());
                await bro.refresh();
            };

            await assertWelcomePopup(this.testpalmId + '-welcome');
            await assertWelcomePopup(this.testpalmId + '-save_time');

            await bro.click(popups.common.welcomePopup.closeButton());
            await bro.yaWaitForHidden(popups.common.welcomePopup());
        });

        hermione.auth.createAndLogin();
        hermione.only.notIn(clientDesktopBrowsersList, 'Актуально только для мобильной версии');
        it('diskclient-1141: Заглушка установки ПО', async function() {
            const bro = this.browser;
            this.testpalmId = 'diskclient-1141';

            await bro.yaWaitForVisible(popups.touch.promoAppPopup());
            await bro.yaAssertView(this.testpalmId, popups.touch.promoAppPopup());

            await bro.click(popups.touch.promoAppPopup.skip());
            await bro.yaWaitForHidden(popups.touch.promoAppPopup());
        });
    });

    describe('Тип листинга -> ', () => {
        /**
         * @param {'icons'|'tile'|'list'} listingType
         * @returns {Promise<void>}
         */
        const assertViewListingTypeTest = async function(listingType) {
            const bro = this.browser;

            await bro.yaSetListingType(listingType);

            await bro.yaWaitForHidden(clientContentListing.common.listingSpinner());
            await bro.yaWaitPreviewsLoaded(clientContentListing.common.listing.item.preview());
            await bro.yaAssertView(this.testpalmId, clientContentListing.common.listingBody());
        };

        hermione.auth.tus({ tus_consumer: 'disk-front-client', tags: ['diskclient-5415', 'diskclient-5414'] });
        it('diskclient-5414, 5415: Тип листинга сохраняется после рефреша', async function() {
            const bro = this.browser;
            this.testpalmId = await bro.yaIsMobile() ? 'diskclient-5415' : 'diskclient-5414';

            for (const listingType of [consts.LISTING.icons, consts.LISTING.list, consts.LISTING.tile]) {
                await bro.yaSetListingType(listingType);
                await bro.yaWaitForVisible(listingType === consts.LISTING.list ?
                    clientContentListing.common.listingThemeRow() :
                    clientContentListing.common.listingThemeTile()
                );

                await bro.refresh();
                await bro.yaWaitForHidden(clientContentListing.common.listingSpinner());

                await bro.yaWaitForVisible(listingType === consts.LISTING.list ?
                    clientContentListing.common.listingThemeRow() :
                    clientContentListing.common.listingThemeTile()
                );
            }
        });

        it('diskclient-1437, 1479: [Тип листинга] Список', async function() {
            this.testpalmId = await this.browser.yaIsMobile() ? 'diskclient-1437' : 'diskclient-1479';
            await this.browser.yaClientLoginFast('yndx-ufo-test-350');
            await assertViewListingTypeTest.call(this, consts.LISTING.list);
        });

        it('diskclient-1478, 1436: [Тип листинга] Маленькая плитка', async function() {
            this.testpalmId = await this.browser.yaIsMobile() ? 'diskclient-1436' : 'diskclient-1478';
            await this.browser.yaClientLoginFast('yndx-ufo-test-357');
            await assertViewListingTypeTest.call(this, consts.LISTING.icons);
        });

        it('diskclient-1435, 1477: [Тип листинга] Крупная плитка', async function() {
            this.testpalmId = await this.browser.yaIsMobile() ? 'diskclient-1435' : 'diskclient-1477';
            await this.browser.yaClientLoginFast('yndx-ufo-test-362');
            await assertViewListingTypeTest.call(this, consts.LISTING.tile);
        });

        hermione.auth.tus({ tus_consumer: 'disk-front-client', tags: ['diskclient-5418', 'diskclient-5417'] });
        it('diskclient-5418, 6164, 5417, 659: Переключение типа листинга в разделе Последние и проверка сохранения типа листинга при переходе по разделам', async function() {
            this.testpalmId = await this.browser.yaIsMobile() ? 'diskclient-5418, 6164' : 'diskclient-5417, 659';
            const bro = this.browser;

            await bro.yaOpenSection('recent');
            await bro.yaSetListingType(consts.LISTING.list);
            await bro.yaWaitForVisible(clientContentListing.common.listingThemeRow());

            await bro.refresh();
            await bro.yaWaitForVisible(clientContentListing.common.listingThemeRow());

            await bro.yaOpenSection('disk');
            await bro.yaWaitForVisible(clientContentListing.common.listingThemeRow());

            await bro.yaOpenSection('recent');
            await bro.yaSetListingType(consts.LISTING.icons);
            await bro.yaWaitForVisible(clientContentListing.common.listingThemeTile());
        });
    });

    describe('Отображение наименования ресурса -> ', () => {
        /**
         * @param {string} user
         * @param {'tile'|'icons'|'list'} listingType
         * @param {'photos'|'weird'} folder
         * @returns {Promise<void>}
         */
        async function testClampedText(user, listingType, folder) {
            const bro = this.browser;
            const isMobile = await bro.yaIsMobile();
            this.testpalmId = isMobile ? 'diskclient-2035' : 'diskclient-2034';

            await bro.yaClientLoginFast(user);
            await bro.url(`${consts.CLIENT}/clamped-text-${folder}`);

            await bro.yaWaitForVisible(clientContentListing.common.listingBody());
            await bro.pause(500); // подгрузка превью
            await bro.assertView(
                `${this.testpalmId}-${folder}-${listingType}`,
                clientContentListing.common.listingBody()
            );
        }

        it('diskclient-2035, 2034: Перенос и заточивание наименования ресурса (мелкая плитка)', async function() {
            await testClampedText.call(this, 'yndx-ufo-test-40', 'tile', 'photos');
            await testClampedText.call(this, 'yndx-ufo-test-40', 'tile', 'weird');
        });

        it('diskclient-2035, 2034: Перенос и заточивание наименования ресурса (крупная плитка)', async function() {
            await testClampedText.call(this, 'yndx-ufo-test-41', 'icons', 'photos');
            await testClampedText.call(this, 'yndx-ufo-test-41', 'icons', 'weird');
        });

        hermione.only.notIn('firefox-desktop', 'https://st.yandex-team.ru/CHEMODAN-75164');
        it('diskclient-2035, 2034: Перенос и заточивание наименования ресурса (список)', async function() {
            await testClampedText.call(this, 'yndx-ufo-test-42', 'list', 'photos');
            await testClampedText.call(this, 'yndx-ufo-test-42', 'list', 'weird');
        });
    });

    describe('Хлебные крошки -> ', () => {
        afterEach(cleanTempFiles);

        /**
         * @param {string} targetCrumbSelector
         * @returns {Promise<void>}
         */
        const testFunctionForCrumbs = async function(targetCrumbSelector) {
            const bro = this.browser;

            await bro.yaClientLoginFast('yndx-ufo-test-225');
            await bro.yaWaitForHidden(clientContentListing.common.listingSpinner());

            await bro.yaGoToFolderAndWaitForListingSpinnerHide('level-1');
            await bro.yaGoToFolderAndWaitForListingSpinnerHide('level-2');

            const pathToFolder = await bro.getAttribute(targetCrumbSelector + ' span', 'id');
            const folderNameForMoving = pathToFolder === '/disk' ? 'Файлы' : path.basename(pathToFolder);

            const testFileName = await bro.yaUploadFiles('test-file.jpg', { uniq: true });

            await bro.yaDragAndDrop(
                clientContentListing.common.listingBodyItemsIconXpath().replace(/:titleText/g, testFileName),
                targetCrumbSelector,
                async() => {
                    await bro.yaAssertView(this.testpalmId, navigation.desktop.listingCrumbs());
                }
            );

            await bro.yaAssertProgressBarAppeared();
            await bro.yaWaitNotificationForResource(
                { name: testFileName, folder: folderNameForMoving },
                consts.TEXT_NOTIFICATION_FILE_MOVED_TO_FOLDER
            );
            this.currentTest.ctx.items = [testFileName];
            this.currentTest.ctx.url = consts.NAVIGATION.folder(folderNameForMoving).url;

            await bro.click(targetCrumbSelector);
            await bro.yaWaitForHidden(clientContentListing.common.listingSpinner());
            await bro.yaAssertListingHas(testFileName);
        };

        hermione.only.in(clientDesktopBrowsersList);
        it('diskclient-921: Проверка днд на хлебную крошку "Файлы"', async function() {
            this.testpalmId = 'diskclient-921';
            await testFunctionForCrumbs.call(this, navigation.desktop.listingCrumbs.headCrumb());
        });

        hermione.only.in(clientDesktopBrowsersList);
        it('diskclient-920: Проверка днд на хлебные крошки', async function() {
            this.testpalmId = 'diskclient-920';
            await testFunctionForCrumbs.call(this, navigation.desktop.listingCrumbs.lastCrumb());
        });

        hermione.only.in(clientDesktopBrowsersList);
        it('diskclient-1977: assertView: Хлебные крошки - переход в корень', async function() {
            const bro = this.browser;
            this.testpalmId = 'diskclient-1977';
            await bro.yaClientLoginFast('yndx-ufo-test-140');
            await bro.yaWaitForVisible(clientContentListing.common.listing());
            //открываем все вложенные папки
            try {
                while (bro.yaAssertListingHas(consts.TEST_FOLDER_NAME)) {
                    await bro.yaOpenListingElement(consts.TEST_FOLDER_NAME);
                    await bro.yaAssertFolderOpened(consts.TEST_FOLDER_NAME);
                }
                //перехватываем состояние, когда дошли до максимального уровня вложенности
            } catch (listingHasNotFolder) {
                await bro.yaAssertListingHasNot(consts.TEST_FOLDER_NAME);
            }
            await bro.refresh(); //смотрим, что после рефреша хлебные крошки не пропали
            await bro.yaWaitForVisible(navigation.desktop.listingCrumbs());
            await bro.yaAssertView(this.testpalmId, navigation.desktop.listingCrumbs());

            await bro.click(navigation.desktop.listingCrumbs.lastCrumb()); //клик в последнюю хлебную крошку
            await bro.yaWaitForVisible(navigation.desktop.listingCrumbs());
            await bro.click(navigation.desktop.listingCrumbs.headCrumb()); //клик в главную хлебную крошку
            await bro.yaWaitForHidden(navigation.desktop.listingCrumbs());
            await bro.yaWaitForVisible(clientContentListing.common.listingBody.items.trashIcon());
        });
    });

    describe('Сортировка ресурсов -> ', () => {
        afterEach(cleanTempFiles);

        hermione.skip.in('chrome-phone', 'https://st.yandex-team.ru/CHEMODAN-84267');
        it('diskclient-707, 1180: Сортировка ресурсов по названию в листинге', async function() {
            const bro = this.browser;
            const isMobile = await bro.yaIsMobile();
            const listingSortButtonSelector = isMobile ?
                navigation.touch.touchListingSettings.settings() :
                clientContentListing.desktop.listingSortButton();
            const prefix = `tmp-${Date.now()}-`;
            const foldersSortAscending = ['A', 'Z', 'a', 'z', 'А', 'Я', 'а', 'я'];
            const filesSortAscending = ['A.jpg', 'Z.jpg', 'a.jpg', 'z.jpg', 'А.png', 'Я.png', 'а.png', 'я.png'];
            const newTestFolderName = prefix + 'work-folder';
            this.currentTest.ctx.items = [newTestFolderName];
            /**
             * @param {boolean} isAscending
             * @returns {Promise<void>}
             */
            const assertSorting = async(isAscending) => {
                await bro.yaSetSortingType(isAscending);

                const actualFileNamesSorting = await bro.yaGetListingElementsTitles();
                const expectedFileNamesSorting = isAscending ?
                    [...foldersSortAscending, ...filesSortAscending] :
                    [...foldersSortAscending.reverse(), ...filesSortAscending.reverse()];

                assert.sameOrderedMembers(actualFileNamesSorting, expectedFileNamesSorting);
            };

            await bro.yaClientLoginFast('yndx-ufo-test-245');
            await bro.yaWaitForHidden(clientContentListing.common.listingSpinner());

            await bro.yaSelectResource('Папка-для-теста-сортировки');
            await bro.yaCopySelected('Файлы');

            await bro.yaWaitForVisible(clientPopups.common.confirmationPopup());
            await bro.yaSetValue(clientPopups.common.confirmationPopup.nameInput(), newTestFolderName);
            await bro.yaWaitForVisible(clientPopups.common.confirmationPopup.acceptButton());
            await bro.click(clientPopups.common.confirmationPopup.acceptButton());

            await bro.yaWaitNotificationForResource(newTestFolderName, consts.TEXT_NOTIFICATION_FOLDER_COPIED);
            await bro.yaAssertListingHas(newTestFolderName);
            await bro.yaCloseActionBar();

            await bro.yaOpenListingElement(newTestFolderName);
            await bro.yaWaitForHidden(clientContentListing.common.listingSpinner());

            await assertSorting(true);
            await bro.yaScrollIntoView(listingSortButtonSelector);
            await assertSorting(false);
        });
    });

    describe('Счетчик скачиваний и просмотров публичного ресурса', () => {
        afterEach(cleanTempFiles);
        /**
         * @param {string} fileName
         * @returns {Promise<void>}
         */
        const openPublicFileTooltip = async function(fileName) {
            const bro = this.browser;

            const listingItem = await bro.$(
                clientContentListing.common.listingBodyItemsXpath().replace(/:titleText/g, fileName)
            );
            const publicIcon = await listingItem.$(
                clientContentListing.common.listingBody.items.publicIconButton()
            );

            await publicIcon.moveTo();

            const tooltip = await bro.$(clientPopups.desktop.shareLinkButtonTooltip());

            await tooltip.waitForDisplayed();
        };
        /**
         * @param {string} fileName
         * @param {'views'|'downloads'} type
         * @param {string} assertionValue
         * @returns {Promise<void>}
         */
        const checkCounter = async function(fileName, type, assertionValue) {
            const bro = this.browser;
            await bro.refresh();

            await bro.yaWaitForHidden(clientContentListing.common.listingSpinner());
            await openPublicFileTooltip.call(this, fileName);

            const counter = await bro.$(clientPopups.desktop.shareLinkButtonTooltip[`${type}Count`]());
            const count = await counter.getText();

            assert.equal(count, assertionValue);
        };

        /**
         * @returns {Promise<string>}
         */
        const publishFileAndGetLink = async function() {
            await this.yaWaitActionBarDisplayed();
            await this.yaCallActionInActionBar('publish');
            await this.yaWaitForVisible(clientPopups.common.shareDialog());
            return this.getValue(clientPopups.common.shareDialog.textInput());
        };

        hermione.only.in('chrome-desktop');
        it('diskclient-992: Счётчик просмотров публичной папки.', async function() {
            const bro = this.browser;
            const testFolderName = `tmp-${Date.now()}-test-folder`;
            this.currentTest.ctx.items = [testFolderName];

            await bro.yaClientLoginFast('yndx-ufo-test-264');
            await bro.yaCreateFolder(testFolderName);
            const commonFolderLink = await publishFileAndGetLink.call(bro);

            await bro.newWindow(commonFolderLink);

            const tabIds = await bro.getTabIds();
            await bro.window(tabIds[0]);

            await retriable(() => checkCounter.call(this, testFolderName, 'views', '1'), 5, 1000);

            await bro.window(tabIds[1]);
            await bro.refresh();
            await bro.window(tabIds[0]);

            await retriable(() => checkCounter.call(this, testFolderName, 'views', '2'), 5, 1000);
        });

        hermione.skip.notIn('', 'Не работает счетчик загрузки, расскипать после https://st.yandex-team.ru/DISKSUP-11216');
        it('diskclient-993: Счётчик скачиваний публичного файла.', async function() {
            const bro = this.browser;
            const downloadFromPublicAndReturnBack = async(publicFileLink) => {
                await bro.newWindow(publicFileLink);
                await bro.yaWaitForVisible(publicPageObjects.desktopToolbar.downloadButton());
                await bro.click(publicPageObjects.desktopToolbar.downloadButton());
                const tabs = await bro.getTabIds();
                await bro.window(tabs[0]);
            };

            await bro.yaClientLoginFast('yndx-ufo-test-274');

            const testFileName = await bro.yaUploadFiles('test-file.jpg', { uniq: true });
            this.currentTest.ctx.items = [testFileName];
            await bro.yaSelectResource(testFileName);
            const publicLink = await publishFileAndGetLink.call(bro);

            await downloadFromPublicAndReturnBack(publicLink);
            await retriable(() => checkCounter.call(this, testFileName, 'downloads', '1'), 5, 1000);

            await downloadFromPublicAndReturnBack(publicLink);
            await retriable(() => checkCounter.call(this, testFileName, 'downloads', '2'), 5, 1000);
        });

        hermione.only.in('chrome-desktop');
        hermione.skip.notIn('', 'Мигает https://st.yandex-team.ru/CHEMODAN-75031');
        it('diskclient-3463: Отображение тултипа с количеством скачиваний и просмотров на иконке публичности', async function() {
            const bro = this.browser;
            const testFolderName = 'public-resource';
            const testFolderItemSelector = clientContentListing.common.listingBodyItemsXpath()
                .replace(/:titleText/g, testFolderName);
            this.testpalmId = 'diskclient-3463';

            await bro.yaClientLoginFast('yndx-ufo-test-381');

            await bro.yaSetListingType(consts.LISTING.icons);
            await openPublicFileTooltip.call(this, testFolderName);

            for (const listingType of [consts.LISTING.tile, consts.LISTING.icons, consts.LISTING.list]) {
                await bro.yaSetListingType(listingType);
                if (listingType === consts.LISTING.list) {
                    await bro.yaAssertView(`${this.testpalmId}-list`, testFolderItemSelector);
                } else {
                    await openPublicFileTooltip.call(this, testFolderName);
                    await bro.yaWaitForVisible(clientPopups.desktop.shareLinkButtonTooltip());
                    await bro.yaAssertView(
                        `${this.testpalmId}-${listingType}`,
                        clientPopups.desktop.shareLinkButtonTooltip()
                    );
                }
            }
        });
    });

    describe('Панель навигации', () => {
        hermione.skip.in('chrome-phone', 'https://st.yandex-team.ru/CHEMODAN-84268'); // свайпы не работают
        it('diskclient-574, 1066: Панель навигации', async function() {
            const bro = this.browser;
            const isMobile = await bro.yaIsMobile();
            this.testpalmId = isMobile ? 'diskclient-1066' : 'diskclient-574';
            await bro.yaClientLoginFast('yndx-ufo-test-245');

            if (isMobile) {
                await bro.yaWaitForVisible(navigation.touch.mobileNavigation());
                await bro.yaAssertView(`${this.testpalmId}-1`, navigation.touch.mobileNavigation());

                await bro.swipeLeft(navigation.touch.mobileNavigation(), 400);

                await bro.yaWaitForVisible(navigation.touch.mobileNavigation());
                await bro.yaAssertView(`${this.testpalmId}-2`, navigation.touch.mobileNavigation());
            } else {
                await bro.yaWaitForVisible(navigation.desktop.sidebarNavigation());
                await bro.yaAssertView(`${this.testpalmId}-1`, navigation.desktop.sidebarNavigation());

                await bro.yaWaitForVisible(navigation.desktop.sidebarButtons());
                await bro.yaAssertView(`${this.testpalmId}-2`, navigation.desktop.sidebarButtons());
            }
        });
    });

    describe('Переходы', () => {
        it('diskclient-4779, 4780: [Переход с нуля] Переход в несуществующую директорию', async function() {
            const bro = this.browser;
            const isMobile = await bro.yaIsMobile();
            this.testpalmId = isMobile ? 'diskclient-4780' : 'diskclient-4779';
            await bro.yaClientLoginFast('yndx-ufo-test-146');
            await bro.url(`${consts.CLIENT}/not-existing`);
            await bro.yaWaitForVisible(clientContentListing.common.listing());
            await bro.yaWaitForVisible(clientContentListing.common.listingBody.items.trashIcon());
            const browserUrl = await bro.getUrl();
            assert.equal(url.parse(browserUrl).path, consts.CLIENT);
        });

        it('diskclient-3709, 5402: Переход в пустую Корзину', async function() {
            const bro = this.browser;
            const isMobile = await bro.yaIsMobile();
            const trashSelector = clientContentListing.common.listing[isMobile ? 'head' : 'cleanTrash']();
            this.testpalmId = isMobile ? 'diskclient-5402' : 'diskclient-3709';
            await bro.yaClientLoginFast('yndx-ufo-test-268');

            await bro.yaWaitForHidden(clientContentListing.common.listingSpinner());
            await bro.yaOpenListingElement('Корзина');

            await bro.yaWaitForVisible(trashSelector);
            await bro.yaAssertView(this.testpalmId, trashSelector);
        });
    });

    describe('Подгрузка порций', () => {
        it('diskclient-1367, 5200: Подгрузка порции файлов', async function() {
            const bro = this.browser;
            const isMobile = await bro.yaIsMobile();

            await bro.yaClientLoginFast('yndx-ufo-test-247');
            await bro.yaOpenSection('disk');

            await bro.yaWaitForHidden(clientContentListing.common.listingSpinner());
            const { length: firstPortionCount } = await bro.$$(clientContentListing.common.listingBody.items());

            await bro.yaScrollIntoView(clientContentListing.common.listing.item() + ':last-child');
            await bro.yaWaitForVisible(
                `${clientContentListing.common.listingBody.items()}:nth-child(${firstPortionCount + 2})`
            );
            const { length: secondPortionCount } = await bro.$$(clientContentListing.common.listingBody.items());

            assert.equal(firstPortionCount, isMobile ? 40 : 80);
            assert.equal(secondPortionCount, isMobile ? 80 : 120);
        });
    });

    describe('Открытие документов в DV по клику на файл', () => {
        /**
         * @param {string} testFolderName
         * @returns {Promise<void>}
         */
        const authorizeAndEnterToFolder = async function(testFolderName) {
            const bro = this.browser;

            await bro.yaClientLoginFast('yndx-ufo-test-269');
            await bro.yaWaitForHidden(clientContentListing.common.listingSpinner());

            await bro.yaGoToFolderAndWaitForListingSpinnerHide(testFolderName);
        };

        it('diskclient-4998, 3517: Нельзя открыть документ без превью в DV', async function() {
            await authorizeAndEnterToFolder.call(this, 'items-without-preview');
            for (const fileName of ['2.epub', '3.pdf']) {
                if (await this.browser.yaIsMobile()) {
                    const selector = clientContentListing.common.listingBodyItemsInfoXpath().replace(
                        /:titleText/g,
                        fileName
                    );
                    await this.browser.click(selector);
                } else {
                    await this.browser.yaOpenListingElement(fileName);
                }
                await this.browser.yaAssertTabsCount(1);
            }
        });

        it('diskclient-4999: Открыть документ с превью в DV', async function() {
            await authorizeAndEnterToFolder.call(this, 'items-with-preview');
            const isMobile = await this.browser.yaIsMobile();
            const PDF_FILE = '1.pdf';
            const FB2_FILE = '2.fb2';
            for (const fileName of [PDF_FILE, FB2_FILE]) {
                if (isMobile) {
                    const selector = clientContentListing.common.listingBodyItemsInfoXpath().replace(
                        /:titleText/g,
                        fileName
                    );
                    await this.browser.click(selector);
                } else {
                    await this.browser.yaOpenListingElement(fileName);
                }
                const tabs = await this.browser.getTabIds();
                assert(tabs.length > 1, 'Вкладка не открылась');
                await this.browser.window(tabs[1]);
                const shouldOpenInDocs = !isMobile && fileName === PDF_FILE;
                await this.browser.yaAssertUrlInclude(
                    shouldOpenInDocs ?
                        'https://docs.yandex.ru/docs/view?url=ya-disk' :
                        'https://docviewer.yandex.ru'
                );
                await this.browser.close();
            }
        });

        hermione.only.notIn('chrome-phone', 'кривая логика на тачах - https://st.yandex-team.ru/CHEMODAN-80967');
        it('diskclient-3516, 3674: Открытие офисных документов в DV', async function() {
            const bro = this.browser;
            const isMobile = await bro.yaIsMobile();
            this.testpalmId = isMobile ? 'diskclient-3516' : 'diskclient-3674';
            await authorizeAndEnterToFolder.call(this, 'office-files');
            const testFileNamesWithPreview = [
                'with-preview.docx',
                'with-preview.pptx',
                'with-preview.xlsx'
            ];
            const testFileNamesWithoutPreview = [
                'without-preview.docx',
                'without-preview.pptx',
                'without-preview.xlsx'
            ];

            await bro.yaScrollIntoView(clientContentListing.common.listing.head());

            for (const fileName of testFileNamesWithoutPreview) {
                if (isMobile) {
                    const selector = clientContentListing.common.listingBodyItemsInfoXpath().replace(
                        /:titleText/g,
                        fileName
                    );
                    await this.browser.click(selector);
                    await bro.yaAssertTabsCount(1);
                } else {
                    await bro.yaOpenActionPopup(fileName);
                    await bro.yaWaitForHidden(clientPopups.common.actionPopup.viewButton());
                    // закроем меню кликом вне попапа
                    await bro.yaExecuteEvent(clientContentListing.common.listing(), 'mousedown');
                    await bro.yaWaitForHidden(clientPopups.common.actionPopup());
                }
            }

            for (const fileName of testFileNamesWithPreview) {
                if (await this.browser.yaIsMobile()) {
                    const selector = clientContentListing.common.listingBodyItemsInfoXpath().replace(
                        /:titleText/g,
                        fileName
                    );
                    await this.browser.click(selector);
                } else {
                    await bro.yaCallActionInActionPopup(fileName, 'view');
                }

                const tabs = await bro.getTabIds();
                assert(tabs.length > 1, 'Вкладка не открылась');
                await bro.window(tabs[1]);
                await bro.yaAssertUrlInclude(isMobile ? 'docviewer' : '/docs/view?url=ya-disk');
                await bro.close();
            }
        });

        it('diskclient-3675: Открытие документов в DV (только txt и fb2)', async function() {
            await authorizeAndEnterToFolder.call(this, 'fb2-txt');
            for (const fileName of ['1.fb2', '2.fb2', '3.txt', '4.txt']) {
                await this.browser.yaOpenListingElement(fileName);

                await this.browser.pause(1000);
                const tabs = await this.browser.getTabIds();
                assert(tabs.length > 1, 'Вкладка не открылась');
                await this.browser.window(tabs[1]);
                await this.browser.yaAssertUrlInclude('docviewer');
                await this.browser.close();
            }
        });
    });

    hermione.only.in(clientDesktopBrowsersList);
    describe('Создание документов через контекстное меню и сайдбар', () => {
        const SPREADSHEET = 'Excel';
        const PRESENTATION = 'PowerPoint';
        const DOCUMENT = 'Word';

        const officeDocumentsDefaultNames = {
            [SPREADSHEET]: 'Новая таблица.xlsx',
            [PRESENTATION]: 'Новая презентация.pptx',
            [DOCUMENT]: 'Новый документ.docx'
        };

        /**
         * @param {string} diskTabId
         * @returns {Promise<void>}
         */
        const assertEditorOpened = async function(diskTabId) {
            await this.browser.pause(1000);
            const [editorTabId] = (await this.browser.getTabIds()).filter((tab) => tab !== diskTabId);

            await this.browser.window(editorTabId);

            await this.browser.yaAssertUrlInclude('yandex.ru/edit');

            await this.browser.close();
        };

        /**
         * @param {string} user
         * @param {'Word'|'PowerPoint'|'Excel'} docType
         * @returns {Promise<void>}
         */
        const createOfficeDocumentFromContextMenuTest = async function(user, docType) {
            const bro = this.browser;
            const diskTabId = await bro.getCurrentTabId();
            const documentName = officeDocumentsDefaultNames[docType];
            const testFolderName = `tmp-${Date.now()}`;
            this.currentTest.ctx.items = [testFolderName];

            await bro.yaClientLoginFast(user);

            await bro.yaCreateFolder(testFolderName);
            await bro.yaOpenListingElement(testFolderName);

            await bro.yaOpenListingContextMenu();
            await bro.click(clientPopups.desktop.contextMenuCreatePopup[`create${docType}`]());
            await bro.yaWaitForVisible(clientPopups.common.confirmationDialog());
            await bro.click(clientPopups.common.confirmationDialog.submitButton());

            await assertEditorOpened.call(this, diskTabId);

            await bro.yaWaitForVisible(clientContentListing.common.listing.item());
            await bro.yaAssertListingHas(documentName);
        };

        it('diskclient-854: Создание Презентации', async function() {
            await createOfficeDocumentFromContextMenuTest.call(this, 'yndx-ufo-test-478', PRESENTATION);
        });

        it('diskclient-853: Создание Таблицы', async function() {
            await createOfficeDocumentFromContextMenuTest.call(this, 'yndx-ufo-test-479', SPREADSHEET);
        });

        it('diskclient-852: Создание Текстового документа', async function() {
            await createOfficeDocumentFromContextMenuTest.call(this, 'yndx-ufo-test-480', DOCUMENT);
        });

        it('diskclient-583: Создание презентации через попап "+ Создать" в сайдбаре', async function() {
            const bro = this.browser;
            const diskTabId = await bro.getCurrentTabId();
            const documentName = officeDocumentsDefaultNames[PRESENTATION];
            const testFolderName = `tmp-${Date.now()}`;
            this.currentTest.ctx.items = [testFolderName];

            await bro.yaClientLoginFast('yndx-ufo-test-481');

            await bro.yaCreateFolder(testFolderName);
            await bro.yaOpenListingElement(testFolderName);

            await bro.yaOpenCreatePopup();
            await bro.click(clientPopups.desktop.createPopup.createPresentation());
            await bro.yaWaitForVisible(clientPopups.common.confirmationDialog());
            await bro.click(clientPopups.common.confirmationDialog.submitButton());

            await assertEditorOpened.call(this, diskTabId);

            await bro.yaWaitForVisible(clientContentListing.common.listing.item());
            await bro.yaAssertListingHas(documentName);
        });
    });
});
