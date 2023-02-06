describe('b-edit-banner2_type_mcbanner', function() {
    var blockTree,
        bemBlock,
        bannerModel,
        sandbox,
        requests = [],
        geoModel,
        xhr,
        defaultBannerData = { modelId: 'banner-id' },
        defaultGroupData =  { modelId: 'group-id', isSingleGroup: true, cid: 'campaign-id', adgroup_type: 'mcbanner' },
        bemjson,
        constStub;

    function createBlock(extraParams, bannerData) {
        xhr = sinon.useFakeXMLHttpRequest();
        xhr.onCreate = function(request) {
            requests.push(request);
        };

        var campaignModel = BEM.MODEL.create({ name: 'm-campaign', id: 'campaign-id' }, {
                cid: 'campaign-id',
                strategy: {
                    search: {},
                    net: {}
                },
                mediaType: 'mcbanner'
            }),
            groupModel = BEM.MODEL.create({ name: 'dm-mcbanner-group', id: 'group-id' }, $.extend({}, defaultGroupData, {
                banners: [
                    $.extend({}, defaultBannerData, bannerData)
                ]
            })),
            groupData = groupModel.provideData();

        bannerModel = groupModel.getBanners()[0];
        geoModel = BEM.MODEL.create(
            { name: 'm-geo-regions', id: 'group-id', parentName: 'dm-mcbanner-group', parentId: 'group-id' },
            { geo: '1'});
        groupData.adgroup_type = 'mcbanner';

        bemjson = $.extend( {
            block: 'b-edit-banner2',
            mods: { type: 'mcbanner' },
            banner: bannerModel.provideData(),
            group: groupData,
            campaign: campaignModel.toJSON()
        }, extraParams);

        blockTree = u.getDOMTree(bemjson);

        $('body').append(blockTree);

        bemBlock = BEM.DOM.init(blockTree).bem('b-edit-banner2');
    }

    function destructBlock() {
        xhr.restore && xhr.restore();
        bemBlock && bemBlock.domElem ? BEM.DOM.destruct(bemBlock.domElem) : bemBlock.destruct();

        BEM.MODEL.destruct({ name: 'm-campaign', id: 'campaign-id' });
        BEM.MODEL.destruct({ name: 'dm-mcbanner-group', id: 'group-id' });
        BEM.MODEL.destruct({ name: 'm-banner', id: 'banner-id', parentName: 'dm-mcbanner-group', parentId: 'group-id' });
        geoModel.destruct();

        requests = [];
    }

    before(function() {
        constStub = sinon.stub(u, 'consts');

        constStub
            .withArgs('rights').returns({})
            .withArgs('yandex_domain').returns('//yandex.ru')
            .withArgs('ulogin').returns('ulogin');
    });

    after(function() {
        constStub.restore();
    });

    describe('Инициализация блока с дефолтными параметрами', function() {
        before(function() {
            createBlock({});
        });

        after(function() {
            destructBlock();
        });

        ['header', 'href2', 'image-ad2'].forEach(function(name) {
            it('В баннере должен содержаться один элемент ' + name, function() {
                expect(bemBlock.elem(name).length).to.be.equal(1);
            });
        });

        it('В баннере должен содержаться один блок b-banner-href', function() {
            expect(bemBlock.findBlocksInside('b-banner-href').length).to.be.equal(1);
        });

        it('При создании блока b-edit-banner должна создаться одна модель m-banner', function() {
            expect(BEM.MODEL.get('m-banner').length).to.be.equal(1);
        });

        it('Путь m-banner должен браться из параметров блока', function() {
            expect(BEM.MODEL.getOne('m-banner').path()).to.be.equal('dm-mcbanner-group:group-id.m-banner:banner-id');
        });

        it('Должна быть доступна наружу функция isValid', function() {
            expect(bemBlock.isValid).to.be.an.instanceof(Function);
        });

        it('Cсылка на сайт обязательна для заполнения', function() {
            expect(bemBlock.getMod(bemBlock.elem('href-required'), 'hidden')).not.to.be.equal('yes');
        });
    });

    describe('Работа ссылки "удалить баннер" Инициализация на домене h', function() {
        var clock;

        beforeEach(function() {
            createBlock({ hasHeaderActions: true, canDelete: true });

            sandbox = sinon.sandbox.create({ useFakeTimers: true });
            clock = sandbox.clock;
        });

        afterEach(function() {
            sandbox.restore();
            destructBlock();
        });

        it('Должна быть ссылка "удалить", если hasHeaderActions: true и canDelete: true', function() {
            expect(bemBlock.elem('delete-btn').length).to.be.equal(1);
        });

        it('Отстуствует при инициализации блока с параметром canDelete = false', function() {
            destructBlock();
            createBlock({ hasHeaderActions: true, canDelete: false });

            expect(bemBlock).not.to.haveElem('delete-btn');
        });

        it('При клике на "удалить" должно стриггериться событие remove, если данные баннера не были изменены', function() {
            expect(bemBlock).to.triggerEvent('remove', function() {
                bemBlock.elem('delete-btn').click();
                clock.tick(1);
            });
        });

        it('Перед удалением баннера показывается предупреждение, если данные банера были изменены', function() {
            sandbox.stub(BEM.blocks['b-confirm'], 'open').callsFake(function() {});

            bemBlock.model.set('display_domain', 'Текст');
            bemBlock.elem('delete-btn').click();
            sandbox.clock.tick(1);

            expect(BEM.blocks['b-confirm'].open.called).to.be.true;
        });

        it('Если данные баннера были изменены и пользователь согласился с предупреждением - тригерит событие remove', function() {
            var spy;

            sandbox.stub(BEM.blocks['b-confirm'], 'open').callsFake(function(options, callbackCtx) {
                options.onYes.apply(callbackCtx);
            });


            spy = sandbox.spy(bemBlock, 'trigger');

            bemBlock.model.set('display_domain', 'Текст');
            bemBlock.elem('delete-btn').click();

            sandbox.clock.tick(1);

            expect(spy.calledWith('remove')).to.be.true;
        });

        it('При удалении модели баннера - блок НЕ удаляется, и НЕ удаляется его domElem', function() {
            bemBlock.model.destruct();

            expect(bemBlock).not.to.equal(undefined);
            expect(bemBlock.domElem).not.to.equal(undefined);
        });
    });

    describe('Шапка баннера', function() {
        before(function() {
            createBlock();
        });

        after(function() {
            destructBlock();
        });

        it('Не должна содержать галочку Мобильное объявление', function() {
            expect(bemBlock).not.to.haveElem('mobile');
        });

        it('Не должно быть ссылки "очистить текст"', function() {
            expect(bemBlock.elem('clear-text').length).to.be.equal(0);
        });
    });
});

