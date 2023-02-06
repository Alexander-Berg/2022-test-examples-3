'use strict';

const method = require('./hidden_trash_turn_on');

let core;
let mockService;

beforeEach(() => {
    mockService = jest.fn();
    core = {
        service: jest.fn().mockReturnValue(mockService)
    };
});

test('hidden_trash_turn_on', async () => {
    await method.call(core);
    expect(core.service).toHaveBeenNthCalledWith(1, 'mops');
    expect(mockService).toHaveBeenNthCalledWith(1, '/folders/create_hidden_trash', {});
    expect(core.service).toHaveBeenNthCalledWith(2, 'settings');
    expect(mockService).toHaveBeenNthCalledWith(2, '/update_params', { hidden_trash_enabled: 'on' });
});
