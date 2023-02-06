const {
    mockSessionScreenshot,
} = require('../../mocks');

describe('project-id-2: failed firefox test', () => {
    beforeEach(() => {
        mockSessionScreenshot('chrome');
        mockSessionScreenshot('chrome', 2);
    });

    it('awesome test', function() {
        if (this.currentTest.browserId === 'chrome') {
            const err = new Error('Test Error');
            err.testStack = 'error stacktrace';
            throw err;
        }
    });
});
