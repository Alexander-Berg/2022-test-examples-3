describe('b-edit-banner2_type_dynamic', function() {
    var sandbox,
        block,
        sitelinksPopup,
        campaignModel,
        groupModel,
        createBlock = function(ctxOptions, bannerOptions) {
            var bannerModel;

            campaignModel = BEM.MODEL.create(
                {
                    name: 'm-campaign',
                    id: 'campaign-id'
                },
                {
                    cid: 'campaign-id',
                    strategy: {
                        search: {},
                        net: {}
                    },
                    mediaType: 'dynamic'
                });

            groupModel = BEM.MODEL.create(
                {
                    name: 'dm-dynamic-group',
                    id: 'group-id'
                },
                {
                    adgroup_type: 'dynamic',
                    main_domain: 'hrefValue',
                    banners: [$.extend(
                        {},
                        {
                            modelId: 'banner-id',
                            vcard: {
                                city: 'Санкт-Петербург',
                                city_code: '812',
                                country: 'Россия'
                            }
                        },
                        bannerOptions)]
                });

            bannerModel = groupModel.getBanners()[0];

            //в коде попап для сайтлинков инициализируется в p-multiedit2_type_dynamic.bemtree.xjst
            sitelinksPopup = u.createBlock({
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
                },
                { inject: true });

            return u.createBlock(
                u._.extend(
                    {},
                    {
                        block: 'b-edit-banner2',
                        mods: {
                            type: 'dynamic'
                        },
                        index: 0,
                        campaign: campaignModel.toJSON(),
                        group: groupModel.provideData(),
                        banner: bannerModel.provideData(),
                        withVCard: true,
                        canDelete: false,
                        canEditDomain: true,
                        isCopyGroup: false,
                        isSingleGroup: false,
                        errorPath: 'groups.banners[0]'
                    },
                    ctxOptions || {}
                ),
                { inject: true }
            );
        },
        destructBlock = function() {
            block.domElem && block.destruct();
            sitelinksPopup.destruct();
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
        constsStub.withArgs('MAX_BODY_LENGTH').returns(75);
    });

    afterEach(function() {
        destructBlock();
        sandbox.restore();
    });

    it('Заголовок баннера недоступен для редактирования', function() {
        block = createBlock();

        expect(block.findBlockOn('title', 'input')).to.haveMod('disabled', 'yes');
    });

    it('При удалении модели баннера - блок НЕ удаляется, но удаляется его domElem', function() {
        block = createBlock();

        block.model.destruct();

        expect(block).not.to.equal(undefined);
        expect(block.domElem).to.equal(undefined);
    });

    describe('Инициализация блока', function() {
        beforeEach(function() {
            block = createBlock();
        });

        u._.forOwn({
            'dynamic-title': 'Заголовок',
            'body': 'Текст объявления',
            'additions': 'Дополнения'
        }, function(value, key) {
            it('Баннер должен содержать поле ' + value, function() {
                expect(block).to.haveElem(key);
            });
        });

        describe('Дополнения', function() {
            u._.forOwn({
                'banner-pic': false,
                'b-video-extension': false,
                'b-banner-sitelinks': true,
                'b-banner-callouts': true
            }, function(value, key) {
                it('Блок ' + key + (value ? '' : ' не') + ' должен присутствовать', function() {
                    expect(block.findBlocksInside(key).length > 0).to.equal(value);
                });
            });
        });
    });

    describe('Модель', function() {
        beforeEach(function() {
            block = createBlock({}, {
                body: 'Текст'
            });
        });

        it('Заголовок баннера корректно инициализируется', function() {
            expect(block.model.get('title')).to.equal(u.dynamicGroupsData.getBannerTitle());
        });

        it('Текст баннера корректно инициализируется', function() {
            expect(block.model.get('body')).to.equal('Текст');
        });

        it('Поля, ответственные за ссылку корректно инициализируются', function() {
            var hrefData = block.model.get('href_model').toJSON();

            expect(block.model.get('domain')).to.equal('hrefValue');
            expect(hrefData.href).to.equal('hrefValue');
            expect(hrefData.domain).to.equal('hrefValue');
            expect(hrefData.url_protocol).to.equal('http://');
        });

        it('При изменении поля main_domain группы: поля, ответственные за ссылку корректно меняют свои значения', function() {
            var hrefData;

            groupModel.set('main_domain', '');

            hrefData = block.model.get('href_model').toJSON();

            expect(block.model.get('domain')).to.equal('');
            expect(hrefData.href).to.equal('');
            expect(hrefData.domain).to.equal('');
            expect(hrefData.url_protocol).to.equal('http://');
        });
    });

    describe('Если передан параметр withVCard = true', function() {
        beforeEach(function() {
            block = createBlock({
                withVCard: true
            });
        });

        it('Баннер должен содержать поле Визитка', function() {
            expect(block).to.haveElem('vcard');
        });

        it('У блока должен быть модификатор with-vcard = yes', function() {
            expect(block).to.haveMod('with-vcard', 'yes');
        })
    });

    describe('Если передан параметр withVCard = false', function() {
        beforeEach(function() {
            block = createBlock({
                withVCard: false
            });
        });

        it('Баннер НЕ должен содержать поле Визитка', function() {
            expect(block).not.to.haveElem('vcard');
        });

        it('У блока НЕ должно быть модификатора with-vcard = yes', function() {
            expect(block).not.to.haveMod('with-vcard', 'yes');
        })
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

            it('Триггерит событие remove, если визитка пустая и данные баннера не были изменены', function() {
                var spy;

                block = createBlock({
                    canDelete: true
                });

                // fix - потому, что нужно, чтобы isChanged модели был равен false
                block.model.set('isVCardEmpty', true).fix();

                spy = sandbox.spy(block, 'trigger');

                block.elem('delete-btn').click();
                sandbox.clock.tick(1);

                expect(spy.calledWith('remove')).to.be.true;
            });

            it('Перед удалением баннера показывается предупреждение, если визитка не пустая', function() {
                sandbox.stub(BEM.blocks['b-confirm'], 'open').callsFake(function() {});

                block = createBlock({
                    canDelete: true
                });

                block.elem('delete-btn').click();
                sandbox.clock.tick(1);

                expect(BEM.blocks['b-confirm'].open.called).to.be.true;
            });

            it('Перед удалением баннера показывается предупреждение, если данные банера были изменены', function() {
                sandbox.stub(BEM.blocks['b-confirm'], 'open').callsFake(function() {});

                block = createBlock({
                    canDelete: true
                });

                block.model.set('body', 'Текст');
                block.model.set('isVCardEmpty', true);
                block.elem('delete-btn').click();
                sandbox.clock.tick(1);

                expect(BEM.blocks['b-confirm'].open.called).to.be.true;
            });

            it('Если данные баннера были изменены и пользователь согласился с предупреждением - тригерит событие remove', function() {
                var spy;

                sandbox.stub(BEM.blocks['b-confirm'], 'open').callsFake(function(options, callbackCtx) {
                    options.onYes.apply(callbackCtx);
                });

                block = createBlock({
                    canDelete: true
                });

                spy = sandbox.spy(block, 'trigger');

                block.model.set('body', 'Текст');
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

            it('Очищает поля объявления', function() {
                var blockModel,
                    spy;

                block = createBlock(
                    {
                        hasHeaderActions: true
                    },
                    {
                        body: '123'
                    }
                );

                block.elem('clear-text').click();
                sandbox.clock.tick(1);

                blockModel = block.model;

                expect(blockModel.get('body')).to.be.empty;

                [
                    blockModel.get('image_model'),
                    blockModel.get('sitelinks')
                ].forEach(function(model) {
                    var spy = sandbox.spy(model, 'clear');

                    expect(spy).to.be.called;
                });

                expect(sandbox.spy(block.findBlockInside('b-vcard-control'), 'clear')).to.be.called;
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

    describe('Счетчик оставшихся символов', function() {
        beforeEach(function() {
            block = createBlock();
        });

        it('Корректно инициализируется для поля Текст объявления', function() {
            expect(+block.elem('body-length-counter').text()).to.equal(75);
        });

        it('Корректно обновляется для поля Текст объявления', function() {
            block.getFieldBlock('body').input.val('Ааа');
            sandbox.clock.tick(1);

            expect(+block.elem('body-length-counter').text()).to.equal(72);
        });

        it('Подсвечивается, если счетчик отрицательный', function() {
            block.getFieldBlock('body').input.val(new Array(100).join('a'));
            sandbox.clock.tick(1);

            expect(block.getMod(block.elem('body-length-counter'), 'under-zero')).to.equal('yes');
        });

        it('Не учитывает символы ##, используемые для шаблонов при подсчете', function() {
            block.getFieldBlock('body').input.val('aa #123# bb');
            sandbox.clock.tick(1);

            expect(+block.elem('body-length-counter').text()).to.equal(66);
        });
    });

    describe('Предупреждение о соответствии региона и языка заголовка', function() {
        beforeEach(function() {
            block = createBlock();
        });

        it('При изменении текста объявления происходит его серверная валидация на соответствие языка региону', function() {
            var spy = sandbox.spy(block._geoRestrictionsRequest, 'get');

            block.getFieldBlock('body').input.val('www');

            expect(spy.calledWith({ text: 'www', geo: '' })).to.be.true;
        });

        it('При изменении установок региона объявления происходит серверная валидация на соответствие языка текста объявления региону', function() {
            var spy = sandbox.spy(block._geoRestrictionsRequest, 'get');

            block.geoModel.set('geo', '255');

            expect(spy).to.be.called;
        });

        it('Если сервер ответил c информацией об ошибке - тригерится событие geo-check-error с корректным messages', function() {
            var spy = sandbox.spy(block.model, 'trigger');

            sandbox.stub(block._geoRestrictionsRequest, 'get').callsFake(function(options, successCallback, errorCallback) {
                successCallback({
                    warning: "Warning!"
                });
            });

            block.getFieldBlock('body').input.val('www');

            expect(spy.calledWith('geo-check-error', { messages: 'Warning!' })).to.be.true;
        });

        it('Если сервер вернул ошибочный статус - тригерится событие geo-check-error с пустым messages', function() {
            var spy = sandbox.spy(block.model, 'trigger');

            sandbox.stub(block._geoRestrictionsRequest, 'get').callsFake(function(options, successCallback, errorCallback) {
                errorCallback();
            });

            block.getFieldBlock('body').input.val('www');

            expect(spy.calledWith('geo-check-error', { messages: '' })).to.be.true;
        });
    });
});
