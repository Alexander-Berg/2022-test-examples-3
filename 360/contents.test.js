'use strict';

const s = require('serializr');
const contents = require('./contents.js');

describe('deserializer', () => {
    it('merges properties to context target', () => {
        const schema = s.createSimpleSchema({
            foo: contents(s.object(s.createSimpleSchema({ '*': true })))
        });

        const result = s.deserialize(schema, { foo: { bar: 1 } });

        expect(result).toEqual({ bar: 1 });
    });

    it('proxies errors', () => {
        jest.spyOn(s, 'object').mockReturnValue({
            serializer: Object,
            deserializer: jest.fn().mockImplementation((_, done) => done('oops'))
        });

        const schema = s.createSimpleSchema({
            foo: contents(s.object(s.createSimpleSchema({ '*': true })))
        });
        const callback = jest.fn();

        s.deserialize(schema, { foo: { bar: 1 } }, callback);

        expect(callback).toBeCalledWith('oops');

        s.object.mockRestore();
    });
});
