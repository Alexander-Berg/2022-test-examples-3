import config from '../../src/config';

describe('config', () => {
    it('urls from config should be equal to production values', () => {
        expect(config.getApiHost()).toEqual(config._production.apiHost);
        expect(config.getStorageHost()).toEqual(config._production.storageHost);
        expect(config.getSettingsURL()).toEqual(`${config._production.settingsHost  }/app/settings`);
        expect(config.getLandingHost()).toEqual(config._production.landingHost);
    });
});
