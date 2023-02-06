'use strict';

const sanitizeSigns = require('./sanitize-signs');

let core;
const sanitizerService = jest.fn();

beforeEach(() => {
    core = {
        config: {
            REQUEST_ID: 'FAKE_REQUEST_ID'
        },
        service: () => sanitizerService,

        console: {
            error: jest.fn()
        },
        yasm: {
            sum: jest.fn()
        }
    };
});

test('не ходит в санитайзер без надобности', async () => {
    const signs = [ {
        text: '<div>-- </div><div>First</div>',
        is_sanitize: true
    }, {
        text: '<div>-- </div><div>Second</div>'
    } ];

    const result = await sanitizeSigns(core, signs);

    expect(result).toEqual(signs);
    expect(sanitizerService).toBeCalledTimes(0);
});

test('ходит в санитайзер', async () => {
    const signs = [ {
        text: 'Initial sign',
        is_sanitize: false
    } ];

    sanitizerService.mockResolvedValueOnce('Sanitized sign');

    const result = await sanitizeSigns(core, signs);
    expect(result).toEqual([ {
        text: 'Sanitized sign',
        is_sanitize: true
    } ]);

    expect(sanitizerService).toBeCalledTimes(1);
    expect(sanitizerService.mock.calls[0]).toEqual([
        '/pr_https',
        {
            text: 'Initial sign'
        },
        {
            query: { id: core.config.REQUEST_ID }
        }
    ]);
});

test('если не получилось засанитайзить подпись – исключаем их из выдачи', async () => {
    const signs = [ {
        text: 'First sign',
        is_sanitize: false
    }, {
        text: 'Bad sign',
        is_sanitize: false
    } ];

    sanitizerService
        .mockResolvedValueOnce('Sanitized sign')
        .mockRejectedValueOnce(new Error('Some sanitizer error'));

    const result = await sanitizeSigns(core, signs);
    expect(result).toEqual([ {
        text: 'Sanitized sign',
        is_sanitize: true
    } ]);

    expect(sanitizerService).toBeCalledTimes(2);

    expect(core.yasm.sum.mock.calls).toHaveLength(1);
    expect(core.console.error.mock.calls).toHaveLength(1);
});
