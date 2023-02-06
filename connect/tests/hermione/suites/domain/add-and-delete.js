const { URL } = require('url');
const { assert } = require('chai');
const { testUsers: { adminYO2: admin, jane }, cacheMode } = hermione.ctx;
const { removeDomain: dirRemoveDomain } = require('../../helpers/directoryApi');

const DOMAIN_NAME_AUTO = 'ui-tester.auto.connect-tk.tk';
const DOMAIN_NAME = 'ui-tester-yo2-16212304.ru';
const INVALID_DOMAIN_NAME = 'testru';

function addValidDomain(browser, name) {
    return browser
        // авторизоваться под администратором организации и перейти по ссылке "/portal/admin/domains"
        .login({ ...admin, retpath: '/portal/services/webmaster' })
        .waitForVisible('.service-page_slug_webmaster')
        .waitForVisible('.service-page__content .domain-list', 20000)
        // .waitForVisible('.domain-status__badge_status_pending', 5000, true)
        .disableAnimations('*')
        .disableBorderRadius('.status-notification, .modal__content')

        // нажать кнопку "+Добавить"
        .click('.domain-list__add-button')

        // открылся попап "Новый домен"
        .waitForVisible('.modal_visible_yes.add-domain-modal')

        // ввести в поле домен "*.auto.connect-test.tk"
        .setValue('.add-domain-modal .textinput__control[name="domain-name"]', name)

        // нажать кнопку "Добавить"
        .setHash('add-domain')
        .click('.modal_visible_yes.add-domain-modal .form__button_type_submit')
        .waitForVisible('.status-notification', 10000)

        // попап и сообщение закрылись
        .click('.status-notification__close-button')
        .waitForVisible('.status-notification', 8000, true)
        .waitForVisible('.modal_visible_yes.add-domain-modal', 10000, true)

        // произошел переход на страницу домена
        .waitForVisible('.domain-page_slug_webmaster', 5000)
        .getUrl()
        .then(url => {
            assert.equal(
                new URL(url).pathname,
                `/portal/services/webmaster/resources/${name}`
            );
        })
        .waitForVisible('.domain-page__header', 30000);
}

function removeDomain(browser, name) {
    return browser
        // нажать на экшен "Удалить домен"
        .click('.domain-page__action_type_remove')

        // появился попап "Вы уверены, что хотите удалить домен
        .waitForExactText('.modal_visible_yes .modal__body', `Вы уверены, что хотите удалить домен ${name}?`)

        // нажать на "Удалить"
        .click('.modal_visible_yes .modal__button_type_confirm')

        // попап закрылся
        .waitForVisible('.modal_visible_yes', 5000, true)

        // произошел переход на страницу {{CONNECT_HOST}}/portal/services/webmaster
        .waitForVisible('.service-page_slug_webmaster')
        .getUrl()
        .then(url => {
            assert.equal(new URL(url).pathname, '/portal/services/webmaster');
        })

        // на вкладке "Домены" не отображается плитка с доменом
        .waitForVisible('.service-page__content .domain-list', 10000)
        .waitForVisible(`.domain-title__name=${name}`, 1000, true);
}

describe('Добавление и удаление домена', () => {
    describe('Положительные', () => {
        it('1. Основной домен удалить нельзя', function() {
            /* alias: pos-1-main */
            return this.browser
                // перейти в карточку основного домена
                .login({ ...admin, retpath: '/portal/services/webmaster/resources/adminyo2.auto.connect-test.tk' })
                // открыта карточка домена
                .waitForVisible('.domain-page__header', 30000)

                // экшена "Удалить домен" нет
                .waitForVisible('.domain-page__action_type_remove', 1000, true)

                // скриншот карточки домена без экшена [not-action-domain]
                .assertView('not-action-domain', '.domain-page__header');
        });

        it('2. Подтвержденный домен', function() {
            let prepareData = Promise.resolve();

            if (cacheMode === 'write') {
                prepareData = dirRemoveDomain(DOMAIN_NAME_AUTO, admin);
            }

            /* alias: pos-2-confirmed */

            return prepareData
                .then(() =>
                    this.browser
                        .then(() => addValidDomain(this.browser, DOMAIN_NAME_AUTO))
                        .waitForVisible('.domain-page__header', 20000)
                        .setHash('add-domain1')

                        // подождать, обновить страницу
                        .then(() => {
                            if (cacheMode !== 'read') {
                                return this.browser.pause(10000);
                            }
                        })
                        .url(`/portal/services/webmaster/resources/${DOMAIN_NAME_AUTO}`)
                        .disableAnimations('*')
                        .disableBorderRadius('.modal__content')

                        // отображается статус "Домен подтверждён"
                        .waitForExactText('.domain-page__status', 'Домен подтверждён', 5000)

                        // экшен "Удалить домен" есть
                        .waitForVisible('.domain-page__action_type_remove')

                        // скриншот карточки домена с экшеном [action-domain]
                        .setViewportSize({ width: 1280, height: 2000 })
                        .waitForVisible('.dns-settings__table', 20000)
                        .assertView('action-domain', '.domain-page__content')

                        // нажать на экшен "Удалить домен"
                        .click('.domain-page__action_type_remove')

                        // появился попап "Вы уверены, что хотите удалить домен *.auto.connect-test.tk?"
                        .waitForExactText('.modal_visible_yes .modal__body',
                            `Вы уверены, что хотите удалить домен ${DOMAIN_NAME_AUTO}?`)
                        // скриншот попапа "Вы уверены, что хотите удалить домен [delete-domain]
                        .assertView('delete-domain', '.modal_visible_yes .modal__content')

                        // нажать на крестик
                        .click('.modal_visible_yes .modal__close-button')
                        // попап закрылся
                        .waitForVisible('.modal_visible_yes.remove-domain-control__confirm-modal', 10000, true)

                        // нажать на экшен "Удалить домен"
                        .click('.domain-page__action_type_remove')

                        // появился попап "Вы уверены, что хотите удалить домен *.auto.connect-test.tk?"
                        .waitForExactText('.modal_visible_yes .modal__body',
                            `Вы уверены, что хотите удалить домен ${DOMAIN_NAME_AUTO}?`)
                        // нажать на "Отменить"
                        .click('.modal_visible_yes .modal__button_type_cancel')
                        .setHash('delete-domain')

                        // попап закрылся
                        .waitForVisible('.modal_visible_yes.remove-domain-control__confirm-modal', 15000, true)

                        .then(() => removeDomain(this.browser, DOMAIN_NAME_AUTO))
                );
        });

        it('3. Неподтвержденный домен', function() {
            let prepareData = Promise.resolve();

            if (cacheMode === 'write') {
                prepareData = dirRemoveDomain(DOMAIN_NAME, admin);
            }

            /* alias: pos-3-unconfirmed */
            return prepareData
                .then(() =>
                    this.browser
                        .then(() => addValidDomain(this.browser, DOMAIN_NAME))

                        // нажать "К списку доменов"
                        .setHash('update-domains')
                        .click('.resource-header__back')
                        .waitForVisible('.service-page__content .domain-list', 20000)
                        // на вкладке "Домены" отображается плитка с добавленный доменом
                        .waitForVisible(`.domain-title__name=${DOMAIN_NAME}`, 10000)

                        // скриншот плитки доменов с новым доменом [updated-domain-list]
                        .waitForVisible('.domain-status__badge_status_pending', 5000, true)
                        .assertView('updated-domain-list', '.service-page__content')

                        // открыть карточку домена /portal/services/webmaster/resources/*.connect-test.tk
                        .setHash('add-domain2')
                        .url(`/portal/services/webmaster/resources/${DOMAIN_NAME}`)
                        .waitForVisible('.domain-page__header', 20000)
                        .disableBorderRadius('.modal__content')

                        .then(() => removeDomain(this.browser, DOMAIN_NAME))
                );
        });
    });

    describe('Отрицательные', () => {
        beforeEach(function() {
            return this.browser
                // авторизоваться под администратором организации и перейти по ссылке "/portal/services/webmaster"
                .login({ ...jane, retpath: '/portal/services/webmaster' })
                .waitForVisible('.service-page__content .domain-list', 20000)
                .disableAnimations('*')
                .disableBorderRadius('.modal__content')
                .hideCaret();
        });

        it('1. Добавление пустого домена', function() {
            /* alias: neg-1-empty */
            return this.browser
                // нажать кнопку "+Добавить"
                .click('.domain-list__add-button')

                // открылся попап "Новый домен"
                .waitForVisible('.modal_visible_yes.add-domain-modal')

                // кнопка "Добавить" - неактивна
                .waitForVisible('.modal_visible_yes.add-domain-modal .form__button_type_submit.button2_disabled_yes')

                // скриншот попапа с неактивной кнопкой [error]
                .assertView('disabled', '.modal_visible_yes.add-domain-modal .modal__content');
        });

        it('2. Отмена добавления домена', function() {
            /* alias: neg-2-cancel */
            return this.browser

                // нажать кнопку "+Добавить"
                .click('.domain-list__add-button')

                // открылся попап "Новый домен"
                .waitForVisible('.modal_visible_yes.add-domain-modal')

                // нажать кнопку "Отменить"
                .click('.modal_visible_yes.add-domain-modal .form__button_type_cancel')

                // попап закрылся
                // отображается страница с доменами [domain-list]
                .assertView('domain-list', '.service-page__content');
        });

        it('3. Добавление некорректного домена', function() {
            /* alias: neg-3-invalid */
            return this.browser
                // нажать кнопку "+Добавить"
                .click('.domain-list__add-button')

                // открылся попап "Новый домен"
                .waitForVisible('.modal_visible_yes.add-domain-modal')

                // ввести в поле некорректный домен "testru"
                .setValue('.add-domain-modal .textinput__control[name="domain-name"]', INVALID_DOMAIN_NAME)

                // нажать кнопку "Добавить"
                .click('.modal_visible_yes.add-domain-modal .form__button_type_submit')

                // появилось сообщение "Некорректное имя домена"
                // попап остался открытым
                .waitForVisible('.modal_visible_yes.add-domain-modal .add-domain-modal__error')

                // скриншот попапа с сообщением [invalid-domain-error]
                .assertView('invalid-domain-error', '.modal_visible_yes.add-domain-modal .modal__content');
        });
    });
});
