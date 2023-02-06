'use strict';

const model = require('./staff-is-external');

let core;
let staffService;

beforeEach(() => {
    staffService = jest.fn();

    core = {
        config: {
            IS_CORP: true
        },
        auth: {
            get: () => ({ login: 'FAKE_LOGIN' })
        },
        service: () => staffService,

        console: {
            error: jest.fn()
        }
    };
});

test('сразу возвращает результат для не-корпов', async () => {
    core.config.IS_CORP = false;

    const res = await model({}, core);

    expect(staffService).not.toBeCalled();
    expect(res).toBeFalse();
});

test('идёт в сервис с правильными параметрами', async () => {
    staffService.mockResolvedValueOnce({
        official: { is_robot: false, affiliation: 'yandex' }
    });

    await model({}, core);

    expect(staffService.mock.calls).toMatchSnapshot();
});

test('работает', async () => {
    staffService.mockResolvedValue({
        official: { is_robot: false, affiliation: 'external' }
    });

    const res = await model({}, core);

    expect(res).toBeTrue();
});

test('возвращает результат при проблемах c сервисом', async () => {
    staffService.mockRejectedValueOnce(new Error('Some Error'));

    const res = await model({}, core);

    expect(res).toBeTrue();
});
