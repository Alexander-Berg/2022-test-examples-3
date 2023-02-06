describe('Daria.vComposePopularContacts', function() {
    beforeEach(function() {
        this.view = ns.View.create('compose-popular-contacts');

        this.$node = $('<div><div class="js-close"></div></div>');
        this.view.node = this.$node[0];
        this.view.$node = this.$node;

        this.sinon.stubGetModel(this.view,
            [ 'index-data', 'compose-message', 'compose-popular-contacts', 'compose-state' ]);

        this.sinon.stub(this.view, '_$closeButton').value(null);
    });

    describe('#onHtmlInit', function() {
        it('Запоминает ссылку на кнопку закрытия полоски', function() {
            this.view.onHtmlInit();

            expect(this.view._$closeButton.hasClass('js-close')).to.be.ok;
        });
    });

    describe('#onHtmlDestroy', function() {
        it('Удаляет ссылку на кнопку закрытия полоски', function() {
            this.sinon.stub(this.view, '_$closeButton').value({});

            this.view.onHtmlDestroy();

            expect(this.view._$closeButton).to.be.equal(null);
        });
    });

    describe('#onContactClick', function() {
        beforeEach(function() {
            this.sinon.stub(window, '$').callsFake(function(arg) {
                return arg;
            });

            this.event = {
                currentTarget: { 'data': this.dataStub = this.sinon.stub() }
            };

            this.sinon.stub(this.view, 'updateContacts');
            this.sinon.stub(this.mIndexData, 'get').withArgs('.email').returns('me@exmaple.test');
            this.sinon.stub(this.mComposePopularContacts, 'actualizeUsedContacts');
            this.sinon.stubMethods(this.mComposeMessage, ['appendContact', 'getContacts']);
        });

        it('Достает кликнутый контакт из mComposePopularContacts и добавляет его в mComposeMessage', function() {
            this.dataStub
                .withArgs('email').returns('dydka2@yandex.ru')
                .withArgs('name').returns('Invar Dydka');
            this.sinon.stub(this.mComposePopularContacts, 'getContact')
                .withArgs({ email: 'dydka2@yandex.ru', name: 'Invar Dydka' })
                .returns({ email: 'dydka2@yandex.ru', name: 'Invar Dydka' });

            this.view.onContactClick(this.event);

            expect(this.mComposeMessage.appendContact).to.be.calledWith('to', {
                email: 'dydka2@yandex.ru',
                name: 'Invar Dydka'
            });
        });

        it('Достает собственный контакт из mComposePopularContacts и добавляет его в mComposeMessage', function() {
            this.dataStub
                .withArgs('email').returns('me@exmaple.test')
                .withArgs('name').returns(i18n('%Compose_Send_it_to_me'));
            this.sinon.stub(this.mComposePopularContacts, 'getContact')
                .withArgs({ email: 'me@exmaple.test', name: i18n('%Compose_Send_it_to_me') })
                .returns({ email: 'me@exmaple.test', name: i18n('%Compose_Send_it_to_me') });

            this.view.onContactClick(this.event);

            expect(this.mComposeMessage.appendContact).to.be.calledWith('to', { email: 'me@exmaple.test' });
        });

        it('Должен вызвать обновление контактов', function() {
            this.dataStub
                .withArgs('email').returns('dydka2@yandex.ru')
                .withArgs('name').returns('Invar Dydka');
            this.sinon.stub(this.mComposePopularContacts, 'getContact')
                .withArgs({email: 'dydka2@yandex.ru', name: 'Invar Dydka'})
                .returns({email: 'dydka2@yandex.ru', name: 'Invar Dydka'});

            this.view.onContactClick(this.event);

            expect(this.view.updateContacts).to.have.callCount(1);
        });
    });

    describe('#onCloseClick', function() {
        beforeEach(function() {
            this.mSettings = ns.Model.get('settings');
            this.sinon.stub(this.mSettings, 'setSettings');
            this.sinon.stub(this.mSettings, 'setSettingOff');
            this.sinon.stub(this.mComposeState, 'trigger');
            this.getSign = this.sinon.stub(this.mSettings, 'getSign');
            this.getSetting = this.sinon.stub(this.mSettings, 'getSetting');
        });

        it('Если есть настройка популярных контактов, то выключаем ее и перерисовываем композ', function() {
            this.getSign.returns(true);
            this.getSetting.returns(true);
            this.view.onCloseClick();
            expect(this.mSettings.setSettings).to.have.callCount(0);
            expect(this.mSettings.setSettingOff).to.have.callCount(1);
            expect(this.mComposeState.trigger).to.have.callCount(1).and.calledWith('ns-model:contacts:redraw');
        });


        it('Если нет настройки популярных контактов, то просто сначала выставим ее, ' +
            'а потом выключаем ее и перерисовываем композ', function() {
            this.getSign.returns(true);
            this.getSetting.returns(undefined);
            this.view.onCloseClick();
            expect(this.mSettings.setSettings).to.have.callCount(1).and.calledWith({ compose_samples: true });
            expect(this.mSettings.setSettingOff).to.have.callCount(1);
            expect(this.mComposeState.trigger).to.have.callCount(1).and.calledWith('ns-model:contacts:redraw');
        });
    });


    describe('#onRecipientFieldChanged', function() {
        beforeEach(function() {
            this.sinon.stub(this.view, 'updateContacts');
            this.sinon.stub(this.view, 'composeUpdate');
        });

        it('Должен вызвать обновление контактов', function() {
            this.view.onRecipientFieldChanged();

            expect(this.view.updateContacts).to.have.callCount(1);
        });

        it('Должен вызвать перерисовку', function() {
            this.view.onRecipientFieldChanged();
            expect(this.view.composeUpdate).to.be.calledAfter(this.view.updateContacts);
        });
    });

    describe('#updateContacts', function() {
        beforeEach(function() {
            this.sinon.stub(this.mComposePopularContacts, 'actualizeUsedContacts');
            this.recepients = [{email: 'a@a.ru'}, {email: 'b@a.ru'}];
            this.sinon.stub(this.mComposeMessage, 'getAllRecepients').returns(this.recepients);
        });

        it('Должен актуализировать популярные контакты', function() {
            this.view.updateContacts();

            expect(this.mComposePopularContacts.actualizeUsedContacts).to.be.calledWith(this.recepients);
        });
    });
});
