import { assert } from 'chai';
import { splitThousands } from '../.';

describe('splitThousands', () => {
    it('default', () => {
        assert.isUndefined(splitThousands(undefined));
        assert.isUndefined(splitThousands(0));
        assert.equal(splitThousands(1), '1');
        assert.equal(splitThousands(10), '10');
        assert.equal(splitThousands(100), '100');
        assert.equal(splitThousands(1000), '1\u2009000');
        assert.equal(splitThousands(10000), '10\u2009000');
        assert.equal(splitThousands(100000), '100\u2009000');
        assert.equal(splitThousands(1000000), '1\u2009000\u2009000');
    });

    it('withZero', () => {
        assert.equal(splitThousands(undefined, true), '0');
    });
});
