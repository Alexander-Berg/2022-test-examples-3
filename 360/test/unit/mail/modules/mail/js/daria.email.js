describe('Daria.email', function() {
    describe('#isCorp', function() {
        it('Должен вернуть true, если адрес содержит домен yandex-team', function() {
            this.sinon.stub(Daria, 'IS_CORP').value(true);
            expect(Daria.email.isCorp('rikishi@yandex-team.ru')).to.be.equal(true);
        });

        it('Должен вернуть true, если адрес содержит домен yamoney', function() {
            this.sinon.stub(Daria, 'IS_CORP').value(true);
            expect(Daria.email.isCorp('rikishi@yamoney.ru')).to.be.equal(true);
        });

        it('Должен вернуть false, если адрес не содержит домен yamoney или yandex-team', function() {
            this.sinon.stub(Daria, 'IS_CORP').value(true);
            expect(Daria.email.isCorp('rikishi@test.ru')).to.be.equal(false);
        });

        it('Должен вернуть false, если вызов выполняется не из корпа', function() {
            this.sinon.stub(Daria, 'IS_CORP').value(false);
            expect(Daria.email.isCorp('rikishi@yandex-team.ru')).to.be.equal(false);
        });
    });

    describe('#isSupport', function() {
        var checks = [
            [ 'help@support.yandex.ru', true ],
            [ 'help@support.yandex.ua', true ],
            [ 'help@support.yandex.com', true ],
            [ 'help@support.yandex.com.tr', true ],
            [ 'help@support.yandex.com.ua', true ],
            [ 'help@@support.yandex.com.ua', false ],
            [ 'help@@support.yandex.ruua', false ],
            [ 'help@support.yandex.ololo.com', false ],
            [ 'help@support.yandex.comxua', false ],
            [ '@support.yandex.com.ua', false ]
        ];

        checks.forEach(function(check) {
            it('для ' + check[0] + ' должен вернуть ' + check[1], function() {
                expect(Daria.email.isSupport(check[0])).to.be.equal(check[1]);
            });
        });
    });

    describe('#normalize', function() {
        it('Должен вернуть email без изменений, если в логине нет дефисов', function() {
            expect(Daria.email.normalize('y.i.demin@yandex-team.ru')).to.be.equal('y.i.demin@yandex-team.ru');
        });

        it('Должен заменить в логине дефисы на точки', function() {
            expect(Daria.email.normalize('y-i-demin@yandex-team.ru')).to.be.equal('y.i.demin@yandex-team.ru');
        });
    });

    describe('#getDomain', function() {
        it('должен вернуть домен', function() {
            expect(Daria.email.getDomain('y.i.demin@yandex-team.ru')).to.be.equal('yandex-team.ru');
            expect(Daria.email.getDomain('y@i@demin@yandex-team.ru')).to.be.equal('yandex-team.ru');
        });
    });

    describe('#getLogin', function() {
        it('должен вернуть логин', function() {
            expect(Daria.email.getLogin('y.i.demin@yandex-team.ru')).to.be.equal('y.i.demin');
            expect(Daria.email.getLogin('y@i@demin@yandex-team.ru')).to.be.equal('y@i@demin');
        });
    });
});
