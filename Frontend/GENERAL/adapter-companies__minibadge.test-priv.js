describeBlock('adapter-companies__minibadge-thumb-photo', function(block) {
    let context;
    let photo;

    beforeEach(function() {
        context = stubData('experiments');
        context.retina = { scale: 1 };
        photo = {
            height: 1436,
            urlTemplate: 'https://avatars.mds.yandex.net/get-altay/1595534/%s',
            width: 1811
        };
    });

    it('should return altay suffix', function() {
        let result = block(context, photo);
        assert.equal(result.image, 'https://avatars.mds.yandex.net/get-altay/1595534/80x80');
    });

    it('should return eda suffix', function() {
        photo.mds_type = 'eda';

        let result = block(context, photo);
        assert.equal(result.image, 'https://avatars.mds.yandex.net/get-altay/1595534/200x200');
    });

    it('should return sprav-products suffix', function() {
        photo.mds_type = 'sprav-products';

        let result = block(context, photo);
        assert.equal(result.image, 'https://avatars.mds.yandex.net/get-altay/1595534/200x200');
    });

    it('should return tycoon suffix', function() {
        photo.mds_type = 'tycoon';

        let result = block(context, photo);
        assert.equal(result.image, 'https://avatars.mds.yandex.net/get-altay/1595534/pin-desktop_x3');
    });

    it('should return ydo suffix', function() {
        photo.mds_type = 'ydo';

        let result = block(context, photo);
        assert.equal(result.image, 'https://avatars.mds.yandex.net/get-altay/1595534/med');
    });
});
