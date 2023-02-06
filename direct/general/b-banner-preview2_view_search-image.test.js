describe('b-banner-preview2_view_search-image', function() {
    var block,
        model,
        image,
        imageParams,
        spyGetImageUrl;

    beforeEach(function() {
        model = BEM.MODEL.create('b-banner-preview2_type_text');
    });

    afterEach(function() {
        model.destruct();
        block.destruct();
    });

    it('не должен иметь элемент image, если во входных данных нет imageAd', function() {
        block = u.getInitedBlock({
            block: 'b-banner-preview2',
            mods: { view: 'search-image' },
            data: {},
            modelsParams: {
                vmParams: { name: model.name, id: model.id }
            }
        });

        expect(block).to.not.haveElem('image');
    });

    it('должен использовать в качестве адрес ссылки customTitleUrl из данных', function() {
        var url = 'some-url';

        block = u.getInitedBlock({
            block: 'b-banner-preview2',
            mods: { view: 'search-image' },
            data: {
                customTitleUrl: url,
                imageAd: {}
            },
            modelsParams: {
                vmParams: { name: model.name, id: model.id }
            }
        });

        expect(block.findBlockInside('image-link', 'link').domElem.attr('href')).to.be.equal(url);
    });

    describe('по входным данным', function() {
        before(function() {
            imageParams = {
                group_id: 'gid',
                hash: 'fe4f32',
                scale: 0.5,
                width: 100,
                height: 50
            };
            spyGetImageUrl = sinon.spy(u, 'getImageUrl');

            block = u.getInitedBlock({
                block: 'b-banner-preview2',
                mods: { view: 'search-image' },
                data: {
                    customTitleUrl: 'some-url',
                    imageAd: imageParams
                },
                modelsParams: {
                    vmParams: { name: model.name, id: model.id }
                }
            });

            image = block.elem('image');
        });

        ['width', 'height'].forEach(function(param) {
            it('формирует параметр изображения учитывая scale — ' + param, function() {
                expect(image.attr(param)).to.be.equal('' + imageParams[param] * imageParams.scale);
            });
        });

        it('вызывает функцию u.getImageUrl', function() {
            expect(spyGetImageUrl.called).to.be.true;
        });

        ['namespace', 'mdsGroupId', 'hash'].forEach(function(param) {
            var map = {
                namespace: 'direct-picture',
                mdsGroupId: 'group_id',
                hash: 'hash'
            };

            it('вызывает функцию u.getImageUrl, где ' + param + ' = ' + map[param], function() {
                expect(spyGetImageUrl.calledWith({
                    namespace: 'direct-picture',
                    mdsGroupId: imageParams.group_id,
                    hash: imageParams.hash
                })).to.be.true;
            });
        });

    });
});
