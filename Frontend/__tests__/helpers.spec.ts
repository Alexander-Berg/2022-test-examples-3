import * as dom from '@yandex-turbo/core/canUseDOM';
import { isExternalDomain } from '../helpers';

describe('Хелпер isExternalDomain', () => {
    const { location } = window;

    beforeAll(() => {
        delete window.location;

        // Явно указываем hostname
        window.location = { ...location, hostname: 'auto.ru' };
    });

    afterAll(() => {
        window.location = location;
    });

    it('должен вернуть false для yandex.{tld} домена', () => {
        const internalUrl = 'https://events.yandex.ru/events/meetings/14-sep-2019';

        expect(isExternalDomain(internalUrl)).toBe(false);
    });

    it('должен вернуть false если домен совпадает с текущим', () => {
        // @ts-ignore-next-line
        dom.canUseDOM = true;

        const internalUrl = 'https://auto.ru/novosibirsk/cars/';

        expect(isExternalDomain(internalUrl)).toBe(false);
    });

    it('должен вернуть true для внешнего домена', () => {
        const externalUrl = 'https://www.youtube.com/watch?v=v0P2SddtRC4';

        expect(isExternalDomain(externalUrl)).toBe(true);
    });
});
