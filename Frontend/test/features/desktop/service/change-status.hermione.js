const assert = require('assert');

describe('Смена статуса', function() {
    it('Изменение статуса сервиса', function() {
        return this.browser
            // открыть страницу сервиса robotinternal003service (/services/robotinternal003service/)
            // под логином robot-internal-003
            .openIntranetPage({ pathname: '/services/robotinternal003service' })
            .disableAnimations('*')

            // отображается плашка статуса "Развивается"
            // (н.у. теста, нужно изначально выставить такой статус, если он другой)
            .getText('.service-name-state__state .service-state__text').then(text =>
                assert.strictEqual(text, 'Развивается'))

            // вид плашки статуса сервиса [plain]
            .assertView('plain', '.service-name-state__state')

            // кликнуть по карандашу
            .click('.service-name-state-editor__button_role_edit')

            // появился попап с открытой формой "Редактирование статуса"
            // кликнуть по плашке "Редактирование статуса"
            .waitForVisible('.service-name-state-editor__popup')
            .click('.service-name-state-editor__popup .service-name-state-editor__item_role_state')

            // вид формы [state-edit-form]
            .assertPopupView('.service-name-state-editor__popup', 'state-edit-form', '.service-name-state-editor__item_active_yes')

            // выбираем из списка статус "Поддерживается"
            .setSelectVal('.service-state-editor__input_role_state', 'Поддерживается')

            // жмем "Сохранить"
            .click('.service-state-editor__button_role_save')

            // попап скрылся
            .waitForVisible('.service-name-state-editor__popup', 10000, true)

            // статус изменился "Поддерживается"
            .getText('.service-name-state__state .service-state__text').then(text =>
                assert.strictEqual(text, 'Поддерживается'))

            // вид плашки смены статуса [state-changed]
            .assertView('state-changed', '.service-name-state__state');
    });
});
