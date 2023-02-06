const PO = require('./PO');

describe('Обработка запросов ролей', function() {
    describe('Положительные', function() {
        it('1. Отклонение непосредственного запроса роли со страницы "Подтверждения"', async function() {
            // открыть страницу "Подтверждения" на табе "Роли"
            await this.browser.openIntranetPage({
                pathname: '/approves/roles/',
            }, {
                user: 'robot-abc-001',
            });
            // покрутился и пропал спиннер
            await this.browser.waitForVisible(PO.spinner(), 10000, true);
            await this.browser.waitForVisible(PO.approvesContent(), 10000);
            // по умолчанию выбран переключатель "Непосредственные"
            // в таблице есть запрос на подтверждение роли для сотрудника "abc-robot service-viewer"
            await this.browser.assertView('roles-request-for-decline', PO.approvesContent());
            await this.browser.setHash('decline-role');
            // в столбце "Действия" кликнуть на значок "палец вниз"
            await this.browser.click(PO.approvesTable.declineRoleButton());
            // покрутился спиннер, строка с запросом из таблицы пропала
            await this.browser.waitForVisible(PO.spinner(), 10000, true);
            await this.browser.waitForVisible(PO.emptyTable(), 1000);
        });
        it('2. Одобрение запроса роли с учётом иерархии со страницы "Подтверждения"', async function() {
            // открыть страницу "Подтверждения" на табе "Роли"
            await this.browser.openIntranetPage({
                pathname: '/approves/roles/',
            }, {
                user: 'robot-abc-001',
            });
            // покрутился и пропал спиннер
            await this.browser.waitForVisible(PO.spinner(), 10000, true);
            await this.browser.waitForVisible(PO.approvesContent(), 10000);
            // выбран переключатель "Непосредственные", запросов ролей нет
            await this.browser.assertView('direct-roles-request', PO.approvesContent());
            // кликнуть на переключатель "С учетом иерархии"
            await this.browser.click(PO.approvesContent.inheritedButton());
            // выбран переключатель "С учетом иерархии"
            await this.browser.waitForVisible(PO.approvesContent.selectedInheritedButton(), 5000);
            // в таблице есть запрос на подтверждение роли для сотрудника "abc-robot own-only-viewer"
            await this.browser.waitForVisible(PO.approvesTable.roleRequestForUser.forRobotAbc004(), 5000);
            await this.browser.assertView('roles-request-for-approve', PO.approvesContent());
            await this.browser.setHash('approve-role');
            // в столбце "Действия" кликнуть на значок "палец вверх"
            await this.browser.click(PO.approvesTable.approveRoleButton());
            // покрутился спиннер, строка с запросом из таблицы пропала
            await this.browser.waitForVisible(PO.spinner(), 10000, true);
            await this.browser.waitForVisible(PO.emptyTable(), 1000);
        });
    });
});
