const mockImage = "\"data:image/svg+xml;charset=utf8,%3Csvg xmlns='http://www.w3.org/2000/svg' height='10' " +
    "width='10'%3E%3Cpath fill='%23f6ff01' d='M0 0h10v10H0z'/%3E%3Cpath d='M2 6l2 3 5-7' fill='none' " +
    "stroke='%23ff2fe4'/%3E%3C/svg%3E\"";

const runOnProject = require('../../commands/util/runOnProject');
const testWithThemes = require('../../commands/util/testWithThemes');

runOnProject(['home', 'serp'], 'touch', function() {
    describe('direct', function() {
        const mock = require('./mocks-direct/base');

        testWithThemes('should show base item', function() {
            return this.browser
                .click('.mini-suggest__input')
                .yaMockSuggest('купить квартиру', mock)
                .keys('купить квартиру')
                .waitForVisible('.mini-suggest__popup-content')
                .assertView('popup', '.mini-suggest__popup-content');
        });

        testWithThemes('should show item with age', function() {
            const mock = require('./mocks-direct/age');

            return this.browser
                .click('.mini-suggest__input')
                .yaMockSuggest('купить квартиру', mock)
                .keys('купить квартиру')
                .waitForVisible('.mini-suggest__popup-content')
                .assertView('popup', '.mini-suggest__popup-content');
        });

        testWithThemes('should show item with long warning', function() {
            const mock = require('./mocks-direct/warn');

            return this.browser
                .click('.mini-suggest__input')
                .yaMockSuggest('купить квартиру', mock)
                .keys('купить квартиру')
                .waitForVisible('.mini-suggest__popup-content')
                .assertView('popup', '.mini-suggest__popup-content');
        });

        testWithThemes('should show item with short warning', function() {
            const mock = require('./mocks-direct/warnlen');

            return this.browser
                .click('.mini-suggest__input')
                .yaMockSuggest('купить квартиру', mock)
                .keys('купить квартиру')
                .waitForVisible('.mini-suggest__popup-content')
                .assertView('popup', '.mini-suggest__popup-content');
        });

        testWithThemes('should show full item', function() {
            const mock = require('./mocks-direct/full');

            mock[3][5].img.url = mockImage;

            return this.browser
                .click('.mini-suggest__input')
                .yaMockSuggest('купить квартиру', mock)
                .keys('купить квартиру')
                .waitForVisible('.mini-suggest__popup-content')
                .assertView('popup', '.mini-suggest__popup-content');
        });

        testWithThemes('should show item with yaservice icon', function() {
            const mock = require('./mocks-direct/yaservice');

            mock[3][5].img.url = mockImage;

            return this.browser
                .click('.mini-suggest__input')
                .yaMockSuggest('купить квартиру', mock)
                .keys('купить квартиру')
                .waitForVisible('.mini-suggest__popup-content')
                .assertView('popup', '.mini-suggest__popup-content');
        });

        testWithThemes('should show item with yaservice and verified icons', function() {
            const mock = require('./mocks-direct/marks');

            mock[3][5].img.url = mockImage;

            return this.browser
                .click('.mini-suggest__input')
                .yaMockSuggest('купить квартиру', mock)
                .keys('купить квартиру')
                .waitForVisible('.mini-suggest__popup-content')
                .assertView('popup', '.mini-suggest__popup-content');
        });
    });
});
