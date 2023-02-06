const { assert } = require('chai');
const { adminYO1: user } = hermione.ctx.testUsers;

const VALID_CODE = 'CONNECT_50';

describe('Промокод на странице баланса', () => {
    beforeEach(function() {
        return this.browser
            // авторизоваться под администратором c заполненной платежной информацией
            // и перейти по ссылке "/portal/balance"
            .login({ ...user, retpath: '/portal/balance' })
            .hideCaret()
            .waitForVisible('.balance-page')
            .waitForVisible('.balance-page__promo-code')
            .click('.promo-code__button_role_toggle');
    });

    describe('Положительные', () => {
        it('1. Действующий промокод', function() {
            /* alias: pos-1-valid */

            return this.browser
                // в поле промокод ввести действующий промокод, например, CONNECT_50_WITHOUT_TRACKER
                .setValue('.promo-code__input input', VALID_CODE)
                // кнопка "Применить" стала кликабельной
                .getAttribute('.promo-code__button_role_submit', 'disabled')
                .then(disabled => {
                    assert.isNull(disabled);
                })
                .setHash('create-promocode')
                // нажать на кнопку "Применить"
                .click('.promo-code__button_role_submit')
                // промокод применен
                .waitForExactText('.promo-code__name', VALID_CODE)
                // отображается сообщение "Промокод успешно активирован" [notification]
                .closeSuccessNotify('notification')
                // внешний вид введенного промокода [success]
                .assertView('success', '.balance-page__promo-code');
        });
    });

    describe('Отрицательные', () => {
        it('1. Несуществующий промокод', function() {
            /* alias: neg-1-invalid */

            return this.browser
                // в поле промокод ввести несуществующий промокод, например qwerty
                .setValue('.promo-code__input input', 'qwerty')
                // нажать на кнопку "Применить"
                .click('.promo-code__button_role_submit')
                // промокод не применился, появилось сообщение о несуществующем коде
                .waitForVisible('.promo-code__error')
                // скриншот экрана с сообщением об ошибке [error]
                .assertView('error', '.promo-code');
        });

        it('2. Промокод не введен', function() {
            /* alias: neg-2-empty */

            return this.browser
                // поле "промокод" оставить пустым
                // кнопка "применить" осталась серой и неактивной
                .getAttribute('.promo-code__button_role_submit', 'disabled')
                .then(disabled => {
                    assert.strictEqual(disabled, 'true');
                });
        });
    });
});
