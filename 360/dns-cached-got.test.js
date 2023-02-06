'use strict';

const originalGot = require('got');

const mockLookup = jest.fn();
jest.mock('dns', () => ({
    lookup: (hostname, _, callback) => {
        const [ error, address, family ] = mockLookup(hostname);
        if (Array.isArray(address)) {
            callback(error, address);
        } else {
            callback(error, [ { address, family } ]);
        }
    }
}));

const mockGot = jest.fn();
jest.mock('got', () => async (url, options) => mockGot(url, options));

const got = require('./dns-cached-got.js');

beforeEach(() => {
    mockLookup.mockReturnValue([ null, '1.1.1.9', 4 ]);
    mockGot.mockResolvedValue({ status: 'ok' });
    jest.useFakeTimers();
});

afterEach(() => {
    mockLookup.mockReset();
    mockGot.mockReset();
    got.clearCache();
    jest.useRealTimers();
});

describe('без опции dnsCache должен работать как обычный got', () => {
    it('для домена', async () => {
        const res = await got('http://a.ru', { param1: 100, param2: 1 });
        expect(res).toEqual({ status: 'ok' });
        expect(mockLookup).not.toHaveBeenCalled();
        expect(mockGot).toHaveBeenCalledWith('http://a.ru', { param1: 100, param2: 1 });
    });

    it('для ip4', () => {
        got('http://1.1.1.9:1000');
        expect(mockLookup).not.toHaveBeenCalled();
        expect(mockGot).toHaveBeenCalledWith('http://1.1.1.9:1000', {});
    });

    it('для ip6', () => {
        got('http://[::]:1001/u?a');
        expect(mockLookup).not.toHaveBeenCalled();
        expect(mockGot).toHaveBeenCalledWith('http://[::]:1001/u?a', {});
    });
});

describe('с опцией dnsCache', () => {
    it('должен работать для ip4 как обычный got', () => {
        got('http://127.0.0.1:10/a?b=10', { dnsCache: true });
        expect(mockLookup).not.toHaveBeenCalled();
        expect(mockGot).toHaveBeenCalledWith('http://127.0.0.1:10/a?b=10', { dnsCache: true });
    });

    it('должен работать для ip6 как обычный got', () => {
        got('http://[1:1::1]:10/a?b=10', { dnsCache: true });
        expect(mockLookup).not.toHaveBeenCalled();
        expect(mockGot).toHaveBeenCalledWith('http://[1:1::1]:10/a?b=10', { dnsCache: true });
    });

    it('должен работать и резолвить ip4', async () => {
        await got('http://a.ru:99/u/u?i', { dnsCache: true });

        expect(mockLookup).toHaveBeenCalledWith('a.ru');
        expect(mockGot).toHaveBeenCalledWith(
            'http://1.1.1.9:99/u/u?i',
            { dnsCache: true, headers: { host: 'a.ru' } }
        );
    });

    it('должен работать и резолвить ip6', async () => {
        mockLookup.mockReturnValue([ null, '1:1::2', 6 ]);

        await got('http://a.ru:100/a?b', { dnsCache: true });

        expect(mockLookup).toHaveBeenCalledWith('a.ru');
        expect(mockGot).toHaveBeenCalledWith(
            'http://[1:1::2]:100/a?b',
            { dnsCache: true, headers: { host: 'a.ru' } }
        );
    });

    it('должен обрабатываеть одновременные обращения к dns lookup', async () => {
        await Promise.all([
            got('http://a.ru:100/a?b', { dnsCache: true }),
            got('https://a.ru', { dnsCache: true })
        ]);

        expect(mockLookup).toHaveBeenCalledWith('a.ru');
        expect(mockLookup).toHaveBeenCalledTimes(1);
    });

    it('должен кешировать обращения к dns lookup', async () => {
        mockLookup.mockReturnValueOnce([ null, '1:1::2', 6 ]);

        await Promise.all([
            got('http://a.ru/', { dnsCache: true }),
            got('https://b.ru/', { dnsCache: true })
        ]);

        expect(mockLookup).toHaveBeenCalledWith('a.ru');
        expect(mockLookup).toHaveBeenCalledWith('b.ru');

        mockLookup.mockClear();
        mockGot.mockClear();

        await Promise.all([
            got('http://a.ru/', { dnsCache: true }),
            got('https://b.ru/', { dnsCache: true })
        ]);

        expect(mockLookup).not.toHaveBeenCalled();
        expect(mockGot).toHaveBeenCalledWith(
            'http://[1:1::2]/',
            { dnsCache: true, headers: { host: 'a.ru' } }
        );
        expect(mockGot).toHaveBeenCalledWith(
            'https://1.1.1.9/',
            { dnsCache: true, headers: { host: 'b.ru' } }
        );
    });

    it('должен сбрасывать кеш после ошибки got', async () => {
        const error = new Error();
        error.code = 'ANY_ERROR_CODE';
        mockGot.mockRejectedValueOnce(error);

        try {
            await got('http://a.ru/', { dnsCache: true });
        } catch (err) {
            expect(err).toBe(error);
        }

        await got('http://a.ru/', { dnsCache: true });

        expect(mockLookup).toHaveBeenCalledTimes(2);
        expect(mockGot).toHaveBeenCalledWith(
            'http://1.1.1.9/',
            { dnsCache: true, headers: { host: 'a.ru' } }
        );
    });

    it('должен передавать ошибки от got', async () => {
        expect.hasAssertions();
        mockGot.mockRejectedValueOnce('AAA');

        try {
            await got('http://a.ru/', { dnsCache: true });
        } catch (err) {
            expect(err).toBe('AAA');
        }
    });

    it('должен передавать данные от got', async () => {
        const res = await got('http://a.ru/', { dnsCache: true });

        expect(res).toEqual({ status: 'ok' });
    });

    it('должен корректно поддерживать ttl', async () => {
        await got('http://a.ru/', { dnsCache: 10 });
        await got('http://a.ru/', { dnsCache: true });

        expect(mockLookup).toHaveBeenCalledTimes(1);

        jest.advanceTimersByTime(15000);

        await got('http://a.ru/', { dnsCache: true });

        expect(mockLookup).toHaveBeenCalledTimes(2);
    });

    it('должен переходить по оригинальной урле при ошибке dns lookup', async () => {
        mockLookup.mockReturnValue([ 'ERROR' ]);

        await got('http://a.ru:99/u/u?i', { dnsCache: true });

        expect(mockLookup).toHaveBeenCalledWith('a.ru');
        expect(mockGot).toHaveBeenCalledWith('http://a.ru:99/u/u?i', { dnsCache: true });
    });

    it('должен отдавать приоритет ip6', async () => {
        mockLookup.mockReturnValue([ null, [ { address: '1.1.1.9', family: 4 }, { address: '::1', family: 6 } ] ]);

        await got('http://a.ru/', { dnsCache: true });

        expect(mockGot).toHaveBeenCalledWith(
            'http://[::1]/',
            { dnsCache: true, headers: { host: 'a.ru' } }
        );
    });

    it('должны быть все конструкторы ошибок got', () => {
        expect(got.RequestError).toBe(originalGot.RequestError);
        expect(got.ReadError).toBe(originalGot.ReadError);
        expect(got.ParseError).toBe(originalGot.ParseError);
        expect(got.HTTPError).toBe(originalGot.HTTPError);
        expect(got.MaxRedirectsError).toBe(originalGot.MaxRedirectsError);
    });
});

