import { getTabName } from './index';

describe('Get tab name for yt log', () => {
    it('undefined -> ""', () => {
        expect(getTabName(undefined)).toBe('');
    });

    it('/ -> /', () => {
        expect(getTabName('/')).toBe('/');
    });

    it('/cars/ -> cars', () => {
        expect(getTabName('/cars/')).toBe('cars');
    });

    it('/cars -> cars', () => {
        expect(getTabName('/cars')).toBe('cars');
    });

    it('/settings/tags -> settings/tags', () => {
        expect(getTabName('/settings/tags')).toBe('settings/tags');
    });

    it('/settings/tags/test -> empty string', () => {
        expect(getTabName('/settings/tags/test')).toBe('');
    });

    it('/clients/b430ce4f-0938-4df8-90db-3c68e4b227f7/info -> clients/info', () => {
        expect(getTabName('/clients/b430ce4f-0938-4df8-90db-3c68e4b227f7/info')).toBe('clients/info');
    });

    it('/clients/b430ce4f-0938-4df8-90db-3c68e4b227f7 -> empty string', () => {
        expect(getTabName('/clients/b430ce4f-0938-4df8-90db-3c68e4b227f7')).toBe('');
    });

    it('/session/479c863b-122e086b-5e3d42d2-c2636aba7 -> empty string', () => {
        expect(getTabName('/session/479c863b-122e086b-5e3d42d2-c2636aba')).toBe('');
    });

    it('/session/479c863b-122e086b-5e3d42d2-c2636aba7/ -> empty string', () => {
        expect(getTabName('/session/479c863b-122e086b-5e3d42d2-c2636aba/')).toBe('');
    });
});
