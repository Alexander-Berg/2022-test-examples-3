import { assert } from 'chai';

import { parseTime } from './Timer.utils';

describe('TimerUtils.parseTime', () => {
    it('should parse seconds', () => {
        assert.strictEqual(parseTime('30 секунд таймер'), 30);
    });

    it('should parse seconds (short variant)', () => {
        assert.strictEqual(parseTime('15 сек'), 15);
    });

    it('should parse minutes', () => {
        assert.strictEqual(parseTime('таймер на 3 минуты'), 3 * 60);
    });

    it('should parse minutes (short variant)', () => {
        assert.strictEqual(parseTime('10 мин таймер'), 10 * 60);
    });

    it('should parse hours', () => {
        assert.strictEqual(parseTime('таймер 2 часа'), 2 * 60 * 60);
    });
});
