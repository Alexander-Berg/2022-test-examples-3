describe('Daria.ContactBubble', function() {
    describe('Конструктор', function() {
        it('должно использовать innerText для получения данных, если не переданы data атрибуты', function() {
            var node = document.createElement('span');
            node.innerText = 'test@ya.ru';
            var bubble = new Daria.ContactBubble(node);

            expect(bubble.get('email')).to.be.eql([ 'test@ya.ru' ]);
            expect(bubble.get('value')).to.be.eql([ '<test@ya.ru>' ]);
        });

        it('должно заполнить поле name, если передан формат контакта', function() {
            var node = document.createElement('span');
            node.innerText = '"test" <test@ya.ru>';
            var bubble = new Daria.ContactBubble(node);

            expect(bubble.get('email')).to.be.eql([ 'test@ya.ru' ]);
            expect(bubble.get('value')).to.be.eql([ '"test" <test@ya.ru>' ]);
            expect(bubble.get('name')).to.be.eql('test');
        });

        it('если передан data-yabble-value, то данные выбираются из data-yabble-* атрибутов', function() {
            var node = document.createElement('span');
            node.setAttribute('data-yabble-value', '"test123" <test123@ya.ru>');
            node.setAttribute('data-yabble-name', 'test123');
            node.setAttribute('data-yabble-email', 'test123@ya.ru');
            node.innerText = '"test" <test@ya.ru>';

            var bubble = new Daria.ContactBubble(node);

            expect(bubble.get('email')).to.be.eql([ 'test123@ya.ru' ]);
            expect(bubble.get('value')).to.be.eql([ '"test123" <test123@ya.ru>' ]);
            expect(bubble.get('name')).to.be.eql('test123');
        });

        it('если передан data-yabble-value, но не переданы другие data атрибуты, то бабл пустой', function() {
            var node = document.createElement('span');
            node.setAttribute('data-yabble-value', '"test123" <test123@ya.ru>');
            node.innerText = '"test" <test@ya.ru>';

            var bubble = new Daria.ContactBubble(node);

            expect(bubble.get('email')).to.be.eql([]);
            expect(bubble.get('value')).to.be.eql([]);
            expect(bubble.get('name')).to.be.eql('');
        });

        it('если нет cid, но передан email, то выполнит поиск контакта по email и заполнит cid если найдено',
            function() {
                this.sinon.stub(ns.Model.get('abook-contacts'), 'getContactDataByEmail')
                    .withArgs('test@ya.ru').returns({ cid: '123' });

                var node = document.createElement('span');
                node.innerText = '"test" <test@ya.ru>';

                var bubble = new Daria.ContactBubble(node);

                expect(bubble.get('cid')).to.be.eql('123');
            }
        );
    });

    describe('#toString', function() {
        it('должен вернуть значение value', function() {
            var node = document.createElement('span');
            node.innerText = '"test" <test@ya.ru>';

            var bubble = new Daria.ContactBubble(node);

            expect(bubble.toString()).to.be.eql('"test" <test@ya.ru>');
            expect(bubble.get('value')).to.be.eql([ '"test" <test@ya.ru>' ]);
        });
    });

    describe('#setEmail', function() {
        it('должен изменить набор email с сохранением name', function() {
            var node = document.createElement('span');
            node.innerText = '"test" <test@ya.ru>';

            var bubble = new Daria.ContactBubble(node);

            expect(bubble.get('name')).to.be.eql('test');
            expect(bubble.get('email')).to.be.eql([ 'test@ya.ru' ]);
            expect(bubble.get('value')).to.be.eql([ '"test" <test@ya.ru>' ]);

            bubble.setEmail('test123@ya.ru');

            expect(bubble.get('name')).to.be.eql('test');
            expect(bubble.get('email')).to.be.eql([ 'test123@ya.ru' ]);
            expect(bubble.get('value')).to.be.eql([ '"test" <test123@ya.ru>' ]);
        });
    });

    describe('#appendEmail', function() {
        it('должен изменить набор email и value', function() {
            var node = document.createElement('span');
            node.innerText = '"test" <test@ya.ru>';

            var bubble = new Daria.ContactBubble(node);

            expect(bubble.get('name')).to.be.eql('test');
            expect(bubble.get('email')).to.be.eql([ 'test@ya.ru' ]);
            expect(bubble.get('value')).to.be.eql([ '"test" <test@ya.ru>' ]);

            bubble.appendEmail('test123@ya.ru');

            expect(bubble.get('name')).to.be.eql('test');
            expect(bubble.get('email')).to.be.eql([ 'test@ya.ru', 'test123@ya.ru' ]);
            expect(bubble.get('value')).to.be.eql([ '"test" <test@ya.ru>', '<test123@ya.ru>' ]);
        });

        it('должен должен добавить имя к новому контакту, если передан второй аргумент', function() {
            var node = document.createElement('span');
            node.innerText = '"test" <test@ya.ru>';

            var bubble = new Daria.ContactBubble(node);

            expect(bubble.get('name')).to.be.eql('test');
            expect(bubble.get('email')).to.be.eql([ 'test@ya.ru' ]);
            expect(bubble.get('value')).to.be.eql([ '"test" <test@ya.ru>' ]);

            bubble.appendEmail('test123@ya.ru', 'test123');

            expect(bubble.get('name')).to.be.eql('test');
            expect(bubble.get('email')).to.be.eql([ 'test@ya.ru', 'test123@ya.ru' ]);
            expect(bubble.get('value')).to.be.eql([ '"test" <test@ya.ru>', '"test123" <test123@ya.ru>' ]);
        });
    });

    describe('#removeEmail', function() {
        it('должен удалить email', function() {
            var node = document.createElement('span');
            node.innerText = '"test" <test@ya.ru>';

            var bubble = new Daria.ContactBubble(node);

            expect(bubble.get('name')).to.be.eql('test');
            expect(bubble.get('email')).to.be.eql([ 'test@ya.ru' ]);
            expect(bubble.get('value')).to.be.eql([ '"test" <test@ya.ru>' ]);

            bubble.removeEmail('test@ya.ru');

            expect(bubble.get('name')).to.be.eql('test');
            expect(bubble.get('email')).to.be.eql([]);
            expect(bubble.get('value')).to.be.eql([]);
        });
    });

    describe('#tokenizer', function() {
        [
            [ ',,,,,a,,,b,;;;c;;;;,d,,,', [ 'a', 'b', 'c', 'd' ] ],
            [ 'asd@ya.ru', [ 'asd@ya.ru' ] ],
            [ 'asd@ya.ru, asdqwe', [ 'asd@ya.ru', 'asdqwe' ] ],
            [ 'asd@ya.ru, , asdqwe, qwe@ya.ru', [ 'asd@ya.ru', 'asdqwe', 'qwe@ya.ru' ] ],
            [ 'test <test@ya.ru>', [ 'test <test@ya.ru>' ] ],
            [ 'asd <asd@ya.ru>, ,,,, qwe <qwe@ya.ru>', [ 'asd <asd@ya.ru>', 'qwe <qwe@ya.ru>' ] ],
            [ 'test1, test2 <test@ya.ru>', [ 'test1, test2 <test@ya.ru>' ] ],
            [ 'test1@ya.ru, test2 <test@ya.ru>', [ 'test1@ya.ru', 'test2 <test@ya.ru>' ] ],
            [ 'test0, test1@ya.ru, test2 <test@ya.ru>', [ 'test0, test1@ya.ru, test2 <test@ya.ru>' ] ],
            [ '"test@ya.ru" <test@ya.ru>', [ '"test@ya.ru" <test@ya.ru>' ] ],
            [ 'test@ya.ru <test@ya.ru>', [ 'test@ya.ru', '<test@ya.ru>' ] ],
            [ '"me@me.me me@me.me" <me@me.me>', [ '"me@me.me me@me.me" <me@me.me>' ] ],
            [ 'test@ya.ru test@ya.ru', [ 'test@ya.ru', 'test@ya.ru' ] ],
            [ 'test test@ya.ru', [ 'test', 'test@ya.ru' ] ],
            [ 'test2 test test@ya.ru', [ 'test2', 'test', 'test@ya.ru' ] ],
            [ '"test2@yandex.ru" test@ya.ru', [ '"test2@yandex.ru"', 'test@ya.ru' ] ],
            [ '"test2 test3" test3', [ '"test2 test3"', 'test3' ] ],
            [ '"me@me.me,me@me.me" me@me.me', [ '"me@me.me,me@me.me"', 'me@me.me' ] ]
        ].forEach(function(test) {
            it('должен выделить контакт "' + test[0] + '"', function() {
                var tokens = Daria.ContactBubble.tokenizer(test[0]);
                expect(tokens).to.be.eql(test[1]);
            });
        });
    });

    describe('#getPhone', function() {
        beforeEach(function() {
            this.bubble = new Daria.ContactBubble($('<div data-value="test"></div>')[0]);
            this.sinon.stub(this.bubble, 'hasInvalidEmail').returns(false);
            this.sinon.stub(this.bubble, 'get').withArgs('email').returns([ 1 ]);

            this.mAbookContact = ns.Model.get('abook-contact');
            this.mAbookMultipleContacts = ns.Model.get('abook-contacts');

            this.sinon.stub(ns.Model, 'get')
                .withArgs('abook-contacts').returns(this.mAbookMultipleContacts)
                .withArgs('abook-contact', this.sinon.match.any).returns(this.mAbookContact);
        });

        describe('Поле phone', function() {
            it('Если есть значение в поле `phone`, то должен отдать эти данные', function() {
                this.bubble.get.withArgs('phone').returns('+79117777777');
                expect(this.bubble.getPhone()).to.be.eql({
                    status: Daria.ContactBubble.PHONE.FOUND, value: '+79117777777'
                });
            });
        });

        describe('Поиск контакта', function() {
            beforeEach(function() {
                this.sinon.stub(this.bubble, 'isContact').returns(true);
            });

            it('Если контакт и нет валидной модели контакта по cid, то должен отдать `not_found`', function() {
                expect(this.bubble.getPhone()).to.be.eql({ status: Daria.ContactBubble.PHONE.NOT_FOUND, value: null });
            });

            it('Если контакт и есть валидная модель контакта по cid, то должен отдать телефон', function() {
                this.sinon.stub(this.mAbookContact, 'isValid').returns(true);
                this.sinon.stub(this.mAbookContact, 'getPhone').returns('+79117777777');

                expect(this.bubble.getPhone()).to.be.eql({
                    status: Daria.ContactBubble.PHONE.FOUND, value: '+79117777777'
                });
            });
        });

        describe('Поиск в группе', function() {
            beforeEach(function() {
                this.sinon.stub(this.bubble, 'isGroup').returns(true);
            });

            it('Если группа и есть валидная модель контакта по cid, то должен отдать телефон', function() {
                this.sinon.stub(this.bubble, 'getHeadEmail').returns('');
                this.sinon.stub(this.mAbookMultipleContacts, 'getContactDataByEmail').returns({ cid: 'test' });
                this.sinon.stub(this.mAbookContact, 'isValid').returns(true);
                this.sinon.stub(this.mAbookContact, 'getPhone').returns('+79117777777');

                expect(this.bubble.getPhone()).to.be.eql({
                    status: Daria.ContactBubble.PHONE.FOUND, value: '+79117777777'
                });
            });

            it('Если группа и нет валидных моделей группы и контакта, то должен отдать `should_request`', function() {
                this.sinon.stub(this.mAbookMultipleContacts, 'getContactDataByEmail').returns({ cid: 'test' });
                expect(this.bubble.getPhone()).to.be.eql({
                    status: Daria.ContactBubble.PHONE.SHOULD_REQUEST, value: null
                });
            });
        });

        describe('Поведение, если у контакта нет телефона', function() {
            it('Если не валидный ябл, то должен отдать `not_found`', function() {
                this.sinon.stub(this.bubble, 'isGroup').returns(false);
                this.sinon.stub(this.bubble, 'isContact').returns(false);

                expect(this.bubble.getPhone()).to.be.eql({ status: Daria.ContactBubble.PHONE.NOT_FOUND, value: null });

                this.bubble.isGroup.restore();
                this.bubble.isContact.restore();

                this.sinon.stub(this.bubble, 'isContact').returns(true);
                this.bubble.hasInvalidEmail.returns(true);

                expect(this.bubble.getPhone()).to.be.eql({ status: Daria.ContactBubble.PHONE.NOT_FOUND, value: null });

                this.bubble.hasInvalidEmail.restore();
                this.bubble.get.withArgs('email').returns([ 1, 2 ]);

                expect(this.bubble.getPhone()).to.be.eql({ status: Daria.ContactBubble.PHONE.NOT_FOUND, value: null });
            });

            it('Если после всех проверок телефон не найден, то должен вернуть `not_found`', function() {
                this.sinon.stub(this.bubble, 'isContact').returns(true);
                this.sinon.stub(this.mAbookContact, 'isValid').returns(true);
                this.sinon.stub(this.mAbookContact, 'getPhone').returns('');

                expect(this.bubble.getPhone()).to.be.eql({ status: Daria.ContactBubble.PHONE.NOT_FOUND, value: null });
            });
        });
    });
});

