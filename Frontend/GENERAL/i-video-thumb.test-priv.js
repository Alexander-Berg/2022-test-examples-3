describeBlock('i-video-thumb__url', function(block) {
    var options;

    beforeEach(function() {
        options = {
            url: '//tub.yandex.net/i?id=1',
            w: 200,
            h: 100
        };
    });

    it('should add n, w, h to thumb url', function() {
        options.n = 10;
        assert.strictEqual(block(options), '//tub.yandex.net/i?id=1&n=10&w=200&h=100');
    });

    it('should add n=1040 by default', function() {
        assert.strictEqual(block(options), '//tub.yandex.net/i?id=1&n=1040&w=200&h=100');
    });

    it('should scale thumb size if retinaScale option is provided', function() {
        options.retinaScale = 2;
        assert.match(block(options), /&n=1040&w=400&h=200$/);
    });

    it('should set thumb size as /WxH if there is no query string in url', function() {
        options.url = '//tub.yandex.net/get-video_frame';
        assert.strictEqual(block(options), `${options.url}/200x100`);
    });

    it('should scale thumb size if retinaScale options is provided and there is no query string in url', function() {
        options.retinaScale = 2;
        options.url = '//tub.yandex.net/get-video_frame';
        assert.strictEqual(block(options), `${options.url}/400x200`);
    });

    it('should take into account trailing slash when there is no query string in url', function() {
        options.url = '//tub.yandex.net/get-video_frame/';
        assert.strictEqual(block(options), `${options.url}/200x100`);
    });
});
