const runOnProject = require('../../commands/util/runOnProject');
const testWithThemes = require('../../commands/util/testWithThemes');

runOnProject(['home', 'serp'], 'touch', function() {
    describe('phone', function() {
        testWithThemes('phone', function() {
            return this.browser
                .click('.mini-suggest__input')
                .yaMockSuggest('+7', require('./mocks-phone/phone'))
                .keys('+7')
                .waitForVisible('.mini-suggest__popup-content')
                .assertView('simple', '.mini-suggest__popup-content');
        });
    });
});
