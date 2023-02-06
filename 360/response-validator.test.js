'use strict';

jest.mock('../error/api-error.js');
jest.mock('./compile-schema.js');

const MockApiError = require('../error/api-error.js');
const mockCompileSchema = require('./compile-schema.js');
const ResponseValidator = require('./response-validator.js');

test('passes valid response', () => {
    const mockValidate = jest.fn().mockReturnValue(true);
    mockCompileSchema.mockReturnValue(mockValidate);

    const validator = new ResponseValidator('foo.json');

    expect(validator.call.bind(validator, 1)).not.toThrow();
    expect(mockCompileSchema).toBeCalledWith('foo.json');
    expect(mockValidate).toBeCalledWith(1);
});

test('fails invalid response', () => {
    const mockValidate = jest.fn().mockReturnValue(false);
    mockValidate.errors = [ { dataPath: '.bar', message: 'baz' } ];
    mockCompileSchema.mockReturnValue(mockValidate);

    const validator = new ResponseValidator('foo.json');

    expect(validator.call.bind(validator, 1)).toThrow(MockApiError);
    expect(validator.call.bind(validator, 1)).toThrow('bar baz');
    expect(mockCompileSchema).toBeCalledWith('foo.json');
    expect(mockValidate).toBeCalledWith(1);
});
