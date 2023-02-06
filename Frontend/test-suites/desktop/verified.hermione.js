const runOnProject = require('../../commands/util/runOnProject');
const testWithThemes = require('../../commands/util/testWithThemes');

runOnProject(['home', 'serp'], 'desktop', function() {
    describe('verified', function() {
        testWithThemes('verified', function() {
            return this.browser
                .click('.mini-suggest__input')
                .yaMockSuggest('yandex', require('./mocks-verified/verified'))
                .keys('yandex')
                .waitForVisible('.mini-suggest__popup-content')
                .assertView('simple', '.mini-suggest__popup-content');
        });
    });
});
