const runOnProject = require('../../../commands/util/runOnProject');
const baseSuite = require('../common/hermione.base');

runOnProject(['market'], 'desktop', function() {
    baseSuite.apply(this, arguments);

    it('Навигация стрелками', function() {
        const text = 'iphone';
        const mock = require('./mocks/base.json');

        return this.browser
            .yaMockSuggest(text, mock)
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
