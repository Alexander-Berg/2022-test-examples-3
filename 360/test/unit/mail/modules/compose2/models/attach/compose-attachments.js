describe('Daria.mComposeAttachments', function() {
    beforeEach(function() {
        ns.Model.get('account-information').setData(mock['account-information'][0].data);

        this.model = ns.Model.get('compose-attachments');
    });
    describe('#request', function() {
        describe('Открывается пустой композ (в параметрах нет ids) →', function() {
            it('Возвращает резолвленный промис', function() {
                expect(this.model.request().isResolved()).to.be.ok;
            });

            it('Модель находится в состоянии ok', function() {
                this.model.request();

                expect(this.model.status).to.be.equal('ok');
            });
        });

        describe('Открывается черновик (в параметрах есть ids и нет oper) →', function() {
            beforeEach(function() {
                this.params = {ids: '111', draft: true};
                this.sinon.stub(ns.Model.info('message-body'), 'calculateParamsForMessageBody').returns(this.params);
            });

            it('Запрашивает модели mMessageBody и mComposeMessage', function() {
                this.sinon.stub(ns, 'request').callsFake(() => vow.reject());

                this.model.request();

                expect(ns.request).to.be.calledWith(['message-body', 'compose-message'], this.params);
            });

            it('Запускается инициализация модели после запроса моделей', function() {
                var that = this;
                this.sinon.stub(ns, 'request').returns(vow.resolve(['model1', 'model2']));
                this.sinon.stub(this.model, '_initByModels');

                return this.model.request().then(function() {
                    expect(that.model._initByModels).to.be.calledWith('model1', 'model2');
                });
            });
        });
    });

    describe('#_initByModels', function() {
        beforeEach(function() {
            this.sinon.stub(this.model, 'setData');
            this.sinon.stub(this.model, 'bindEventsToAttachments');
            this.attachCollection = {
                test: '1',
                addAttachToCollection: this.sinon.stub()
            };
            this.sinon.stub(Daria, 'AttachmentCollection2').returns(this.attachCollection);

            this.mComposeMessage = ns.Model.get('compose-message');

            this.mMessageBody = ns.Model.get('message-body');
            this.attachData = [{ inline: true, id: 1 }, { inline: false, id: 2 }];
            this.sinon.stub(this.mMessageBody, 'getAttachments').returns(this.attachData);

            this.attach = {test: 'test'};
            this.sinon.stub(Daria.Attachment2, 'fromMessageBody').withArgs(this.attachData[1]).returns(this.attach);

            this.sinon.stub(this.mComposeMessage, 'isReplyAny').returns(false);
        });

        it('Делает модель валидной', function() {
            this.model._initByModels(this.mMessageBody, this.mComposeMessage);
            expect(this.model.setData).to.have.callCount(1);
        });

        it('Создает коллекцию, вызывая конструктор без параметров', function() {
            this.model._initByModels(this.mMessageBody, this.mComposeMessage);
            expect(Daria.AttachmentCollection2).to.be.calledWith();
        });

        it('Кладет mComposeMessage в коллекцию', function() {
            this.model._initByModels(this.mMessageBody, this.mComposeMessage);
            expect(this.attachCollection.mComposeMessage).to.be.equal(this.mComposeMessage);
        });

        it('Привязывает обработчики к событиям коллекции', function() {
            this.model._initByModels(this.mMessageBody, this.mComposeMessage);
            expect(this.model.bindEventsToAttachments).to.have.callCount(1);
        });

        it('Добавляет в коллекцию только НЕ инлайновые аттачи', function() {
            this.model._initByModels(this.mMessageBody, this.mComposeMessage);
            expect(this.attachCollection.addAttachToCollection).to.have.callCount(1);
            expect(this.attachCollection.addAttachToCollection).to.be.calledWith(this.attach);
        });

        it('Не добавляет в коллекцию аттачи, если операция reply или reply-all', function() {
            this.mComposeMessage.isReplyAny.returns(true);
            this.model._initByModels(this.mMessageBody, this.mComposeMessage);
            expect(this.attachCollection.addAttachToCollection).to.have.callCount(0);
        });

    });
});
