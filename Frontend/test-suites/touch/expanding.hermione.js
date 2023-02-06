describe('expanding', function() {
    it('expanding', function() {
        return this.browser
            .yaOpenExample('serp', 'touch')
            .execute(function() {
                // Драйвер хрома не умеет ландшафтную ориентацию девайсов, приходится перекрывать значения
                try {
                    var val = innerWidth > innerHeight ? 90 : 0;
                    Object.defineProperty(window, 'orientation', { value: val });
                    Object.defineProperty(window.screen, 'orientation', { value: val });
                } catch (err) {
                    // Нет defineProperty
                }
            })
            .click('.mini-suggest__input:not(.mini-suggest__control)')
            .yaMockSuggest('yandex', require('./mocks-expanding/yandex'))
            .keys('yandex')
            // Иконки могут моргать, так как на тестовых запросах нет кеширования
            .pause(300)
            .assertView('simple', 'body')

            .click('.mini-suggest__input-clear')
            .pause(500)
            .yaMockSuggest('yandex.ru главная страница вход почта ', require('./mocks-expanding/mail'))
            .keys('yandex.ru главная страница вход почта ')
            .pause(300)
            .assertView('two_lines', 'body')

            .click('.mini-suggest__input-clear')
            .pause(500)
            .yaMockSuggest('переводчик с английского на русский онлайн бесплатно без вирусов', require('./mocks-expanding/translate'))
            .keys('переводчик с английского на русский онлайн бесплатно без вирусов')
            .assertView('long', 'body')

            .click('.mini-suggest__popup-spacer')
            .pause(300)
            .assertView('blur', 'body')

            .click('.mini-suggest__input-clear')
            .pause(500)
            .yaMockSuggest('очень длинный запрос очень длинный запрос очень длинный запрос очень длинный запрос ' +
                'очень длинный запрос очень длинный запрос очень длинный запрос', require('./mocks-expanding/extra-long'))
            .keys('очень длинный запрос очень длинный запрос очень длинный запрос очень длинный запрос очень длинный запрос очень длинный запрос очень длинный запрос')
            .assertView('extra_long', 'body');
    });
});
