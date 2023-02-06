import { URL } from 'url';
import {
    parseURI,
    parseURIQuery,
    makeURIQuery,
    replaceTLD,
    makeURIString,
    replaceTLDByURI,
    parseURITLD,
    compareETLD1,
} from '../uri';

const QUERY_PARAMS_WITH_RUSSIAN = {
    'Русские буквы': 'с пробелами и ? =',
    'различными ?': 'спецсимволами',
};

function buildQuery(params: { [key: string]: string }) {
    const url = new URL('http://example.com');

    Object.entries(params).forEach(([key, value]) => url.searchParams.append(key, value));

    return url.search;
}

describe('URI', () => {
    describe('#parseURI', () => {
        it('https://john.doe@www.example.com:123/forum/questions/?tag=networking&order=newest#top', () => {
            expect(parseURI('https://john.doe@www.example.com:123/forum/questions/?tag=networking&order=newest#top')).toEqual({
                scheme: 'https',
                authority: 'john.doe@www.example.com:123',
                userinfo: 'john.doe',
                username: 'john.doe',
                host: 'www.example.com:123',
                hostname: 'www.example.com',
                port: '123',
                path: '/forum/questions/',
                query: '?tag=networking&order=newest',
                queryParams: {
                    tag: 'networking',
                    order: 'newest',
                },
                fragment: '#top',
            });
        });

        it('ldap://[2001:db8::7]/c=GB?objectClass?one', () => {
            expect(parseURI('ldap://[2001:db8::7]/c=GB?objectClass?one')).toEqual({
                scheme: 'ldap',
                authority: '[2001:db8::7]',
                host: '[2001:db8::7]',
                hostname: '[2001:db8::7]',
                path: '/c=GB',
                query: '?objectClass?one',
                queryParams: {
                    'objectClass?one': '',
                },
            });
        });

        it('mailto:John.Doe@example.com', () => {
            expect(parseURI('mailto:John.Doe@example.com')).toEqual({
                scheme: 'mailto',
                path: 'John.Doe@example.com',
            });
        });

        it('news:comp.infosystems.www.servers.unix', () => {
            expect(parseURI('news:comp.infosystems.www.servers.unix')).toEqual({
                scheme: 'news',
                path: 'comp.infosystems.www.servers.unix',
            });
        });

        it('tel:+1-816-555-1212', () => {
            expect(parseURI('tel:+1-816-555-1212')).toEqual({
                scheme: 'tel',
                path: '+1-816-555-1212',
            });
        });

        it('telnet://192.0.2.16:80/', () => {
            expect(parseURI('telnet://192.0.2.16:80/')).toEqual({
                scheme: 'telnet',
                authority: '192.0.2.16:80',
                host: '192.0.2.16:80',
                hostname: '192.0.2.16',
                port: '80',
                path: '/',
            });
        });

        it('urn:oasis:names:specification:docbook:dtd:xml:4.1.2', () => {
            expect(parseURI('urn:oasis:names:specification:docbook:dtd:xml:4.1.2')).toEqual({
                scheme: 'urn',
                path: 'oasis:names:specification:docbook:dtd:xml:4.1.2',
            });
        });

        it('http://username:password@example.com/', () => {
            expect(parseURI('http://username:password@example.com/')).toEqual({
                scheme: 'http',
                authority: 'username:password@example.com',
                userinfo: 'username:password',
                username: 'username',
                password: 'password',
                host: 'example.com',
                hostname: 'example.com',
                path: '/',
            });
        });

        it('div-action://set_state?state_id=2', () => {
            expect(parseURI('div-action://set_state?state_id=2')).toEqual({
                scheme: 'div-action',
                authority: 'set_state',
                host: 'set_state',
                hostname: 'set_state',
                path: '',
                query: '?state_id=2',
                queryParams: {
                    state_id: '2',
                },
            });
        });
    });

    describe('#parseURIQuery', () => {
        it('?tag=networking&order=newest', () => {
            expect(parseURIQuery('?tag=networking&order=newest')).toEqual({
                tag: 'networking',
                order: 'newest',
            });
        });

        it('?objectClass?one', () => {
            expect(parseURIQuery('?objectClass?one')).toEqual({
                'objectClass?one': '',
            });
        });

        it('?foo&&bar', () => {
            expect(parseURIQuery('?foo&&bar')).toEqual({
                foo: '',
                bar: '',
            });
        });

        it('?flags=config=testing;internal=1', () => {
            expect(parseURIQuery('?flags=config=testing;internal=1')).toEqual({
                flags: 'config=testing;internal=1',
            });
        });

        it('?test=1&test=2&test=3', () => {
            expect(parseURIQuery('?test=1&test=2&test=3')).toEqual({
                test: ['1', '2', '3'],
            });
        });

        it('Should return {} when query is empty', () => {
            expect(parseURIQuery('')).toEqual({});
            expect(parseURIQuery('?')).toEqual({});
        });

        it('Should decode queryParams', () => {
            expect(parseURIQuery(`${buildQuery(QUERY_PARAMS_WITH_RUSSIAN)}`)).toEqual(QUERY_PARAMS_WITH_RUSSIAN);
        });
    });

    describe('#makeURIQuery', () => {
        it('returns correct query string #1', () => {
            const params = {
                foo: 'bar',
                hello: 'world',
            };

            const expectedValue = '?foo=bar&hello=world';

            expect(makeURIQuery(params)).toEqual(expectedValue);
        });
        it('returns correct query string #2', () => {
            const params = {
                foo: 'bar',
                hello: '',
                world: undefined,
            };

            const expectedValue = '?foo=bar&hello&world';

            expect(makeURIQuery(params)).toEqual(expectedValue);
        });
        it('returns correct query string #3', () => {
            const params = {
                foo: 'бар',
            };

            const expectedValue = '?foo=%D0%B1%D0%B0%D1%80';

            expect(makeURIQuery(params)).toEqual(expectedValue);
        });
        it('returns correct query string #4', () => {
            const params = {
                foo: ['1', '2', 'бар'],
            };

            const expectedValue = '?foo=1&foo=2&foo=%D0%B1%D0%B0%D1%80';

            expect(makeURIQuery(params)).toEqual(expectedValue);
        });
        it('returns correct query string #5', () => {
            expect(makeURIQuery({})).toEqual('');
        });
    });

    describe('#replaceTLD', () => {
        it('example.com', () => {
            expect(replaceTLD('example.com', 'org')).toEqual('example.org');
        });
    });

    describe('#replaceTLDByURI', () => {
        it('https://example.com/#top', () => {
            expect(replaceTLDByURI('https://example.com/#top', 'https://some.org/#other'))
                .toEqual('https://some.com/#other');
        });
    });

    describe('#parseURITLD', () => {
        it('https://example.com/#top', () => {
            expect(parseURITLD('https://example.com/#top'))
                .toEqual('com');
        });
    });

    describe('#makeURIString', () => {
        const uris = [
            'https://john.doe:pass@www.example.com:123/forum/questions/?tag=networking&order=newest&l#top;some',
            'https://example.com/',
            'https://example.com',
            'https://example.com/#top',
            'ldap://[2001:db8::7]/c=GB?objectClass?one',
            'div-action://set_state?state_id=2',
        ];
        uris.map((uri) =>
            it(uri, () => {
                expect(makeURIString(parseURI(uri))).toEqual(uri);
            }));
    });

    describe('#compareETLD', () => {
        it('should be true if same hosts', () => {
            expect(compareETLD1({
                currentOrigin: 'https://yandex.ru',
                parentOrigin: 'https://yandex.ru',
                currentTld: 'ru',
            })).toBeTruthy();
        });

        it('should be true if same etlds + 1', () => {
            expect(compareETLD1({
                currentOrigin: 'https://yandex.ru',
                parentOrigin: 'https://some.yandex.ru',
                currentTld: 'ru',
            })).toBeTruthy();

            expect(compareETLD1({
                currentOrigin: 'https://some.yandex.ru',
                parentOrigin: 'https://yandex.ru',
                currentTld: 'ru',
            })).toBeTruthy();
        });

        it('should be false if tld etlds+1 is different', () => {
            expect(compareETLD1({
                currentOrigin: 'https://yandex.com',
                parentOrigin: 'https://kinopoist.ru',
                currentTld: 'com',
            })).toBeFalsy();

            expect(compareETLD1({
                currentOrigin: 'https://yandex.ru',
                parentOrigin: 'https://kinopoist.ru',
                currentTld: 'ru',
            })).toBeFalsy();
        });

        it('should be false if tld not matched', () => {
            expect(compareETLD1({
                currentOrigin: 'https://yandex.co.tr',
                parentOrigin: 'https://some.yandex.ru',
                currentTld: 'co.tr',
            })).toBeFalsy();

            expect(compareETLD1({
                currentOrigin: 'https://yandex.ru',
                parentOrigin: 'https://some.yandex.co.tr',
                currentTld: 'co.tr',
            })).toBeFalsy();

            expect(compareETLD1({
                currentOrigin: 'https://yandex.ru',
                parentOrigin: 'https://co.tr',
                currentTld: 'co.tr',
            })).toBeFalsy();
        });
    });
});
