const { assert } = require('chai');
const { domainAdmin: user } = hermione.ctx.testUsers;

function noBlockButton(uid, browser) {
    return browser

        // открыть карточку сотрудника ${uid}
        .url(`/portal/admin/users/${uid}`)
        .waitForVisible('.user-section')
        .disableAnimations('*')

        // нажать на "..."
        .click('.user-controls__edit .ui-flat-button')
        .waitForVisible('.popup_visible .menu')

        // в меню нет пункта "Заблокировать пользователя"
        .assertView('menu', '.popup_visible .menu')
        .isExisting('.popup_visible .menu .menu-item[data-bem=\'{"menu-item":{"val":"block"}}\']')
        .then(assert.isFalse);
}

describe('Блокировка и разблокировка сотрудников', () => {
    beforeEach(function() {
        return this.browser
            .login({ ...user, retpath: '/portal/admin/departments/1' })
            .hideCaret()
            .waitForVisible('.department-section');
    });

    describe('Положительные', () => {
        it('1. Блокировка сотрудника', function() {
            /* alias: pos-1-block */
            return this.browser

                // открыть карточку сотрудника "Сотрудник Доменный"(test2t@test1.connect-test.tk)
                .click('.section-list-item .unit[title="Сотрудник Доменный"]')
                .waitForVisible('.user-section')

                // нажать на "..."
                .click('.user-controls__edit .ui-flat-button')
                .waitForVisible('.popup_visible .menu')

                // в меню выбрать "Заблокировать пользователя"
                .setHash('block-user')
                .click('.popup_visible .menu .menu-item[data-bem=\'{"menu-item":{"val":"block"}}\']')

                // меню закрылось
                .isExisting('.popup_visible .menu')
                .then(assert.isFalse)

                // карточка с красной плашкой "Пользователь заблокирован"
                .waitForVisible('.user-identity__block-badge')
                .assertView('user-identity-blocked', '.user-identity')

                // нажать на "..."
                .click('.user-controls__edit .ui-flat-button')
                .waitForVisible('.popup_visible .menu')

                // в меню появился пункт "Разблокировать пользователя"
                .isExisting('.popup_visible .menu .menu-item[data-bem=\'{"menu-item":{"val":"unblock"}}\']')
                .then(assert.isTrue)

                // в меню нет пункта "Заблокировать пользователя"
                .isExisting('.popup_visible .menu .menu-item[data-bem=\'{"menu-item":{"val":"block"}}\']')
                .then(assert.isFalse)

                // нажать на "Разблокировать пользователя"
                .setHash('unblock-user')
                .click('.popup_visible .menu .menu-item[data-bem=\'{"menu-item":{"val":"unblock"}}\']')

                // меню закрылось
                .isExisting('.popup_visible .menu')
                .then(assert.isFalse)

                // карточка без красной плашки "Пользователь заблокирован"
                .waitForVisible('.notification__success')
                .assertView('user-identity', '.user-identity');
        });
    });

    describe('Отрицательные', () => {
        it('1. Отсутствие блокировки админа владельца организации', function() {
            /* alias: neg-1-owner */
            return noBlockButton(1130000001930742, this.browser);
        });

        it('2. Отсутствие блокировки портального сотрудника', function() {
            /* alias: neg-2-portal-user */
            return noBlockButton(4026360850, this.browser);
        });

        it('3. Отсутствие блокировки самого себя', function() {
            /* alias: neg-3-self */
            return noBlockButton(1130000001930740, this.browser);
        });
    });
});
