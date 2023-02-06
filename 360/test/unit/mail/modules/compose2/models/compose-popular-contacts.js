describe('Daria.mComposePopularContacts', function() {
    beforeEach(function() {
        this.model = ns.Model.get('compose-popular-contacts');
        this.mComposeMessage = ns.Model.get('compose-message');
        this.mAbookSuggest = ns.Model.get('abook-suggest');
        this.mIndexData = ns.Model.get('index-data');
    });

    describe('#_constructModelRequest', function() {
        beforeEach(function() {
            this.params = {
                'test': '42',
                'count': 666
            };
            this.sinon.stub(this.model, 'params').value(this.params);
        });
        it('Должен правильно сконструировать запрос подмоделей', function() {
             expect(this.model._constructModelRequest()).to.be.eql([
                 {
                     'id': 'abook-suggest',
                     'params': {
                         'q': '',
                         'popular': true,
                         'climit': 666
                     }
                 },
                 {
                     'id': 'compose-message',
                     'params': {
                         'test': '42',
                         'count': 666
                     }
                 },
                 {
                     'id': 'index-data'
                 }
             ]);
        });
    });

    describe('#_initFromModels', function() {
        beforeEach(function() {
            this.modelsHash = {
                'compose-message': this.mComposeMessage,
                'abook-suggest': this.mAbookSuggest,
                'index-data': this.mIndexData
            };

            var toContacts = [
                {
                    email: 'dydka2@yandex.ru',
                    name: 'Ivan Dydka'
                }
            ];

            var popularContacts = [
                {
                    email: 'dydka2@yandex.ru',
                    name: 'Ivan Dydka'
                },
                {
                    email: 'dydka6@yandex.ru',
                    name: 'Vladimir Dydka6'
                }
            ];

            this.sinon.stub(this.model, 'setData');

            this.sinon.stub(this.mComposeMessage, 'getContacts').withArgs('to').returns(toContacts);
            this.sinon.stub(this.mAbookSuggest, 'get').withArgs('.contacts').returns(popularContacts);
            this.sinon.stub(this.mIndexData, 'get').withArgs('.email').returns('sender@example.test');
        });

        it('Правильно проставляет признак использования контакта в поле `to`', function() {
            this.model._initFromModels(this.modelsHash);

            expect(this.model.setData).to.be.calledWith([
                {
                    email: 'sender@example.test',
                    name: i18n('%Compose_Send_it_to_me'),
                    phone: undefined,
                    used: false
                },
                {
                    email: 'dydka2@yandex.ru',
                    name: 'Ivan Dydka',
                    phone: undefined,
                    used: true
                },
                {
                    email: 'dydka6@yandex.ru',
                    name: 'Vladimir Dydka6',
                    phone: undefined,
                    used: false
                }
            ]);
        });
    });

    describe('#request', function() {
        beforeEach(function() {
            this.sinon.stubMethods(this.model, ['_constructModelRequest', '_initFromModels']);
            this.sinon.stub(ns, 'request').callsFake(() => vow.Promise.reject());
        });

        it('Посылает сконструированный запрос', function() {
            this.model._constructModelRequest.returns({'test': '42'});

            this.model.request();

            expect(ns.request).to.be.calledWith({'test': '42'});
        });

        it('Инициализирует модель полученными подмоделями после успешного запроса', function() {
            ns.request.returns(vow.Promise.resolve([this.mAbookSuggest, this.mComposeMessage]));

            return this.model.request().then(function() {
                expect(this.model._initFromModels).to.be.calledWith({
                    'abook-suggest': this.mAbookSuggest,
                    'compose-message': this.mComposeMessage
                });
            }, this);
        });
    });

    describe('#getContact', function() {
        beforeEach(function() {
            var contacts = [
                { email: 'email1', name: 'name1' },
                { email: 'email2', name: 'name2' },
                { email: 'email2', name: 'name3' }
            ];
            this.sinon.stub(this.model, 'getData').returns(contacts);
        });

        describe('Выбирает первый контакт, удовлетворяющий критерию выбора →', function() {
            it('Кейс 1', function() {
                expect(this.model.getContact({'email': 'email0'})).to.be.equal(undefined);
            });

            it('Кейс 2', function() {
                expect(this.model.getContact({'email': 'email2'})).to.be.eql({ email: 'email2', name: 'name2' });
            });

            it('Кейс 3', function() {
                expect(this.model.getContact({'email': 'email2', name: 'name3'})).to.be.eql({'email': 'email2', name: 'name3'});
            });
        });
    });

    describe('#actualizeUsedContacts', function() {
        beforeEach(function() {
            this.contacts = [
                {
                    email: 'dydka2@yandex.ru',
                    name: 'Ivan Dydka',
                    used: true
                },
                {
                    email: 'dydka6@yandex.ru',
                    name: 'Vladimir Dydka6',
                    used: false
                }
            ];

            this.toContacts = [
                {
                    email: 'dydka6@yandex.ru',
                    name: 'Vladimir Dydka6',
                }
            ];
            this.sinon.stub(this.model, 'getData').returns(this.contacts);
        });

        it('Проставляет атрибут использования у популярного контакта в зависимости от контактов в поле `to`', function() {
            this.model.actualizeUsedContacts(this.toContacts);

            expect(this.contacts[0].used).to.be.equal(false);
            expect(this.contacts[1].used).to.be.equal(true);
        });
    });
});
