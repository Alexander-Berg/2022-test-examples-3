/* eslint-disable max-nested-callbacks */
const { assert } = require('chai');
const { alex } = hermione.ctx.testUsers;

const LONG_DOMAIN_NAME = 'loooooooooooooooooooooooooooooooooooooooooooooooooooooooooooong.ru';

describe('Форма добавления домена', () => {
    beforeEach(function() {
        return this.browser

            // авторизоваться под администратором организации и перейти по ссылке "/portal/services/webmaster"
            .login({ ...alex, retpath: '/portal/services/webmaster' })
            .waitForVisible('.service-page__content .domain-list', 20000)
            .waitForVisible('.domain-status__badge_status_pending', 5000, true)
            .disableAnimations('*')
            .disableBorderRadius('.modal__content')
            .hideCaret();
    });

    it('1. Корректное отображение подсказки', function() {
        /* alias: 1-long-name */
        return this.browser

            // нажать кнопку "+Добавить"
            .click('.domain-list__add-button')

            // открылся попап "Новый домен"
            .waitForVisible('.modal_visible_yes.add-domain-modal')

            // ввести "loooooooooooooooooooooooooooooooooooooooooooooooooooooooooooong.ru" в поле нового домена
            .setValue('.modal_visible_yes.add-domain-modal .textinput__control[name="domain-name"]', LONG_DOMAIN_NAME)

            // ширина подсказки соотвествует ширине попапа
            .getAttribute('.modal_visible_yes.add-domain-modal .add-domain-modal__hint', 'offsetWidth')
            .then(offsetWidth => this.browser
                .getAttribute('.modal_visible_yes.add-domain-modal .add-domain-modal__hint', 'scrollWidth')
                .then(scrollWidth => {
                    assert.equal(scrollWidth, offsetWidth);
                }))

            // скриншот попапа [tip]
            .assertView('tip', '.modal_visible_yes.add-domain-modal .modal__content');
    });
});
