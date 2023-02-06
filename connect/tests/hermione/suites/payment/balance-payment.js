const { URL } = require('url');
const { assert } = require('chai');
const { alex: noContractUser, debtor } = hermione.ctx.testUsers;

const BALANCE_PAYMENT_ERROR = 'К сожалению, ваш аккаунт используется для оплаты ' +
    'другого сервиса Яндекса. Для проведения оплаты, пожалуйста, ' +
    'используйте другой аккаунт администратора или создайте новый.';

describe('Пополнение счета', () => {
    describe('Положительные', () => {
        it('1. Редирект на заполнение платежной информации', function() {
            /* alias: pos-1-redirect */
            return this.browser
                // авторизоваться под учеткой в паспорте у которой есть организация
                // или зарегистрироваться и создать организацию
                // перейти по ссылке {{CONNECT_HOST}}/portal/balance
                .login({ ...noContractUser, retpath: '/portal/balance' })
                .disableAnimations('*')

                // выполнился переход на страницу {{CONNECT_HOST}}/portal/balance/contract?source=balance
                .waitForVisible('.create-contract-form')
                .getUrl()
                .then(url => new URL(url))
                .then(({ pathname, search }) => {
                    assert.equal(pathname, '/portal/balance/contract');
                    assert.equal(search, '?source=balance');
                });
        });
    });

    describe('Отрицательные', () => {
        it('1. Сотрудник у которого уже есть client_id', function() {
            /* alias: neg-1-has-client-id */
            return this.browser

                // залогиниться под админом у которого есть client_id и в его организации заполнена платежная информация
                // перейти по ссылке {{CONNECT_HOST}}/portal/balance
                .login({ ...debtor, retpath: '/portal/balance' })
                .waitForVisible('.balance-page__balance')

                // ввести сумму в поле "Сумма"
                .setValue('.balance__payment input', '10')

                // нажать на кнопку "Пополнить"
                .click('.payment__button_role_submit')

                // под полем "Сумма" появилось сообщение красного цвета
                .waitForExactText('.payment__error', BALANCE_PAYMENT_ERROR, 3000, true)

                // скриншот формы с сообщением [balance-payment-error]
                .assertView('balance-payment-error', '.balance-page__balance');
        });
    });
});
