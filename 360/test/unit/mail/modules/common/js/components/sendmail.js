describe('Daria.SendMail', function() {
    beforeEach(function() {
        this.mSettings = ns.Model.get('settings');
    });

    describe('#getUndoSendTime', function() {
        beforeEach(function() {
            this.sinon.stub(this.mSettings, 'getSetting').withArgs('undo_send_time').returns(5);
            this.sinon.stub(Daria, 'hasFeature').withArgs('undo-send').returns(true);
        });

        it('должен вернуть время для отмены отправки из настройки', function() {
            expect(Daria.SendMail.getUndoSendTime()).to.be.equal(5000);
        });

        it('должен вернуть 0 (фича undo-send неактивна)', function() {
            Daria.hasFeature.restore();
            this.sinon.stub(Daria, 'hasFeature').withArgs('undo-send').returns(false);

            expect(Daria.SendMail.getUndoSendTime()).to.be.equal(0);
        });

        it('должен вернуть 0 (неверное значение настройки "undo_send_time")', function() {
            this.mSettings.getSetting.restore();
            this.sinon.stub(this.mSettings, 'getSetting').withArgs('undo_send_time').returns(0);

            expect(Daria.SendMail.getUndoSendTime()).to.be.equal(0);
        });
    });

    describe('#shouldShowUndoSend', function() {
        it('должен вернуть false', function() {
            this.sinon.stub(Daria.SendMail, 'getUndoSendTime').returns(0);
            expect(Daria.SendMail.shouldShowUndoSend()).to.be.equal(false);
        });

        it('должен вернуть true', function() {
            this.sinon.stub(Daria.SendMail, 'getUndoSendTime').returns(5000);
            expect(Daria.SendMail.shouldShowUndoSend()).to.be.equal(true);
        });
    });

    describe('#showSendingMessage', function() {
        beforeEach(function() {
            this.sinon.stub(Daria.SendMail, 'shouldShowUndoSend');
            this.sinon.stub(Daria.Statusline, 'show');
            this.sinon.stub(Daria.Statusline, 'hide');

            this.options = {
                onCancel: this.sinon.stub(),
                onClose: this.sinon.stub()
            };
        });

        describe('Без кнопки Отмена', function() {
            beforeEach(function() {
                Daria.SendMail.shouldShowUndoSend.returns(false);
            });

            it('Должен показать statusline c нужными опциями', function() {
                Daria.SendMail.showSendingMessage(this.options);

                expect(Daria.Statusline.show)
                    .to.have.callCount(1)
                    .and
                    .to.be.calledWithExactly({
                        name: 'sending_message',
                        hideOnTimeout: false,
                        shouldSkipAnimation: true,
                        onClose: this.options.onClose,
                        params: {}
                    });
            });
        });

        describe('С кнопкой Отмена', function() {
            beforeEach(function() {
                Daria.SendMail.shouldShowUndoSend.returns(true);
            });

            it('Должен показать statusline c нужными опциями', function() {
                Daria.SendMail.showSendingMessage(this.options);

                expect(Daria.Statusline.show)
                    .to.have.callCount(1)
                    .and
                    .to.be.calledWith(this.sinon.match({
                        name: 'sending_message',
                        hideOnTimeout: false,
                        shouldSkipAnimation: true,
                        hideOnClick: false,
                        onClose: this.options.onClose,
                        params: {
                            onCancel: this.sinon.match.func
                        }
                    }));
            });

            describe('Клик по кнопке Отмена', function() {
                beforeEach(function() {
                    Daria.SendMail.showSendingMessage(this.options);

                    const onCancel = Daria.Statusline.show.getCall(0).args[0].params.onCancel;

                    expect(typeof onCancel).to.be.equal('function');

                    onCancel();
                });

                it('должен скрыть нотифайку "Письмо отправляется..."', function() {
                    expect(Daria.Statusline.hide)
                        .to.have.callCount(1)
                        .and
                        .to.be.calledWithExactly('sending_message');
                });

                it('должен вызвать переданный колбек onCancel', function() {
                    expect(this.options.onCancel).to.have.callCount(1);
                });
            });
        });
    });

    describe('#showMessageSent', function() {
        beforeEach(function() {
            this.sinon.stub(Daria.SendMail, '_addCancelOptions');
            this.sinon.stub(Daria.Statusline, 'show');

            this.undoLogger = {};
            this.sinon.stub(Daria.SendMail, 'getUndoSendLogger').returns(this.undoLogger);

            this.options = {
                onClose: this.sinon.stub()
            };

            this.statuslineOptions = {
                name: 'message_sent',
                shouldSkipAnimation: true,
                hideOnClick: false,
                timeout: 3000,
                onClose: this.options.onClose,
                params: {}
            };

            Daria.SendMail.showMessageSent('123', this.options);
        });

        it('должен сбросить информацию о том, что была отмена отправки', function() {
            expect(this.undoLogger.cancelled).to.be.eql(false);
        });

        it('должен добавить параметры для отмены отправки', function() {
            expect(Daria.SendMail._addCancelOptions)
                .to.have.callCount(1)
                .and
                .to.be.calledWithExactly('123', 'message_sent', this.statuslineOptions, this.options, false);
        });

        it('Должен показать statusline', function() {
            expect(Daria.Statusline.show)
                .to.have.callCount(1)
                .and
                .to.be.calledWith(this.statuslineOptions);
        });
    });

    describe('#showMessageSentDelayed', function() {
        beforeEach(function() {
            this.sinon.stub(Daria.SendMail, '_addCancelOptions');
            this.sinon.stub(Daria.Statusline, 'show');

            this.undoLogger = {};
            this.sinon.stub(Daria.SendMail, 'getUndoSendLogger').returns(this.undoLogger);

            this.sendTime = new Date(2018, 7, 29, 13, 3, 0);

            this.options = {
                onClose: this.sinon.stub()
            };

            this.statuslineOptions = {
                name: 'message_sent_delayed',
                shouldSkipAnimation: true,
                hideOnClick: false,
                timeout: 5000,
                onClose: this.options.onClose,
                params: {
                    sendTime: this.sendTime
                }
            };

            Daria.SendMail.showMessageSentDelayed('123', this.sendTime, this.options);
        });

        it('должен сбросить информацию о том, что была отмена отправки', function() {
            expect(this.undoLogger.cancelled).to.be.eql(false);
        });

        it('должен добавить параметры для отмены отправки', function() {
            expect(Daria.SendMail._addCancelOptions)
                .to.have.callCount(1)
                .and
                .to.be.calledWithExactly('123', 'message_sent_delayed', this.statuslineOptions, this.options, true);
        });

        it('Должен показать statusline', function() {
            expect(Daria.Statusline.show)
                .to.have.callCount(1)
                .and
                .to.be.calledWith(this.statuslineOptions);
        });
    });

    describe('#showTryAgainPopup', function() {
        beforeEach(function() {
            this.sinon.stub(Daria.Dialog, 'confirm');

            this.options = {
                message: 'fake message',
                title: 'fake title',
                onRetry: this.sinon.stub(),
                onCancel: this.sinon.stub()
            };
        });

        it('должен вызвать показ диалогового окна с правильными параметрами', function() {
            Daria.SendMail.showTryAgainPopup(this.options);

            expect(Daria.Dialog.confirm)
                .to.have.callCount(1)
                .and
                .to.be.calledWith({
                    title: 'fake title',
                    body: '<div class="b-popup__p">fake message</div>',
                    width: 360,
                    okValue: 'Повторить',
                    cancelValue: 'Отмена',
                    okHandler: this.options.onRetry,
                    oncancel: this.options.onCancel
                });
        });

        it('должен кинуть исключение в случае, когда не указано сообщение (options.message)', function() {
            delete this.options.message;

            expect(() => { Daria.SendMail.showTryAgainPopup(this.options); })
                .to.throw('[Daria.SendMail.showTryAgainPopup] should specify message to show');
        });
    });

    describe('#_addCancelOptions', function() {
        beforeEach(function() {
            this.sinon.stub(Daria.SendMail, 'getUndoSendTime').returns(5000);
            this.sinon.stub(Daria.SendMail, 'cancelSendMessage');

            this.options = {
                onCancel: this.sinon.stub()
            };

            this.statuslineOptions = {
                timeout: 1000,
                params: {}
            };
        });

        it('должен добавить параметры для отмены отправки письма', function() {
            Daria.SendMail._addCancelOptions('123', 'fake_message_name', this.statuslineOptions, this.options);

            expect(this.statuslineOptions.timeout).to.be.equal(5000);
            expect(this.statuslineOptions.params.hideCancelAfter).to.be.equal(5000);
            expect(typeof this.statuslineOptions.params.onCancel).to.be.equal('function');
        });

        describe('Обработчик отмены отправки', function() {
            beforeEach(function() {
                Daria.SendMail._addCancelOptions(
                    '123',
                    'fake_message_name',
                    this.statuslineOptions,
                    this.options,
                    false
                );
                this.onCancel = this.statuslineOptions.params.onCancel;
                this.onCancel();
            });

            it('должен вызвать переданный options.onCancel', function() {
                expect(this.options.onCancel)
                    .to.have.callCount(1)
                    .and
                    .to.be.calledWithExactly();
            });

            it('должен инициировать отмену отправки письма', function() {
                expect(Daria.SendMail.cancelSendMessage)
                    .to.have.callCount(1)
                    .and
                    .to.be.calledWithExactly('123', 'fake_message_name', false);
            });
        });

        it('не должен добавлять параметры для отмены отправки письма (не указан sentMid)', function() {
            Daria.SendMail._addCancelOptions(null, 'fake_message_name', this.statuslineOptions);

            expect(typeof this.statuslineOptions.params.onCancel).to.be.equal('undefined');
        });

        it('не должен добавлять параметры для отмены отправки письма (невалидное время отмены отправки)', function() {
            Daria.SendMail.getUndoSendTime.returns(0);

            Daria.SendMail._addCancelOptions('123', 'fake_message_name', this.statuslineOptions);

            expect(typeof this.statuslineOptions.params.onCancel).to.be.equal('undefined');
        });
    });

    describe('#_cancelSend', function() {
        beforeEach(function() {
            this.sinon.stub(ns, 'forcedRequest');
            this.sinon.stub(Jane.ErrorLog, 'send');
            this.sinon.stub(Daria.Statusline, 'show');

            this.model = ns.Model.get('do-cancel-send-delayed', { ids: '123' });
            this.mMessage = ns.Model.get('message', { ids: '123' });
            this.mMessage.setData({});

            this.sinon.stub(this.mMessage, 'moveToDrafts');
        });

        it('должен отметить отмену в логгере письма, отправку которого отменяют', function() {
            ns.forcedRequest.returns(vow.resolve([ this.model ]));

            Daria.SendMail._cancelSend('123', 'fake_message_name', false);

            expect(Daria.SendMail.getUndoSendLogger('123').cancelled).to.be.equal(true);
        });

        runTests('Отложенная отправка', true);

        runTests('Отправка с возможностью отмены', false);

        function runTests(title, isDelayed) {
            describe(title, function() {
                const cancelSendModelId = isDelayed ? 'do-cancel-send-delayed' : 'do-cancel-send-undo';

                beforeEach(function() {
                    this.model = ns.Model.get(cancelSendModelId, { ids: '123' });
                });

                it('должен выполнить запрос на отмену отправки и запросить отправленное письмо', function() {
                    ns.forcedRequest.returns(vow.resolve([ this.model ]));

                    return Daria.SendMail._cancelSend('123', 'fake_message_name', isDelayed)
                        .always(function() {
                            expect(ns.forcedRequest)
                                .to.have.callCount(1)
                                .and
                                .to.be.calledWithExactly([ cancelSendModelId, 'message' ], { ids: '123' });
                        });
                });

                describe('запрос завершился ошибкой', function() {
                    beforeEach(function() {
                        this.sinon.stub(this.model, 'getError').returns({ error: 'mega-fail' });

                        ns.forcedRequest.returns(vow.reject({
                            invalid: [ this.model ],
                            valid: [ this.mMessage ]
                        }));

                        return Daria.SendMail._cancelSend('123', 'fake_message_name', isDelayed)
                            .then(() => {
                                throw new Error('should fail');
                            })
                            .fail(() => vow.resolve);
                    });

                    it('логируем ошибку', function() {
                        expect(Jane.ErrorLog.send)
                            .to.have.callCount(1)
                            .and
                            .to.be.calledWithExactly({
                                errorType: 'cancel_send_failed',
                                modelId: cancelSendModelId,
                                mid: '123',
                                statusline: 'fake_message_name',
                                error: JSON.stringify({ error: 'mega-fail' })
                            });
                    });

                    it('показываем нотифайку', function() {
                        expect(Daria.Statusline.show)
                            .to.have.callCount(1)
                            .and
                            .to.be.calledWithExactly({
                                name: 'cancel_send_failed'
                            });
                    });
                });

                describe('запрос завершился успехом', function() {
                    beforeEach(function() {
                        this.model.setData({
                            status: 'ok',
                            allDone: true
                        });

                        ns.forcedRequest.returns(vow.resolve([ this.model, this.mMessage ]));
                    });

                    it('должен положить письмо черновики', function() {
                        return Daria.SendMail._cancelSend('123', 'fake_message_name', isDelayed)
                            .then(() => {
                                expect(this.mMessage.moveToDrafts).to.have.callCount(1);
                            });
                    });

                    it('не должен показывать нотифаек', function() {
                        return Daria.SendMail._cancelSend('123', 'fake_message_name', isDelayed)
                            .then(function() {
                                expect(Daria.Statusline.show).to.have.callCount(0);
                            });
                    });

                    describe('бэк что-то не доделал', function() {
                        beforeEach(function() {
                            this.model.set('.allDone', false);
                            return Daria.SendMail._cancelSend('123', 'fake_message_name', isDelayed);
                        });

                        it('должен сделать повторный запрос', function() {
                            expect(ns.forcedRequest).to.have.callCount(2);
                            expect(ns.forcedRequest.getCall(0).args)
                                .to.be.eql([ [ cancelSendModelId, 'message' ], { ids: '123' } ]);
                            expect(ns.forcedRequest.getCall(1).args)
                                .to.be.eql([ cancelSendModelId, { ids: '123' } ]);
                        });

                        it('должен залогировать факт повторного запроса', function() {
                            expect(Jane.ErrorLog.send)
                                .to.have.callCount(1)
                                .and
                                .to.be.calledWithExactly({
                                    errorType: 'cancel_send_second_request',
                                    modelId: cancelSendModelId,
                                    mid: '123',
                                    statusline: 'fake_message_name'
                                });
                        });
                    });
                });
            });
        }
    });

    describe('#cancelSendMessage', function() {
        beforeEach(function() {
            this.sinon.stub(Daria.SendMail, '_cancelSend').returns(new vow.Promise());
            this.sinon.stub(Daria.Statusline, 'hide');
            this.sinon.stub(ns.router, 'generateUrl').returns('/generated-url-compose');
            this.sinon.stub(ns.page, 'go').returns(vow.resolve());
        });

        it('должен отменить отправку', function() {
            Daria.SendMail.cancelSendMessage('123', 'fake_message_name', false);
            expect(Daria.SendMail._cancelSend)
                .to.have.callCount(1)
                .and
                .to.be.calledWithExactly('123', 'fake_message_name', false);
        });

        it('должен спрятать нотифайку', function() {
            Daria.SendMail.cancelSendMessage('123', 'fake_message_name');
            expect(Daria.Statusline.hide)
                .to.have.callCount(1)
                .and
                .to.be.calledWithExactly('fake_message_name');
        });

        it('должен выполнить переход на страницу композа', function() {
            const cancelSendPromise = vow.resolve();

            Daria.SendMail._cancelSend.returns(cancelSendPromise);

            Daria.SendMail.cancelSendMessage('123', 'fake_message_name');

            expect(ns.router.generateUrl).to.have.callCount(0);
            expect(ns.page.go).to.have.callCount(0);

            return cancelSendPromise
                .then(() => {
                    expect(ns.router.generateUrl)
                        .to.have.callCount(1)
                        .and
                        .to.be.calledWithExactly('compose2', { ids: '123' });

                    expect(ns.page.go)
                        .to.have.callCount(1)
                        .and
                        .to.be.calledWithExactly('/generated-url-compose');
                });
        });
    });

    describe('#getUndoSendLogger', function() {
        it('должен создать и вернуть созданный экземпляр логера', function() {
            const logger = Daria.SendMail.getUndoSendLogger('newids');

            expect(logger).not.to.be.equal(null);
        });

        it('должен вернуть созданный ранее экземпляр логера', function() {
            let logger1 = Daria.SendMail.getUndoSendLogger('ids');
            let logger2 = Daria.SendMail.getUndoSendLogger('ids');

            expect(logger2).to.be.equal(logger1);
        });
    });
});
