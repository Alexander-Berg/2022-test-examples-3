const generateRandomString = require('../../../src/shared/utils/generate-random-string');

describe('utils/generateRandomString', () => {
    it('should return a String', () => {
        assert(typeof generateRandomString() === 'string');
    });

    it('should return a non-empty String', () => {
        assert.isNotEmpty(generateRandomString());
    });
});
