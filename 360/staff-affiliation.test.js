'use strict';

const StaffAffiliation = require('../../build/models/staff-api/staff-affiliation.js').default;
const staffAffiliation = new StaffAffiliation().action;

let core;
let mockService;

beforeEach(() => {
    mockService = jest.fn();
    core = {
        config: {},
        auth: {
            get: () => ({ login: 'TEST_LOGIN' })
        },
        service: () => mockService
    };
});

test('дергает сервис с правильными параметрами', async () => {
    core.config.IS_CORP = true;
    mockService.mockResolvedValueOnce({ official: { affiliation: 'TEST_AFFILIATION' } });

    await staffAffiliation({}, core);

    expect(mockService.mock.calls).toMatchSnapshot();
});

test('работает', async () => {
    core.config.IS_CORP = true;
    mockService.mockResolvedValueOnce({ official: { affiliation: 'TEST_AFFILIATION' } });

    const res = await staffAffiliation({}, core);

    expect(res).toEqual({ affiliation: 'TEST_AFFILIATION' });
});
