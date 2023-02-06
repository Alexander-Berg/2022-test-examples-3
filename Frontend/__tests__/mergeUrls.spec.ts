import { mergeUrls } from '../mergeUrls';

describe('mergeUrls', () => {
    let locationSpy;

    beforeEach(() => {
        locationSpy = jest.spyOn(window, 'location', 'get');

        locationSpy.mockImplementation(() => ({
            toString: () => 'https://yandex.ru/',
        }));
    });

    afterEach(() => {
        locationSpy.mockRestore();
    });

    it('should use window.location as base for url', () => {
        expect(mergeUrls('', [])).toBe('https://yandex.ru/');
    });

    it('should preserve path from pathProvider', () => {
        expect(mergeUrls('/jobs/', [])).toBe('https://yandex.ru/jobs/');
    });

    it('should ignore query params of pathProvider', () => {
        expect(mergeUrls('/jobs/?test=123', [])).toBe('https://yandex.ru/jobs/');
    });

    it('should override pathProvider query params by queryProvider', () => {
        expect(mergeUrls('/jobs/?test=123', ['/?test=456'])).toBe('https://yandex.ru/jobs/?test=456');
    });

    it('should add query params of queryProviders', () => {
        const queryProviders = [
            '/?test1=1',
            '/?test2=2',
            '/',
            '/?test3=3&test4=4',
        ];

        const expectedUrl = 'https://yandex.ru/jobs/?test1=1&test2=2&test3=3&test4=4';

        expect(mergeUrls('/jobs/?test=123', queryProviders)).toBe(expectedUrl);
    });

    it('should merge query params of queryProviders', () => {
        const queryProviders = [
            '/?test1=1&test&test2=str',
            '/?test1=2&test',
        ];

        const expectedUrl = 'https://yandex.ru/jobs/?test1=1&test=&test2=str&test1=2';

        expect(mergeUrls('/jobs/?test=123', queryProviders)).toBe(expectedUrl);
    });
});
