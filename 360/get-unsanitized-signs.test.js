'use strict';

const getUnsanitizedSigns = require('./get-unsanitized-signs');

let core;
const sanitizerService = jest.fn();

beforeEach(() => {
    core = {
        config: {
            REQUEST_ID: 'FAKE_REQUEST_ID'
        },
        service: () => sanitizerService
    };
});

test('ходит в санитайзер', async () => {
    const signs = [ {
        text: '__SANITIZED_SIGN__'
    } ];

    sanitizerService.mockResolvedValueOnce('__UNSANITIZED_SIGN__');

    const result = await getUnsanitizedSigns(core, signs);
    expect(result).toEqual([ {
        text: '__UNSANITIZED_SIGN__'
    } ]);

    expect(sanitizerService).toBeCalledTimes(1);
    expect(sanitizerService).toHaveBeenCalledWith(
        '/unproxy',
        {
            text: '__SANITIZED_SIGN__'
        },
        {
            query: { id: core.config.REQUEST_ID }
        }
    );
});

test('подписи сохранять не будем, если поход в санитайзер завершился с ошибкой', async () => {
    expect.assertions(2);

    const signs = [ {
        text: '__FIRST_SIGN__'
    }, {
        text: '__SECOND_BAD_SIGN__'
    } ];

    sanitizerService
        .mockResolvedValueOnce('__SANITIZED_FIRST_SIGN__')
        .mockRejectedValueOnce(new Error('Some sanitizer error'));

    try {
        await getUnsanitizedSigns(core, signs);
    } catch (e) {
        expect(e).toBeInstanceOf(Error);
        expect(e.message).toBe('Some sanitizer error');
    }
});
