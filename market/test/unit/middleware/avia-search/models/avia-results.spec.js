const { validateAviaResult, parseAviaResult } = require('./../../../../../middleware/avia-search/models/avia-result');

describe('Avia result', () => {
    const AVIA_RESULTS = require('./avia-results.json');
    const NOT_VALID_AVIA_RESULTS = require('./not-valid-avia-results.json');

    describe('validator', () => {
        test('should return undefined if avia result is valid', () => {
            AVIA_RESULTS &&
                Array.isArray(AVIA_RESULTS) &&
                AVIA_RESULTS.forEach((data) => {
                    const aviaResult = data.aviaResult;
                    const errors = validateAviaResult(aviaResult);

                    expect(errors).toBeUndefined();
                });
        });

        test('should return errors if avia result is not valid', () => {
            NOT_VALID_AVIA_RESULTS &&
                Array.isArray(NOT_VALID_AVIA_RESULTS) &&
                NOT_VALID_AVIA_RESULTS.forEach((data) => {
                    const aviaResult = data.aviaResult;
                    const errors = validateAviaResult(aviaResult);

                    expect(errors).toBeDefined();
                });
        });
    });

    describe('parser', () => {
        test('should parse correctly', () => {
            AVIA_RESULTS &&
                Array.isArray(AVIA_RESULTS) &&
                AVIA_RESULTS.forEach((data) => {
                    const aviaResult = data.aviaResult;
                    const expected = data.expected;
                    const actual = parseAviaResult(aviaResult);

                    expect(actual).toMatchObject(expected);
                });
        });

        test('should return null if avia result is not valid', () => {
            if (NOT_VALID_AVIA_RESULTS && Array.isArray(NOT_VALID_AVIA_RESULTS)) {
                NOT_VALID_AVIA_RESULTS.forEach((data) => {
                    const aviaResult = data.aviaResult;
                    const actual = parseAviaResult(aviaResult);

                    expect(actual).toBeNull();
                });
            }
        });
    });
});
