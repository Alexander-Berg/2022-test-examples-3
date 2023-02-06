const mockImage = "\"data:image/svg+xml;charset=utf8,%3Csvg xmlns='http://www.w3.org/2000/svg' height='10' " +
    "width='10'%3E%3Cpath fill='%23f6ff01' d='M0 0h10v10H0z'/%3E%3Cpath d='M2 6l2 3 5-7' fill='none' " +
    "stroke='%23ff2fe4'/%3E%3C/svg%3E\"";

const runOnProject = require('../../commands/util/runOnProject');
const testWithThemes = require('../../commands/util/testWithThemes');

runOnProject(['home', 'serp'], 'desktop', function() {
    const mock = require('./mocks-direct/base');

    mock[1] = mock[1].map(function(item) {
        if (item[5]) {
            item[5].img.url = mockImage;
        }

        return item;
    });

    describe('direct', function() {
        testWithThemes('should show items', function() {
            return this.browser
                .click('.mini-suggest__input')
                .yaMockSuggest('купить квартиру', mock)
                .keys('купить квартиру')
                .waitForVisible('.mini-suggest__popup-content')
                .assertView('popup', '.mini-suggest__popup-content')
                .keys('ArrowDown')
                .assertView('popup-label-selected', '.mini-suggest__popup-content');
        });
    });
});
