describe('b-errors-presenter2', function() {
    var errBlockName = 'b-errors-presenter2-test-error-output',
        sandbox,
        getDataAttrs = function(path) {
            return { 'data-err-path': path };
        },
        createPresenter = function(content) {
            var presenter = u.getInitedBlock({
                block: 'b-errors-presenter2',
                content: content
            });

            sandbox.spy(presenter, 'trigger');
            sandbox.spy(presenter, 'clearErrors');

            presenter.findBlocksByInterfaceInside('i-error-view-interface').forEach(function(viewBlock) {
                sandbox.spy(viewBlock, 'showErrors');
                sandbox.spy(viewBlock, 'clearErrors');
            });

            return presenter;
        };

    before(function() {
        // блок-заглушка, реализующий i-error-view-interface
        BEM.DOM.decl({ block: errBlockName, implements: 'i-error-view-interface' }, {
            showErrors: function(errors) {},
            clearErrors: function() {}
        });
    });

    beforeEach(function() {
        sandbox = sinon.sandbox.create();
    });

    afterEach(function() {
        sandbox.restore();
    });

    describe('Базовая функциональность', function() {
        it('Отображаются только ошибки с подходящими `path`', function() {
            var presenter = createPresenter({
                    attrs: getDataAttrs('test'),
                    content: {
                        attrs: getDataAttrs('0'),
                        content: {
                            attrs: getDataAttrs('time'),
                            content: { block: errBlockName }
                        }
                    }
                }),
                errViewBlocks = presenter.findBlocksInside(errBlockName),
                errors = [
                    { path: 'test[0].time' },
                    { path: 'qqq' }
                ];

            presenter.showErrors(errors);

            expect(errViewBlocks[0].showErrors.calledWith([errors[0]])).to.be.true;

            BEM.DOM.destruct(presenter.domElem);
        });

        it('Все ошибки складываются на самый вехний уровень, если он есть и нет более подходяшего', function() {
            var presenter = createPresenter({
                    content: [
                        { block: errBlockName }
                    ]
                }),
                errViewBlocks = presenter.findBlocksInside(errBlockName),
                errors = [
                    { path: 'test[0].time' },
                    { path: 'qqq' }
                ];

            presenter.showErrors(errors);

            expect(errViewBlocks[0].showErrors.calledWith(errors)).to.be.true;

            BEM.DOM.destruct(presenter.domElem);
        });

        it('Ошибки дублируются в блоках с одинковым `path`', function() {
            var presenter = createPresenter({
                    attrs: getDataAttrs('cond'),
                    content: [
                        { block: errBlockName },
                        { block: errBlockName }
                    ]
                }),
                errViewBlocks = presenter.findBlocksInside(errBlockName),
                errors = [
                    { path: 'cond.qwe.piupiu' },
                    { path: 'cond[12].qwerty' }
                ];

            presenter.showErrors(errors);

            expect(errViewBlocks.every(function(errViewBlock) {
                return errViewBlock.showErrors.calledWith(errors);
            })).to.be.true;

            BEM.DOM.destruct(presenter.domElem);
        });

        it('Атрибут `data-err-path` может состоять больше чем из одного уровня', function() {
            var presenter = createPresenter({
                    attrs: getDataAttrs('cond[12].sd'),
                    content: [
                        { block: errBlockName }
                    ]
                }),
                errViewBlocks = presenter.findBlocksInside(errBlockName),
                errors = [
                    { path: 'cond[12].sd.qq' }
                ];

            presenter.showErrors(errors);

            expect(errViewBlocks[0].showErrors.calledWith(errors)).to.be.true;

            BEM.DOM.destruct(presenter.domElem);
        });

        it('Очистка ошибок происходит на всех дочерних уровнях от переданного `path`', function() {
            var presenter = createPresenter(u._.range(5).map(function(index) {
                    return {
                        attrs: getDataAttrs('cond[' + index + '].piu'),
                        content: [
                            {
                                block: errBlockName,
                                attrs: getDataAttrs('uip')
                            }
                        ]
                    };
                })),
                errViewBlocks = presenter.findBlocksInside(errBlockName),
                errors = [
                    { path: 'cond[0].piu.uip.wow' },
                    { path: 'cond[1].piu.uip' },
                    { path: 'cond[2].piu.uip.pepsi' },
                    { path: 'cond[3].piu.uip.pepsi' }
                ];

            presenter.showErrors(errors);
            presenter.clearErrors('cond[0].piu');
            presenter.clearErrors('cond[1]');

            expect(errViewBlocks[0].clearErrors.called).to.be.true;
            expect(errViewBlocks[1].clearErrors.called).to.be.true;
            expect(errViewBlocks[2].clearErrors.called).to.be.false;
            expect(errViewBlocks[3].clearErrors.called).to.be.false;
            expect(errViewBlocks[4].clearErrors.called).to.be.false;

            BEM.DOM.destruct(presenter.domElem);
        });
    });

    describe('События', function() {
        var presenter,
            errors;

        beforeEach(function() {
            presenter = createPresenter(u._.range(2).map(function(index) {
                return {
                    attrs: getDataAttrs('data[' + index + ']'),
                    content: { block: errBlockName }
                };
            }));
            errors = [
                { path: 'data[0].name', text: 'name' },
                { path: 'data[1].text', text: 'text' },
                { path: 'another', text: 'anothe' }
            ];

            presenter.showErrors(errors);
        });

        afterEach(function() {
            BEM.DOM.destruct(presenter.domElem);
        });


        it('show', function() {
            var rightCall = presenter.trigger.calledWith('show',
            {
                used: [errors[0], errors[1]],
                unUsed: [errors[2]]
            });

            expect(rightCall).to.be.true;
        });

        it('reset', function() {
            presenter.showErrors([{ path: 'qqqq' }]);

            expect(presenter.trigger.calledWith('reset')).to.be.true;
        });

        it('clear', function() {
            presenter.clearErrors('data[1]');

            var rightCall = presenter.trigger.calledWith('clear', {
                cleared: [errors[1]],
                used: [errors[0]]
            });

            expect(rightCall).to.be.true;
        });
    });

    describe('Чистилщик', function() {
        var presenter,
            errViewBlocks,
            inputs;

        beforeEach(function() {
            presenter = createPresenter(u._.range(2).map(function(index) {
                return {
                    attrs: getDataAttrs('data[' + index + ']'),
                    content: [
                        {
                            block: 'input',
                            mods: { theme: 'normal' },
                            mix: {
                                block: 'b-errors-presenter2',
                                elem: 'cleaner',
                                js: { block: 'input' }
                            }
                        },
                        { block: errBlockName }
                    ]
                };
            }));
            inputs = presenter.findBlocksInside('input');
            errViewBlocks = presenter.findBlocksInside(errBlockName);
            presenter.showErrors([
                { path: 'data[0].name', text: 'qqq1' },
                { path: 'data[1].name', text: 'qqq2' }
            ]);
        });

        afterEach(function() {
            BEM.DOM.destruct(presenter.domElem);
        });

        it('При изменении в инпуте вызывается очистка ошибок только в сооствествующем блоке', function() {
            inputs[0].val('change');

            expect(errViewBlocks[0].clearErrors.called).to.be.true;
            expect(errViewBlocks[1].clearErrors.called).to.be.false;
            expect(presenter.clearErrors.calledWith('data.0')).to.be.true;
        });

        it('При повтороном изменении в инпуте вызывается очистка ошибок только в сооствествующем блоке', function() {
            inputs[0].val('change');
            inputs[0].val('change2');
            inputs[0].val('change3');

            expect(presenter.clearErrors.callCount).to.equal(1);
        });
    });

    describe('Метод `_createMap` при построении вспомогательного объекта учитывает', function() {
        var presenter,
            map,
            errViewBlocks;

        before(function() {
            presenter = createPresenter([
                {
                    attrs: getDataAttrs(''),
                    content: { block: errBlockName }
                },
                {
                    attrs: getDataAttrs('lines'),
                    content: [
                        {
                            attrs: getDataAttrs('9'),
                            content: {
                                attrs: getDataAttrs('multi[12].level.path'),
                                content: { block: errBlockName }
                            }
                        },
                        {
                            attrs: getDataAttrs('0'),
                            content: {
                                attrs: getDataAttrs('errBlockAbsent')
                            }
                        },
                        {
                            attrs: getDataAttrs('1'),
                            content: {
                                attrs: getDataAttrs('errBlockInside'),
                                content: { block: errBlockName }
                            }
                        },
                        {
                            attrs: getDataAttrs('2'),
                            content: {
                                block: errBlockName,
                                attrs: getDataAttrs('errBlockOn'),
                            }
                        }
                    ]
                },
                {
                    attrs: getDataAttrs(''),
                    content: { block: errBlockName }
                }
            ]);

            errViewBlocks = presenter.findBlocksInside(errBlockName);

            map = presenter._createMap();
        });

        after(function() {
            BEM.DOM.destruct(presenter.domElem);
        });

        it('Возможность наличия нескольких блоков с одинаковым путями', function() {
            expect(map[''].length).to.equal(2);
        });

        it('`data-err-path` только при наличии блока отображающего ошибки', function() {
            expect(map.hasOwnProperty('lines.0.errBlockAbsent')).to.be.false;
        });

        it('`data-err-path` на блоке отображающем ошибки', function() {
            expect(map['lines.2.errBlockOn'][0]).to.eql(errViewBlocks[3]);
        });

        it('`data-err-path` на родителях блоке отображающем ошибки', function() {
            expect(map['lines.1.errBlockInside'][0]).to.eql(errViewBlocks[2]);
        });

        it('Многоуровневое значение атрибута `data-err-path`', function() {
            expect(map.hasOwnProperty('lines.9.multi.12.level.path')).to.be.true;
        });
    });

    describe('Метод `_getMatchedPath` возвращает наиболее подходящий путь к переданному', function() {
        var checkPath;

        before(function() {
            checkPath = function(path, map) {
                var presenter = createPresenter(),
                    result = presenter._getMatchedPath.apply(
                        presenter,
                        [
                            u['b-errors-presenter2'].toPath(path),
                            map
                        ]
                    );

                BEM.DOM.destruct(presenter.domElem);

                return result;
            };
        });

        it('Наиболее подходящий путь по вложенности', function() {
            expect(
                checkPath(
                    'temp[0].qwerty.abc.qwer.qweqwekjkac',
                    {
                        'temp': true,
                        'temp.0': true,
                        'temp.0.qwerty': true,
                        'temp.0.qwerty.abc': true
                    }
                )
            ).to.eql('temp.0.qwerty.abc');
        });

        it('При отсутствии совпадения - пустой путь, при наличии', function() {
            expect(
                checkPath(
                    'mpet.12.qwerty.abc',
                    {
                        '': true,
                        'temp': true,
                        'temp.0': true,
                        'temp.0.qwerty': true,
                        'temp.0.qwerty.abc': true
                    }
                )
            ).to.eql('');
        });

        it('При отсутствии совпадения и пустого пути в - undefined', function() {
            expect(
                checkPath(
                    'mpet.12.qwerty.abc',
                    {
                        'temp': true,
                        'temp.0': true,
                        'temp.0.qwerty': true,
                        'temp.0.qwerty.abc': true
                    }
                )
            ).to.be.undefined;
        });
    });

});
