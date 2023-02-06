import { JSDOM } from 'jsdom';

import { ENV } from 'utils/getEnv';

import { getEnvHost } from 'features/EnvSwitcher/helpers/getEnvHost/getEnvHost';

describe('getEnvHost', function () {
    beforeEach(function () {
        const jsDom = new JSDOM();
        // @ts-ignore
        window = jsDom.window;
        // @ts-ignore
        delete window.location;
    });

    it('works with prestable origin', function () {
        window.location = {
            ...window.location,
            hostname: 'prestable.drivematics.yandex.ru',
            origin: 'https://prestable.drivematics.yandex.ru',
        };

        expect(getEnvHost(ENV.prestable)).toMatchInlineSnapshot(`"https://prestable.drivematics.yandex.ru"`);
        expect(getEnvHost(ENV.testing)).toMatchInlineSnapshot(`"https://testing.drivematics.yandex.ru"`);
        expect(getEnvHost(ENV.prod)).toMatchInlineSnapshot(`"https://drivematics.yandex.ru"`);
    });

    it('works with testing origin', function () {
        window.location = {
            ...window.location,
            hostname: 'testing.drivematics.yandex.com',
            origin: 'https://testing.drivematics.yandex.com',
        };

        expect(getEnvHost(ENV.prestable)).toMatchInlineSnapshot(`"https://prestable.drivematics.yandex.com"`);
        expect(getEnvHost(ENV.testing)).toMatchInlineSnapshot(`"https://testing.drivematics.yandex.com"`);
        expect(getEnvHost(ENV.prod)).toMatchInlineSnapshot(`"https://drivematics.yandex.com"`);
    });

    it('works with production origin', function () {
        window.location = {
            ...window.location,
            hostname: 'drivematics.yandex.eu',
            origin: 'https://drivematics.yandex.eu',
        };

        expect(getEnvHost(ENV.prestable)).toMatchInlineSnapshot(`"https://prestable.drivematics.yandex.eu"`);
        expect(getEnvHost(ENV.testing)).toMatchInlineSnapshot(`"https://testing.drivematics.yandex.eu"`);
        expect(getEnvHost(ENV.prod)).toMatchInlineSnapshot(`"https://drivematics.yandex.eu"`);
    });

    it('works with PR origin', function () {
        window.location = {
            ...window.location,
            hostname: 'pr-1.testing.drivematics.yandex.ru',
            origin: 'https://pr-1.testing.drivematics.yandex.ru',
        };

        expect(getEnvHost(ENV.prestable)).toMatchInlineSnapshot(`"https://pr-1.prestable.drivematics.yandex.ru"`);
        expect(getEnvHost(ENV.testing)).toMatchInlineSnapshot(`"https://pr-1.testing.drivematics.yandex.ru"`);
        expect(getEnvHost(ENV.prod)).toMatchInlineSnapshot(`"https://drivematics.yandex.ru"`);
    });
});
