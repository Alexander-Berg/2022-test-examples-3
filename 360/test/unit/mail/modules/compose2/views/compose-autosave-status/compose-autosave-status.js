describe('Daria.vComposeAutosaveStatus', function() {
    beforeEach(function() {
        /** @type {Daria.vComposeAutosaveStatus} */
        this.view = ns.View.create('compose-autosave-status');
        this.$node = $('<div><div class="js-loader"></div></div>');
        this.view.node = this.$node[0];
        this.view.$node = this.$node;

        this.mComposeMessage = ns.Model.get('compose-message');
        this.mComposeFsm = ns.Model.get('compose-fsm');
        this.mComposeState = ns.Model.get('compose-state');
        this.sinon.stub(this.view, 'getModel')
            .withArgs('compose-message').returns(this.mComposeMessage)
            .withArgs('compose-fsm').returns(this.mComposeFsm)
            .withArgs('compose-state').returns(this.mComposeState);
    });

    describe('#onHtmlInit', function() {
        beforeEach(function() {
            this.sinon.stub(nb, 'init');
            this.sinon.stub(this.view, 'bindEvents');
            this.sinon.stub(this.view.$node, 'find').returns({'bla': '42'});
            this.sinon.stub(nb, '$block').withArgs('.js-template-popup', this.$node[0]).returns({'bla': '42'});

            this.view.onHtmlInit();
        });

        it('Инициализирует наноострова', function() {
            expect(nb.init).to.be.calledWith(this.$node[0]);
        });

        it('Привязывает события', function() {
            expect(this.view.bindEvents).to.have.callCount(1);
        });

        it('Сохраняет ссылку на наноблок попапа', function() {
            expect(this.view._nbPopup).to.be.eql({'bla': '42'});
        });
    });

    describe('#onHtmlDestroy', function() {
        beforeEach(function() {
            this.sinon.stub(nb, 'destroy');
            this.sinon.stub(this.view, 'unbindEvents');

            this.view.onHtmlDestroy();
        });

        it('Уничтожает наноострова', function() {
            expect(nb.destroy).to.be.calledWith(this.$node[0]);
        });

        it('Отвязывает события', function() {
            expect(this.view.unbindEvents).to.have.callCount(1);
        });
    });

    describe('#onClickTemplateLink', function() {
        beforeEach(function() {
            this.sinon.stub(this.mComposeMessage, 'set');
            this.sinon.stub(this.mComposeFsm, 'setState');

            this.event = {
                preventDefault: this.sinon.stub()
            };

            this.sinon.stub(this.view, '_nbPopup').value({
                close: this.sinon.stub()
            });

            // запускаем тест
            this.view.onClickTemplateLink(this.event);
        });

        it('Запрещает дефолтное поведение', function() {
            expect(this.event.preventDefault).to.have.callCount(1);
        });

        it('Закрывает попап', function() {
            expect(this.view._nbPopup.close).to.have.callCount(1);
        });

        it('Устанавливает признак шаблона в модели compose-message', function() {
            expect(this.mComposeMessage.set).to.be.calledWith('.save_symbol', 'template');
        });

        it('Запускает переход в состояние `save`', function() {
            expect(this.mComposeFsm.setState).to.be.calledWith('save');
        });
    });

    describe('#togglePopup', function() {
        beforeEach(function() {
            this.popup = {
                isOpen: this.sinon.stub(),
                open: this.sinon.stub(),
                close: this.sinon.stub()
            };
            this.sinon.stub(this.view, '_nbPopup').value(this.popup);
        });

        it('Закрывает попап, если он открыт', function() {
            this.popup.isOpen.returns(true);

            this.view.togglePopup();

            expect(this.popup.close).to.have.callCount(1);
        });

        it('Открывает попап с правильными параметрами, если он закрыт', function() {
            this.popup.isOpen.returns(false);
            var event = {
                target: {'blaa': '42'}
            };

            this.view.togglePopup(event);

            expect(this.popup.open).to.be.calledWith({
                'where': {'blaa': '42'},
                'how': {
                    'at': 'top',
                    'my': 'bottom'
                },
                'autoclose': true
            });
        });
    });

    describe('#onDraftSaved', function() {
        beforeEach(function() {
            this.sinon.stub(this.mComposeState, 'setLastSaveTime');
            this.sinon.stub(this.view, 'composeUpdate');
        });

        it('Обновляет время последнего сохранения', function() {
            this.view.onDraftSaved();

            expect(this.mComposeState.setLastSaveTime).to.have.callCount(1);
        });

        it('Запускает принудительную перерисовку себя', function() {
            this.view.onDraftSaved();

            expect(this.view.composeUpdate).to.have.callCount(1);
        });
    });
});
