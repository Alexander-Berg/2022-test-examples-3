describe('Daria.mMessagesItemWidget', function() {
    beforeEach(function() {
        this.view = ns.View.create('messages-item-widget', { ids: '123456' });
        this.mMessage = ns.Model.get('message', { ids: '123456' });

        this.mMessageBody = ns.Model.get('message-body', { ids: '123456' });
        this.mComposeMessage = ns.Model.get('compose-message', { ids: '123456', oper: 'template' });

        this.mComposeMessage.mMessage = this.mMessage;
        this.mComposeMessage.mMessageBody = this.mMessageBody;
        this.mComposeMessage.mComposeState = ns.Model.get('compose-state', {
            ids: '123456', oper: 'template'
        });

        this.sinon.stubGetModel(this.view, [ this.mMessage, this.mMessageBody, this.mComposeMessage ]);

        this.sinon.stub(Jane.ErrorLog, 'send');
        this.sinon.stub(this.view, 'openMessage');
        this.sinon.stub(this.view, 'onClickControl');
    });

    describe('onClickCompose ->', function() {
        beforeEach(function() {
            this.sinon.stub(this.view, 'createCustomTemplate');
        });

        describe('Есть mid ->', function() {
            beforeEach(function() {
                this.sinon.stub(this.mMessage, 'getMessagesListWidget')
                    .returns({
                        controls: [
                            { type: 'compose', attributes: { mid: '123455' } }
                        ]
                    });
            });
            it('Если модель message есть на сервере, то должны вызвать создание шаблона', function() {
                var promise = vow.resolve();
                this.sinon.stub(ns, 'request').returns(promise);
                var view = this.view;

                var evt = {
                    preventDefault: this.sinon.stub()
                };

                this.view.onClickCompose(evt);

                return promise.then(function() {
                    expect(view.createCustomTemplate).to.have.callCount(1);
                });
            });

            it('Если модели message нет на сервере, то не вызываем создание шаблона', function() {
                var promise = vow.reject();
                var view = this.view;
                var evt = {
                    preventDefault: this.sinon.stub()
                };
                this.sinon.stub(ns, 'request').returns(promise);

                this.view.onClickCompose(evt);

                return promise.then(function() {
                }).fail(function() {
                    expect(view.createCustomTemplate).to.have.callCount(0);
                    expect(Jane.ErrorLog.send).to.have.callCount(1);
                    expect(Jane.ErrorLog.send).to.have.calledWithExactly({
                        type: 'widget-bounce-compose-no-message',
                        mid: '123455'
                    });
                    expect(view.openMessage).to.have.callCount(1);
                });
            });
        });

        describe('Нет mid ->', function() {
            it('Пустой массив attributes -> Ничего не делаем', function() {
                this.mMessage.setData({
                    widget: {
                        controls: {
                            type: 'compose',
                            attributes: [ {} ]
                        }
                    }
                });
                this.sinon.stub(ns.page, 'go');
                this.view.onClickCompose({
                    preventDefault: this.sinon.stub()
                });
                expect(ns.page.go).to.have.callCount(0);
            });
        });
    });

    describe('createCustomTemplate -> ', function() {
        beforeEach(function() {
            this.sinon.stub(this.mMessage, 'getMessagesListWidget')
                .returns({
                    controls: [
                        {
                            type: 'compose',
                            attributes: {
                                mid: '123455'
                            }
                        }
                    ]
                });

            this.sinon.stub(this.view, '_createCustomTemplate');
        });

        it('Если есть compose-message, вызываем непосредственное формирование кастомного шаблона', function() {
            var composeMessage = ns.Model.get('compose-message', { ids: '123455' });
            composeMessage.setData({ test: '1' });
            var promise = vow.resolve([ composeMessage ]);

            var view = this.view;

            this.sinon.stub(ns, 'request').returns(promise);

            this.view.createCustomTemplate({ ids: '123455' });

            return promise.then(function() {
                expect(view._createCustomTemplate).to.have.callCount(1);
            }).fail(function() {
            });
        });

        it('Если есть compose-message, вызываем непосредственное формирование кастомного шаблона', function() {
            var promise = vow.reject();
            var view = this.view;
            this.sinon.stub(ns, 'request').returns(promise);

            this.view.createCustomTemplate({ ids: '123455' });

            return promise.then(function() {
            }).fail(function() {
                expect(view._createCustomTemplate).to.have.callCount(0);
                expect(Jane.ErrorLog.send).to.have.callCount(1);
                expect(Jane.ErrorLog.send).to.have.calledWithExactly({ type: 'widget-bounce-compose-no-compose-message',
                    mid: '123455' });
            });
        });
    });

    describe('_createCustomTemplate -> ', function() {
        beforeEach(function() {
            /*eslint new-cap: "warn"*/
            this.sinon.stub(ns.page, 'go').returns(new vow.resolve());
            this.sinon.stub(ns.router, 'generateUrl');
            this.mComposeMessage.setData({});
        });
        describe('Новое письмо по нажатию на контрол Исправить ->', function() {
            beforeEach(function() {
                this.sinon.stub(this.mMessage, 'getMessagesListWidget')
                    .returns({
                        controls: [
                            {
                                type: 'compose',
                                attributes: {
                                    mid: '123455',
                                    fix_rcpt: false
                                }
                            }
                        ]
                    });
            });

            it('Формируется правильная ссылка на композ', function(done) {
                this.sinon.stub(this.mComposeMessage, 'getMessageType');

                this.view._createCustomTemplate(this.mComposeMessage, {
                    ids: '123456',
                    oper: 'template'
                }).then(function() {
                    expect(ns.router.generateUrl).to.have.callCount(1);
                    expect(ns.router.generateUrl).to.be.calledWith('compose2', { ids: '123456', oper: 'template' });
                    done();
                });
            });
            it('Если письмо plain, то в режиме без оформления убираем теги', function(done) {
                this.sinon.stub(this.mComposeMessage, 'getMessageType').returns('plain');

                this.mComposeMessage.mMessage.setData({
                    subject: 'Subject'
                });

                this.mComposeMessage.mMessageBody.setData({
                    body: [ {
                        type: 'text',
                        subtype: 'plain',
                        content: '<div class="day">Какой чудесный день!</div>'
                    } ]
                });

                this.view._createCustomTemplate(this.mComposeMessage, {
                    ids: '123456',
                    oper: 'template'
                }).then(function() {
                    expect(this.mComposeMessage.getData()).to.be.eql({
                        overwrite: '123456',
                        ign_overwrite: 'yes',
                        save_symbol: 'draft',
                        ttype: 'plain',
                        from_name: '',
                        from_mailbox: '',
                        inreplyto: null,
                        references: null,
                        current_folder: null,
                        subj: 'Subject',
                        send: 'Какой чудесный день!'
                    });
                    done();
                }, this);
            });
            it('Если письмо plain, то в режиме c оформлением убираем теги', function(done) {
                this.sinon.stub(this.mComposeMessage, 'getMessageType').returns('html');

                this.mComposeMessage.mMessage.setData({
                    subject: 'Subject'
                });

                this.mComposeMessage.mMessageBody.setData({
                    body: [ {
                        type: 'text',
                        subtype: 'html',
                        content: '<div class="day">Какой чудесный день!</div>'
                    } ]
                });

                this.view._createCustomTemplate(this.mComposeMessage, {
                    ids: '123456',
                    oper: 'template'
                }).then(function() {
                    expect(this.mComposeMessage.getData()).to.be.eql({
                        overwrite: '123456',
                        ign_overwrite: 'yes',
                        save_symbol: 'draft',
                        ttype: 'html',
                        from_name: '',
                        from_mailbox: '',
                        inreplyto: null,
                        references: null,
                        current_folder: null,
                        subj: 'Subject',
                        send: '<div class="day">Какой чудесный день!</div>'
                    });
                    done();
                }, this);
            });

            describe('fix_rcpt ->', function() {
                beforeEach(function() {
                    this.mComposeMessage.mComposeState.setData({});
                    this.mComposeMessage.mMessage.setData({
                        subject: '1231232'
                    });

                    this.sinon.stub(this.mComposeMessage, 'getMessageType').returns('plain');
                    this.sinon.stub(this.mComposeMessage.mComposeState, 'setFocusField');
                });
                it('fix_rcpt=false -> Устанавливаем фокус в тело письма', function(done) {
                    this.view._createCustomTemplate(this.mComposeMessage, {
                        ids: '123456',
                        oper: 'template'
                    }, false).then(function() {
                        expect(this.mComposeMessage.mComposeState.setFocusField).to.be.calledWith('send');
                        done();
                    }, this);
                });
                it('fix_rcpt не передан -> Устанавливаем фокус в тело письма', function(done) {
                    this.view._createCustomTemplate(this.mComposeMessage, {
                        ids: '123456',
                        oper: 'template'
                    }).then(function() {
                        expect(this.mComposeMessage.mComposeState.setFocusField).to.be.calledWith('send');
                        done();
                    }, this);
                });
                it('fix_rcpt=true -> Устанавливаем фокус в тело письма', function(done) {
                    this.view._createCustomTemplate(this.mComposeMessage, {
                        ids: '123456',
                        oper: 'template'
                    }, true).then(function() {
                        expect(this.mComposeMessage.mComposeState.setFocusField).to.be.calledWith('to');
                        done();
                    }, this);
                });
            });
        });
    });
});
