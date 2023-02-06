describe('b-edit-banner2_type_mobile-content', function() {
    var sandbox,
        block,
        campaignModel,
        groupModel,
        createBlock = function(ctxOptions, bannerOptions) {
            var bannerModel;

            campaignModel = BEM.MODEL.create(
                {
                    name: 'dm-mobile-content-campaign',
                    id: 'campaign-id'
                },
                {
                    cid: 'campaign-id',
                    strategy: {
                        search: {},
                        net: {}
                    },
                    mediaType: 'mobile_content'
                });

            groupModel = BEM.MODEL.create(
                {
                    name: 'dm-mobile-content-group',
                    id: 'group-id'
                },
                {
                    adgroup_type: 'mobile_content',
                    banners: [ $.extend({}, { modelId: 'banner-id' }, bannerOptions) ],
                    mobile_content: {
                        icon_url: '//avatars.mds.yandex.net/get-google-play-app-icon/39706/06ec32a477f5946291ee01036a5046bf/icon',
                        rating: 4.49
                    }
                });

            bannerModel = groupModel.getBanners()[0];

            //в коде попап для pic-selector-control инициализируется в p-multiedit2_type_mobile-content.bemtree.xjst
            u.createBlock({
                block: 'b-outboard-controls',
                js: { id: 'pic-selector-control' },
                attrs: { id: 'pic-selector-control' },
                content: {
                    elem: 'popup',
                    buttons: false,
                    header: false,
                    innerBlock: {}
                }
            },
            { inject: true });

            return u.createBlock(
                u._.extend(
                    {},
                    {
                        block: 'b-edit-banner2',
                        mods: {
                            type: 'mobile-content'
                        },
                        urlLengthLimit: 1024,
                        index: 0,
                        campaign: campaignModel.toJSON(),
                        group: groupModel.provideData(),
                        banner: bannerModel.provideData(),
                        canEditDomain: true,
                        isCopyGroup: false,
                        isSingleGroup: false,
                        availableBannerCount: 10
                    },
                    ctxOptions || {}
                ),
                { inject: true }
            );
        },
        destructBlock = function() {
            block.domElem && block.destruct();
            groupModel.destruct();
            campaignModel.destruct();
        };

    beforeEach(function() {
        var constsStub;

        sandbox = sinon.sandbox.create({
            useFakeTimers: true,
            useFakeServer: true
        });

        constsStub = u.stubCurrencies2(sandbox);

        constsStub.withArgs('rights').returns({});
        constsStub.withArgs('NEW_MAX_TITLE_LENGTH_MOBILE').returns(35);
        constsStub.withArgs('MAX_BODY_LENGTH_MOBILE').returns(75);
    });

    afterEach(function() {
        destructBlock();
        sandbox.restore();
    });

    describe('Инициализация блока с параметрами по умолчанию', function() {
        beforeEach(function() {
            block = createBlock();
        });

        u._.forOwn({
            'ad-type': 'Тип объявления',
            'image-ad': 'Креатив',
            'mobile-content-title': 'Заголовок',
            'body': 'Текст объявления',
            'mobile-content-tracking-href': 'Трекинговая ссылка',
            'mobile-content-additions': 'Дополнительно',
            'age-label': 'Возрастные ограничения'
        }, function(value, key) {
            it('Баннер должен содержать поле ' + value, function() {
                expect(block).to.haveElem(key);
            });
        });
    });

    describe('Шапка баннера', function() {
        it('Не должна содержать галочку Мобильное объявление', function() {
            block = createBlock();

            expect(block).not.to.haveElem('mobile');
        });

        describe('Контрол удаления баннера', function() {
            it('Доступен при инициализации блока с параметром canDelete = true', function() {
                block = createBlock({
                    canDelete: true
                });

                expect(block).to.haveElem('delete-btn');
            });

            it('Отстуствует при инициализации блока с параметром canDelete = false', function() {
                block = createBlock({
                    canDelete: false
                });

                expect(block).not.to.haveElem('delete-btn');
            });

            it('Корректно работает елсли данные банера не были изменены', function() {
                var spy;

                block = createBlock({
                    canDelete: true
                });

                spy = sandbox.spy(block, 'trigger');

                block.elem('delete-btn').click();
                sandbox.clock.tick(1);

                expect(spy.calledWith('remove')).to.be.true;
            });

            it('Показывает предупреждение, если данные банера были изменены', function() {
                sandbox.stub(BEM.blocks['b-confirm'], 'open').callsFake(function() {});

                block = createBlock({
                    canDelete: true
                });

                block.model.set('title', 'Название');
                block.elem('delete-btn').click();
                sandbox.clock.tick(1);

                expect(BEM.blocks['b-confirm'].open.called).to.be.true;
            });

            it('Если данные баннера были изменены и пользователь согласился с предупреждением - триггерит событие remove', function() {
                var spy;

                sandbox.stub(BEM.blocks['b-confirm'], 'open').callsFake(function(options, callbackCtx) {
                    options.onYes.apply(callbackCtx);
                });

                block = createBlock({
                    canDelete: true
                });

                spy = sandbox.spy(block, 'trigger');

                block.model.set('title', 'Название');
                block.elem('delete-btn').click();
                sandbox.clock.tick(1);

                expect(spy.calledWith('remove')).to.be.true;
            });
        });

        describe('Ссылка Очистить объявление', function() {
            it('Доступна при инициализации блока с параметром hasHeaderActions = true', function() {
                block = createBlock({
                    hasHeaderActions: true
                });

                expect(block).to.haveElem('clear-text');
            });

            it('Недоступна если hasHeaderActions = false или отсутсвует', function() {
                block = createBlock();

                expect(block).not.to.haveElem('clear-text');
            });

            it('Отстуствует, если баннер графический и не новый', function() {
                block = createBlock(
                    {
                        hasHeaderActions: true
                    },
                    {
                        ad_type: 'image_ad',
                        isNewBanner: false
                    }
                );

                expect(block).not.to.haveElem('clear-text');
            });

            it('Доступна, если баннер графический и новый', function() {
                block = createBlock(
                    {
                        hasHeaderActions: true
                    },
                    {
                        ad_type: 'image_ad',
                        isNewBanner: true
                    }
                );

                expect(block).to.haveElem('clear-text');
            });

            it('Корректно работает', function() {
                var blockModel;

                block = createBlock(
                    {
                        hasHeaderActions: true
                    },
                    {
                        title: 'Название',
                        body: '123',
                        href: 'appmetrica.yandex.com/serve/1177090425626855822?click_id={LOGID}',
                        video_resources: {
                            id: 'testId',
                            name: 'testVideoName',
                            resource_type: 'creative'
                        }
                    }
                );

                block.elem('clear-text').click();
                sandbox.clock.tick(1);

                blockModel = block.model;

                expect(blockModel.get('ad_type')).equal('text');
                expect(blockModel.get('title')).to.be.empty;
                expect(blockModel.get('body')).to.be.empty;
                expect(blockModel.get('href')).to.be.empty;
                expect(blockModel.get('video_resources')).to.deep.equal({});

                [
                    blockModel.get('image_model'),
                    blockModel.get('href_model'),
                    blockModel.get('sitelinks'),
                    blockModel.get('image_ad'),
                    blockModel.get('creative')
                ].forEach(function(model) {
                    var spy = sandbox.spy(model, 'clear');

                    expect(spy).to.be.called;
                });
            });
        });

        it('Ссылка Cкопировать объявление недоступна всегда', function() {
            block = createBlock(
                {
                    hasHeaderActions: true
                },
                {
                    hasCopyFromPrev: true
                }
            );

            expect(block).not.to.haveElem('copy-from-prev');
        });

    });

    it('При удалении модели баннера - блок НЕ удаляется, но удаляется его domElem', function() {
        block = createBlock();

        block.model.destruct();

        expect(block).not.to.equal(undefined);
        expect(block.domElem).to.equal(undefined);
    });

    describe('Счетчик оставшихся символов', function() {
        beforeEach(function() {
            block = createBlock();
        });

        [
            {
                name: 'title',
                limit: 35
            },
            {
                name: 'body',
                limit: 75
            }
        ].forEach(function(item) {
            it('Корректно инициализируется для поля ' + item.name, function() {
                expect(+block.elem(item.name + '-length-counter').text()).to.equal(item.limit);
            });

            it('Корректно обновляется для поля ' + item.name, function() {
                block.getFieldBlock(item.name).input.val('Ааа');
                sandbox.clock.tick(1);

                expect(+block.elem(item.name + '-length-counter').text()).to.equal(item.limit - 3);
            });
        });

        it('Подсвечивается, если счетчик отрицательный', function() {
            block.getFieldBlock('title').input.val(new Array(100).join('a'));
            sandbox.clock.tick(1);

            expect(block.getMod(block.elem('title-length-counter'), 'under-zero')).to.equal('yes');
        });

        it('Не учитывает символы ##, используемые для шаблонов при подсчете', function() {
            block.getFieldBlock('title').input.val('aa #123# bb');
            sandbox.clock.tick(1);

            expect(+block.elem('title-length-counter').text()).to.equal(26);
        });
    });

    describe('Предупреждение о соответствии региона и языка заголовка', function() {
        beforeEach(function() {
            block = createBlock();
        });

        it('При изменении текста объявления присходит его серверная валидация на соответсвие языка региону', function() {
            var spy = sandbox.spy(block._geoRestrictionsRequest, 'get');

            block.getFieldBlock('body').input.val('www');

            expect(spy.calledWith({ text: 'www', geo: '' })).to.be.true;
        });

        it('При изменении установок региона объявления присходит серверная валидация на соответсвие языка текста объявления региону', function() {
            var spy = sandbox.spy(block._geoRestrictionsRequest, 'get');

            block._geoModel.set('geo', '255');

            expect(spy).to.be.called;
        });

        it('Если сервер ответил c информацией об ошибке - тригеррится событие geo-check-error с коррекным messages', function() {
            var spy = sandbox.spy(block.model.getDM()['dm-mobile-content-banner'], 'trigger');

            sandbox.stub(block._geoRestrictionsRequest, 'get').callsFake(function(options, successCallback, errorCallback) {
                successCallback({
                    warning: 'Warning!'
                });
            });

            block.getFieldBlock('body').input.val('www');

            expect(spy.calledWith('geo-check-error', { messages: 'Warning!' })).to.be.true;
        });

        it('Если сервер вернул ошибочный статус - тригеррится событие geo-check-error с пустым messages', function() {
            var spy = sandbox.spy(block.model.getDM()['dm-mobile-content-banner'], 'trigger');

            sandbox.stub(block._geoRestrictionsRequest, 'get').callsFake(function(options, successCallback, errorCallback) {
                errorCallback();
            });

            block.getFieldBlock('body').input.val('www');

            expect(spy.calledWith('geo-check-error', { messages: '' })).to.be.true;
        });

        it('Если баннер графический, то сообщения не должно выводиться', function() {
            var spy = sandbox.spy(block.model.getDM()['dm-mobile-content-banner'], 'trigger');

            block.getFieldBlock('body').input.val('www');
            block.model.set('ad_type', 'image_ad');

            expect(spy.calledWith('geo-check-error', { messages: '' })).to.be.true;
        });
    });

    describe('Предупреждение о неподдерживемой валюте', function() {
        var spy;

        beforeEach(function() {
            block = createBlock();
            spy = sandbox.spy(block.model.getDM()['dm-mobile-content-banner'], 'trigger');
        });

        describe('Если галочка цена в разделе Дополнительно отмечена', function() {
            beforeEach(function(){
                block.model.set('reflected_attrs', ['price']);
            });

            it('Если валюта неподдерживаемая - предупреждение показывается', function() {
                block.model.getMobileContentModelForGroup().set('isCurrencySupported', false);

                expect(spy.calledWith('currency-check-error', { messages: [ 'Цена не отображается, т.к. указана ссылка на приложение с неподдерживаемой валютой' ] })).to.be.true;
            });

            it('Если валюта поддерживаемая - предупреждение не показывается', function() {
                block.model.getMobileContentModelForGroup().set('isCurrencySupported', true);

                expect(spy.calledWith('currency-check-error', { messages: [] })).to.be.true;
            });

            it('Если валюта неподдерживаемая, но объявление графическое - предупреждение не показывается', function() {
                block.model.getMobileContentModelForGroup().set('isCurrencySupported', false);
                block.model.set('ad_type', 'image_ad');

                expect(spy.calledWith('currency-check-error', { messages: [] })).to.be.true;
            });
        });

        it('Если галочка цена в разделе Дополнительно не отмечена  - предупреждение не показывается', function() {
            block.model.getMobileContentModelForGroup().set('isCurrencySupported', false);
            block.model.set('reflected_attrs', []);

            expect(spy.calledWith('currency-check-error', { messages: [] })).to.be.true;
        })
    });

    describe('Если изменить тип объявления', function() {
        beforeEach(function() {
            block = createBlock();
            block.model.set('ad_type', 'image_ad');
        });

        it('то меняется вид превью баннера', function() {
            expect(block.findBlockOn('banner-viewer', 'b-banner-viewer')).to.haveMod('current-view', 'base-image');
        });

        it('то устанавливается корректный модификатор для блока', function() {
            expect(block).to.haveMod('ad-type', 'image-ad');
        });
    });

    describe('Графическое объявление', function() {
        var imageAdLoader;

        beforeEach(function() {
            block = createBlock({}, { ad_type: 'image_ad' });

            imageAdLoader = block.findBlockOn('image-ad-loader', 'b-image-add-loader');
        });

        describe('При событии change:creatives блока b-image-add-loader', function() {
            beforeEach(function() {
                imageAdLoader.trigger('change:creatives', {
                    images: [{
                        hash: 'hash',
                        name: 'name',
                        width: 300,
                        height: 300,
                        scale: 0.5,
                        group_id: 'group_id'
                    }]
                })
            });

            it('Должна очищаться модель creative', function() {
                var spy = sandbox.spy(block.model.get('creative'), 'clear');

                expect(spy).to.be.called;
            });

            it('Должна корректно обновляться модель поля image_ad', function() {
                expect(block.model.get('image_ad').toJSON()).to.deep.equal({
                    hash: 'hash',
                    name: 'name',
                    width: 300,
                    height: 300,
                    scale: 0.5,
                    group_id: 'group_id'
                });
            });
        });

        describe('При событии change:creatives блока b-image-add-loader', function() {
            var newCreativeData = {
                    creative_id: '1262172',
                    name: 'Новая группа',
                    preview_url: 'https://avatars.mds.yandex.net/lll',
                    scale: 0.35,
                    width: 1000,
                    height: 120,
                    creative_type: 'some',
                    composed_from: '',
                    live_preview_url: '',
                    duration: '',
                    cpmSubtype: '',
                    has_packshot: false,
                    is_adaptive: '0'
                },
                triggerChangeCreativeEvent = function(creativeArray) {
                    imageAdLoader.trigger('change:creatives', {
                        creatives: creativeArray,
                        href: {
                            href: 'https://ya.ru',
                            href_domain: 'ya.ru',
                            protocol: 'https://'
                        }
                    });
                };

            it('Должна очищаться модель image_ad', function() {
                var spy = sandbox.spy(block.model.get('image_ad'), 'clear');

                triggerChangeCreativeEvent([newCreativeData]);

                expect(spy).to.be.called;
            });

            it('Должна корректно обновляться модель поля creative', function() {
                triggerChangeCreativeEvent([newCreativeData]);

                var newCreativeModel = block.model.get('creative').toJSON();

                delete newCreativeModel.outdoorResolutions;

                expect(newCreativeModel).to.deep.eq(newCreativeData);
            });

            it('Если в событии пришли данные о ссылке, то поля модели баннера href, domain, url_protocol корректно обновляются', function() {
                triggerChangeCreativeEvent([newCreativeData]);

                expect(block.model.get('href')).to.equal('https://ya.ru');
                expect(block.model.get('domain')).to.equal('ya.ru');
                expect(block.model.get('url_protocol')).to.equal('https://');
            });

            it('Если в событии креативов в данных > 1, то триггерится событие new:banners:needed', function() {
                var spy = sandbox.spy(block, 'trigger');

                triggerChangeCreativeEvent([newCreativeData, newCreativeData]);

                expect(spy.calledWith(
                    'new:banners:needed',
                    [
                        {
                            ad_type: 'image_ad',
                            creative: newCreativeData,
                            href: 'https://ya.ru',
                            domain: 'ya.ru',
                            url_protocol: 'https://'
                        }
                    ])).to.be.true;
            });

            it('Если в событии 1 креатив, то new:banners:needed НЕ триггерится', function() {
                var spy = sandbox.spy(block, 'trigger');

                triggerChangeCreativeEvent([newCreativeData]);

                expect(spy).not.to.be.called;
            })
        });
    });

    describe('Синхронизация с DM', function() {
        var dm;

        beforeEach(function() {
            block = createBlock();

            dm = block.model.getDM()['dm-mobile-content-banner'];
        });

        [
            {
                name: 'title',
                value: 'Название'
            },
            {
                name: 'body',
                value: 'Текст'
            },
            {
                name: 'url_protocol',
                value: 'https://'
            },
            {
                name: 'reflected_attrs',
                value: [ 4.49, '//avatars.mds.yandex.net/get-google-pl']
            },
            {
                name: 'image_model',
                value: {
                    image: 'image',
                    image_name: 'imageName',
                    image_type: 'imageType',
                    source_image: 'sourceImage',
                    image_source_url: 'imageSourceUrl',
                    image_width: 300,
                    image_height: 300,
                    image_processing_state: 'progress',
                    mds_group_id: '111'
                }
            },
            {
                name: 'image_ad',
                value: {
                    hash: 'hash',
                    name: 'name',
                    width: 300,
                    height: 300,
                    scale: 0.5,
                    group_id: 'group_id'
                }
            },
            {
                name: 'creative',
                value: {
                    creative_id: '1262172',
                    name: 'Новая группа',
                    preview_url: 'https://avatars.mds.yandex.net/lll',
                    scale: 0.35,
                    width: 1000,
                    height: 120,
                    creative_type: 'canvas',
                    composed_from: '',
                    live_preview_url: '',
                    duration: '',
                    cpmSubtype: '',
                    has_packshot: false,
                    is_adaptive: '0'
                }
            },
            {
                name: 'ad_type',
                value: 'image_ad'
            }
        ].forEach(function(field) {
            it('При изменении поля ' + field.name + ' в модели баннера - происходит изменение значения соответсвующего поля в DM', function() {
                var dmField;

                block.model.set(field.name, field.value);

                dmField = dm.get(field.name);

                if (dmField.toJSON) {
                    var dmFieldJSON = dmField.toJSON();

                    if (field.name === 'creative') {
                        delete dmFieldJSON.outdoorResolutions;
                    }

                    expect(dmFieldJSON).to.deep.equal(field.value);
                } else if (Array.isArray(dmField)) {
                    expect(dmField + '').to.equal(field.value + '');
                } else {
                    expect(dmField).to.equal(field.value);
                }
            });

            it('При изменении поля ' + field.name + ' в DM - происходит изменение значения соответсвующего поля в модели баннера', function() {
                var blockModelField;

                dm.set(field.name, field.value);

                blockModelField = block.model.get(field.name);

                if (blockModelField.toJSON) {
                    var blockModelFieldJSON = blockModelField.toJSON();

                    if (field.name === 'creative') {
                        delete blockModelFieldJSON.outdoorResolutions;
                    }

                    expect(blockModelFieldJSON).to.deep.equal(field.value);
                } else if (Array.isArray(blockModelField)) {
                    expect(blockModelField + '').to.equal(field.value + '');
                } else {
                    expect(blockModelField).to.equal(field.value);
                }
            });
        });

        it('При изменении поля ageLabel в модели баннера - происходит изменение значения hash_flags поля в DM', function() {
            block.model.set('ageLabel', '12+');

            expect(dm.get('hash_flags').age).to.equal('12');
        });

        it('При изменении поля hash_flags.age в DM - происходит изменение значения поля ageLabel в модели баннера', function() {
            dm.set('hash_flags', { age: '16' });

            expect(block.model.get('ageLabel')).to.equal('16+');
        });
    });

    it('Метод toJSONForErrors возвращает значения полей isNewBanner, newBannerIndex, bid, modelId модели баннера', function() {
        block = createBlock();

        expect(block.model.toJSONForErrors()).to.deep.equal({
            isNewBanner: block.model.get('isNewBanner'),
            newBannerIndex: block.model.get('newBannerIndex'),
            bid: block.model.get('bid'),
            modelId: block.model.get('modelId')
        });
    });
});
