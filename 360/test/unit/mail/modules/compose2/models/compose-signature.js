describe('Daria.mComposeSignature', function() {

    beforeEach(function() {
        this.model = ns.Model.get('compose-signature');

        return this.model.request();
    });

    describe('#hasSignatureControl', function() {
        it('должен вернуть false, если среда запуска приложения - iOs', function() {
            this.sinon.stub(Modernizr, 'ios').value(true);

            expect(this.model.hasSignatureControl()).to.be.equal(false);
        });

        it('должен вернуть true, если среда запуска приложения - не iOs', function() {
            this.sinon.stub(Modernizr, 'ios').value(false);

            expect(this.model.hasSignatureControl()).to.be.equal(true);
        });

        it('должен возвращать true по умолчанию', function() {
            expect(this.model.hasSignatureControl()).to.be.equal(true);
        });
    });

    describe('#request', function() {

        beforeEach(function() {
            this.modelNames = ['settings', 'compose-message'];
            this.sinon.stub(ns, 'request').returns({
                then: function(callback) {
                    callback();
                }
            });

            this.requestOptions = [
                {
                    id: 'test',
                    params: {}
                }
            ];
            this.sinon.stubMethods(this.model, [
                '_constructModelRequest',
                '_initFromModels'
            ]);

            this.model._constructModelRequest.returns(this.requestOptions);
        });

        it('Должен запросить модели по сформированному запросу', function() {
            this.model.request();

            expect(ns.request).to.be.calledWith(this.requestOptions);
        });

        it('должен после запроса вызвать инициализацию модели', function() {
            this.model.request();

            expect(this.model._initFromModels).to.have.callCount(1);
        });
    });

    describe('#_initFromModels', function() {
        it('должен установить дефолтные данные в модель', function() {
            this.model._initFromModels([]);

            expect(this.model.getData()).to.eql({ signature: '' });
        });

        it('должен запомнить запрошенные модели в свойствах текущей', function() {
            var models = [
                {}, {}
            ];
            this.model._initFromModels(models);

            expect(this.model.mSettings).to.be.equal(models[0]);
            expect(this.model.mComposeMessage).to.be.equal(models[1]);
        });

    });

    describe('#getSignatureList', function() {

        beforeEach(function() {
            this.model.mComposeMessage = ns.Model.get('compose-message');
            this.sinon.stub(this.model.mComposeMessage, 'get');
            this.sinon.stub(this.model.mComposeMessage, 'getLanguage');

            this.mode = this.model.mComposeMessage.get.withArgs('.ttype');
        });

        it('должен вернуть список подписей для html режима ввода текста письма', function() {
            var signs = [{}, {}];
            this.sinon.stub(Daria.signs, 'getHtml').returns(signs);
            this.sinon.stub(Daria.signs, 'getFilteredSigns').returns(signs);
            this.mode.returns('html');

            expect(this.model.getSignatureList()).to.eql({
                mode: 'html',
                signs: signs
            });
        });

        it('должен вернуть список подписей для plain режима ввода текста письма', function() {
            var signs = [{}, {}];
            this.sinon.stub(Daria.signs, 'getPlain').returns(signs);
            this.sinon.stub(Daria.signs, 'getFilteredSigns').returns(signs);
            this.mode.returns('plain');

            expect(this.model.getSignatureList()).to.eql({
                mode: 'plain',
                signs: signs
            });
        });

        it('должен подготовить тип сортировки для подписей на основе языка письма и текущего email отправителя', function() {
            this.sinon.stub(Daria.signs, 'sort');
            this.model.mComposeMessage.get.withArgs('.from_mailbox').returns('test@example.com');
            this.model.mComposeMessage.getLanguage.returns('ru');
            this.sinon.stub(Daria.signs, 'getFilteredSigns').returns([{
                userLang: 'ru',
                lang: 'ru',
                emails: [ 'test@example.com' ]
            }]);
            this.model.getSignatureList();

            expect(Daria.signs.sort).to.be.calledWithExactly('test@example.com', 'ru');
        });

        it('должен отсортировать список подписей по полученному типу сортировки', function() {
            this.sinon.stub(Daria.signs, 'sort');
            var signs = [{}, {}];
            var sort = function() {};
            this.sinon.stub(Daria.signs, 'getHtml').returns(signs);
            this.sinon.stub(signs, 'sort');
            this.mode.returns('html');
            Daria.signs.sort.returns(sort);
            this.sinon.stub(Daria.signs, 'getFilteredSigns').returns(signs);
            this.model.getSignatureList();

            expect(signs.sort).to.be.calledWithExactly(sort);
        });

        it('должен вернуть все подписи #1 (есть привязанные и дефолтные подписи)', function() {
            var signs = [
                {
                    lang: 'ru',
                    emails: [ 'user1@yandex.ru' ]
                }, {
                    lang: 'en',
                    emails: [ 'test@example.com' ]
                }, {
                    lang: 'ru',
                    emails: []
                }
            ];
            this.mode.returns('html');
            this.model.mComposeMessage.get.withArgs('.from_mailbox').returns('test@example.com');
            this.model.mComposeMessage.getLanguage.returns('ru');
            this.sinon.stub(Daria.signs, 'getHtml').returns(signs);
            this.sinon.stub(Daria.signs, 'getFilteredSigns').returns([signs[1], signs[2]]);
            expect(this.model.getSignatureList()).to.be.eql({
                mode: 'html',
                signs: [
                {
                    lang: 'en',
                    emails: [ 'test@example.com' ]
                },
                {
                    lang: 'ru',
                    emails: []
                }, {
                    lang: 'ru',
                    emails: [ 'user1@yandex.ru' ]
                }
            ]});
        });

        it('должен вернуть все подписи #2 (есть только привязанные подписи)', function() {
            var signs = [
                {
                    lang: 'ru',
                    emails: [ 'user1@yandex.ru' ]
                }, {
                    lang: 'en',
                    emails: [ 'test@example.com' ]
                }, {
                    lang: 'ru',
                    emails: [ 'user2@yandex.ru' ]
                }
            ];
            this.mode.returns('html');
            this.model.mComposeMessage.get.withArgs('.from_mailbox').returns('test@example.com');
            this.model.mComposeMessage.getLanguage.returns('ru');
            this.sinon.stub(Daria.signs, 'getHtml').returns(signs);
            this.sinon.stub(Daria.signs, 'getFilteredSigns').returns([signs[1], signs[2]]);
            expect(this.model.getSignatureList()).to.be.eql({
                mode: 'html',
                signs: [
                    {
                        lang: 'en',
                        emails: [ 'test@example.com' ]
                    },
                    {
                        lang: 'ru',
                        emails: [ 'user2@yandex.ru' ]
                    }, {
                        lang: 'ru',
                        emails: [ 'user1@yandex.ru' ]
                    }
                ]});
        });

        it('должен вернуть все подписи #2 (есть только привязанные подписи и нет привязанной к нужному адресу)', function() {
            var signs = [
                {
                    lang: 'ru',
                    emails: [ 'user1@yandex.ru' ]
                }, {
                    lang: 'en',
                    emails: [ 'test@example.com' ]
                }, {
                    lang: 'ru',
                    emails: [ 'user2@yandex.ru' ]
                }
            ];
            this.mode.returns('html');
            this.model.mComposeMessage.get.withArgs('.from_mailbox').returns('test11@example.com');
            this.model.mComposeMessage.getLanguage.returns('ru');
            this.sinon.stub(Daria.signs, 'getHtml').returns(signs);
            this.sinon.stub(Daria.signs, 'getFilteredSigns').returns([]);
            expect(this.model.getSignatureList()).to.be.eql({
                mode: 'html',
                signs: [
                    {
                        lang: 'ru',
                        emails: [ 'user1@yandex.ru' ]
                    },
                    {
                        lang: 'en',
                        emails: [ 'test@example.com' ]
                    }, {
                        lang: 'ru',
                        emails: [ 'user2@yandex.ru' ]
                    }
                ],
                noSuitableSignature: true
            });
        });

    });
});
