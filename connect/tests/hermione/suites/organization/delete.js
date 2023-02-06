const { URL } = require('url');
const { assert } = require('chai');
const {
    testUsers: {
        removedOrgWithoutUsersAdmin,
        removedOrgWithUsersAdmin,
        removedOrgWithBillingInfo,
        removedOrgWithResourcesAdmin,
        removedOrgWithDomain,
    },
} = hermione.ctx;

function start(browser, user) {
    return browser

        // авторизоваться под учеткой в паспорте у которой есть организация
        // или зарегистрироваться и создать организацию
        // перейти на страницу "Профиль организации" {{CONNECT_HOST}}/portal/profile
        .login({ ...user, retpath: '/portal/profile' })
        .waitForVisible('.org-profile_busy', 3000, true)
        .disableAnimations('*')

        // Нажать на кнопку "Удалить организацию"
        .click('.org-profile__button_type_remove-org')
        // открылся попап "Удаление организации"
        .waitForVisible('.modal_visible_yes.remove-org-modal', 5000)
        .waitForVisible('.modal_visible_yes.remove-org-modal .modal__busy', 5000, true);
}

function removeFlowShort(browser, user) {
    let prevPath = '';

    return start(browser, user)
        .getUrl()
        .then(url => {
            prevPath = new URL(url).pathname;
        })

        // нажать на кнопку "Удалить"
        .click('.remove-org-modal .modal__button_type_confirm')

        // После удаления выполнился переход на '/'
        .pause(10000)
        .getUrl()
        .then(url => {
            assert.notEqual(new URL(url).pathname, prevPath);
        });
}

describe('Удаление организации', () => {
    it('1. Удаление организации без сотрудников', function() {
        let prevPath = '';

        /* alias: 1-without-users */
        return start(this.browser, removedOrgWithoutUsersAdmin)

            // появился попап "Удаление организации"
            // с крестиком и кнопками "Отмена", "Удалить" [modal]
            .assertView('modal', '.modal_visible_yes .modal__content')

            // нажать на крестик
            .click('.modal_visible_yes .modal__close-button')

            // попап закрылся
            .waitForVisible('.modal_visible_yes', 5000, true)

            // Нажать на кнопку "Удалить организацию"
            .click('.org-profile__button_type_remove-org')

            // открылся попап "Удаление организации"
            .waitForVisible('.modal_visible_yes.remove-org-modal', 5000)

            // нажать на кнопку "Отменить"
            .click('.remove-org-modal .modal__button_type_cancel')

            // попап закрылся
            .waitForVisible('.modal_visible_yes', 5000, true)

            // Нажать на кнопку "Удалить организацию"
            .click('.org-profile__button_type_remove-org')

            // открылся попап "Удаление организации"
            .waitForVisible('.modal_visible_yes.remove-org-modal', 5000)

            .getUrl()
            .then(url => {
                prevPath = new URL(url).pathname;
            })

            // нажать на кнопку "Удалить"
            .click('.remove-org-modal .modal__button_type_confirm')

            // После удаления выполнился переход на '/'
            .pause(10000)
            .getUrl()
            .then(url => {
                assert.notEqual(new URL(url).pathname, prevPath);
            });
    });

    it('2. Удаление организации с сотрудниками', function() {
        /* alias: 2-with-users */
        return removeFlowShort(this.browser, removedOrgWithUsersAdmin);
    });

    it('3. Удаление организации с биллинговой информацией', function() {
        /* alias: 3-with-contract */
        return removeFlowShort(this.browser, removedOrgWithBillingInfo);
    });

    it('4. Удаление организации с ресурсами', function() {
        /* alias: 4-with-resources */
        return removeFlowShort(this.browser, removedOrgWithResourcesAdmin);
    });

    it('5. Удаление организации без сотрудников и с подтвержденным доменом', function() {
        /* alias: 5-with-domain */
        return removeFlowShort(this.browser, removedOrgWithDomain);
    });
});
