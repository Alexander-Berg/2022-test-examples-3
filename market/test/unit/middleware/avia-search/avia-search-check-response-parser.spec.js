/* eslint-disable max-len */

const data = require('./avia-response.json');
const aviaSearchCheckResponseParser = require('./../../../../middleware/avia-search/avia-search-check/avia-search-check-response-parser');

describe('Avia search check response parser', () => {
    test('should parse correctly', () => {
        const aviaResponse = data['avia-response'];
        const expected = data['expected'];
        expected.minPrice.backward = undefined;
        const actual = aviaSearchCheckResponseParser(aviaResponse);

        expect(actual).toMatchObject(expected);
    });
});
