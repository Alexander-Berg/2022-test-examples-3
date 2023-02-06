'use strict';

const method = require('./backup_delete');

let core;
let mockService;

beforeEach(() => {
    mockService = jest.fn();
    core = {
        service: jest.fn().mockReturnValue(mockService)
    };
});

test('backup_delete', async () => {
    await method.call(core);
    expect(core.service).toHaveBeenCalledWith('barbet');
    expect(mockService).toHaveBeenCalledWith('/backup/delete', {}, { method: 'post' });
});
