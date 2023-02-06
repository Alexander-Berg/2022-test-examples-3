import { assert } from 'chai';
import { smartCutWithHighlight } from '../.';

describe('splitThousands', () => {
    it('default', () => {
        assert.equal(smartCutWithHighlight('тип: смартфон, линейка: \u0007[iPhone\u0007] \u0007[11\u0007], диагональ экрана: 6  ', 0), 'тип:...');
        assert.equal(smartCutWithHighlight('тип: смартфон, линейка: \u0007[iPhone\u0007] \u0007[11\u0007], диагональ экрана: 6  ', 30), 'тип: смартфон, линейка: \u0007[iPhone\u0007]...');
        assert.equal(smartCutWithHighlight('тип: смартфон, линейка: \u0007[iPhone\u0007] \u0007[11\u0007], диагональ экрана: 6  ', 40), 'тип: смартфон, линейка: \u0007[iPhone\u0007] \u0007[11\u0007],...');
        assert.equal(smartCutWithHighlight('тип: смартфон, линейка: \u0007[iPhone\u0007] \u0007[11\u0007], диагональ экрана: 6  ', 60), 'тип: смартфон, линейка: \u0007[iPhone\u0007] \u0007[11\u0007], диагональ экрана: 6');
    });
});
