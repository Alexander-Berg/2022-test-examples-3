describe('b-banner-preview2_view_search-mobile', function() {
    var block,
        model,
        modelData = {
            url: 'http://okna.ru/',
            title: 'Кислые лимоны',
            body: 'Ледяной борщ! Креветка уже умерла',
            sitelinks: [
                {
                    title: 'быстрая ссылка 1',
                    url: 'http://ya.ru/'
                },
                {
                    title: 'быстрая ссылка 2',
                    url: 'http://direct.yandex.ru',
                    turbolanding: {
                        id: '1',
                        name: 'qwe',
                        href: 'https://yandex.ru/turbo?ola=1'
                    }
                }
            ]
        },
        constStub;

    function createBlock(model) {
        block = u.getInitedBlock({
            block: 'b-banner-preview2',
            mods: { view: 'search-mobile' },
            data: model.toJSON(),
            modelsParams: {
                vmParams: { name: model.name, id: model.id }
            }
        });
    }
    beforeEach(function() {
        model = BEM.MODEL.create('b-banner-preview2_type_text', modelData);

        constStub = sinon.stub(u, 'consts');
        constStub.withArgs('rights').returns({});
    });

    afterEach(function() {
        model.destruct();
        block.destruct();
        constStub.restore();
    });

    describe('Турбостраницы', function() {

        describe('В объявлении', function() {

            it('Если не задана, то в заголовке должна быть стандартная ссылка', function() {
                createBlock(model);
                expect(block.findBlockInside('title', 'link').domElem[0].href).to.be.eq(modelData.url);
            });

            it('Если задана, то в заголовке должна быть турбо ссылка', function() {
                model.set('turbolanding', {
                    id: '1',
                    href: 'https://yandex.ru/turbo?ola=1',
                    name: 'Ola'
                })
                createBlock(model);
                expect(block.findBlockInside('title', 'link').domElem[0].href).to.be.eq('https://yandex.ru/turbo?ola=1');
            });

        });

        describe('В сайтлинках', function() {

            it('Если не задана, то в сайтлинке должна быть стандартная ссылка', function() {
                createBlock(model);
                expect(block.findBlocksInside('sitelink', 'link')[0].domElem[0].href)
                    .to.be.eq(modelData.sitelinks[0].url);
            });

            it('Если задана, то в сайтлинке должна быть турбо ссылка', function() {
                createBlock(model);
                expect(block.findBlocksInside('sitelink', 'link')[1].domElem[0].href)
                    .to.be.eq(modelData.sitelinks[1].turbolanding.href);
            });

        });

    });

});
