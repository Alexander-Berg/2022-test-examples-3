jest.disableAutomock();

import getRaspDomain from '../getRaspDomain';

describe('getRaspDomain', () => {
    it('ru', () => {
        expect(
            getRaspDomain({
                tld: 'ru',
            }),
        ).toBe('rasp.yandex.ru');

        expect(
            getRaspDomain({
                tld: 'ru',
                platform: 'mobile',
            }),
        ).toBe('t.rasp.yandex.ru');
    });

    it('ua', () => {
        expect(
            getRaspDomain({
                tld: 'ua',
            }),
        ).toBe('rasp.yandex.ua');
    });

    it('Wrong platform and unknown tld', () => {
        expect(
            getRaspDomain({
                tld: 'unknown',
                platform: 'wrong',
            }),
        ).toBe('rasp.yandex.unknown');
    });
});
