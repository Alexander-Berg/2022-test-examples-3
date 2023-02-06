const { clientDesktopBrowsersList } = require('@ps-int/ufo-hermione/browsers');
const docs = require('../page-objects/docs');
const clientCommon = require('../page-objects/client-common');
const clientNavigation = require('../page-objects/client-navigation');
const popups = require('../page-objects/client-popups');
const listing = require('../page-objects/client-content-listing');
const footer = require('../page-objects/client-footer');
const versions = require('../page-objects/versions-dialog-modal');
const { retriable } = require('@ps-int/ufo-hermione/helpers/retries');
const { consts } = require('../config');
const { assert } = require('chai');
const { performance } = require('perf_hooks');
const { URL } = require('url');
const _ = require('lodash');
const {
    getDocsUrl,
    closeDocsSort,
    openDocsSection,
    openDocsSort,
    openDocsUrl,
    setDocsSort,
    DOCS_SECTION_TITLES
} = require('../helpers/docs');

describe('Документы ->', () => {
    afterEach(async function () {
        const { items } = this.currentTest.ctx;
        if (Array.isArray(items)) {
            await this.browser.url(consts.NAVIGATION.disk.url);
            await this.browser.yaDeleteCompletely([].concat(items), { safe: true, fast: true });
        }
    });

    describe('Просмотр страницы ->', () => {
        it('diskclient-6532, diskclient-6632: Смена языка', async function() {
            const bro = this.browser;
            await openDocsUrl.call(this, 'yndx-ufo-test-562');

            await bro.yaWaitForVisible(docs.common.docsListing());

            const currentLang = await bro.getText(clientCommon.common.langSwicher.currentLang());
            const targetLang = currentLang === 'RU' ? 'EN' : 'RU';

            await bro.click(clientCommon.common.langSwicher());
            await bro.yaWaitForVisible(clientCommon.common.langMenu());
            await bro.click(clientCommon.common[`langMenu${targetLang}`]());

            const title = targetLang === 'RU' ? 'Недавние документы' : 'Recent documents';
            await bro.yaWaitForVisible(docs.common.docsPage.title().replace(':title:', title));
        });

        /**
         * @param {'icons'|'tile'|'list'} target
         * @param {string} user
         * @param {string} testId
         */
        async function checkListingType(target, user, testId) {
            const bro = this.browser;
            const isMobile = await bro.yaIsMobile();

            await openDocsUrl.call(this, user);

            const setListingType = async (type) => {
                if (isMobile) {
                    await bro.click(docs.touch.touchListingSettingsButton());
                    await bro.yaWaitForVisible(docs.touch.touchListingSettings());
                    await bro.pause(500);
                    await bro.click(docs.touch.touchListingSettings[type]());
                    await bro.yaWaitForHidden(docs.touch.touchListingSettings());
                } else {
                    await bro.yaSetListingType(type);
                }
            };

            await bro.yaWaitForVisible(docs.common.docsListing());

            const isCurrent = (await bro.getAttribute(docs.common.docsListing.item() + ':first-child', 'class'))
                .split(' ').join('.')
                .includes(docs.common.docsListing[`${target}Item`]().split(' ')[1]);

            if (isCurrent) {
                const other = ['icons', 'tile', 'list'].find((type) => type !== target);
                await setListingType(other);
                await bro.yaWaitForVisible(docs.common.docsListing[`${other}Item`]());
            }

            await setListingType(target);

            await bro.yaWaitForVisible(docs.common.docsListing[`${target}Item`]());

            await bro.yaWaitPreviewsLoaded(docs.common.docsListing.preview());
            await bro.yaAssertView(testId, docs.common.docsListing());

            if (!isMobile) {
                await bro.click(listing.desktop.listingType());
                await bro.yaWaitForVisible(listing.desktop.listingType.popup());
                await bro.pause(500);
                await bro.yaAssertView(`${testId}-menu`, [
                    listing.desktop.listingType(),
                    listing.desktop.listingType.popup()
                ]);
            }

            await bro.refresh();
            await bro.yaWaitForVisible(docs.common.docsListing[`${target}Item`]());
        }

        it('diskclient-6533, diskclient-6633: Переключение типа листинга - Плитка', async function () {
            const testId = await this.browser.yaIsMobile() ? 'diskclient-6633' : 'diskclient-6533';
            await checkListingType.call(this, 'icons', 'yndx-ufo-test-563', testId);
        });

        it('diskclient-6534, diskclient-6634: Переключение типа листинга - Крупная плитка', async function () {
            const testId = await this.browser.yaIsMobile() ? 'diskclient-6634' : 'diskclient-6534';
            await checkListingType.call(this, 'tile', 'yndx-ufo-test-564', testId);
        });

        it('diskclient-6535, diskclient-6635: Переключение типа листинга - Список', async function () {
            const testId = await this.browser.yaIsMobile() ? 'diskclient-6635' : 'diskclient-6535';
            await checkListingType.call(this, 'list', 'yndx-ufo-test-565', testId);
        });

        it('diskclient-6536, diskclient-6636: Переключение между разделами', async function() {
            const bro = this.browser;
            const testId = await bro.yaIsMobile() ? 'diskclient-6636' : 'diskclient-6536';

            await openDocsUrl.call(this, 'yndx-ufo-test-561');

            for (const type of ['docx', 'xlsx', 'pptx']) {
                await openDocsSection.call(this, type);
                await bro.yaWaitPreviewsLoaded(docs.common.docsListing.preview());
                await bro.yaAssertView(`${testId}-${type}`, docs.common.docsPage(), {
                    ignoreElements: [
                        docs.common.docsListing(),
                        clientNavigation.desktop.infoSpaceButton()
                    ]
                });
            }
        });

        it('diskclient-6537, diskclient-6637: Сортировка ресурсов - по названию', async function () {
            const bro = this.browser;
            const isMobile = await bro.yaIsMobile();

            await openDocsUrl.call(this, 'yndx-ufo-test-566');

            await bro.yaWaitForVisible(docs.common.docsListing());

            await openDocsSort.call(this);
            await bro.yaWaitPreviewsLoaded(docs.common.docsListing.preview());
            await bro.yaAssertView(`${isMobile ? 'diskclient-6637' : 'diskclient-6537'}-menu`, isMobile ?
                [clientCommon.touch.drawerHandle(), clientCommon.touch.drawerContent()] :
                [docs.common.docsPage.titleWrapper(), docs.desktop.sortPopup()]
            );
            await closeDocsSort.call(this);

            assert.deepEqual(
                await bro.yaGetListingElementsTitles(),
                [
                    'IT_pam.docx',
                    'Legneva.docx',
                    'Italy.docx',
                    '22 Document.docx',
                    '2 Document.docx',
                    '12.docx',
                    '1.docx',
                    'Document One.docx',
                    'Документ 12.docx',
                    'Документ 1.docx',
                    'Документ 2.docx',
                    'Не Документ.docx',
                    'Документ 22.docx'
                ]
            );

            await setDocsSort.call(this, { order: 'asc' });
            assert.deepEqual(
                await bro.yaGetListingElementsTitles(),
                [
                    'Документ 22.docx',
                    'Не Документ.docx',
                    'Документ 2.docx',
                    'Документ 1.docx',
                    'Документ 12.docx',
                    'Document One.docx',
                    '1.docx',
                    '12.docx',
                    '2 Document.docx',
                    '22 Document.docx',
                    'Italy.docx',
                    'Legneva.docx',
                    'IT_pam.docx'
                ]
            );

            await setDocsSort.call(this, { type: 'title', order: 'desc' });
            assert.deepEqual(
                await bro.yaGetListingElementsTitles(),
                [
                    'Не Документ.docx',
                    'Документ 22.docx',
                    'Документ 12.docx',
                    'Документ 2.docx',
                    'Документ 1.docx',
                    'Legneva.docx',
                    'Italy.docx',
                    'IT_pam.docx',
                    'Document One.docx',
                    '22 Document.docx',
                    '12.docx',
                    '2 Document.docx',
                    '1.docx'
                ]
            );

            await setDocsSort.call(this, { order: 'asc' });
            assert.deepEqual(
                await bro.yaGetListingElementsTitles(),
                [
                    '1.docx',
                    '2 Document.docx',
                    '12.docx',
                    '22 Document.docx',
                    'Document One.docx',
                    'IT_pam.docx',
                    'Italy.docx',
                    'Legneva.docx',
                    'Документ 1.docx',
                    'Документ 2.docx',
                    'Документ 12.docx',
                    'Документ 22.docx',
                    'Не Документ.docx'
                ]
            );

            await setDocsSort.call(this, { type: 'size', order: 'desc' });
            assert.deepEqual(
                await bro.yaGetListingElementsTitles(),
                [
                    'Документ 2.docx',
                    'Document One.docx',
                    'Legneva.docx',
                    'IT_pam.docx',
                    'Документ 12.docx',
                    '2 Document.docx',
                    'Не Документ.docx',
                    '22 Document.docx',
                    'Italy.docx',
                    'Документ 22.docx',
                    'Документ 1.docx',
                    '12.docx',
                    '1.docx'
                ]
            );

            await setDocsSort.call(this, { order: 'asc' });
            assert.deepEqual(
                await bro.yaGetListingElementsTitles(),
                [
                    '1.docx',
                    '12.docx',
                    'Документ 1.docx',
                    'Документ 22.docx',
                    'Italy.docx',
                    '22 Document.docx',
                    'Не Документ.docx',
                    '2 Document.docx',
                    'Документ 12.docx',
                    'IT_pam.docx',
                    'Legneva.docx',
                    'Document One.docx',
                    'Документ 2.docx'
                ]
            );
        });

        it('diskclient-6538, diskclient-6638: Переключение раздела при загрузке ресурса', async function () {
            const bro = this.browser;

            await openDocsUrl.call(this, 'yndx-ufo-test-567');
            await bro.pause(500);
            const fileName = await bro.yaUploadFiles('test-file.xlsx', {
                inputSelector: docs.common.docsStub.upload(),
                waitForVisibleSelector: docs.common.docsStub(),
                uniq: true,
                selectFolder: true,
                target: 'docs-stub'
            });

            this.currentTest.ctx.items = [fileName];

            await bro.yaWaitForVisible(docs.common.docsPage.title().replace(':title:', DOCS_SECTION_TITLES.xlsx));
            await bro.yaAssertListingHas(fileName);
        });

        it('diskclient-6539, diskclient-6639: Переключение раздела при создании ресурса', async function () {
            const bro = this.browser;

            await openDocsUrl.call(this, 'yndx-ufo-test-567');

            await bro.yaWaitForVisible(docs.common.docsStub());
            await bro.click(docs.common.docsStub.pptx());

            await bro.yaWaitForVisible(docs.common.documentTitleDialog());

            const fileName = `tmp-${performance.now()}`;
            const fullFileName = fileName + '.pptx';
            this.currentTest.ctx.items = [fullFileName];

            await bro.yaSetValue(docs.common.documentTitleDialog.nameInput(), fileName);
            await bro.click(docs.common.documentTitleDialog.submitButton());

            const tabs = await bro.getTabIds();
            assert(tabs.length === 2, 'Новый таб не открылся');

            await bro.window(tabs[1]);
            let url;
            await bro.waitUntil(async () => {
                url = await bro.getUrl();
                return url.includes('/edit/disk/disk');
            });

            await bro.yaWaitForVisible(docs[await bro.yaIsMobile() ? 'touch' : 'desktop'].editorShareButton(), 10000);

            await bro.close();

            await bro.yaWaitForHidden(docs.common.documentTitleDialog());

            await bro.yaWaitForVisible(docs.common.docsPage.title().replace(':title:', DOCS_SECTION_TITLES.pptx));
            await bro.yaAssertListingHas(fullFileName);
        });

        it('diskclient-6540, diskclient-6640: Переключение раздела при открытии ресурса из Диска', async function () {
            const bro = this.browser;

            await bro.yaClientLoginFast('yndx-ufo-test-568');

            const fileName = await bro.yaUploadFiles('test-file.xlsx', { uniq: true });

            this.currentTest.ctx.items = [fileName];

            await openDocsUrl.call(this, 'yndx-ufo-test-568');

            await bro.yaWaitForVisible(docs.common.docsStub());
            await bro.click(docs.common.docsStub.open());

            await bro.yaWaitForVisible(docs.common.openFromDiskDialog());

            const fileSelector = listing.common.listingBodyItemsInfoXpath().replace(/:titleText/g, fileName);
            await bro.yaWaitForVisible(fileSelector);
            await bro.click(fileSelector);

            await bro.waitForEnabled(docs.common.openFromDiskDialog.acceptButton());
            await bro.click(docs.common.openFromDiskDialog.acceptButton());

            await bro.yaWaitForHidden(docs.common.openFromDiskDialog());

            await bro.yaWaitForVisible(docs.common.docsPage.title().replace(':title:', DOCS_SECTION_TITLES.xlsx));
            await bro.yaAssertListingHas(fileName);

            const tabs = await bro.getTabIds();
            assert(tabs.length === 2, 'Новый таб не открылся');

            await bro.window(tabs[1]);
            await bro.waitUntil(async () => {
                return (await bro.getUrl()).includes('/edit/disk/disk');
            });
            await bro.close();
        });

        it('diskclient-6542, diskclient-6642: Сортировка запиненых ресурсов', async function () {
            const bro = this.browser;

            await openDocsUrl.call(this, 'yndx-ufo-test-569');

            await bro.yaWaitForVisible(docs.common.docsListing());

            assert.deepEqual(
                await bro.yaGetListingElementsTitles(),
                [
                    '1.docx',
                    'Документ 1.docx',
                    'Документ 22.docx',
                    'IT_pam.docx',
                    'Italy.docx',
                    'Документ 12.docx',
                    '22 Document.docx',
                    'Не Документ.docx',
                    '2 Document.docx',
                    '12.docx',
                    'Legneva.docx',
                    'Document One.docx',
                    'Документ 2.docx'
                ]
            );

            await setDocsSort.call(this, { order: 'asc' });

            assert.deepEqual(
                await bro.yaGetListingElementsTitles(),
                [
                    '1.docx',
                    'Документ 1.docx',
                    'Документ 22.docx',
                    'IT_pam.docx',
                    'Italy.docx',
                    'Документ 2.docx',
                    'Document One.docx',
                    'Legneva.docx',
                    '12.docx',
                    '2 Document.docx',
                    'Не Документ.docx',
                    '22 Document.docx',
                    'Документ 12.docx'
                ]
            );

            await setDocsSort.call(this, { type: 'title', order: 'desc' });

            assert.deepEqual(
                await bro.yaGetListingElementsTitles(),
                [
                    '1.docx',
                    'Документ 1.docx',
                    'Документ 22.docx',
                    'IT_pam.docx',
                    'Italy.docx',
                    'Не Документ.docx',
                    'Документ 12.docx',
                    'Документ 2.docx',
                    'Legneva.docx',
                    'Document One.docx',
                    '22 Document.docx',
                    '12.docx',
                    '2 Document.docx'
                ]
            );

            await setDocsSort.call(this, { order: 'asc' });

            assert.deepEqual(
                await bro.yaGetListingElementsTitles(),
                [
                    '1.docx',
                    'Документ 1.docx',
                    'Документ 22.docx',
                    'IT_pam.docx',
                    'Italy.docx',
                    '2 Document.docx',
                    '12.docx',
                    '22 Document.docx',
                    'Document One.docx',
                    'Legneva.docx',
                    'Документ 2.docx',
                    'Документ 12.docx',
                    'Не Документ.docx'
                ]
            );

            await setDocsSort.call(this, { type: 'size', order: 'desc' });
            assert.deepEqual(
                await bro.yaGetListingElementsTitles(),
                [
                    '1.docx',
                    'Документ 1.docx',
                    'Документ 22.docx',
                    'IT_pam.docx',
                    'Italy.docx',
                    'Документ 2.docx',
                    'Document One.docx',
                    'Legneva.docx',
                    'Документ 12.docx',
                    '2 Document.docx',
                    'Не Документ.docx',
                    '22 Document.docx',
                    '12.docx'
                ]
            );

            await setDocsSort.call(this, { order: 'asc' });
            assert.deepEqual(
                await bro.yaGetListingElementsTitles(),
                [
                    '1.docx',
                    'Документ 1.docx',
                    'Документ 22.docx',
                    'IT_pam.docx',
                    'Italy.docx',
                    '12.docx',
                    '22 Document.docx',
                    'Не Документ.docx',
                    '2 Document.docx',
                    'Документ 12.docx',
                    'Legneva.docx',
                    'Document One.docx',
                    'Документ 2.docx'
                ]
            );
        });

        /**
         * @param {'my'|'other'} filter
         */
        async function setFilter(filter) {
            const bro = this.browser;
            const isMobile = await bro.yaIsMobile();

            if (isMobile) {
                await bro.click(docs.touch.touchListingSettingsButton());
                await bro.yaWaitForVisible(docs.touch.touchListingSettings());
                await bro.pause(500);

                await bro.click(docs.touch.touchListingSettings[`${filter}Filter`]());
                await bro.yaWaitForHidden(docs.touch.touchListingSettings());
            } else {
                await bro.click(docs.desktop.filterButton());
                await bro.yaWaitForVisible(docs.desktop.filterPopup());
                await bro.click(docs.desktop.filterPopup[filter]());
                await bro.yaWaitForHidden(docs.desktop.filterPopup());
            }
        }

        it('diskclient-6543, diskclient-6643: Фильтрация ресурсов (Опция "Только мои")', async function () {
            const bro = this.browser;
            const isMobile = await bro.yaIsMobile();

            await openDocsUrl.call(this, 'yndx-ufo-test-570');

            await bro.yaWaitForVisible(docs.common.docsListing());

            if (!isMobile) {
                await bro.click(docs.desktop.filterButton());
                await bro.yaWaitForVisible(docs.desktop.filterPopup());
                await bro.yaResetPointerPosition();
                await bro.pause(500);

                await bro.yaAssertView(isMobile ? 'diskclient-6643' : 'diskclient-6543', [
                    docs.desktop.filterButton(),
                    docs.desktop.filterPopup()
                ]);

                await bro.keys('Escape');
            }
            await setFilter.call(this, 'my');

            assert.deepEqual(
                await bro.yaGetListingElementsTitles(),
                [
                    '12.docx',
                    '1.docx',
                    '2 Document.docx',
                    'IT_pam.docx',
                    'Italy.docx',
                    '22 Document.docx',
                    'Document One.docx',
                    'Не Документ.docx',
                    'Документ 2.docx',
                    'Документ 12.docx',
                    'Документ 22.docx',
                    'Документ 1.docx',
                    'Legneva.docx'
                ]
            );
        });

        it('diskclient-6544, diskclient-6644: Фильтрация ресурсов (Опция "Со мной поделилились")', async function () {
            const bro = this.browser;

            await openDocsUrl.call(this, 'yndx-ufo-test-570');

            await bro.yaWaitForVisible(docs.common.docsListing());

            await setFilter.call(this, 'other');
            assert.deepEqual(
                await bro.yaGetListingElementsTitles(),
                // Хозяин документов: yndx-ufo-test-571
                [
                    'Тестовый чужой документ 4.docx',
                    'Тестовый чужой документ 3.docx',
                    'Тестовый чужой документ 2.docx',
                    'Тестовый чужой документ 1.docx'
                ]
            );
        });

        it('diskclient-6545, diskclient-6645: Заглушка фильтра личных документов', async function() {
            const bro = this.browser;
            const isMobile = await bro.yaIsMobile();

            await openDocsUrl.call(this, 'yndx-ufo-test-572');

            await bro.yaWaitForVisible(docs.common.docsListing());

            await setFilter.call(this, 'my');
            await bro.yaWaitForVisible(docs.common.emptyFilterStub());
            await bro.yaAssertView(
                isMobile ? 'diskclient-6645' : 'diskclient-6545',
                docs.common.docsPage.innerWrapper()
            );
        });

        it('diskclient-6546, diskclient-6646: Заглушка фильтра чужих документов', async function() {
            const bro = this.browser;
            const isMobile = await bro.yaIsMobile();

            await openDocsUrl.call(this, 'yndx-ufo-test-569');

            await bro.yaWaitForVisible(docs.common.docsListing());

            await setFilter.call(this, 'other');
            await bro.yaWaitForVisible(docs.common.emptyFilterStub());
            await bro.yaAssertView(
                isMobile ? 'diskclient-6646' : 'diskclient-6546',
                docs.common.docsPage.innerWrapper()
            );
        });

        it('diskclient-6547, diskclient-6647: Заглушки нового пользователя', async function() {
            const bro = this.browser;
            const isMobile = await bro.yaIsMobile();

            await openDocsUrl.call(this, 'yndx-ufo-test-573');

            await bro.yaWaitForVisible(docs.common.docsStub());

            await bro.yaAssertView(
                isMobile ? 'diskclient-6647' : 'diskclient-6547',
                docs.common.docsPage.innerWrapper()
            );
        });

        /**
         * @param {string} testId
         * @param {string} fileName
         */
        async function testContextMenu(testId, fileName) {
            const bro = this.browser;
            const isMobile = await bro.yaIsMobile();

            await openDocsUrl.call(this, 'yndx-ufo-test-574');

            await bro.yaWaitForVisible(docs.common.docsListing());

            await bro.yaOpenActionPopup(fileName);

            await bro.pause(500);

            const itemSelector = listing.common.listingBodyItemsXpath().replace(/:titleText/g, fileName);

            await bro.yaAssertView(testId, isMobile ?
                [clientCommon.touch.drawerHandle(), clientCommon.touch.drawerContent()] :
                [popups.common.actionPopup(), itemSelector]
            );
        }

        it('diskclient-6556, diskclient-6655: Вызов контекстного меню для своих документов', async function() {
            const testId = await this.browser.yaIsMobile() ? 'diskclient-6655' : 'diskclient-6556';
            await testContextMenu.call(this, testId, 'Мой документ.docx');
        });

        it('diskclient-6557, diskclient-6656: Вызов контекстного меню для чужих документов', async function() {
            const testId = await this.browser.yaIsMobile() ? 'diskclient-6656' : 'diskclient-6557';
            // Хозяин документа: yndx-ufo-test-571
            await testContextMenu.call(this, testId, 'Тестовый чужой документ 1.docx');
        });

        it('diskclient-6558, diskclient-6657: Вызов контекстного меню для документов в ОП с read_only правами', async function() {
            const testId = await this.browser.yaIsMobile() ? 'diskclient-6657' : 'diskclient-6558';
            // хозяин папки yndx-ufo-test-571
            await testContextMenu.call(this, testId, 'Документ из RO папки.docx');
        });

        // hermione.only.in(clientDesktopBrowsersList);
        hermione.skip.notIn('', 'https://st.yandex-team.ru/CHEMODAN-84264');
        it('diskclient-6559: Вызов контекстного меню из трех точек на ресурсе', async function() {
            const bro = this.browser;

            await openDocsUrl.call(this, 'yndx-ufo-test-574');

            await bro.yaWaitForVisible(docs.common.docsListing());

            const itemSelector = listing.common.listingBodyItemsXpath().replace(/:titleText/g, 'Мой документ.docx');
            const item = await bro.$(itemSelector);

            await item.moveTo();

            const resourceActionsButtons = await item.$$(docs.common.resourceActionsButton());

            let resourceActionsButton;

            for await (const button of resourceActionsButtons) {
                if (await button.isDisplayed()) {
                    resourceActionsButton = button;
                    break;
                }
            }

            assert(resourceActionsButton, 'Кнопка действий не появилась');

            await resourceActionsButton.click();
            await bro.yaWaitForVisible(popups.common.actionPopup());
            await bro.yaResetPointerPosition();
            await bro.pause(500);

            await bro.yaAssertView('diskclient-6559', [popups.common.actionPopup(), itemSelector]);
        });
    });

    describe('Переход из Доксов ->', async () => {
        /**
         * @param {string} fileName
         */
        async function assertGoToFile(fileName) {
            const bro = this.browser;

            const tabs = await bro.getTabIds();
            assert(tabs.length === 2, 'Новый таб не открылся');

            await bro.switchTab(tabs[1]);

            await bro.yaAssertListingHas(fileName);
            await bro.yaWaitActionBarDisplayed();

            const fileSelector = listing.common.listingBodyItemsInfoXpath().replace(/:titleText/g, fileName);
            const inViewport = await bro.isVisibleWithinViewport(fileSelector);

            assert(inViewport === true, 'Не произошел подскрол к ресурсу');
        }

        it('diskclient-6560, diskclient-6658: Переход к ресурсу на Диске', async function() {
            const bro = this.browser;

            await openDocsUrl.call(this, 'yndx-ufo-test-574');

            await bro.yaWaitForVisible(docs.common.docsListing());

            const fileName = 'Мой документ.docx';
            await bro.yaCallActionInActionPopup(fileName, 'goToFile', false);

            await assertGoToFile.call(this, fileName);
        });

        hermione.only.in(clientDesktopBrowsersList);
        it('diskclient-6561: Переход к ресурсу на Диске из загрузчика', async function() {
            const bro = this.browser;

            await openDocsUrl.call(this, 'yndx-ufo-test-575');

            const fileName = await bro.yaUploadFiles('test-file.xlsx', {
                uniq: true,
                selectFolder: true,
                target: 'docs-sidebar',
                closeUploader: false
            });

            this.currentTest.ctx.items = [fileName];

            await bro.rightClick(popups.common.uploader.listingItem());
            await bro.yaWaitForVisible(popups.common.actionPopup.goToFileButton());
            await bro.click(popups.common.actionPopup.goToFileButton());

            await assertGoToFile.call(this, fileName);
        });

        it('diskclient-6562, diskclient-6659: Переход по ссылке "Справка и поддержка"', async function() {
            const bro = this.browser;

            await openDocsUrl.call(this, 'yndx-ufo-test-561');

            await bro.yaWaitForVisible(footer.common.footer());
            await bro.yaClickAndAssertNewTabUrl(
                footer.common.footerHelpAndSupportLink(),
                {
                    linkShouldContain: await bro.yaIsMobile() ?
                        'https://yandex.ru/support/docs-mobile' :
                        'https://yandex.ru/support/docs'
                }
            );
        });

        hermione.only.in(clientDesktopBrowsersList);
        it('diskclient-6563: Переход по ссылке "Условия использования"', async function() {
            const bro = this.browser;

            await openDocsUrl.call(this, 'yndx-ufo-test-561');

            await bro.yaWaitForVisible(footer.common.footer());
            await bro.yaClickAndAssertNewTabUrl(
                footer.common.footerRulesLink(),
                { linkShouldContain: 'https://yandex.ru/legal/docs_termsofuse' }
            );
        });

        hermione.only.in(clientDesktopBrowsersList);
        it('diskclient-6564: Переход по ссылке "Участие в исследованиях"', async function() {
            const bro = this.browser;

            await openDocsUrl.call(this, 'yndx-ufo-test-561');

            await bro.yaWaitForVisible(footer.common.footer());
            await bro.yaClickAndAssertNewTabUrl(
                footer.common.footerResearchesLink(),
                { linkShouldContain: ['https://yandex.ru/jobs/', 'usability'] }
            );
        });
    });

    const EXISTING_FILES = {
        docx: ['Legneva.doc', 'beidzh.doc', 'Italy.doc'],
        xlsx: ['Acts.xlsx', 'p2p-сетка.xlsx', 'table.xlsx'],
        pptx: ['Презентация.pptx', 'Преза.pptx', 'Новая презентация.pptx']
    };

    /**
     * @param {'docx'|'xlsx'|'pptx'} type
     * @param {string} fileName
     */
    async function checkNewFilesOrder(type, fileName) {
        const bro = this.browser;

        await bro.yaAssertListingHas(fileName);

        const titles = await bro.yaGetListingElementsTitles();

        const uploadedFileInStart = EXISTING_FILES[type].every((existingFile) => {
            return titles.indexOf(existingFile) > titles.indexOf(fileName);
        });

        assert(uploadedFileInStart === true, 'Загруженный файл не в начале списка');
    }

    describe('Загрузка ->', async () => {
        /**
         * @param {'docx'|'xlsx'|'pptx'} type
         */
        async function testUploadFromSidebar(type) {
            const bro = this.browser;
            const yaIsMobile = await bro.yaIsMobile();

            await openDocsUrl.call(this, 'yndx-ufo-test-576');

            const fileName = await bro.yaUploadFiles(`test-file.${type}`, {
                uniq: true,
                selectFolder: true,
                target: yaIsMobile ? 'docs-plus' : 'docs-sidebar'
            });

            this.currentTest.ctx.items = [fileName];

            await checkNewFilesOrder.call(this, type, fileName);
        }

        /**
         * @param {'docx'|'xlsx'|'pptx'} type
         */
        async function testUploadFromToolbar(type) {
            const bro = this.browser;

            await openDocsUrl.call(this, 'yndx-ufo-test-577');

            await bro.pause(1000); // https://st.yandex-team.ru/CHEMODAN-77461
            await openDocsSection.call(this, type);

            const fileName = await bro.yaUploadFiles(`test-file.${type}`, {
                uniq: true,
                selectFolder: true,
                waitForVisibleSelector: docs.desktop.docsToolbar.upload(),
                inputSelector: docs.desktop.docsToolbar.upload.input()
            });

            this.currentTest.ctx.items = [fileName];

            await checkNewFilesOrder.call(this, type, fileName);
        }

        it('diskclient-6454, diskclient-6578: Загрузка документа из сайдбара', async function() {
            await testUploadFromSidebar.call(this, 'docx');
        });

        hermione.skip.notIn('', 'Падает - https://st.yandex-team.ru/CHEMODAN-79572');
        it('diskclient-6455, diskclient-6579: Загрузка таблицы из сайдбара', async function() {
            await testUploadFromSidebar.call(this, 'xlsx');
        });

        it('diskclient-6456, diskclient-6580: Загрузка презентации из сайдбара', async function() {
            await testUploadFromSidebar.call(this, 'pptx');
        });

        hermione.only.in(clientDesktopBrowsersList);
        it('diskclient-6457: Загрузка документа из контролов', async function() {
            await testUploadFromToolbar.call(this, 'docx');
        });

        hermione.only.in(clientDesktopBrowsersList);
        hermione.skip.notIn('', 'https://st.yandex-team.ru/CHEMODAN-83407');
        it('diskclient-6458: Загрузка таблицы из контролов', async function() {
            await testUploadFromToolbar.call(this, 'xlsx');
        });

        hermione.only.in(clientDesktopBrowsersList);
        hermione.skip.notIn('', 'https://st.yandex-team.ru/CHEMODAN-83407');
        it('diskclient-6459: Загрузка презентации из контролов', async function() {
            await testUploadFromToolbar.call(this, 'pptx');
        });

        hermione.skip.notIn('', 'https://st.yandex-team.ru/CHEMODAN-80037');
        it('diskclient-6466, diskclient-6587: Загрузка нескольких разноформатных ресурсов', async function() {
            const bro = this.browser;
            const isMobile = await bro.yaIsMobile();

            await openDocsUrl.call(this, 'yndx-ufo-test-578');

            await bro.pause(1000); // https://st.yandex-team.ru/CHEMODAN-77461
            await openDocsSection.call(this, 'xlsx');

            const fileNames = await bro.yaUploadFiles(['test-file.docx', 'test-file.xlsx', 'test-file.pptx'], {
                uniq: true,
                selectFolder: true,
                target: isMobile ? 'docs-plus' : 'docs-sidebar'
            });

            this.currentTest.ctx.items = fileNames;

            await bro.yaAssertListingHas(fileNames[1]);
            await bro.yaAssertListingHasNot(fileNames[0], true);
            await bro.yaAssertListingHasNot(fileNames[2], true);

            await openDocsSection.call(this, 'docx');
            await bro.yaAssertListingHas(fileNames[0]);
            await bro.yaAssertListingHasNot(fileNames[1], true);
            await bro.yaAssertListingHasNot(fileNames[2], true);

            await openDocsSection.call(this, 'pptx');
            await bro.yaAssertListingHas(fileNames[2]);
            await bro.yaAssertListingHasNot(fileNames[1], true);
            await bro.yaAssertListingHasNot(fileNames[0], true);
        });
    });

    describe('Открытие из Диска ->', async () => {
        it('diskclient-6478, diskclient-6595: Открытие документа из сайдбара', async function () {
            const bro = this.browser;
            const isMobile = await bro.yaIsMobile();

            await bro.yaClientLoginFast('yndx-ufo-test-579');

            const fileName = await bro.yaUploadFiles('test-file.docx', { uniq: true });

            this.currentTest.ctx.items = [fileName];

            await openDocsUrl.call(this, 'yndx-ufo-test-579');

            if (isMobile) {
                await bro.yaWaitForVisible(docs.touch.docsCreateButton());
                await bro.click(docs.touch.docsCreateButton());
                await bro.yaWaitForVisible(docs.touch.docsCreateDrawer());
                await bro.pause(500);
                await bro.click(docs.touch.docsCreateDrawer.open());
                await bro.yaWaitForHidden(docs.touch.docsCreateDrawer());
            } else {
                await bro.yaWaitForVisible(docs.desktop.docsSidebar.createButton());
                await bro.click(docs.desktop.docsSidebar.createButton());
                await bro.yaWaitForVisible(docs.desktop.docsSidebarCreatePopup());
                await bro.click(docs.desktop.docsSidebarCreatePopup.open());
            }

            await bro.yaWaitForVisible(docs.common.openFromDiskDialog());
            const listingIemSelector = listing.common.listingBodyItemsXpath().replace(/:titleText/g, fileName);
            const dialogListingItemSelector = [
                '//div[contains(@class, "Open-From-Disk__Confirm-Dialog")]',
                listingIemSelector
            ].join('');

            await bro.yaWaitForVisible(dialogListingItemSelector);
            await bro.click(dialogListingItemSelector);
            await bro.waitForEnabled(docs.common.openFromDiskDialog.acceptButton());
            await bro.click(docs.common.openFromDiskDialog.acceptButton());

            const tabs = await bro.getTabIds();
            assert(tabs.length === 2, 'Редактор не открылся');

            await bro.window(tabs[1]);
            await bro.close();

            await bro.window(tabs[0]);

            await bro.yaWaitForHidden(docs.common.openFromDiskDialog());

            await checkNewFilesOrder.call(this, 'docx', fileName);
        });
    });

    describe('Действия над документами ->', async () => {
        /**
         * @param {string} fileName
         * @param {string} action
         */
        async function callActionInResourceActionPopup(fileName, action) {
            const bro = this.browser;

            const listingIemSelector = listing.common.listingBodyItemsXpath().replace(/:titleText/g, fileName);
            await bro.yaWaitForVisible(listingIemSelector);

            await retriable(async() => {
                await bro.yaOpenActionPopup(fileName);
            }, 5, 500);

            await bro.click(popups.common.actionPopup[`${action}Button`]());
        }

        hermione.only.in(clientDesktopBrowsersList);
        it('diskclient-6502: Поделение ресурсом из контекстного меню', async function () {
            const bro = this.browser;
            const isMobile = await bro.yaIsMobile();

            await openDocsUrl.call(this, 'yndx-ufo-test-580');

            const fileName = await bro.yaUploadFiles('test-file.docx', {
                uniq: true,
                selectFolder: true,
                target: isMobile ? 'docs-plus' : 'docs-sidebar'
            });

            this.currentTest.ctx.items = [fileName];

            await callActionInResourceActionPopup.call(this, fileName, 'publish');

            await bro.yaWaitForVisible(popups.common.shareDialog());
            await bro.yaWaitForVisible(popups.common.shareDialogEditAccessTypeTitle());

            if (isMobile) {
                await bro.yaExecuteClick(clientNavigation.touch.modalCell());
            } else {
                await bro.click(popups.common.shareDialog.closeButton());
            }

            await bro.yaWaitForHidden(popups.common.shareDialog());
        });

        it('diskclient-6510, diskclient-6614: Редактирование ресурса из контекстного меню', async function () {
            const bro = this.browser;

            await openDocsUrl.call(this, 'yndx-ufo-test-581');

            await callActionInResourceActionPopup.call(this, 'тест.docx', 'edit');

            const tabs = await bro.getTabIds();
            assert(tabs.length === 2, 'Редактор не открылся');

            await bro.switchTab(tabs[1]);

            let url;
            await bro.waitUntil(async () => {
                url = await bro.getUrl();
                return url.includes('disk.yandex.ru');
            });

            const parsedUrl = new URL(url);
            assert(
                decodeURIComponent(parsedUrl.pathname) === '/edit/disk/disk/тест.docx' &&
                parsedUrl.searchParams.get('source') === 'docs',
                'Некорректный урл редактора'
            );
        });

        it('diskclient-6513, diskclient-6616: Скачивание ресурса', async function () {
            const bro = this.browser;

            await openDocsUrl.call(this, 'yndx-ufo-test-581');

            const url = await bro.yaGetDownloadUrlFromAction(async () => {
                await callActionInResourceActionPopup.call(this, 'тест.docx', 'download');
            });

            assert.match(
                url,
                /downloader\.disk\.yandex\.ru\/disk\/.+&filename=%D1%82%D0%B5%D1%81%D1%82\.docx/,
                'Некорректный url для скачивания'
            );
        });

        hermione.skip.notIn('', 'файл становится антиФО и тест падает - https://st.yandex-team.ru/CHEMODAN-79462');
        it('diskclient-6514, diskclient-6617: Скачивание чужого ресурса', async function () {
            const bro = this.browser;

            await openDocsUrl.call(this, 'yndx-ufo-test-581');

            const url = await bro.yaGetDownloadUrlFromAction(async () => {
                // Хозяин документа: yndx-ufo-test-571
                await callActionInResourceActionPopup.call(this, 'Тестовый чужой документ 1.docx', 'download');
            });

            assert.match(
                url,
                // eslint-disable-next-line max-len
                /downloader\.disk\.yandex\.ru\/disk\/.+uid=0&filename=%D0%A2%D0%B5%D1%81%D1%82%D0%BE%D0%B2%D1%8B%D0%B9%20%D1%87%D1%83%D0%B6%D0%BE%D0%B9%20%D0%B4%D0%BE%D0%BA%D1%83%D0%BC%D0%B5%D0%BD%D1%82%201\.docx.+&owner_uid=1013657385/,
                'Некорректный url для скачивания'
            );
        });

        hermione.only.in(clientDesktopBrowsersList);
        it('diskclient-6516: Переименование ресурса', async function () {
            const bro = this.browser;
            const isMobile = await bro.yaIsMobile();

            await openDocsUrl.call(this, 'yndx-ufo-test-582');

            const fileName = await bro.yaUploadFiles('test-file.docx', {
                uniq: true,
                selectFolder: true,
                target: isMobile ? 'docs-plus' : 'docs-sidebar'
            });

            const newFilename = 'RENAMED_' + fileName;

            this.currentTest.ctx.items = [fileName, newFilename];

            await callActionInResourceActionPopup.call(this, fileName, 'rename');

            await bro.yaWaitForVisible(popups.common.renameDialog.nameInput());
            await bro.yaSetValue(popups.common.renameDialog.nameInput(), newFilename);
            await bro.click(popups.common.renameDialog.submitButton());
            await bro.yaWaitForHidden(popups.common.renameDialog());

            await bro.yaAssertListingHas(newFilename);
        });

        hermione.skip.notIn('', 'https://st.yandex-team.ru/CHEMODAN-83407');
        it('diskclient-6519, diskclient-6620: Восстановление предыдущей версии ресурса (сохранить как копию)', async function () {
            const bro = this.browser;
            const isMobile = await bro.yaIsMobile();

            await bro.yaClientLoginFast('yndx-ufo-test-583');
            const fileName = 'test-file.docx';

            await bro.yaUploadFiles(fileName, { replace: true });

            await openDocsUrl.call(this, 'yndx-ufo-test-583');

            await callActionInResourceActionPopup.call(this, fileName, 'versions');

            if (isMobile) {
                await bro.yaWaitForVisible(versions.touch.moreButton());
                await bro.pause(500);
                await bro.click(versions.touch.moreButton());
                await bro.yaWaitForVisible(versions.touch.restoreButton());
                await bro.click(versions.touch.restoreButton());
            } else {
                await bro.yaWaitForVisible(versions.common.versionsDialog.versionItemWithActions());
                await bro.moveToObject(versions.common.versionsDialog.versionItemWithActions());
                await bro.waitForVisible(versions.common.versionsDialog.restoreButton());
                await bro.click(versions.common.versionsDialog.restoreButton());
            }

            await bro.yaWaitForVisible(versions.common.versionsRestoreDialog());
            const date = await bro.getAttribute(versions.common.versionsRestoreDialog.content(), 'data-date');

            const items = await bro.yaGetListingElementsTitles();
            const duplicates = items.filter((fileName) => fileName.includes(date));
            const suffix = duplicates.length > 0 ? ` (${duplicates.length})` : '';
            const newFilename = `test-file (версия от ${date})${suffix}.docx`;

            this.currentTest.ctx.items = [newFilename];

            await bro.click(versions.common.versionsRestoreDialog.saveAsCopyButton());

            await bro.yaAssertListingHas(newFilename);
        });

        hermione.only.in(clientDesktopBrowsersList);
        it('diskclient-6521: Удаление ресурса', async function () {
            const bro = this.browser;
            const isMobile = await bro.yaIsMobile();

            await openDocsUrl.call(this, 'yndx-ufo-test-584');

            const fileName = await bro.yaUploadFiles('test-file.docx', {
                uniq: true,
                selectFolder: true,
                target: isMobile ? 'docs-plus' : 'docs-sidebar'
            });

            this.currentTest.ctx.items = [fileName];

            await callActionInResourceActionPopup.call(this, fileName, 'delete');

            await bro.yaAssertListingHasNot(fileName, true);
            await bro.yaWaitNotificationForResource(fileName, consts.TEXT_NOTIFICATION_FILE_MOVED_TO_TRASH);
        });

        hermione.skip.notIn('', 'нужен GC – https://st.yandex-team.ru/CHEMODAN-79463');
        it('diskclient-6523, diskclient-6624: Удаление чужого ресурса', async function () {
            const bro = this.browser;
            const isMobile = await bro.yaIsMobile();

            await openDocsUrl.call(this, 'yndx-ufo-test-585');

            // Хозяин документов: yndx-ufo-test-571
            const DOCS = {
                'Тестовый чужой документ 1.docx': 'https://disk.yandex.ru/i/TEyhWuupxnnwQQ',
                'Тестовый чужой документ 2.docx': 'https://disk.yandex.ru/i/BqcyYIrGArEw9w',
                'Тестовый чужой документ 3.docx': 'https://disk.yandex.ru/i/IFPB11mQOHMDxw',
                'Тестовый чужой документ 4.docx': 'https://disk.yandex.ru/d/AZytseqVCgtiPg',
                'Тестовый чужой документ 5.docx': 'https://disk.yandex.ru/i/nZQgB1uk-pSRrw',
                'Тестовый чужой документ 6.docx': 'https://disk.yandex.ru/i/K3l89LfTy89rfw',
                'Тестовый чужой документ 7.docx': 'https://disk.yandex.ru/i/Pe49rbysr-qzIA',
                'Тестовый чужой документ 8.docx': 'https://disk.yandex.ru/i/KDuNNXNlOocL3Q',
                'Тестовый чужой документ 9.docx': 'https://disk.yandex.ru/i/XUJFXaHGg0t10Q',
            };

            // Документ может добавиться от параллельного прогона, нужно выбрать документ которого ещё нет в листинге
            const items = await bro.yaGetListingElementsTitles();
            const fileName = _.shuffle(Object.keys(DOCS)).find((doc) => !items.includes(doc));

            await bro.newWindow(DOCS[fileName]);
            await bro.yaWaitForVisible(docs[isMobile ? 'touch' : 'desktop'].editorShareButton(), 20000);

            const tabs = await bro.getTabIds();
            await bro.window(tabs[0]);

            await callActionInResourceActionPopup.call(this, fileName, 'removeFromDocs');
            await bro.yaAssertListingHasNot(fileName, true);
        });

        /**
         *
         */
        async function assertDVOpened() {
            const bro = this.browser;

            const isMobile = await bro.yaIsMobile();

            if (isMobile) {
                const tabs = await bro.getTabIds();
                assert(tabs.length === 2, 'DV не открылся');

                await bro.switchTab(tabs[1]);

                let url = '';
                await bro.waitUntil(async () => {
                    url = await bro.getUrl();
                    return !['blank', 'about:blank'].includes(url);
                });

                assert(
                    /^https:\/\/docviewer(?:\d?\.dsp)?\.yandex\.ru\/view\/1013657402\/\?\*=/.test(url),
                    'Некорректный урл DV: ' + url
                );
            } else {
                const url = await bro.getUrl();
                assert(url.startsWith(getDocsUrl(bro) + '/docs/view?url='), 'DV не открылся');
            }
        }

        it('diskclient-6525, diskclient-6682: Открытие ресурса в DV', async function () {
            const bro = this.browser;

            await openDocsUrl.call(this, 'yndx-ufo-test-574');

            const listingIemSelector = listing.common.listingBodyItemsXpath()
                .replace(/:titleText/g, 'Документ из RO папки.docx'); // Хозяин папки: yndx-ufo-test-571
            await bro.yaWaitForVisible(listingIemSelector);

            await bro.click(listingIemSelector);

            await assertDVOpened.call(this);
        });

        it('diskclient-6526, diskclient-6626: Открытие ресурса в DV из контекстного меню', async function () {
            await openDocsUrl.call(this, 'yndx-ufo-test-574');

            // Хозяин папки: yndx-ufo-test-571
            await callActionInResourceActionPopup.call(this, 'Документ из RO папки.docx', 'view');

            await assertDVOpened.call(this);
        });
    });
});
