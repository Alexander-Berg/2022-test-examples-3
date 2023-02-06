const runOnProject = require('../../commands/util/runOnProject');
const testWithThemes = require('../../commands/util/testWithThemes');

runOnProject(['home', 'serp'], 'touch', function() {
    describe('copy-fact', function() {
        testWithThemes('long fact', function() {
            let mockLongFact = require('./mock-copy-fact/long-fact.json');

            return this.browser
                .click('.mini-suggest__input')
                .yaMockSuggest(mockLongFact[0], mockLongFact)
                .keys(mockLongFact[0])
                .waitForVisible('.mini-suggest__popup-content')
                .assertView('popup', '.mini-suggest__popup-content')
                .click('.mini-suggest__copy')
                .assertView('copy-toast', '.content', {
                    ignoreElements: [
                        '.mini-suggest__input-clear',
                        '.mini-suggest__button',
                        '.mini-suggest__popup-content',
                    ],
                });
        });
    });
});
