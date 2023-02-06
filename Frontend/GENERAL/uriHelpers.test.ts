import { parse, ParsedUrlQuery } from 'querystring';

import {
    langFromQuery,
    getEnvFlags,
    getUriParams,
    getAccessAllowOrigin,
    getHeader,
    getHeaders,
    mergeQuery,
    cleanupPublicationUrl,
    getCanonicalUrl,
    getUTMQuery,
    getXFrameOptions,
    getQueryValues,
    appendQueryValue,
    replaceQueryParam,
} from './uriHelpers';
import { IRequestData, RequestDataHeader } from '../../typings/apphost';

describe('uriHelpers', () => {
    describe('getHeader', () => {
        it('should return object with headers key and value', () => {
            const requestData = {
                headers: [['Host', 'hamster.yandex.ru'], ['X-Yandex-Internal-Request', '1'], ['Origin', 'https://hamster.yandex.ru']],
            } as IRequestData;

            expect(getHeader(requestData, 'host')).toEqual('hamster.yandex.ru');
            expect(getHeader(requestData, 'origin')).toEqual('https://hamster.yandex.ru');
        });
    });

    describe('getHeaders', () => {
        it('should return object with headers key and value', () => {
            const requestData = {
                headers: [['host', 'hamster.yandex.ru'], ['X-Yandex-Internal-Request', '1'], ['Origin', 'https://hamster.yandex.ru']],
            } as IRequestData;

            const result: Record<string, string> = {
                host: 'hamster.yandex.ru',
                origin: 'https://hamster.yandex.ru',
            };
            result['x-yandex-internal-request'] = '1';

            expect(getHeaders(requestData)).toEqual(result);
        });

        it('should return empty headers object', () => {
            const requestData = {
                headers: [] as RequestDataHeader[],
            } as IRequestData;

            expect(getHeaders(requestData)).toEqual({});
        });
    });

    describe('mergeQuery', () => {
        it('should return string with both queries', () => {
            expect(mergeQuery(
                'https://test.com?noNeed=no',
                [
                    'https://ololo.ru?test1=1',
                    'https://ololo.ru/?test2=2&test3=3',
                ]),
            ).toEqual('https://test.com/?test1=1&test2=2&test3=3');
        });
        it('should encode base64 params', () => {
            expect(mergeQuery(
                'https://test.com',
                [
                    'https://ololo.ru?b64_name=b2xvbG8=',
                ]),
            ).toEqual('https://test.com/?b64_name=ololo');
        });
    });

    describe('getAccessAllowOrigin', () => {
        it('should return hamster host', () => {
            const requestData = {
                headers: [['host', 'hamster.yandex.ru'], ['X-Yandex-Internal-Request', '1'], ['Origin', 'https://hamster.yandex.ru']],
                uri: '/jobs',
            } as IRequestData;

            const allowOriginHost = getAccessAllowOrigin(requestData);

            expect(allowOriginHost).toEqual('https://hamster.yandex.ru');
        });

        it('should return l7 host', () => {
            const requestData = {
                headers: [['host', 'l7test.yandex.ru'], ['X-Yandex-Internal-Request', '1']],
                uri: '/jobs',
            } as IRequestData;

            const allowOriginHost = getAccessAllowOrigin(requestData);

            expect(allowOriginHost).toEqual('https://l7test.yandex.ru');
        });

        it('should return yandex production host', () => {
            const requestData = {
                headers: [['host', 'yandex.ru']],
                uri: '/jobs',
            } as IRequestData;

            const allowOriginHost = getAccessAllowOrigin(requestData);

            expect(allowOriginHost).toEqual('https://yandex.ru');
        });
    });

    describe('getUriParams', () => {
        it('should parse uri with path, without query', () => {
            const requestData: IRequestData = {
                headers: [['host', 'yandex.ru']],
                uri: '/jobs',
                type: '',
                remote_ip: '',
                port: 80,
                method: 'GET',
                content: '',
            };

            const { query, uri } = getUriParams(requestData);

            expect(query).toEqual({});
            expect(uri.pathname).toEqual('/jobs');
        });

        it('should parse uri without path and query', () => {
            const requestData: IRequestData = {
                headers: [['host', 'yandex.ru']],
                uri: '/',
                type: '',
                remote_ip: '',
                port: 80,
                method: 'GET',
                content: '',
            };

            const { query, uri } = getUriParams(requestData);

            expect(query).toEqual({});
            expect(uri.pathname).toEqual('/');
        });

        it('should parse uri with path and query', () => {
            const requestData: IRequestData = {
                headers: [['host', 'yandex.ru']],
                uri: '/jobs/locations?cities=moscow&cities=peter&professions=analitic',
                type: '',
                remote_ip: '',
                port: 80,
                method: 'GET',
                content: '',
            };

            const { query, uri } = getUriParams(requestData);

            expect(query).toEqual({
                cities: ['moscow', 'peter'],
                professions: 'analitic',
            });
            expect(uri.pathname).toEqual('/jobs/locations');
        });
    });

    describe('getEnvFlags', () => {
        it('should detect production and no preview for yandex.ru', () => {
            const requestData = {
                headers: [['host', 'yandex.ru']],
                uri: '/jobs',
            } as IRequestData;

            expect(getEnvFlags(requestData)).toEqual(expect.objectContaining({
                needPreview: false,
                isTesting: false,
                isL7Test: false,
            }));
        });

        it('should detect production and no preview for yandex.ru even with internal header', () => {
            const requestData = {
                headers: [['host', 'yandex.ru'], ['X-Yandex-Internal-Request', '1']],
                uri: '/jobs',
            } as IRequestData;

            expect(getEnvFlags(requestData)).toEqual(expect.objectContaining({
                needPreview: false,
                isTesting: false,
                isL7Test: false,
            }));
        });

        it('should detect production and no preview if no internal header', () => {
            const requestData = {
                headers: [['host', 'hamster.yandex.ru']],
                uri: '/jobs',
            } as IRequestData;

            expect(getEnvFlags(requestData)).toEqual(expect.objectContaining({
                needPreview: false,
                isTesting: false,
                isL7Test: false,
            }));
        });

        for (const tld of ['ru', 'ua', 'by', 'com', 'com.tr']) {
            it(`should detect testing and preview for hamster.${tld} and internal header`, () => {
                const requestData = {
                    headers: [['host', `hamster.yandex.${tld}`], ['X-Yandex-Internal-Request', '1']],
                    uri: '/jobs',
                } as IRequestData;

                expect(getEnvFlags(requestData)).toEqual(expect.objectContaining({
                    needPreview: true,
                    isTesting: true,
                    isL7Test: false,
                }));
            });
        }
        for (const tld of ['ru', 'ua', 'by', 'com', 'com.tr']) {
            it('should not detect production only by internal header absence', () => {
                const requestData = {
                    headers: [['host', `hamster.yandex.${tld}`]],
                    uri: '/jobs',
                } as IRequestData;

                expect(getEnvFlags(requestData)).toEqual(expect.objectContaining({
                    needPreview: false,
                    isTesting: false,
                    isL7Test: false,
                }));
            });
        }

        it('should detect production and preview on l7test', () => {
            const requestData = {
                headers: [['host', 'l7test.yandex.ru'], ['X-Yandex-Internal-Request', '1']],
                uri: '/jobs',
            } as IRequestData;

            expect(getEnvFlags(requestData)).toEqual(expect.objectContaining({
                needPreview: true,
                isTesting: false,
                isL7Test: true,
            }));
        });

        it('should detect production and preview on l7test.*.com', () => {
            const requestData = {
                headers: [['host', 'l7test.yandex.com'], ['X-Yandex-Internal-Request', '1']],
                uri: '/jobs',
            } as IRequestData;

            expect(getEnvFlags(requestData)).toEqual(expect.objectContaining({
                needPreview: true,
                isTesting: false,
                isL7Test: true,
            }));
        });

        it('should not detect preview on l7test without internal header', () => {
            const requestData = {
                headers: [['host', 'l7test.yandex.ru']],
                uri: '/jobs',
            } as IRequestData;

            expect(getEnvFlags(requestData)).toEqual(expect.objectContaining({
                needPreview: false,
                isTesting: false,
                isL7Test: false,
            }));
        });

        it('should detect rus lang and ru zone for yandex.ru host', () => {
            const requestData = {
                headers: [['host', 'yandex.ru']],
                uri: '/jobs',
            } as IRequestData;

            expect(getEnvFlags(requestData)).toEqual(expect.objectContaining({
                zone: 'ru',
                lang: 'ru',
            }));
        });

        it('should detect eng lang for yandex.ru host with lang=en query', () => {
            const requestData = {
                headers: [['host', 'yandex.ru'], ['X-Yandex-Internal-Request', '1']],
                uri: '/jobs?lang=en',
            } as IRequestData;

            expect(getEnvFlags(requestData)).toEqual(expect.objectContaining({
                zone: 'ru',
                lang: 'en',
            }));
        });

        it('should detect ru lang for yandex.com host with lang=ru query', () => {
            const requestData = {
                headers: [['host', 'yandex.com'], ['X-Yandex-Internal-Request', '1']],
                uri: '/jobs?lang=ru',
            } as IRequestData;

            expect(getEnvFlags(requestData)).toEqual(expect.objectContaining({
                zone: 'com',
                lang: 'ru',
            }));
        });

        it('should detect ru lang for yandex.ru?lang=en if no internal header', () => {
            const requestData = {
                headers: [['host', 'yandex.ru']],
                uri: '/jobs?lang=en',
            } as IRequestData;

            expect(getEnvFlags(requestData)).toEqual(expect.objectContaining({
                zone: 'ru',
                lang: 'ru',
            }));
        });

        it('should detect en lang for yandex.com?lang=ru if no internal header', () => {
            const requestData = {
                headers: [['host', 'yandex.com']],
                uri: '/jobs?lang=ru',
            } as IRequestData;

            expect(getEnvFlags(requestData)).toEqual(expect.objectContaining({
                zone: 'com',
                lang: 'en',
            }));
        });

        it('should detect eng lang for yandex.com host', () => {
            const requestData = {
                headers: [['host', 'yandex.com']],
                uri: '/jobs',
            } as IRequestData;

            expect(getEnvFlags(requestData)).toEqual(expect.objectContaining({
                zone: 'com',
                lang: 'en',
            }));
        });

        it('should detect eng lang for yandex.com.tr host', () => {
            const requestData = {
                headers: [['host', 'yandex.com.tr']],
                uri: '/jobs',
            } as IRequestData;

            expect(getEnvFlags(requestData)).toEqual(expect.objectContaining({
                zone: 'com',
                lang: 'en',
            }));
        });
    });

    describe('langFromQuery', () => {
        it('should return en for lang en', () => {
            expect(langFromQuery({ lang: 'en' })).toEqual('en');
        });

        it('should return null for no lang', () => {
            expect(langFromQuery({})).toEqual(null);
        });

        it('should return null for unknown langs', () => {
            expect(langFromQuery({ lang: 'kk' })).toEqual(null);
            expect(langFromQuery({ lang: 'uk' })).toEqual(null);
            expect(langFromQuery({ lang: 'de' })).toEqual(null);
        });
    });

    describe('cleanupPublicationUrl', () => {
        it('should replace seo parts in vacancy urls', () => {
            expect(cleanupPublicationUrl('/jobs/vacancies/very-интересная-%D0%B4-странная-строка-123'))
                .toEqual('/jobs/vacancies/123');

            expect(cleanupPublicationUrl('/jobs/vacancies/very-интересная-%D0%B4-странная-строка-123/'))
                .toEqual('/jobs/vacancies/123');
        });

        it('should preserve as is non-survey tails in vacancy urls', () => {
            expect(cleanupPublicationUrl('/jobs/vacancies/123/not-surveys'))
                .toEqual('/jobs/vacancies/123/not-surveys');

            expect(cleanupPublicationUrl('/jobs/vacancies/123/not-surveys/'))
                .toEqual('/jobs/vacancies/123/not-surveys/');
        });
    });

    describe('getCanonicalUrl', () => {
        it('should preserve pathname of uri', () => {
            const requestData: IRequestData = {
                headers: [['host', 'yandex.ru']],
                uri: '/jobs',
                type: '',
                remote_ip: '',
                port: 80,
                method: 'GET',
                content: '',
            };

            expect(getCanonicalUrl(requestData))
                .toEqual('https://yandex.ru/jobs');
        });

        it('should not preserve query params', () => {
            const requestData: IRequestData = {
                headers: [['host', 'yandex.ru']],
                uri: '/?cities=moscow&cities=peter&professions=analitic',
                type: '',
                remote_ip: '',
                port: 80,
                method: 'GET',
                content: '',
            };

            expect(getCanonicalUrl(requestData))
                .toEqual('https://yandex.ru/');
        });

        it('should use yandex.ru if headers has not host', () => {
            const requestData: IRequestData = {
                headers: [],
                uri: '/',
                type: '',
                remote_ip: '',
                port: 80,
                method: 'GET',
                content: '',
            };

            expect(getCanonicalUrl(requestData))
                .toEqual('https://yandex.ru/');
        });
    });

    describe('getUTMQuery', () => {
        const QUERY_WITH_UTM =
            'utm_source=1&utm_medium=1&utm_medium=2&utm_campaign=123123&utm_term&utm_content=asdf&otherQuery=123';
        const FILTERED_QUERY = [
            'utm_source=1',
            'utm_medium=1',
            'utm_medium=2',
            'utm_campaign=123123',
            'utm_term',
            'utm_content=asdf',
        ];

        let queryStr: string;
        let parsedQuery: ParsedUrlQuery;

        beforeEach(() => {
            queryStr = QUERY_WITH_UTM;

            parsedQuery = parse(QUERY_WITH_UTM);
        });

        it('should extract utm params from query string', () => {
            const result = getUTMQuery(queryStr);

            for (const queryItem of FILTERED_QUERY) {
                expect(result.includes(queryItem)).toBeTruthy();
            }
        });

        it('should extract utm params from query string, which starts with question mark', () => {
            const result = getUTMQuery('?' + queryStr);

            for (const queryItem of FILTERED_QUERY) {
                expect(result.includes(queryItem)).toBeTruthy();
            }
        });

        it('should work without crashing with empty query string', () => {
            expect(getUTMQuery('')).toBe('');
        });

        it('should extract utm params from ParsedUrlQuery', () => {
            const result = getUTMQuery(parsedQuery);

            for (const queryItem of FILTERED_QUERY) {
                expect(result.includes(queryItem)).toBeTruthy();
            }
        });
    });

    describe('getXFrameOptions', () => {
        it('should return DENY for empty referer', () => {
            const requestData = {
                headers: [['', '']],
            } as IRequestData;

            const xFrameOptions = getXFrameOptions(requestData);

            expect(xFrameOptions).toEqual('DENY');
        });

        it('should return DENY for not allowed referer', () => {
            const requestData = {
                headers: [['referer', 'https://test.ru']],
            } as IRequestData;

            const xFrameOptions = getXFrameOptions(requestData);

            expect(xFrameOptions).toEqual('DENY');
        });

        it('should be empty for allowed referer https://webvisor.com/', () => {
            const requestData = {
                headers: [['referer', 'https://webvisor.com/']],
            } as IRequestData;

            const xFrameOptions = getXFrameOptions(requestData);

            expect(xFrameOptions).toBeNull();
        });

        it('should be empty for allowed referer https://metrika.yandex.ru/', () => {
            const requestData = {
                headers: [['referer', 'https://metrika.yandex.ru/']],
            } as IRequestData;

            const xFrameOptions = getXFrameOptions(requestData);

            expect(xFrameOptions).toBeNull();
        });
    });

    describe('getQueryValues', () => {
        it('should return [] as defaultValue', () => {
            const query = {};
            const values = getQueryValues(query, 'foo');

            expect(values).toEqual([]);
        });

        it('should return passed defaultValue', () => {
            const query = {};
            const values = getQueryValues(query, 'foo', ['default']);

            expect(values).toEqual(['default']);
        });

        it('should return existed value as array', () => {
            const query = { foo: 'bar' };
            const values = getQueryValues(query, 'foo');

            expect(values).toEqual(['bar']);
        });

        it('should return existed value', () => {
            const query = { foo: ['a', 'b'] };
            const values = getQueryValues(query, 'foo');

            expect(values).toEqual(['a', 'b']);
        });
    });

    describe('appendQueryValue', () => {
        it('should append if no key', () => {
            const query: ParsedUrlQuery = {};
            appendQueryValue(query, 'foo', 'bar');

            expect(query.foo).toEqual(['bar']);
        });

        it('should append if value is string', () => {
            const query: ParsedUrlQuery = { foo: ['baz'] };
            appendQueryValue(query, 'foo', 'bar');

            expect(query.foo).toEqual(['baz', 'bar']);
        });

        it('should append if value is array', () => {
            const query: ParsedUrlQuery = { foo: ['baz', 'fizz'] };
            appendQueryValue(query, 'foo', 'bar');

            expect(query.foo).toEqual(['baz', 'fizz', 'bar']);
        });
    });

    describe('replaceQueryParam', () => {
        it('should replace existed key: value', () => {
            const query: ParsedUrlQuery = {
                foo: 'bar',
            };
            replaceQueryParam(query, { key: 'foo', value: 'bar' }, { key: 'baz', value: 'fizz' });

            expect(query.foo).toBeUndefined();
            expect(query.baz).toEqual(['fizz']);
        });

        it('should not do anything if from.key does\'n exist', () => {
            const query: ParsedUrlQuery = {
                foo: 'bar',
            };
            replaceQueryParam(query, { key: 'foo2', value: 'bar2' }, { key: 'baz', value: 'fizz' });

            expect(query.foo).toEqual('bar');
            expect(query.baz).toBeUndefined();
        });

        it('should append value to existed key', () => {
            const query: ParsedUrlQuery = {
                foo: 'bar',
                foo2: 'bar2',
            };
            replaceQueryParam(query, { key: 'foo', value: 'bar' }, { key: 'foo2', value: 'fizz' });

            expect(query.foo).toBeUndefined();
            expect(query.foo2).toEqual(['bar2', 'fizz']);
        });
    });
});
