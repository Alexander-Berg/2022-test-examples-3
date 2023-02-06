describe('Daria.mAccountInformation', function() {

    beforeEach(function() {
        /** @type Daria.mAccountInformation */
        this.h = ns.Model.get("account-information");
        this.mockData = getModelMock(this.h);
        this.h.setData(this.mockData);

        this.defaultEmail = 'whatever@wherever.com';
        this.sinon.stub(ns.Model.get('settings'), 'getSetting').withArgs('default_email').returns(this.defaultEmail);
        this.sinon.stub(Daria.Config, 'product').value('RUS');
    });

    afterEach(function() {
        this.h.destroy();
    });

    describe('onsetcache', function() {
        it('после вызова есть Daria.ckey', function() {
            expect(Daria.ckey).to.be.equal(this.mockData.ckey);
        });

        it('после вызова есть таймер обновления', function() {
            expect(this.h).to.have.property('_timer');
        });

        it('вызов стирает this.emails', function() {
            var h = this.h;
            h.getEmails();
            h.preprocessData({});
            expect(h).to.not.have.property('_emails');
        });
    });

    describe('getEmails', function() {
        it('возвращает массив emailов', function() {
            expect(this.h.getEmails()).to.eql([
                'doochik@yandex.ru',
                'doochik@yandex.com',
                'my@ya.ru',
                'my2@ya.ru',
                'my.dot@ya.ru',
                'my-dot@ya.ru',
                'my-dash@ya.ru',
                'my.dash@ya.ru',
                'my.dot-dash@ya.ru',
                'my-dot-dash@ya.ru',
                'my.dot.dash@ya.ru',
                'myCamelCase@ya.ru'
            ]);
        });

        it('кеширует результат', function() {
            var h = this.h;
            h.getEmails();
            expect(h._emails).to.eql([
                'doochik@yandex.ru',
                'doochik@yandex.com',
                'my@ya.ru',
                'my2@ya.ru',
                'my.dot@ya.ru',
                'my-dot@ya.ru',
                'my-dash@ya.ru',
                'my.dash@ya.ru',
                'my.dot-dash@ya.ru',
                'my-dot-dash@ya.ru',
                'my.dot.dash@ya.ru',
                'myCamelCase@ya.ru'
            ]);
        });
    });

    describe('getEmails (PDD)', function() {
        beforeEach(function() {
            var h = this.h;
            h.setData(getModelMockByName('account-information', 'pdd'));

            this.sinon.stub(Daria, 'Config').value({pddDomain: 'technocat.ru'});
        });

        it('в PDD не должно быть замены точек на минусы в логине адресов', function() {
            expect(this.h.getEmails()).to.eql(['spam.zzap@technocat.ru', 'hello-test@technocat.ru']);
        });
    });

    describe('getData', function() {
        it('должен равняться исхоным данным', function() {
            expect(this.h.getData()).to.eql(this.mockData);
        });
    });

    describe('getDatKey', function() {
        it('должен вернуть значение, если есть', function() {
            expect(this.h.getDataKey('uid')).to.eql(this.mockData['uid']);
        });
        it('должен вернуть undefined, если нет', function() {
            expect(this.h.getDataKey('uid1')).to.eql(undefined);
        });
    });

    describe('sortEmails', function() {
        beforeEach(function() {
            this.sinon.stub(Daria.Config, 'product').value('RUS');

            this.emailsToSort = _.shuffle([
                this.defaultEmail, 'vasily@ya.ru', 'vasily@yandex.ru',
                'vasily@yandex.by', 'vasily@pupkine.ru', 'thereis@nospoon.kz',
                'login@yandex.com.tr', 'login@yandex.com'
            ]);
        });

        it('должен выводить дефолтный емейл в самом начале списка', function() {
            expect(this.h.sortEmails(this.emailsToSort).shift()).to.eql(this.defaultEmail);
        });

        describe('После дефолтного емейла', function() {
            describe('Русский продукт', function() {
                beforeEach(function() {
                    this.sinon.stub(Daria.Config, 'product').value('RUS');
                });

                it('должен выводить емейл @ya.ru', function() {
                    expect(this.h.sortEmails(this.emailsToSort)[1]).to.eql('vasily@ya.ru');
                });

                it('должен выводить @yandex.ru раньше других, кроме @ya.ru', function() {
                    expect(this.h.sortEmails(this.emailsToSort)[2]).to.eql('vasily@yandex.ru');
                });
            });

            describe('Международный продукт', function() {
                beforeEach(function() {
                    this.sinon.stub(Daria.Config, 'product').value('INT');
                });

                it('должен выводить емейл @yandex.com', function() {
                    expect(this.h.sortEmails(this.emailsToSort)[1]).to.eql('login@yandex.com');
                });
            });

            describe('Турецкий продукт', function() {
                beforeEach(function() {
                    this.sinon.stub(Daria.Config, 'product').value('TUR');
                });

                it('должен выводить емейл @yandex.com.tr', function() {
                    expect(this.h.sortEmails(this.emailsToSort)[1]).to.eql('login@yandex.com.tr');
                });
            });
        });


        it('должен сортировать стандартные алиасы почты в конец списка', function() {
            expect(this.h.sortEmails(['petya@yandex.by', 'vasily@pupkine.ru', 'vasily@yandex.ru']).pop())
                .to.eql('petya@yandex.by');
        });

        it('должен сортировать емейлы как обычные строки, если всё остальное выполнено', function() {
            expect(this.h.sortEmails([
                //@ya.ru group
                'boris@ya.ru', 'anatoly@ya.ru',

                //@yandex.ru group
                'zyalt@yandex.ru', 'vasily@yandex.ru',

                //Other emails group
                'anya@what.ru', 'kolya@so.ru', 'abay@ever.ru',

                //Mail aliases group
                'yarik@yandex.ua', 'petya@yandex.by', 'roma@yandex.kz'
            ])).to.eql([
                //@ya.ru group
                'anatoly@ya.ru', 'boris@ya.ru',

                //@yandex.ru group
                'vasily@yandex.ru', 'zyalt@yandex.ru',

                //Other emails group
                'abay@ever.ru', 'anya@what.ru', 'kolya@so.ru',

                //Mail aliases group
                'petya@yandex.by', 'roma@yandex.kz', 'yarik@yandex.ua'
                ]);
        });
    });

    describe('#getTimezoneInHours', function() {
        it('Должен вернуть значение таймзоны пользователя в настройках в часах', function() {
            expect(ns.Model.get("account-information").getTimezoneInHours()).to.be.equal(4);
        });
    });

    describe('#getAllUserEmails', function() {
        beforeEach(function() {
            this.sinon.stub(ns.Model.get('settings'), 'getValidatedEmails').returns(['my@ya.ru']);
            this.sinon.stub(Daria.Config, 'product').value('TUR');
        });

        it('Должен возвращать все пользовательские имэйлы', function() {
            expect(this.h.getAllUserEmails()).to.be.eql([
                'doochik@yandex.com',
                'doochik@yandex.ru',
                'my-dash@ya.ru',
                'my-dot-dash@ya.ru',
                'my-dot@ya.ru',
                'my.dash@ya.ru',
                'my.dot-dash@ya.ru',
                'my.dot.dash@ya.ru',
                'my.dot@ya.ru',
                'my2@ya.ru',
                'my@ya.ru',
                'myCamelCase@ya.ru'
            ]);
        });
    });

    describe('#getMA', function() {

        it('должен вернуть массив МА', function() {
            var ma = [
                {uid: '1', suid: '2', mdb: 'mdb'}
            ];
            this.h.set('.users', ma);

            expect(this.h.getMA()).to.be.equal(ma);
        });

    });

    describe('#hasMA', function() {

        it('должен вернуть true, если есть аккаунты в  МА', function() {
            var ma = [
                {uid: '1', suid: '2', mdb: 'mdb'}
            ];
            this.h.set('.users', ma);

            expect(this.h.hasMA()).to.be.equal(true);
        });

        it('должен вернуть false, если нет аккаунтов в  МА', function() {
            this.h.set('.users', []);

            expect(this.h.hasMA()).to.be.equal(false);
        });

    });

    describe('#getFromDefaultEmail', function() {
        describe('Если списки from-емейлов есть ->', function() {
            beforeEach(function() {
                this.sinon.stub(this.h, 'getFromEmails').returns([
                    'test1@ya.ru',
                    'test1@yandex.ru',
                    '+7123456789@ya.ru',
                    '+7123456789@yandex.ru',
                    '+7123456789@ya.kz',
                    '+7123456789@yandex.kz',
                    'test1@yandex.com',
                    'test1@yandex.com.tr'
                ]);
                this.sinon.stub(Jane, 'Config').value({'yandex-domain': 'yandex.ru'});
            });

            it('должен вернуть сформированный емейл из конфигов и account-information', function() {
                this.h.setData(getModelMockByName('account-information','yandex-account-right'));
                expect(this.h.getFromDefaultEmail()).to.be.eql('test1@yandex.ru');

            });

            it('должен вернуть первый емейл в списке from-емейлов если такого сформированного нами емейла', function() {
                this.h.setData(getModelMockByName('account-information','yandex-account-wrong'));
                expect(this.h.getFromDefaultEmail()).to.be.eql('test1@ya.ru');
            });
        });

        describe('список from-емейлов пустой или не существует ->', function() {
            beforeEach(function() {
                this.h.setData(getModelMockByName('account-information','yandex-account-wrong'));
                this.sinon.stub(Jane, 'Config').value({'yandex-domain': 'yandex.ru'});
            });
            it('должен вернуть пустоту, если не сформированного емейла в списке from-емейлов нет, ни самого массива from-емейлов', function() {
                this.sinon.stub(this.h, 'getFromEmails').returns([]);
                expect(this.h.getFromDefaultEmail()).to.be.eql('');
            });
        });
    });
});
