import { csp } from '../server/csp';

describe('CSP', () => {
    const config = {
        from: 'test-from',
        tld: 'by',
        reqid: 'test-reqid',
        uid: 'test-uid',
        login: 'test-login',
        env: 'production',
    };

    const expectedPolicies = {
        'connect-src': 'self blob: mc.yandex.by yandexmetrica.com:* yandex.ru *.yandex.ru yastatic.net *.yastatic.net yastat.net *.yastat.net yandex.net *.yandex.net *.yandex-team.ru yandex.by *.yandex.by',
        'default-src': 'none',
        'font-src': "'self' data: yastatic.net yandex.ru",
        'frame-src': "'self' data: yandex.ru *.yandex.ru yastatic.net *.yastatic.net yastat.net *.yastat.net yandex.net *.yandex.net *.yandex-team.ru yandex.by *.yandex.by",
        'form-action': 'https://*',
        'img-src': "* 'self' blob: data:",
        'media-src': "'self' data: blob:",
        'script-src': "'self' blob: nonce-1234 'unsafe-inline' 'unsafe-eval' yandex.ru *.yandex.ru yastatic.net *.yastatic.net yastat.net *.yastat.net yandex.net *.yandex.net *.yandex-team.ru yandex.by *.yandex.by",
        'style-src': "'self' 'unsafe-inline' 'unsafe-eval' yandex.ru *.yandex.ru yastatic.net *.yastatic.net yastat.net *.yastat.net yandex.net *.yandex.net *.yandex-team.ru yandex.by *.yandex.by",
        'manifest-src': "'self' yandex.ru *.yandex.ru",
        'report-uri': 'https://csp.yandex.net/csp?from=test-from&reqid=test-reqid&yandexuid=test-uid&yandex_login=test-login&project=turbo',
    };

    // eslint-disable-next-line no-methods/no-entries
    for (const [directive, policy] of Object.entries(expectedPolicies)) {
        it(`Генерирует корректные политики ${directive}`, () => {
            expect(csp(config).policies).toMatch(`${directive} ${policy};`);
        });
    }

    it('Добавляет локальные хосты в development', () => {
        expect(csp({ ...config, env: 'development' }).policies).toMatch('localhost:* local.yandex.ru:*');
    });
});
