'use strict';

const method = require('./backup_get_settings');

let core;
let mockService;

beforeEach(() => {
    mockService = jest.fn();
    mockService = jest.fn().mockResolvedValue({ fids: [ '1', '12' ], tabs: [ 'relevant' ] });
    core = {
        service: jest.fn().mockReturnValue(mockService)
    };
});

test('backup_get_settings', async () => {
    const result = await method.call(core);
    expect(result).toEqual({ fids: [ '1', '12' ] });
    expect(core.service).toHaveBeenCalledWith('barbet');
    expect(mockService).toHaveBeenCalledWith('/backup/settings', {}, { method: 'get' });
});
