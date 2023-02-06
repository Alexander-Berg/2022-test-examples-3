const runOnProject = require('../../../commands/util/runOnProject');
const mock = require('./mocks/base.json');
const baseSuite = require('../common/hermione.base');

runOnProject(['market'], 'touch', baseSuite);

runOnProject(['market'], 'touch', function() {
    it('tpah', function() {
        const text = 'iphone';

        return this.browser
            .click('.mini-suggest__input')
            .yaMockSuggest(text, mock)
            .keys(text)
            .waitForVisible('.mini-suggest__popup-content')
            .assertView('all_tpah', [
                '.mini-suggest__item_type_tpah:nth-child(1)',
                '.mini-suggest__item_type_tpah:nth-child(2)',
                '.mini-suggest__item_type_tpah:nth-child(3)',
                '.mini-suggest__item_type_tpah:nth-child(4)',
            ]);
    });
});
