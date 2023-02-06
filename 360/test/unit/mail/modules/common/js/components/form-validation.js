describe('Jane.FormValidation', function() {
    describe('#checkEmail', function() {
        it('должен принять адрес с поддерживаемыми символами', function() {
            expect(Jane.FormValidation.checkEmail('derẞßÜü@ışığagöçüm.com')).to.be.ok;
        });

        it('должен принять адрес с символами европейских языков', function() {
            expect(Jane.FormValidation.checkEmail('jøran@blåbærsyltetøy.gulbrandsen.priv.no')).to.be.ok;
        });

        it('должен принять адрес с диакритическими знаками в домене 1 уровня', function() {
            expect(Jane.FormValidation.checkEmail('derẞßÜvü@ışığagöçüm.ẞßÜü')).to.be.ok;
        });

        it('должен не принять адрес без @', function() {
            expect(Jane.FormValidation.checkEmail('mailyandex.m')).to.not.be.ok;
        });

        it('должен не принять адрес без точки', function() {
            expect(Jane.FormValidation.checkEmail('mail@localhost')).to.not.be.ok;
        });

        it('должен не принять адрес с доменом 1 уровня, короче двух символов', function() {
            expect(Jane.FormValidation.checkEmail('mail@yandex.m')).to.not.be.ok;
        });
    });

    describe('#splitPhones', function() {
        beforeEach(function() {
            this.phone1 = '+ 7 (911) 748-41-15';
            this.phone2 = '123123123 123 12 3 12 3 12 3';
            this.phone3 = '456%(123)-71238&*012-';
        });

        it('Не должен разделять строку, если нет , и ;', function() {
            expect(Jane.FormValidation.splitPhones(this.phone1)).to.eql([ this.phone1 ]);
            expect(Jane.FormValidation.splitPhones(this.phone2)).to.eql([ this.phone2 ]);
            expect(Jane.FormValidation.splitPhones(this.phone3)).to.eql([ this.phone3 ]);
        });

        it('Должен разделять строку, если есть ,', function() {
            var phones = this.phone1 + ',' + this.phone2 + ',' + this.phone3;
            expect(Jane.FormValidation.splitPhones(phones)).to.eql([ this.phone1, this.phone2, this.phone3 ]);
        });

        it('Должен разделять строку, если есть ;', function() {
            var phones = this.phone1 + ';' + this.phone2 + ';' + this.phone3;
            expect(Jane.FormValidation.splitPhones(phones)).to.eql([ this.phone1, this.phone2, this.phone3 ]);
        });

        it('Должен обрезать пробелы в начале и конце строки при разделении телефонов', function() {
            var phones = this.phone1 + '; ' + this.phone2 + ', ' + this.phone3;
            expect(Jane.FormValidation.splitPhones(phones)).to.eql([ this.phone1, this.phone2, this.phone3 ]);
        });

        it('Не должен парсить пустую строку, как телефон', function() {
            var phones = this.phone1 + ',' + ' ,' + this.phone2 + ',';
            expect(Jane.FormValidation.splitPhones(phones)).to.eql([ this.phone1, this.phone2 ]);
        });
    });

    describe('#obj2contact', function() {
        it('Если нет email и name, то должен возвращать пустую строку', function() {
            expect(Jane.FormValidation.obj2contact({})).to.equal('');
        });

        it('Если есть только email, должен возвращать строку формата "<email>"', function() {
            expect(Jane.FormValidation.obj2contact({ email: 'email' })).to.equal('<email>');
        });

        it('Если есть email и name, то должен возвращать строку в формате \'"name" <email>\'', function() {
            expect(Jane.FormValidation.obj2contact({ email: 'email', name: 'name' })).to.equal('"name" <email>');
        });
    });

    describe('#splitContacts', function() {
        var expectedContacts = [
            { email: 's.rez@gmail.com', name: 'Step Rez' },
            { email: 'yarik.181@yandex.ru', name: 'yarik.181@yandex.ru' },
            { email: 'step-vr@mail.ru' },
            { email: 'step@yandex-team.ru', name: '' },
            { email: 'test4@yandex.ru', name: 'Test4' }
        ];

        it('должен распарсить строку с разделителем \',\' между письмами и вернуть 5 контактов', function() {
            var source = '"Step Rez" <s.rez@gmail.com>, "yarik.181@yandex.ru" <yarik.181@yandex.ru>,' +
                ' step-vr@mail.ru, <step@yandex-team.ru>, Test4 <test4@yandex.ru>';
            var contacts = Jane.FormValidation.splitContacts(source);

            expect(contacts).to.eql(expectedContacts);
        });

        it('должен распарсить строку с разделителем \',\' в конце строки и вернуть 5 контактов', function() {
            var source = '"Step Rez" <s.rez@gmail.com>, "yarik.181@yandex.ru" <yarik.181@yandex.ru>,' +
                ' step-vr@mail.ru, <step@yandex-team.ru>, Test4 <test4@yandex.ru>, ';
            var contacts = Jane.FormValidation.splitContacts(source);
            expect(contacts).to.eql(expectedContacts);
        });

        it('должен распарсить строку с разделителем \';\' между письмами и вернуть 5 контактов', function() {
            var source = '"Step Rez" <s.rez@gmail.com>; "yarik.181@yandex.ru" <yarik.181@yandex.ru>;' +
                ' step-vr@mail.ru; <step@yandex-team.ru>; Test4 <test4@yandex.ru>';
            var contacts = Jane.FormValidation.splitContacts(source);
            expect(contacts).to.eql(expectedContacts);
        });

        it('должен распарсить строку с разделителем \';\' в конце строки и вернуть 5 контактов', function() {
            var source = '"Step Rez" <s.rez@gmail.com>; "yarik.181@yandex.ru" <yarik.181@yandex.ru>;' +
                ' step-vr@mail.ru; <step@yandex-team.ru>; Test4 <test4@yandex.ru>;';
            var contacts = Jane.FormValidation.splitContacts(source);
            expect(contacts).to.eql(expectedContacts);
        });

        it('Разделители кроме < не учитываются между угловыми скобками "<>"', function() {
            var source = 'Test4 <tes,t4@yande"x.ru>; <test">, <123;456@yandex.ru>';
            var contacts = Jane.FormValidation.splitContacts(source);

            expect(contacts).to.eql([
                {
                    name: 'Test4',
                    email: 'tes,t4@yande"x.ru'
                },
                {
                    name: '',
                    email: 'test"'
                },
                {
                    name: '',
                    email: '123;456@yandex.ru'
                }
            ]);
        });

        it('Должен правильно распарсить паттерн "<.*<.*>"', function() {
            var source = 'TEST <test <tes,t4@yande"x.ru>';
            var contacts = Jane.FormValidation.splitContacts(source);

            expect(contacts).to.eql([ { name: 'TEST <test', email: 'tes,t4@yande"x.ru' } ]);
        });
    });
});
