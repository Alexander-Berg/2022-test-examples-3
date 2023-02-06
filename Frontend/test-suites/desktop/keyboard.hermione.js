const runOnProject = require('../../commands/util/runOnProject');

runOnProject(['serp'], 'desktop', function() {
    it('Навигация стрелками', function() {
        const text = 'iphone';

        return this.browser
            .yaMockSuggest(text, require('./mocks-popup/yandex'))
            .click('.mini-suggest__input')
            .keys(text)
            .waitForVisible('.mini-suggest__popup-content')
            .keys('ArrowDown')
            .assertView('after-down-selected', ['.mini-suggest', '.mini-suggest__popup-content'])
            .keys('ArrowUp')
            .assertView('zero-selected', ['.mini-suggest', '.mini-suggest__popup-content'])
            .keys('ArrowUp')
            .assertView('last-item-selected', ['.mini-suggest', '.mini-suggest__popup-content']);
    });
});
