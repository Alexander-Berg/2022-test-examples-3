import { parseArguments } from './parse-arguments';

describe('Logger > parseArguments', () => {
    test('Should detect error', () => {
        const err = new Error('error-foo');

        expect(parseArguments([err])).toEqual({
            err,
            message: ['error-foo'],
        });
        expect(parseArguments([err, 'message'])).toEqual({
            err,
            message: ['message'],
        });
        expect(parseArguments([{ err }])).toEqual({
            err,
            fields: {},
            message: ['error-foo'],
        });
        expect(parseArguments([{ err }, 'foo', { bar: 'baz' }])).toEqual({
            err,
            fields: {},
            message: ['foo', { bar: 'baz' }],
        });
    });

    test('Should parse message and fields', () => {
        expect(parseArguments([{ foo: 'bar' }])).toEqual({
            fields: { foo: 'bar' },
            message: [],
        });
        expect(parseArguments(['foo', { bar: 'baz' }])).toEqual({
            message: ['foo', { bar: 'baz' }],
        });
        expect(parseArguments([{ foo: 'bar' }, 'message %s', 'baz'])).toEqual({
            fields: { foo: 'bar' },
            message: ['message %s', 'baz'],
        });
    });
});
