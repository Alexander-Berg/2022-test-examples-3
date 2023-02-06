const runOnProject = require('../../commands/util/runOnProject');
const testWithThemes = require('../../commands/util/testWithThemes');

runOnProject(['home', 'serp'], 'touch', function() {
    describe('personal', function() {
        testWithThemes('should show mixed items', function() {
            return this.browser
                .click('.mini-suggest__input')
                .yaMockSuggest('яндекс', require('./mocks-personal/mixed'))
                .keys('яндекс')
                .waitForVisible('.mini-suggest__popup-content')
                .assertView('mixed', '.mini-suggest__popup', { allowViewportOverflow: true, ignoreElements: ['.mini-suggest'] })
                .execute(function() {
                    const block = MBEM.getBlock(document.querySelector('.mini-suggest'), 'mini-suggest');
                    block._freezeClicks = 0;
                })
                // Сразу после показа попапа клики по нему задизаблены
                // Ждём, пока станут доступны
                .click('.mini-suggest__delete')
                // Ждём окончания анимации
                .pause(2000)
                .assertView('deleted', '.mini-suggest__popup', { allowViewportOverflow: true, ignoreElements: ['.mini-suggest'] });
        });

        testWithThemes('should show full history', function() {
            return this.browser
                .yaMockSuggest('', require('./mocks-personal/full'))
                .click('.mini-suggest__input')
                .waitForVisible('.mini-suggest__popup-content')
                .assertView('full', '.mini-suggest__popup', { allowViewportOverflow: true, ignoreElements: ['.mini-suggest'] });
        });
    });
});
