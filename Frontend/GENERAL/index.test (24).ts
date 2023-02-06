import { runNginx, getRedirectDestination, stopNginx, getSplittingInjection } from './helpers';

jest.setTimeout(30000);

describe('redirects', () => {
    beforeAll(async() => {
        await runNginx();
    });

    afterAll(async() => {
        await stopNginx();
    });

    test('/', async done => {
        await expect(
            getRedirectDestination('http://localhost/')
        ).resolves.toEqual('https://yandex.ru/health/turbo/articles');

        done();
    });

    test('/articles', async done => {
        await expect(
            getRedirectDestination('http://localhost/articles')
        ).resolves.toEqual('https://yandex.ru/health/turbo/articles');

        done();
    });

    test('/articles/zachem-nuzhen-chekap-i-kak-ego-prokhodit', async done => {
        await expect(
            getRedirectDestination('http://localhost/articles/zachem-nuzhen-chekap-i-kak-ego-prokhodit')
        ).resolves.toEqual('https://yandex.ru/health/turbo/articles');

        done();
    });

    test('/pills', async done => {
        await expect(
            getRedirectDestination('http://localhost/pills')
        ).resolves.toEqual('https://yandex.ru/health/pills');

        done();
    });

    test('/pills?q=1', async done => {
        await expect(
            getRedirectDestination('http://localhost/pills?q=1')
        ).resolves.toEqual('https://yandex.ru/health/pills?q=1');

        done();
    });

    test('/pills/atc/:category', async done => {
        await expect(
            getRedirectDestination('http://localhost/pills/atc/xxx')
        ).resolves.toEqual('https://yandex.ru/health/pills/atc/xxx');

        done();
    });

    test('/pills/atc/:category?q=1', async done => {
        await expect(
            getRedirectDestination('http://localhost/pills/atc/xxx?q=1')
        ).resolves.toEqual('https://yandex.ru/health/pills/atc/xxx?q=1');

        done();
    });

    test('/pills/bad/biologicheski-aktivnye-dobavki-bady', async done => {
        await expect(
            getRedirectDestination(
                'http://localhost/pills/bad/biologicheski-aktivnye-dobavki-bady'
            )
        ).resolves.toEqual(
            'https://yandex.ru/health/pills/type/biologicheski-aktivnaya-dobavka'
        );

        done();
    });

    test('/pills/bad/biologicheski-aktivnye-dobavki-bady?q=1', async done => {
        await expect(
            getRedirectDestination(
                'http://localhost/pills/bad/biologicheski-aktivnye-dobavki-bady?q=1'
            )
        ).resolves.toEqual(
            'https://yandex.ru/health/pills/type/biologicheski-aktivnaya-dobavka?q=1'
        );

        done();
    });

    test('/pills/type/:category', async done => {
        await expect(
            getRedirectDestination('http://localhost/pills/type/xxx')
        ).resolves.toEqual('https://yandex.ru/health/pills/type/xxx');

        done();
    });

    test('/pills/type/:category?q=1', async done => {
        await expect(
            getRedirectDestination('http://localhost/pills/type/xxx?q=1')
        ).resolves.toEqual('https://yandex.ru/health/pills/type/xxx?q=1');

        done();
    });

    test('/pills/substance-in/:id', async done => {
        await expect(
            getRedirectDestination('http://localhost/pills/substance-in/xxx')
        ).resolves.toEqual('https://yandex.ru/health/pills/substance-in/xxx');

        done();
    });

    test('/pills/substance-in/:id?q=1', async done => {
        await expect(
            getRedirectDestination('http://localhost/pills/substance-in/xxx?q=1')
        ).resolves.toEqual('https://yandex.ru/health/pills/substance-in/xxx?q=1');

        done();
    });

    test('/pills/substance/:id', async done => {
        await expect(
            getRedirectDestination('http://localhost/pills/substance/xxx')
        ).resolves.toEqual('https://yandex.ru/health/pills/substance/xxx');

        done();
    });

    test('/pills/substance/:id?q=1', async done => {
        await expect(
            getRedirectDestination('http://localhost/pills/substance/xxx?q=1')
        ).resolves.toEqual('https://yandex.ru/health/pills/substance/xxx?q=1');

        done();
    });

    test('/pills/:id', async done => {
        await expect(
            getRedirectDestination('http://localhost/pills/xxx')
        ).resolves.toEqual('https://yandex.ru/health/pills/product/xxx');

        done();
    });

    test('/pills/:id?q=1', async done => {
        await expect(
            getRedirectDestination('http://localhost/pills/xxx?q=1')
        ).resolves.toEqual('https://yandex.ru/health/pills/product/xxx?q=1');

        done();
    });

    test('hardcore query string', async done => {
        await expect(
            getRedirectDestination('http://localhost/pills/aksosef-37841?%3C/script%3E%3Cscript%3E751(9802)%3C/script%3E=1')
        ).resolves.toEqual('https://yandex.ru/health/pills/product/aksosef-37841?%3C/script%3E%3Cscript%3E751(9802)%3C/script%3E=1');

        done();
    });

    // Такие странные урлы существуют на старом портале только благодаря недоразумению.
    // Но их мы тоже вынуждены редиректить.
    // Важно, что редирект затрагивает все категории `/pills/bad/*`, кроме, собственно
    // БАДов (`biologicheski-aktivnye-dobavki-bady`), см выше.
    test('/pills/bad/:atcCategory', async done => {
        await expect(
            getRedirectDestination('http://localhost/pills/bad/protivovospalitelnye-i-protivorevmaticheskie-preparaty')
        ).resolves.toEqual('https://yandex.ru/health/pills/atc/protivovospalitelnye-i-protivorevmaticheskie-preparaty');

        done();
    });

    ['/', '/atc/', '/bad/', '/type/', '/substance-in/', '/substance/'].forEach(p => {
        test(`no redirect on splitting vulnerability for /pills${p}`, async done => {
            await expect(
                getRedirectDestination(`http://localhost/pills${p}%0aX-Splitting:<--here`)
            ).resolves.toBeNull();

            done();
        });

        test(`no injection on splitting vulnerability for /pills${p}`, async done => {
            await expect(
                getSplittingInjection(`http://localhost/pills${p}%0aX-Splitting:<--here`)
            ).resolves.toBeNull();

            done();
        });
    });
});
