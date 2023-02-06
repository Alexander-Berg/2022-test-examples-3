'use strict';

const method = require('./backup_restore');

let core;
let mockService;

beforeEach(() => {
    mockService = jest.fn();
    core = {
        params: {
            method: 'full_hierarchy'
        },
        service: jest.fn().mockReturnValue(mockService)
    };
});

test('backup_restore', async () => {
    await method.call(core);
    expect(core.service).toHaveBeenCalledWith('barbet');
    expect(mockService).toHaveBeenCalledWith('/backup/restore', {}, { method: 'post', query: core.params });
});
