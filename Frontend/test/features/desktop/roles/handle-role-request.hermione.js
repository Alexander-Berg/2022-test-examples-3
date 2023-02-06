const PO = require('./PO');

describe('Обработка запросов ролей', function() {
    describe('Положительные', function() {
        it('1. Подтверждение роли', async function() {
            // перейти на страницу сервиса "Роли (автотесты)"
            await this.browser.openIntranetPage({
                pathname: '/services/podozrevaka27021/',
            }, { user: 'robot-abc-001' });
            // роль для пользователя robot-internal-003 находится в состоянии "ожидающие подтверждения"
            await this.browser.waitForVisible(PO.team.unapproved(), 7000);
            await this.browser.assertView('role-request', PO.team.unapproved());
            // нажать кнопку "подтвердить участие" (палец вверх)
            await this.browser.setHash('approve-role');
            await this.browser.click(PO.team.unapproved.approveButton());
            await this.browser.waitForVisible(PO.unapprovedBlockSpinner(), 7000, true);
            // роль находится в статусе "подтверждена"
            await this.browser.waitForVisible(PO.team.unapproved.approvedIcon(), 15000);
        });
        it('2. Отказ в выдаче роли', async function() {
            // перейти на страницу сервиса "Роли (автотесты)"
            await this.browser.openIntranetPage({
                pathname: '/services/podozrevaka27021/',
            }, { user: 'robot-abc-001' });
            // роль для пользователя robot-internal-003 находится в состоянии "ожидающие подтверждения"
            await this.browser.waitForVisible(PO.team.unapproved(), 7000);
            // нажать кнопку "отклонить участие" (палец вниз)
            await this.browser.setHash('decline-role');
            await this.browser.click(PO.team.unapproved.declineButton());
            // исчез раздел "ожидающие подтверждения"
            await this.browser.waitForVisible(PO.team.unapproved(), 7000, true);
        });
    });
});
