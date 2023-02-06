const assert = require('assert');
const PO = require('./PO');

const isUserInList = (selector, id) => {
    return function() {
        return this.execute((selector, id) => {
            const allPersonsElems = document // eslint-disable-line no-undef
                .querySelector(selector).querySelectorAll('.person__username');

            return [...allPersonsElems].some(el => el.dataset.bem.includes(id));
        }, selector, id);
    };
};

describe('Изменение управляющих сервисами', function() {
    describe('Положительные', () => {
        it('1. Добавление управляющего', function() {
            return this.browser
                .openIntranetPage({ pathname: '/services/automanager/' })
                .disableAnimations('*')
                .waitForVisible(PO.serviceManagers.editButton(), 5000)
                .click(PO.serviceManagers.editButton())
                .moveToObject(PO.serviceManagersModal(), 0, 0) // предотвращаем случайный ховер из-за положения модалки
                .assertView('service-managers-popup', PO.serviceManagersModal.content(), {
                    ignoreElements: '.service-responsible__modal .person__userpic',
                })
                .setSuggestVal(PO.serviceManagersEditor.addForm.input.control(), 'user3371')
                .waitForVisible(PO.inputPopup.items.first())
                .click(PO.inputPopup.items.first())
                .click(PO.serviceManagersEditor.submitButton())
                // оно может работать реально долго
                .waitForVisible(PO.serviceManagersModal(), 15000, true)
                .then(isUserInList(PO.serviceManagers.users(), 'user3371'))
                .then(({ value }) => {
                    assert.ok(value, 'Пользователь не добавился');
                });
        });

        it('2. Удаление управляющего', function() {
            return this.browser
                .openIntranetPage({ pathname: '/services/automanager/' })
                .waitForVisible(PO.serviceManagers.editButton(), 5000)
                .click(PO.serviceManagers.editButton())
                .waitForVisible(PO.serviceManagersModal())
                .click(PO.serviceManagersEditor.users.itemFirst.removeLink())
                .click(PO.serviceManagersEditor.submitButton())
                .waitForVisible(PO.serviceManagersModal(), 5000, true)
                .then(isUserInList(PO.serviceManagers.users(), 'robot-internal-002'))
                .then(({ value }) => {
                    assert.ok(!value, 'Пользователь не удалился');
                });
        });
    });
});
