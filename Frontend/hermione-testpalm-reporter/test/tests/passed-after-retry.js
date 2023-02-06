const {
    mockSessionScreenshot,
} = require('../../mocks');

describe('project-id-2: passed after retry', () => {
    let count = 0;

    beforeEach(() => {
        mockSessionScreenshot('chrome');
        mockSessionScreenshot('firefox');
    });

    it('awesome test', async() => {
        count++;

        if (count === 1) {
            const err = new Error('Test Error');
            err.testStack = 'error stacktrace';
            throw err;
        }
    });
});
