const PO = require('./PO');

describe('Роли сотрудников', () => {
    describe('Удаление сотрудника', () => {
        describe('Положительные', () => {
            it('1. Удаление пользователя из команды со страницы сервиса', function() {
                // открыть страницут команды сервиса "Мой тестовый сервис 007" (/services/zanartestservice007)
                return this.browser.openIntranetPage({
                    pathname: '/services/zanartestservice007/',
                })
                    .waitForVisible(PO.serviceTeamScopes())

                    .moveToObject(PO.team.lastScope.lastMember())
                    .setHash('member-removed')
                    // кликнуть на иконку корзины на последней строке с пользователем
                    .click(PO.team.lastScope.lastMember.removeButton())

                    // Убираем курсор с иконки удаления и с блока удаляющегося участника
                    .moveToObject(PO.team.header())
                    .waitForVisible(PO.team.lastScope.lastMember.spin(), 10000, true)
                    // роль пользователя на последней строке отображается с пометкой "отзывается" [role-depriving]
                    .assertView('role-depriving', PO.team.lastScope.lastMember(), { ignoreElements: PO.team.lastScope.lastMember.userpic() });
            });
        });
    });
});
