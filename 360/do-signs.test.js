'use strict';

jest.unmock('@yandex-int/duffman');
jest.mock('./_helpers/get-unsanitized-signs');

const { CUSTOM_ERROR } = require('@yandex-int/duffman').errors;

const doSigns = require('./do-signs');
const getUnsanitizedSigns = require('./_helpers/get-unsanitized-signs');

let core;
const settingsService = jest.fn();

beforeEach(() => {
    core = {
        service: () => settingsService,
        hideParamInLog: jest.fn(),

        console: {
            error: jest.fn()
        }
    };
});

test('нормализует формат подписей', async () => {
    settingsService.mockResolvedValueOnce({});
    getUnsanitizedSigns.mockImplementationOnce(() => '__UNSANITIZED_SIGNS__');

    const result = await doSigns({
        signs: JSON.stringify([ {
            text: 'Some text',
            isDefault: true,
            emails: [],
            lang: 'en'
        } ])
    }, core);

    expect(getUnsanitizedSigns.mock.calls[0][1]).toEqual([ {
        text: 'Some text',
        is_default: true,
        associated_emails: [],
        lang: 'en'
    } ]);
    expect(result).toEqual({ data: 'ok' });
});

test('идёт в сервис с правильными параметрами', async () => {
    settingsService.mockResolvedValueOnce({});
    getUnsanitizedSigns.mockImplementationOnce((core, signs) => {
        signs.forEach((sign) => {
            sign.text = '__UNSANITIZED_SIGN__';
        });
        return signs;
    });

    await doSigns({
        signs: JSON.stringify([ {
            text: 'Some text',
            isDefault: true,
            emails: []
        } ])
    }, core);

    expect(settingsService).toHaveBeenCalledTimes(1);
    expect(settingsService).toHaveBeenCalledWith('/update_profile', {
        signature: '',
        signs: [ {
            text: '__UNSANITIZED_SIGN__',
            is_default: true,
            associated_emails: []
        } ]
    });
});

test('если врдуг произошла ошибка при походе в санитайзер – молчать не будет', async () => {
    expect.assertions(3);

    settingsService.mockResolvedValueOnce({});
    getUnsanitizedSigns.mockRejectedValueOnce(new Error('Some Error'));

    try {
        await doSigns({
            signs: JSON.stringify([ {
                text: 'Some text',
                isDefault: true,
                emails: []
            } ])
        }, core);
    } catch (e) {
        expect(e).toBeInstanceOf(CUSTOM_ERROR);
        expect(e.error).toEqual({ code: 'UNSANITIZE_SIGNS_ERROR' });
    }

    expect(settingsService).not.toHaveBeenCalled();
});

test('если врдуг произошла ошибка при походе в настройки – тоже не промолчит', async () => {
    expect.assertions(2);

    settingsService.mockRejectedValueOnce(new Error('Some Error'));
    getUnsanitizedSigns.mockResolvedValueOnce('__UNSANITIZED_SIGNS__');

    try {
        await doSigns({
            signs: JSON.stringify([ {
                text: 'Some text',
                isDefault: true,
                emails: []
            } ])
        }, core);
    } catch (e) {
        expect(e).toBeInstanceOf(CUSTOM_ERROR);
        expect(e.error).toEqual({ code: 'UPDATE_SINGS_REQUEST_ERROR' });
    }
});
