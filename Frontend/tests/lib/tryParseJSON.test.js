const assert = require('assert');
const tryParseJSON = require('lib/tryParseJSON');

describe('tryParseJSON method', () => {
    it('should return null on invalid JSON', () => {
        const actual = tryParseJSON('{ght');

        assert.strictEqual(actual, null);
    });

    it('should return null on other types', () => {
        const actual = tryParseJSON(undefined);

        assert.strictEqual(actual, null);
    });

    it('should parse valid JSON', () => {
        const jsonData = {
            a: 'b',
            c: 'd',
            e: {
                f: 'g',
            },
        };
        const actual = tryParseJSON(JSON.stringify(jsonData));

        assert.deepStrictEqual(actual, jsonData);
    });

    it('should parse valid JSON by string', () => {
        const jsonData = {
            a: 'b',
            c: 'd',
            e: {
                f: 'g',
            },
        };
        const actual = tryParseJSON('{"a":"b","c":"d","e":{"f":"g"}}');

        assert.deepStrictEqual(actual, jsonData);
    });
});
