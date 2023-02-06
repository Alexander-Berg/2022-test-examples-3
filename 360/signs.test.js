'use strict';

jest.mock('./_helpers/sanitize-signs');
jest.mock('./_filters/filter-signs');

const signs = require('./signs');
const sanitizeSigns = require('./_helpers/sanitize-signs');
const filterSigns = require('./_filters/filter-signs');

let core;
const settingsService = jest.fn();

beforeEach(() => {
    core = {
        service: () => settingsService,

        yasm: {
            sum: jest.fn()
        },
        console: {
            log: jest.fn()
        }
    };

    sanitizeSigns.mockImplementationOnce(() => '__SANITIZED_SIGNS__');
    filterSigns.mockImplementationOnce(() => '__FILTERED_SIGNS__');
});

test('штатно возвращает подписи', async () => {
    const settingsServiceResponseMock = {
        settings: {
            profile: {
                signs: [
                    {
                        text: '<div>-- </div><div>First</div>'
                    },
                    {
                        text: '<div>-- </div><div>Second</div>'
                    }
                ],
                single_settings: {
                    signature: 'Some old school sign'
                }
            }
        }
    };

    settingsService.mockResolvedValueOnce(settingsServiceResponseMock);

    const result = await signs({}, core);
    expect(result).toEqual('__FILTERED_SIGNS__');
});

test('не забывает про старые подписи, если новых не оказалось', async () => {
    const settingsServiceResponseMock = {
        settings: {
            profile: {
                signs: [],
                single_settings: {
                    signature: '__OLD_SCHOOL_SIGN__'
                }
            }
        }
    };

    settingsService.mockResolvedValueOnce(settingsServiceResponseMock);

    const result = await signs({}, core);

    expect(sanitizeSigns).toBeCalledTimes(1);
    expect(sanitizeSigns).toBeCalledWith(core, [ {
        text: '__OLD_SCHOOL_SIGN__',
        is_sanitize: false,
        is_default: true
    } ]);
    expect(result).toEqual('__FILTERED_SIGNS__');
});

test('санитайзит подписи', async () => {
    const signsData = [
        {
            text: '<div>-- </div><div>First</div>'
        },
        {
            text: '<div>-- </div><div>Second</div>'
        }
    ];
    const settingsServiceResponseMock = {
        settings: {
            profile: {
                signs: signsData
            }
        }
    };

    settingsService.mockResolvedValueOnce(settingsServiceResponseMock);

    await signs({}, core);

    expect(sanitizeSigns).toBeCalledTimes(1);
    expect(sanitizeSigns).toBeCalledWith(core, signsData);
});

test('фильтрует засанитайженые подписи', async () => {
    const settingsServiceResponseMock = {
        settings: {
            profile: {
                signs: [
                    {
                        text: '<div>-- </div><div>First</div>'
                    },
                    {
                        text: '<div>-- </div><div>Second</div>'
                    }
                ]
            }
        }
    };

    settingsService.mockResolvedValueOnce(settingsServiceResponseMock);

    await signs({}, core);

    expect(filterSigns).toBeCalledTimes(1);
    expect(filterSigns).toBeCalledWith(core, '__SANITIZED_SIGNS__');
});

test('если вдруг подписи пришли не массивом', async () => {
    const settingsServiceResponseMock = {
        settings: {
            profile: {
                signs: '__INVALID_SIGNS__'
            }
        }
    };

    settingsService.mockResolvedValueOnce(settingsServiceResponseMock);

    await signs({}, core);

    expect(filterSigns).toBeCalledTimes(1);
    expect(filterSigns).toBeCalledWith(core, '__SANITIZED_SIGNS__');
});

test('должен вернуть fallback, если не удалось получить подписи', async () => {
    settingsService.mockRejectedValueOnce(new Error('Some Error'));

    const result = await signs({}, core);

    expect(result).toEqual({
        signs: [],
        FALLBACK: true
    });

    expect(core.yasm.sum).toBeCalledTimes(1);
    expect(core.console.log).toBeCalledTimes(1);
});

test('должен вернуть fallback, если ошибка без сообщения', async () => {
    settingsService.mockRejectedValueOnce({});

    const result = await signs({}, core);

    expect(result).toEqual({
        signs: [],
        FALLBACK: true
    });

    expect(core.yasm.sum).toBeCalledTimes(1);
    expect(core.console.log).toBeCalledTimes(1);
});
