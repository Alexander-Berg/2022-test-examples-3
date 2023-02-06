describeBlock('i-entity-helpers__yandex-video-url', function(block) {
    let context;
    let dataset;
    let result;
    let service;

    beforeEach(function() {
        dataset = {
            type: 'film',
            duration: 120,
            text: 'Титаник'
        };

        context = {
            reportData: { reqdata: {} },
            device: { BrowserName: undefined }
        };

        service = sinon.stub(RequestCtx.Service, 'service').returns({
            id: 'video',
            label: 'Видео',
            params: '',
            pathnames: '',
            root: '//yandex.ru/video',
            search: '/touch/search?text=%D1%82%D0%B8%D1%82%D0%B0%D0%BD%D0%B8%D0%BA'
        });
    });

    afterEach(function() {
        service.restore();
    });

    it('should add video params in search app', function() {
        context.isSearchApp = true;
        context.reportData.reqdata.uuid = 'test-uuid';

        result = block(context, dataset);

        assert.equal(result.url(),
            '//yandex.ru/video/touch/search?text=%D0%A2%D0%B8%D1%82%D0%B0%D0%BD%D0%B8%D0%BA&source=qa&oo_type=film' +
            '&duration=short&autoopen=1&ui=webmobileapp.yandex&service=video.yandex&uuid=test-uuid');
    });

    it('should not add video params in web search', function() {
        context.isSearchApp = false;

        result = block(context, dataset);

        assert.equal(result.url(),
            '//yandex.ru/video/touch/search?text=%D0%A2%D0%B8%D1%82%D0%B0%D0%BD%D0%B8%D0%BA' +
            '&source=qa&oo_type=film&duration=short&autoopen=1');
    });
});
