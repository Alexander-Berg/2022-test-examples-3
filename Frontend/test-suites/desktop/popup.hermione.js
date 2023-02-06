const runOnProject = require('../../commands/util/runOnProject');
const testWithThemes = require('../../commands/util/testWithThemes');

runOnProject(['home', 'serp'], 'desktop', function() {
    describe('popup', function() {
        testWithThemes('popup', function() {
            return this.browser
                .click('.mini-suggest__input')
                .yaMockSuggest('yandex', require('./mocks-popup/yandex'))
                .keys('yandex')
                .waitForVisible('.mini-suggest__popup-content')
                .assertView('simple', '.mini-suggest__popup-content')

                .yaMockSuggest('длина окружности', require('./mocks-popup/fact'))
                .setValue('.mini-suggest__input', 'длина окружности')
                .waitForVisible('.mini-suggest__popup-content')
                .assertView('fact', '.mini-suggest__popup-content')

                .yaMockSuggest('пробки в мск', require('./mocks-popup/traffic'))
                .setValue('.mini-suggest__input', 'пробки в мск')
                .waitForVisible('.mini-suggest__popup-content')
                .assertView('fact_traffic', '.mini-suggest__popup-content')

                .yaMockSuggest('погода в с', require('./mocks-popup/weather'))
                .setValue('.mini-suggest__input', 'погода в с')
                .waitForVisible('.mini-suggest__popup-content')
                .assertView('fact_weather', '.mini-suggest__popup-content')

                .yaMockSuggest('флаг', require('./mocks-popup/flags'))
                .setValue('.mini-suggest__input', 'флаг')
                .waitForVisible('.mini-suggest__popup-content')
                .assertView('fact_flags', '.mini-suggest__popup-content')

                .yaMockSuggest('как почистить блендер сковородкой', require('./mocks-popup/fulltext'))
                .setValue('.mini-suggest__input', 'как почистить блендер сковородкой')
                .waitForVisible('.mini-suggest__popup-content')
                .assertView('long_fulltext', '.mini-suggest__popup-content');
        });
    });
});
