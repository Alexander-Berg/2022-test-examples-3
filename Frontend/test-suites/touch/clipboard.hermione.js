const runOnProject = require('../../commands/util/runOnProject');
const testWithThemes = require('../../commands/util/testWithThemes');

const showSuggestWithClipboard = (browser, data) => {
    return browser
        .click('.mini-suggest__input')
        .keys('y')
        .waitForVisible('.mini-suggest__popup-content')
        .execute(function(data) {
            const instance = MBEM.getBlock(document.querySelector('.mini-suggest'), 'mini-suggest');
            instance._renderItems([data]);
        }, data);
};

runOnProject(['home'], 'touch', function() {
    describe('clipboard', function() {
        describe('Внешний вид', function() {
            testWithThemes('Текст', function() {
                const mock = require('./mocks-clipboard/clipboard-text.json');

                return showSuggestWithClipboard(this.browser, mock)
                    .assertView('popup', '.mini-suggest__popup-content');
            });

            testWithThemes('Длинный текст', function() {
                const mock = require('./mocks-clipboard/clipboard-long-text.json');

                return showSuggestWithClipboard(this.browser, mock)
                    .assertView('popup', '.mini-suggest__popup-content');
            });

            testWithThemes('Ссылка', function() {
                const mock = require('./mocks-clipboard/clipboard-link.json');

                return showSuggestWithClipboard(this.browser, mock)
                    .assertView('popup', '.mini-suggest__popup-content');
            });

            testWithThemes('Длинная ссылка', function() {
                const mock = require('./mocks-clipboard/clipboard-long-link.json');

                return showSuggestWithClipboard(this.browser, mock)
                    .assertView('popup', '.mini-suggest__popup-content');
            });
        });
    });
});
