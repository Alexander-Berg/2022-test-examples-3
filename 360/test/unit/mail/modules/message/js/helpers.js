describe('Daria.message', function() {

    describe('#getLinkStaffByEmail', function() {
        it('Должен вернуть пустую строку, если получение ссылки выполняется не со корп. почты', function() {
            this.sinon.stub(Daria, 'IS_CORP').value(false);
            expect(Daria.message.getLinkStaffByEmail('rikishi@yandex-team.ru')).to.be.equal('');
        });

        it('Должен вернуть пустую строку, если email не принадлежит корп. почте', function() {
            this.sinon.stub(Daria, 'IS_CORP').value(true);
            this.sinon.stub(Daria.email, 'isCorp')
                .withArgs('rikishi@yandex-team.ru')
                .returns(false);

            expect(Daria.message.getLinkStaffByEmail('rikishi@yandex-team.ru')).to.be.equal('');
        });

        it('Должен вернуть строку, содержащую ссылку на стаф', function() {
            this.sinon.stub(Daria, 'IS_CORP').value(true);
            this.sinon.stub(Daria.email, 'isCorp')
                .withArgs('rikishi@yandex-team.ru')
                .returns(true);

            expect(Daria.message.getLinkStaffByEmail('rikishi@yandex-team.ru')).to.be.a('string');
        });
    });
});

