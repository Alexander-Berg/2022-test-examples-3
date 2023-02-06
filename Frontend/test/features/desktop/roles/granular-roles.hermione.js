const PO = require('./PO');

describe('Управление ролями', function() {
    describe('Положительные', function() {
        it('1. Пользователю с ограниченной ролью недоступно добавление новых членов команды', async function() {
            // открыть страницу сервиса "Автотестовый сервис"
            await this.browser.openIntranetPage({
                pathname: '/services/autotestservice2309/',
            }, {
                user: 'robot-abc-003'
            });
            // загрузился список с командой
            await this.browser.waitForVisible(PO.serviceTeamScopes(), 7000);
            // появился заголовок "Команда"
            await this.browser.waitForVisible(PO.team.header(), 5000);
            // рядом с заголовком "Команда" нет плюсика
            await this.browser.assertView('robot-abc-003-cant-add-members', PO.team.header());
        });
        it('2. Пользователю с сильно ограниченной ролью недоступно добавление новых членов команды', async function() {
            // открыть страницу сервиса "Автотестовый сервис"
            await this.browser.openIntranetPage({
                pathname: '/services/autotestservice2309/',
            }, {
                user: 'robot-abc-004'
            });
            // загрузился список с командой
            await this.browser.waitForVisible(PO.serviceTeamScopes(), 7000);
            // появился заголовок "Команда"
            await this.browser.waitForVisible(PO.team.header(), 5000);
            // рядом с заголовком "Команда" нет плюсика
            await this.browser.assertView('robot-abc-004-cant-add-members', PO.team.header());
        });
        it('3. Пользователю с ограниченной ролью недоступно удаление членов команды', async function() {
            // открыть страницу сервиса "Автотестовый сервис"
            await this.browser.openIntranetPage({
                pathname: '/services/autotestservice2309/',
            }, {
                user: 'robot-abc-003'
            });
            // загрузился список с командой
            await this.browser.waitForVisible(PO.serviceTeamScopes(), 7000);
            // нет иконок корзины около департаментов
            await this.browser.waitForExist(PO.deleteDepartmentButtons(), 100, true);
            // нет иконок корзины около пользователей
            await this.browser.moveToObject(PO.team.lastScope.lastMember());
            await this.browser.waitForExist(PO.team.lastScope.lastMember.removeButton(), 100, true);
            await this.browser.assertView('abc-003-cant-delete-users', PO.team.lastScope.lastMember());
        });
        it('4. Пользователю с сильно ограниченной ролью недоступно удаление членов команды', async function() {
            // открыть страницу сервиса "Автотестовый сервис"
            await this.browser.openIntranetPage({
                pathname: '/services/autotestservice2309/',
            }, {
                user: 'robot-abc-004'
            });
            // загрузился список с командой
            await this.browser.waitForVisible(PO.serviceTeamScopes(), 7000);
            // нет иконок корзины около департаментов
            await this.browser.waitForExist(PO.deleteDepartmentButtons(), 100, true);
            // нет иконок корзины около пользователей
            await this.browser.moveToObject(PO.team.lastScope.lastMember());
            await this.browser.waitForExist(PO.team.lastScope.lastMember.removeButton(), 100, true);
        });
    });
});
