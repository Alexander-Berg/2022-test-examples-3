require('models/contacts/contacts');

describe('Модель contacts', () => {
    describe('Запрос контактов с сервера', () => {
        beforeEach(function() {
            createFakeXHR();

            addResponseModel([
                {
                    oid: 1,
                    type: 'contacts'
                },
                {
                    status: 'WAITING',
                    type: 'contacts'
                },
                {
                    status: 'EXECUTING',
                    type: 'contacts',
                    stages: [
                        {
                            status: 'success',
                            service: 'email',
                            details: [
                                {
                                    user: {
                                        name: 'Alfa',
                                        userid: 'alfa@domain.com'
                                    }
                                }
                            ]
                        }
                    ]
                },
                {
                    status: 'DONE',
                    type: 'contacts',
                    stages: [
                        {
                            status: 'success',
                            service: 'email',
                            details: [
                                {
                                    user: {
                                        name: 'bravo',
                                        userid: 'bravo@domain.com'
                                    }
                                }
                            ]
                        }
                    ]
                }
            ]);

            this.model = ns.Model.get('contacts');
            this.model.setDataDefault();
        });

        afterEach(function() {
            deleteFakeXHR();

            delete this.model;
        });

        it('после создания в поле `contacts` должен оказаться пустой массив', function() {
            expect(this.model.get('.contacts').length).not.to.be.ok();
        });

        it('после создания статус не должен быть `loaded`', function() {
            expect(this.model.get('.status')).not.to.be('loaded');
        });

        describe('метод `load` создает операцию `getContacts`', () => {
            beforeEach(function(done) {
                const model = this.model;
                this.onChange = function() {
                    if (model.get('.status') === 'loaded') {
                        done();
                    }
                };

                this.model.on('ns-model-changed', this.onChange);

                this.model.load();
            });

            afterEach(function() {
                this.model.off('ns-model-changed', this.onChange);
            });

            it('после ее успешного завершения поле `loaded` должно стать true', function() {
                expect(this.model.get('.status')).to.be('loaded');
            });

            it('после ее успешного завершения поле `contacts` должно быть массивом с контактами', function() {
                expect(this.model.get('.contacts')).to.have.length(2);
            });
        });
    });

    describe('индекс коллекции', () => {
        beforeEach(function() {
            this.model = ns.Model.get('contacts');
            this.model.setData({
                contacts: [{
                    name: 'Alfa',
                    userid: 'alfa@domain.com',
                    service: 'email'
                }, {
                    name: 'bravo',
                    userid: 'bravo@domain.com',
                    service: 'email'
                }],
                status: 'loaded'
            });
        });
        afterEach(function() {
            delete this.model;
        });

        it('должен быть верным при наполнении пачкой данных', function() {
            expect(this.model.get('.index')).to.eql(['alfa@domain.com', 'bravo@domain.com']);
        });

        it('должен быть верным после одиночного добавления контакта', function() {
            const contact = ns.Model.get('contact', { userid: 'bravo@domain.com' });
            contact.setData({
                userid: 'test@domain.com',
                id: 'test@domain.com',
                name: 'test',
                service: 'email'
            });
            this.model.insertItem(contact);
            expect(this.model.get('.index')).to.eql(['test@domain.com', 'alfa@domain.com', 'bravo@domain.com']);
        });
    });

    describe('#filterByQuery', () => {
        const contacts = [
            { name: 'Отдел аудита', userid: '0', email: 'dep-b4c523ec37824b978efd@garris-debug-ws.yaserv.biz' },
            { name: 'Отдел авралов', userid: '1' },
            // неразрывный пробел между словами
            { name: 'Отдел\u00a0рекламы', userid: '2' },
            { name: 'Департамент аудита технологий', userid: '3' },
            { name: 'Департамент рекламных технологий', userid: '4' },
            { name: '', userid: 'foo@yandex.ru' },
            { name: 'user super', userid: 'testuser607@mail.ru' }
        ];
        const tests = [
            { query: 'Отдел', expected: getContactsNamesByIndex(0, 1, 2) },
            { query: 'Отдел ', expected: getContactsNamesByIndex(0, 1, 2) },
            { query: 'Отдел р', expected: getContactsNamesByIndex(1, 2) },
            { query: 'Отдел   ', expected: getContactsNamesByIndex(0, 1, 2) },
            { query: '   Отдел   ', expected: getContactsNamesByIndex(0, 1, 2) },
            { query: 'Отдел а', expected: getContactsNamesByIndex(0, 1, 2) },
            { query: 'Отдел ау', expected: getContactsNamesByIndex(0) },
            { query: 'Депа', expected: getContactsNamesByIndex(3, 4) },
            { query: 'Департамент аудита', expected: getContactsNamesByIndex(3) },
            { query: 'Департамент технологий', expected: getContactsNamesByIndex(3, 4) },
            { query: 'foo', expected: ['foo@yandex.ru'] },
            { query: 'testuser', expected: getContactsNamesByIndex(6) },
            { query: 'dep-b4', expected: getContactsNamesByIndex(0) }
        ];

        beforeEach(function() {
            this.contacts = ns.Model.get('contacts');
            this.contacts.setData({ contacts: contacts });
        });
        afterEach(function() {
            this.contacts = null;
        });

        tests.forEach((test) => {
            it('по запросу «' + test.query + '» должен вернуть контакты: ' + test.expected.join(', ') + ' (без учета сортировки)', function() {
                expect(
                    this.contacts
                        .filterByQuery(test.query)
                        .map((contact) => {
                            return contact.get('.name');
                        })
                        .sort()
                ).to.be.eql(test.expected.sort());
            });
        });

        /**
         * Возвращает массив имен из списка контактов
по указанный индексам в качестве аргументов
         *
         * @returns {Array}
         * @param args
         */
        function getContactsNamesByIndex(...args) {
            return args.map((index) => {
                return contacts[index].name;
            });
        }
    });
});
