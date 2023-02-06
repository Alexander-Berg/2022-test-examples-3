/**
 * Аналог sendKeys https://www.selenium.dev/documentation/webdriver/actions_api/keyboard/#send-keys
 *
 * @param {string} selector
 * @param {string} str
 */

module.exports = function sendKeys(selector, str) {
    return this.executeAsync(async(selector, str, done) => {
        const wait = timeout => new Promise(resolve => setTimeout(resolve, timeout));

        const element = window.document.querySelector(selector);

        if (!element) {
            throw Error(`Элемент ${selector} не найден`);
        }

        const chars = str.split('');

        for (const char of chars) {
            window.ReactTestUtils.Simulate.keyDown(element, { key: char });

            await wait(150);
        }

        done();
    }, selector, str);
};
