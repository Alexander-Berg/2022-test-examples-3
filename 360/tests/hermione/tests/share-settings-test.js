const { clientDesktopBrowsersList } = require('@ps-int/ufo-hermione/browsers');
const clientPopups = require('../page-objects/client-popups');
const { overdraftContent } = require('../page-objects/client');
const clientContentListing = require('../page-objects/client-content-listing');
const albums = require('../page-objects/client-albums-page');
const publicPageObjects = require('../page-objects/public');
const { consts: { NAVIGATION } } = require('../config');
const { assert } = require('chai');
const dateToString = require('@ps-int/ufo-helpers/lib/date/date-to-string');

const SHARE_SETTINGS_TEST_ID = '?test-id=585107';

const openShareDialog = async (bro, {
    fileName = 'Санкт-Петербург.jpg',
    shouldCloseOverdraft,
    url = NAVIGATION.disk.url
} = {}) => {
    await bro.url(url + SHARE_SETTINGS_TEST_ID);

    if (shouldCloseOverdraft) {
        await bro.click(overdraftContent.closeButton());
        await bro.yaWaitForHidden(overdraftContent());
    }

    await bro.yaSelectResource(fileName);
    await bro.yaClick(clientPopups.desktop.actionBar.publishButton());

    await bro.yaWaitForVisible(clientPopups.common.shareDialog());
    await bro.pause(200); // dialog appearance animation
};

hermione.only.in(clientDesktopBrowsersList, 'Ограниченный шаринг пока только на десктопах');
describe('Ограниченный шаринг', () => {
    it('diskclient-7255: Отображение заблокированных настроек для б2с бесплатного юзера', async function() {
        const bro = this.browser;
        this.testpalmId = 'diskclient-7255';

        await bro.yaClientLoginFast('yndx-ufo-test-779');

        await openShareDialog(bro);

        await bro.yaSetModalDisplay(clientPopups.common.shareDialog());
        await bro.yaAssertView(this.testpalmId, clientPopups.common.shareDialog.securitySettings());
    });

    it('diskclient-7256: Отображение заблокированных настроек для б2б бесплатного юзера', async function() {
        const bro = this.browser;
        this.testpalmId = 'diskclient-7256';

        await bro.yaClientLoginFast('b2b');

        await openShareDialog(bro);

        await bro.yaSetModalDisplay(clientPopups.common.shareDialog());
        await bro.yaAssertView(this.testpalmId, clientPopups.common.shareDialog.securitySettings());
    });

    it('diskclient-7257: Отображение тултипа заблокированных настроек для б2с бесплатного юзера', async function() {
        const bro = this.browser;
        this.testpalmId = 'diskclient-7257';

        await bro.yaClientLoginFast('yndx-ufo-test-779');

        await openShareDialog(bro);

        await bro.moveToObject(clientPopups.common.shareDialog.securitySettings.lock());
        await bro.yaWaitForVisible(clientPopups.desktop.shareDialogNoFeatureTooltip());

        const tooltipText = await bro.getText(clientPopups.desktop.shareDialogNoFeatureTooltip());
        const textShouldBe = 'Доступны для пользователей Яндекс 360 Премиум';
        assert.include(tooltipText, textShouldBe);

        await bro.yaWaitForVisible(clientPopups.desktop.shareDialogNoFeatureTooltip.button());
        await bro.yaOpenLinkInNewTab(clientPopups.desktop.shareDialogNoFeatureTooltip.button(), {
            assertUrlHas: 'https://360.yandex.ru/premium-plans?from=share_settings'
        });
    });

    it('diskclient-7258: Отображение тултипа заблокированных настроек для б2б бесплатного юзера', async function() {
        const bro = this.browser;
        this.testpalmId = 'diskclient-7258';

        await bro.yaClientLoginFast('b2b');

        await openShareDialog(bro);

        await bro.moveToObject(clientPopups.common.shareDialog.securitySettings.lock());
        await bro.yaWaitForVisible(clientPopups.desktop.shareDialogNoFeatureTooltip());

        const tooltipText = await bro.getText(clientPopups.desktop.shareDialogNoFeatureTooltip());
        const textShouldBe = 'Доступно только на Оптимальном и Расширенном тарифах Яндекс 360 для бизнеса';
        assert.include(tooltipText, textShouldBe);

        await bro.yaWaitForVisible(clientPopups.desktop.shareDialogNoFeatureTooltip.button());
        await bro.yaOpenLinkInNewTab(clientPopups.desktop.shareDialogNoFeatureTooltip.button(), {
            assertUrlHas: 'https://360.yandex.ru/business/?from=share_settings'
        });
    });

    it('diskclient-7261: Отображение тултипа при наведении на тумблер запрета скачивания', async function() {
        const bro = this.browser;
        this.testpalmId = 'diskclient-7261';

        await bro.yaClientLoginFast('yndx-ufo-test-780');
        await openShareDialog(bro);

        await bro.moveToObject(clientPopups.common.shareDialog.securitySettings.forbidDownloadTumbler());
        await bro.yaWaitForVisible(clientPopups.desktop.shareDialogSecuritySettingsTooltip());

        const tooltipText = await bro.getText(clientPopups.desktop.shareDialogSecuritySettingsTooltip());
        const textShouldBe = 'Кнопки «Скачать» и «Сохранить на Яндекс Диск» будут недоступны';
        assert.equal(tooltipText, textShouldBe);
    });

    it('diskclient-7262: Отображение тултипа при наведении на "бессрочно"', async function() {
        const bro = this.browser;
        this.testpalmId = 'diskclient-7262';

        await bro.yaClientLoginFast('yndx-ufo-test-780');
        await openShareDialog(bro);

        await bro.moveToObject(clientPopups.common.shareDialog.securitySettings.availableUntilDropdownButton());
        await bro.yaWaitForVisible(clientPopups.desktop.shareDialogSecuritySettingsTooltip());

        const tooltipText = await bro.getText(clientPopups.desktop.shareDialogSecuritySettingsTooltip());
        const textShouldBe = 'Когда срок действия закончится, ссылка удалится и файл будет недоступен пользователям';
        assert.equal(tooltipText, textShouldBe);
    });

    it('diskclient-7263, diskclient-7264: Открытие/закрытие списка ограничения срока действия', async function() {
        const bro = this.browser;

        await bro.yaClientLoginFast('yndx-ufo-test-780');
        await openShareDialog(bro);

        // diskclient-7263: Открытие
        await bro.click(clientPopups.common.shareDialog.securitySettings.availableUntilDropdownButton());

        await bro.yaWaitForVisible(clientPopups.common.shareDialogAvailableUnlim());
        await bro.yaWaitForVisible(clientPopups.common.shareDialogAvailableDay(), 50);
        await bro.yaWaitForVisible(clientPopups.common.shareDialogAvailableWeek(), 50);
        await bro.yaWaitForVisible(clientPopups.common.shareDialogAvailableMonth(), 50);
        await bro.yaWaitForVisible(clientPopups.common.shareDialogAvailableCustom(), 50);

        assert.equal(await bro.getText(clientPopups.common.shareDialogAvailableUnlim()), 'Бессрочно');
        assert.include(await bro.getText(clientPopups.common.shareDialogAvailableDay()), 'Сутки');
        assert.include(await bro.getText(clientPopups.common.shareDialogAvailableWeek()), 'Неделя');
        assert.include(await bro.getText(clientPopups.common.shareDialogAvailableMonth()), 'Месяц');
        assert.equal(await bro.getText(clientPopups.common.shareDialogAvailableCustom()), 'Выбрать дату и время');

        // diskclient-7264: Закрытие
        await bro.click(clientPopups.common.shareDialog.securitySettings.availableUntilDropdownButton());
        await bro.yaWaitForHidden(clientPopups.common.shareDialogAvailableUnlim());
    });

    it('diskclient-7273: Опубликование Альбома: отсутствие настроек шаринга', async function() {
        const bro = this.browser;
        this.testpalmId = 'diskclient-7273';

        await bro.yaClientLoginFast('yndx-ufo-test-779');

        await bro.url('/client/albums/62ceb3bff73e6f4e809b554a' + SHARE_SETTINGS_TEST_ID);
        await bro.click(albums.album2.publishButton());
        await bro.yaWaitForVisible(clientPopups.common.shareDialog());
        await bro.pause(200); // dialog appearance animation

        await bro.yaWaitForVisible(clientPopups.common.shareDialog.copyButton(), 50);
        await bro.yaWaitForVisible(clientPopups.common.shareDialog.accessAccordion(), 50);
        await bro.yaWaitForHidden(clientPopups.common.shareDialog.securitySettings(), 50);
    });

    it('diskclient-7274: Опубликование ОП с доступом только на редактирование', async function() {
        const bro = this.browser;
        await bro.yaClientLoginFast('yndx-ufo-test-780');

        await openShareDialog(bro, { fileName: 'Editable shared' });

        await bro.yaWaitForVisible(clientPopups.common.shareDialog.copyButton(), 50);
        await bro.yaWaitForVisible(clientPopups.common.shareDialog.accessAccordion(), 50);
        await bro.yaWaitForVisible(clientPopups.common.shareDialog.securitySettings(), 50);
    });

    it('diskclient-7276: Опубликование файла в ОП с доступом только на просмотр', async function() {
        const bro = this.browser;
        await bro.yaClientLoginFast('yndx-ufo-test-780');

        await openShareDialog(bro, { fileName: 'RO shared' });

        await bro.yaWaitForVisible(clientPopups.common.shareDialog.copyButton(), 50);
        await bro.yaWaitForVisible(clientPopups.common.shareDialog.accessAccordion(), 50);
        await bro.yaWaitForHidden(clientPopups.common.shareDialog.securitySettings(), 50);

        const warningText = await bro.getText(clientPopups.common.shareDialog.accessAccordion());
        const textShouldBe = 'Настройки ссылки недоступны\nФайлы в этой папке можно только просматривать';
        assert.equal(warningText, textShouldBe);
    });

    it('diskclient-7277: Опубликование файла в разделе "Ссылки"', async function() {
        const bro = this.browser;
        this.testpalmId = 'diskclient-7277';

        await bro.yaClientLoginFast('yndx-ufo-test-779');

        await openShareDialog(bro, {
            url: NAVIGATION.published.url
        });

        await bro.yaWaitForVisible(clientPopups.common.shareDialog.title(), 50);
        await bro.yaWaitForVisible(clientPopups.common.shareDialog.copyButton(), 50);
        await bro.yaWaitForHidden(clientPopups.common.shareDialog.accessAccordion(), 50);
        await bro.yaWaitForHidden(clientPopups.common.shareDialog.securitySettings(), 50);
    });

    it('diskclient-7278: Опубликование файла овердрафтником', async function() {
        const bro = this.browser;
        this.testpalmId = 'diskclient-7278';

        await bro.yaClientLoginFast('yndx-ufo-test-776');

        await openShareDialog(bro, {
            fileName: 'PublicTestFile.jpg',
            shouldCloseOverdraft: true
        });

        await bro.yaWaitForVisible(clientPopups.common.shareDialog.copyButton(), 50);
        await bro.yaWaitForVisible(clientPopups.common.shareDialog.accessAccordion(), 50);
        await bro.yaWaitForHidden(clientPopups.common.shareDialog.securitySettings(), 50);

        const warningText = await bro.getText(clientPopups.common.shareDialog.accessAccordion());
        const textShouldBe = 'Файл по ссылке недоступен\nФайлы занимают больше места, чем у вас есть';
        assert.equal(warningText, textShouldBe);
    });

    it('diskclient-7279: Опубликование файла из "Почтовые вложения"', async function() {
        const bro = this.browser;
        this.testpalmId = 'diskclient-7279';

        await bro.yaClientLoginFast('yndx-ufo-test-43');
        await bro.url(NAVIGATION.archive.url + SHARE_SETTINGS_TEST_ID);

        await bro.yaClick(clientContentListing.desktop.listingItemPublicLinkIcon());

        await bro.yaWaitForVisible(clientPopups.desktop.shareDialog());
        await bro.pause(200); // dialog appearance animation

        await bro.yaWaitForVisible(clientPopups.common.shareDialog.copyButton(), 50);
        await bro.yaWaitForVisible(clientPopups.common.shareDialog.accessAccordion(), 50);
        await bro.yaWaitForHidden(clientPopups.common.shareDialog.securitySettings(), 50);

        const warningText = await bro.getText(clientPopups.common.shareDialog.accessAccordion());
        const textShouldBe = 'Настройки ссылки недоступны\nФайлы в этой папке можно только просматривать';
        assert.equal(warningText, textShouldBe);
    });

    it('diskclient-7280: Опубликование аудиофайла: недоступность запрета скачивания', async function() {
        const bro = this.browser;
        await bro.yaClientLoginFast('yndx-ufo-test-780');

        await openShareDialog(bro, { fileName: 'sample.mp3' });

        const forbidDownloadTumblerSelector = clientPopups.common.shareDialog.securitySettings.forbidDownloadTumbler();
        const forbidDownloadTumbler = await bro.$(forbidDownloadTumblerSelector);

        assert.equal(await forbidDownloadTumbler.getAttribute('aria-disabled'), 'true');

        await bro.moveToObject(forbidDownloadTumbler);
        await bro.yaWaitForVisible(clientPopups.desktop.shareDialogSecuritySettingsTooltip());

        const tooltipText = await bro.getText(clientPopups.desktop.shareDialogSecuritySettingsTooltip());
        const textShouldBe = 'Нельзя включить запрет скачивания для аудиофайла';
        assert.equal(tooltipText, textShouldBe);
    });

    it('diskclient-7284: Опубликование папки с доступными настройками', async function() {
        const bro = this.browser;
        this.testpalmId = 'diskclient-7284';

        await bro.yaClientLoginFast('yndx-ufo-test-780');
        await openShareDialog(bro, { fileName: 'папка' });

        await bro.yaWaitForVisible(clientPopups.common.shareDialog.securitySettings(), 50);
    });
});

// Тесты с чисткой
hermione.only.in(clientDesktopBrowsersList, 'Ограниченный шаринг пока только на десктопах');
describe('Ограниченный шаринг', () => {
    afterEach(async function() {
        // если тест упал при открытом диалоге, то очистка не сработает,
        // т.к. кнопки будут закрыты диалогом; рефреш помогает
        await this.browser.refresh();
        const items = this.currentTest.ctx.items;
        if (items) {
            await this.browser.yaDeleteCompletely([].concat(items), { safe: true, fast: true });
        }
    });

    const months = ['янв', 'фев', 'мар', 'апр', 'мая', 'июн', 'июл', 'авг', 'сен', 'окт', 'ноя', 'дек'];

    const checkText = (currentText, plusDays = 0, mode) => {
        const expectedDate = new Date();
        if (plusDays) {
            expectedDate.setDate(expectedDate.getDate() + plusDays);
        }
        // пока проходит тест - текущая минута может поменяться
        // для стабильности проверим совпадение с текущей или предыдущей минутой
        const prevMinute = new Date(expectedDate);
        prevMinute.setMinutes(prevMinute.getMinutes() - 1);

        const isCurrentYear = expectedDate.getFullYear() === (new Date()).getFullYear();
        const generateExpectedText = (date) => 'До ' + dateToString(
            date,
            `D mmmm${isCurrentYear ? '' : ' YYYY'} HH:mm`,
            undefined,
            months
        );
        const expectedDateText = generateExpectedText(expectedDate);
        const prevMinuteText = generateExpectedText(prevMinute);
        const isOk = [expectedDateText, prevMinuteText].includes(currentText);
        return {
            isOk,
            errorMessage: isOk ?
                '' :
                `Для режима "${mode}" ожидалось "${expectedDateText}", а получилось "${currentText}"`
        };
    };

    it('diskclient-7266, diskclient-7267, diskclient-7268, diskclient-7269: Выбор даты (фиксированные варианты)', async function() {
        const bro = this.browser;

        await bro.yaClientLoginFast('yndx-ufo-test-781');

        const availableUntilButtonSelector =
            clientPopups.common.shareDialog.securitySettings.availableUntilDropdownButton();

        const selectors = {
            unlim: clientPopups.common.shareDialogAvailableUnlim(),
            day: clientPopups.common.shareDialogAvailableDay(),
            week: clientPopups.common.shareDialogAvailableWeek(),
            month: clientPopups.common.shareDialogAvailableMonth()
        };
        const rightSideSelectors = {
            day: clientPopups.common.shareDialogAvailableDay.right(),
            week: clientPopups.common.shareDialogAvailableWeek.right(),
            month: clientPopups.common.shareDialogAvailableMonth.right()
        };

        const checkForDays = async (selector, days, rightSideSelector) => {
            await bro.click(availableUntilButtonSelector);
            await bro.yaWaitForVisible(selector);

            const menuItemDateText = await bro.getText(rightSideSelector);
            const checkResult = checkText(menuItemDateText, days, await bro.getText(selector));
            assert(checkResult.isOk, checkResult.errorMessage);

            await bro.click(selector);
            await bro.yaWaitForHidden(selector);
            const currentAvailableUntilText = await bro.getText(availableUntilButtonSelector);
            assert.equal(currentAvailableUntilText, menuItemDateText);
        };

        const fileName = await bro.yaUploadFiles('test-file.txt', { uniq: true });
        this.currentTest.ctx.items = fileName;

        await openShareDialog(bro, { fileName });

        let currentAvailableUntilText = await bro.getText(availableUntilButtonSelector);
        assert.equal(currentAvailableUntilText, 'Бессрочно');

        await checkForDays(selectors.day, 1, rightSideSelectors.day);
        await checkForDays(selectors.week, 7, rightSideSelectors.week);
        await checkForDays(selectors.month, 30, rightSideSelectors.month);

        await bro.click(availableUntilButtonSelector);
        await bro.yaWaitForVisible(selectors.unlim);
        await bro.click(selectors.unlim);
        await bro.yaWaitForHidden(selectors.unlim);
        currentAvailableUntilText = await bro.getText(availableUntilButtonSelector);
        assert.equal(currentAvailableUntilText, 'Бессрочно');
    });

    it('diskclient-7265, diskclient-7270: Выбор даты / времени', async function() {
        const bro = this.browser;

        await bro.yaClientLoginFast('yndx-ufo-test-782');

        const availableUntilButtonSelector =
            clientPopups.common.shareDialog.securitySettings.availableUntilDropdownButton();
        const customDateAndTimeSelector = clientPopups.common.shareDialogAvailableCustom();

        const fileName = await bro.yaUploadFiles('test-file.txt', { uniq: true });
        this.currentTest.ctx.items = fileName;

        await openShareDialog(bro, { fileName });

        let currentAvailableUntilText = await bro.getText(availableUntilButtonSelector);
        assert.equal(currentAvailableUntilText, 'Бессрочно');

        await bro.click(availableUntilButtonSelector);
        await bro.yaWaitForVisible(customDateAndTimeSelector);

        await bro.click(customDateAndTimeSelector);
        await bro.yaWaitForHidden(customDateAndTimeSelector);
        await bro.yaWaitForVisible(clientPopups.common.shareDialog.securitySettings.dateTimePicker());

        await bro.click(clientPopups.common.shareDialog.securitySettings.dateTimePicker.calendar.nextMonth());
        await bro.click(clientPopups.common.shareDialogSecuritySettingsCalendarDate15());

        await bro.click(clientPopups.common.shareDialog.securitySettings.dateTimePicker.timeFieldHour());
        await bro.keys('1');
        await bro.keys('2');
        await bro.keys('0');
        await bro.keys('0');

        await bro.click(clientPopups.common.shareDialog.securitySettings.dateTimePicker.apply());
        await bro.yaWaitForHidden(clientPopups.common.shareDialog.securitySettings.dateTimePicker());

        const nextMonth = new Date();
        nextMonth.setMonth(nextMonth.getMonth() + 1);
        const nextMonthAndYear = months[nextMonth.getMonth()] +
            (nextMonth.getMonth() === 0 ? ' ' + nextMonth.getFullYear() : '');

        currentAvailableUntilText = await bro.getText(availableUntilButtonSelector);
        assert.equal(
            currentAvailableUntilText,
            `До 15 ${nextMonthAndYear} 12:00`
        );
    });

    it('diskclient-7271, diskclient-7272: Опубликование документа на "Редактирование" / "Просмотр" - взаимодействие с запретом скачивания', async function() {
        const bro = this.browser;
        await bro.yaClientLoginFast('yndx-ufo-test-783');

        const fileName = await bro.yaUploadFiles('test-file.docx', { uniq: true });
        this.currentTest.ctx.items = fileName;

        await openShareDialog(bro, { fileName });

        const forbidDownloadTumblerSelector = clientPopups.common.shareDialog.securitySettings.forbidDownloadTumbler();
        const forbidDownloadTumbler = await bro.$(forbidDownloadTumblerSelector);
        const forbidDownloadButton =
            await bro.$(clientPopups.common.shareDialog.securitySettings.forbidDownloadTumbler.button());

        assert.equal(await forbidDownloadTumbler.getAttribute('aria-disabled'), 'false');
        assert.equal(await forbidDownloadButton.getAttribute('aria-pressed'), null);

        await bro.click(forbidDownloadTumblerSelector);
        assert.equal(await forbidDownloadButton.getAttribute('aria-pressed'), 'true');

        // переключение на "Редактирование"
        await bro.click(clientPopups.common.shareDialog.radioBoxNotChecked());

        assert.equal(await forbidDownloadTumbler.getAttribute('aria-disabled'), 'true');
        assert.equal(await forbidDownloadButton.getAttribute('aria-pressed'), 'false');

        await bro.moveToObject(forbidDownloadTumbler);
        await bro.yaWaitForVisible(clientPopups.desktop.shareDialogSecuritySettingsTooltip());

        const tooltipText = await bro.getText(clientPopups.desktop.shareDialogSecuritySettingsTooltip());
        const textShouldBe = 'Нельзя включить запрет скачивания, когда выбран доступ на редактирование';
        assert.equal(tooltipText, textShouldBe);

        // обратно на "Просмотр"
        await bro.click(clientPopups.common.shareDialog.radioBoxNotChecked());

        assert.equal(await forbidDownloadTumbler.getAttribute('aria-disabled'), 'false');
        assert.equal(await forbidDownloadButton.getAttribute('aria-pressed'), 'false');
    });

    it('diskclient-7287: Нотификация при выборе времени в прошлом', async function() {
        const bro = this.browser;
        await bro.yaClientLoginFast('yndx-ufo-test-784');

        const availableUntilButtonSelector =
            clientPopups.common.shareDialog.securitySettings.availableUntilDropdownButton();
        const customDateAndTimeSelector = clientPopups.common.shareDialogAvailableCustom();

        const fileName = await bro.yaUploadFiles('test-file.txt', { uniq: true });
        this.currentTest.ctx.items = fileName;

        await openShareDialog(bro, { fileName });

        let currentAvailableUntilText = await bro.getText(availableUntilButtonSelector);
        assert.equal(currentAvailableUntilText, 'Бессрочно');

        await bro.click(availableUntilButtonSelector);
        await bro.yaWaitForVisible(customDateAndTimeSelector);

        await bro.click(customDateAndTimeSelector);
        await bro.yaWaitForHidden(customDateAndTimeSelector);
        await bro.yaWaitForVisible(clientPopups.common.shareDialog.securitySettings.dateTimePicker());

        await bro.click(clientPopups.common.shareDialog.securitySettings.dateTimePicker.timeFieldHour());
        await bro.keys('0');
        await bro.keys('0');
        await bro.keys('0');
        await bro.keys('0');

        await bro.click(clientPopups.common.shareDialog.securitySettings.dateTimePicker.apply());
        await bro.yaWaitForHidden(clientPopups.common.shareDialog.securitySettings.dateTimePicker());

        await bro.yaWaitNotificationWithText('Не удалось сохранить настройки ссылки', 500);

        currentAvailableUntilText = await bro.getText(availableUntilButtonSelector);
        assert.equal(currentAvailableUntilText, 'Бессрочно');
    });

    it('diskclient-7292: Включение запрета скачивания', async function() {
        const bro = this.browser;
        await bro.yaClientLoginFast('yndx-ufo-test-785');

        const fileName = await bro.yaUploadFiles('test-file.txt', { uniq: true });
        this.currentTest.ctx.items = fileName;

        await openShareDialog(bro, { fileName });

        await bro.click(clientPopups.common.shareDialog.securitySettings.forbidDownloadTumbler());

        await bro.click(clientPopups.common.shareDialog.copyButton());
        const publicUrl = await bro.yaGetClipboard();
        await bro.newWindow(publicUrl);

        await bro.yaWaitForVisible(publicPageObjects.publicMain());
        await bro.yaWaitForVisible(publicPageObjects.fileName());
        await bro.yaWaitForHidden(publicPageObjects.desktopToolbar.saveButton());
        await bro.yaWaitForHidden(publicPageObjects.desktopToolbar.downloadButton());

        await bro.close();
    });

    it('diskclient-7293: Сохранение настроек шаринга', async function() {
        const bro = this.browser;
        await bro.yaClientLoginFast('yndx-ufo-test-786');

        const fileName = await bro.yaUploadFiles('test-file.txt', { uniq: true });
        this.currentTest.ctx.items = fileName;

        await openShareDialog(bro, { fileName });

        await bro.click(clientPopups.common.shareDialog.securitySettings.forbidDownloadTumbler());

        const availableUntilButtonSelector =
            clientPopups.common.shareDialog.securitySettings.availableUntilDropdownButton();
        const daySelector = clientPopups.common.shareDialogAvailableDay();
        const daySelectorDate = clientPopups.common.shareDialogAvailableDay.right();

        await bro.click(availableUntilButtonSelector);
        await bro.yaWaitForVisible(daySelector);

        const menuItemDateText = await bro.getText(daySelectorDate);

        await bro.click(daySelector);
        await bro.yaWaitForHidden(daySelector);

        await bro.click(clientPopups.common.shareDialog.closeButton());
        await openShareDialog(bro, { fileName });

        const forbidDownloadButton =
            await bro.$(clientPopups.common.shareDialog.securitySettings.forbidDownloadTumbler.button());
        assert.equal(await forbidDownloadButton.getAttribute('aria-pressed'), 'true');

        const currentAvailableUntilText = await bro.getText(availableUntilButtonSelector);
        assert.equal(currentAvailableUntilText, menuItemDateText);
    });
});
