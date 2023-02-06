describe('Daria.vComposePopupButton', function() {

    beforeEach(function() {
        this.vComposePopupButton = ns.View.create('compose-popup-button');
        this.stubNode = function stubNode() {
            return {
                find: function() {
                    return stubNode();
                },
                on: this.sinon.stub(),
                off: this.sinon.stub()
            };
        };
        this.nbButton = {
            $node: this.stubNode(),
            on: this.sinon.stub(),
            off: this.sinon.stub()
        };
        this.nbInput = {
            $node: this.stubNode(),
            on: this.sinon.stub(),
            off: this.sinon.stub()
        };
        this.nbPopup = {};

        this.vComposePopupButton.$node = $('<div />');
        this.vComposePopupButton.node = this.vComposePopupButton.$node[0];
        this.vComposePopupButton.nanoislands = {
            blocks: {
                'button': '.js-some-button',
                'input': '.js-some-input'
            },
            events: {
                'click button .js-some-button-text': 'onButtonTextClick',
                'nb-changed input': 'onInputChanged'
            }
        };
        this.vComposePopupButton.onButtonTextClick = this.sinon.stub();
        this.vComposePopupButton.onInputChanged = this.sinon.stub();

        this.nb$block = this.sinon.stub(nb, '$block');

        this.nb$block
            .withArgs('.js-some-button', this.vComposePopupButton.$node)
            .returns(this.nbButton);

        this.nb$block
            .withArgs('.js-some-input', this.vComposePopupButton.$node)
            .returns(this.nbInput);
    });

    describe('#onHtmlInit', function() {

        it('должен создать указанные наноблоки', function() {
            this.sinon.stub(this.vComposePopupButton, 'initNbs');
            this.vComposePopupButton.onHtmlInit();

            expect(this.vComposePopupButton.initNbs).to.have.callCount(1);
        });

    });

    describe('#onHtmlDestroy', function() {

        it('должен уничтожить созданные наноблоки', function() {
            this.sinon.stub(this.vComposePopupButton, 'destroyNbs');
            this.vComposePopupButton.onHtmlDestroy();

            expect(this.vComposePopupButton.destroyNbs).to.have.callCount(1);
        });

    });

    describe('#initNbs', function() {

        beforeEach(function() {
            this.sinon.stub(this.vComposePopupButton, 'bindNbs');
            this.vComposePopupButton.initNbs();
        });

        it('не должен вызывать ошибку, если секция наноблоков не описана', function() {
            delete this.vComposePopupButton.nanoislands;

            expect(function() {
                this.vComposePopupButton.initNbs();
            }.bind(this)).to.not.throw();
        });

        it('должен создать указанные наноблоки', function() {
            expect(this.vComposePopupButton.nbs.button).to.be.equal(this.nbButton);
            expect(this.vComposePopupButton.nbs.input).to.be.equal(this.nbInput);
        });

        it('должен сменить флаг инициализированности наноблоков', function() {
            expect(this.vComposePopupButton._nbsInited).to.be.equal(true);
        });

        it('должен запустить подписку на описанные события', function() {
            expect(this.vComposePopupButton.bindNbs).to.have.callCount(1);
        });

    });

    describe('#destroyNbs', function() {

        beforeEach(function() {
            this.sinon.stub(nb, 'destroy');
            this.sinon.stub(this.vComposePopupButton, 'unbindNbs');
            this.vComposePopupButton.destroyNbs();
        });

        it('должен запустить подписку на описанные события', function() {
            expect(this.vComposePopupButton.unbindNbs).to.have.callCount(1);
        });

        it('должен сменить флаг инициализированности наноблоков', function() {
            expect(this.vComposePopupButton._nbsInited).to.be.equal(false);
        });

        it('должен создать указанные наноблоки', function() {
            expect(nb.destroy).to.be.calledWithExactly(this.vComposePopupButton.node);
        });

    });

    describe('#bindNbs', function() {

        beforeEach(function() {
            this.sinon.stub(this.vComposePopupButton, 'handleBinding');
        });

        it('должен вызвать подписку на указанные события наноблоков', function() {
            this.vComposePopupButton.bindNbs();

            expect(this.vComposePopupButton.handleBinding).to.be.calledWithExactly('on');
        });

    });


    describe('#unbindNbs', function() {

        beforeEach(function() {
            this.sinon.stub(this.vComposePopupButton, 'handleBinding');
        });

        it('должен вызвать отписку от указанных события наноблоков', function() {
            this.vComposePopupButton.unbindNbs();

            expect(this.vComposePopupButton.handleBinding).to.be.calledWithExactly('off');
        });

    });

    describe('#handleBinding', function() {

        beforeEach(function() {
            this.vComposePopupButton.initNbs();
        });

        afterEach(function() {
            delete this.vComposePopupButton._nbsInited;
        });

        it('не должен вызывать ошибку, если секция наноблоков не описана', function() {
            delete this.vComposePopupButton.nanoislands;

            expect(function() {
                this.vComposePopupButton.handleBinding('on');
            }.bind(this)).to.not.throw();
        });

        it('не должен вызывать ошибку, если описанные элементы не унициализированы', function() {
            this.vComposePopupButton._nbsInited = false;

            expect(function() {
                this.vComposePopupButton.handleBinding('on');
            }.bind(this)).to.not.throw();
        });

        it('не должен вызывать ошибку, если события для наноблоков не описаны', function() {
            delete this.vComposePopupButton.nanoislands.events;

            expect(function() {
                this.vComposePopupButton.handleBinding('on');
            }.bind(this)).to.not.throw();
        });

        it('должен вызвать assert, если указанный в событии блок не существует', function() {
            this.vComposePopupButton.nanoislands = {
                events: {
                    'nb-open popup': 'onPopupOpen'
                }
            };

            expect(function() {
                this.vComposePopupButton.handleBinding('on');
            }.bind(this)).to.throw();
        });

        describe('в ключе описания события 2 слова ->', function() {

            it('должен подписаться на событие наноблока, указанного в ключе', function() {
                this.vComposePopupButton.handleBinding('on');

                expect(this.nbInput.on).to.be.calledWith('nb-changed');
            });

            it('должен связать обработчик события наноблока с методом, указанным в описании события', function() {
                this.vComposePopupButton.handleBinding('on');
                this.nbInput.on.callArg(1);

                expect(this.vComposePopupButton.onInputChanged).to.have.callCount(2);
            });

            it('должен отписаться от события наноблока, указанного в ключе', function() {
                this.vComposePopupButton.handleBinding('off');
                expect(this.nbInput.off).to.be.calledWith('nb-changed');
            });

        });

        describe('в ключе описания события 3 слова ->', function() {

            beforeEach(function() {
                this.$node = this.stubNode();
                this.sinon.stub(this.nbButton.$node, 'find').returns(this.$node);

            });

            it('должен найти элемент в наноблоке, селектор которого описан в событии', function() {
                this.vComposePopupButton.handleBinding('on');

                expect(this.nbButton.$node.find).to.be.calledWith('.js-some-button-text');
            });

            it('должен подписаться на событие найденного элемента', function() {
                this.vComposePopupButton.handleBinding('on');

                expect(this.$node.on).to.be.calledWith('click');
            });

            it('должен при срабатывании события вызвать метод view, связанный с этим событием', function() {
                this.vComposePopupButton.handleBinding('on');
                this.$node.on.callArg(1);

                expect(this.vComposePopupButton.onButtonTextClick).to.have.callCount(1);
            });

            it('должен подписаться от события найденного элемента', function() {
                this.vComposePopupButton.handleBinding('off');

                expect(this.$node.off).to.be.calledWith('click');
            });

        });

    });

    describe('#showBubble', function() {

        beforeEach(function() {
            this.vComposePopupButton.containerSelector = '.js-some-container';
            this.vComposePopupButton.nbs = {
                popup: {
                    isOpen: this.sinon.stub(),
                    open: this.sinon.stub()
                }
            };

            this.sinon.stub(vow, 'resolve').returns(true);
            this.sinon.stub(vow, 'reject').returns(false);
        });

        it('должен вызвать ошибку, если у view не установлен containerSelector', function() {
            delete this.vComposePopupButton.containerSelector;

            expect(function() {
                this.vComposePopupButton.showBubble();
            }.bind(this)).to.throw();
        });

        describe('попап закрыт ->', function() {

            beforeEach(function() {
                this.vComposePopupButton.nbs.popup.isOpen.returns(false);
                this.vComposePopupButton.showBubble();
            });

            it('должен отобразить попап', function() {
                expect(this.vComposePopupButton.nbs.popup.open).to.have.callCount(1);
            });

            it('должен вернуть зарезолвленный promise', function() {
                expect(vow.resolve).to.have.callCount(1);
                expect(vow.reject).to.have.callCount(0);
            });

        });

        describe('попап открыт ->', function() {

            beforeEach(function() {
                this.vComposePopupButton.nbs.popup.isOpen.returns(true);
                this.vComposePopupButton.showBubble();
            });

            it('не должен открывать попап', function() {
                expect(this.vComposePopupButton.nbs.popup.open).to.have.callCount(0);
            });

            it('должен вернуть реджекнутый promise', function() {
                expect(vow.resolve).to.have.callCount(0);
                expect(vow.reject).to.have.callCount(1);
            });

        });

    });

    describe('#setCheckboxState', function() {

        beforeEach(function() {
            this.nbCheckbox = {
                isChecked: this.sinon.stub().returns(true),
                check: this.sinon.stub(),
                uncheck: this.sinon.stub()
            };
            this.vComposePopupButton.nbs = {
                checkbox: this.nbCheckbox
            };
        });

        it('должен вызвать ошибку, если не создан островной чекбокс', function() {
            this.sinon.stub(this.vComposePopupButton, 'nbs').value({});
            expect(function() {
                this.vComposePopupButton.setCheckboxState('checkbox', true);
            }.bind(this)).to.throw();
        });

        describe('чекбокс выбран ->', function() {

            beforeEach(function() {
                this.nbCheckbox.isChecked.returns(true);
            });

            it('должен не изменять состояние чекбокса, если его выбирают', function() {
                this.vComposePopupButton.setCheckboxState('checkbox', true);

                expect(this.nbCheckbox.check).to.have.callCount(0);
                expect(this.nbCheckbox.uncheck).to.have.callCount(0);
            });

            it('должен изменить состояние чекбокса, если его отменяют', function() {
                this.vComposePopupButton.setCheckboxState('checkbox', false);

                expect(this.nbCheckbox.check).to.have.callCount(0);
                expect(this.nbCheckbox.uncheck).to.have.callCount(1);
            });

        });

        describe('чекбокс отменен ->', function() {

            beforeEach(function() {
                this.nbCheckbox.isChecked.returns(false);
            });

            it('должен не изменять состояние чекбокса, если его отменяют', function() {
                this.vComposePopupButton.setCheckboxState('checkbox', false);

                expect(this.nbCheckbox.check).to.have.callCount(0);
                expect(this.nbCheckbox.uncheck).to.have.callCount(0);
            });

            it('должен изменить состояние чекбокса, если его выбирают', function() {
                this.vComposePopupButton.setCheckboxState('checkbox', true);

                expect(this.nbCheckbox.check).to.have.callCount(1);
                expect(this.nbCheckbox.uncheck).to.have.callCount(0);
            });

        });

    });

});
