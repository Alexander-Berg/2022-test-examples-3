const { assert } = require('chai');
const { clientDesktopBrowsersList } = require('@ps-int/ufo-hermione/browsers');
const clientTuning = require('../page-objects/client-tuning-page');
const clientContentListing = require('../page-objects/client-content-listing');
const clientNavigation = require('../page-objects/client-navigation');
const clientPopups = require('../page-objects/client-popups');
const client = require('../page-objects/client');
const slider = require('../page-objects/slider');
const { consts } = require('../config');

/**
 * @typedef { import('@ps-int/ufo-hermione/types').Browser } Browser
 */

/**
 * @param {Browser} bro
 * @param {string} expectedText
 * @returns {Promise<void>}
 */
async function assertListingHeaderText(bro, expectedText) {
    const headerSelector = clientContentListing.common.listing.head.header();
    await bro.yaWaitForVisible(headerSelector);
    const sectionHeading = await bro.getText(headerSelector);
    assert.equal(sectionHeading, expectedText,
        `Текст заголовка не совпал с "${expectedText}"`);
}

describe('Навигация ->', () => {
    hermione.skip.notIn('', 'https://st.yandex-team.ru/CHEMODAN-83870');
    it('diskclient-4544, 5743: Редиректы оплаты', async function() {
        const bro = this.browser;
        await bro.yaClientLoginFast('yndx-ufo-test-732');

        const checkPayForm = async(expectedText) => {
            await bro.yaWaitForVisible(clientTuning.common.iframe());
            await bro.yaWaitForHidden(clientTuning.common.spin());
            const iframe = await bro.element(clientTuning.common.iframe());
            await bro.frame(iframe);
            await bro.yaWaitForVisible(clientTuning.common.cardFormAmount());
            const text = await bro.getText(clientTuning.common.cardFormAmount());
            await bro.frameParent();
            await assert.include(text[0], expectedText);
        };

        const pages = [
            { link: '/payment/10gb_1m' },
            { link: '/payment/10gb_1y' },
            { link: '/payment/100gb_1m', callback: () => checkPayForm('100 ГБ на месяц') },
            { link: '/payment/100gb_1y', callback: () => checkPayForm('100 ГБ на год') },
            { link: '/payment/1tb_1m', callback: () => checkPayForm('1 ТБ на месяц') },
            { link: '/payment/1tb_1y', callback: () => checkPayForm('1 ТБ на год') },
            { link: '/payment/' },
            { link: '/pay/' },
        ];
        for (const page of pages) {
            await bro.yaOpenUrlAndAssert(
                page.link,
                consts.NAVIGATION.tuning.url,
                page.callback
            );
        }
    });

    hermione.only.in(clientDesktopBrowsersList);
    it('diskclient-1361: Редирект промо оплаты', async function() {
        const bro = this.browser;
        await bro.yaOpenUrlAndAssert('https://disk.yandex.ru/tuning-soft',
            '/blog/disk/disk-pro-bolshe-mesta-i-vozmozhnostey');
        await bro.yaClientLoginFast('yndx-ufo-test-732');
        await bro.yaOpenUrlAndAssert('https://disk.yandex.ru/tuning-soft',
            consts.NAVIGATION.tuning.url);
    });

    hermione.only.in(clientDesktopBrowsersList);
    describe('Клики по ссылкам ->', () => {
        const tests = [
            { section: consts.NAVIGATION.disk, testName: 'diskclient-690: Клик по ссылке Файлы' },
            { section: consts.NAVIGATION.recent, testName: 'diskclient-689: Клик по ссылке Последние' },
            { section: consts.NAVIGATION.trash, testName: 'diskclient-695: Клик по ссылке Корзина' }
        ];

        tests.forEach(({ section, testName }) =>
            it(testName, async function() {
                const bro = this.browser;
                await bro.yaClientLoginFast('yndx-ufo-test-732');
                await bro.yaOpenSection(section.name.toLowerCase());
                await bro.yaAssertSectionOpened(section.name.toLowerCase());
            })
        );

        hermione.only.in(clientDesktopBrowsersList);
        it('diskclient-694: Клик по ссылке Архив', async function() {
            const bro = this.browser;
            const testFileName = 'file-100mb.txt';
            await bro.yaClientLoginFast('yndx-ufo-test-736');
            await bro.url(consts.NAVIGATION.disk.url);
            await bro.yaWaitForHidden(clientContentListing.common.listingSpinner());

            await bro.yaOpenSection('archive');

            await bro.yaAssertListingHas(testFileName);
        });
    });

    hermione.skip.notIn('', 'https://st.yandex-team.ru/CHEMODAN-83870');
    hermione.only.in(clientDesktopBrowsersList);
    it('diskclient-1264: Форма оплаты', async function() {
        const bro = this.browser;
        await bro.yaClientLoginFast('yndx-ufo-test-732', 'com');
        await bro.yaOpenUrlOnTld('com');
        await bro.yaWaitForVisible(clientNavigation.desktop.infoSpaceButton());
        const buttonText = await bro.getText(clientNavigation.desktop.infoSpaceButton());
        const price = buttonText.match(/\d+₽/g)[0];
        const size = buttonText.match(/\+\d+\s\W{2}/g)[0];

        await bro.click(clientNavigation.desktop.infoSpaceButton());
        await bro.yaSwitchTab();

        await bro.yaWaitForVisible(client.root());
        await bro.yaAssertUrlInclude(consts.NAVIGATION.tuning.url);

        await bro.yaWaitForVisible(
            clientTuning.common.tuningPage.tariffContainer.tariffWrapper.tariffSize()
        );

        const tarifsSizes = await bro.$$(clientTuning.common.tuningPage.tariffContainer.tariffWrapper.tariffSize());
        const buyLinks = await bro.$$(clientTuning.common.tuningPage.tariffContainer.tariffWrapper.buyLink());

        const amountLabels = await Promise.all(tarifsSizes.map((size) => size.getText()));
        const priceLinks = await Promise.all(buyLinks.map((link) => link.getText()));

        const tariffNum = amountLabels.findIndex((x) => x === size);
        const link = priceLinks[tariffNum];

        assert(link.replace(/ /g, '').search(price) !== -1, 'Цена на тариф не содержится в ссылке');

        await bro.click(clientTuning.common.tuningPage.tariffContainer
            .tariffWrapper.buyLink());
        await bro.yaWaitForVisible(clientTuning.common.iframe());
        await bro.yaWaitForHidden(clientTuning.common.spin());
        const iframe = await bro.element(clientTuning.common.iframe());
        await bro.frame(iframe);
        await bro.yaWaitForHidden(clientTuning.common.spin());
        await bro.yaWaitForVisible(clientTuning.common.cardFormTariff());
        await bro.getText(clientTuning.common.cardFormTariff());
        await bro.frameParent();
    });

    describe('История ->', () => {
        const tests = [
            { linkText: 'test-file.jpg', testName: 'diskclient-683: Переход к ресурсу в разделе История',
                onlyIn: 'chrome-desktop' },
            { linkText: 'test-file.exe', testName: 'diskclient-5753, 3283: История. Переход к файлу' },
            { linkText: 'Тестовая папка',
                testName: 'diskclient-5751, 3280: История. Переход к пользовательской папке' },
        ];

        tests.forEach(({ linkText, testName, onlyIn }) => {
            if (onlyIn) {
                hermione.only.in(onlyIn);
            }
            it(testName, async function() {
                const bro = this.browser;
                await bro.yaClientLoginFast('yndx-ufo-test-735');
                await bro.yaOpenSection(consts.NAVIGATION.journal.name.toLowerCase());
                await bro.yaWaitForVisible(`a[title="${linkText}"]:not([href="/client/disk/${linkText}"])`);
                await bro.click(`a[title="${linkText}"]:not([href="/client/disk/${linkText}"])`);
                await bro.yaWaitForVisible(clientContentListing.common.listing());
                await bro.yaWaitActionBarDisplayed();
                assert(await bro.yaIsResourceSelected(linkText), `Ресурс ${linkText} не выбран после клика`);
            });
        });

        it('diskclient-5750, 3282: История. Переход к видео', async function() {
            const bro = this.browser;
            await bro.yaClientLoginFast('yndx-ufo-test-735');
            const previewTitle = 'Хлебные крошки.mp4';
            await bro.yaOpenSection(consts.NAVIGATION.journal.name.toLowerCase());
            await bro.yaWaitForVisible(`span[title*="${previewTitle}"]`);
            await bro.click(`span[title*="${previewTitle}"]`);
            await bro.yaWaitForVisible(slider.common.contentSlider());
            await bro.yaWaitForHidden(slider.common.contentSlider.activeItem.spin());
            await bro.yaAssertVideoIsPlaying();
            await bro.keys('Escape');
            await bro.yaWaitForHidden(slider.common.contentSlider());
            await bro.yaAssertListingHas(previewTitle);
        });

        it('diskclient-5752, 1105: История. Переход к Корзине', async function() {
            const bro = this.browser;
            await bro.yaClientLoginFast('yndx-ufo-test-735');
            const section = consts.NAVIGATION.trash;
            await bro.yaOpenSection(consts.NAVIGATION.journal.name.toLowerCase());
            await bro.yaWaitForVisible(`a[href="${section.url}"]`);
            await bro.click(`a[href="${section.url}"]`);

            await bro.yaAssertSectionOpened(section.name.toLowerCase());
        });
    });

    hermione.only.in(clientDesktopBrowsersList);
    describe('Общий доступ ->', () => {
        const tests = [
            {
                testName: 'diskclient-2059: [Последние] Переход к файлу в общей папке',
                testResourceName: 'test-file.pdf',
                sectionWithTestFile: 'Тестовая папка',
                user: 'yndx-ufo-test-736',
                sectionUrl: consts.NAVIGATION.recent.url
            },
            {
                testName: 'diskclient-674: Переход к файлу (топбар)',
                testResourceName: 'test-file.jpg',
                sectionWithTestFile: 'diskclient-674',
                user: 'yndx-ufo-test-736',
                sectionUrl: consts.NAVIGATION.recent.url
            },
            {
                testName: 'diskclient-2085: [Общий доступ, Ссылки] Перейти к файлу',
                testResourceName: 'test-file.pdf',
                sectionWithTestFile: 'Тестовая папка',
                user: 'yndx-ufo-test-735'
            },
            {
                testName: 'diskclient-2084: [Общий доступ, Ссылки] Перейти к папке',
                testResourceName: 'Тестовая папка',
                sectionWithTestFile: consts.NAVIGATION.disk.contentTitle,
                user: 'yndx-ufo-test-736',
                isFolder: true
            },
            {
                testName: 'diskclient-2083: [Общий доступ, Общие папки] Перейти к папке',
                testResourceName: 'shared',
                sectionWithTestFile: consts.NAVIGATION.disk.contentTitle,
                sectionUrl: consts.NAVIGATION.shared.url,
                user: 'yndx-ufo-test-736',
                isFolder: true
            }
        ];

        tests.forEach(({ testName, testResourceName, sectionWithTestFile, user, isFolder, sectionUrl }) => {
            it(testName, async function() {
                const bro = this.browser;

                await bro.yaClientLoginFast(user);
                await bro.url(sectionUrl ? sectionUrl : consts.NAVIGATION.published.url);
                await bro.yaWaitForHidden(clientContentListing.common.listingSpinner());
                await bro.yaSelectResource(testResourceName);

                await bro.yaWaitForVisible(clientPopups.common.actionBar.moreButton());
                await bro.click(clientPopups.common.actionBar.moreButton());
                const buttonGoTo = isFolder ? clientPopups.common.actionBarMorePopup.goToFolder() :
                    clientPopups.common.actionBarMorePopup.goToFile();
                await bro.yaWaitForVisible(buttonGoTo);
                await bro.click(buttonGoTo);
                await bro.yaWaitForHidden(buttonGoTo);

                await assertListingHeaderText(bro, sectionWithTestFile);
                await bro.pause(200);
                assert(await bro.yaIsResourceSelected(testResourceName), `Ресурс ${testResourceName} не выделен`);
                await bro.yaWaitActionBarDisplayed();
            });
        });
    });

    hermione.only.in(clientDesktopBrowsersList);
    it('diskclient-709: Открыть папку из поисковой выдачи', async function() {
        const bro = this.browser;
        const testFolder = 'diskclient-709';
        await bro.yaClientLoginFast('yndx-ufo-test-736');
        await bro.yaWaitForVisible(client.psHeader.suggest.input());
        await bro.yaSetValue(client.psHeader.suggest.input(), testFolder);
        await bro.yaWaitForVisible(client.psHeader.suggest.items.item() + `[title=${testFolder}]`);
        await bro.click(client.psHeader.suggest.items.item() + `[title=${testFolder}]`);
        await bro.yaWaitForHidden(client.psHeader.suggest.items());

        await assertListingHeaderText(bro, testFolder);
    });

    it('diskclient-692, 1070: Панель навигации. Общий доступ', async function() {
        const bro = this.browser;
        await bro.yaClientLoginFast('yndx-ufo-test-736');
        await bro.yaOpenSection('shared');
        await bro.yaWaitForHidden(clientContentListing.common.listingSpinner());
        await bro.click(clientContentListing.common.listing.head() + ` a[href="${consts.NAVIGATION.published.url}"]`);
        await bro.yaWaitForHidden(clientContentListing.common.listingSpinner());
        await bro.yaAssertUrlInclude(consts.NAVIGATION.published.url);
        await assertListingHeaderText(bro, consts.NAVIGATION.published.contentTitle);
    });

    hermione.skip.in('chrome-phone', 'https://st.yandex-team.ru/CHEMODAN-84269'); // мигает
    it('diskclient-2058, 1207: Последние файлы. Переход к файлу', async function() {
        const bro = this.browser;
        const testFolderName = 'test folder';
        const testFiles = ['test-file.jpg', 'z-test-image1-diskclient1207.jpg'];
        await bro.yaClientLoginFast('yndx-ufo-test-737');

        for (const testFile of testFiles) {
            await bro.yaOpenSection('recent');
            await bro.yaWaitForHidden(clientContentListing.common.listingSpinner());
            await bro.yaSelectResource(testFile);
            await bro.yaWaitForVisible(clientPopups.common.actionBar.moreButton());
            await bro.click(clientPopups.common.actionBar.moreButton());
            const buttonGoTo = clientPopups.common.actionBarMorePopup.goToFile();
            await bro.yaWaitForVisible(buttonGoTo);
            await bro.click(buttonGoTo);
            await bro.yaWaitForHidden(buttonGoTo);
            await bro.yaWaitForHidden(clientContentListing.common.listingSpinner());
            await assertListingHeaderText(bro, testFolderName);
            // для завершения прокрутки
            await bro.yaWaitForHidden(clientContentListing.common.listingSpinner());
            assert(await bro.yaIsResourceSelected(testFile), `Ресурс ${testFile} не выделен`);
            await bro.yaWaitActionBarDisplayed();
        }
    });
});
