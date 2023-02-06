describeBlock('adapter-companies__bcard-thumb-image-url', function(block) {
    let context;
    let thumb;

    beforeEach(function() {
        context = stubData('experiments');
        context.retina = { scale: 1 };
        thumb = {
            height: 1436,
            urlTemplate: 'https://avatars.mds.yandex.net/get-altay/1595534/%s',
            width: 1811
        };
    });

    it('should return altay suffix', function() {
        let result = block(context, thumb);
        assert.equal(result, 'https://avatars.mds.yandex.net/get-altay/1595534/M');
    });

    it('should return eda suffix', function() {
        thumb.mds_type = 'eda';

        let result = block(context, thumb);
        assert.equal(result, 'https://avatars.mds.yandex.net/get-altay/1595534/400x300');
    });

    it('should return sprav-products suffix', function() {
        thumb.mds_type = 'sprav-products';

        let result = block(context, thumb);
        assert.equal(result, 'https://avatars.mds.yandex.net/get-altay/1595534/medium');
    });

    it('should return tycoon suffix', function() {
        thumb.mds_type = 'tycoon';

        let result = block(context, thumb);
        assert.equal(result, 'https://avatars.mds.yandex.net/get-altay/1595534/main');
    });
});
