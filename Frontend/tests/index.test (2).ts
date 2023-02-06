import { getPageForOutdateBrowsers } from '../src/getPageForOutdatedBrowser';

const modernUserAgent =
    'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_14_6) AppleWebKit/537.36 ' +
    '(KHTML, like Gecko) Chrome/78.0.3904.87 Safari/537.36';

const oldUserAgent =
    'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_14_6) AppleWebKit/537.36 ' +
    '(KHTML, like Gecko) Chrome/38.0.3904.87 Safari/537.36';

const outdatedUserAgent =
    'Mozilla/5.0 (Linux; Android 5.1.1; SAMSUNG SM-J120F Build/LMY47X) ' +
    'AppleWebKit/537.36 (KHTML, like Gecko) SamsungBrowser/3.5 Chrome/38.0.2125.102 Mobile Safari/537.36';

describe('getPageForOutdateBrowsers cases', () => {
    test('page should render', () => {
        const page = getPageForOutdateBrowsers({
            useragent: oldUserAgent,
            platform: 'desktop',
            logoUrl: '',
            favicons: '',
        });

        expect(typeof page).toBe('string');
    });

    test('page shouldnt render', () => {
        const page = getPageForOutdateBrowsers({
            useragent: modernUserAgent,
            platform: 'desktop',
            logoUrl: '',
            favicons: '',
        });

        expect(page).toBe(undefined);
    });

    test('webview android page should render', () => {
        const page = getPageForOutdateBrowsers({
            useragent: oldUserAgent,
            platform: 'touch',
            logoUrl: '',
            favicons: '',
            webviewDeviceType: 'android',
        });

        expect(typeof page).toBe('string');
        expect(page).toMatch('обновить системный браузер');
    });

    test('webview ios page should render', () => {
        const page = getPageForOutdateBrowsers({
            useragent: oldUserAgent,
            platform: 'touch',
            logoUrl: '',
            favicons: '',
            webviewDeviceType: 'ios',
        });

        expect(typeof page).toBe('string');
        expect(page).toMatch('нужно обновить iOS');
    });

    test('page shouldnt render for yandex bot', () => {
        const page = getPageForOutdateBrowsers({
            useragent: `${oldUserAgent} YandexBot`,
            platform: 'desktop',
            logoUrl: '',
            favicons: '',
        });

        expect(page).toBe(undefined);
    });

    test('page shouldnt render for google bot', () => {
        const page = getPageForOutdateBrowsers({
            useragent: `${oldUserAgent} GoogleBot`,
            platform: 'desktop',
            logoUrl: '',
            favicons: '',
        });

        expect(page).toBe(undefined);
    });

    test('page should render for exclusion browser', () => {
        const page = getPageForOutdateBrowsers({
            useragent: outdatedUserAgent,
            platform: 'touch',
            logoUrl: '',
            favicons: '',
            config: {
                minBrowserFamilyVersion: {
                    'Samsung Internet': 10,
                },
            },
        });

        expect(typeof page).toBe('string');
    });
});
