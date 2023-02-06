const { extension } = require('../src');

let browser;

describe('Check setup', () => {
    beforeAll(async () => {
        browser = await extension.launch();
    });

    afterAll(async () => {
        if (browser) {
            await browser.close();
        }
    });

    test('should open browser', () => {
        expect(browser).toBeDefined();
    });
});
