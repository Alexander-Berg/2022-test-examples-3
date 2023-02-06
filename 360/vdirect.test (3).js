'use strict';

const mockVdirect = jest.fn();
jest.mock('vdirect', () => mockVdirect);

test('exports vdirect', () => {
    jest.resetModules();
    mockVdirect.mockReturnValue(1);

    const result = require('./vdirect.js');

    expect(result).toBe(1);
    expect(mockVdirect).toBeCalledWith('/home/wmi/.vdirectkeys');
});
