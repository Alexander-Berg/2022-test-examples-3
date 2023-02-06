import { getHostname } from './getHostname';

let url: string;

describe('getHostname', () => {
    it('должен вернуть undefined, если не удалось распарсить URL', () => {
        expect(getHostname('абвгд_qwe_123')).toBeUndefined();

        expect(getHostname()).toBeUndefined();
    });

    describe('домен с ASCII символами', () => {
        beforeEach(() => {
            url = 'https://www.spb.aliexpress.ru/search?q=iphone';
        });

        it('должен вернуть домен без www', () => {
            expect(getHostname(url, { omitWww: true })).toEqual('spb.aliexpress.ru');
        });

        it('должен вернуть домен с www', () => {
            expect(getHostname(url)).toEqual('www.spb.aliexpress.ru');
        });
    });

    describe('домен с unicode символами', () => {
        beforeEach(() => {
            url = 'https://www.всевместе.рф/search?q=iphone';
        });

        it('должен вернуть домен с www', () => {
            expect(getHostname(url)).toEqual('www.xn--b1aaiba4b6aej.xn--p1ai');
        });

        it('должен вернуть hostname для домена в unicode', () => {
            expect(getHostname(url, { omitWww: true, punycode: true })).toEqual('всевместе.рф');
        });

        it('должен вернуть hostname для домена в unicode без punycode декодирования', () => {
            expect(getHostname(url, { omitWww: true })).toEqual('xn--b1aaiba4b6aej.xn--p1ai');
        });
    });
});
