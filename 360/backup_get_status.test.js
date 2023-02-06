'use strict';

const method = require('./backup_get_status');

let core;
let mockService;

beforeEach(() => {
    mockService = jest.fn();
    core = {
        service: jest.fn().mockReturnValue(mockService)
    };
});

test('backup_get_status', async () => {
    await method.call(core);
    expect(core.service).toHaveBeenCalledWith('barbet');
    expect(mockService).toHaveBeenCalledWith('/backup/status', {}, { method: 'get' });
});
