import nock from 'nock';
import fetcher from './fetcher';
import {
    ErrorFetch,
    ErrorDSimple,
    ErrorAbcDetail,
    ErrorData,
} from './types';

/** конфигурация jest окружения для тестов
 * `abc-www/test/jest/setup.js`
 * там определяются дефолты BEM_LANG и X-Csrf-Token
 */
const LANG = { lang: process.env.BEM_LANG };
const OK = { data: 'OK' };

describe('fetcher', () => {
    afterAll(nock.restore);
    afterEach(nock.cleanAll);

    it('Should return fetcher', async() => {
        nock('https://abc.local.yandex-team.ru')
            .get('/')
            .query(LANG)
            .reply(200, OK);

        const res = await fetcher({
            protocol: 'https:',
            hostname: 'abc.local.yandex-team.ru',
            pathname: '/',
        });

        expect(res).toEqual(OK);
    });

    it('Should support custom protocol', async() => {
        nock('http://abc.local.yandex-team.ru')
            .get('/')
            .query(LANG)
            .reply(200, OK);

        const res = await fetcher({
            protocol: 'http:',
            hostname: 'abc.local.yandex-team.ru',
            pathname: '/',
        });

        expect(res).toEqual(OK);
    });

    it('Should support custom hostname', async() => {
        nock('https://foo.bar')
            .get('/')
            .query(LANG)
            .reply(200, OK, {
                'Access-Control-Allow-Origin': '*',
            });

        const res = await fetcher({
            protocol: 'https:',
            hostname: 'foo.bar',
            pathname: '/',
        });

        expect(res).toEqual(OK);
    });

    it('Should support custom pathname', async() => {
        nock('https://abc.local.yandex-team.ru')
            .get('/foo')
            .query(LANG)
            .reply(200, OK);

        const res = await fetcher({
            protocol: 'https:',
            hostname: 'abc.local.yandex-team.ru',
            pathname: '/foo',
        });

        expect(res).toEqual(OK);
    });

    it('Should support default hostname and protocol', async() => {
        const { location: { protocol, hostname } } = window;

        nock(`${protocol}//${hostname}`)
            .get('/')
            .query(LANG)
            .reply(200, OK);

        const res = await fetcher({
            pathname: '/',
        });

        expect(res).toEqual(OK);
    });

    it('Should support custom port', async() => {
        nock('https://abc.local.yandex-team.ru:9999')
            .get('/')
            .query(LANG)
            .reply(200, OK, {
                'Access-Control-Allow-Origin': '*',
            });

        const res = await fetcher({
            protocol: 'https:',
            hostname: 'abc.local.yandex-team.ru',
            port: '9999',
            pathname: '/',
        });

        expect(res).toEqual(OK);
    });

    it('Should support options.query', async() => {
        nock('https://abc.local.yandex-team.ru')
            .get('/')
            .query({
                ...LANG,
                foo: 'bar',
                bar: ['baz', 'zot'],
            })
            .reply(200, OK);

        const res = await fetcher({
            protocol: 'https:',
            hostname: 'abc.local.yandex-team.ru',
            pathname: '/',
            query: {
                ...LANG,
                foo: 'bar',
                bar: ['baz', 'zot'],
            },
        });

        expect(res).toEqual(OK);
    });

    it('Should support custom headers', async() => {
        nock('https://abc.local.yandex-team.ru', {
            reqheaders: {
                'X-Test': 'Test',
            },
        })
            .get('/')
            .query(LANG)
            .reply(200, OK);

        const res = await fetcher({
            protocol: 'https:',
            hostname: 'abc.local.yandex-team.ru',
            pathname: '/',
            headers: {
                'X-Test': 'Test',
            },
        });

        expect(res).toEqual(OK);
    });

    it('Should set X-Csrf-Token header', async() => {
        nock('https://abc.local.yandex-team.ru', {
            reqheaders: {
                'X-Csrf-Token': 'jest-test',
            },
        })
            .get('/')
            .query(LANG)
            .reply(200, OK);

        const res = await fetcher({
            protocol: 'https:',
            hostname: 'abc.local.yandex-team.ru',
            pathname: '/',
        });

        expect(res).toEqual(OK);
    });

    it('Should set X-Request-Id header', async() => {
        let firstId: string;

        nock('https://abc.local.yandex-team.ru', {
            reqheaders: {
                'X-Request-Id': requestId => {
                    firstId = requestId;
                    return Boolean(requestId);
                },
            },
        })
            .get('/')
            .query(LANG)
            .reply(200, OK);

        nock('https://abc.local.yandex-team.ru', {
            reqheaders: {
                'X-Request-Id': requestId => {
                    return Boolean(requestId) && requestId !== firstId;
                },
            },
        })
            .get('/')
            .query(LANG)
            .reply(200, OK);

        const res = await fetcher({
            protocol: 'https:',
            hostname: 'abc.local.yandex-team.ru',
            pathname: '/',
        });

        const res2 = await fetcher({
            protocol: 'https:',
            hostname: 'abc.local.yandex-team.ru',
            pathname: '/',
        });

        expect(res).toEqual(OK);
        expect(res2).toEqual(OK);
    });

    it('Should handle plain text errors', async() => {
        const SYSTEM_MESSAGE = 'request to https://abc.local.yandex-team.ru/?lang=ru failed, reason: ';
        const PLAIN = 'plain error';

        nock('https://abc.local.yandex-team.ru')
            .get('/')
            .query(LANG)
            .replyWithError(PLAIN);

        let resultError: ErrorFetch = {
            type: 'initial',
            message: 'no error',
        };

        try {
            await fetcher({
                protocol: 'https:',
                hostname: 'abc.local.yandex-team.ru',
                pathname: '/',
            });
        } catch (error) {
            resultError = error;
        }
        expect(resultError.type).toEqual('system');
        expect(resultError.message).toEqual(SYSTEM_MESSAGE + PLAIN);
    });

    it('Should handle status errors', async() => {
        const NOT_FOUND = 'Not Found';

        nock('https://abc.local.yandex-team.ru')
            .get('/')
            .query(LANG)
            .reply(404, NOT_FOUND);

        let resultError: ErrorDSimple = {
            status: 200,
            title: 'no error',
        };

        try {
            await fetcher({
                protocol: 'https:',
                hostname: 'abc.local.yandex-team.ru',
                pathname: '/',
            });
        } catch (error) {
            resultError = error;
        }
        expect(resultError.status).toEqual(404);
        expect(resultError.title).toEqual(NOT_FOUND);
    });

    it('Should handle detail errors', async() => {
        const DETAIL = 'not in the group';

        nock('https://abc.local.yandex-team.ru')
            .get('/')
            .query(LANG)
            .reply(403, { data: DETAIL });

        let resultError: ErrorAbcDetail = {
            data: {
                detail: 'no details',
            },
        };

        try {
            await fetcher({
                protocol: 'https:',
                hostname: 'abc.local.yandex-team.ru',
                pathname: '/',
            });
        } catch (error) {
            resultError = error;
        }
        expect(resultError.data.detail).toEqual(DETAIL);
    });

    it('Should handle title status errors', async() => {
        const TITLE = 'error title';

        nock('https://abc.local.yandex-team.ru')
            .get('/')
            .query(LANG)
            .reply(403, { title: TITLE });

        let resultError: ErrorDSimple = {
            status: 200,
            title: 'no error',
        };

        try {
            await fetcher({
                protocol: 'https:',
                hostname: 'abc.local.yandex-team.ru',
                pathname: '/',
            });
        } catch (error) {
            resultError = error;
        }

        expect(resultError.title).toEqual(TITLE);
        expect(resultError.status).toEqual(403);
    });

    it('Should handle details status errors', async() => {
        const TITLE = 'error title';
        const DESCRIPTION = 'error description';

        nock('https://abc.local.yandex-team.ru')
            .get('/')
            .query(LANG)
            .reply(403, { title: TITLE, description: DESCRIPTION });

        let resultError: ErrorDSimple = {
            status: 200,
            title: 'no error',
        };

        try {
            await fetcher({
                protocol: 'https:',
                hostname: 'abc.local.yandex-team.ru',
                pathname: '/',
            });
        } catch (error) {
            resultError = error;
        }

        expect(resultError.title).toEqual(TITLE);
        expect(resultError.description).toEqual(DESCRIPTION);
        expect(resultError.status).toEqual(403);
    });

    it('Should handle object errors', async() => {
        const ERROR = {
            ru: 'ru error',
            en: 'en error',
        };

        nock('https://abc.local.yandex-team.ru')
            .get('/')
            .query(LANG)
            .reply(403, {
                data: { error: ERROR },
            });

        let resultError: ErrorData = {
            data: {},
        };

        try {
            await fetcher({
                protocol: 'https:',
                hostname: 'abc.local.yandex-team.ru',
                pathname: '/',
            });
        } catch (error) {
            resultError = error;
        }

        expect(resultError.data).toEqual(ERROR);
    });
});
