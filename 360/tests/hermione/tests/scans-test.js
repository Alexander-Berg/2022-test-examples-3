// yndx-ufo-test-622 - юзер с документами для скринов контента/слайдеров/сортировки и прочего
// yndx-ufo-test-623 - юзер с пустой папкой сканов

const listing = require('../page-objects/client-content-listing');
const { assert } = require('chai');
const clientCommon = require('../page-objects/client-common');
const client = require('../page-objects/client');
const docs = require('../page-objects/docs');
const consts = require('../config').consts;
const {
    getDocsUrl,
    openDocsSection,
    setDocsSort,
    openDocsSort,
    closeDocsSort,
} = require('../helpers/docs');
const { NAVIGATION } = require('../config').consts;
const SCANS_TEST_ID = 395492;

const testFile = 'test-file.jpg';

/**
 * @param {Object} options
 */
async function openDocsScans(options = {}) {
    const bro = this.browser;
    let url = bro.options.baseUrl
        .replace(/\.regtests\./, '.docs.regtests.')
        .replace('disk', 'docs');
    if (url.match(/\?\w*=?.*/)) {
        url = url + `&test-id=${SCANS_TEST_ID}`;
    } else {
        url = url + `?test-id=${SCANS_TEST_ID}`;
    }

    await bro.url(url.toString());
    await openDocsSection.call(this, 'scans', options.isStub);
    if (!options.isStub) {
        await bro.yaWaitForVisible(docs.common.docsListing());
    }
}

describe('Сканы в доксах', () => {
    it('diskclient-6691, 6710, Открытие слайдера в сканах', async function () {
        const bro = this.browser;
        const testpalmId = (await bro.yaIsMobile()) ?
            'diskclient-6710' :
            'diskclient-6691';
        await bro.yaClientLoginFast('yndx-ufo-test-622');
        await openDocsScans.call(this);
        bro.click(docs.common.scansListingItem().replace(
            /:titleText/g,
            'img1.jpg'
        ));
        await bro.pause(1000); // wait unhover animation
        await bro.yaAssertView(testpalmId, 'body');
    });

    it('diskclient-6702, 6720, Отображение контекстного меню в слайдере в сканах', async function () {
        const bro = this.browser;
        const testpalmId = (await bro.yaIsMobile()) ?
            'diskclient-6720' :
            'diskclient-6702';
        await bro.yaClientLoginFast('yndx-ufo-test-622');
        await openDocsScans.call(this);
        bro.click(docs.common.scansListingItem().replace(
            /:titleText/g,
            'img1.jpg'
        ));
        await bro.pause(1000);
        bro.click(docs.common.scansSliderMoreButton());

        await bro.pause(1500);

        await bro.yaAssertView(testpalmId, 'body');
    });

    it('diskclient-6799, 6801, Открытие DV на просмотр документов', async function () {
        const bro = this.browser;
        await bro.yaClientLoginFast('yndx-ufo-test-622');
        await openDocsScans.call(this);
        bro.click(docs.common.scansListingItem().replace(
            /:titleText/g,
            'pdf1.pdf'
        ));
        await bro.pause(300);

        const isMobile = await bro.yaIsMobile();
        if (isMobile) {
            const tabIds = await bro.getTabIds();
            await bro.switchTab(tabIds[1]);
            await bro.pause(1000);
        }

        const url = await bro.getUrl();
        const isDvUrl = url.startsWith(
            isMobile ?
                'https://docviewer' :
                getDocsUrl(bro) + '/docs/view?url='
        );
        if (!isDvUrl) {
            throw new Error('docviewer have not been open');
        }
    });

    it('diskclient-6690, 6709, КМ ресурса в сканах', async function () {
        const bro = this.browser;
        const isMobile = await bro.yaIsMobile();
        const testpalmId = isMobile ? 'diskclient-6709' : 'diskclient-6690';
        await bro.yaClientLoginFast('yndx-ufo-test-622');
        await openDocsScans.call(this);

        if (isMobile) {
            const selector = docs.common.scansListingItem().replace(/:titleText/g, 'img1.jpg');
            await bro.yaLongPress(selector);
        } else {
            bro.rightClick(docs.common.scansListingItem().replace(
                /:titleText/g,
                'img1.jpg'
            ));
            await bro.pause(1000);
        }
        await bro.yaAssertView(testpalmId, docs.common.docsPage.innerWrapper());
    });

    it('diskclient-6755, 6812, Открытие папки в диске из сканов', async function () {
        const bro = this.browser;
        await bro.yaClientLoginFast('yndx-ufo-test-622');
        await openDocsScans.call(this);
        bro.click(docs.common.scansListingItem().replace(
            /:titleText/g,
            'Новая папка'
        ));
        await bro.pause(300);
        const tabIds = await bro.getTabIds();
        await bro.switchTab(tabIds[1]);
        await bro.pause(2000);
        await bro.yaWaitForVisible(listing.common.listing.head.title().replace(':title', 'Новая папка'));
    });

    it('diskclient-6753, 6804, Отображение заглушки в сканах в доксах', async function () {
        const bro = this.browser;
        const testpalmId = (await bro.yaIsMobile()) ?
            'diskclient-6804' :
            'diskclient-6753';
        await bro.yaClientLoginFast('yndx-ufo-test-623');
        await openDocsScans.call(this, { isStub: true });
        await bro.yaWaitForVisible(docs.common.scansStub());
        await bro.yaAssertView(testpalmId, docs.common.docsPage.innerWrapper());
    });

    it('diskclient-6750, 6805,Отображение заглушки в папке Сканы в диске', async function () {
        const bro = this.browser;
        const testpalmId = (await bro.yaIsMobile()) ?
            'diskclient-6805' :
            'diskclient-6750';
        await bro.yaClientLoginFast('yndx-ufo-test-623');
        await bro.url(
            `${NAVIGATION.disk.url}/%D0%A1%D0%BA%D0%B0%D0%BD%D1%8B?test-id=${SCANS_TEST_ID}`
        );
        await bro.pause(1000);

        await bro.yaAssertView(testpalmId, docs.common.rootContentContainer());
    });

    hermione.skip.in('chrome-phone', 'Тест только для дэсктопа');
    it('diskclient-6749, Открытие папки Сканы из сайдбара', async function () {
        const bro = this.browser;
        await bro.yaClientLoginFast('yndx-ufo-test-622');
        await bro.url(`${NAVIGATION.disk.url}?test-id=${SCANS_TEST_ID}`);
        await bro.click(client.scansInSidebar());
        await bro.yaWaitForVisible(docs.common.listingItems());
        await bro.yaAssertView('diskclient-6749', docs.common.rootContentContainer());
    });

    it('diskclient-6817, 6818, Сортировка в сканах в доксах', async function () {
        const bro = this.browser;
        const isMobile = await bro.yaIsMobile();
        const testpalmId = isMobile ? 'diskclient-6817' : 'diskclient-6818';
        await bro.yaClientLoginFast('yndx-ufo-test-622');

        await openDocsScans.call(this);

        await bro.yaWaitForVisible(docs.common.docsListing());

        await openDocsSort.call(this);
        await bro.yaWaitPreviewsLoaded(docs.common.docsListing.preview());
        await bro.yaAssertView(
            testpalmId,
            isMobile ?
                [
                    clientCommon.touch.drawerHandle(),
                    clientCommon.touch.drawerContent(),
                ] :
                [
                    docs.common.docsPage.titleWrapper(),
                    docs.desktop.sortPopup(),
                ]
        );
        await closeDocsSort.call(this);
        assert.deepEqual(await bro.yaGetListingElementsTitles(), [
            'Новая папка',
            'img1.jpg',
            'img2.jpg',
            'img3.jpg',
            'img4.jpg',
            'img5.jpg',
            'pdf1.pdf',
        ]);

        await setDocsSort.call(this, { type: 'title' });
        await bro.pause(1000);
        await bro.yaWaitForVisible(docs.common.docsListing.item());
        assert.deepEqual(await bro.yaGetListingElementsTitles(), [
            'Новая папка',
            'pdf1.pdf',
            'img5.jpg',
            'img4.jpg',
            'img3.jpg',
            'img2.jpg',
            'img1.jpg',
        ]);

        await setDocsSort.call(this, { order: 'asc' });
        await bro.pause(1000);
        await bro.yaWaitForVisible(docs.common.docsListing.item());
        assert.deepEqual(await bro.yaGetListingElementsTitles(), [
            'Новая папка',
            'img1.jpg',
            'img2.jpg',
            'img3.jpg',
            'img4.jpg',
            'img5.jpg',
            'pdf1.pdf',
        ]);

        await setDocsSort.call(this, { type: 'size' });
        await bro.pause(1000);
        await bro.yaWaitForVisible(docs.common.docsListing.item());
        assert.deepEqual(await bro.yaGetListingElementsTitles(), [
            'Новая папка',
            'img4.jpg',
            'img2.jpg',
            'img3.jpg',
            'img1.jpg',
            'img5.jpg',
            'pdf1.pdf',
        ]);

        await setDocsSort.call(this, { order: 'desc' });
        await bro.pause(1000);
        await bro.yaWaitForVisible(docs.common.docsListing.item());
        assert.deepEqual(await bro.yaGetListingElementsTitles(), [
            'Новая папка',
            'pdf1.pdf',
            'img5.jpg',
            'img1.jpg',
            'img3.jpg',
            'img2.jpg',
            'img4.jpg',
        ]);
    });

    hermione.only.notIn('chrome-phone');
    hermione.auth.createAndLogin({
        language: 'ru',
        tus_consumer: 'disk-front-client',
    });
    it('diskclient-6808, Отображение заглушки после удаления единственного файла в папке Сканы в диске', async function () {
        const bro = this.browser;
        const testpalmId = 'diskclient-6808';
        await bro.yaSkipWelcomePopup();
        await bro.url(`${NAVIGATION.disk.url}?test-id=${SCANS_TEST_ID}`);
        const folderName = 'Сканы';
        await bro.yaCreateFolder(folderName);

        await bro.yaOpenListingElement(folderName);
        await bro.yaUploadFiles(testFile);
        const popupExist = await bro.isExisting(docs.common.promoTooltipHide());
        if (popupExist) {
            await bro.click(docs.common.promoTooltipHide());
        }
        await bro.pause(200);
        await bro.yaAssertView(
            `${testpalmId}-1`,
            listing.common.listing.inner(),
            {
                ignoreElements: ['.MessageBox-Content', '.MessageBox-Backdrop'],
            }
        );
        await bro.yaSelectResources([testFile]);
        await bro.yaDeleteSelected();
        await bro.yaWaitNotificationForResource(
            testFile,
            consts.TEXT_NOTIFICATION_FILE_MOVED_TO_TRASH
        );
        await bro.yaAssertView(
            `${testpalmId}-2`,
            listing.common.listing.inner(),
            {
                ignoreElements: ['.MessageBox-Content', '.MessageBox-Backdrop'],
            }
        );
    });

    hermione.only.notIn('chrome-phone');
    hermione.auth.createAndLogin({
        language: 'ru',
        tus_consumer: 'disk-front-client',
    });
    it('diskclient-6754, Отображение заглушки после удаления единственного файла в сканах в доксах', async function () {
        const bro = this.browser;
        const testpalmId = 'diskclient-6754';
        await bro.yaSkipWelcomePopup();
        await bro.url(`${NAVIGATION.disk.url}?test-id=${SCANS_TEST_ID}`);
        const folderName = 'Сканы';
        await bro.yaCreateFolder(folderName);

        await bro.yaOpenListingElement(folderName);
        await bro.yaUploadFiles(testFile);
        await openDocsScans.call(this);
        const popupExist = await bro.isExisting(docs.common.promoTooltipHide());
        if (popupExist) {
            await bro.click(docs.common.promoTooltipHide());
        }
        await bro.yaAssertView(`${testpalmId}-1`, docs.common.docsPage.innerWrapper());
        bro.rightClick(docs.common.scansListingItem().replace(
            /:titleText/g,
            'test-file.jpg'
        ));
        await bro.pause(1000);
        bro.click(docs.common.scansDeleteResource());
        await bro.pause(1000);

        await bro.yaAssertView(`${testpalmId}-2`, docs.common.docsPage.innerWrapper(), {
            ignoreElements: ['.MessageBox-Content'],
        });
    });
});
