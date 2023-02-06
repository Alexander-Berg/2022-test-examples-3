import { replaceCSP } from '../replaceCSP';

describe('csp', () => {
    it('{tld}', () => {
        const cspWithTldPlaceholders = 'child-src yandex.{tld} https://files.messenger.alpha.yandex.net https://files.messenger.alpha.yandex.{tld};';
        const cleanCSP = 'child-src yandex.ru https://files.messenger.alpha.yandex.net https://files.messenger.alpha.yandex.ru;';

        expect(replaceCSP(cspWithTldPlaceholders, 'ru', '')).toBe(cleanCSP);
    });

    it('{csp.strictMetrikaHosts}', () => {
        const cspWithMetrikaPlaceholders = 'connect-src \'self\' yandex.ru https://yastatic.net https://yastat.net {csp.strictMetrikaHosts};';
        const cleanRuCSP = 'connect-src \'self\' yandex.ru https://yastatic.net https://yastat.net https://mc.yandex.ru;';
        const cleanComCSP = 'connect-src \'self\' yandex.ru https://yastatic.net https://yastat.net https://mc.yandex.ru https://mc.yandex.com;';

        expect(replaceCSP(cspWithMetrikaPlaceholders, 'ru', '')).toBe(cleanRuCSP);
        expect(replaceCSP(cspWithMetrikaPlaceholders, 'com', '')).toBe(cleanComCSP);
    });

    it('{csp.strictYandexHosts}', () => {
        const cspWithYandexPlaceholders = 'frame-src {csp.strictYandexHosts} https://files.messenger.alpha.yandex.net;';
        const cleanRuCSP = 'frame-src https://yandex.ru https://*.yandex.ru https://files.messenger.alpha.yandex.net;';
        const cleanComCSP = 'frame-src https://yandex.ru https://*.yandex.ru https://yandex.com https://*.yandex.com https://files.messenger.alpha.yandex.net;';

        expect(replaceCSP(cspWithYandexPlaceholders, 'ru', '')).toBe(cleanRuCSP);
        expect(replaceCSP(cspWithYandexPlaceholders, 'com', '')).toBe(cleanComCSP);
    });

    it('{csp.iframeTrustedOrigins}', () => {
        const cspWithOriginsPlaceholders = 'frame-src yandex.{tld} {csp.iframeTrustedOrigins};';
        const cleanCSP = 'frame-src yandex.ru test.ru;';

        expect(replaceCSP(cspWithOriginsPlaceholders, 'ru', 'test.ru')).toBe(cleanCSP);
    });
});
