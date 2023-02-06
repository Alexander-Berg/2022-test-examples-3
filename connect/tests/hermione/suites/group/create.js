const { assert } = require('chai');
const { removeGroup } = require('../../helpers/directoryApi');
const { testUsers: { alex }, cacheMode } = hermione.ctx;
const newGroup = {
    name: 'Новая команда',
    description: 'Описание Новой команды',
    label: 'newteam',
};

describe('Создание команды', () => {
    beforeEach(function() {
        return this.browser
            // зайти в раздел "Команда" как Администратор
            .login({ ...alex, retpath: 'portal/admin/groups' })
            .waitForVisible('.sections', 3000)
            .waitForVisible('.loader_visible', 3000, true);
    });

    afterEach(function() {
        // В режиме для чтения ничего удалять не надо
        if (cacheMode === 'read') {
            return;
        }

        this.browser.getMeta('newGroup').then(id => {
            if (!id) {
                return;
            }

            return removeGroup(id, alex);
        });
    });

    it('Создается и отображается в списке команд, если заполнены обязательные поля', function() {
        /* alias: 1-create */
        return this.browser
            .disableAnimations('*')
            .hideCaret()

            // отображается раздел где можно создавать команды [plain]
            .assertView('plain', '.sections')

            // список команд пустой, отображается ссылка 'Создайте команду'
            .getText('.group-list .link')
            .then(text => {
                assert.equal(text, 'Создайте команду');
            })

            // навести мышь на кнопку Добавить
            .moveToObject('.add-section-item-control')

            // плюс горит желтым [add-button-hovered]
            .assertView('add-button-hovered', '.add-section-item-control .plus-button')

            // нажать на кнопку Добавить
            .click('.add-section-item-control')

            // отображается модал с формой добавления Новая команда [form-shown]
            .waitForVisible('.modal_visible .create-group-form')
            .assertView('form-shown', '.modal_visible .modal__content')

            // заполнить все необходимые поля
            .setValue('.create-group-form .input__control[name="name[ru]"]', newGroup.name)
            .setValue('.create-group-form .textarea[name="description[ru]"]', newGroup.description)
            .setValue('.create-group-form .input__control[name="label"]', newGroup.label)

            .setHash('createGroup')

            // отображается форма с заполненными полями [form-filled]
            .assertView('form-filled', '.modal_visible .modal__content .create-group-form')

            // нажать на кнопку Создать
            .click('.create-group-form .button_type_submit')

            // отображается сообщение “Команда создана” [notification]
            .waitForVisible('.notification__success')
            .assertView('notification', '.notification__success .notification-content')

            // нажать на Крестик
            .click('.notification__success .ui-flat-button')

            .getUrl()
            .then(url => {
                const groupId = url.match(/\d+$/)[0];

                this.browser.setMeta('newGroup', Number(groupId));
            })

            // в списке команд присутствует новая команда
            .getText('.group-list-section .scrollable-list__content .section-list-item:nth-of-type(1) .unit__title')
            .then(text => {
                assert.equal(text, newGroup.name);
            })

            // внешний вид созданной команды [command-added]
            .waitForVisible('.loader_visible', 5000, true)
            .waitForVisible('.group-section', 3000)
            .assertView('command-added', '.sections')

            // название команды совпадает с введенным в форму
            .getText('.group-section .group-header .section-header__title')
            .then(text => {
                assert.equal(text, newGroup.name);
            });
    });

    it('Должны показаться ошибки если не заполнены обязательные поля', function() {
        /* alias: 2-errors */
        return this.browser
            // нажать на кнопку Добавить
            .click('.add-section-item-control')
            .waitForVisible('.modal_visible .create-group-form')

            // нажать на кнопку Создать
            .click('.create-group-form .button_type_submit')

            // отображаются ошибки около незаполненных полей [errors]
            .waitForVisible('.create-group-form .form__error')
            .assertView('errors', '.create-group-form');
    });
});
