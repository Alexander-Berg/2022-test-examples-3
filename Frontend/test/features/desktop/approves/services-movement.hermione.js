const PO = require('./PO');

describe('Подтверждение перемещения сервисов', function() {
    describe('Положительные', function() {
        it('1. Отклонение непосредственного запроса на перемещение сервиса со страницы "Подтверждения"', async function() {
            // открыть сервис "Автотестовый сервис для отклонения перемещений"
            await this.browser.openIntranetPage({
                pathname: '/services/autotestservicedeclinemov/',
            }, {
                user: 'robot-abc-001'
            });
            // страница загрузилась
            await this.browser.waitForVisible(PO.serviceTeamSpinner(), 10000, true);
            // на странице есть запрос на перемещение сервиса, от имени robot-internal-003
            await this.browser.waitForVisible(PO.serviceMoveRequest(), 5000);
            // раскрыть подробную информацию про запрос
            await this.browser.click(PO.serviceMoveRequest.openButton());
            await this.browser.waitForVisible(PO.serviceMoveRequest.requestMoreInfo(), 1000);
            // фото плашки с запросом на перемещение
            await this.browser.assertView('service-movement-request', PO.serviceMoveRequest());
            // открыть страницу "Подтверждения" на табе "Перемещения сервисов"
            await this.browser.openIntranetPage({
                pathname: '/approves/services-movement/',
            }, {
                user: 'robot-abc-001'
            });
            // покрутился и пропал спиннер
            await this.browser.waitForVisible(PO.spinner(), 10000, true);
            await this.browser.waitForVisible(PO.approvesTable(), 20000);
            // в таблице есть запрос от пользователя robot-internal-003
            // на перемещение сервиса "Автотестовый сервис для отклонения перемещений"
            await this.browser.waitForVisible(PO.approvesTable.outgoingRequestForDecliningTestService(), 1000);
            // по умолчанию выбран переключатель "Непосредственные"
            await this.browser.waitForVisible(PO.approvesContent.selectedDirectButton(), 1000);
            // фото выбранного переключателя "Непосредственные" и запроса в таблице
            await this.browser.assertView('approve-content', PO.approvesContent());
            // в столбце "Новый владелец подтвердил" кликнуть на значок "палец вниз"
            await this.browser.click(PO.approvesTable.decisionButtons.declineButton());
            // покрутился спиннер
            await this.browser.waitForVisible(PO.spinner(), 10000, true);
        });
        it('2. Одобрение запроса на перемещение сервиса с учётом иерархии со страницы "Подтверждения"', async function() {
            // открыть страницу "Подтверждения" на табе "Перемещения сервисов"
            await this.browser.openIntranetPage({
                pathname: '/approves/services-movement/',
            }, {
                user: 'robot-abc-001'
            });
            // покрутился и пропал спиннер
            await this.browser.waitForVisible(PO.spinner(), 10000, true);
            await this.browser.waitForVisible(PO.approvesTable(), 20000);
            // по умолчанию выбран переключатель "Непосредственные"
            await this.browser.waitForVisible(PO.approvesContent.selectedDirectButton(), 1000);
            // запросов на перемещение сервисов нет
            await this.browser.waitForVisible(PO.emptyTable(), 1000);
            // фото выбранного переключателя "Непосредственные" и надписи "Ничего не найдено" в таблице
            await this.browser.assertView('no-direct-requests', PO.approvesContent());
            // кликнуть на переключатель "С учетом иерархии"
            await this.browser.click(PO.approvesContent.inheritedButton());
            // покрутился спиннер
            await this.browser.waitForVisible(PO.spinner(), 10000);
            await this.browser.waitForVisible(PO.spinner(), 10000, true);
            // открылся таб "С учётом иерархии"
            await this.browser.waitForVisible(PO.approvesContent.selectedInheritedButton(), 5000);
            // в таблице есть запрос от пользователя robot-internal-003
            // на перемещение сервиса "Автотестовый сервис для подтверждения перемещений"
            await this.browser.waitForVisible(PO.approvesTable.outgoingRequestForAccseptingTestService(), 10000);
            // запрос на перемещение сервиса "Автотестовый сервис для подтверждения перемещений"
            await this.browser.assertView('inherited-approve-content', PO.approvesContent());
            // в столбце "Новый владелец подтвердил" кликнуть на значок "палец вверх"
            await this.browser.click(PO.approvesTable.decisionButtons.approveButton());
            // покрутился спиннер
            await this.browser.waitForVisible(PO.spinner(), 10000, true);
            // открыть сервис "Автотестовый сервис для подтверждения перемещений"
            await this.browser.openIntranetPage({
                pathname: '/services/autotestserviceforapprove/',
            }, {
                user: 'robot-abc-001'
            });
            // страница загрузилась
            await this.browser.waitForVisible(PO.serviceTeamSpinner(), 10000, true);
            // сервис начал перемещаться, рядом с его названием появилась плашка "RO Перемещается"
            await this.browser.waitForVisible(PO.ROLabel(), 10000);
            // плашка "RO Перемещается"
            await this.browser.assertView('ro-label', PO.ROLabel());
            // на странице есть плашка с запросом на перемещение сервиса
            await this.browser.waitForVisible(PO.serviceMoveRequest(), 15000);
        });
    });
});
