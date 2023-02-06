describe('b-dynamic-media-creatives-choose', function() {
    var sandbox,
        block,
        initBlock = function(type) {
            block = u.createBlock({
                block: 'b-dynamic-media-creatives-choose',
                mods: { type: type },
                ulogin: 'test',
                exclude: {
                    creativeIds: [1, 2, 3]
                },
                currentBannerId: 2,
                limit: 10
            });
        };

    beforeEach(function() {
        sandbox = sinon.sandbox.create({
            useFakeTimers: true,
            useFakeServer: true
        });

        sandbox.stub(u, 'getUrl').callsFake(function() { return 'yandex.ru'; });
    });

    afterEach(function() {
        block && block.destruct() && (block = null);
        sandbox.restore();
    });

    describe('Добавление нового креатива. Проверка реакции на действия пользователя.', function() {
        it('При клике на кнопку "Отмена" генерится событие cancel', function() {
            initBlock('add');
            sandbox.spy(block, 'trigger');

            block.findBlockOn('cancel', 'button').trigger('click');
            expect(block.trigger.called).to.be.true;
        });

        describe('Добавление новых баннеров', function() {
            var popup;

            beforeEach(function() {
                initBlock('add');
                sandbox.stub(BEM.DOM.blocks['b-shared-popup'], 'getInstance').callsFake(function(){
                    if (!popup) {
                        popup = {
                            setContent: sandbox.spy(function() {
                                return this;
                            }),
                            show: sandbox.spy(function() {
                                return this;
                            }),
                            hide: sandbox.spy(function() {
                                return this;
                            })
                        };
                    }

                    return popup;
                });

                sandbox.stub(u, 'consts').withArgs('SCRIPT_OBJECT').returns(u.getScriptObjectForStub());
            });

            describe('После клика по кнопке "Добавить баннеры"', function() {
                it('Должен создаться экземляр b-shared-popup', function() {
                    block.findBlockOn('add-new', 'button').trigger('click');

                    expect(BEM.DOM.blocks['b-shared-popup'].getInstance.called).to.be.true;
                });

                it('Созданному экзмепляру b-shared-popup должен установиться контент из блока b-dynamic-media-creatives-popup', function() {
                    block.findBlockOn('add-new', 'button').trigger('click');

                    expect(popup.setContent.calledWith(BEMHTML.apply({ block: 'b-dynamic-media-creatives-popup', js: true }))).to.be.true;
                });

                it('Попап должен показаться на странице', function() {
                    block.findBlockOn('add-new', 'button').trigger('click');

                    expect(popup.show.called).to.be.true;
                });
            });

            describe('После события ad-designer-close на BEM.blocks[\'b-dynamic-media-creatives-popup\']', function() {
                it('Открытый попап должен закрыться', function() {
                    block.findBlockOn('add-new', 'button').trigger('click');
                    BEM.blocks['b-dynamic-media-creatives-popup'].trigger('ad-designer-close');

                    expect(popup.hide.called).to.be.true;
                });

                it('Блок со списком креативов должен перерисоваться', function() {
                    block.findBlockOn('add-new', 'button').trigger('click');
                    var renderSpy = sandbox.spy(block._creativeWrapper, 'fetchDataAndRenderBlock');

                    BEM.blocks['b-dynamic-media-creatives-popup'].trigger('ad-designer-close');
                    expect(renderSpy.called).to.be.true;
                })
            });
        });
    });
});
