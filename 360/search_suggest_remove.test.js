'use strict';

const method = require('./search_suggest_remove');

let core;
let mockService;

beforeEach(() => {
    mockService = jest.fn();
    core = {
        params: {
            client: 'aphone',
            uuid: 'deadbeef42'
        },
        res: {
            status: jest.fn(() => ({
                send: jest.fn()
            })),
            set: jest.fn()
        },
        service: () => mockService
    };
});

test('ходит в сервис с правильными параметрами', async () => {
    mockService.mockResolvedValueOnce({});
    core.params.request = 'TEST REQUEST';

    await method(core);

    expect(mockService.mock.calls).toMatchSnapshot();
});
