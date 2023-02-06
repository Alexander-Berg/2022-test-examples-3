const PO = require('./PO');

describe('Подтверждение ресурсов', function() {
    describe('Положительные', function() {
        it('1. Подтвердить выдачу запрошенного ресурса со страницы "Подтверждения"', async function() {
            // открыть страницу "Подтверждения" на табе "Ресурсы"
            await this.browser.openIntranetPage({
                pathname: '/approves/resources/',
            }, {
                user: 'robot-abc-001',
            });
            // покрутился и пропал спиннер
            await this.browser.waitForVisible(PO.spinner(), 10000, true);
            await this.browser.waitForVisible(PO.resourcesTable(), 20000);
            // в таблице есть запись о запросе ресурса,
            // где потребитель - "autotest-for-resources-requests"
            await this.browser.waitForVisible(PO.resourcesTable.firstRowInTable.testServiceRecord(), 1000);
            // фото строки с запросом на подтверждение ресурса
            await this.browser.assertView(
                'resource-request',
                PO.resourcesTable.firstRowInTable(),
                { ignoreElements: [PO.resourcesTable.firstRowInTable.time()] },
            );
            // кликнуть на многоточие в конце строки с запросом ресурса
            await this.browser.click(PO.resourcesTable.firstRowInTable.kebabMenu());
            // появился попап с двумя блоками - "Статус",
            // в котором есть пункты "Подтвердить", "Отклонить", "Редактировать" и "Посмотреть"
            await this.browser.waitForVisible(PO.resourcesActionsMenu(), 1000);
            await this.browser.waitForVisible(PO.resourcesActionsMenu.firstMenuGroup.approveButton(), 5000);
            await this.browser.assertPopupView(PO.resourcesActionsMenu(), 'actions-menu', PO.resourcesActionsMenu(), {
                allowViewportOverflow: true,
                captureElementFromTop: true,
                compositeImage: false,
            });
            // кликнуть на пункт "Подтвердить"
            await this.browser.click(PO.resourcesActionsMenu.firstMenuGroup.approveButton());
            // попап скрылся
            await this.browser.waitForVisible(PO.resourcesActionsMenu(), 2000, true);
            // список ресурсов на подтверждение обновился но проверить, что ресурс
            // пропал из списка, не получается, так как автотест считывает
            // сохранённый на первом открытии страницы дамп
            await this.browser.waitForVisible(PO.spinner(), 20000, true);
            // открыть страницу с ресурсами сервиса "autotest-for-resources-requests"
            // открываем с предустановленным фильтром, чтобы увидеть только выданные ресурсы
            await this.browser.openIntranetPage({
                pathname: '/services/autotest-for-resources-requests/resources/',
                query: {
                    supplier: 2604,
                    type: 164,
                    state: 'granted',
                    view: 'consuming',
                    layout: 'table',
                },
            }, {
                user: 'robot-abc-001',
            });
            // покрутился и пропал спиннер
            await this.browser.waitForVisible(PO.resourceSpinner(), 20000, true);
            // в таблице есть ресурс "loc:VLA-seg:default-cpu:0.918-mem:0-hdd:0-ssd:0-ip4:0-net:0-io_ssd:0-io_hdd:0"
            await this.browser.waitForVisible(PO.resourcesTable.testYPResource(), 5000);
        });
        it('2. Отклонить выдачу запрошенного ресурса со страницы "Подтверждения"', async function() {
            // открыть страницу "Подтверждения" на табе "Ресурсы"
            await this.browser.openIntranetPage({
                pathname: '/approves/resources/',
            }, {
                user: 'robot-abc-001',
            });
            // покрутился и пропал спиннер
            await this.browser.waitForVisible(PO.spinner(), 20000, true);
            await this.browser.waitForVisible(PO.resourcesTable(), 20000);
            // в таблице есть запись о запросе ресурса,
            // где потребитель - "autotest-for-resources-requests"
            await this.browser.waitForVisible(PO.resourcesTable.firstRowInTable.testServiceRecord(), 1000);
            // кликнуть на строку с запрошенным ресурсом
            await this.browser.click(PO.resourcesTable.firstRowInTable());
            // появилась модалка с просмотром информации о ресурсе
            await this.browser.waitForVisible(PO.modal.resourceView.spinner(), 2000);
            await this.browser.waitForVisible(PO.modal.resourceView.spinner(), 15000, true);
            await this.browser.waitForVisible(PO.modal.resourceView(), 15000);
            // фото модалки с информацией о ресурсе
            await this.browser.assertPopupView(PO.visibleModal(), 'resource-view-modal', PO.visibleModal());
            // кликнуть на кнопку "Отклонить" в модалке
            await this.browser.click(PO.modal.resourceView.buttons.rejectButton());
            // модалка закрылась
            await this.browser.waitForVisible(PO.modal.resourceView(), 20000, true);
            // список ресурсов на подтверждение обновился
            // но проверить, что ресурс пропал из списка, не получается, так как
            // автотест считывает сохранённый на первом открытии страницы дамп
            await this.browser.waitForVisible(PO.spinner(), 20000, true);
            // открыть страницу с ресурсами сервиса "autotest-for-resources-requests"
            await this.browser.openIntranetPage({
                pathname: '/services/autotest-for-resources-requests/resources/',
                query: {
                    supplier: 2604,
                    type: 164,
                    state: 'granted',
                    view: 'consuming',
                    layout: 'cards'
                },
            }, {
                user: 'robot-abc-001'
            });
            // покрутился и пропал спиннер
            await this.browser.waitForVisible(PO.resourceSpinner(), 20000, true);
            // в таблице нет ресурса "loc:VLA-seg:default-cpu:0.918-mem:0-hdd:0-ssd:0-ip4:0-net:0-io_ssd:0-io_hdd:0"
            await this.browser.waitForVisible(PO.resourcesTable.testYPResource(), 5000, true);
        });
        it('3. Открытие формы редактирования со страницы "Подтверждения"', async function() {
            // открыть страницу "Подтверждения" на табе "Ресурсы"
            await this.browser.openIntranetPage({
                pathname: '/approves/resources/',
            }, {
                user: 'robot-abc-001',
            });
            // покрутился и пропал спиннер
            await this.browser.waitForVisible(PO.spinner(), 10000, true);
            await this.browser.waitForVisible(PO.resourcesTable(), 20000);
            // в таблице есть запись о запросе ресурса,
            // где потребитель - "autotest-for-resources-requests"
            await this.browser.waitForVisible(PO.resourcesTable.firstRowInTable.testServiceRecord(), 1000);
            // кликнуть на многоточие в конце строки с запросом ресурса
            await this.browser.click(PO.resourcesTable.firstRowInTable.kebabMenu());
            // screenshot: появился попап с двумя блоками - "Статус"
            // в котором есть пункты "Подтвердить", "Отклонить", "Редактировать" и "Посмотреть"
            await this.browser.waitForVisible(PO.resourcesActionsMenu(), 1000);
            await this.browser.waitForVisible(PO.resourcesActionsMenu.secondMenuGroup.editButton(), 3000);
            // кликнуть на пункт "Редактировать"
            await this.browser.click(PO.resourcesActionsMenu.secondMenuGroup.editButton());
            // появилась форма для редактирования запроса ресурса
            await this.browser.waitForVisible(PO.modal.resourseEditForm.spinner(), 1000);
            await this.browser.waitForVisible(PO.modal.resourseEditForm.spinner(), 10000, true);
            // появилась форма для редактирования запроса ресурса
            await this.browser.waitForVisible(PO.modal.resourseEditForm(), 10000);
        });
        it('5. Открытие формы просмотра ресурса со страницы "Подтверждения"', async function() {
            // открыть страницу "Подтверждения" на табе "Ресурсы"
            await this.browser.openIntranetPage({
                pathname: '/approves/resources/',
            }, {
                user: 'robot-abc-001',
            });
            // таблица загрузилась
            await this.browser.waitForVisible(PO.resourcesTable(), 20000);
            // в таблице есть запись о запросе ресурса,
            // где потребитель - "autotest-for-resources-requests"
            await this.browser.waitForVisible(PO.resourcesTable.firstRowInTable.testServiceRecord(), 1000);
            // кликнуть на многоточие в конце строки с запросом ресурса
            await this.browser.click(PO.resourcesTable.firstRowInTable.kebabMenu());
            // появился попап с двумя блоками - "Статус"
            // в котором есть пункты "Подтвердить", "Отклонить", "Редактировать" и "Посмотреть"
            await this.browser.waitForVisible(PO.resourcesActionsMenu(), 1000);
            await this.browser.waitForVisible(PO.resourcesActionsMenu.secondMenuGroup.editButton(), 3000);
            await this.browser.waitForVisible(PO.resourcesActionsMenu.secondMenuGroup.watchButton(), 1000);
            // кликнуть на пункт "Посмотреть"
            await this.browser.click(PO.resourcesActionsMenu.secondMenuGroup.watchButton());
            // появилась модалка с просмотром информации о ресурсе
            await this.browser.waitForVisible(PO.modal.resourceView.spinner(), 15000, true);
            await this.browser.waitForVisible(PO.modal.resourceView(), 15000);
        });
    });
});
