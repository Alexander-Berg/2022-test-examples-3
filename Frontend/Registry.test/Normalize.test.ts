import { describe, it } from 'mocha';
import { assert } from 'chai';
import { getNormalizedKeyFromSnippet, normalizeName } from '../normalize';

describe('Normalize', () => {
    it('should normalize name', () => {
        type Pair = [string, string];
        const testData: Pair[] = [
            ['a', 'A'],
            ['test', 'Test'],
            ['test123', 'Test123'],
            ['test_string', 'TestString'],
            ['TEST_STRING', 'TestString'],
            ['test-string', 'TestString'],
            ['test string', 'TestString'],
            ['test__string__', 'TestString'],
            ['test_three_parts', 'TestThreeParts'],
        ];

        for (const [name, expectedNormalizedName] of testData) {
            assert.equal(normalizeName(name), expectedNormalizedName);
        }
    });

    it('should normalize key from snippet', () => {
        const actual = getNormalizedKeyFromSnippet({ type: 'test_type-string', subtype: 'subtype_string' });
        const expected = { type: 'TestTypeString', subtype: 'SubtypeString' };

        assert.deepEqual(actual, expected);
    });
});
