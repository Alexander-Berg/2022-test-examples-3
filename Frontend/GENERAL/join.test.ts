import assert from 'assert';
import { join } from './join';

describe('join', () => {
    it('должен корректно возвращать строку', () => {
        const string = join(['part1', null, undefined, 'part2'], 'SEPARATOR');
        assert.strictEqual(string, 'part1SEPARATORpart2');
    });

    it('должен корректно возвращать строку со стандартным разделителем', () => {
        const string = join(['part1', null, undefined, 'part2']);
        assert.strictEqual(string, 'part1 part2');
    });
});
