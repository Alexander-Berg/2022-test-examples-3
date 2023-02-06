describe('b-banner-preview', function() {
    var block,
        blockTree,
        modelParams = {
            name: 'm-banner',
            id: 'banner-id',
            parentName: 'm-group',
            parentId: 'group-id'
        },
        geoModel,
        bannerModel2,
        bannerModel,
        constStub,
        getUrlStub;

    function createBlock(bannerData, extraParams, mods) {
        bannerData = u.bannerData({ banner: bannerData });
        bannerModel = BEM.MODEL.create(modelParams,  u.bannerModelData({ banner: bannerData }));
        geoModel = BEM.MODEL.create(
            { name: 'm-geo-regions', id: modelParams.parentId },
            { geo: '1'});

        blockTree = $(BEMHTML.apply($.extend({
            block: 'b-banner-preview',
            banner: bannerData || {},
            modelParams: modelParams,
            mods: mods
        }, extraParams)));

        $('body').append(blockTree);

        block = BEM.DOM.init(blockTree).bem('b-banner-preview');
    }

    function destructBlock() {
        bannerModel && bannerModel.destruct();
        geoModel && geoModel.destruct();
        bannerModel2 && bannerModel2.destruct();

        block.destruct();
    }

    before(function() {
        BEM.DOM.scope = $('<div>').appendTo('body');
    });

    after(function() {
        BEM.DOM.destruct(BEM.DOM.scope);
        BEM.DOM.scope = $('body');
    });

    beforeEach(function() {
        constStub = sinon.stub(u, 'consts');
        getUrlStub = sinon.stub(u, 'getUrl').callsFake(function() { return 'yandex.ru'; })

        constStub
            .withArgs('AD_WARNINGS').returns({ 1: 'Предупреждение1', 2: 'ПРедупреждение2' })
            .withArgs('SITELINKS_NUMBER').returns(4)
            .withArgs('ulogin').returns('ulogin')
            .withArgs('NEW_MAX_TITLE_LENGTH').returns(35)
            .withArgs('MAX_BODY_LENGTH').returns(81)
            .withArgs('rights').returns({ ajaxAdGroupsMultiSave: false });
    });

    afterEach(function() {
        constStub.restore();
        getUrlStub.restore();
    });

    //на каком-то из этих тестов падает afterEach
    describe.skip('Проверяем превью на соответствие заданный параметр -> элемент превью', function() {
        [
            { title: 'Должен отрисоваться номер баннера', banner: { bid: 10 }, params: { number: 111 }, elemName: 'header', count: 1 },
            { title: 'Предупреждение о шаблоне, banner.is_template_banner: true', banner: { bid: 10, is_template_banner: true }, params: { }, elemName: 'warning', count: 1 },
            { title: 'Предупреждение о шаблоне, isTemplateBanner: true  ', banner: { bid: 10 }, params: { isTemplateBanner: true }, elemName: 'warning', count: 1 },
            { title: 'Предупреждение о шаблоне, hideTemplateWarning: true ', banner: { bid: 10, is_template_banner: true }, params: { hideTemplateWarning: true }, elemName: 'warning', count: 0 },
            { title: 'Картинка, регион - Турция, showThumbnail = true', banner: { bid: 10, image: '/image.jpg' }, params: { showThumbnail: true }, yandex_domain: '//yandex.com.tr', elemName: 'thumb', count: 1 },
            { title: 'Картинка, регион - не Турция', banner: { bid: 10, image: '/image.jpg' }, params: { }, elemName: 'thumb', yandex_domain: '//yandex.com', count: 0 },
            { title: 'Картинка, регион - не Турция, showThumbnail = true', banner: { bid: 10, image: '/image.jpg' }, params: { showThumbnail: true }, elemName: 'thumb', count: 1 },
            { title: 'Картинка, banner.image не определено', banner: { bid: 10 }, params: {  }, elemName: 'thumb', yandex_domain: '//yandex.com', count: 0 },
            { title: 'Предупреждения о контенте (алкоголь и т.п.)', banner: { bid: 10, hash_flags: [1, 2] }, params: {  }, elemName: 'adv-alert', count: 1 },
            { title: 'Предупреждения о контенте (алкоголь и т.п.), banner.hash_flags не определен', banner: { bid: 10 }, params: {  }, elemName: 'adv-alert', count: 0 },
            { title: 'Предупреждения о контенте (алкоголь и т.п.), hideAdWarnings: true', banner: { bid: 10, hash_flags: [1, 2] }, params: { hideAdWarnings: true }, elemName: 'adv-alert', count: 0 },
            { title: 'Возраст, isBannersEditable: true', banner: { bid: 10 }, params: { showAgeLabels: true }, elemName: 'age-label', count: 1 },
            { title: 'Возраст, isBannersEditable: false', banner: { bid: 10 }, params: { }, elemName: 'age-label', count: 0 },
            { title: 'Блок "Дополнения", нет banner.image и banner.sitelinks', banner: { bid: 10 }, params: { }, elemName: 'additions', count: 0 },
            { title: 'Блок "Дополнения" , есть banner.image', banner: { bid: 10, image: '/image.jpg' }, params: { }, elemName: 'additions',  count: 1 },
            { title: 'Блок "Дополнения", есть banner.sitelinks', banner: { bid: 10, sitelinks: [{ title: 'bla', url: 'bla.ru' }] }, params: { }, elemName: 'additions', count: 1 },
            { title: 'Блок "Дополнения", hideAdditions: true', banner: { bid: 10, sitelinks: [{ title: 'bla', url: 'bla.ru' }] }, params: { hideAdditions: true }, elemName: 'additions', count: 0 },
            { title: 'Блок "Статус", showStatus: true', banner: { bid: 10 }, params: { showStatus: true }, elemName: 'statuses', count: 1 },
            { title: 'Блок "Статус", showStatus: false', banner: { bid: 10 }, params: {  }, elemName: 'statuses', consts: {  }, count: 0 },
            { title: 'Блок actions, параметр actions не определен', banner: { bid: 10 }, params: { actions: { stopAndRemoderate: true } }, elemName: 'actions', count: 1 },
            { title: 'Блок actions, параметр actions определен', banner: { bid: 10 }, params: {  }, elemName: 'actions', count: 0 },
            { title: 'Должен отрисоваться регион', banner: { bid: 10, geo: '122, 3232' }, params: { showGeo: true }, elemName: 'geo-names', count: 1 }
        ].forEach(function(data) {
            describe(data.title, function() {
                beforeEach(function() {
                    constStub.withArgs('yandex_domain').returns(data.yandex_domain || '//yandex.ru');
                    createBlock(data.banner, data.params)
                });

                afterEach(destructBlock);

                it('Отрисовалось элементов ' + data.elemName + ' - ' + data.count, function() {
                    var elems = block.elem(data.elemName) || [];

                    expect(elems.length).to.be.equal(data.count);
                });
            });
        }, this);
    });

    describe('Если есть визитка, должен быть блок b-modal-popup-opener', function() {
        beforeEach(function() {
            createBlock({
                vcard: { city: 'bla' }
            }, {
            })
        });

        afterEach(destructBlock);

        it('В превью должен содержаться блок b-modal-popup-opener', function() {
            expect(block.findBlocksInside('b-modal-popup-opener').length).to.be.equal(1);
        });
    });

    describe('Если есть непустые флаги hash_flags ', function() {
        beforeEach(function() {
            createBlock({
                hash_flags: [1, 2]
            }, {
            })
        });

        afterEach(destructBlock);

        it('В превью должен содержаться блок b-banner-adv-alert', function() {
            expect(block.findBlocksInside('b-banner-adv-alert').length).to.be.equal(1);
        });
    });

    describe('Если showGeo = true', function() {
        beforeEach(function() {
            createBlock({
            }, {
                showGeo: true
            })
        });

        afterEach(destructBlock);

        it('В превью должен содержаться блок b-group-regions', function() {
            expect(block.findBlocksInside('b-group-regions').length).to.be.equal(1);
        });
    });

    describe('Проверяем реакцию на действия пользователя', function() {
        describe('Блок "Дополнения"', function() {
            var params = {
                block: 'b-banner-preview',
                banner: { image: 'test.jpg', sitelinks: [{ title: 'bla', href: 'bla.ru'}] },
                hideAdWarnings: true,
                hideWarnings: true,
                hideAdditions: true
            };

            beforeEach(function() {
                createBlock({ image: 'test.jpg', sitelinks: [{ title: 'bla', href: 'bla.ru'}] }, { })
            });

            afterEach(destructBlock);

            it('Клик по addition-link type image - должен открыться попап c изображением', function() {
                sinon.spy(BEM.blocks['b-shared-popup'], 'getInstance');
                sinon.spy(BEMHTML, 'apply');
                var imageParams = $.extend({}, params, {
                    hideSitelinks: true,
                    mods: { type: 'image' }
                });

                block.elem('addition-link', 'type', 'image').click();

                expect(BEM.blocks['b-shared-popup'].getInstance.called).to.be.equal(true);
                // todo DIRECT-47749
                // expect(BEMHTML.apply.calledWithExactly(imageParams)).to.be.equal(true);

                BEM.blocks['b-shared-popup'].getInstance.restore();
                BEMHTML.apply.restore();
            });

            it('Клик по addition-link type sitelinks - должен открыться попап с сайтлинками', function() {
                sinon.spy(BEM.blocks['b-shared-popup'], 'getInstance');
                sinon.spy(BEMHTML, 'apply');

                var sitelinksParams = $.extend({}, params, {
                    hideSitelinks: false,
                    mods: { type: 'sitelinks' }
                });

                block.elem('addition-link', 'type', 'sitelinks').click();

                expect(BEM.blocks['b-shared-popup'].getInstance.called).to.be.equal(true);
                // todo DIRECT-47749
                // expect(BEMHTML.apply.calledWithExactly(sitelinksParams)).to.be.equal(true);

                BEM.blocks['b-shared-popup'].getInstance.restore();
                BEMHTML.apply.restore();
            });
        });

        describe('Блок "Заголовок"', function() {
            describe('Баннер с визиткой', function() {
                beforeEach(function() {
                    createBlock({ vcard: { phone: '111' } });
                });

                afterEach(destructBlock);

                it('Клик по урлу должен открыть визитку', function(done) {
                    var popupOpener = block.findBlockInside('b-modal-popup-opener');

                    sinon.spy(popupOpener, 'show');

                    block.elem('title').click();

                    u.waitFor(function() {
                        if (popupOpener.show.called)  {
                            popupOpener.show.restore();

                            return true;
                        } else {
                            return false;
                        }
                    }, done);


                });
            });
        });

        describe('Ссылка "Остановить и перемодерировать"', function() {
            beforeEach(function() {

                createBlock({ statusModerate: 'Yes' }, {
                    actions: {
                        stopAndRemoderate: true
                    }
                });
            });

            afterEach(destructBlock);

            it('Ссылка "Остановить и перемодерировать" должна существовать', function() {
                expect(block.elem('stop-and-remoderate').length).to.be.equal(1);
            });

            it('При клике по ссылке "Остановить и перемодерировать" должен показываться конфирм', function() {
                sinon.stub(BEM.blocks['b-confirm'], 'open');

                block.elem('stop-and-remoderate').click();

                expect(BEM.blocks['b-confirm'].open.called).to.be.equal(true);

                BEM.blocks['b-confirm'].open.restore();
            });
        });

        describe('Блок "Адрес и телефон", loadVCardFromClient != true', function() {
            beforeEach(function() {
                createBlock({ bid: 10, vcard_id: 11 }, {});
            });

            afterEach(destructBlock);

            it('При клике по ссылке "Адрес и телефон" должно открыться новое окно', function(done) {
                var popupOpener = block.findBlockInside('b-modal-popup-opener');

                sinon.spy(popupOpener, 'show');

                block.elem('vcard').click();

                u.waitForAndRestore(popupOpener.show, done);
            });
        });

        describe('Блок "Адрес и телефон", loadVCardFromClient = true', function() {
            beforeEach(function() {
                createBlock({ bid: 10, loadVCardFromClient: true, vcard: { city: 'bla' } }, {  });
            });

            afterEach(destructBlock);

            it('При клике по ссылке "Адрес и телефон" должна вызваться функция getVCardData', function(done) {
                sinon.spy(BEM.blocks['b-banner-preview'], 'getVCardData');

                block.elem('vcard').click();

                u.waitForAndRestore(BEM.blocks['b-banner-preview'].getVCardData, done);

            });
        });
    });

    describe('b-banner-preview_updatable_yes', function() {

        describe('Клик по заголовку баннера с невалидным урлом', function() {
            beforeEach(function() {
                createBlock({ href: 'blabla', adgroup_id: 'group-id' }, {}, { updatable: 'yes' });
            });

            afterEach(destructBlock);

            it('Должно появиться сообщение с ошибкой', function(done) {
                sinon.spy(BEM.blocks['b-confirm'], 'alert');

                block.elem('title').click();

                u.waitForAndRestore(BEM.blocks['b-confirm'].alert, done);
            });
        });

        describe('API setBanner', function() {
            beforeEach(function() {
                var bannerData2 = u.bannerData({
                        banner: {
                            bid: 'banner-id-2',
                            title: 'title2',
                            body: 'body2',
                            href: 'bla2'
                        }
                    });
                bannerModel2 = BEM.MODEL.create(
                    { name: 'm-banner', id: 'banner-id-2', parentName: 'm-group', parentId: 'group-id' },
                    u.bannerModelData({ banner: bannerData2}));

                createBlock({
                    bid: 'banner-id',
                    has_href: true,
                    href: 'bla',
                    adgroup_id: 'group-id'
                }, {}, { updatable: 'yes' });
            });

            afterEach(destructBlock);

            it('В API должен присутствовать функция setBanner', function() {
                expect(block.setBanner).to.be.an.instanceof(Function);
            });

            it('При установке новой модели, должен обновиться вид баннера', function(done) {
                block.setBanner({ name: 'm-banner', id: 'banner-id-2', parentName: 'm-group', parentId: 'group-id' });

                u.waitFor(function() {
                    return block.elem('title').html() == 'title2' &&
                        block.elem('body').html() == 'body2';
                }, done);
            });

        });

        describe('Инициализация', function() {
            beforeEach(function() {

                createBlock({
                    title: 'title',
                    body: 'body',
                    has_href: true,
                    href: 'yandex.ru',
                    adgroup_id: 'group-id'
                },
                {},
                { updatable: 'yes' });
            });

            afterEach(destructBlock);

            it('У блока с updatable = yes всегда есть блок с сайтлинками если не задано hideSitelinks = true', function() {
                expect(block.elem('sitelinks').length).to.be.above(0);
            });

            it('При создании блок должен проапдейтится данными из  привязанной к нему модели', function(done){
                u.waitFor(function() {
                    return block.elem('title').html() == 'title' &&
                        block.elem('body').html() == 'body';
                }, done);
            });
        });

        describe('Реакция на изменения модели', function() {
            beforeEach(function() {
                createBlock({
                    bid: 10,
                    title: 'title',
                    body: 'body',
                    sitelinks: [
                        {
                            title: 'title1',
                            href: 'href1'
                        }
                    ],
                    has_href: true,
                    href: 'yandex.ru',
                    domain: 'yandex.ru',
                    adgroup_id: 'group-id'
                },
                {},
                { updatable: 'yes' });
            });

            afterEach(destructBlock);

            it('Изменился title в модели - должен измениться заголовок', function(done) {
                bannerModel.set('title', 'title2');
                u.waitFor(function() {
                    return block.elem('title').html() == 'title2';
                }, done);
            });

            it('Изменился body в модели - должен измениться текст', function(done) {
                bannerModel.set('body', 'body2');
                u.waitFor(function() {
                    return block.elem('body').html() == 'body2';
                }, done);

            });

            it('Изменился sitelinks в модели - должен измениться sitelinks в превью', function(done) {
                block._getSitelinksModel().set('title0', 'new_title');
                u.waitFor(function() {
                    return block.elem('sitelink').get(0).innerHTML == 'new_title';
                }, done);
            });

            it('Изменился href в модели на валидный - должна измениться ссылка в заголовке  href', function(done) {
                block._getHrefModel().set('href', 'market.ru');
                u.waitFor(function() {
                    return block.elem('title').prop('href') == block._formatUrl('market.ru/');
                }, done);
            });

            it('Изменился href в модели на не валидный - должна измениться ссылка в заголовке на #', function(done) {
                block._getHrefModel().set('href', 'mwerfwer');
                u.waitFor(function() {
                    return block.elem('title').attr('href') == '#';
                }, done);
            });
        });
    });
});
