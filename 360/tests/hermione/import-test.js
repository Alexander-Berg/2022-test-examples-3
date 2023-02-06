const adminPage = require('../../../hermione/pages/admin');
const { gotoOrganization } = require('./helpers/common');
const { assert } = require('chai');

hermione.only.in('chrome-desktop');

describe('Импорт', function () {
    it('diskforbusiness-509: Переход в саппорт из попапа о некорректном импорте почтовых ящиков', async function () {
        const bro = this.browser;
        await bro.yaLoginFast('yndx-sarah-test-018', 'gfhjkm13-gfhjkm13');

        await gotoOrganization(bro, true);

        await bro.url('/import');

        await bro.yaWaitForVisible(adminPage.importErrorShow);
        await bro.click(adminPage.importErrorShow);

        await bro.yaWaitForVisible(adminPage.importErrorPopup);

        await bro.click(
            adminPage.linkByText('Почему это произошло и как исправить')
        );
        let tabs = await bro.getTabIds();
        await bro.switchTab(tabs[1]);
        let currentUrl = await bro.getUrl();
        assert(
            currentUrl.startsWith('https://yandex.ru/support/business'),
            'Нет перехода в справку по первой ссылке'
        );
    });
    it('diskforbusiness-508: Переход в саппорт из подсказок в разделе Импорт', async function () {
        const bro = this.browser;
        await bro.yaLoginFast('yndx-sarah-test-018', 'gfhjkm13-gfhjkm13');

        await gotoOrganization(bro, true);

        await bro.url('/import');

        await bro.click(
            adminPage.importSupportLinkByText(
                'Как это сделать, читайте в',
                'Справке'
            )
        );
        let tabs = await bro.getTabIds();
        await bro.switchTab(tabs[1]);
        let currentUrl = await bro.getUrl();
        assert(
            currentUrl.startsWith('https://yandex.ru/support/business'),
            'Нет перехода в справку по первой ссылке'
        );

        await bro.switchTab(tabs[0]);

        await bro.click(
            adminPage.importSupportLinkByText(
                'как правильно подготовить CSV-файл и что делать, если вы не знаете пароли сотрудников',
                'Узнайте'
            )
        );
        tabs = await bro.getTabIds();
        await bro.switchTab(tabs[2]);
        currentUrl = await bro.getUrl();
        assert(
            currentUrl.startsWith('https://yandex.ru/support/business'),
            'Нет перехода в справку по второй ссылке'
        );
    });
    it('diskforbusiness-507: Отображение нотифайки "Импорт запущен"', async function () {
        const bro = this.browser;
        await bro.yaLoginFast('yndx-sarah-test-018', 'gfhjkm13-gfhjkm13');

        await gotoOrganization(bro, true);

        await bro.url('/import');

        await bro.click(adminPage.importYandexSourceCheck);
        await bro.setValue(adminPage.importEmailInput, 'test@yandex.ru');
        await bro.setValue(adminPage.importPasswordInput, 'password');
        await bro.setValue(adminPage.importLoginInput, 'login');
        await bro.click(adminPage.importSubmitButton);

        await bro.yaWaitForVisible(adminPage.importConfirmModal());
        await bro.click(adminPage.importConfirmModalSubmit);
        await bro.yaWaitForVisible(
            adminPage.notificationByText('Импорт запущен')
        );
        await bro.yaAssertView(
            'import-success-notification',
            adminPage.notificationByText('Импорт запущен')
        );
    });
});
