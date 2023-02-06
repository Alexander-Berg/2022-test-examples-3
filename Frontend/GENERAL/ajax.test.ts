import nock from 'nock';

import { getJsonRes } from './ajax';

describe('fetcher', () => {
    it('Should return response', async() => {
        nock('https://magiclinks.local.yandex-team.ru')
            .get('/')
            .reply(200, { data: 'OK' });

        const data = await getJsonRes({
            protocol: 'https:',
            hostname: 'magiclinks.local.yandex-team.ru',
            pathname: '/',
        });

        expect(data).toEqual({ data: 'OK' });
    });

    it('Should support custom protocol', async() => {
        nock('http://magiclinks.local.yandex-team.ru')
            .get('/')
            .reply(200, { data: 'OK' }, {
                'Access-Control-Allow-Origin': '*',
            });

        const data = await getJsonRes({
            protocol: 'http:',
            hostname: 'magiclinks.local.yandex-team.ru',
            pathname: '/',
            mode: 'cors',
        });

        expect(data).toEqual({ data: 'OK' });
    });

    it('Should support custom hostname', async() => {
        nock('https://foo.bar')
            .get('/')
            .reply(200, { data: 'OK' }, {
                'Access-Control-Allow-Origin': '*',
            });

        const data = await getJsonRes({
            hostname: 'foo.bar',
            pathname: '/',
            mode: 'cors',
        });

        expect(data).toEqual({ data: 'OK' });
    });

    it('Should support empty pathname', async() => {
        nock('https://magiclinks.local.yandex-team.ru')
            .get('/')
            .reply(200, { data: 'OK' });

        const data = await getJsonRes({
            protocol: 'https:',
            hostname: 'magiclinks.local.yandex-team.ru',
        });

        expect(data).toEqual({ data: 'OK' });
    });

    it('Should support custom pathname', async() => {
        nock('https://magiclinks.local.yandex-team.ru')
            .get('/foo')
            .reply(200, { data: 'OK' });

        const data = await getJsonRes({
            protocol: 'https:',
            hostname: 'magiclinks.local.yandex-team.ru',
            pathname: '/foo',
        });

        expect(data).toEqual({ data: 'OK' });
    });

    it('Should support default hostname and protocol', async() => {
        const { location: { protocol, hostname } } = window;

        nock(`${protocol}//${hostname}`)
            .get('/')
            .reply(200, { data: 'OK' });

        const data = await getJsonRes({
            pathname: '/',
        });

        expect(data).toEqual({ data: 'OK' });
    });

    it('Should support username and password', async() => {
        nock('https://user:pass@magiclinks.local.yandex-team.ru')
            .get('/')
            .reply(200, { data: 'OK' });

        const data = await getJsonRes({
            protocol: 'https:',
            username: 'user',
            password: 'pass',
            hostname: 'magiclinks.local.yandex-team.ru',
            pathname: '/',
        });

        expect(data).toEqual({ data: 'OK' });
    });

    it('Should support custom port', async() => {
        nock('https://magiclinks.local.yandex-team.ru:9999')
            .get('/')
            .reply(200, { data: 'OK' }, {
                'Access-Control-Allow-Origin': '*',
            });

        const data = await getJsonRes({
            protocol: 'https:',
            hostname: 'magiclinks.local.yandex-team.ru',
            port: '9999',
            pathname: '/',
            mode: 'cors',
        });

        expect(data).toEqual({ data: 'OK' });
    });

    it('Should support options.query', async() => {
        nock('https://magiclinks.local.yandex-team.ru')
            .get('/')
            .query({
                foo: 'bar',
                bar: ['baz', 'zot'],
            })
            .reply(200, { data: 'OK' });

        const data = await getJsonRes({
            protocol: 'https:',
            hostname: 'magiclinks.local.yandex-team.ru',
            pathname: '/',
            query: {
                foo: 'bar',
                bar: ['baz', 'zot'],
            },
        });

        expect(data).toEqual({ data: 'OK' });
    });

    it('Should support custom headers', async() => {
        nock('https://magiclinks.local.yandex-team.ru', {
            reqheaders: {
                'X-Test': 'Test',
            },
        })
            .get('/')
            .reply(200, { data: 'OK' });

        const data = await getJsonRes({
            protocol: 'https:',
            hostname: 'magiclinks.local.yandex-team.ru',
            pathname: '/',
            headers: {
                'X-Test': 'Test',
            },
        });

        expect(data).toEqual({ data: 'OK' });
    });

    it('Should fetch json', async() => {
        nock('https://magiclinks.local.yandex-team.ru')
            .get('/')
            .reply(200, { data: 'OK' });

        const data = await getJsonRes({
            protocol: 'https:',
            hostname: 'magiclinks.local.yandex-team.ru',
            pathname: '/',
        });

        expect(data).toEqual({ data: 'OK' });
    });

    it('Should throw error with invalid json', async() => {
        nock('https://magiclinks.local.yandex-team.ru')
            .get('/')
            .reply(500, 'NOT A VALID JSON');

        try {
            await getJsonRes({
                protocol: 'https:',
                hostname: 'magiclinks.local.yandex-team.ru',
                pathname: '/',
            });
        } catch (err) {
            expect(err).toEqual(new Error('NOT A VALID JSON'));
        }
    });

    it('Should throw error with error.message', async() => {
        nock('https://magiclinks.local.yandex-team.ru')
            .get('/')
            .reply(500, { message: 'ERROR!!!' });

        try {
            await getJsonRes({
                protocol: 'https:',
                hostname: 'magiclinks.local.yandex-team.ru',
                pathname: '/',
            });
        } catch (err) {
            expect(err).toEqual(new Error('ERROR!!!'));
        }
    });
});
