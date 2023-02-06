describe('Daria.mDone', function() {
    beforeEach(function() {
        this.mComposeMessage = ns.Model.get('compose-message');
        this.sinon.stub(this.mComposeMessage, 'get');

        this.model = ns.Model.get('done');
    });

    describe('#_getDoneTitle', function() {
        beforeEach(function() {
            this.i18n = this.sinon.stub(window, 'i18n');

            this._prevStatus = Daria.Config['db-status'];
            Daria.Config['db-status'] = 'rw';

            this.getDoneTitle = this.model._getDoneTitle.bind(this.model, this.mComposeMessage);
        });

        afterEach(function() {
            Daria.Config['db-status'] = this._prevStatus;
        });

        it('Возвращает текст про базу в ro-состоянии, если она в таковом', function() {
            this.sinon.stub(Daria.Config, 'db-status').value('ro');
            this.i18n.withArgs('%Compose_Done_отправлено_RO').returns('42');

            expect(this.getDoneTitle()).to.be.equal('42');
        });

        describe('Если есть информация об ограничениях при получении письма адресатами из-за размера', function() {
            it('Ограничение у одного получателя', function() {
                this.mComposeMessage.get.withArgs('.limited').returns({
                    recipient: {'login': '1'},
                    limit: 1500
                });
                this.i18n.withArgs('%Compose_Done_Message_Limit_Recipient', {'login': '1'}, 1500).returns('42');

                expect(this.getDoneTitle()).to.be.equal('42');
            });

            it('Ограничение у нескольких получателей', function() {
                this.mComposeMessage.get.withArgs('.limited').returns({
                    recipients: [{'login': '1'}, {'login': '2'}],
                    limit: 1500
                });
                this.i18n.withArgs('%Compose_Done_Message_Limit_Many_Recipient', [{'login': '1'}, {'login': '2'}], 1500).returns('42');

                expect(this.getDoneTitle()).to.be.equal('42');
            });
        });

        it('Возвращает текст про время отправки, если произошла отложенная отправка', function() {
            this.sinon.stub(this.mComposeMessage, 'getPassportSendDate').returns('passport time');
            this.sinon.stub(Jane.Date, 'format').withArgs('%Date_dBY_year_in_HM', 'passport time').returns('formatted passport time');

            this.i18n
                .withArgs('%Compose_Done_Письмо').returns('1')
                .withArgs('%Compose_Done_будет_отправлено', 'formatted passport time').returns('2')
                .withArgs('%Compose_Done_Лежать_в_исходящих').returns('3');

            expect(this.getDoneTitle()).to.be.equal('1&#160;2&#160;3');
        });

        it('Возвращает обычный текст, если ни одно из вышеописанных условий не сбылось', function() {
            this.i18n
                .withArgs('%Compose_Done_Письмо').returns('42')
                .withArgs('%Compose_Done_отправлено').returns('43');

            expect(this.getDoneTitle()).to.be.equal('42 43');
        });
    });

    describe('#fromComposeMessage', function() {
        beforeEach(function() {
            this.sinon.stub(this.model, 'setData');
            this.fromMessage = this.model.fromComposeMessage.bind(this.model, this.mComposeMessage);
        });

        it('Должен задать данные для успешного перехода на дан без редиректа', function() {
            this.fromMessage();

            expect(this.model.setData.getCall(0).args[0]).to.have.property('allowDone', true);
        });

        it('Должен задать текст для заголовка', function() {
            this.sinon.stub(this.model, '_getDoneTitle').returns('42');

            this.fromMessage();

            expect(this.model.setData.getCall(0).args[0]).to.have.property('title', '42');
        });

        it('Должен взять поля из mComposeMessage (to, cc, phone)', function() {
            this.sinon.stub(this.mComposeMessage, 'getData').returns({
                to: 'to1',
                cc: 'cc1',
                phone: 'phone1'
            });

            this.fromMessage();

            expect(this.model.setData.getCall(0).args[0]).to.have.property('to', 'to1');
            expect(this.model.setData.getCall(0).args[0]).to.have.property('cc', 'cc1');
            expect(this.model.setData.getCall(0).args[0]).to.have.property('phone', 'phone1');
        });
    });
});
