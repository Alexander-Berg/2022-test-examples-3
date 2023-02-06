describe('js/common', function() {
    var exp = 33833;

    beforeEach(function() {
        Daria.Config['exp-test-ids'].push(exp.toString());

        this.group = {
            contacts: [
                { email: 'a@ya.ru', name: 'a' },
                { email: 'b@ya.ru', name: 'b' },
                { email: 'c@ya.ru', name: 'x' }
            ]
        };
        this.contact = this.group.contacts[0];
    });

    describe('#hasExperiment', function() {
        it('возвращает "false" если не был передан номер эксперимента', function() {
            expect(Daria.hasExperiment()).to.be.equal(false);
        });

        it('принимает в качестве номера эксперимента строку', function() {
            expect(Daria.hasExperiment(exp.toString())).to.be.equal(true);
        });

        it('принимает в качестве номера эксперимента число', function() {
            expect(Daria.hasExperiment(exp)).to.be.equal(true);
        });
    });

    describe('Daria.formatContacts', function() {
        it('массив объектов преобразуется в строку', function() {
            var str = Daria.formatContacts([
                {
                    email: 'doochik1@ya.ru',
                    name: 'test1'
                },
                {
                    email: 'doochik2@ya.ru',
                    name:   'test2'
                }
            ]);
            expect(str).to.be.equal('"test1" <doochik1@ya.ru>, "test2" <doochik2@ya.ru>');
        });

        it('один объект преобразуется в строку', function() {
            var str = Daria.formatContacts({
                email: 'doochik1@ya.ru',
                name: 'test1'
            });
            expect(str).to.be.equal('"test1" <doochik1@ya.ru>');
        });
    });

    describe('Daria.createAutocompleteFormatter', function() {
        beforeEach(function() {
            this.formatter = Daria.createAutocompleteFormatter(contact => `${contact.name}:${contact.email}`);
        });

        it('Должен создать функцию-форматтер', function() {
            expect(typeof this.formatter).to.be.equal('function');
        });

        it('Должен вернуть список отформатированных контактов, разделённых запятой с пробелом', function() {
            expect(this.formatter(this.group)).to.be.equal('a:a@ya.ru, b:b@ya.ru, x:c@ya.ru');
        });

        it('Должен вернуть одиночный отформатированный контакт', function() {
            expect(this.formatter(this.contact)).to.be.equal('a:a@ya.ru');
        });
    });

    describe('Daria.formatAutocompleteResult', function() {
        it('Должен вернуть список email-ов, разделённых запятой с пробелом', function() {
            expect(Daria.formatAutocompleteResult(this.group)).to.be.equal('a@ya.ru, b@ya.ru, c@ya.ru');
        });

        it('Должен вернуть одиночный email', function() {
            expect(Daria.formatAutocompleteResult(this.contact)).to.be.equal('a@ya.ru');
        });
    });
});
