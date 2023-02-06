describe('Daria.Attachment2', function() {
    var ATTACH_ID = 5;

    beforeEach(function() {
        ns.Model.get('account-information').setData(mock['account-information'][0].data);
        this.mComposeMessage = ns.Model.get('compose-message');

        this.attach = new Daria.Attachment2({
            mComposeMessage: this.mComposeMessage
        });
        this.info = {
            att_id: 'aaa',
            hid: 3
        };
        this.sinon.stub(this.attach, 'info').value(this.info);
        this.sinon.stub(this.attach.info, 'id').value(ATTACH_ID);
        this.sinon.stub(this.attach, 'getNode').returns($('<div>'));
    });

    afterEach(function() {
        this.mComposeMessage.destroy();
    });

    describe('Создание аттача из mail при пересылке', function() {
        beforeEach(function() {
            this.attachEml = new Daria.Attachment2({
                mComposeMessage: this.mComposeMessage,
                class: 'mail',
                name: 'test'
            });
        });

        it('Должно добавить разширение eml, если его нет', function() {
            expect(this.attachEml.info.name).to.eql('test.eml');
        });

        it('Должен правильно установить extension с информацией о типе eml', function() {
            expect(this.attachEml.info.extension).to.eql({ class: 'mail', preview: false, icon: 'eml' });
        });
    });

    describe('#markDeleted', function() {
        it('Должен убрать аттач из модели mComposeMessage, если аттач залитый', function() {
            this.sinon.stub(this.attach, 'status').value(Daria.Attachment2.AttachStatus.COMPLETED);
            this.sinon.stub(this.attach, '_removeFromComposeMessage');

            this.attach.markDeleted();

            expect(this.attach._removeFromComposeMessage).to.have.callCount(1);
        });

        it('Должен удалить аттач совсем, если аттач еще не залитый', function() {
            this.sinon.stub(this.attach, 'status').value(Daria.Attachment2.AttachStatus.UPLOADING);
            this.sinon.stub(this.attach, 'remove');

            this.attach.markDeleted();

            expect(this.attach.remove).to.have.callCount(1);
        });
    });

    describe('#unmarkDeleted', function() {
        it('Должен добавить аттач в модели mComposeMessage', function() {
            this.sinon.stub(this.attach, '_addToComposeMessage');
            this.attach.unmarkDeleted();

            expect(this.attach._addToComposeMessage).to.have.callCount(1);
        });
    });

    describe('#remove', function() {
        it('Должен убрать аттач из модели mComposeMessage', function() {
            this.sinon.stub(this.attach, '_removeFromComposeMessage');

            this.attach.remove();

            expect(this.attach._removeFromComposeMessage).to.have.callCount(1);
        });
    });

    describe('#metrika', function() {
        beforeEach(function() {
            this.sinon.stub(Jane, 'c');
            this.sinon.stub(Jane.Metrika, 'counter').value({ reachGoal: this.sinon.stub() });
        });

        describe('Событие добавление дискового аттача →', function() {
            it('Отправляет метрику про прикреплении, если это папка', function() {
                this.attach.info.folder = true;

                this.attach.metrika(Daria.Attachment2.METRIKA_EVENTS.ATTACHING_TO_DISK);

                expect(Jane.c).to.be.calledWith('Аттачи из Диска', 'Прикрепление', 'Папка');
            });

            it('Отправляет метрику про прикреплении, если это файл', function() {
                this.attach.info.folder = false;

                this.attach.metrika(Daria.Attachment2.METRIKA_EVENTS.ATTACHING_TO_DISK);

                expect(Jane.c).to.be.calledWith('Аттачи из Диска', 'Прикрепление', 'Файл');
            });

            it('Отправляет метрику про тип файлов', function() {
                this.sinon.stub(this.attach.info, 'mediaType').value('aaa');

                this.attach.metrika(Daria.Attachment2.METRIKA_EVENTS.ATTACHING_TO_DISK);

                expect(Jane.c).to.be.calledWith('Аттачи из Диска', 'Типы файлов', 'aaa');
            });

            it('Отправляет goal', function() {
                this.attach.metrika(Daria.Attachment2.METRIKA_EVENTS.ATTACHING_TO_DISK);

                expect(Jane.Metrika.counter.reachGoal).to.be.calledWith('ATTACHFROMDISK');
            });
        });

        it('Отправляет метрику при ошибке загрузки дискового аттача', function() {
            this.attach.metrika(Daria.Attachment2.METRIKA_EVENTS.ATTACHING_TO_DISK_FAILED);

            expect(Jane.c).to.be.calledWith('Аттачи из Диска', 'Ошибка загрузки');
        });
    });
});
