import getPlaneUrl from '../getPlaneUrl';

describe('getPlaneUrl', () => {
    it('production', () => {
        expect(getPlaneUrl(undefined, true)).toBe(
            'https://travel.yandex.ru/avia',
        );
        expect(getPlaneUrl('/somepath', true)).toBe(
            'https://travel.yandex.ru/avia/somepath',
        );
    });

    it('testing', () => {
        expect(getPlaneUrl()).toBe('https://travel-test.yandex.ru/avia');
        expect(getPlaneUrl('/somepath')).toBe(
            'https://travel-test.yandex.ru/avia/somepath',
        );
    });
});
