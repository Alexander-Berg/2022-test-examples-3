describe('b-context-preview', function() {
    var sandbox,
        block,
        bannerDataStub = {
            "adId": "1493806814512",
            "title": "Дизайн интерьера",
            "body": "Предлагаем услуги в сфере дизайна интерьеров квартир и загородных домов.",
            "url": "http://artspline.ru?yclid=101351264523126",
            "domain": "artspline.ru",
            "punyDomain": "artspline.ru",
            "favicon": "https://favicon.yandex.net/favicon/artspline.ru/",
            "vcardUrl": "#",
            "callUrl": "",
            "age": "18+",
            "warning": "Дефолтный дисклеймер-ru",
            "sitelinks": [{ "title": "Готовые проекты", "url": "http://artspline.ru/?id=97", "description": "" },
                { "title": "Дизайн интерьера", "url": "http://artspline.ru/?id=25", "description": "" },
                { "title": "Цены и состав проекта", "url": "http://artspline.ru/?id=27", "description": "" },
                { "title": "Архитектура", "url": "http://artspline.ru/?id=78", "description": "" }],
            "linkTail": "",
            "debug": ""
        };

    function createBlock(options) {
        block = u.getInitedBlock({
            block: 'b-context-preview',
            data: {
                options: options.options,
                banner: options.banner
            }
        }, false);
    }

    beforeEach(function() {
        sandbox = sinon.sandbox.create();
    });

    afterEach(function() {
        sandbox.restore();
        block.destruct && block.destruct();
    });

    describe('Обработка события message', function(){

        it('При load из iframe отсылается postMessage', function() {
            createBlock({ options: {}, banner: bannerDataStub });
            var iframe = block.findBlockOn('iframe', 'i-foreign-iframe');
            sandbox.stub(iframe, 'postMessage');
            iframe.trigger('load');

            expect(JSON.parse(iframe.postMessage.getCall(0).args[0]))
                .to.have.property('directPreview');
        });

        it('Если есть options.scrollId, то оно должно попасть в postMessage', function() {
            createBlock({ options: { scrollId: 'video' }, banner: bannerDataStub });
            var iframe = block.findBlockOn('iframe', 'i-foreign-iframe');
            sandbox.stub(iframe, 'postMessage');
            iframe.trigger('load');

            expect(JSON.parse(iframe.postMessage.getCall(0).args[0]))
                .to.have.property('scrollId', 'video');
        });

    });

});
