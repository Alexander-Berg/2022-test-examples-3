const {
    mockSessionScreenshot,
} = require('../../mocks');

describe('project-id-2: failed test', () => {
    beforeEach(() => {
        mockSessionScreenshot('chrome');
        mockSessionScreenshot('firefox');
        mockSessionScreenshot('chrome', 2);
        mockSessionScreenshot('firefox', 2);
    });

    it('awesome test', () => {
        const err = new Error('Test Error');
        err.testStack = 'error stacktrace';
        throw err;
    });
});
