describe('Daria.vCompose2', function() {
    beforeEach(function() {
        ns.Model.get('account-information').setData(mock['account-information'][0].data);
        this.sinon.stub(ns.Update.prototype, 'log');
        this.sinon.stub(ns.page.current, 'page').value('compose2');

        this.mComposeFsm = ns.Model.get("compose-fsm");
        this.sinon.stubMethods(this.mComposeFsm, [
            'setState'
        ]);

        this.view = ns.View.create('compose2');
        this.view.leaveInit();
        this.view.$node = $("<div>");

        this.mSettings = ns.Model.get('settings');
        this.mSettings.setData(getModelMockByName('settings', 'settings_conf1'));

        this.mScrollerCompose = ns.Model.get('scroller-compose');

        this.sinon.stubGetModel(this.view, [
            'compose-message', this.mComposeFsm,
            'compose-state', this.mSettings,
            'compose-attachments'
        ]);

        this.sinon.stub(this.view, 'isQuickReply').returns(false);
        this.sinon.stub(ns.events, 'trigger');

        this.undoLogger = {};
        this.sinon.stub(Daria.SendMail, 'getUndoSendLogger').returns(this.undoLogger);

        this.mComposeState.setData({});

        this.sinon.stub(this.mComposeAttachments, 'addAttach');

        ns.router.init();
    });

    describe('#onInit', function() {
        beforeEach(function() {
            this.sinon.stub(this.view, 'autosaveInit');
            this.view.onInit();
        });

        it('Инициализирует автосохранение vComposeAutosaveMixin', function() {
            expect(this.view.autosaveInit).to.have.callCount(1);
        });
    });

    describe('#onHtmlInit', function() {
        beforeEach(function() {
            this.sinon.stub(this.view, 'scrollToComposeTop');
            this.sinon.stub(this.view, 'leaveInit');

            this.mComposeMessage = this.getModel('compose-message');
            this.mComposeMessage.mMessage = {};
            this.sinon.stub(this.mComposeMessage, 'isTemplate').returns(false);
            this.sinon.stub(this.mComposeMessage, 'isDraft').returns(false);

            this.mMessagesChecked = ns.Model.get('messages-checked');
            this.sinon.stub(this.mMessagesChecked, 'resetChecked');
            this.sinon.stub(this.mMessagesChecked, 'check');

            // Стаб для mFocus
            this.sinon.stub(Daria.nsTreeWalker, 'getTreeForFocus').returns([[], [], []]);
            this.mFocus = ns.Model.get('focus');
            this.sinon.stub(this.mFocus, 'resetFocus');

            this._pageParams = ns.page.current;
        });

        afterEach(function() {
            ns.page.current = this._pageParams;
        });

        it('Должен сбросить фокус, если мы на странице композа', function() {
            ns.page.current.page = 'compose2';

            this.view.onHtmlInit();

            expect(this.mFocus.resetFocus).to.have.callCount(1);
        });

        it('Не должен сбрасывать фокус, если мы не на странице композа', function() {
            ns.page.current.page = 'messages';

            this.view.onHtmlInit();

            expect(this.mFocus.resetFocus).to.have.callCount(0);
        });

        it('Запускает инициализацию миксина vComposeLeaveMixin', function() {
            this.view.onHtmlInit();
            expect(this.view.leaveInit).to.have.callCount(1);
        });

        it('Проскролливает композ в область видимости', function() {
            this.view.onHtmlInit();
            expect(this.view.scrollToComposeTop).to.have.callCount(1);
        });

        it('Выделяет письмо, если это не QR и письмо - шаблон', function() {
            this.mComposeMessage.isTemplate.returns(true);
            this.view.onHtmlInit();
            expect(this.mMessagesChecked.resetChecked).to.have.callCount(1);
            expect(this.mMessagesChecked.check).to.have.callCount(1);
        });

        it('Выделяет письмо, если это не QR и письмо - черновик', function() {
            this.mComposeMessage.isDraft.returns(true);
            this.view.onHtmlInit();
            expect(this.mMessagesChecked.resetChecked).to.have.callCount(1);
            expect(this.mMessagesChecked.check).to.have.callCount(1);
        });

        it('Не выделяет письмо, если это QR', function() {
            this.view.isQuickReply.returns(true);
            this.mComposeMessage.isDraft.returns(true);
            this.view.onHtmlInit();
            expect(this.mMessagesChecked.resetChecked).to.have.callCount(0);
            expect(this.mMessagesChecked.check).to.have.callCount(0);
        });

        it('Не выделяет письмо, если это не шаблон и не черновик', function() {
            this.view.isQuickReply.returns(false);
            this.mComposeMessage.isDraft.returns(false);
            this.mComposeMessage.isTemplate.returns(false);
            this.view.onHtmlInit();
            expect(this.mMessagesChecked.resetChecked).to.have.callCount(0);
            expect(this.mMessagesChecked.check).to.have.callCount(0);
        });
    });

    describe('#onShow', function() {
        beforeEach(function() {
            this.sinon.stub(this.view, 'autosaveStart');
        });

        it('При показе композа compose-fsm должна быть включена', function() {
            this.view.trigger('ns-view-show');
            expect(this.view.getModel('compose-fsm')._isSwitchOn).to.be.equal(true);
        });

        it('При переходе с композа compose-fsm должна включаться при дестрое модели,' +
            ' затем при показе композа быть включённой',
            function() {
                this.tmpView = ns.View.create('compose2', {'param': 'test'});
                this.tmpView.leaveInit();

                this.tmpView.trigger('ns-view-show');
                this.tmpView.trigger('ns-view-before-load');
                this.tmpView.trigger('ns-view-hide');

                expect(this.tmpView.getModel('compose-fsm')._isSwitchOn).to.be.equal(true);

                this.tmpView.trigger('ns-view-show');
                expect(this.tmpView.getModel('compose-fsm')._isSwitchOn).to.be.equal(true);
            }
        );

        it('При переходе с композа compose-fsm должна включаться при дестрое модели,' +
            ' затем, если после этого её выключить, то при показе вида композа она снова должна включаться',
            function() {
                this.tmpView = ns.View.create('compose2', {'param': 'test'});
                this.tmpView.leaveInit();

                this.tmpView.trigger('ns-view-show');
                this.tmpView.trigger('ns-view-before-load');
                this.tmpView.trigger('ns-view-hide');

                this.tmpView.getModel('compose-fsm').switchOff();

                this.tmpView.trigger('ns-view-show');
                expect(this.tmpView.getModel('compose-fsm')._isSwitchOn).to.be.equal(true);
            }
        );

        it('Запускает автосохранение vComposeAutosaveMixin', function() {
            this.view.onShow();
            expect(this.view.autosaveStart).to.have.callCount(1);
        });

        it('Триггерит перерисовку тулбара, если не QR', function() {
            this.view.isQuickReply.returns(false);
            this.view.onShow();
            expect(ns.events.trigger).to.be.calledWith('daria:vToolbarButton:restruct');
        });

        it('Не должен триггерить перерисовку тулбара, если QR', function() {
            this.view.isQuickReply.returns(true);
            this.view.onShow();
            expect(ns.events.trigger).to.have.callCount(0);
        });

        it('Должен инициировать прикрепление аттачей, если не QR и compose-state.quickReplyAttaches не пустой массив',
            function() {
                this.mComposeState.set('.quickReplyAttaches', [{ id: 'id', resource: 'rsc' }]);
                this.view.isQuickReply.returns(false);

                this.view.onShow();

                delete this.mComposeState.data.quickReplyAttaches;

                expect(this.mComposeAttachments.addAttach).to.have.callCount(1);
            }
        );

        it('Не должен инициировать прикрепление аттачей, если не QR и compose-state.quickReplyAttaches пустой массив',
            function() {
                this.mComposeState.set('.quickReplyAttaches', []);
                this.view.isQuickReply.returns(false);

                this.view.onShow();

                delete this.mComposeState.data.quickReplyAttaches;

                expect(this.mComposeAttachments.addAttach).to.have.callCount(0);
            }
        );

        it('Должен присвоить [] в compose-state.quickReplyAttaches после прикрепления аттачей оттуда',
            function() {
                this.mComposeState.set('.quickReplyAttaches', [{ id: 'id', resource: 'rsc' }]);
                this.view.isQuickReply.returns(false);

                this.view.onShow();

                var currentQuickReplyAttaches = this.mComposeState.get('.quickReplyAttaches');
                delete this.mComposeState.data.quickReplyAttaches;

                expect(currentQuickReplyAttaches).to.be.eql([]);
            }
        );

        it('Должен выставить признак "композ загружен"', function() {
            this.view.onShow();

            expect(this.undoLogger.composeLoaded).to.be.eql(true);
        });
    });

    describe('#_logComposeOpen', function() {
        beforeEach(function() {
            this.scenarioManager = this.sinon.stubScenarioManager(this.view);
            this.scenario = this.scenarioManager.stubScenario;
            this.sinon.stub(Date, 'now');
            this.sinon.stub(Daria, 'now');
            this.sinon.stub(ns.page.history, 'getPrevious');
        });

        it('должен начать дефолтный сценарий написания письма', function() {
            this.scenarioManager.hasActiveScenario.returns(false);
            ns.page.history.getPrevious.returns({});

            this.view._logComposeOpen(this.scenarioManager);

            expect(this.scenarioManager.hasActiveScenario).to.have.been.calledWithExactly('compose-scenario');
            expect(this.scenarioManager.startScenario).to.have.been.calledWithExactly('compose-scenario', 'default');
            expect(ns.page.history.getPrevious).to.have.been.calledWith();
        });

        it('должен продолжить существующий сценарий и залогироваать время открытия композа', function() {
            this.scenarioManager.hasActiveScenario.returns(true);
            this.scenarioManager.getActiveScenario.returns(this.scenario);
            this.scenario.getTimeFromStart.returns(42);
            ns.page.history.getPrevious.returns({});

            this.view._logComposeOpen(this.scenarioManager);

            expect(this.scenarioManager.hasActiveScenario).to.have.been.calledWithExactly('compose-scenario');
            expect(this.scenario.logStep).to.have.been.calledWithExactly('compose-have-opened', { duration: 42 });
            expect(this.scenarioManager.startScenario).not.to.have.been.called;
            expect(ns.page.history.getPrevious).not.to.have.been.called;
        });

        it('должен начать сценарий открытия композа по прямой ссылке и залогировать длительность открытия', function() {
            this.scenarioManager.hasActiveScenario.returns(false);
            ns.page.history.getPrevious.returns(undefined);
            this.sinon.stub(window, 'performance.timing.navigationStart').value(10);
            Date.now.returns(100);
            Daria.now.returns(200);

            this.view._logComposeOpen(this.scenarioManager);

            expect(this.scenarioManager.hasActiveScenario).to.have.been.calledWithExactly('compose-scenario');
            expect(ns.page.history.getPrevious).to.have.been.calledWith();
            expect(this.scenario.logStep).to.have.been.calledWithExactly('compose-have-opened', { duration: 90 });
            expect(this.scenarioManager.startScenario).to.have.been.calledWithExactly('compose-scenario', 'direct-url', { startTime: 110 });
        });

        it('должен начать сценарий открытия композа по прямой ссылке и залогировать длительность открытия', function() {
            this.scenarioManager.hasActiveScenario.returns(false);
            ns.page.history.getPrevious.returns(undefined);
            this.sinon.stub(window, 'performance.timing.navigationStart').value(10);
            Date.now.returns(100);
            Daria.now.returns(200);

            this.view._logComposeOpen(this.scenarioManager);

            expect(this.scenarioManager.hasActiveScenario).to.have.been.calledWithExactly('compose-scenario');
            expect(ns.page.history.getPrevious).to.have.been.calledWith();
            expect(this.scenario.logStep).to.have.been.calledWithExactly('compose-have-opened', { duration: 90 });
            expect(this.scenarioManager.startScenario).to.have.been.calledWithExactly('compose-scenario', 'direct-url', { startTime: 110 });
        });

        it('должен начать сценарий открытия композа по прямой ссылке и не логировать длительность открытия', function() {
            this.scenarioManager.hasActiveScenario.returns(false);
            ns.page.history.getPrevious.returns(undefined);
            this.sinon.stub(window, 'performance.timing.navigationStart').value(undefined);

            this.view._logComposeOpen(this.scenarioManager);

            expect(this.scenarioManager.hasActiveScenario).to.have.been.calledWithExactly('compose-scenario');
            expect(ns.page.history.getPrevious).to.have.been.calledWith();
            expect(this.scenario.logStep).not.to.have.been.called;
            expect(this.scenarioManager.startScenario).to.have.been.calledWithExactly('compose-scenario', 'direct-url');
        });
    });

    describe('#onHide', function() {
        beforeEach(function() {
            this.sinon.stub(this.view, 'autosaveStop');

            this.mMessagesChecked = ns.Model.get('messages-checked');
            this.sinon.stub(this.mMessagesChecked, 'resetChecked');
            this.sinon.stub(this.mMessagesChecked, 'check');
        });

        it('Должен выставить признак "композ не загружен"', function() {
            this.view.onHide();

            expect(this.undoLogger.composeLoaded).to.be.eql(false);
        });

        it('Останавливает автосохранение vComposeAutosaveMixin', function() {
            this.view.onHide();
            expect(this.view.autosaveStop).to.have.callCount(1);
        });

        xit('Сбрасывает выделение писем и триггерит перерисовку тулбара, если не QR', function() {
            this.view.isQuickReply.returns(false);
            this.view.onHide();
            expect(this.mMessagesChecked.resetChecked).to.have.callCount(1);
            expect(ns.events.trigger).to.be.calledWith('daria:vToolbarButton:restruct');
        });

        xit('Не должен сбрасывать выделение писем и триггерить перерисовку тулбара, если QR', function() {
            this.view.isQuickReply.returns(true);
            this.view.onHide();
            expect(this.mMessagesChecked.resetChecked).to.have.callCount(0);
            expect(ns.events.trigger).to.have.callCount(0);
        });
    });

    describe('#onDraftSaved', function() {
        beforeEach(function() {
            this.sinon.stub(this.view, 'isVisible').returns(true);
            this.sinon.stub(ns.page, 'go');
        });

        it('Должен изменить URL после сохранения черновика', function() {
            this.view.onDraftSaved('eventName', { mid: '123' });
            expect(ns.page.go).to.be.calledWithExactly('#compose/123', 'replace');
        });
    });

    describe('#onPageBeforeLoad', function() {
        beforeEach(function() {
            this.sinon.stub(this.view, 'checkSameUrlParams').returns(false);
            this.sinon.stub(this.view, 'clean');
            this.sinon.spy(this.view, '_destroy');

            this.mMessage = ns.Model.get('message', {ids: '11'});
            this.sinon.stub(this.mMessage, 'isTemplate').returns(false);

            this.pagesParam = [
                { 'page': 'compose2', 'params': {}  },
                { 'page': 'compose2', 'params': {} }
            ];
        });

        it('Если переход внутри страницы композа, то ничего не делать', function() {
            this.view.onPageBeforeLoad('eventName', this.pagesParam, 'url');

            expect(this.view._destroy).to.have.callCount(0);
        });

        it('Если это переход на страницу композа и была отмена отправки - уничтожаем вид и модели', function() {
            this.sinon.stub(Daria, 'composeEqualRoutes').returns(false);

            this.mComposeMessage = {
                wasSendCancelled: this.sinon.stub().returns(true)
            };

            this.view.getModel.withArgs('compose-message').returns(this.mComposeMessage);

            this.view.onPageBeforeLoad('eventName', this.pagesParam, 'url');

            expect(this.view._destroy).to.have.callCount(1);
        });

        it('Не должен изменять признак "композ загружен", если переход на страницу композа', function() {
            this.view.onPageBeforeLoad('eventName', this.pagesParam, 'url');

            expect(this.undoLogger.composeLoaded).to.be.eql(undefined);
        });

        it('Должен выставить признак "композ не загружен"', function() {
            this.view.onPageBeforeLoad('eventName', [
                { 'page': 'compose2', 'params': {}  },
                { 'page': 'not-compose2', 'params': {} }
            ], 'url');

            expect(this.undoLogger.composeLoaded).to.be.eql(false);
        });
    });

    describe('#onSaveToSending', function() {
        beforeEach(function() {
            this.mComposeMessage = {
                waitForDraftSave: this.sinon.stub()
            };

            this.sinon.stub(Daria.Statusline, 'show');
            this.view.getModel.withArgs('compose-message').returns(this.mComposeMessage);
        });

        it('Должен вызвать нотификацию', function() {
            this.view.onSaveToSending();

            expect(Daria.Statusline.show).have.callCount(1);
            expect(Daria.Statusline.show).to.have.been.calledWith({
                name: 'before_sending_message',
                hideOnTimeout: false,
                hideOnClick: false,
                isCloseButtonVisible: false
            });
        });

        it('Должен вызвать метод waitForDraftSave модели compose-message', function() {
            this.view.onSaveToSending();

            expect(this.mComposeMessage.waitForDraftSave).have.callCount(1);
        });
    });

    describe('#onSendingToSent', function() {
        beforeEach(function() {
            this.sinon.stub(this.view, 'closePopup');
            this.sinon.stub(this.view, 'send');
            this.sinon.stub(this.view, 'isVisible').returns(true);
        });
    });

    describe('#onSendingToSentFailed', function() {
        beforeEach(function() {
            this.sinon.stub(this.view, 'isVisible').returns(true);
            this.sinon.stub(this.view, 'showErrorPopup');
            this.view.silentStatuses = ['silent_status'];
            this.sinon.stub(this.view, 'setState');
            this.sinon.stub(Daria.Statusline, 'hide');
        });

        it('Должен выставить состояние "edit"', function() {
            this.view.onSendingToSentFailed('sending !> sent', { error: 'test_status' });
            expect(this.view.setState).to.be.calledWithExactly('edit');
        });

        it('Должен показать попап об ошибке', function() {
            this.view.onSendingToSentFailed('sending !> sent', { error: 'test_status' });
            expect(this.view.showErrorPopup).to.have.callCount(1);
        });

        it('Не должен показывать попап для ошибки из списка silentStatuses', function() {
            this.view.onSendingToSentFailed('sending !> sent', { error: 'silent_status' });
            expect(this.view.showErrorPopup).to.have.callCount(0);
        });

        it('Должен скрыть нотификации', function() {
            this.view.onSendingToSentFailed('sending !> sent', { error: 'test_status' });
            expect(Daria.Statusline.hide).have.callCount(1);
        });
    });

    describe('#send', function() {
        beforeEach(function() {
            this.sinon.stub(this.mComposeMessage, 'send').callsFake(() => vow.reject());
        });

        it('Должен вызвать посылку в модели mComposeMessage', function() {
            this.view.send();
            expect(this.mComposeMessage.send).to.have.callCount(1);
        });
    });

    describe('#getErrorMessageByCode', function() {
        beforeEach(function() {
            this.view.ERROR_CODES = {
                'test_code': 'test text'
            };
        });

        it('Должен вернуть текст ошибки по ее коду', function() {
            expect(this.view.getErrorMessageByCode('test_code')).to.be.equal('test text');
        });
    });

    describe('#showErrorPopup', function() {
        beforeEach(function() {
            this.sinon.stub(this.view, 'getErrorMessageByCode').returns('error_message');
            this.sinon.stub(Daria.Dialog, 'notice');
            this.sinon.stub(Daria.SendMail, 'showTryAgainPopup');
        });

        for (const code of [ 'undo_cannot_save', 'undo_message_saved' ]) {
            describe(`Попап "Не удалось отправить письмо. Повторить?", при ${code}`, function() {
                beforeEach(function() {
                    this.view.showErrorPopup(code);
                });

                it('Должен получить сообщение по коду', function() {
                    expect(this.view.getErrorMessageByCode).to.be.calledWith(code);
                });

                it('Должен вызвать Daria.SendMail.showTryAgainPopup', function() {
                    expect(Daria.SendMail.showTryAgainPopup)
                        .to.have.callCount(1)
                        .and
                        .to.have.been.calledWithExactly({
                            title: 'Произошла ошибка',
                            message: 'error_message',
                            onRetry: this.sinon.match.func,
                            onCancel: null
                        });
                });

                describe('Обработчик повторной отправки', function() {
                    beforeEach(function() {
                        this.mComposeMessage = this.getModel('compose-message');
                        this.sinon.stub(this.mComposeMessage, 'setSendWithoutUndo');
                        this.sinon.stub(this.view, 'onSend');

                        this.onRetry = Daria.SendMail.showTryAgainPopup.getCall(0).args[0].onRetry;
                        this.onRetry();
                    });

                    it('Должен выставить признак "отправка без возможности отмены"', function() {
                        expect(this.mComposeMessage.setSendWithoutUndo).to.have.callCount(1);
                    });

                    it('Должен запустить отправку', function() {
                        expect(this.view.onSend).to.have.callCount(1);
                    });
                });
            });
        }

        for (const code of [ 'delayed_cannot_save', 'delayed_message_saved' ]) {
            describe(`Попап "Не удалось отложить отправку письма, отправить прямо сейчас?, при ${code}`, function() {
                beforeEach(function() {
                    this.view.showErrorPopup(code);
                });

                it('Должен получить сообщение по коду', function() {
                    expect(this.view.getErrorMessageByCode).to.be.calledWith(code);
                });

                it('Должен вызвать Daria.SendMail.showTryAgainPopup', function() {
                    expect(Daria.SendMail.showTryAgainPopup)
                        .to.have.callCount(1)
                        .and
                        .to.have.been.calledWithExactly({
                            title: 'Произошла ошибка',
                            message: 'error_message',
                            isDelayed: true,
                            onRetry: this.sinon.match.func,
                            onCancel: null
                        });
                });

                describe('Обработчик повторной отправки', function() {
                    beforeEach(function() {
                        this.mComposeMessage = this.getModel('compose-message');
                        this.sinon.stub(this.mComposeMessage, 'setSendWithoutUndo');
                        this.sinon.stub(this.mComposeMessage, 'resetDelayed');
                        this.sinon.stub(this.view, 'onSend');

                        this.onRetry = Daria.SendMail.showTryAgainPopup.getCall(0).args[0].onRetry;
                        this.onRetry();
                    });

                    it('Должен выставить признак "отправка без возможности отмены"', function() {
                        expect(this.mComposeMessage.setSendWithoutUndo).to.have.callCount(1);
                    });

                    it('Должен сбросить отложенную отправку', function() {
                        expect(this.mComposeMessage.resetDelayed).to.have.callCount(1);
                    });

                    it('Должен запустить отправку', function() {
                        expect(this.view.onSend).to.have.callCount(1);
                    });
                });
            });
        }

        describe('Попап "Произошла ошибка"', function() {
            beforeEach(function() {
                this.view.showErrorPopup('test_code');
            });

            it('Должен получить сообщение по коду', function() {
                expect(this.view.getErrorMessageByCode).to.be.calledWith('test_code');
            });

            it('Должен вызвать Daria.Dialog', function() {
                expect(Daria.Dialog.notice).to.have.callCount(1);
            });
        });
    });

    describe('#showForgottenAttachmentsDialog', function() {
        beforeEach(function() {
            this.sinon.stub(this.view, 'isVisible').returns(true);
            this.attachView = ns.View.create('compose-forgotten-attach');
            this.sinon.stub(ns.View, 'create').withArgs('compose-forgotten-attach').returns(this.attachView);
            this.sinon.stub(this.attachView, 'updateByLayout').callsFake(() => vow.reject());
            this.sinon.stub(this.view, 'onForgottenAttachUpdated');
            this.sinon.stub(this.view, 'onForgottenAttachClose');
        });

        it('Запускает update на созданном виде compose-forgotten-attach', function() {
            this.view.showForgottenAttachmentsDialog();
            expect(this.attachView.updateByLayout).to.be.calledWith('forgotten-attach');
        });

        it('Вызывает обработчик по успешному окончанию update-a вида compose-forgotten-attach', function() {
            var promise = Vow.resolve();
            this.attachView.updateByLayout.returns(promise);

            this.view.showForgottenAttachmentsDialog();

            return promise.then(function() {
                expect(this.view.onForgottenAttachUpdated).to.have.callCount(1);
            }, this);
        });

        it('Однократно подписывается на событие закрытия вида compose-forgotten-attach', function() {
            this.view.showForgottenAttachmentsDialog();
            this.attachView.trigger('ns-view:close');
            this.attachView.trigger('ns-view:close');

            expect(this.view.onForgottenAttachClose).to.have.callCount(1);
        });
    });

    describe('#onForgottenAttachUpdated', function() {
        beforeEach(function() {
            this.sinon.stub(this.view, 'closePopup');
            this.sinon.stub(Daria.Dialog, 'open');

            this.$node = $('<div>');
            this.sinon.stub(this.view, '_forgottenAttachView').value({
                $node: this.$node,
                popupData: {
                    width: 100
                }
            });

            this.view.onForgottenAttachUpdated();
        });

        it('Закрывает существующий попап', function() {
            expect(this.view.closePopup).to.have.callCount(1);
        });

        it('Открывает попап с видом compose-forgotten-attach', function() {
            expect(Daria.Dialog.open).to.be.calledWithExactly({
                width: 100,
                body: this.$node
            });
        });
    });

    describe('#onForgottenAttachClose', function() {
        beforeEach(function() {
            this.sinon.stub(this.view, 'closePopup');
            this.sinon.useFakeTimers();
        });

        it('Закрывает попап', function() {
            this.view.onForgottenAttachClose();

            expect(this.view.closePopup).to.have.callCount(1);
        });

        it('Асинхронно запускает уничтожение вида', function() {
            var destroyStub = this.sinon.stub();
            this.sinon.stub(this.view, '_forgottenAttachView').value({
                destroy: destroyStub
            });

            this.view.onForgottenAttachClose();

            expect(destroyStub).to.have.callCount(0);
            this.sinon.clock.tick(0);
            expect(destroyStub).to.have.callCount(1);
        });
    });

    describe('#clean', function() {
        var models = [
            'compose-attachments',
            'compose-forwarded-messages',
            'compose-fsm',
            'compose-message',
            'compose-notify-noreply-options',
            'compose-signature',
            'compose-state'
        ];

        beforeEach(function() {
            this.sinon.stub(this.view, 'invalidate');

            this.view.getModel.restore();
            models.forEach(function(model) {
                this.sinon.stub(this.view.getModel(model), 'destroy');
            }, this);
        });

        it('Инвалидирует вид', function() {
            this.view.clean();
            expect(this.view.invalidate).to.have.callCount(1);
        });

        describe('Уничтожает модели →', function() {
            var models = [
                'compose-message', 'compose-state', 'compose-fsm',
                'compose-attachments',
                'compose-notify-noreply-options', 'compose-signature'
            ];

            _.each(models, function(model) {
                it(model, function() {
                    var composeModel = this.view.getModel(model);

                    this.view.clean();

                    expect(composeModel.destroy).to.have.callCount(1);
                });
            });
        });
    });

    describe('#onValidationError', function() {
        beforeEach(function() {
            var errorsObj = {
                'cc': ['blaa'],
                'bcc': ['42']
            };

            this.sinon.stub(this.mComposeState, 'setFocusField');
            this.sinon.stub(this.view, 'isVisible').returns(true);
            this.sinon.stub(this.view, 'scrollToComposeTop');

            this.view.onValidationError('eventName', errorsObj);
        });

        it('Должен поставить фокус в первое поле с ошибкой по порядку их вывода на UI', function() {
            expect(this.mComposeState.setFocusField).to.be.calledWith('cc');
        });

        it('Должен подскроллить композ к верхней границе', function() {
           expect(this.view.scrollToComposeTop).to.have.callCount(1);
        });
    });

    describe ('#onMessageRemoved', function() {
        beforeEach(function() {
            this.sinon.stub(ns.router, 'generateUrl').withArgs('messages').returns('template');
            this.sinon.stub(ns.page, 'go');
            this.onMessageRemovedData = {
                originalAction: 'remove.draft',
                messageInfo: {data: 'some data'}
            };
        });

        it('Должен перейти в папку удалённого письма, если у события originalAction ===" remove.draft"', function() {
            this.view.onMessageRemoved({}, this.onMessageRemovedData);
            expect(ns.page.go).to.be.calledWith('template');
        });

        it('Не должен никуда переходить, если у события originalAction !== "remove.draft"', function() {
            this.onMessageRemovedData.originalAction = null;
            this.view.onMessageRemoved({}, this.onMessageRemovedData);
            expect(ns.page.go).to.have.callCount(0);
        });
    });

    describe('#scrollToComposeTop', function() {
        beforeEach(function() {
            this.view.node = {
                getBoundingClientRect: this.sinon.stub().returns({ top: 20 }),
                scrollIntoView: this.sinon.stub()
            };

            this.sinon.stub(window, 'scrollTo');
            this.sinon.stub(window, 'pageYOffset').value(10);
        });

        it(
            'Если кнопка "Написать" находится в левой колонке, то должен проскролить к верхней границе композа ' +
            'с отступом от верхней границы вьюпорта',
            function() {
                this.view.scrollToComposeTop();

                expect(this.view.node.getBoundingClientRect).have.callCount(1);
                expect(window.scrollTo).have.callCount(1);
                expect(window.scrollTo).have.been.calledWith(0, 22);
                expect(this.view.node.scrollIntoView).have.callCount(0);
            }
        );
    });

});
