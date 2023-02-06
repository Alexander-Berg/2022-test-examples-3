const clientContentListing = require('../page-objects/client-content-listing');
const clientPopups = require('../page-objects/client-popups');
const slider = require('../page-objects/slider').common;
const clientNavigation = require('../page-objects/client-navigation');

const { clientDesktopBrowsersList } = require('@ps-int/ufo-hermione/browsers');
const { consts } = require('../config');
const assert = require('chai').assert;
const { retriable } = require('@ps-int/ufo-hermione/helpers/retries');

describe('История -> ', () => {
    describe('Действия с ресурсами', () => {
        it('diskclient-1621, 1620: Переход к ресурсу по клику на ссылку в разделе История', async function() {
            const bro = this.browser;
            this.testpalmId = await bro.yaIsMobile() ? 'diskclient-1621' : 'diskclient-1620';

            await bro.yaClientLoginFast('yndx-ufo-test-15');
            await bro.url(consts.NAVIGATION.journal.url);
            await bro.yaWaitForVisible(clientContentListing.common.journalListing(), 20000);
            await bro.waitForVisible(clientContentListing.common.journalListing.fileLink(), 1000);
            await bro.click(clientContentListing.common.journalListing.fileLink());
            await bro.yaWaitActionBarDisplayed();

            await bro.yaWaitForUrlPath('/client/disk');
        });

        it('diskclient-1622, 684: Переход к ресурсу по клику на превью в разделе История', async function() {
            const bro = this.browser;
            const isMobile = await bro.yaIsMobile();
            this.testpalmId = isMobile ? 'diskclient-1622' : 'diskclient-684';

            await bro.getReady(
                'yndx-ufo-test-15',
                consts.NAVIGATION.journal.url,
                clientContentListing.common.journalListing()
            );

            const link = await bro.$(clientContentListing.common.journalListing.previewImageLink());

            await link.waitForExist({ timeout: 5000 });
            await link.waitForDisplayed();
            await link.click();

            const image = await bro.$(slider.contentSlider.previewImage());

            await image.waitForExist({ timeout: 5000 });
            await image.waitForDisplayed();

            const closeButton = await bro.$(slider.sliderButtons.closeButton());

            await closeButton.click();
            await image.waitForDisplayed({ reverse: true });

            if (isMobile) {
                await bro.yaWaitActionBarHidden();
                await bro.waitForVisible(clientContentListing.common.listing());
            } else {
                await bro.yaWaitActionBarDisplayed();
            }
        });
    });

    describe('Поведение', () => {
        it('diskclient-3568: [История] Автовоспроизведение видео', async function() {
            const bro = this.browser;

            await bro.getReady(
                'yndx-ufo-test-43',
                consts.NAVIGATION.journal.url,
                clientContentListing.common.journalListing()
            );

            const link = await bro.$(clientContentListing.common.journalListing.previewVideoLink());

            await link.waitForExist({ timeout: 10000 });
            await link.click();
            await bro.yaAssertVideoIsPlaying();
        });

        it('diskclient-1104, 5305: История. Список изменений', async function() {
            const bro = this.browser;
            await bro.yaClientLoginFast('yndx-ufo-test-271');

            await bro.yaOpenSection('journal');
            await bro.yaWaitForVisible(clientContentListing.common.journalListing.group());

            const { length: firstPortionLength } = await bro.$$(clientContentListing.common.journalListing.group());
            await bro.yaScrollToEnd();
            await bro.yaWaitForHidden(clientContentListing.common.listingSpinner());
            const { length: secondPortionLength } = await bro.$$(clientContentListing.common.journalListing.group());

            assert.equal(firstPortionLength, 40);
            assert.equal(secondPortionLength, 76);
        });

        hermione.auth.tus({ tus_consumer: 'disk-front-client', tags: ['diskclient-685', 'diskclient-5732'] });
        hermione.skip.notIn('', 'Мигающий тест – https://st.yandex-team.ru/CHEMODAN-71539');
        it('diskclient-685, 5732: Логгирование в разделе История', async function() {
            const bro = this.browser;
            await bro.yaWaitForHidden(clientContentListing.common.listingSpinner());

            const [testFileName] = await bro.yaGetListingElementsTitles();
            const newTestFileName = `dont-touch-me-${Date.now()}.jpg`;
            const expectedText = `Вы переименовали фотографию «${testFileName}» в «${newTestFileName}»`;

            await bro.yaSelectResource(testFileName);
            await bro.yaRenameSelected(newTestFileName);

            await bro.yaAssertProgressBarAppeared();
            await bro.yaAssertProgressBarDisappeared();

            await bro.yaOpenSection('journal');

            await retriable(async() => {
                await bro.refresh();
                await bro.yaAssertSectionOpened('journal');

                const [lastEventText] = await bro.getText(clientContentListing.common.journalListing.group.container());
                assert.equal(lastEventText, expectedText);
            }, 10, 4000);
        });

        it('diskclient-5734, 5735: Отображение действий об отмене подписки в истории ', async function() {
            const bro = this.browser;
            const expectedEventTexts = [
                'Вы отключили автоматический ежемесячный платёж на сумму 240 рублей',
                'Вы отключили автоматический ежегодный платёж на сумму 2000 рублей'
            ];

            await bro.yaClientLoginFast('yndx-ufo-test-428');
            await bro.url(consts.NAVIGATION.journal.url + '?startEventTimestamp=1582318799999');
            await bro.yaWaitForVisible(clientContentListing.common.journalListing.previewImageLink());

            const groups = await bro.$$(clientContentListing.common.journalListing.group.container());
            const actualEventTexts = await Promise.all(groups.map((group) => group.getText()));

            for (const expectedEventText of expectedEventTexts) {
                assert.include(actualEventTexts, expectedEventText);
            }
        });
    });

    describe('Отображение', () => {
        hermione.skip.notIn('', 'расскипать после 16 мая – https://st.yandex-team.ru/CHEMODAN-72131');
        it('diskclient-1628, 1504: Смоук: assertView: Отображение данных в разделе История', async function() {
            const bro = this.browser;
            this.testpalmId = await bro.yaIsMobile() ? 'diskclient-1628' : 'diskclient-1504';

            await bro.yaClientLoginFast('yndx-ufo-test-15');
            await bro.yaOpenSection('journal');

            await bro.yaAssertView(
                this.testpalmId,
                clientContentListing.common.journalListing(),
                {
                    ignoreElements: [
                        clientContentListing.common.journalListing.calendarDropdown(),
                        clientNavigation.desktop.infoSpaceButton()
                    ]
                }
            );
        });

        it('diskclient-4369, diskclient-4370: отображение действия другого пользователя.', async function() {
            const bro = this.browser;

            await bro.yaClientLoginFast('yndx-ufo-test-172');
            await bro.yaOpenSection('journal');
            await bro.yaWaitForVisible(clientContentListing.common.journalListing.group.container.user());

            const userNameText = await bro.getText(clientContentListing.common.journalListing.group.container.user());
            assert.notStrictEqual(userNameText, '', 'Имя пользователя пустое.');

            const group = await bro.$(clientContentListing.common.journalListing.group.container());
            const message = await group.getText();

            assert.equal(
                message,
                // eslint-disable-next-line max-len
                'yndx-ufo-test-173 загрузил фотографию «106662-priroda-dikaya_mestnost-ozero_luiza-gora-morennoe_ozero-1920x1080.jpg» в папку «Тестовая папка»',
                'Текст действия не соответсвует шаблону'
            );
        });
    });

    describe('Фильтры', () => {
        /**
         * @param {string} type
         * @returns {Promise<void>}
         */
        const filterTest = async function(type) {
            const bro = this.browser;

            await bro.yaClientLoginFast('yndx-ufo-test-375');
            await bro.yaOpenSection('journal');

            await bro.yaWaitForVisible(clientContentListing.desktop.journalFilter());
            await bro.click(clientContentListing.desktop.journalFilter[`${type}Filter`]());
        };

        hermione.only.in(clientDesktopBrowsersList);
        it('diskclient-682: Фильтр в разделе История. Фильтр по папкам', async function() {
            const bro = this.browser;
            this.testpalmId = 'diskclient-682';

            await filterTest.call(this, 'folders');

            await bro.yaSelectFolderInDialogAndApply('test-folder');
            await bro.yaWaitForVisible(clientContentListing.common.journalListing.previewImageLink());

            await bro.yaAssertView(
                this.testpalmId,
                clientContentListing.common.journalListing(),
                {
                    ignoreElements: [
                        clientContentListing.common.journalListing.calendarDropdown(),
                        clientNavigation.desktop.infoSpaceButton()
                    ]
                }
            );
        });

        hermione.only.in(clientDesktopBrowsersList);
        it('diskclient-5425: Фильтр в разделе История. Фильтр по дате', async function() {
            const bro = this.browser;
            let calendarTitle;
            this.testpalmId = 'diskclient-5425';

            await filterTest.call(this, 'calendar');
            await bro.yaWaitForVisible(clientPopups.desktop.calendar());

            calendarTitle = await bro.getText(clientPopups.desktop.calendar.title());
            while (calendarTitle !== 'Январь 2020') {
                await bro.click(clientPopups.desktop.calendar.prevMonthButton());
                calendarTitle = await bro.getText(clientPopups.desktop.calendar.title());
            }
            await bro.click(clientPopups.desktop.calendarDay().replace(':titleText', 27));
            await bro.yaWaitForHidden(clientPopups.desktop.calendar());
            await bro.yaWaitForVisible(clientContentListing.common.journalListing.previewImageLink());

            await bro.yaAssertView(
                this.testpalmId,
                clientContentListing.common.journalListing(),
                {
                    ignoreElements: [
                        clientContentListing.common.journalListing.calendarDropdown(),
                        clientNavigation.desktop.infoSpaceButton()
                    ]
                }
            );
        });

        hermione.only.in(clientDesktopBrowsersList);
        it('diskclient-5426: Фильтр в разделе История. Фильтр по событиям', async function() {
            const bro = this.browser;
            this.testpalmId = 'diskclient-5426';

            await filterTest.call(this, 'events');
            await bro.yaWaitForVisible(clientPopups.desktop.selectPopup());

            await bro.click(clientPopups.desktop.selectPopupItem().replace(':titleText', 'Перемещение'));
            await bro.yaWaitForHidden(clientPopups.desktop.selectPopupItem());
            await bro.yaWaitForVisible(clientContentListing.common.journalListing.previewImageLink());

            await bro.yaAssertView(
                this.testpalmId,
                clientContentListing.common.journalListing(),
                {
                    ignoreElements: [
                        clientContentListing.common.journalListing.calendarDropdown(),
                        clientNavigation.desktop.infoSpaceButton()
                    ]
                }
            );
        });

        hermione.only.in(clientDesktopBrowsersList);
        it('diskclient-5427: Фильтр в разделе История. Фильтр по устройствам', async function() {
            const bro = this.browser;
            this.testpalmId = 'diskclient-5427';

            await filterTest.call(this, 'platform');
            await bro.yaWaitForVisible(clientPopups.desktop.selectPopup());

            await bro.click(clientPopups.desktop.selectPopupItem().replace(':titleText', 'iPhone или iPad'));
            await bro.yaWaitForHidden(clientPopups.desktop.selectPopupItem());
            await bro.yaWaitForVisible(clientContentListing.common.journalListing.previewImageLink());

            await bro.yaAssertView(
                this.testpalmId,
                clientContentListing.common.journalListing(),
                {
                    ignoreElements: [
                        clientContentListing.common.journalListing.calendarDropdown(),
                        clientNavigation.desktop.infoSpaceButton()
                    ]
                }
            );
        });
    });
});
