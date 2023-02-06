'use strict';

jest.mock('../config/load-config.js');
jest.mock('../util/resolve-schema.js');

const mockLoadConfig = require('../config/load-config.js');
const mockResolveSchema = require('../util/resolve-schema.js');
const compileSchema = require('./compile-schema.js');

test('compiles schema', () => {
    mockResolveSchema.mockReturnValue({ type: 'string' });

    const result = compileSchema({ type: 'number' });

    expect(mockResolveSchema.mock.calls[0][0]).toEqual({ type: 'number' });
    expect(result.schema).toEqual({ type: 'string' });
});

test('loads and compiles schema', () => {
    mockLoadConfig.mockReturnValue({ type: 'number' });
    mockResolveSchema.mockReturnValue({ type: 'string' });

    const result = compileSchema('foo.json');

    expect(mockLoadConfig).toBeCalledWith('foo.json');
    expect(mockResolveSchema.mock.calls[0][0]).toEqual({ type: 'number' });
    expect(result.schema).toEqual({ type: 'string' });
    expect(result).toBe(compileSchema('foo.json'));
});

test('calls hideParamInLog', () => {
    mockResolveSchema.mockReturnValue({
        type: 'object',
        properties: {
            secret: {
                type: 'string',
                hideParamInLog: '***'
            }
        }
    });
    const validate = compileSchema({});
    const hideParamInLog = jest.fn();
    validate({ secret: '42' });
    validate({ secret: '42', hideParamInLog });
    expect(hideParamInLog).toBeCalledWith(expect.anything(), 'secret', '***');
});
