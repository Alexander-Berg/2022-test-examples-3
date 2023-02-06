'use strict';

const s = require('serializr');
const optional = require('./optional.js');

describe('deserializer', () => {
    it('returns non-empty primitive', () => {
        const schema = s.createSimpleSchema({
            foo: optional()
        });

        const result = s.deserialize(schema, { foo: 'bar' });

        expect(result).toEqual({ foo: 'bar' });
    });

    it('skips empty primitive', () => {
        const schema = s.createSimpleSchema({
            foo: optional()
        });
        const result = s.deserialize(schema, { foo: '' });

        expect(result).toEqual({});
    });

    it('returns non-empty object', () => {
        const schema = s.createSimpleSchema({
            foo: optional(s.object(s.createSimpleSchema({ bar: true })))
        });

        const result = s.deserialize(schema, { foo: { bar: 'baz' } });

        expect(result).toEqual({ foo: { bar: 'baz' } });
    });

    it('skips non-empty object', () => {
        const schema = s.createSimpleSchema({
            foo: optional(s.object(s.createSimpleSchema({ bar: true })))
        });

        const result = s.deserialize(schema, { foo: null });

        expect(result).toEqual({});
    });
});
