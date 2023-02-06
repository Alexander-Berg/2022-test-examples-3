const runOnProject = require('../../commands/util/runOnProject');
const testWithThemes = require('../../commands/util/testWithThemes');

runOnProject(['home', 'serp'], 'touch', function() {
    describe('affix', function() {
        testWithThemes('scroll to end', function() {
            var tpah = '111111111111111111111111111center11111111111111111111111111111end';

            return this.browser
                .click('.mini-suggest__input')
                .yaMockSuggest('1', ['1111', '11111', [[tpah, 0, { tpah: [0, 1, tpah.length] }]], {}])
                .keys('1')
                .waitForVisible('.mini-suggest__popup-content')
                .execute(function() {
                    var block = MBEM.getBlock(document.querySelector('.mini-suggest'), 'mini-suggest');
                    block._freezeClicks = 0;
                })
                .click('.mini-suggest__item_type_tpah')
                .waitUntil(async() => (await this.browser.getValue('.mini-suggest__input')) === tpah + ' ')
                .assertView('simple', '.mini-suggest__input');
        });
    });
});
