'use strict';

jest.mock('../error/api-error.js');

const Ajv = require('ajv');
const ajv = new Ajv();
const MockApiError = require('../error/api-error.js');
let RequestValidator;

beforeEach(() => {
    jest.doMock('./compile-schema.js', () => ajv.compile.bind(ajv));
    RequestValidator = require('./request-validator.js');
});

test('fails unknown properties', () => {
    const validator = new RequestValidator({}, { schema: {} });
    const request = { headers: {}, params: {}, foo: {} };

    expect(validator.call.bind(validator, request)).toThrow(MockApiError);
    expect(validator.call.bind(validator, request)).toThrow('should NOT have additional properties: foo');
});

test('fails missing properties', () => {
    const validator = new RequestValidator(
        {},
        { schema: {} }
    );
    const request = {};

    expect(validator.call.bind(validator, request)).toThrow(MockApiError);
    expect(validator.call.bind(validator, request)).toThrow('should have required property \'.params\'');
});

describe('when validating headers', () => {
    it('passes valid headers', () => {
        const validator = new RequestValidator(
            {
                foo: {
                    schema: { type: 'number' }
                }
            },
            { schema: {} }
        );
        const request = { headers: { foo: 1 }, params: {} };

        expect(validator.call.bind(validator, request)).not.toThrow();
    });

    it('handles header schema location', () => {
        ajv.addSchema({ type: 'number' }, 'foo.json');
        const validator = new RequestValidator(
            {
                foo: {
                    schema: 'foo.json'
                }
            },
            { schema: {} }
        );
        const request = { headers: { foo: 1 }, params: {} };

        expect(validator.call.bind(validator, request)).not.toThrow();

        ajv.removeSchema('foo.json');
    });

    it('passes unknown headers', () => {
        const validator = new RequestValidator(
            {
                foo: {
                    schema: { type: 'number' }
                }
            },
            { schema: {} }
        );
        const request = { headers: { bar: 'baz' }, params: {} };

        expect(validator.call.bind(validator, request)).not.toThrow();
    });

    it('fails missing headers', () => {
        const validator = new RequestValidator(
            {
                foo: {
                    required: true,
                    schema: { type: 'number' }
                }
            },
            { schema: {} }
        );
        const request = { headers: {}, params: {} };

        expect(validator.call.bind(validator, request)).toThrow(MockApiError);
        expect(validator.call.bind(validator, request)).toThrow('headers should have required property \'foo\'');
    });
});

describe('when validating params', () => {
    it('passes valid params', () => {
        const validator = new RequestValidator(
            {},
            { schema: { type: 'number' } }
        );
        const request = { headers: {}, params: 1 };

        expect(validator.call.bind(validator, request)).not.toThrow();
    });

    it('fails invalid params', () => {
        const validator = new RequestValidator(
            {},
            { schema: { type: 'number' } }
        );
        const request = { headers: {}, params: 'foo' };

        expect(validator.call.bind(validator, request)).toThrow(MockApiError);
        expect(validator.call.bind(validator, request)).toThrow('params should be number');
    });
});
