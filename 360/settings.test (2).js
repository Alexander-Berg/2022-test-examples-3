'use strict';

jest.mock('./_helpers/sanitize-signs', () => jest.fn());
jest.mock('./_filters/filter-all');

const settings = require('./settings.js');
const filterAll = require('./_filters/filter-all');

let core;
const settingsService = jest.fn();

beforeEach(() => {
    core = {
        req: {
            cookies: {}
        },
        service: () => settingsService
    };
});

test('вызывает filterAll с нужными параметрами', async () => {
    settingsService.mockResolvedValueOnce({
        settings: {
            parameters: {
                single_settings: { foo: 'bar' }
            },
            profile: {
                single_settings: { enable_pop: true }
            }
        }
    });

    await settings({}, core);
    expect(filterAll.mock.calls[0]).toEqual([
        core,
        {
            foo: 'bar',
            enable_pop: true,
            emails: [],
            reply_to: []
        },
        { include: [] }
    ]);
});

test('settings.internal прокидывает options.user в сервис', async () => {
    const user = {};

    settingsService.mockResolvedValueOnce({
        settings: {
            parameters: {
                single_settings: { foo: 'bar' }
            },
            profile: {
                single_settings: { enable_pop: true }
            }
        }
    });

    await settings.internal({}, core, user);

    expect(settingsService.mock.calls[0][2]).toEqual(expect.objectContaining({
        user
    }));
});
