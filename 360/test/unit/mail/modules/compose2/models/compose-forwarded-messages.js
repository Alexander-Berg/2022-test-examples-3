describe('Daria.mComposeForwardedMessages', function() {
    beforeEach(function() {
        /** @type Daria.mComposeForwardedMessages */
        this.model = ns.Model.get('compose-forwarded-messages');
    });

    describe('#request', function() {
        beforeEach(function() {
            this.models = ['42', '43'];
            this.request = ['messages', 'message'];

            this.mComposeMessage = ns.Model.get('compose-message', _.clone(this.model.params));

            this.sinon.stub(ns, 'request').returns(vow.resolve());

            this.sinon.stub(this.model, '_constructModelRequest').returns(this.request);
            this.sinon.stub(this.model, '_initFromModels');
        });

        it('должен запросить mComposeMessage', function() {
            return this.model.request().then(function() {
                expect(ns.request.getCall(0)).to.be.calledWith([this.mComposeMessage]);
            }, null, this);
        });

        it('должен задать дефолтное значение в виде пустого массива', function() {
            return this.model.request().then(function() {
                expect(this.model.getData()).to.be.eql([]);
            }, null, this);
        });

        it('должен запросить данные для пересылки', function() {
            return this.model.request().then(function() {
                expect(ns.request.getCall(1))
                    .to.be.calledWith(this.request)
                    .and.to.be.calledAfter(ns.request.getCall(0));
            }, null, this);
        });
    });

    describe('#_constructModelRequest', function() {
        beforeEach(function() {
            this.sinon.stub(ns, 'request').callsFake(() => vow.reject());
        });

        it('не запрашивает mMessage/mMessage, если письма не пересылаются', function() {
            this.sinon.stub(this.model, 'params').value({ oper: '42' });

            expect(this.model._constructModelRequest()).to.be.eql([]);
        });

        it('не запрашивает mMessage/mMessage, если нет ни писем, ни тредов для пересылки', function() {
            this.sinon.stub(this.model, 'params').value({ oper: 'forward' });

            expect(this.model._constructModelRequest()).to.be.eql([]);
        });

        it('Создает правильный запрос для тредов', function() {
            this.sinon.stub(this.model, 'params').value({oper: 'forward', tids: ['42', '43']});

            expect(this.model._constructModelRequest()).to.be.eql([
                {
                    id: 'messages',
                    params: {
                        thread_id: '42',
                        first: 0,
                        count: 500
                    }
                },
                {
                    id: 'messages',
                    params: {
                        thread_id: '43',
                        first: 0,
                        count: 500
                    }
                }
            ]);
        });

        it('Создает правильный запрос для писем', function() {
            this.sinon.stub(this.model, 'params').value({oper: 'forward', ids: ['42', '43']});

            expect(this.model._constructModelRequest()).to.be.eql([
                {
                    id: 'message',
                    params: {
                        ids: '42'
                    }
                },
                {
                    id: 'message',
                    params: {
                        ids: '43'
                    }
                }
            ]);
        });
    });

    describe('#_initFromModels', function() {
        beforeEach(function() {
            this.sinon.stub(this.model, '_setMessages');
        });

        it('Разбирает модели, формируя из mMessage плоски список и передает их в #_setMessages', function() {
            var mMessage = {
                id: 'message',
                params: { ids: '2'}
            };
            var mMessagesModels = [
                {
                    id: 'message',
                    params: { ids: '3'}
                },
                {
                    id: 'message',
                    params: { ids: '4'}
                }
            ];
            var models = [
                mMessage,
                {
                    id: 'messages',
                    models: mMessagesModels
                }
            ];

            this.model._initFromModels(models);

            var arg = this.model._setMessages.getCall(0).args[0];
            expect(arg).to.be.eql([mMessage].concat(mMessagesModels));
        });
    });

    describe('#_setMessages', function() {
        function getStubFirstArg(stub) {
            return stub.getCall(0).args[0];
        }

        beforeEach(function() {
            this.getStubFirstArg = getStubFirstArg.bind(this);

            this.mMessage1 = ns.Model.get('message', {ids: '42' });
            this.mMessage2 = ns.Model.get('message', {ids: '43' });

            var methods = [
                'get',
                'getMessageLink'
            ];
            this.sinon.stubMethods(this.mMessage1, methods);
            this.sinon.stubMethods(this.mMessage2, methods);

            this.sinon.stub(this.model, 'setData');

            this.model.mComposeMessage = ns.Model.get('compose-message');
            this.sinon.stub(this.model.mComposeMessage, 'addForwardedMessages');
        });

        afterEach(function() {
            delete this.model.mComposeMessage;
        });

        describe('Чекнутость писем →', function() {
            it('Письмо будет не выбрано, если оно одно', function() {
                this.model._setMessages([this.mMessage1]);

                var data = this.getStubFirstArg(this.model.setData);
                expect(data[0].checked).to.not.be.ok;
            });

            it('Все письма будут выбраны, если их больше 1', function() {
                this.model._setMessages([this.mMessage1, this.mMessage2]);

                var data = this.getStubFirstArg(this.model.setData);
                expect(data[0].checked).to.be.ok;
                expect(data[1].checked).to.be.ok;
            });
        });

        describe('Данные для каждого письма вычисляются правильно →', function() {
            it('mid', function() {
                this.mMessage1.get.withArgs('.mid').returns('42');

                this.model._setMessages([this.mMessage1]);

                var data = this.getStubFirstArg(this.model.setData);
                expect(data[0].mid).to.be.equal('42');
            });

            it('subject', function() {
                this.mMessage1.get.withArgs('.subject').returns('42');

                this.model._setMessages([this.mMessage1]);

                var data = this.getStubFirstArg(this.model.setData);
                expect(data[0].subject).to.be.equal('42');
            });

            it('link', function() {
                this.mMessage1.getMessageLink.returns('42');

                this.model._setMessages([this.mMessage1]);

                var data = this.getStubFirstArg(this.model.setData);
                expect(data[0].link).to.be.equal('42');
            });
        });

        it('Mid\'ы чекнутых писем добавляются в mComposeMessage', function() {
            this.mMessage1.get.withArgs('.mid').returns('42');
            this.mMessage2.get.withArgs('.mid').returns('43');

            this.model._setMessages([this.mMessage1, this.mMessage2]);

            var data = this.getStubFirstArg(this.model.mComposeMessage.addForwardedMessages);
            expect(data).to.be.eql(['42', '43']);
        });
    });
});
