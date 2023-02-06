const { NAVIGATION } = require('../config').consts;
const popups = require('../page-objects/client-popups');
const slider = require('../page-objects/slider');
const { desktop: desktopNav } = require('../page-objects/client-navigation');
const { assert } = require('chai');
const tuning = require('../page-objects/client-tuning-page');
const { clientDesktopBrowsersList } = require('@ps-int/ufo-hermione/browsers');
const clientContentListing = require('../page-objects/client-content-listing');

const FILE_NAME = 'MeowFile.jpg';
const FOLDER_NAME = 'MeowFolder';
const DELETED_FOLDER_NAME = 'DeletedFolder';
const TRASH_NAME = 'Корзина';
// eslint-disable-next-line max-len
const LONG_TEXT = 'Санкт-Петербург -- это чудесный город, поэтому файлу с ним можно посвятить большое и длинное название для тестирования диска.jpg';

const infoPopups = [
    {
        navigate: NAVIGATION.disk.url,
        resource: TRASH_NAME,
        desktopId: 1277,
        mobileId: 4889,
        description: 'Инфопопап корзины',
        isShared: false
    },
    {
        navigate: NAVIGATION.trash.url,
        resource: DELETED_FOLDER_NAME,
        desktopId: 1278,
        mobileId: 4890,
        description: 'Инфопопап папки из Корзины',
        isShared: false
    },
    {
        navigate: NAVIGATION.disk.url,
        resource: FOLDER_NAME,
        desktopId: 1088,
        mobileId: 934,
        description: 'Проверка инфопопапа для папки',
        isShared: true
    },
    {
        navigate: NAVIGATION.disk.url,
        resource: FILE_NAME,
        desktopId: 933,
        mobileId: 1011,
        description: 'Проверка инфопопапа для файла',
        isShared: true
    },
    {
        navigate: NAVIGATION.recent.url,
        resource: FILE_NAME,
        desktopId: 341,
        mobileId: 873,
        description: 'Проверка инфопопапа для файла в разделе последние',
        isShared: true
    }
];

const doTest = ({ navigate, resource, desktopId, mobileId, description, isShared }) => {
    describe('Инфопопапы -> ', () => {
        it(`diskclient-${desktopId} diskclient-${mobileId}: ${description}`, async function() {
            const bro = this.browser;
            await bro.yaClientLoginFast('yndx-ufo-test-159');
            await bro.url(navigate);
            await bro.yaSelectResource(resource);
            const isMobile = await bro.yaIsMobile();
            this.testpalmId = isMobile ? `diskclient-${mobileId}` : `diskclient-${desktopId}`;
            await bro.click(popups.common.actionBar.infoButton());
            await bro.pause(1000);
            if (isShared) {
                await bro.yaAssertView(this.testpalmId, popups.common.resourceInfoDropdownContent(), {
                    hideElements: [
                        popups.common.resourceInfoDropdownContent.viewCount(),
                        popups.common.resourceInfoDropdownContent.downloadCount()
                    ]
                });
                const views = await bro.getText(popups.common.resourceInfoDropdownContent.viewCount());
                assert.match(views, /^\d+$/, `${views} не число`);
                const download = await bro.getText(popups.common.resourceInfoDropdownContent.downloadCount());
                assert.match(download, /^\d+$/, `${download} не число`);
            } else {
                await bro.yaAssertView(this.testpalmId, popups.common.resourceInfoDropdownContent());
            }
            const isAndroid = await bro.yaIsAndroid();
            if (isAndroid) {
                await bro.orientation('landscape');
                await bro.pause(500);
                await bro.yaAssertView(`${this.testpalmId}-landscape`, popups.common.resourceInfoDropdownContent(), {
                    hideElements: [
                        popups.common.resourceInfoDropdownContent.viewCount(),
                        popups.common.resourceInfoDropdownContent.downloadCount()
                    ]
                });
            }
        });
    });
};

infoPopups.forEach(doTest);

describe('Инфопопапы -> ', () => {
    it('diskclient-1150 diskclient-1248 Инфопопап. Слайдер', async function() {
        const bro = this.browser;
        const isMobile = await bro.yaIsMobile();
        this.testpalmId = isMobile ? 'diskclient-1248' : 'diskclient-1150';

        await bro.yaClientLoginFast('yndx-ufo-test-159');
        await bro.url(NAVIGATION.disk.url);

        await bro.yaWaitForHidden(clientContentListing.common.listingSpinner());
        await bro.yaOpenListingElement(LONG_TEXT);

        await bro.yaWaitForVisible(slider.common.contentSlider());
        await bro.click(slider.common.sliderButtons.infoButton());
        await bro.pause(500);
        await bro.yaAssertView(this.testpalmId, popups.common.resourceInfoDropdownContent(), {
            hideElements: popups.common.tooltip() //тултип в десктопе может открыться над попапом
        });
        const isAndroid = await bro.yaIsAndroid();
        if (isAndroid) {
            await bro.orientation('landscape');
            await bro.pause(500);
            await bro.yaAssertView(`${this.testpalmId}-landscape`, popups.common.resourceInfoDropdownContent());
        }
    });
});

hermione.only.in(clientDesktopBrowsersList, 'Актуально только для десктопной версии');
describe('Инфопопапы -> ', () => {
    it('diskclient-5196: Промо попап о скидке', async function() {
        const bro = this.browser;
        await bro.yaClientLoginFast('yndx-ufo-test-242', 'com');
        await bro.yaOpenUrlOnTld('com');
        await bro.yaSetUserSettings('timestampLastClosedDiscountPromo');
        await bro.yaWaitForVisible(popups.desktop.discountPromoPopup(), 'Промо-попап о скидке не отобразился');
        await bro.assertView('diskclient-5196', [popups.desktop.discountPromoPopup(), desktopNav.infoSpaceButton()]);

        await bro.click(popups.desktop.discountPromoPopup.laterButton());
        await bro.yaWaitForHidden(
            popups.desktop.discountPromoPopup(),
            'Промо-попап не скрылся после нажатия на кнопку Позже'
        );
        await bro.refresh();
        await bro.yaWaitForHidden(
            popups.desktop.discountPromoPopup(),
            'Промо-попап отобразился после нажатия на кнопку Позже и перезагрузку страницы'
        );

        await bro.yaSetUserSettings('timestampLastClosedDiscountPromo');

        await bro.yaOpenLinkInNewTab(popups.desktop.discountPromoPopup.buyButton(), { assertUrlHas: 'tuning' });
        await bro.yaWaitForVisible(tuning.common.tuningPage());
        await bro.yaOpenUrlOnTld('com');
        await bro.yaWaitForHidden(
            popups.desktop.discountPromoPopup(),
            'Промо-попап отобразился после нажатия на кнопку Купить и перезагрузку страницы'
        );

        await bro.yaSetUserSettings('timestampLastClosedDiscountPromo');
    });
});

describe('Инфопопапы -> ', () => {
    hermione.skip.notIn('', 'Мигает https://st.yandex-team.ru/CHEMODAN-75029');
    it('diskclient-6039, diskclient-6163: Обновление информации о размере в инфопопапе папки', async function() {
        const bro = this.browser;
        const testData = {
            user: 'yndx-ufo-test-537',
            folderName: (await bro.yaGetUniqResourceName()),
            firstFile: 'Горы.jpg',
            secondFile: 'Москва.jpg',
            expSizeOneFile: '1,6 МБ',
            expSizeTwoFiles: '3 МБ'
        };

        await bro.yaClientLoginFast(testData.user);
        this.currentTest.ctx.listingResources = [testData.folderName];

        await bro.yaOpenCreateDirectoryDialog();
        await bro.yaSetResourceNameAndApply(testData.folderName);

        const copyAndAssertFolderSize = async(fileName, expectedSize, folderName = testData.folderName) => {
            await bro.yaSelectResource(fileName);
            await bro.yaCopySelected(folderName);
            await bro.yaAssertProgressBarDisappeared();

            await bro.yaSelectResource(folderName);
            await bro.click(popups.common.actionBar.infoButton());
            await bro.yaWaitForVisible(popups.common.resourceInfoDropdownContent.size());
            await bro.pause(1000);
            const actualSize = await bro.getText(popups.common.resourceInfoDropdownContent.size());
            assert.equal(actualSize, expectedSize);
        };

        await copyAndAssertFolderSize(testData.firstFile, testData.expSizeOneFile);
        await copyAndAssertFolderSize(testData.secondFile, testData.expSizeTwoFiles);
    });
});
