const PO = require('./PO');

describe('Страница сервиса', function() {
    describe('Положительные', function() {
        it('1. Если в скоупе не более 30 участников, то не показываем ссылку "Ещё N"', async function() {
            // открыть страницу сервиса "autotest-different-scopes"
            await this.browser.openIntranetPage({
                pathname: '/services/autotest-different-scopes/',
            }, {
                user: 'robot-abc-001',
            });
            await this.browser.waitForVisible(PO.team.lastScope(), 20000);
            // снять скриншот со скоупом, где меньше 30 человек
            await this.browser.assertView('scope-less-30', PO.team.lastScope(), { ignoreElements: [PO.team.allUserPics()] });
        });
        it('2. Если в скоупе больше 30 участников, то показываем 30, остальных - по клику на "Ещё N"', async function() {
            // открыть страницу сервиса "autotest-different-scopes-2"
            await this.browser.openIntranetPage({
                pathname: '/services/autotest-different-scopes-2/',
            }, {
                user: 'robot-abc-001',
            });
            await this.browser.waitForVisible(PO.team.lastScope(), 20000);
            // снять скриншот со свёрнутым скоупом, где больше 30 человек
            await this.browser.assertView('scope-more-30', PO.team.lastScope(), { ignoreElements: [PO.team.allUserPics()] });
            // кликнуть на ссылку "Ещё 38"
            await this.browser.click(PO.team.lastScope.moreLink());
            // покрутился спиннер, остальные участники загрузились
            await this.browser.waitForVisible(PO.team.serviceTeamMoreSpinner(), 2000, true);
            // ссылка "Ещё 38" пропала
            await this.browser.waitForVisible(PO.team.lastScope.moreLink(), 2000, true);
            // показаны все 68 участников
            const allMembersInScope = await this.browser.$$(PO.team.lastScope.allMembers());
            assert(allMembersInScope.length === 68,
                `В скоупе должно быть 68 участников (а получили ${allMembersInScope.length})`);
            // навести курсор на правый край строки с последним в списке участником
            await this.browser.moveToObject(PO.team.lastScope.lastMember());
            // строка подсветилась жёлтым, с правого края появилась иконка корзины
            await this.browser.waitForVisible(PO.team.lastScope.selectedLastMember(), 2000);
            await this.browser.waitForVisible(PO.team.lastScope.lastMember.removeButton(), 1000);
        });
        it('3. Порядок отображения скоупов ролей в списке участников команды сервиса', async function() {
            // открыть страницу сервиса "autotest-all-role-scopes"
            await this.browser.openIntranetPage({ pathname: '/services/autotest-all-role-scopes/' }, { user: 'robot-abc-001' });
            await this.browser.waitForVisible(PO.team.lastScope(), 20000);
            // первым показан скоуп "Управление продуктом", где есть руководитель сервиса
            let firstScopeName = await this.browser.getText(PO.team.firstScope.scopeLink());
            assert(firstScopeName === 'Управление продуктом',
                `Первый скоуп - "${firstScopeName}" вместо "Управление продуктом`);
            // последним показан скоуп "Другие роли"
            let lastScopeName = await this.browser.getText(PO.team.lastScope.scopeLink());
            assert(lastScopeName === 'Другие роли',
                `Последний скоуп - "${lastScopeName}" вместо "Управление продуктом`);
        });
    });
});
