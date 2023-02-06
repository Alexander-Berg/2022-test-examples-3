import { telURL, mailtoURL, mapsURL, addProtocol, setProtocol, parseQuery, extendUrlWithQueryParams, getPassportUrl, appendQueryToUrl } from '../url';

describe('url', () => {
    it('telURL', () => {
        expect(telURL('+7 111 22-33-44')).toBe('tel:+7111223344');
        expect(telURL('+7 (111) 22 33 44')).toBe('tel:+7111223344');
        expect(telURL('+7-111-22-33-44')).toBe('tel:+7111223344');
        expect(telURL('+7111223344')).toBe('tel:+7111223344');
    });

    it('mailtoURL', () => {
        expect(mailtoURL('user@example.com')).toBe('mailto:user@example.com');
    });

    it('mapsURL', () => {
        expect(mapsURL('foobar')).toBe('https://yandex.ru/maps?text=foobar');
    });

    it('addProtocol', () => {
        expect(addProtocol('foobar')).toBe('http://foobar');
        expect(addProtocol('foobar', true)).toBe('https://foobar');
        expect(addProtocol('foobar', false)).toBe('http://foobar');
        expect(addProtocol('', true)).toBe('');
        expect(addProtocol()).toBe('');
    });

    it('setProtocol', () => {
        expect(setProtocol('yandex.net', 'https')).toBe('https://yandex.net');
        expect(setProtocol('ftp://my.com:8080', 'http')).toBe('http://my.com:8080');
        expect(setProtocol('/settings/private/', 'browser')).toBe('browser:///settings/private/');
        expect(setProtocol('http://foo.bar/page?param=hf://d ', 'https')).toBe('https://foo.bar/page?param=hf://d');
        expect(setProtocol('', 'protocol')).toBe('');
    });

    describe('getPassportUrl', () => {
        let windowLocation: Location;
        const passportTlds = [
            'az',
            'by',
            'co.il',
            'com',
            'com.am',
            'com.ge',
            'com.tr',
            'ee',
            'eu',
            'fi',
            'fl',
            'kg',
            'kz',
            'lt',
            'lv',
            'md',
            'pl',
            'ru',
            'tj',
            'tm',
            'ua',
            'uz',
        ];

        beforeAll(() => {
            windowLocation = window.location;
            delete window.location;
        });

        afterAll(() => {
            window.location = windowLocation;
        });

        function testTld(tld: string) {
            window.location = {
                hostname: `yandex.${tld}`,
                href: `https://yandex.${tld}/promo/test?utm_source=qq`,
                search: '?utm_source=qq',
            } as Location;

            return `https://passport.yandex.${tld}/auth?retpath=https%3A%2F%2Fyandex.${tld}%2Fpromo%2Ftest%3Futm_source%3Dqq`;
        }

        for (let i = 0; i < passportTlds.length; i += 1) {
            const tld = passportTlds[i];

            it(`tld - ${tld}`, () => {
                const url = testTld(tld);

                expect(getPassportUrl()).toBe(url);
            });
        }

        it('tld - qwertyuio', () => {
            testTld('qwertyuio');

            expect(getPassportUrl()).toBe('https://passport.yandex.com/auth?retpath=https%3A%2F%2Fyandex.qwertyuio%2Fpromo%2Ftest%3Futm_source%3Dqq');
        });

        it('random domain', () => {
            window.location = {
                hostname: 'random.random',
                href: 'https://random.random/test?utm_source=qq',
            } as Location;

            expect(getPassportUrl()).toBe('https://passport.yandex.com/auth?retpath=https%3A%2F%2Frandom.random%2Ftest%3Futm_source%3Dqq');
        });

        it('with simple retpath', () => {
            testTld('ru');

            expect(getPassportUrl('https://test.ru')).toBe('https://passport.yandex.ru/auth?retpath=https%3A%2F%2Ftest.ru%2F%3Futm_source%3Dqq');
        });

        it('with difficult retpath', () => {
            testTld('ru');

            expect(getPassportUrl('https://test.ru/tt?qwe=rty')).toBe('https://passport.yandex.ru/auth?retpath=https%3A%2F%2Ftest.ru%2Ftt%3Fqwe%3Drty%26utm_source%3Dqq');
        });
    });
});

describe('Parse query', () => {
    it('should return object of search params', () => {
        expect(parseQuery('?param1=1&param2=2')).toEqual({
            param1: '1',
            param2: '2',
        });
    });
});

describe('extendUrlWithQueryParams', () => {
    it('should return original url if there are not search params in location', () => {
        expect(extendUrlWithQueryParams('https://yandex.ru')).toEqual('https://yandex.ru');
    });

    it('should return original url if url starts with #', () => {
        expect(extendUrlWithQueryParams('#section')).toEqual('#section');
    });

    it('should return original url if url starts with tel', () => {
        expect(extendUrlWithQueryParams('tel:900')).toEqual('tel:900');
    });

    it('should return original url if url starts with mailto', () => {
        expect(extendUrlWithQueryParams('mailto: example@mail.ru')).toEqual('mailto: example@mail.ru');
    });

    it('should return original url if url is empty', () => {
        expect(extendUrlWithQueryParams('')).toEqual('');
    });

    it('should return original search params if queryParams is empty', () => {
        expect(extendUrlWithQueryParams('https://yandex.ru?param1=1&param2=2')).toEqual(
            'https://yandex.ru?param1=1&param2=2'
        );
        expect(extendUrlWithQueryParams('https://yandex.ru?param1=1&param2=2', {})).toEqual(
            'https://yandex.ru?param1=1&param2=2'
        );
    });

    it('should return original url if filtered params are empty', () => {
        expect(extendUrlWithQueryParams('https://yandex.ru?param1=1', { notAllowed: '123' })).toEqual(
            'https://yandex.ru?param1=1'
        );
    });

    it('should provide allowed search params from query', () => {
        const url = extendUrlWithQueryParams('https://yandex.ru', { categoryId: '2', specializationId: '3' });

        expect(url).toEqual('https://yandex.ru/?categoryId=2&specializationId=3');
    });

    it('should not provide not allowed search params from query', () => {
        const url = extendUrlWithQueryParams('https://yandex.ru', { notAllowed: '1', utm_source: '2' });

        expect(url).toEqual('https://yandex.ru/?utm_source=2');
    });

    it('should not provide search params from location if there are the same in original search params', () => {
        expect(extendUrlWithQueryParams('https://yandex.ru/?utm_medium=1', { utm_medium: '2' })).toEqual(
            'https://yandex.ru/?utm_medium=1'
        );
    });

    it('should mix search params from query and original params', () => {
        const query = { utm_campaign: '1', utm_content: '2', from: '3' };
        const extendedURL = extendUrlWithQueryParams('https://yandex.ru?utm_term=4&param1=5', query);
        const expectedQuery = '?utm_term=4&param1=5&utm_campaign=1&utm_content=2&from=3';

        expect(extendedURL).toEqual(`https://yandex.ru/${expectedQuery}`);
    });

    it('should return url with param via query', () => {
        expect(extendUrlWithQueryParams('https://yandex.ru', { utm_medium: '1' })).toEqual(
            'https://yandex.ru/?utm_medium=1'
        );
    });
});

describe('appendQueryToUrl', () => {
    it('should append query to url without protocol', () => {
        expect(appendQueryToUrl('yandex.ru', { utm_medium: '1' })).toEqual('yandex.ru?utm_medium=1');
    });

    it('should append query to url without query', () => {
        expect(appendQueryToUrl('https://yandex.ru', { utm_medium: '1' })).toEqual('https://yandex.ru/?utm_medium=1');
    });

    it('should append query to url with query', () => {
        expect(appendQueryToUrl('https://yandex.ru?foo=bar', { utm_medium: '1' })).toEqual('https://yandex.ru/?foo=bar&utm_medium=1');
    });

    it('should not duplicate params', () => {
        expect(appendQueryToUrl('https://yandex.ru?utm_source=yandex', { utm_source: 'google' })).toEqual('https://yandex.ru/?utm_source=yandex');
    });

    it('should not append param without value', () => {
        expect(appendQueryToUrl('https://yandex.ru?foo=bar', { utm_source: undefined })).toEqual('https://yandex.ru/?foo=bar');
        expect(appendQueryToUrl('https://yandex.ru', { utm_source: undefined })).toEqual('https://yandex.ru/');
    });

    it('should return url with query with encoded spaces', () => {
        // Тест нужен, чтобы убедиться, что вместо пробелов подставляется %20, а не плюс
        expect(appendQueryToUrl('https://yandex.ru?text=А теперь пробелы', { utm_source: 'yandex' })).toEqual('https://yandex.ru/?text=%D0%90%20%D1%82%D0%B5%D0%BF%D0%B5%D1%80%D1%8C%20%D0%BF%D1%80%D0%BE%D0%B1%D0%B5%D0%BB%D1%8B&utm_source=yandex');
    });

    it('should return url with pluses in query', () => {
        expect(appendQueryToUrl('https://yandex.ru?text=А+теперь+плюсы', { utm_source: 'yandex' })).toEqual('https://yandex.ru/?text=%D0%90%2B%D1%82%D0%B5%D0%BF%D0%B5%D1%80%D1%8C%2B%D0%BF%D0%BB%D1%8E%D1%81%D1%8B&utm_source=yandex');
    });

    it('should set empty string as default value for query param', () => {
        expect(appendQueryToUrl('https://yandex.ru?foo', {})).toEqual('https://yandex.ru/?foo=');
    });
});
