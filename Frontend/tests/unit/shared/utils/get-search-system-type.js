const { getSearchSystemType } = require('../../../../src/shared/utils/get-search-system-type');

describe('/shared/utils/get-search-system-type', () => {
    it('should support deprecated engine arg val: google, yandex', () => {
        assert.equal(getSearchSystemType('yandex', 'desktop', false), 'yandex-web-desktop');
        assert.equal(getSearchSystemType('google', 'touch', true), 'google-web-touch-iphone');
    });
});
