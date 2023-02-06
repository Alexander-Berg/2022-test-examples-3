const { assert } = require('chai');
const { testUsers: { adminYO2: admin } } = hermione.ctx;

const DomainName = {
    AUTO: 'ui-tester-yo2-59c7b163.auto.connect-tk.tk',
    RU: 'glav-ui.test.ru',
    COM: 'glav-ui.com',
};

function checkDomain(browser, type, attrValue, status) {
    return browser
        // авторизоваться под администратором организации
        // перейти в карточку домена {{CONNECT_HOST}}/portal/services/webmaster/resources/glav.com
        .login({ ...admin, retpath: `/portal/services/webmaster/resources/${type}-${DomainName.COM}` })
        .waitForVisible('.domain-verification')

        // нажать на вкладку type
        .click(`.domain-verification__method-radio_type_webmaster-${type}`)
        .getAttribute('.domain-verification__method-radio.radio-button__radio_checked_yes input', 'value')
        .then(text => {
            assert.equal(text, attrValue);
        })
        .waitForVisible('.domain-verification__button:not(.button2_progress_yes)', 30000)

        // внешний вид вкладки c инструкцией по настройке домена [domain]
        .assertView('domain', '.domain-page__content')

        .setHash('check-domain')

        // нажать кнопку "Проверить"
        .click('.domain-verification__button')

        // кнопка "Проверить" не активна
        .waitForVisible('.domain-verification__button.button2_progress_yes')

        // ничего не делать, подождать
        .waitForVisible('.domain-verification__button:not(.button2_progress_yes)', 30000)
        .getText('.domain-verification__last-check-date')
        .then(text => {
            assert(text.startsWith('Последняя проверка: '));
        })
        .getText('.domain-verification__last-check-status')
        .then(statusText => {
            assert.equal(statusText, `Результат: ${status}`);
        });
}

describe('Способы подтверждения домена', () => {
    describe('Положительные', () => {
        it('1. Способы подтверждения для домена с доменной зоны RU', function() {
            /* alias: pos-1-ru */
            return this.browser
                // авторизоваться под администратором организации
                // перейти в карточку домена /portal/services/webmaster/resources/glav.test.ru
                .login({ ...admin, retpath: `/portal/services/webmaster/resources/${DomainName.RU}` })

                // открыта карточка домена
                .waitForVisible('.domain-page')

                // состояние домена "Домен не подтверждён"
                .waitForExactText('.domain-page__status', 'Домен не подтверждён', 15000)

                // отображается вкладка "Подтверждение домена"
                .getText('.tab-link__group .tab-link')
                .then(text => {
                    assert.equal(text, 'Подтверждение домена');
                })
                .waitForVisible('.domain-verification')

                // отображаются способы подтверждения "DNS", "Метатег", "HTML-файл"
                .waitForExactText('.domain-verification__method .radio-button__text', 'DNS')
                .waitForExactText('.domain-verification__method .radio-button__text', 'Метатег')
                .waitForExactText('.domain-verification__method .radio-button__text', 'HTML-файл')

                // отображается кнопка "Проверить"
                .waitForExactText('.domain-verification__actions .button2__text', 'Проверить')

                // внешний вид карточки домена [ru-domain-card]
                .assertView('ru-domain-card', '.domain-page__content');
        });

        it('2. Способы подтверждения для домена верхнего уровня с доменной зоны COM', function() {
            /* alias: pos-2-com */
            return this.browser
                // авторизоваться под администратором организации
                // перейти в карточку домена {{CONNECT_HOST}}/portal/services/webmaster/resources/glav.com
                .login({ ...admin, retpath: `/portal/services/webmaster/resources/${DomainName.COM}` })

                // открылась карточка домена
                .waitForVisible('.domain-page')

                // состояние домена "Домен не подтверждён"
                .waitForExactText('.domain-page__status', 'Домен не подтверждён', 5000)

                // отображается вкладка "Подтверждение домена"
                .getText('.tab-link__group .tab-link')
                .then(text => {
                    assert.equal(text, 'Подтверждение домена');
                })
                .waitForVisible('.domain-verification')

                // отображаются способы подтверждения "DNS", "Метатег", "HTML-файл", "WHOIS"
                .waitForExactText('.domain-verification__method .radio-button__text', 'DNS')
                .waitForExactText('.domain-verification__method .radio-button__text', 'Метатег')
                .waitForExactText('.domain-verification__method .radio-button__text', 'HTML-файл')
                .waitForExactText('.domain-verification__method .radio-button__text', 'WHOIS')

                // отображается кнопка "Проверить"
                .waitForExactText('.domain-verification__actions .button2__text', 'Проверить')

                // скриншот карточки домена [com-domain-card]
                .assertView('com-domain-card', '.domain-page__content');
        });

        it('3. Запуск проверки домена через "DNS"', function() {
            /* alias: pos-3-dns */
            return this.browser
                .then(() => checkDomain(this.browser, 'dns', 'webmaster.dns', 'DNS-запись не найдена'));
        });

        it('4. Запуск проверки домена через "Метатег"', function() {
            /* alias: pos-4-metatag */
            return this.browser
                .then(() => checkDomain(this.browser, 'meta-tag', 'webmaster.meta_tag', 'Страница недоступна.'));
        });

        it('5. Запуск проверки домена через "HTML-файл"', function() {
            /* alias: pos-5-html */
            return this.browser
                .then(() => checkDomain(this.browser, 'html-file', 'webmaster.html_file', 'Страница недоступна.'));
        });

        it('6. Запуск проверки домена через "WHOIS"', function() {
            /* alias: pos-6-whois */
            return this.browser
                .then(() => checkDomain(this.browser, 'whois', 'webmaster.whois',
                    'Не удалось подтвердить права на домен. Попробуйте ещё раз.'));
        });
    });

    describe('Отрицательные', () => {
        it('1. Домен подтвержден, способов проверки нет', function() {
            /* alias: neg-1-verify */
            return this.browser
                // авторизоваться под администратором организации
                // перейти в карточку подтвержденного домена /portal/services/webmaster/resources/ui.auto.connect-tk.tk
                .login({ ...admin, retpath: `/portal/services/webmaster/resources/${DomainName.AUTO}` })
                .waitForVisible('.domain-page')
                .disableAnimations('*')

                // внешний вид карточки домена [domain]
                .waitForVisible('.dns-settings')
                .assertView('domain', '.domain-page__content')

                // состояние домена "Домен подтверждён"
                .waitForExactText('.domain-page__status', 'Домен подтверждён', 5000)

                // вкладки "Подтверждение домена" нет
                // отображается вкладка "Управление DNS"
                .getText('.tab-link__group .tab-link')
                .then(text => {
                    assert.equal(text, 'Управление DNS');
                })
                // способов подтверждения домена нет
                .waitForVisible('.domain-verification', 1000, true);
        });
    });
});
