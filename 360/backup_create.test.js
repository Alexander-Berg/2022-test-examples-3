'use strict';

const method = require('./backup_create');

let core;
let mockService;

beforeEach(() => {
    mockService = jest.fn().mockResolvedValue({ fids: [ '1', '12' ], tabs: [ 'relevant' ] });
    core = {
        params: {
            fids: [ '-10', '1', '12' ],
            withTabs: '1'
        },
        service: jest.fn().mockReturnValue(mockService)
    };
});

test('backup_create', async () => {
    await method.call(core);
    expect(core.service).toHaveBeenNthCalledWith(1, 'barbet');
    expect(mockService).toHaveBeenNthCalledWith(1, '/backup/update_settings', {}, {
        method: 'post',
        query: { tabs: [ 'relevant' ], fids: [ '1', '12' ] }
    });
    expect(core.service).toHaveBeenNthCalledWith(2, 'barbet');
    expect(mockService).toHaveBeenNthCalledWith(2, '/backup/create', {}, { method: 'post' });
});
