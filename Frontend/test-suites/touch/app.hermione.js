const runOnProject = require('../../commands/util/runOnProject');
const testWithThemes = require('../../commands/util/testWithThemes');
const mockApp = require('./mock-app/base.json');
const mockAppWithMarks = require('./mock-app/marks.json');
mockAppWithMarks[3][5].img.url = mockApp[3][5].img.url = "\"data:image/svg+xml;charset=utf8,%3Csvg xmlns='http://www.w3.org/2000/svg' height='10' " +
    "width='10'%3E%3Cpath fill='%23f6ff01' d='M0 0h10v10H0z'/%3E%3Cpath d='M2 6l2 3 5-7' fill='none' " +
    "stroke='%23ff2fe4'/%3E%3C/svg%3E\"";

runOnProject(['home', 'serp'], 'touch', function() {
    describe('app', function() {
        testWithThemes('app', function() {
            const text = 'игра престолов';

            return this.browser
                .click('.mini-suggest__input')
                .yaMockSuggest(text, mockApp)
                .keys(text)
                .waitForVisible('.mini-suggest__popup-content')
                .assertView('popup', '.mini-suggest__popup-content');
        });

        testWithThemes('app with marks', function() {
            const text = 'игра престолов';

            return this.browser
                .click('.mini-suggest__input')
                .yaMockSuggest(text, mockAppWithMarks)
                .keys(text)
                .waitForVisible('.mini-suggest__popup-content')
                .assertView('popup', '.mini-suggest__popup-content');
        });
    });
});
