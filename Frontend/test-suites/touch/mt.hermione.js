const mockImage = "\"data:image/svg+xml;charset=utf8,%3Csvg xmlns='http://www.w3.org/2000/svg' height='10' " +
    "width='10'%3E%3Cpath fill='%23f6ff01' d='M0 0h10v10H0z'/%3E%3Cpath d='M2 6l2 3 5-7' fill='none' " +
    "stroke='%23ff2fe4'/%3E%3C/svg%3E\"";

const runOnProject = require('../../commands/util/runOnProject');
const testWithThemes = require('../../commands/util/testWithThemes');

runOnProject(['serp'], 'touch', function() {
    describe('mt', function() {
        for (const view of ['base', 'reverse']) {
            testWithThemes(`${view}`, function() {
                let mockMt = require(`./mock-mt/mt-${view}.json`);
                mockMt[3][5].icon.url = mockImage;

                return this.browser
                    .click('.mini-suggest__input')
                    .yaMockSuggest(mockMt[0], mockMt)
                    .keys(mockMt[0])
                    .waitForVisible('.mini-suggest__popup-content')
                    .assertView('popup', '.mini-suggest__popup-content');
            });

            testWithThemes(`${view}-without-icon`, function() {
                let mockMt = JSON.parse(JSON.stringify(require(`./mock-mt/mt-${view}.json`)));
                mockMt[3][5].icon = null;

                return this.browser
                    .click('.mini-suggest__input')
                    .yaMockSuggest(mockMt[0], mockMt)
                    .keys(mockMt[0])
                    .waitForVisible('.mini-suggest__popup-content')
                    .assertView('popup', '.mini-suggest__popup-content');
            });
        }
    });
});
