import { Release } from './release';

describe('Env', () => {
    it('getReleaseServiceDirname', () => {
        expect(Release.getReleaseServiceDirname('release/test/v1.2.3')).toEqual('test');
        expect(Release.getReleaseServiceDirname('release/test/123')).toEqual('test');
        expect(() => Release.getReleaseServiceDirname('')).toThrow();
        expect(() => Release.getReleaseServiceDirname('release/')).toThrow();
        expect(() => Release.getReleaseServiceDirname('release/@yandex-int')).toThrow();
        expect(() => Release.getReleaseServiceDirname('invalid/@yandex-int/v1.2.3')).toThrow();
    });

    describe('getReleaseServicePath', () => {
        it('get from git release branch name', () => {
            expect(Release.getReleaseServicePath('release/test/v1.2.3')).toEqual('services/test');
        });

        it('get from arc release branch name', () => {
            expect(Release.getReleaseServicePath('releases/frontend/test/v1.2.3')).toEqual('services/test');
        });
    });
});
