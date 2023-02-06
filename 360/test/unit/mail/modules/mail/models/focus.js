describe('Daria.mFocus', function() {
    beforeEach(function() {
        this.sinon.stub(Daria.nsTreeWalker, 'getTreeForFocus').returns([ [], [], [] ]);
        this.sinon.stub(Daria.Focus, 'scrollIntoView');
        this.sinon.stub(Daria.Focus, 'needLazyScroll');
        this.sinon.stub(Daria.Focus, 'lazyScroll');

        this.mFocus = ns.Model.get('focus');
    });

    describe('#changeItem', function() {
        beforeEach(function() {
            this.sinon.stub(this.mFocus, '_loadMoreMessages');
            this.sinon.stub(this.mFocus, '_changeFocus');
            this.sinon.stub(this.mFocus, '_getOpenMessageIndex').returns(-1);
            this.sinon.stub(this.mFocus, '_getFirstUnpinnedMessage').returns(0);

            this.dataForModel = {
                tree: [
                    [ 1, 2, 3 ],
                    [ 4, 5, 6 ],
                    [ 7, 8, 9 ]
                ],
                itemIndex: 1,
                columnIndex: 1,
                currentFocus: 5
            };
        });

        it('Должен установить фокус в текущее положение, если фокуса нет', function() {
            this.dataForModel.currentFocus = null;
            this.mFocus.setData(this.dataForModel);

            this.mFocus.changeItem(true);

            expect(this.mFocus._changeFocus).to.be.calledWith(1, 1);
        });

        it('Должен установить фокус на открытое письмо, если фокуса нет', function() {
            this.dataForModel.currentFocus = null;
            this.dataForModel.itemIndex = -1;
            this.mFocus.setData(this.dataForModel);

            this.mFocus._getOpenMessageIndex.returns(2);

            this.mFocus.changeItem(true);

            expect(this.mFocus._changeFocus).to.be.calledWith(1, 2);
        });

        it('Должен установить фокус на первое неприкреплённое письмо, если фокуса нет', function() {
            this.dataForModel.currentFocus = null;
            this.dataForModel.itemIndex = -1;
            this.mFocus.setData(this.dataForModel);

            this.mFocus._getFirstUnpinnedMessage.returns(2);

            this.mFocus.changeItem(true);

            expect(this.mFocus._changeFocus).to.be.calledWith(1, 2);
        });

        it('Не должен устанавливать фокус, если нужна ленивая прокрутка', function() {
            this.mFocus.setData(this.dataForModel);

            Daria.Focus.needLazyScroll.returns(true);

            this.mFocus.changeItem(true);

            expect(this.mFocus._changeFocus).to.have.callCount(0);
        });

        it('Должен увеличить индекс на 1, если перемещаемся вниз', function() {
            this.mFocus.setData(this.dataForModel);

            this.mFocus.changeItem(true);

            expect(this.mFocus._changeFocus).to.be.calledWith(1, 2);
        });

        it('Должен уменьшить индекс на 1, если перемещаемся вверх', function() {
            this.mFocus.setData(this.dataForModel);

            this.mFocus.changeItem(false);

            expect(this.mFocus._changeFocus).to.be.calledWith(1, 0);
        });

        it('Должен подгрузить новую пачку писем, если фокус в колонке с письмами и элементов ниже нет', function() {
            this.dataForModel.itemIndex = 2;
            this.dataForModel.currentFocus = 6;
            this.mFocus.setData(this.dataForModel);

            this.mFocus.changeItem(true);

            expect(this.mFocus._loadMoreMessages).to.have.callCount(1);
        });

        it('Индекс не должен быть больше индекса последнего элемента', function() {
            this.dataForModel.itemIndex = 2;
            this.dataForModel.columnIndex = 0;
            this.dataForModel.currentFocus = 3;
            this.mFocus.setData(this.dataForModel);

            this.mFocus.changeItem(true);

            expect(this.mFocus._changeFocus).to.have.callCount(0);
        });

        it('Индекс не должен быть меньше нуля', function() {
            this.dataForModel.itemIndex = 0;
            this.dataForModel.currentFocus = 1;
            this.mFocus.setData(this.dataForModel);

            this.mFocus.changeItem(false);

            expect(this.mFocus._changeFocus).to.have.callCount(0);
        });
    });

    describe('#changeColumn', function() {
        beforeEach(function() {
            this.sinon.stub(this.mFocus, 'setColumn');
            this.dataForModel = {
                tree: [
                    [ 1, 2, 3 ],
                    [ 4, 5, 6 ],
                    [ 7, 8, 9 ]
                ],
                itemIndex: 0,
                columnIndex: 0,
                lastFocus: [ {
                    focusView: 1,
                    itemIndex: 0
                }, {
                    focusView: 4,
                    itemIndex: 3
                }, {
                    focusView: 7,
                    itemIndex: 6
                } ]
            };
        });

        it('Должен увеличить индекс на 1, если перемещаемся вправо', function() {
            this.mFocus.setData(this.dataForModel);

            this.mFocus.changeColumn(true);

            expect(this.mFocus.setColumn).to.be.calledWith(1);
        });

        it('Должен уменьшить индекс на 1, если перемещаемся влево', function() {
            this.dataForModel.columnIndex = 1;
            this.mFocus.setData(this.dataForModel);

            this.mFocus.changeColumn(false);

            expect(this.mFocus.setColumn).to.be.calledWith(0);
        });
    });

    describe('#setColumn', function() {
        beforeEach(function() {
            this.sinon.stub(this.mFocus, 'hasFocus').returns(true);
            this.sinon.stub(this.mFocus, '_saveFocus');
            this.sinon.stub(this.mFocus, '_changeFocus');
            this.sinon.stub(this.mFocus, '_getFirstIndexForViews').returns(-1);

            this.dataForModel = {
                tree: [
                    [ 1, 2, 3 ],
                    [ 4, 5, 6 ],
                    [ 7, 8, 9 ]
                ],
                itemIndex: 0,
                columnIndex: 0,
                lastFocus: [ {
                    focusView: 1,
                    itemIndex: 0
                }, {
                    focusView: 4,
                    itemIndex: 3
                }, {
                    focusView: 7,
                    itemIndex: 6
                } ]
            };
        });

        it('Индекс не должен быть меньше нуля', function() {
            this.mFocus.setData(this.dataForModel);

            this.mFocus.setColumn(-1);

            expect(this.mFocus._changeFocus).to.have.callCount(0);
        });

        it('Не должен перемещать фокус, если индекс не изменился и есть текущий фокус', function() {
            // Индекс не изменился, если мы не нашли непустую колонку в направление движения
            this.dataForModel.tree = [
                [ 1, 2, 3 ],
                [],
                []
            ];
            this.mFocus.setData(this.dataForModel);

            this.mFocus.hasFocus.returns(true);

            this.mFocus.setColumn(0);

            expect(this.mFocus._changeFocus).to.have.callCount(0);
        });

        it('Должен выставить фокус на элемент в текущей колонке, если индекс не изменился и нет текущего фокуса',
            function() {
                this.dataForModel.tree = [
                    [ 1, 2, 3 ],
                    [],
                    []
                ];
                this.mFocus.setData(this.dataForModel);

                this.mFocus.hasFocus.returns(false);

                this.mFocus.setColumn(0);

                expect(this.mFocus._changeFocus).to.be.calledWith(0, 0);
            }
        );

        it('Индекс не должен быть больше индекса последнего элемента', function() {
            this.dataForModel.columnIndex = 2;
            this.mFocus.setData(this.dataForModel);

            this.mFocus.changeColumn(3);

            expect(this.mFocus._changeFocus).to.have.callCount(0);
        });

        it('Должен использовать сохраненный индекс элемента, если он есть', function() {
            this.dataForModel.lastFocus = [ {
                focusView: 1,
                itemIndex: 0
            }, {
                focusView: 5,
                itemIndex: 4
            }, {
                focusView: 7,
                itemIndex: 6
            } ];

            this.mFocus.setData(this.dataForModel);

            this.mFocus.setColumn(1);

            expect(this.mFocus._changeFocus).to.be.calledWith(1, 1);
        });

        it('Индекс элемента должен быть равен нулю, если для колонки нет сохраненного фокуса', function() {
            this.dataForModel.lastFocus = [ {
                focusView: 1,
                itemIndex: 0
            }, null, {
                focusView: 7,
                itemIndex: 6
            } ];

            this.mFocus.setData(this.dataForModel);

            this.mFocus.setColumn(1);

            expect(this.mFocus._changeFocus).to.be.calledWith(1, 0);
        });
    });

    describe('#pubEvent', function() {
        beforeEach(function() {
            this.view = {
                isValid: no.True,
                onFocusEvent: function() {}
            };

            this.sinon.spy(this.view, 'onFocusEvent');

            this.mFocus.setData({
                currentFocus: this.view,
                columnIndex: 0
            });
        });

        it('Должен передать событие вью, которая находится в фокусе', function() {
            this.mFocus.pubEvent('blur');

            expect(this.view.onFocusEvent).to.be.calledWith({
                type: 'blur'
            });
        });
    });

    describe('#hasFocus', function() {
        it('Должен возвращать true, если есть фокус', function() {
            this.mFocus.setData({
                currentFocus: 'view'
            });

            expect(this.mFocus.hasFocus()).to.be.equal(true);
        });

        it('Должен возвращать false, если фокуса нет', function() {
            this.mFocus.setData({
                currentFocus: null
            });

            expect(this.mFocus.hasFocus()).to.be.equal(false);
        });
    });

    describe('#getFocus', function() {
        it('Должен возвращать текущий фокус', function() {
            this.mFocus.setData({
                currentFocus: 'view'
            });

            expect(this.mFocus.getFocus()).to.be.equal('view');
        });
    });

    describe('#setFocusByMid', function() {
        beforeEach(function() {
            this.sinon.stub(this.mFocus, '_changeFocus');
        });

        it('Должен переместить фокус на письмо, если его нашли по mid', function() {
            this.mFocus.setData({
                tree: [
                    [],
                    [
                        {
                            params: {
                                ids: '123'
                            }
                        },
                        {
                            params: {
                                ids: '321'
                            }
                        },
                        {
                            params: {
                                ids: '111'
                            }
                        }
                    ]
                ]
            });

            this.mFocus.setFocusByMid('111');

            expect(this.mFocus._changeFocus).to.be.calledWith(1, 2);
        });

        it('Не должен перемещать фокус на письмо, если его не нашли по mid', function() {
            this.mFocus.setData({
                tree: [
                    [],
                    [
                        {
                            params: {
                                ids: '123'
                            }
                        },
                        {
                            params: {
                                ids: '321'
                            }
                        },
                        {
                            params: {
                                ids: '222'
                            }
                        }
                    ]
                ]
            });

            this.mFocus.setFocusByMid('111');

            expect(this.mFocus._changeFocus).to.have.callCount(0);
        });
    });

    describe('#focusToFirstMessage', function() {
        it('Должен переместить фокус на первое письмо, если оно есть', function() {
            this.mFocus.setData({
                tree: [
                    [],
                    [
                        {
                            params: {
                                ids: '111'
                            },
                            isValid: no.True,
                            onFocusEvent: function() {}
                        },
                        {
                            params: {
                                ids: '222'
                            },
                            isValid: no.True
                        },
                        {
                            params: {
                                ids: '333'
                            },
                            isValid: no.True
                        }
                    ]
                ],
                columnIndex: 1,
                itemIndex: 2
            });

            this.mFocus.focusToFirstMessage();

            expect(this.mFocus.get('.columnIndex')).to.be.equal(1);
            expect(this.mFocus.get('.itemIndex')).to.be.equal(0);
        });

        it('Не должен перемещать фокус, если нет писем', function() {
            this.mFocus.setData({
                tree: [
                    [],
                    []
                ],
                columnIndex: 1,
                itemIndex: 2
            });

            this.mFocus.focusToFirstMessage();

            expect(this.mFocus.get('.columnIndex')).to.be.equal(1);
            expect(this.mFocus.get('.itemIndex')).to.be.equal(2);
        });
    });

    describe('#focusToLastMessage', function() {
        it('Должен переместить фокус на последнее письмо, если оно есть', function() {
            this.mFocus.setData({
                tree: [
                    [],
                    [
                        {
                            params: {
                                ids: '111'
                            },
                            isValid: no.True

                        },
                        {
                            params: {
                                ids: '222'
                            },
                            isValid: no.True
                        },
                        {
                            params: {
                                ids: '333'
                            },
                            isValid: no.True,
                            onFocusEvent: function() {}
                        }
                    ]
                ],
                columnIndex: 1,
                itemIndex: 0
            });

            this.mFocus.focusToLastMessage();

            expect(this.mFocus.get('.columnIndex')).to.be.equal(1);
            expect(this.mFocus.get('.itemIndex')).to.be.equal(2);
        });

        it('Не должен перемещать фокус, если нет писем', function() {
            this.mFocus.setData({
                tree: [
                    [],
                    []
                ],
                columnIndex: 1,
                itemIndex: 0
            });

            this.mFocus.focusToLastMessage();

            expect(this.mFocus.get('.columnIndex')).to.be.equal(1);
            expect(this.mFocus.get('.itemIndex')).to.be.equal(0);
        });
    });

    describe('#getIndexCurrentColumn', function() {
        beforeEach(function() {
            this.dataForModel = {
                tree: [
                    [ 1, 2, 3 ],
                    [ 4, 5, 6 ],
                    [ 7, 8, 9 ]
                ],
                itemIndex: 1,
                columnIndex: 1,
                currentFocus: 5
            };
        });

        it('Должен вернуть индекс текущей колонки, если он есть', function() {
            this.mFocus.setData(this.dataForModel);

            expect(this.mFocus.getIndexCurrentColumn()).to.be.equal(1);
        });

        it('Должен вернуть null, если индекса нет', function() {
            this.dataForModel.columnIndex = null;
            this.mFocus.setData(this.dataForModel);

            expect(this.mFocus.getIndexCurrentColumn()).to.be.equal(null);
        });
    });

    describe('#isCurrentFocusOnMessage', function() {
        beforeEach(function() {
            this.view = {
                getModel: this.sinon.stub(),
                isValid: this.sinon.stub()
            };

            this.sinon.stub(this.mFocus, 'hasFocus');
            this.sinon.stub(this.mFocus, 'getFocus').returns(this.view);
            this.view.isValid.returns(true);
        });

        it('Должен вернуть false, если вообще нет фокуса', function() {
            this.mFocus.hasFocus.returns(false);

            expect(this.mFocus.isCurrentFocusOnMessage()).to.be.equal(false);
        });

        it('Должен вернуть false, если вид невалиден', function() {
            this.view.isValid.returns(false);
            expect(this.mFocus.isCurrentFocusOnMessage()).to.be.equal(false);
        });

        it('Должен вернуть true, если и вью в фокусе есть модели mMessage и mMessagesChecked', function() {
            this.mFocus.hasFocus.returns(true);
            this.view.getModel.withArgs('message').returns({});
            this.view.getModel.withArgs('messages-checked').returns({});

            expect(this.mFocus.isCurrentFocusOnMessage()).to.be.equal(true);
        });

        it('Должен вернуть false, если у вью в фокусе нет модели mMessage', function() {
            this.mFocus.hasFocus.returns(true);
            this.view.getModel.withArgs('message').returns(undefined);
            this.view.getModel.withArgs('messages-checked').returns({});

            expect(this.mFocus.isCurrentFocusOnMessage()).to.be.equal(false);
        });

        it('Должен вернуть false, если у вью в фокусе нет модели mMessagesChecked', function() {
            this.mFocus.hasFocus.returns(true);
            this.view.getModel.withArgs('message').returns({});
            this.view.getModel.withArgs('messages-checked').returns(undefined);

            expect(this.mFocus.isCurrentFocusOnMessage()).to.be.equal(false);
        });
    });

    describe('#resetFocus', function() {
        beforeEach(function() {
            this.sinon.stub(this.mFocus, 'pubEvent');

            this.mFocus.set('.columnIndex', 3);
            this.mFocus.set('.lastFocus', [ 1, 2, 3 ]);
        });

        it('Должен оповестить вью, что фокус будет снят', function() {
            this.mFocus.resetFocus();

            expect(this.mFocus.pubEvent).to.be.calledWith('blur');
        });

        it('Должен сбосить фокус и индекс элемента', function() {
            this.mFocus.resetFocus();

            expect(this.mFocus.get('.currentFocus')).to.be.equal(null);
            expect(this.mFocus.get('.itemIndex')).to.be.equal(0);
        });

        it('Должен сбросить индекс колонки на дефолтный, если передан флаг', function() {
            this.mFocus.resetFocus(true);

            expect(this.mFocus.get('.columnIndex')).to.be.equal(1);
        });

        it('Не должен сбрасывать индекс колонки, если не передан флаг', function() {
            this.mFocus.resetFocus();

            expect(this.mFocus.get('.columnIndex')).to.be.equal(3);
        });

        it('Должен сбросить сохраненный фокус для каждой колонки, кроме первой', function() {
            this.mFocus.resetFocus();

            expect(this.mFocus.get('.lastFocus')).to.be.eql([ 1, null, null ]);
        });
    });

    describe('#isFocusedInColumn ->', function() {
        beforeEach(function() {
            this.mFocusData = {
                tree: [
                    [ 1, 2, 3 ],
                    [ 4, 5, 6 ],
                    [ 7, 8, 9 ]
                ],
                itemIndex: 0,
                columnIndex: 0,
                lastFocus: [ {
                    focusView: 1,
                    itemIndex: 0
                }, {
                    focusView: 4,
                    itemIndex: 0
                }, {
                    focusView: 7,
                    itemIndex: 0
                } ]
            };
        });

        describe('фокус в той же колонке, что и проверяемый вид', function() {
            it('фокус на проверяемом виде => true', function() {
                this.mFocus.setData(_.extend({}, this.mFocusData, {
                    columnIndex: 1,
                    currentFocus: 5
                }));
                expect(this.mFocus.isFocusedInColumn(1, 5)).to.be.equal(true);
            });
            it('фокус на другом виде => false', function() {
                this.mFocus.setData(_.extend({}, this.mFocusData, {
                    columnIndex: 1,
                    currentFocus: 4
                }));
                expect(this.mFocus.isFocusedInColumn(1, 5)).to.be.equal(false);
            });
        });

        describe('фокус в другой колонке, чем проверяемый вид', function() {
            it('в свой колонке вид был последним в фокусе => true', function() {
                var mFocusData = _.extend({}, this.mFocusData, {
                    columnIndex: 2,
                    currentFocus: 7
                });

                mFocusData.lastFocus[1] = {
                    focusView: 5,
                    itemIndex: 1
                };

                this.mFocus.setData(mFocusData);

                expect(this.mFocus.isFocusedInColumn(1, 5)).to.be.equal(true);
            });
            it('в свой колонке последним в фокусе был какой-то другой вид => false', function() {
                var mFocusData = _.extend({}, this.mFocusData, {
                    columnIndex: 2,
                    currentFocus: 7
                });

                mFocusData.lastFocus[1] = {
                    focusView: 6,
                    itemIndex: 2
                };

                this.mFocus.setData(mFocusData);

                expect(this.mFocus.isFocusedInColumn(1, 5)).to.be.equal(false);
            });
        });

        it('после _reset() => false', function() {
            this.mFocus.setData(_.extend({}, this.mFocusData, {
                columnIndex: 1,
                currentFocus: 5
            }));
            this.mFocus._reset();
            expect(this.mFocus.isFocusedInColumn(1, 5)).to.be.equal(false);
        });
    });
});
