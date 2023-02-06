import { expect } from 'chai';
import { TestFileParser } from '../../../src';

describe('tide / test-file-parser', () => {
    describe('getFullTitle', () => {
        it('should get a full title by array of strings and objects', () => {
            const parts = [{ feature: 'Feature', type: 'Type' }, 'Part-1', 'Part-2'];
            const expectedFullTitle = 'Feature / Type Part-1 Part-2';

            const actualFullTitle = TestFileParser.getFullTitle(parts);

            expect(actualFullTitle).deep.equal(expectedFullTitle);
        });
    });
});
