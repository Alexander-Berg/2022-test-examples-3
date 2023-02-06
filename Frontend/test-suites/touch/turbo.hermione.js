const runOnProject = require('../../commands/util/runOnProject');
const testWithThemes = require('../../commands/util/testWithThemes');

runOnProject(['home', 'serp'], 'touch', function() {
    describe('turbo', function() {
        testWithThemes('turbo icon', function() {
            return this.browser
                .click('.mini-suggest__input')
                .yaMockSuggest('yandex', require('./mocks-turbo/turbo-icon'))
                .keys('yandex')
                .waitForVisible('.mini-suggest__popup-content')
                .assertView('simple', '.mini-suggest__popup-content');
        });
    });
});
