'use strict';

const model = require('./country-by-ip.js');

const linguistics = {
    ablative: '',
    accusative: 'Россию',
    dative: 'России',
    directional: '',
    genitive: 'России',
    instrumental: 'Россией',
    locative: '',
    nominative: 'Россия',
    preposition: 'в',
    prepositional: 'России'
};
const emptyLingustics = {
    ablative: '',
    accusative: '',
    dative: '',
    directional: '',
    genitive: '',
    instrumental: '',
    locative: '',
    nominative: '',
    preposition: '',
    prepositional: ''
};
const geobaseLangs = [ 'be', 'en', 'kk', 'ru', 'tr', 'tt', 'uk', 'uz' ];

let core;
const service = jest.fn();

beforeEach(() => {
    core = {
        service: () => service,
        config: {
            locale: 'ru',
            LOCALES: [ 'FAKE_LOCALE' ],
            domainConfig: {
                tld: 'ru'
            }
        },
        yasm: {
            sum: jest.fn()
        },
        console: {
            error: jest.fn()
        }
    };
});

describe('возаращает пустую строку, ', () => {
    it('если не удалось определить регион по ip', async () => {
        service.mockResolvedValueOnce({ id: null });

        const result = await model({ ip: 'FAKE_IP' }, core);

        expect(service).toHaveBeenCalledTimes(1);
        expect(result).toEqual('');
    });

    it('если не удалось определить страну по региону', async () => {
        service
            .mockResolvedValueOnce({ id: 1 })
            .mockResolvedValueOnce(0);

        const result = await model({ ip: 'FAKE_IP' }, core);

        expect(service.mock.calls).toMatchSnapshot();
        expect(result).toEqual('');
    });
});

test('возращает результат', async () => {
    service
        .mockResolvedValueOnce({ id: 1 })
        .mockResolvedValueOnce(2)
        .mockResolvedValueOnce(geobaseLangs)
        .mockResolvedValueOnce(linguistics);

    const result = await model({ ip: 'FAKE_IP' }, core);

    expect(service.mock).toMatchSnapshot();
    expect(result).toEqual('Россия');
});

test('берет первый язык из config.LOCALES, если текущий язык не поддерживается геобазой', async () => {
    service
        .mockResolvedValueOnce({ id: 1 })
        .mockResolvedValueOnce(2)
        .mockResolvedValueOnce(geobaseLangs)
        .mockResolvedValueOnce(linguistics);

    core.config.locale = 'zz';

    const result = await model({ ip: 'FAKE_IP' }, core);

    expect(service.mock.calls).toMatchSnapshot();
    expect(result).toEqual('Россия');

});

test('делает запрос за фолбеком, если не удалось получить перевод для запрашиваемой локали', async () => {
    service
        .mockResolvedValueOnce({ id: 1 })
        .mockResolvedValueOnce(2)
        .mockResolvedValueOnce(geobaseLangs)
        .mockResolvedValueOnce(emptyLingustics)
        .mockResolvedValueOnce(linguistics);

    core.config.locale = 'uz';

    const result = await model({ ip: 'FAKE_IP' }, core);

    expect(service.mock.calls).toMatchSnapshot();
    expect(result).toEqual('Россия');
});

test('ошибка геобазы', async () => {
    service
        .mockResolvedValueOnce({ id: 1 })
        .mockResolvedValueOnce(2)
        .mockResolvedValueOnce(geobaseLangs)
        .mockRejectedValueOnce('Some Error');

    core.config.locale = 'uz';

    const result = await model({ ip: 'FAKE_IP' }, core);

    expect(result).toEqual('');
    expect(core.yasm.sum.mock.calls).toMatchSnapshot();
});

test('coverage', async () => {
    service.mockResolvedValueOnce();

    const result = await model({ ip: 'FAKE_IP' }, core);

    expect(result).toEqual('');
});
