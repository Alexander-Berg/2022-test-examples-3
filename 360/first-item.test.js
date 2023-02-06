'use strict';

const s = require('serializr');
const firstItem = require('./first-item.js');

const schema = s.createSimpleSchema({
    alpha: firstItem(s.primitive())
});

describe('deserializer', () => {
    it('picks first item', () => {
        const result = s.deserialize(schema, { alpha: [ 1, 2 ] });

        expect(result).toEqual({ alpha: 1 });
    });

    it('throws for non-array objects', () => {
        expect(() => s.deserialize(schema, { alpha: 1 })).toThrow();
    });
});
