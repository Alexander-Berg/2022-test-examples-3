import { IDeviceSource } from '@yandex-int/frontend-apphost-context';
import { getAssets, getBuildPath } from '../assets';

describe('#getAssets', () => {
    it('foo@1.64.0', () => {
        const assets = getAssets({ build: 'foo', hash: 'bar', version: '1.64.0', tld: 'ru' });

        expect(assets.body).toContain('bar/foo/app.js');
        expect(assets.body).toContain('bar/foo/config.js');
    });

    it('foo@2.3.0', () => {
        const assets = getAssets({ build: 'foo', hash: 'bar', version: '2.3.0', tld: 'ru' });

        expect(assets.body).toContain('bar/foo/app.js');
        expect(assets.body).toContain('bar/foo/config.js');
    });

    it('foo@2.4.0', () => {
        const assets = getAssets({ build: 'foo', hash: 'bar', version: '2.4.0', tld: 'ru' });

        expect(assets.body).toContain('bar/foo/app.js');
        expect(assets.body).not.toContain('bar/foo/config.js');
    });

    it('foo@999.999.999', () => {
        const assets = getAssets({ build: 'foo', hash: 'bar', version: '999.999.999', tld: 'ru' });

        expect(assets.body).toContain('bar/foo/app.js');
        expect(assets.body).not.toContain('bar/foo/config.js');
    });

    [
        ['Chrome', '53'],
        ['Edge', '16'],
        ['Firefox', '46'],
        ['Safari', '10'],
        ['MobileSafari', '11', 'iOS'],
        ['MobileFirefox', '57', 'Android'],
        ['ChromeMobile', '62', 'Android'],
        ['Opera', '40'],
        ['OperaMobile', '45', 'Android'],
        ['YandexBrowser', '17'],
        ['YandexSearch', '8'],
        ['Samsung Internet', '7', 'Android'],
        ['AndroidBrowser', '62', 'Android'],
    ].forEach(([name, version, family]) => {
        it(`include polyfill for ${name} ${version}`, () => {
            const assets = getAssets({
                device: { browser: { name, version }, os: { family } } as IDeviceSource,
                build: 'foo',
                hash: 'bar',
                version: '999.999.999',
                tld: 'ru',
            });

            expect(assets.head).toContain('polyfills.js');
        });
    });

    [
        ['Chrome', '54'],
        ['Edge', '17'],
        ['Firefox', '47'],
        ['Safari', '11'],
        ['MobileSafari', '12', 'iOS'],
        ['MobileFirefox', '58', 'Android'],
        ['ChromeMobile', '63', 'Android'],
        ['Opera', '41'],
        ['OperaMobile', '46', 'Android'],
        ['YandexBrowser', '18'],
        ['Samsung Internet', '8', 'Android'],
        ['AndroidBrowser', '63', 'Android'],
    ].forEach(([name, version, family]) => {
        it(`non include polyfill for ${name} ${version}`, () => {
            const assets = getAssets({
                device: { browser: { name, version }, os: { family } } as IDeviceSource,
                build: 'foo',
                hash: 'bar',
                version: '999.999.999',
                tld: 'ru',
            });

            expect(assets.head).not.toContain('polyfills.js');
        });
    });
});

describe('#getBuildPath', () => {
    [
        'yamb',
        'chamb',
        'yamb-internal',
        'yamb-embed-internal',
    ].forEach((build) => {
        it(`should get legacy folder map for ${build}`, () => {
            expect(getBuildPath(build, '2.35.0')).toEqual(build);
        });
    });

    [
        'yamb',
        'chamb',
        'yamb-internal',
        'yamb-embed-internal',
    ].forEach((build) => {
        it(`should get new folder map for ${build}`, () => {
            expect(getBuildPath(build, '2.36.0')).toEqual('web');
        });
    });

    [
        ['widget', 'yamb-widget'],
        ['yabro', 'yamb-yabro'],
    ].forEach(([build, expectedBuild]) => {
        it(`should correctly get build path for ${build} in old versions`, () => {
            expect(getBuildPath(build, '2.35.0')).toEqual(expectedBuild);
        });
    });

    [
        ['widget', 'web'],
        ['yabro', 'yamb-yabro'],
    ].forEach(([build, expectedBuild]) => {
        it(`should correctly get build path for ${build} in new versions`, () => {
            expect(getBuildPath(build, '2.36.0')).toEqual(expectedBuild);
        });
    });
});
