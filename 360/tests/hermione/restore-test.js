const adminPage = require('../../../hermione/pages/admin');
const { navigateRestore, fillDomain } = require('./helpers/restore');

hermione.only.in('chrome-desktop');

describe('Восстановление доступа к организации', function () {
    it('diskforbusiness-546: Восстановление доступа не владельцем орг-ии', async function () {
        const bro = this.browser;
        await bro.yaLoginFast('yndx-sarah-test-019', 'gfhjkm13-gfhjkm13');

        await navigateRestore(bro);

        await bro.yaAssertView('new-owner', adminPage.restoreNewOwner());

        await fillDomain(bro);
        await bro.click(adminPage.restoreCheckDomainButton);
        await bro.yaWaitForVisible(adminPage.restoreCurrentOwner());
        await bro.yaAssertView(
            'current-owner',
            adminPage.restoreCurrentOwner()
        );

        await bro.setValue(
            adminPage.restoreAdminEmailsInput,
            'yndx-sarah-test-020-admin@restore.adm-testliza.ru, yndx-sarah-test-020@yandex.ru'
        );
        await bro.setValue(
            adminPage.restoreParticipantEmailsInput,
            'yndx-sarah-test-020-ivan@restore.adm-testliza.ru, yndx-sarah-test-020-ivan2@restore.adm-testliza.ru, yndx-sarah-test-020-ivan3@restore.adm-testliza.ru, yndx-sarah-test-020-ivan-4@restore.adm-testliza.ru'
        );
        await bro.setValue(
            adminPage.restoreDepartmentEmailsInput,
            'orgia@restore.adm-testliza.ru, gym@restore.adm-testliza.ru, cat@restore.adm-testliza.ru, dog@restore.adm-testliza.ru'
        );
        await bro.click(adminPage.restoreSubmitButton);

        await bro.yaWaitForVisible(adminPage.restoreConfirmState);
        await bro.yaAssertView('confirm-state', adminPage.restoreConfirmState);
    });
    it('diskforbusiness-547: Восстановление доступа владельцем орг-ии', async function () {
        const bro = this.browser;
        await bro.yaLoginFast('yndx-sarah-test-020', 'gfhjkm13-gfhjkm13');

        await navigateRestore(bro);

        await fillDomain(bro);
        await bro.setValue(
            adminPage.restoreAdminEmailsInput,
            'yndx-sarah-test-020-admin@restore.adm-testliza.ru, yndx-sarah-test-020@yandex.ru'
        );
        await bro.setValue(
            adminPage.restoreParticipantEmailsInput,
            'yndx-sarah-test-020-ivan@restore.adm-testliza.ru, yndx-sarah-test-020-ivan2@restore.adm-testliza.ru, yndx-sarah-test-020-ivan3@restore.adm-testliza.ru, yndx-sarah-test-020-ivan-4@restore.adm-testliza.ru'
        );
        await bro.setValue(
            adminPage.restoreDepartmentEmailsInput,
            'orgia@restore.adm-testliza.ru, gym@restore.adm-testliza.ru, cat@restore.adm-testliza.ru, dog@restore.adm-testliza.ru'
        );
        await bro.click(adminPage.restoreSubmitButton);

        const notification = adminPage.notificationByText(
            'Вы уже являетесь владельцем этой организации'
        );

        await bro.yaWaitForVisible(notification);
        await bro.yaAssertView('already-owner', notification);
    });
    it('diskforbusiness-548: Ввод некорректного домена при восстановлении доступа к орг-ии', async function () {
        const bro = this.browser;
        await bro.yaLoginFast('yndx-sarah-test-019', 'gfhjkm13-gfhjkm13');

        await navigateRestore(bro);

        await bro.setValue(adminPage.restoreDomainInput, 'not-exist-domain.ru');
        await bro.click(adminPage.restoreCheckDomainButton);

        const notification = adminPage.notificationByText(
            'Не удалось найти домен'
        );

        await bro.yaWaitForVisible(notification);

        await bro.setValue(
            adminPage.restoreAdminEmailsInput,
            'yndx-sarah-test-020-admin@restore.adm-testliza.ru, yndx-sarah-test-020@yandex.ru'
        );
        await bro.click(adminPage.restoreSubmitButton);

        await bro.yaWaitForVisible(notification);
        await bro.yaAssertView('domain-not-found', notification);
    });
    it('diskforbusiness-549: Ввод некорректной почты админа при восстановлении доступа к орг-ии', async function () {
        const bro = this.browser;
        await bro.yaLoginFast('yndx-sarah-test-018', 'gfhjkm13-gfhjkm13');

        await navigateRestore(bro);

        await fillDomain(bro);
        await bro.setValue(
            adminPage.restoreAdminEmailsInput,
            'this-email-does-not-exist@kuku.adm-testliza.ru'
        );
        await bro.setValue(
            adminPage.restoreParticipantEmailsInput,
            'yndx-sarah-test-020-ivan@restore.adm-testliza.ru, yndx-sarah-test-020-ivan2@restore.adm-testliza.ru, yndx-sarah-test-020-ivan3@restore.adm-testliza.ru, yndx-sarah-test-020-ivan-4@restore.adm-testliza.ru'
        );
        await bro.setValue(
            adminPage.restoreDepartmentEmailsInput,
            'orgia@restore.adm-testliza.ru, gym@restore.adm-testliza.ru, cat@restore.adm-testliza.ru, dog@restore.adm-testliza.ru'
        );
        await bro.click(adminPage.restoreSubmitButton);

        await bro.yaWaitForVisible(adminPage.restoreDeclineState);
        await bro.yaAssertView('confirm-state', adminPage.restoreDeclineState);
    });
    it('diskforbusiness-550: Ввод менее 4х сотрудников при восстановлении доступа к орг-ии', async function () {
        const bro = this.browser;
        await bro.yaLoginFast('yndx-sarah-test-018', 'gfhjkm13-gfhjkm13');

        await navigateRestore(bro);

        await fillDomain(bro);
        await bro.setValue(
            adminPage.restoreAdminEmailsInput,
            'yndx-sarah-test-020-admin@restore.adm-testliza.ru'
        );
        await bro.click(adminPage.restoreSubmitButton);

        await bro.yaWaitForVisible(adminPage.restoreDeclineState);
        await bro.yaAssertView('confirm-state', adminPage.restoreDeclineState);
    });
    it('diskforbusiness-551: Валидация пустых полей при восстановлении доступа к орг-ии', async function () {
        const bro = this.browser;
        await bro.yaLoginFast('yndx-sarah-test-019', 'gfhjkm13-gfhjkm13');

        await navigateRestore(bro);

        await bro.click(adminPage.restoreSubmitButton);

        await bro.yaAssertView(
            'domain-validation',
            adminPage.restoreDomainField
        );
        await bro.yaAssertView(
            'admins-validation',
            adminPage.restoreAdminEmailsField
        );
    });
});
