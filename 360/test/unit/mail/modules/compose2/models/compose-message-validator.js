describe('compose-message-validator', function() {
    beforeEach(function() {
        this.mComposeMessage = ns.Model.get('compose-message', {});

        const mAccountInformation = ns.Model.get('account-information');
        this.sinon.stub(mAccountInformation, 'getFromDefaultEmail').returns('me@me.com');
        this.sinon.stub(mAccountInformation, 'getAllUserEmails')
            .returns([ 'me@me.com', 'other@me.com' ]);
        this.sinon.stub(Daria.Recipients, '_makeRequest').callsFake(vow.Promise.resolve);

        setModelByMock(ns.Model.get('signs'));
        this.mSettings = ns.Model.get('settings');
        this.sinon.stubMethods(this.mSettings, [
            'getSetting'
        ]);
        this.mComposeMessage.mSettings = this.mSettings;

        this.mComposePredefinedData = ns.Model.get('compose-predefined-data');
        this.mComposeMessage.mComposePredefinedData = this.mComposePredefinedData;

        this.mComposeMessage._initFromModels([
            this.mSettings,
            this.mComposePredefinedData,
            this.mAccountInformation
        ]);

        setupMockRecipients(this);
    });

    describe('#validate', function() {
        it('возвращает промис', function() {
            expect(this.mComposeMessage._checkData() instanceof vow.Promise).to.be.equal(true);
        });

        it('в случае успеха - промис резолвится без указания результата', function() {
            this.mComposeMessage.set('.to', this.email1);

            return this.mComposeMessage._checkData().then(function(result) {
                expect(result).to.be.equal(undefined);
            });
        });

        it('в случае неуспеха - промис реджектится с объектом ошибок по каждому невалидному полю', function() {
            this.mComposeMessage.set('.to', this.email2);
            this.mComposeMessage.set('.cc', this.email1); // валидный
            this.mComposeMessage.set('.bcc', this.email3);

            return this.mComposeMessage._checkData().fail(function(errors) {
                expect(errors).to.be.eql({
                    to: 'Некорректные адреса: TWOOOO@domain2 (Алиса)',
                    bcc: 'Некорректные адреса: three@@sdfsdf.sdfsdf'
                });
            });
        });

        describe('если для поля указано несколько валидаторов - возвращается ошибка из первого ' +
            'вернувшего ошибку валидатора (на примере поля to)', function() {
            it('поле to не заполнено (первый validator)', function() {
                return this.mComposeMessage._checkData().fail(function(errors) {
                    expect(errors).to.be.eql({
                        to: 'Поле не заполнено. Необходимо ввести адрес.'
                    });
                });
            });

            it('в поле to указаны невалидные имейлы (второй validator)', function() {
                this.mComposeMessage.set('.to', this.email2 + ', ' + this.email3);
                return this.mComposeMessage._checkData().fail(function(errors) {
                    expect(errors).to.be.eql({
                        to: 'Некорректные адреса: TWOOOO@domain2 (Алиса), three@@sdfsdf.sdfsdf'
                    });
                });
            });
        });
    });

    function setupMockRecipients(testRun) {
        testRun.email1 = '"Волож Аркадий" <volozh-test@ya.ru>';
        const validRecipient1 = {
            cacheId: '"Волож Аркадий" <volozh-test@ya.ru>',

            "displayName": "Волож Аркадий",
            "type": "none",
            "color": "#b8c1d9",
            "mono": "ВА",
            "local": "volozh-test",
            "url": "",
            "urlSmall": "",
            "domain": "ya.ru",
            "valid": true,
            "email": "Волож Аркадий <volozh-test@ya.ru>",
            "fallbackAvatar": "//betastatic.yastatic.net/mail/socialavatars/socialavatars/v3/person.svg",
            "shortMono": false,
            "normalized": true
        };

        testRun.email2 = '"Алиса" <TWOOOO@domain2>';
        const invalidRecipient2 = {
            cacheId: '"Алиса" <TWOOOO@domain2>',

            "displayName": "Алиса",
            "type": "none",
            "color": "#8f98b3",
            "mono": "А",
            "local": "twoooo",
            "url": "",
            "urlSmall": "",
            "domain": "domain2",
            "valid": true,
            "email": "Алиса <twoooo@domain2>",
            "fallbackAvatar": "//betastatic.yastatic.net/mail/socialavatars/socialavatars/v3/person.svg",
            "shortMono": true,
            "normalized": true
        };

        testRun.email3 = 'three@@sdfsdf.sdfsdf';
        const invalidRecipient3 = {
            cacheId: '<three@@sdfsdf.sdfsdf>',

            "displayName": null,
            "type": "none",
            "local": null,
            "url": "",
            "urlSmall": "",
            "domain": null,
            "valid": false,
            "email": "<three@@sdfsdf.sdfsdf>",
            "fallbackAvatar": "//betastatic.yastatic.net/mail/socialavatars/socialavatars/v3/person.svg",
            "shortMono": false,
            "normalized": true
        };

        Daria.Recipients.cacheRecipient(validRecipient1);
        Daria.Recipients.cacheRecipient(invalidRecipient2);
        Daria.Recipients.cacheRecipient(invalidRecipient3);
    }
});
