const runOnProject = require('../../commands/util/runOnProject');
const testWithThemes = require('../../commands/util/testWithThemes');

runOnProject(['home', 'serp'], 'touch', function() {
    describe('popup', function() {
        testWithThemes('popup', function() {
            return this.browser
                .click('.mini-suggest__input')
                .yaMockSuggest('yandex', require('./mocks-popup/yandex'))
                .keys('yandex')
                .waitForVisible('.mini-suggest__popup-content')
                .assertView('simple', '.mini-suggest__popup-content')

                .scroll('.mini-suggest__input-clear')
                .click('.mini-suggest__input-clear')
                .yaMockSuggest('yandex.ru главная страница вход почта ', require('./mocks-popup/mail'))
                .keys('yandex.ru главная страница вход почта ')
                .waitForVisible('.mini-suggest__popup-content')
                .assertView('only_tpah', '.mini-suggest__popup-content')

                .scroll('.mini-suggest__input-clear')
                .click('.mini-suggest__input-clear')
                .yaMockSuggest('длина окружности', require('./mocks-popup/fact'))
                .keys('длина окружности')
                .waitForVisible('.mini-suggest__popup-content')
                .assertView('fact', '.mini-suggest__popup', { allowViewportOverflow: true })

                .scroll('.mini-suggest__input-clear')
                .click('.mini-suggest__input-clear')
                .yaMockSuggest('пробкти в мск', require('./mocks-popup/traffic'))
                .keys('пробкти в мск')
                .waitForVisible('.mini-suggest__popup-content')
                .assertView(
                    'only_tpah_and_fact',
                    '.mini-suggest__popup',
                    { allowViewportOverflow: true }
                )

                .scroll('.mini-suggest__input-clear')
                .click('.mini-suggest__input-clear')
                .yaMockSuggest('яндекс пробки нижний посёлок', require('./mocks-popup/fulltext'))
                .keys('яндекс пробки нижний посёлок')
                .waitForVisible('.mini-suggest__popup-content')
                .assertView('only_fulltext', '.mini-suggest__popup-content');
        });
    });
});
