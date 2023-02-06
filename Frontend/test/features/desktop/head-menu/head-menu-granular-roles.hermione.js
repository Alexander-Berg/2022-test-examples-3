const PO = require('./PO');

describe('Верхнее меню', function() {
    describe('Положительные', function() {
        it('1. Внешний вид верхнего меню для пользователя с ограниченной ролью', async function() {
            // открыть ABC (/)
            await this.browser.openIntranetPage({
                pathname: '/',
            }, {
                user: 'robot-abc-003'
            });
            // в верхнем меню есть логотип "АВС", пункты "Все сервисы",
            // поле "Поиск сервисов" с кнопкой "Найти", иконки мессенджера и колокольчика, аватар
            await this.browser.waitForVisible(PO.header(), 5000);
            // нужно подождать, пока появится всё на странице, в том числе и скролл,
            // который меняет ширину верхнего меню
            await this.browser.waitForVisible(PO.services(), 15000);
            await this.browser.assertView('restricted-role-menu', PO.header());
        });
        it('2. Внешний вид верхнего меню для пользователя с сильно ограниченной ролью', async function() {
            // открыть ABC (/)
            await this.browser.openIntranetPage({
                pathname: '/',
            }, {
                user: 'robot-abc-004'
            });
            // в верхнем меню есть логотип "АВС", пункты "Все сервисы",
            // иконки мессенджера и колокольчика, аватар
            await this.browser.waitForVisible(PO.header(), 5000);
            // нужно подождать, пока появится всё на странице, в том числе и скролл,
            // который меняет ширину верхнего меню
            await this.browser.waitForVisible(PO.services(), 15000);
            await this.browser.assertView('heavy-restricted-role-menu', PO.header());
        });
    });
});
