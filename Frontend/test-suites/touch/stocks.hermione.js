/* eslint-disable-next-line */
const mockImage = `"data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' fill='none' viewBox='0 0 144 144'%3e%3cpath fill='%23F33' d='M72 144c39.765 0 72-32.235 72-72 0-39.764-32.235-72-72-72C32.236 0 0 32.236 0 72c0 39.765 32.236 72 72 72z'/%3e%3cpath fill='%23fff' d='M73.807 27.844c-3.413 0-6.73.577-9.854 1.73a22.99 22.99 0 00-8.22 5.194c-2.355 2.307-4.23 5.24-5.575 8.703-1.346 3.461-2.02 7.548-2.02 12.116 0 6.54 1.203 11.78 3.606 15.579 2.307 3.702 5.336 6.587 9.085 8.558l-16.392 35.87H56.07l14.853-33.321h10.19v33.321h10.143v-87.75H73.807zm7.259 8.558v37.312h-7.643c-1.971 0-3.894-.288-5.672-.913a10.326 10.326 0 01-4.567-3.03c-1.298-1.442-2.355-3.317-3.076-5.577-.77-2.308-1.154-5.241-1.154-8.655 0-3.606.433-6.635 1.25-9.087.817-2.405 1.923-4.376 3.269-5.866 1.346-1.443 2.884-2.549 4.614-3.174a14.599 14.599 0 015.336-1.01h7.643z'/%3E%3C/svg%3E"`;

const runOnProject = require('../../commands/util/runOnProject');
const testWithThemes = require('../../commands/util/testWithThemes');

runOnProject(['serp', 'home'], 'touch', function() {
    describe('stocks', function() {
        for (const view of ['growth', 'fall', 'growth-with-chart']) {
            testWithThemes(`${view}`, function() {
                let mockStocks = require(`./mock-stocks/stocks-${view}.json`);
                mockStocks[3][5].img.url = mockImage;

                return this.browser
                    .click('.mini-suggest__input')
                    .yaMockSuggest(mockStocks[0], mockStocks)
                    .keys(mockStocks[0])
                    .waitForVisible('.mini-suggest__popup-content')
                    .assertView('popup', '.mini-suggest__popup-content');
            });

            testWithThemes(`${view}-without-icon`, function() {
                let mockStocks = JSON.parse(JSON.stringify(require(`./mock-stocks/stocks-${view}.json`)));
                mockStocks[3][5].img = null;

                return this.browser
                    .click('.mini-suggest__input')
                    .yaMockSuggest(mockStocks[0], mockStocks)
                    .keys(mockStocks[0])
                    .waitForVisible('.mini-suggest__popup-content')
                    .assertView('popup', '.mini-suggest__popup-content');
            });
        }
    });
});
