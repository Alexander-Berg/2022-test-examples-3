describe('b-edit-banner2_type_text', function() {
    var blockTree,
        bemBlock,
        bannerModel,
        picOutboardControl,
        sitelinksOutboardControl,
        requests = [],
        geoModel,
        xhr,
        defaultBannerData = { modelId: 'banner-id', 'has_site_enum': 'yes' },
        defaultGroupData =  { modelId: 'group-id', isSingleGroup: true, cid: 'campaign-id', adgroup_type: 'text' },
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
                mediaType: 'text'
            }),
            groupModel = BEM.MODEL.create({ name: 'm-group', id: 'group-id' }, $.extend({}, defaultGroupData, {
                banners: [
                    $.extend({}, defaultBannerData, bannerData)
                ]
            }));

        bannerModel = groupModel.getBanners()[0];
        geoModel = BEM.MODEL.create(
            { name: 'm-geo-regions', id: 'group-id', parentName: 'm-group', parentId: 'group-id' },
            { geo: '1'});

        bemjson = $.extend( {
            block: 'b-edit-banner2',
            mods: { type: 'text' },
            banner: bannerModel.provideData(),
            group: groupModel.provideData(),
            campaign: campaignModel.toJSON()
        }, extraParams);

        blockTree = u.getDOMTree(bemjson);

        $('body').append(blockTree);

        bemBlock = BEM.DOM.init(blockTree).bem('b-edit-banner2');

         //в коде попап для картинок инициализируется в p-multiedit2_type_text.bemtree.xjst
        blockTree = u.getDOMTree({
            block: 'b-outboard-controls',
            js: { id: 'pic-selector-control' },
            attrs: { id: 'pic-selector-control' },
            content: {
                elem: 'popup',
                buttons: false,
                header: false,
                innerBlock: {}
            }
        });

        $('body').append(blockTree);

        picOutboardControl = BEM.DOM.init(blockTree).bem('b-outboard-controls');

        //в коде попап для сайтлинков инициализируется в p-multiedit2_type_text.bemtree.xjst
        blockTree = u.getDOMTree({
            block: 'b-outboard-controls',
            js: { id: 'sitelinks-selector-control' },
            attrs: { id: 'sitelinks-selector-control' },
            content: {
                elem: 'popup',
                buttons: {
                    block: 'b-sitelinks-selector',
                    mods: { mode: 'banner' },
                    js: { id: 'multiedit-sitelinks-selector' },
                    modelId: 'banner',
                    content: { elem: 'buttons' }
                }
            }
        });

        $('body').append(blockTree);

        sitelinksOutboardControl = BEM.DOM.init(blockTree).bem('b-outboard-controls');
    }

    function destructBlock() {
        xhr.restore && xhr.restore();
        bemBlock && bemBlock.domElem ? BEM.DOM.destruct(bemBlock.domElem) : bemBlock.destruct();

        BEM.MODEL.destruct({ name: 'm-campaign', id: 'campaign-id' });
        BEM.MODEL.destruct({ name: 'm-group', id: 'group-id' });
        BEM.MODEL.destruct({ name: 'm-banner', id: 'banner-id', parentName: 'm-group', parentId: 'group-id' });
        geoModel.destruct();

        picOutboardControl && picOutboardControl.destruct();
        sitelinksOutboardControl && sitelinksOutboardControl.destruct();

        requests = [];
    }

    before(function() {
        constStub = sinon.stub(u, 'consts');

        constStub
            .withArgs('rights').returns({ addRemoveFlags: true })
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

        ['header', 'title', 'body', 'href', 'additions'].forEach(function(name) {
            it('В баннере должен содержаться один элемент ' + name, function() {
                expect(bemBlock.elem(name).length).to.be.equal(1);
            });
        });

        it('В баннере должен содержаться один блок b-banner-href', function() {
            expect(bemBlock.findBlocksInside('b-banner-href').length).to.be.equal(1);
        });

        it('В баннере должно быть поле model-field c name == title и name == body', function() {
            var elemNames = bemBlock.elem('model-field').map(function() {
                return bemBlock.elemParams($(this)).name;
            }).toArray();

            expect(elemNames).to.contain('body');
            expect(elemNames).to.contain('title');
        });

        it('При создании блока b-edit-banner должна создаться одна модель m-banner', function() {
            expect(BEM.MODEL.get('m-banner').length).to.be.equal(1);
        });

        it('Путь m-banner должен браться из параметров блока', function() {
            expect(BEM.MODEL.getOne('m-banner').path()).to.be.equal('m-group:group-id.m-banner:banner-id');
        });

        it('Должна быть доступна наружу функция isValid', function() {
            expect(bemBlock.isValid).to.be.an.instanceof(Function);
        });

        it('Если не передано withVCard, то у баннера не должно быть элемента vcard', function() {
            expect(bemBlock.elem('vcard').length).to.be.equal(0);
        });

    });

    describe('Инициализация блока с withVCard = true', function() {
        before(function() {
            createBlock({ withVCard: true });
        });

        after(function() {
            destructBlock();
        });

        it('Если передано withVCard, то у баннера должен быть элемент vcard', function() {
            expect(bemBlock.elem('vcard').length).to.be.equal(1);
        });

        it('Если передано withVCard, то у баннера должен быть блок b-vcard-control', function() {
            expect(bemBlock.findBlocksInside('b-vcard-control').length).to.be.equal(1);
        });

        it('Блок b-vcard-control должен создаваться с модификатором banner', function() {
            expect(bemBlock.findBlockInside('b-vcard-control').getMod('type')).to.be.equal('banner');
        });
    });

    describe('Инициализация на домене yandex.com.tr', function() {
        before(function() {
            constStub.withArgs('yandex_domain').returns('//yandex.com.tr');

            createBlock({}, false);
        });

        after(function() {
            destructBlock();
        });

        it('Должен быть блок с картинками', function() {
            expect(bemBlock.findBlocksInside('b-banner-pic').length).to.be.equal(1);
        });
    });

    describe('Работа ссылки "очистить текст"', function() {
        before(function() {
            createBlock({ hasHeaderActions: true });
        });

        after(function() {
            destructBlock();
        });

        it('Должна быть ссылка "очистить текст" если hasHeaderActions: true', function() {
            expect(bemBlock.elem('clear-text').length).to.be.equal(1);
        });

        it('При клике по ссылке "очистить текст" должны очищаться поля', function() {
            bemBlock.elem('clear-text').click();

            expect(bannerModel.get('title')).to.be.empty;
            expect(bannerModel.get('body')).to.be.empty;
            expect(bannerModel.get('callouts').length).to.equal(0);
            expect(bannerModel.get('video_resources')).to.be.empty;
            expect(bannerModel.get('display_href')).to.be.empty;
            expect(bannerModel.get('turbolanding').provideData()).to.be.empty;
        });
    });

    describe('Работа ссылки "скопировать из предыдущего"', function() {
        var previousBanner;

        before(function() {
            previousBanner = BEM.MODEL.create({ name: 'm-banner', id: 'prev' }, { title: 'prev_title', body: 'prev_body', 'has_site_enum': 'yes' });

            createBlock({ hasHeaderActions: true }, { modelId: 'banner-id', hasCopyFromPrev: true });
        });

        after(function() {
            previousBanner.destruct();
            destructBlock();
        });

        it('Должна быть ссылка "скопировать из предыдущего" если hasHeaderActions: true и у баннера hasCopyFromPrev = true', function() {
            expect(bemBlock.elem('copy-from-prev').length).to.be.equal(1);
        });

        it('При клике по ссылке заголовок баннера заполняется данными из предыдущего баннера', function() {
            bemBlock.elem('copy-from-prev').click();

            expect(bannerModel.get('title')).to.be.equal('prev_title');
        });

        it('При клике по ссылке тело баннера заполняется данными из предыдущего баннера', function() {
            bemBlock.elem('copy-from-prev').click();

            expect(bannerModel.get('body')).to.be.equal('prev_body');
        });

        it('Если есть видеодополнение, оно должно скопироваться', function() {
            previousBanner.set('video_resources', { id: '123' });

            bemBlock.elem('copy-from-prev').click();

            expect(bannerModel.get('video_resources').id).to.be.equal('123');
        });

        it('Если есть турбостраница, она должна скопироваться', function() {
            previousBanner.set('turbolanding', { id: '123', name: '123' });

            bemBlock.elem('copy-from-prev').click();

            expect(bannerModel.get('video_resources').id).to.be.equal('123');
        });

        it('Появляется предупреждение, если предыдущий баннер не заполнен', function() {
            previousBanner.set('title', '');
            previousBanner.set('body', '');
            previousBanner.set('video_resources', '');

            sinon.spy(BEM.blocks['b-confirm'], 'open');
            bemBlock.elem('copy-from-prev').click();

            expect(BEM.blocks['b-confirm'].open.called).to.be.equal(true);
            BEM.blocks['b-confirm'].open.restore();
        });
    });

    describe('Работа ссылки "удалить баннер" Инициализация на домене h', function() {
        var clock;

        before(function() {
            createBlock({ hasHeaderActions: true, canDelete: true });

            clock = sinon.useFakeTimers();
        });

        after(function() {
            clock.restore();
            destructBlock();
        });

        it('Должна быть ссылка "удалить", если hasHeaderActions: true и canDelete: true', function() {
            expect(bemBlock.elem('delete-btn').length).to.be.equal(1);
        });

        it('При клике на "удалить" должно стриггериться событие remove', function() {
            expect(bemBlock).to.triggerEvent('remove', function() {
                bemBlock.elem('delete-btn').click();
                clock.tick(1);
            });
        });
    });

    describe('Инпуты должны заполниться значениями из баннера', function() {
        var banner = { modelId: 'banner-id', title: 'bla1', body: 'bla2' };

        before(function() {
            createBlock({}, banner);
        });

        after(function() {
            destructBlock();
        });

        ['title', 'body'].forEach(function(name) {
            it('Поле ' + name, function() {
                expect(bemBlock.findBlockInside(name, 'input').val()).to.be.equal(banner[name]);
            })
        });
    });

    describe('Реакция на события модели', function() {
        var clock;

        before(function() {
            clock = sinon.useFakeTimers();
            var banner = {
                title: 'title1',
                body: 'body1',
                modelId: 'banner-id'
            };

            createBlock({
                canDelete: true,
                hasHeaderActions: true,
                withVCard: true,
                has_vcard: 1
            }, banner);
        });

        after(function() {
            clock.restore();
            destructBlock();
        });

        it('Если меняется has_vcard должен меняться флаг href_required рядом с урлом', function() {
            bannerModel.set('has_vcard', 0);

            clock.tick(0);

            expect(bemBlock.getMod(bemBlock.elem('href-required'), 'hidden')).not.to.be.equal('yes');
        });

        // todo:dima117a@xxx
        it('Если тип объявления графическое - ссылка на сайт обязательна для заполнения', function() {
            bannerModel.set('ad_type', 'image_ad');

            clock.tick(0);

            expect(bemBlock.getMod(bemBlock.elem('href-required'), 'hidden')).not.to.be.equal('yes');
        });

        it('При изменении title или body должны изменяться каунтеры рядом с соответствующими полями', function() {
            var prevTitleCounter = bemBlock.elem('title-length-counter').html(),
                prevBodyCounter = bemBlock.elem('body-length-counter').html();

            bannerModel.set('title', 'title11');
            bannerModel.set('body', 'body11');

            clock.tick(100);

            expect(bemBlock.elem('title-length-counter').html()).not.to.be.equal(prevTitleCounter);
            expect(bemBlock.elem('body-length-counter').html()).not.to.be.equal(prevBodyCounter);
        });

    });

    describe('Если withVCard = true и has_vcard = true', function() {
        before(function() {
            var banner = {
                modelId: 'banner-id',
                has_vcard: 1
            };

            createBlock({
                withVCard: true
            }, banner);
        });

        after(function() {
            destructBlock();
        });

        it('Блок должен отрисоваться с необязательным блоком урла', function() {
            expect(bemBlock.getMod(bemBlock.elem('href-required'), 'hidden')).to.be.equal('');
        });
    });

    describe('Если withVCard = true и has_vcard = false', function() {
        before(function() {
            var banner = {
                modelId: 'banner-id',
                has_vcard: 0
            };

            createBlock({
                withVCard: true
            }, banner);
        });

        after(function() {
            destructBlock();
        });

        it('Блок должен отрисоваться с обязательным блоком урла', function() {
            expect(bemBlock.getMod(bemBlock.elem('href-required'), 'hidden')).not.to.be.equal('yes');
        });
    });

    describe('Работа с b-banner-sitelink', function() {
        var clock;

        before(function() {
            createBlock({});
            clock = sinon.useFakeTimers();
        });

        after(function() {
            destructBlock();
            clock.restore();
        });

        it('При инициализации sitelinks должна вызваться функция, инициализирующая b-banner-sitelinks', function() {
            BEM.MODEL.getOne('m-banner').get('sitelinks').trigger('change');

            clock.tick(0);

            expect(bemBlock._sitelinksInited).to.be.true;
        });

        it('В баннере должен содержаться один блок b-banner-sitelinks', function() {
            expect(bemBlock.findBlocksInside('b-banner-sitelinks').length).to.be.equal(1);
        });
    });

    describe('Работа с b-banner-pic', function() {
        var clock;

        before(function() {
            createBlock({});
            clock = sinon.useFakeTimers();
        });

        after(function() {
            destructBlock();
            clock.restore();
        });

        it('При инициализации image_model должна вызваться функция, инициализирующая b-banner-pic', function() {

            BEM.MODEL.getOne('m-banner').get('image_model').trigger('change');

            clock.tick(0);

            expect(bemBlock._imageInited).to.be.true;
        });

        it('В баннере должен содержаться один блок b-banner-pic', function() {
            expect(bemBlock.findBlocksInside('b-banner-pic').length).to.be.equal(1)
        });

        it('При изменении image_model должен удаляться модификатор ошибки валидации', function() {

            var adb = bemBlock.findBlockInside('additions', 'b-model-form-error').setMod('with-error');

            BEM.MODEL.getOne('m-banner').get('image_model').trigger('change');

            clock.tick(0);

            expect(adb).to.not.haveMod('with-error');
        });
    });

    describe('b-edit-banner__mobile', function() {
        var defaultParams = { withMobile: true },
            banner = $.extend({ isNewBanner: true }, defaultBannerData);

        afterEach(function() {
            destructBlock();
        });

        it('должен присутствовать если ctx.withMobile выставлен в true', function() {
            createBlock({ withMobile: true }, banner);
            expect(bemBlock.elem('mobile').length).to.be.equal(1);
        });

        it('должен отсутствовать если ctx.withMobile не выставлен в true', function() {
            createBlock({}, banner);
            expect(bemBlock.elem('mobile').length).to.be.equal(0);
        });

        it('должен присутствовать в профессиональном интерфейсе', function() {
            createBlock(defaultParams, banner);
            expect(bemBlock.elem('mobile').length).to.be.equal(1);
        });

        describe('при создании объявления', function() {
            var cbx;

            before(function() {
                createBlock(defaultParams, banner);
                cbx = bemBlock.findBlockOn('mobile-checkbox', 'checkbox');
            });

            after(function() {
                destructBlock();
            });

            it('должен быть активен', function() {
                expect(cbx).to.not.haveMod('disabled', 'yes');
            });

            it('должен быть выключен', function() {
                expect(cbx).to.not.haveMod('checked', 'yes');
            });
        });

        describe('при редактировании объявления', function() {
            var editBanner,
                cbx,
                createEditBlock = function() {
                    createBlock(defaultParams, editBanner);
                    cbx = bemBlock.findBlockOn('mobile-checkbox', 'checkbox');
                };

            before(function() {
                editBanner = $.extend({}, banner, { isNewBanner: false });
            });

            describe('если объявление мобильное', function() {
                before(function() {
                    editBanner.banner_type = 'mobile';
                });

                afterEach(function() {
                    destructBlock();
                });

                it('должен присутствовать', function() {
                    createEditBlock();
                    expect(bemBlock.elem('mobile').length).to.be.equal(1);
                });

                it('должен быть включен', function() {
                    createEditBlock();
                    expect(cbx.hasMod('checked', 'yes')).to.be.ok;
                });

                it('должен быть задизейблен', function() {
                    createEditBlock();
                    expect(cbx.hasMod('disabled', 'yes')).to.be.ok;
                });
            });

            describe('если объявление десктопное', function() {
                before(function() {
                    editBanner.banner_type = 'desktop';
                });

                after(function() {
                    destructBlock();
                });

                it('должен отсутствовать', function() {
                    createEditBlock();
                    expect(bemBlock.elem('mobile').length).to.be.equal(0);
                });
            });
        });
    });

    describe('Предупреждения о некорректном регионе', function() {
        var clock;

        beforeEach(function() {
            clock = sinon.useFakeTimers();
            createBlock({});

            //даем время выполниться запросу из инициализации
            clock.tick(1000);
        });

        afterEach(function() {
            clock.restore();
            destructBlock();
        });

        it('При изменении заголовка вызывается ajax-запрос getGeoRestrictions', function() {
            requests = [];
            bemBlock.model.set('title', 'title');

            clock.tick(1000);

            var requestData = JSON.parse(u.parseRequestBody(requests[0].requestBody).json_data)[0];

            expect(requestData.text).to.be.eql(' title+');
            expect(requestData.geo).to.be.eql('1');
        });

        it('При изменении текста вызывается ajax-запрос getGeoRestrictions', function() {
            requests = [];
            bemBlock.model.set('body', 'body');

            clock.tick(1000);

            var requestData = JSON.parse(u.parseRequestBody(requests[0].requestBody).json_data)[0];

            expect(requestData.text).to.be.eql('body +');
            expect(requestData.geo).to.be.eql('1');
        });

        it('При изменении региона вызывается ajax-запрос getGeoRestrictions', function() {
            requests = [];
            geoModel.set('geo', '2');

            clock.tick(1000);

            var requestData = JSON.parse(u.parseRequestBody(requests[0].requestBody).json_data)[0];

            expect(requestData.text).to.be.eql(' +');
            expect(requestData.geo).to.be.eql('2');
        });

    });

    describe('Работа с видео', function() {
        var videoBannerData,
            clock;

        beforeEach(function() {
            clock = sinon.useFakeTimers();
            videoBannerData = {
                video_resources: {
                    id: 'testId',
                    name: 'testVideoName',
                    resource_type: 'creative'
                }
            };
            createBlock({});

            //даем время выполниться запросу из инициализации
            clock.tick(1000);
        });

        afterEach(function() {
            clock.restore();
            destructBlock();
        });

        describe('Взаимодействие с b-video-extension', function() {

            it('при генерации add, меняется модель', function() {
                var blockVideoExt = bemBlock.findBlockOn('addition-video-control', 'b-video-extension'),
                    newVideoData = { id: 'newTestId'};

                blockVideoExt.trigger('add', newVideoData);

                expect(bannerModel.get('video_resources')).to.deep.eq(newVideoData);
            });

            it('при генерации delete, очищается поле в модели', function() {
                var blockVideoExt = bemBlock.findBlockOn('addition-video-control', 'b-video-extension');

                blockVideoExt.trigger('delete');

                expect(bannerModel.get('video_resources')).to.deep.eq({});
            });
        });
    });

    describe('Работа с турбостраницами', function() {
        var turboLandingData,
            turboLandingBannerData;

        beforeEach(function() {
            turboLandingData = {
                isTurboLandingEnabled: 1,
                turbolandings: [
                    { id: 'id1', name: 'name1', href: 'href' },
                    { id: 'id2', name: 'name2', href: 'href' },
                    { id: 'id3', name: 'name3', href: 'href' },
                    { id: 'id4', name: 'name4', href: 'href' },
                    { id: 'id5', name: 'name5', href: 'href' }
                ]
            };
            turboLandingBannerData = {
                turbolanding: {
                    id: 'id1',
                    name: 'name1',
                    is_disabled: 0
                }
            };
        });

        after(function() {
            destructBlock();
        });

        it('Если флаг отключен, то взаимодействие с лендингами недоступно', function() {
            turboLandingData.isTurboLandingEnabled = 0;
            createBlock(turboLandingData, turboLandingBannerData);

            expect(bemBlock).to.not.haveElem('turbo-landings');
        });

        describe('Взаимодействие с b-turbo-landings-selector', function() {

            it('при генерации change, меняется модель', function() {
                createBlock(turboLandingData, turboLandingBannerData);
                var blockTurboLand = bemBlock.findBlockInside('turbo-landings-control', 'b-turbo-landings-selector'),
                    newLandData = { id: 'id2', href: 'ya.ru', name: 'lala' };

                blockTurboLand.trigger('change', newLandData);

                expect(bannerModel.get('turbolanding').provideData()).to.deep.eq(newLandData);
            });

            it('при изменении модели, меняется значение в контроле', function() {
                createBlock(turboLandingData, turboLandingBannerData);
                var blockTurboLand = bemBlock.findBlockInside('turbo-landings-control', 'b-turbo-landings-selector'),
                    newLandData = { id: 'id2', is_disabled: 0 };

                bannerModel.set('turbolanding', newLandData);

                expect(blockTurboLand.getValue().id).to.deep.eq(newLandData.id);
            });

            it('Если отсутствует ссылка у объявления, то контрол выключен', function() {
                createBlock(turboLandingData, turboLandingBannerData);
                var blockTurboLand = bemBlock.findBlockInside('turbo-landings-control', 'b-turbo-landings-selector');

                bannerModel.get('href_model').set('href', '');

                expect(blockTurboLand).to.haveMod('disabled');
            });

            it('Если присутствует ссылка у объявления, то контрол включен', function() {
                createBlock(turboLandingData, turboLandingBannerData);
                var blockTurboLand = bemBlock.findBlockInside('turbo-landings-control', 'b-turbo-landings-selector');

                bannerModel.get('href_model').set('href', 'https://ya.ru');

                expect(blockTurboLand).to.not.haveMod('disabled');
            });

        });

    });

});

