describe('Daria.AttachmentCollection2', function() {
    beforeEach(function() {
        ns.Model.get('account-information').setData(mock['account-information'][0].data);

        this.mComposeMessage = ns.Model.get('compose-message');
        this.mComposeState = ns.Model.get('compose-state');

        this.collection = new Daria.AttachmentCollection2({
            $node: $('<div>'),
            mComposeMessage: this.mComposeMessage,
            mComposeState: this.mComposeState
        });
        this.triggerStub = this.sinon.stub(this.collection, 'trigger');
        this.sinon.stub(this.collection, '_processAttachmentSize');

        this.attach = new Daria.Attachment2({
            mComposeMessage: ns.Model.get('compose-message')
        });
        this.sinon.stub(this.attach, 'setNode');
        this.sinon.stub(this.attach.info, 'id').value(5);
    });

    describe('#ctor', function() {
        it('Должен привязать методы к контексту', function() {
            var bindStub = this.sinon.stub(Daria.AttachmentCollection2.prototype, 'bindMethodsToContext');

            new Daria.AttachmentCollection2({
                $node: $('<div>'),
                mComposeMessage: this.mComposeMessage,
                mComposeState: this.mComposeState
            });

            expect(bindStub).to.have.callCount(1);
        });
    });

    describe('#addAttachToCollection', function() {
        beforeEach(function() {
            this.sinon.stub(this.collection, 'bindEventsToAttach');
            this.sinon.stub(this.collection, '_reduceAttachmentsSize');
            this.sinon.stub(this.collection, 'checkVisibility');
        });

        it('Должен начать учитывать размер аттача, если не передан options.processAttachSize', function() {
            var that = this;
            var promise = vow.resolve();

            this.collection.addAttachToCollection(this.attach);

            return promise.then(function() {
                expect(that.collection._processAttachmentSize).to.have.callCount(1);
            });
        });

        it('Не должен учитывать размер аттача, если передан options.processAttachSize === false', function() {
            var that = this;
            var promise = vow.resolve();

            this.collection.addAttachToCollection(this.attach, { processAttachSize: false });

            return promise.then(function() {
                expect(that.collection._processAttachmentSize).to.have.callCount(0);
            });
        });

        it('Должен добавить аттач во внутренний хеш', function() {
            this.sinon.stub(this.collection, '_attachments').value({});

            this.collection.addAttachToCollection(this.attach);

            expect(this.collection._attachments[5]).to.be.equal(this.attach);
        });

        it('Должен стригерить событие о добавлении аттача, если не передан options.silent', function() {
            this.collection.addAttachToCollection(this.attach);

            expect(this.triggerStub).to.be.calledWithExactly('attachments.added', this.attach);
        });

        it('Не должен тригерить событие об удалении аттача, если передан options.silent', function() {
            this.collection.addAttachToCollection(this.attach, { silent: true });

            expect(this.triggerStub).to.have.callCount(0);
        });

        it('Должен привязать обработчики событий к аттачу', function() {
            this.collection.addAttachToCollection(this.attach);

            expect(this.collection.bindEventsToAttach).to.be.calledWithExactly(this.attach);
        });
    });

    describe('#removeAttachFromCollection', function() {
        beforeEach(function() {
            this.sinon.stub(this.collection, 'unbindEventsFromAttach');
            this.sinon.stub(this.collection, '_reduceAttachmentsSize');
            this.sinon.stub(this.collection, 'checkVisibility');
        });

        it('Должен отвязать обработчики событий от аттача', function() {
            this.collection.removeAttachFromCollection(this.attach);

            expect(this.collection.unbindEventsFromAttach).to.be.calledWithExactly(this.attach);
        });

        it('Должен убрать аттач из вычисления итоговой высоты', function() {
            this.collection.removeAttachFromCollection(this.attach);

            expect(this.collection._reduceAttachmentsSize).to.be.calledWithExactly(this.attach);
        });

        it('Должен вычислить высоту заново', function() {
            this.collection.removeAttachFromCollection(this.attach);

            expect(this.collection.checkVisibility).to.have.callCount(1);
        });

        it('Должен стригерить событие об удалении аттача, если не передан options.silent', function() {
            this.collection.removeAttachFromCollection(this.attach);

            expect(this.triggerStub).to.be.calledWithExactly('attachments.removed', this.attach);
        });

        it('Не должен тригерить событие об удалении аттача, если передан options.silent', function() {
            this.collection.removeAttachFromCollection(this.attach, { silent: true });

            expect(this.triggerStub).to.have.callCount(0);
        });

        it('Должен удалить аттач из внутреннего хеша', function() {
            this.sinon.stub(this.collection, '_attachments').value({5: this.attach});

            this.collection.removeAttachFromCollection(this.attach);

            expect(this.collection._attachments[5]).to.be.equal(undefined);
        });
    });

    describe('#addDiskAttach', function() {
        beforeEach(function() {
            this.sinon.stubMethods(this.collection, [
                'addAttachToCollection',
                'removeMarkedAsDeleted',
                '_appendAttach',
                '_uploadQueue'
            ]);

            this.sinon.stub(this.attach, 'attachFromYaDisk').returns(new $.Deferred().reject().promise());
            this.sinon.stub(Daria, 'Attachment2').returns(this.attach);

            this.resource = {
                name: 'test',
                type: 'image',
                meta: {
                    mediatype: 'image',
                    preview: 'preview',
                    size: 100
                }
            };
        });

        it('Добавляет его в коллекцию с опцией не добавления его размера к суммарному размеру аттачей', function() {
            this.collection.addDiskAttach(5, this.resource);

            expect(this.collection.addAttachToCollection).to.be.calledWithExactly(this.attach, {
                processAttachSize: false
            });
        });

        it('Добавляет аттач в DOM', function() {
            this.collection.addDiskAttach(5, this.resource);

            expect(this.collection._appendAttach).to.be.calledWith(this.attach);
        });

        it('Прикрепляет аттач к письму', function() {
            this.collection.addDiskAttach(5, this.resource);

            expect(this.attach.attachFromYaDisk).to.have.callCount(1);
        });

        it('Возобновляет заливку очереди аттачей по окончанию загрузки текущего', function() {
            var that = this;
            var promise = new $.Deferred().resolve().promise();
            this.attach.attachFromYaDisk.returns(promise);

            this.collection.addDiskAttach(5, this.resource);

            return promise.then(function() {
                expect(that.collection._uploadQueue).to.have.callCount(1);
            });
        });

        it('Удаляет помеченные удаленными аттачи', function() {
            this.collection.addDiskAttach(5, this.resource);

            expect(this.collection.removeMarkedAsDeleted).to.have.callCount(1);
        });
    });

    describe('#_appendAttach', function() {
        beforeEach(function() {
            this.sinon.stub(this.attach, 'getNode').returns($('<div>'));
            this.attach.type = 'simple';
        });

        it('Триггерит событие о добавлении аттача в DOM, если preventCheckVisibility == false', function() {
            this.collection._appendAttach(this.attach, true, false);

            expect(this.triggerStub).to.be.calledWithExactly('attachments.appended', this.attach);
        });

        it('Не триггерит событие о добавлении аттача в DOM, если preventCheckVisibility == true', function() {
            this.collection._appendAttach(this.attach, true, true);

            expect(this.triggerStub).to.have.callCount(0);
        });
    });

    describe('#_appendAttachBulk', function() {
        beforeEach(function() {
            this.sinon.stub(this.collection, '_appendAttach');
        });

        it('Добавляет каждый аттач в DOM без генерации события добавления на каждую вставку', function() {
            this.collection._appendAttachBulk([ this.attach ], $('<div>'));

            expect(this.collection._appendAttach).to.have.callCount(1);
            expect(this.collection._appendAttach).to.be.calledWithExactly(this.attach, false, true);
        });

        it('Триггерит событие о вставке аттача в DOM только один раз', function() {
            var attachments = [ this.attach ];
            this.collection._appendAttachBulk(attachments, $('<div>'));

            expect(this.triggerStub).to.have.callCount(1);
            expect(this.triggerStub).to.be.calledWithExactly('attachments.appended', attachments);
        });
    });
});
