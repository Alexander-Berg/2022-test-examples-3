const getCurrentEnv = require('../../../helpers/getCurrentEnv');

// Не стартует тест в CI. Вернуть после разбирательств и починки (INFRADUTY-19235)
// Пример ошибки: https://proxy.sandbox.yandex-team.ru/2503487607/index.html?testNameFilter=Главная%20/%20Главная%20страница%20internal%20Открыть%20главную

describe('Главная / Главная страница internal', function() {
    // eslint-disable-next-line mocha/no-skipped-tests
    it.skip('Открыть главную', function() {
        const env = getCurrentEnv(this);
        return this.browser
            .yaLogin(env)
            .openPage('', '/search', env)
            .waitForVisible('.content')
            .assertView('plain', 'html', {
                ignoreElements: '.footer__addon-text, .chat',
            })
            .setValue('.tools-header-suggest__input', 'д')
            .waitForVisible('.search-suggest__popup-content', 5000)
            .assertView('with_suggest', 'html', {
                ignoreElements: '.footer__addon-text, .chat, .tools-header-suggest-item__logo',
            });
    });
});
