describe('b-banner-sitelinks', function() {
    var block,
        blockTree,
        model,
        sitelinksModel,
        sandbox,
        defaultCtx = {
            block: 'b-banner-sitelinks',
            banner: {
                sitelinks: [
                    {
                        href: 'shop.beletag.com/catalog/768/',
                        hash: '9358122158575216884',
                        title: 'Весна 17 женщинам',
                        sl_id: '1146794318',
                        description: null,
                        url_protocol: 'http://'
                    },
                    {
                        title: 'Весна 17 мужчинам',
                        hash: '15308455877912646174',
                        href: 'shop.beletag.com/catalog/773/',
                        url_protocol: 'http://',
                        description: null,
                        sl_id: '1146794319'
                    },
                    {
                        description: null,
                        url_protocol: 'http://',
                        sl_id: '1146794320',
                        hash: '13199958614195873639',
                        title: 'Весна 17 детям',
                        href: 'shop.beletag.com/catalog/779/'
                    },
                    {
                        title: 'Распродажа',
                        hash: '10659053388561446741',
                        href: 'shop.beletag.com/catalog/705/',
                        url_protocol: 'http://',
                        description: null,
                        sl_id: '248712562'
                    }
                ]
            },
            modelParams: {
                name: 'm-banner',
                id: '281592920',
                parentName: 'm-group',
                parentId: 261383701
            },
            prevBid: '0'
        },
        createBlock = function(ctx) {
            block = u.createBlock(u._.extend({}, defaultCtx, ctx));
        },
        clearSitelinks = function() {
            var empty = u._.fill(new Array(u.consts('SITELINKS_NUMBER')), 0).map(function(value, index) {
                var res = {};

                res['description' + index] = '';
                res['href' + index] = '';
                res['title' + index] = '';
                res['url_protocol' + index] = '';

                return res;
            })
            .reduce(function(res, value) {
                return u._.extend(res, value);
            }, {});

            sitelinksModel.update(empty);
        };

    beforeEach(function() {
        sandbox = sinon.sandbox.create();
        sandbox.stub(u, 'consts').withArgs('SITELINKS_NUMBER').returns(4);

        model = BEM.MODEL.create(defaultCtx.modelParams);
        sitelinksModel = BEM.MODEL.create({ name: 'm-banner-sitelinks', id: 'm-banner-sitelinks-id' }, defaultCtx.banner.sitelinks
            .map(function(value, index) {
                var res = {};

                res['description' + index] = value.description;
                res['href' + index] = value.href;
                res['title' + index] = value.title;
                res['url_protocol' + index] = value.url_protocol;

                return res;
            })
            .reduce(function(res, value) {
                return u._.extend(res, value);
            }, {}));
        model.set('sitelinks', sitelinksModel);

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

        BEM.DOM.init(blockTree).bem('b-outboard-controls');
    });

    afterEach(function() {
        sandbox.restore();
        model.destruct();
    });

    describe('DOM', function() {
        afterEach(function() {
            block.destruct();
        });

        describe('Если к сайтлинкам есть описание ', function() {
            it('то блок получает модификатор description: exist', function() {
                var ctx = u._.cloneDeep(defaultCtx);

                ctx.banner.sitelinks[0].description = 'lalala';
                createBlock(ctx);

                expect(block).to.haveMod('description', 'exist');
            });

            it('Если к сайтлинкам есть описание, то имеющийся модификатор description сбрасывается', function() {
                var ctx = u._.cloneDeep(defaultCtx);

                ctx.banner.sitelinks[0].description = 'lalala';
                ctx.mods = { description: 'none' };
                createBlock(ctx);

                expect(block).to.haveMod('description', 'exist');
            });
        });

        describe('Если к сайтлинкам нет описания ', function() {
            it('то блок не получает модификатор description: exist', function() {
                createBlock();

                expect(block).not.to.haveMod('description', 'exist');
            });

            it('то имеющийся модификатор description сохраняется', function() {
                createBlock({ mods: { description: 'none' } });

                expect(block).to.haveMod('description', 'none');
            });
        });

        it('Если в данных есть ссылки, то текст на кнопке "Изменить"', function() {
            createBlock();

            expect(block.elem('switcher').text()).to.equal('Изменить');
        });

        it('Если в данных нет ссылок, то текст на кнопке "Добавить"', function() {
            var ctx = u._.cloneDeep(defaultCtx);

            ctx.banner.sitelinks = [];
            createBlock(ctx);

            expect(block.elem('switcher').text()).to.equal('Добавить');
        });

        it('Если в данных есть ссылки, но нет текста в сслыках, то текст на кнопке "Добавить"', function() {
            var ctx = u._.cloneDeep(defaultCtx);

            ctx.banner.sitelinks[0].title = null;
            createBlock(ctx);

            expect(block.elem('switcher').text()).to.equal('Добавить');
        });
    });
    
    describe('События', function() {
        var outboardControl;
        beforeEach(function() {
            createBlock();
            outboardControl = block.findBlockInside('b-outboard-controls');
        });
        
        afterEach(function() {
            outboardControl.popup.hide();

            block.destruct();
        });
        
        describe('на модели сайтлинков', function() {

            describe('При фиксе модели', function() {
                beforeEach(function() {
                    sandbox.spy(outboardControl, 'toggleSwitchButton');

                    model.get('sitelinks').fix();
                });
                
                it('Энейблится switcher у b-outboard-control', function() {
                    expect(outboardControl.toggleSwitchButton.calledWith(false));
                });
            });

            describe('При изменении модели', function() {
                beforeEach(function() {
                    sandbox.spy(outboardControl, 'toggleSwitchButton');
                });

                it('Если попап открыт - дизейблим switcher у b-outboard-control', function() {
                    outboardControl.show();
                    model.get('sitelinks').set('href0', 'ya.ru');

                    expect(outboardControl.toggleSwitchButton.calledWith(true));
                });
            });
        });
        
        it('При закрытии попапа энейблится кнопка switcher у  b-outboard-control', function() {
            sandbox.spy(outboardControl, 'toggleSwitchButton');

            outboardControl.popup.hide();

            expect(outboardControl.toggleSwitchButton.calledWith(false));
        });
    });

    describe('Обновление отображения блока', function() {
        var outboardControl;
        beforeEach(function() {
            createBlock();
            outboardControl = block.findBlockInside('b-outboard-controls');
        });
        
        afterEach(function() {
            outboardControl.popup.hide();

            block.destruct();
        });
        
        describe('Вызов обновления', function() {
            it('При изменении модели, если попап закрыт', function() {
                sandbox.stub(outboardControl, 'isShown').callsFake(function() {
                    return false;
                });

                sitelinksModel.set('description0', 'Тутанхомон');

                expect(block.findElem('outcome').text()).to.include('Тутанхомон');
            });

            it('При фиксе модели', function() {
                sandbox.stub(outboardControl, 'isShown').callsFake(function() {
                    return true;
                });

                sitelinksModel.set('description0', 'Тутанхомон');
                expect(block.findElem('outcome').text()).not.to.include('Тутанхомон');

                sitelinksModel.fix();
                expect(block.findElem('outcome').text()).to.include('Тутанхомон');
            });
        });

        describe('Обновление текста хинта', function() {
            [
                {
                    name: 'title0',
                    value: 'Фараон'
                },
                {
                    name: 'description0',
                    value: 'Тутанхомон'
                }
            ].forEach(function(test) {
                it('При обновлении поля ' + test.name + ' модели в тексте хинта появляется текст ' + test.value, function() {
                    sandbox.stub(outboardControl, 'isShown').callsFake(function() {
                       return false;
                    });

                    sitelinksModel.set(test.name, test.value);

                    expect(block.findElem('outcome').text()).to.include(test.value);
                });
            });
        });
        
        describe('Обновление кнопки', function() {
            beforeEach(function() {
                sandbox.stub(outboardControl, 'isShown').callsFake(function() {
                   return false;
                });
            });

            it('Если есть ссылки - текст "Изменить"', function() {
                clearSitelinks();
                sitelinksModel.set('href0', 'ya.ru');

                expect(block.elem('switcher').text()).to.equal('Изменить');
            });

            it('Если нет ссылок - текст "Добавить"', function() {
                clearSitelinks();

                expect(block.elem('switcher').text()).to.equal('Добавить');
            });
        });
    });
});
