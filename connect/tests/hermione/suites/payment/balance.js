const { URL } = require('url');
const { assert } = require('chai');
const { adminUITest2, chuck, externalAdmin, debtor } = hermione.ctx.testUsers;

describe('Вкладка "Баланс"', () => {
    describe('Положительные', () => {
        it('1. Отображение вкладки "Баланс" у админа без задолженности', function() {
            /* alias: pos-1-no-dept */

            return this.browser

                // залогиниться под админом любой организации и перейти по ссылке /portal/admin
                .login({ ...adminUITest2, retpath: '/portal/admin' })
                .waitForExist('.app__sidebar')

                .hideCaret()

                // слева в сайдбаре есть раздел "Баланс" [sidebar]
                .assertView('sidebar', '.app__sidebar')

                // нажать на раздел "Баланс"
                .click('.sidebar__menu-item=Баланс')

                // произошел переход на страницу баланса
                .waitForVisible('.balance-page')
                .getUrl()
                .then(url => new URL(url).pathname)
                .then(path => {
                    assert.equal(path, '/portal/balance');
                })
                .hideCaret()

                // есть поле с указанием баланса
                .waitForVisible('.currency-amount')

                // есть форма для ввода суммы
                .waitForVisible('.balance__payment .payment__form')

                // есть кнопка "пополнить"
                .waitForVisible('.payment__button_role_submit=Пополнить')

                // ниже указаны подключенные услуги и промокоды
                .waitForVisible('.balance-page__promo-code')
                .waitForVisible('.balance-page__services')
                .waitForVisible('.tab-link=Подключенные платные услуги')

                // пример  внешнего вида страницы баланса
                .assertView('plain', '.balance-page');
        });

        hermione.skip.in(/.*/, 'Задолженность приезжает из Я.Баланса');
        it('2. Отображение вкладки "Баланс" у админа с задолженностью', function() {
            /* alias: pos-2-dept */
            return this.browser
                // залогиниться под админом организации с задолженностью и перейти по ссылке portal/balance
                .login({ ...debtor, retpath: '/portal/balance' })

                // произошел переход на страницу баланса
                .waitForVisible('.balance-page')

                // вверху красным цветом выделена сумма задолженности
                .waitForVisible('.balance__value_negative')
                .waitForExactText('.currency-amount', '-10 000 ₽')

                // есть напоминание о необходимости погасить задолженность и сроке отключения услуг
                .waitForExactText('.balance__description', 'Вам необходимо погасить задолженность до ' +
                    '31 декабря 2018 года. В случае неуплаты платные сервисы будут отключены.', 1000, true)

                .hideCaret()

                // пример  внешнего вида страницы баланса
                .assertView('plain', '.balance-page');
        });
    });

    describe('Отрицательные', () => {
        it('1. Отображение вкладки Баланс у неадмина', function() {
            /* alias: neg-1-non-admin */
            return this.browser
                // залогинитьcя под аккаунтом сотрудника любой организации и перейти на portal/balance
                .login({ ...chuck, retpath: '/portal/balance' })

                // переход на страницу баланса не произошел
                .waitForVisible('.balance', 3000, true)
                // случился редирект на forbidden
                .waitForVisible('.forbidden-page__details');
        });

        it('2. Отображение вкладки Баланс у внешнего админа', function() {
            /* alias: neg-2-external-admin */
            return this.browser
                // залогинитьcя под аккаунтом внешнего адлмина и перейти на portal/balance
                .login({ ...externalAdmin, retpath: '/portal/balance' })

                // переход на страницу баланса не произошел
                .waitForVisible('.balance', 3000, true)
                // случился редирект на forbidden
                .waitForVisible('.forbidden-page__details');
        });
    });
});
