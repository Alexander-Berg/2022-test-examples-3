'use strict';

const method = require('./backup_update_settings');

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

test('backup_update_settings', async () => {
    await method.call(core);
    expect(core.service).toHaveBeenCalledWith('barbet');
    expect(mockService).toHaveBeenCalledWith('/backup/update_settings', {}, {
        method: 'post',
        query: { tabs: [ 'relevant' ], fids: [ '1', '12' ] }
    });
});
