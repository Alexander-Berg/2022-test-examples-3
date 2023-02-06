'use strict';

jest.mock('../validator/compile-schema.js');

const mockValidate = jest.fn();
const mockCompileSchema = require('../validator/compile-schema.js');
const validateConfig = require('./validate-config.js');

test('compiles schema and validates config', () => {
    mockValidate.mockReturnValue(true);
    mockCompileSchema.mockReturnValue(mockValidate);

    expect(() => validateConfig('foo.json', 1)).not.toThrow();
    expect(mockCompileSchema).toBeCalledWith('foo.json');
    expect(mockValidate).toBeCalledWith(1);
});

test('compiles schema and invalidates config', () => {
    mockValidate.mockReturnValue(false);
    mockCompileSchema.mockReturnValue(mockValidate);

    expect(() => validateConfig('foo.json', 1)).toThrow();
    expect(mockCompileSchema).toBeCalledWith('foo.json');
    expect(mockValidate).toBeCalledWith(1);
});
