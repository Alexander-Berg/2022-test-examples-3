'use strict';

const method = require('./archive_restore_status');

let core;
let mockService;

beforeEach(() => {
    mockService = jest.fn();
    core = {
        service: jest.fn().mockReturnValue(mockService)
    };
});

test('archive_restore_status', async () => {
    await method.call(core);
    expect(core.service).toHaveBeenCalledWith('meta');
    expect(mockService).toHaveBeenCalledWith('/v2/archive_status', {}, { method: 'get' });
});
