'use strict';

const BaseMethod = require('./BaseMethod.js');
const ApiError = require('../../../routes/helpers/api-error.js');
const { HTTP_ERROR } = require('@yandex-int/duffman').errors;

const httpError = (statusCode) => new HTTP_ERROR({ statusCode });

const paramsSchema = {
    type: 'object',
    properties: {
        foo: {
            type: 'string'
        }
    },
    required: [ 'foo' ],
    additionalProperties: false
};

describe('constructor', () => {
    it('default params', () => {
        const method = new BaseMethod();
        expect(method.paramsSchema).toBeNull();
        expect(method.responseSchema).toBeNull();
        expect(method.paramsToPick).toEqual([]);
    });

    it('with params', () => {
        const method = new BaseMethod({
            paramsSchema: { foo: 'bar' },
            responseSchema: { bar: 'baz' },
            paramsToPick: [ 'foo' ]
        });
        expect(method.paramsSchema).toEqual({ foo: 'bar' });
        expect(method.responseSchema).toEqual({ bar: 'baz' });
        expect(method.responseSchema).toEqual({ bar: 'baz' });
        expect(method.paramsToPick).toEqual([ 'foo' ]);
    });

    it('with schemaPath', () => {
        const method = new BaseMethod({ schemaPath: require.resolve('./BaseMethod.test.yaml') });
        expect(method.paramsSchema).toMatchSnapshot();
        expect(method.responseSchema).toMatchSnapshot();
        expect(method.paramsToPick).toEqual([ 'foo' ]);
    });
});

test('default filter', () => {
    const method = new BaseMethod();
    expect(method.filter({ foo: 'bar' })).toEqual({ foo: 'bar' });
});

describe('call', () => {
    const core = {
        params: { foo: 'bar', xyz: 'abc' }
    };

    it('should validate', async () => {
        const method = new BaseMethod({ paramsToPick: [ 'foo' ] });
        jest.spyOn(method, 'validate');

        await method.call(core);

        expect(method.validate).toBeCalledWith({ foo: 'bar' });
    });

    it('should throw with invalid params', async () => {
        expect.assertions(4);
        const method = new BaseMethod({ paramsToPick: [ 'xyz' ], paramsSchema });
        jest.spyOn(method, 'validate');

        try {
            await method.call(core);
        } catch (e) {
            expect(method.validate).toBeCalledWith({ xyz: 'abc' });
            expect(e).toBeInstanceOf(ApiError);
            expect(e.code).toEqual(400);
            expect(e.message).toEqual('invalid params schema');
        }
    });

    it('should run action', async () => {
        const method = new BaseMethod({ paramsToPick: [ 'foo' ], paramsSchema });
        jest.spyOn(method, 'action');

        await method.call(core);

        expect(method.action).toBeCalledWith({ foo: 'bar' }, core);
    });

    it('should apply filter', async () => {
        const method = new BaseMethod({ paramsToPick: [ 'foo' ], paramsSchema });
        jest.spyOn(method, 'action').mockResolvedValue({ result: { foo: 'bar' } });
        jest.spyOn(method, 'filter').mockReturnValue({ filtered: { foo: 'bar' } });

        const res = await method.call(core);

        expect(method.filter).toBeCalledWith({ result: { foo: 'bar' } }, core);
        expect(res).toEqual({ filtered: { foo: 'bar' } });
    });

    it('should catch errors', async () => {
        expect.assertions(3);
        const method = new BaseMethod({ paramsToPick: [ 'foo' ], paramsSchema });
        jest.spyOn(method, 'action').mockRejectedValue({ message: 'error' });

        try {
            await method.call(core);
        } catch (e) {
            expect(e).toBeInstanceOf(ApiError);
            expect(e.code).toEqual(400);
            expect(e.message).toEqual('error');
        }
    });

    it('should catch http errors', async () => {
        expect.assertions(3);
        const method = new BaseMethod({ paramsToPick: [ 'foo' ], paramsSchema });
        jest.spyOn(method, 'action').mockRejectedValue(httpError(505));

        try {
            await method.call(core);
        } catch (e) {
            expect(e).toBeInstanceOf(ApiError);
            expect(e.code).toEqual(505);
            expect(e.message).toEqual('http error');
        }
    });
});
