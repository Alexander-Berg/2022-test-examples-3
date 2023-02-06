describe('b-banner-preview2__additions', function() {
    var model,
        block,
        sandbox,
        stubData = {
            sitelinks: [
                { title: 'sitelink1', url: 'http://ya.ru' },
                { title: 'sitelink2', url: 'http://yandex.ru' }
            ],
            image: 'lol.jpg',
            videoExtension: {
                type : 'video',
                name : 'ddggdg',
                id : '101',
                urls: [
                    {
                        delivery: 'progressive',
                        type: 'video/webm',
                        bitrate: 1086,
                        height: 720,
                        url: 'https://storage.mdst.yandex.net/get-video-videodirekt/4857/158f91c9ed6/a7d654bd593bd369/720p.webm?redirect=yes&sign=7ab8d9be25137a6be2d4aba522a7d0f43122c4a22792bba6f75c7dc6f5ae5f5b&ts=6b1c2b8b',
                        id: '720p.webm',
                        width: 1280
                    }
                ]
            }
        },
        constStub;

    beforeEach(function() {
        sandbox = sinon.sandbox.create();

        sandbox.useFakeTimers();

        constStub = sandbox.stub(u, 'consts');
        constStub.withArgs('SCRIPT_OBJECT').returns(u.getScriptObjectForStub());
        constStub.withArgs('rights').returns({});

        model = BEM.MODEL.create('b-banner-preview2_type_text');

        block = u.getInitedBlock({
            block: 'b-banner-preview2',
            mods: { view: 'tests-only' },
            data: model.toJSON(),
            modelsParams: { vmParams: { name: model.name,  id: model.id } }
        });
    });

    afterEach(function() {
        block.destruct();
        model.destruct();
        sandbox.restore();
        constStub.restore();
    });

    it('Без картинки и без сайтлинков элемента быть не должно', function() {
        expect(block).to.not.haveElem('addition');
    });

    it('С картинкой, но без сайтлинов текст должен быть соответствующим', function() {
        model.update({ image: stubData.image });
        sandbox.clock.tick(500);
        expect(block.elem('additions-sub-text').text()).to.equal('изображение (стандартное)');
    });

    it('С сайтлинками, но без картинки текст должен быть соответствующим', function() {
        model.update({ sitelinks: stubData.sitelinks });
        sandbox.clock.tick(500);
        expect(block.elem('additions-sub-text').text()).to.equal('быстрые ссылки');
    });

    it('С сайтлинками, картинкой и видео текст должен быть соответсвующий', function() {
        model.update(stubData);
        sandbox.clock.tick(500);
        expect(block.elem('additions-sub-text').text()).to.equal('изображение (стандартное), быстрые ссылки, видео');
    });

    it('С сайтлинками имеющими описание текст должен быть соответсвующий', function() {
        model.update({
            sitelinks: [
                { title: 'sitelink1', url: 'http://ya.ru', description: 'описание' }
            ]
        });
        sandbox.clock.tick(500);
        expect(block.elem('additions-sub-text').text()).to.equal('быстрые ссылки с описаниями');
    });

    it('У баннера с видеодополнением появляется отметка об этом', function() {
        model.update({ videoExtension: stubData.videoExtension });

        sandbox.clock.tick(500);

        expect(block.elem('additions-sub-text').text()).to.equal('видео');
    });
});
