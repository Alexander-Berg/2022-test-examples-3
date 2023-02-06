const clientContentListing = require('../page-objects/client-content-listing');
const clientNavigation = require('../page-objects/client-navigation');
const clientPopups = require('../page-objects/client-popups');
const clientPhotoPage = require('../page-objects/client-photo2-page');
const consts = require('../config').consts;
const { clientDesktopBrowsersList } = require('@ps-int/ufo-hermione/browsers');
const { assert } = require('chai');

describe('Выделение -> ', () => {
    afterEach(async function() {
        const items = this.currentTest.ctx.items || [];
        if (items.length) {
            await this.browser.url(consts.NAVIGATION.disk.url);
            await this.browser.yaDeleteCompletely([].concat(items), { safe: true, fast: true });
        }
    });

    /**
     * @param {string} sourceSelector
     * @param {string} targetName
     * @param {string} targetSelector
     * @returns {Promise<void>}
     */
    const assertDragAndDrop = async function(sourceSelector, targetName, targetSelector) {
        const bro = this.browser;
        await bro.yaDragAndDrop(sourceSelector, targetSelector,
            async() => {
                assert.equal(
                    await bro.getText(clientNavigation.desktop.sidebarNavigation.dropHighlighted()),
                    'Корзина',
                    'Не подсвечивается Корзина при drag&drop в сайдбаре'
                );
                assert.equal(
                    await bro.getText(clientContentListing.common.listing.itemDpopHighlighed.title()),
                    targetName,
                    `Не подсвечивается ${targetName} при drag&drop в листинге`
                );

                await bro.moveToObject(
                    clientContentListing.common.listingBodyItemsIconXpath().replace(/:titleText/g, 'Корзина')
                );
                assert.equal(
                    await bro.getText(clientContentListing.common.listing.itemDpopHighlighed.title()),
                    'Корзина',
                    'Не подсвечивается Корзина при drag&drop в листинге'
                );
                await bro.moveToObject(sourceSelector);
            }
        );
    };

    hermione.only.in(clientDesktopBrowsersList);
    hermione.auth.createAndLogin();
    it('diskclient-914: Проверка в днд разделе "Файлы"', async function() {
        const bro = this.browser;
        this.testpalmId = 'diskclient-914';

        const testdata = {
            firstFolderName: 'Folder1',
            secondFolderName: 'Folder2',
            fileName: 'test-file.txt',
            trash: 'Корзина'
        };

        await bro.yaSkipWelcomePopup();
        await bro.yaSetListingType(consts.LISTING.icons);
        await bro.yaUploadFiles(testdata.fileName, { uniq: false });
        await bro.yaCreateFolders([testdata.firstFolderName, testdata.secondFolderName]);

        await assertDragAndDrop.call(
            this,
            clientContentListing.common.listingBodyItemsIconXpath().replace(/:titleText/g, testdata.fileName),
            testdata.secondFolderName,
            clientContentListing.common.listingBodyItemsIconXpath().replace(/:titleText/g, testdata.secondFolderName)
        );

        await assertDragAndDrop.call(
            this,
            clientContentListing.common.listingBodyItemsIconXpath().replace(/:titleText/g, testdata.firstFolderName),
            testdata.secondFolderName,
            clientContentListing.common.listingBodyItemsIconXpath().replace(/:titleText/g, testdata.secondFolderName)
        );
    });

    hermione.only.in('chrome-desktop');
    it('diskclient-924: Скролл страницы при днд', async function() {
        const bro = this.browser;
        await bro.yaClientLoginFast('yndx-ufo-test-261');
        await bro.yaWaitForHidden(clientContentListing.common.listingSpinner());

        await bro.yaDragAndDrop(
            clientContentListing.common.listingBody.items() + ':nth-child(1)',
            clientContentListing.common.listingBody.items() + ':nth-child(2)',
            async() => {
                await bro.yaScrollIntoView(clientContentListing.common.listing.item() + ':last-child');
                await bro.yaAssertView('diskclient-924', 'body', {
                    ignoreElements: [clientNavigation.desktop.spaceInfoSection()]
                });
            }
        );
    });

    hermione.only.in('chrome-desktop');
    it('diskclient-918: Проверка днд в папках', async function() {
        const bro = this.browser;
        const testFolderName = 'test-folder';
        const testFileName = 'test-file.jpg';
        const testFileSelector = clientContentListing.common.listingBodyItemsIconXpath()
            .replace(/:titleText/g, testFileName);

        await bro.yaClientLoginFast('yndx-ufo-test-262');

        await bro.yaOpenListingElement(testFolderName);
        await bro.yaWaitForVisible(testFileSelector);

        await bro.yaDragAndDrop(
            testFileSelector,
            clientNavigation.desktop.sidebarButtons(),
            async() => {
                await bro.yaAssertView('diskclient-918-1',
                    clientNavigation.desktop.sidebarNavigation());
                await bro.yaAssertView('diskclient-918-2', clientContentListing.common.listing.head());
            }
        );
    });

    hermione.only.in('chrome-desktop');
    hermione.skip.notIn('', 'Мигающий тест https://st.yandex-team.ru/CHEMODAN-70989');
    it('diskclient-912: Проверка днд в разделе "Последние"', async function() {
        const bro = this.browser;

        await bro.yaClientLoginFast('yndx-ufo-test-253');
        await bro.yaOpenSection('disk');

        const firstTestFileName = await bro.yaUploadFiles('test-file.jpg', { uniq: true });
        const secondTestFileName = await bro.yaUploadFiles('test-file.txt', { uniq: true });
        const firstTestFileSelector = clientContentListing.common.listingBodyItemsIconXpath()
            .replace(/:titleText/g, firstTestFileName);
        const secondTestFileSelector = clientContentListing.common.listingBodyItemsIconXpath()
            .replace(/:titleText/g, secondTestFileName);
        this.currentTest.ctx.items = [firstTestFileName, secondTestFileName];

        await bro.yaOpenSection('recent');
        await bro.yaWaitForHidden(clientContentListing.common.listingSpinner());

        await bro.yaDragAndDrop(
            firstTestFileSelector,
            clientNavigation.desktop.navigationItemTrash(),
            async() => {
                await bro.yaAssertView('diskclient-912-1',
                    clientNavigation.desktop.sidebarNavigation());
                await bro.moveToObject(clientNavigation.desktop.navigationItemDisk());
                await bro.yaAssertView('diskclient-912-2',
                    clientNavigation.desktop.sidebarNavigation());
                await bro.moveToObject(secondTestFileSelector);
                await bro.yaAssertView('diskclient-912-3', secondTestFileSelector);
            }
        );
    });

    hermione.only.in('chrome-desktop');
    it('diskclient-911: Проверка днд на разных типах листинга', async function() {
        const bro = this.browser;
        const folderNameForMoving = 'folder-for-moving';
        const anotherFileName = 'Горы.jpg';
        const folderForMovingSelector = clientContentListing.common.listingBodyItemsXpath().replace(
            /:titleText/g,
            folderNameForMoving
        );
        const anotherFileNameItemSelector = clientContentListing.common.listingBodyItemsXpath().replace(
            /:titleText/g,
            anotherFileName
        );
        const anotherFileNameIconSelector = clientContentListing.common.listingBodyItemsIconXpath().replace(
            /:titleText/g,
            anotherFileName
        );

        await bro.yaClientLoginFast('yndx-ufo-test-254');

        await bro.yaOpenSection('disk');
        const testFileName = await bro.yaUploadFiles('test-file.jpg', { uniq: true });
        const testFileNameItemSelector = clientContentListing.common.listingBodyItemsXpath().replace(
            /:titleText/g,
            testFileName
        );
        const testFileNameIconSelector = clientContentListing.common.listingBodyItemsIconXpath().replace(
            /:titleText/g,
            testFileName
        );
        this.currentTest.ctx.items = [testFileName];

        await bro.yaSelectResource(testFileName);
        await bro.yaShareSelected();
        await bro.click(clientPopups.common.shareDialog.closeButton());
        await bro.yaWaitForHidden(clientPopups.common.shareDialog());

        for (const section of ['disk', 'recent', 'shared']) {
            await bro.yaOpenSection(section);
            for (const listingType of [consts.LISTING.tile, consts.LISTING.icons, consts.LISTING.list]) {
                const startSelector = listingType === consts.LISTING.list ?
                    testFileNameItemSelector :
                    testFileNameIconSelector;
                const finishSelector = clientNavigation.desktop.sidebarButtons();

                await bro.yaScrollIntoView(clientContentListing.desktop.listingType());
                await bro.yaSetListingType(listingType);

                await bro.yaScrollIntoView(startSelector);
                await bro.yaDragAndDrop(
                    startSelector,
                    finishSelector,
                    async() => {
                        await bro.yaAssertView(
                            `diskclient-911-${section}-${listingType}-1`,
                            clientNavigation.desktop.sidebarNavigation(),
                            { withHover: true }
                        );
                        if (section !== 'recent') {
                            await bro.yaScrollIntoView(folderForMovingSelector);
                            await bro.moveToObject(folderForMovingSelector);
                            await bro.yaAssertView(
                                `diskclient-911-${section}-${listingType}-2`,
                                folderForMovingSelector,
                                { withHover: true }
                            );
                            await bro.moveToObject(
                                listingType === consts.LISTING.list ?
                                    anotherFileNameItemSelector :
                                    anotherFileNameIconSelector
                            );
                        }
                    }
                );
            }
        }
    });

    hermione.only.in('chrome-desktop');
    it('diskclient-669: Open menu (Три точки)', async function() {
        const bro = this.browser;
        const testFileNames = ['test-file.MOV', 'test-file.jpg', 'test-file.mp3'];

        await bro.yaClientLoginFast('yndx-ufo-test-255');
        await bro.yaWaitForHidden(clientContentListing.common.listingSpinner());
        await bro.setViewportSize({ width: 512, height: 950 });

        for (const fileName of testFileNames) {
            await bro.yaSelectResource(fileName);
            await bro.yaWaitActionBarDisplayed();
            await bro.pause(500);
            await bro.assertView(`diskclient-669-${fileName}-actionBar`, 'body', {
                ignoreElements: ['.root__wrapper']
            });
            await bro.yaCallActionInActionBar('more');
            await bro.yaWaitForVisible(clientPopups.common.actionBarMorePopup());
            await bro.pause(500);
            await bro.yaAssertView(`diskclient-669-${fileName}-popup`, clientPopups.common.actionBarMorePopup());
            await bro.yaCloseActionBar();
        }
    });

    it('diskclient-662, 5202: Проверка топбара', async function() {
        const bro = this.browser;
        this.testpalmId = await bro.yaIsMobile() ? 'diskclient-5202' : 'diskclient-662';
        const testFileName = 'test-file.docx';

        await bro.yaClientLoginFast('yndx-ufo-test-255');
        await bro.yaWaitForHidden(clientContentListing.common.listingSpinner());

        await bro.yaSelectResource(testFileName);

        await bro.yaWaitActionBarDisplayed();
        await bro.pause(500);
        await bro.yaAssertView(this.testpalmId, clientPopups.common.actionBar());
    });

    hermione.only.notIn(clientDesktopBrowsersList, 'Актуально только для мобильной версии');
    it('diskclient-1200: Кнопка "more" для файлов', async function() {
        const bro = this.browser;
        this.testpalmId = 'diskclient-1200';
        const testData = {
            user: 'yndx-ufo-test-542',
            fileName: 'Горы.jpg',
            moreButtonPopupAnimation: 1000
        };

        await bro.yaClientLoginFast(testData.user);
        await bro.yaWaitForHidden(clientContentListing.common.listingSpinner());

        await bro.yaSelectResource(testData.fileName);

        await bro.yaClick(clientPopups.touch.actionBar.moreButton());
        await bro.pause(testData.moreButtonPopupAnimation);
        await bro.yaAssertView(this.testpalmId, clientPopups.touch.moreButtonPopup());
    });

    it('diskclient-639, 5201: Снятие выделения с файлов', async function() {
        const bro = this.browser;
        const isMobile = await bro.yaIsMobile();
        const firstTestFileName = 'test-file.docx';
        const secondTestFileName = 'test-file.jpg';
        const firstTestFileNameSelectedSelector = clientContentListing.common.listingBodySelectedItemsInfoXpath()
            .replace(/:titleText/g, firstTestFileName);
        const secondTestFileNameSelectedSelector = clientContentListing.common.listingBodySelectedItemsInfoXpath()
            .replace(/:titleText/g, secondTestFileName);
        const firstTestFileNameItemSelector = clientContentListing.common.listingBodyItemsXpath()
            .replace(/:titleText/g, firstTestFileName);
        const secondTestFileNameItemSelector = clientContentListing.common.listingBodyItemsXpath()
            .replace(/:titleText/g, secondTestFileName);
        this.testpalmId = isMobile ? 'diskclinet-5201' : 'diskclient-639';

        await bro.yaClientLoginFast('yndx-ufo-test-255');
        await bro.yaWaitForHidden(clientContentListing.common.listingSpinner());

        if (!isMobile) {
            await bro.yaSelectResource(firstTestFileName);
            await bro.yaAssertView(`${this.testpalmId}-3`, firstTestFileNameItemSelector);
            await bro.yaWaitActionBarDisplayed();
            await bro.click(clientContentListing.common.clientListing());
            await bro.yaWaitActionBarHidden();
            await bro.yaAssertView(`${this.testpalmId}-4`, firstTestFileNameItemSelector);
        }

        await bro.yaSelectResources([firstTestFileName, secondTestFileName]);
        await bro.yaWaitActionBarDisplayed();
        if (isMobile) {
            await bro.click(secondTestFileNameSelectedSelector);
        } else {
            await bro.yaClickWithPressedKey(secondTestFileNameSelectedSelector, consts.KEY_CTRL);
        }

        await bro.yaWaitForVisible(firstTestFileNameSelectedSelector);
        await bro.yaWaitForHidden(secondTestFileNameSelectedSelector);
        if (!isMobile) {
            await bro.moveToObject(clientNavigation.desktop.sidebarButtons());
        }

        await bro.yaAssertView(`${this.testpalmId}-1`, firstTestFileNameItemSelector);
        await bro.yaAssertView(`${this.testpalmId}-2`, secondTestFileNameItemSelector);
    });

    hermione.only.in('chrome-phone');
    it('diskclient-4662: [Вау-сетка] Выделение файлов в вау-сетке ', async function() {
        this.testpalmId = 'diskclient-4662';
        const bro = this.browser;
        const firstTestFileName = 'first-file.heic';
        const secondTestFileName = 'second-file.PNG';

        await bro.yaClientLoginFast('yndx-ufo-test-257');

        await bro.yaOpenSection('photo');
        await bro.yaWaitForHidden(clientContentListing.common.listingSpinner());

        await bro.yaSelectPhotoItemByName(firstTestFileName, true);
        await bro.yaAssertView(`${this.testpalmId}-1`, clientPhotoPage.common.photo.itemByName()
            .replace(':title', firstTestFileName));

        await bro.yaSelectPhotoItemByName(secondTestFileName, true, true);
        await bro.yaAssertView(`${this.testpalmId}-2`, clientPhotoPage.common.photo.itemByName()
            .replace(':title', secondTestFileName));
        await bro.yaAssertView(`${this.testpalmId}-3`, clientPhotoPage.common.photo.itemByName()
            .replace(':title', firstTestFileName));
    });

    it('diskclient-4518, 4519: Выделение кластера в фотосрезе', async function() {
        const bro = this.browser;
        const firstTestFileName = 'first-file.JPG';
        this.testpalmId = await bro.yaIsMobile() ? 'diskclient-4519' : 'diskclient-4518';

        await bro.yaClientLoginFast('yndx-ufo-test-282');

        await bro.yaOpenSection('photo');
        await bro.yaWaitForHidden(clientContentListing.common.listingSpinner());

        await bro.yaSelectCluster(clientPhotoPage.common.photo.titleLabel());
        await bro.pause(1000); // ждем, пока пройдет анимация выделения
        await bro.yaAssertView(`${this.testpalmId}-1`, clientPhotoPage.common.photo.group());

        await bro.yaSelectPhotoItemByName(firstTestFileName, true, false);
        await bro.pause(1000); // ждем, пока пройдет анимация выделения
        await bro.yaAssertView(`${this.testpalmId}-2`, clientPhotoPage.common.photo.group());

        await bro.yaSelectCluster(clientPhotoPage.common.photo.titleLabel());
        await bro.pause(1000); // ждем, пока пройдет анимация выделения
        await bro.yaAssertView(`${this.testpalmId}-3`, clientPhotoPage.common.photo.group());
    });

    it('diskclient-1087, 663: Выделение группы файлов', async function() {
        const bro = this.browser;
        const firstFileName = '1.jpg';
        const secondFileName = '2.jpg';
        const thirdFileName = 'Folder';
        const isMobile = await bro.yaIsMobile();
        this.testpalmId = isMobile ? 'diskclient-1087' : 'diskclient-663';

        await bro.yaClientLoginFast('yndx-ufo-test-260');

        await bro.yaWaitForHidden(clientContentListing.common.listingSpinner());

        await bro.yaSelectResource(firstFileName);
        await bro.yaAssertView(`${this.testpalmId}-1`, clientContentListing.common.clientListing());

        if (isMobile) {
            await bro.yaSelectResource(secondFileName, { doTap: true });
            await bro.yaSelectResource(thirdFileName, { doTap: true });
        } else {
            await bro.yaSelectResources([secondFileName, thirdFileName]);
        }

        await bro.yaAssertView(`${this.testpalmId}-2`, clientContentListing.common.clientListing());

        if (isMobile) {
            await bro.yaTap(
                clientContentListing.common.listingBodyItemsInfoXpath().replace(/:titleText/g, secondFileName)
            );
        } else {
            await bro.keys(consts.KEY_CTRL);
            await bro.click(clientContentListing.common.listingBodyItemsIconXpath()
                .replace(/:titleText/g, secondFileName));
            // перемещаем курсор, чтобы на скриншоте не казалось, что второй файл выделен
            await bro.moveToObject(clientContentListing.common.listingBodyItemsIconXpath()
                .replace(/:titleText/g, thirdFileName));
        }

        await bro.yaAssertView(`${this.testpalmId}-3`, clientContentListing.common.clientListing());
    });

    it('diskclient-4503, 4901: Топбар в фотосрезе. Выделение от 500 до 1000 ресурсов', async function() {
        const bro = this.browser;
        const firstTestFileName = 'first-file.JPG';
        this.testpalmId = await bro.yaIsMobile() ? 'diskclient-4503' : 'diskclient-4901';

        await bro.yaClientLoginFast('yndx-ufo-test-511');

        await bro.yaOpenSection('photo');
        await bro.yaWaitForHidden(clientContentListing.common.listingSpinner());

        await bro.yaSelectCluster(clientPhotoPage.common.photo.titleLabel());
        await bro.yaWaitForVisible('.selection-info__tooltip');
        await bro.pause(100); // ждем, пока пройдет анимация выделения
        await bro.yaAssertView(`${this.testpalmId}-1`, [
            clientPopups.common.actionBar(),
            clientPopups.common.selectionInfoLimitTooltip()
        ]);

        await bro.yaSelectPhotoItemByName(firstTestFileName, true, false);
        await bro.pause(100); // ждем, пока пройдет анимация выделения
        await bro.yaAssertView(`${this.testpalmId}-2`, clientPopups.common.actionBar());

        await bro.yaSelectCluster(clientPhotoPage.common.photo.titleLabel());
        await bro.yaWaitForVisible('.selection-info__tooltip');
        await bro.pause(100); // ждем, пока пройдет анимация выделения
        await bro.yaAssertView(`${this.testpalmId}-3`, [
            clientPopups.common.actionBar(),
            clientPopups.common.selectionInfoLimitTooltip()
        ]);
    });
});
