const PO = require('./PO');

describe('Секреты /', function() {
    describe('Страница ресурсов сервиса', function() {
        it('Удаление и пересоздание секрета', async function() {
            // открыть форму редактирования ресурса "TVM-приложение" в сервисе "autotest-contacts"
            await this.browser.openIntranetPage({
                pathname: '/services/autotest-contacts/resources/',
                query: { 'show-resource': 49107360 },
            }, { user: 'robot-abc-002' });
            // появляется содержимое модалки и поверх него крутится спиннер
            await this.browser.waitForVisible(PO.resourceModalAttributes(), 20000);
            // всё можно фоткать только тогда, когда перестали крутиться спиннеры
            await this.browser.waitForVisible(PO.resourceModal.resourceSpinner(), 25000, true);
            // фоткаем содержимое модалки - статус, кнопки
            await this.browser.assertView('modal-content', PO.resourceModal());
            // кликнуть на кнопку "Удалить старый секрет"
            await this.browser.setHash('delete-old-secret');
            await this.browser.click(PO.resourceModal.deleteOldSecretButton());
            // снова появляется и пропадает спиннер
            await this.browser.waitForVisible(PO.resourceModal.resourceSpinner(), 10000, true);
            // появилась надпись "пусто" (это происходит не сразу)
            await this.browser.waitForVisible(PO.resourceModal.emptyAttribute(), 10000);
            // значение напротив поля "old_client_secret" удалилось, вместо него есть надпись "(пусто)"
            // значение в поле "client_secret" осталось
            await this.browser.assertView('old-secret-deleted', PO.resourceModal());
            // кликнуть на кнопку "Пересоздать секрет"
            await this.browser.setHash('create-new-secret');
            await this.browser.click(PO.resourceModal.recreateSecretButton());
            // снова появляется и пропадает спиннер
            await this.browser.waitForVisible(PO.resourceModal.resourceSpinner(), 10000, true);
            // надпись "пусто" пропала
            await this.browser.waitForVisible(PO.resourceModal.emptyAttribute(), 10000, true);
            // значение, которое было в поле "client_secret", перенеслось в поле "old_client_secret"
            // в поле "client_secret" появилось новое значение;
            // значение в поле "version_uuid" также изменилось
            await this.browser.assertView('new-secret-created', PO.resourceModal());
        });
    });
});
