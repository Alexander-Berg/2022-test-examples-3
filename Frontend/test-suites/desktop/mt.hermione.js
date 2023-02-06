const runOnProject = require('../../commands/util/runOnProject');
const testWithThemes = require('../../commands/util/testWithThemes');

runOnProject(['home', 'serp'], 'desktop', function() {
    describe('mt', function() {
        testWithThemes('should show mt with flag', function() {
            return this.browser
                .click('.mini-suggest__input')
                .yaMockSuggest('кот по анг', require('./mocks-mt/flag'))
                .keys('кот по анг')
                .waitForVisible('.mini-suggest__popup-content')
                .assertView('mt-with-flag', '.mini-suggest__popup-content');
        });

        testWithThemes('should show mt without flag', function() {
            return this.browser
                .click('.mini-suggest__input')
                .yaMockSuggest('кот по ара', require('./mocks-mt/no-flag'))
                .keys('кот по ара')
                .waitForVisible('.mini-suggest__popup-content')
                .assertView('mt-without-flag', '.mini-suggest__popup-content');
        });
    });
});
