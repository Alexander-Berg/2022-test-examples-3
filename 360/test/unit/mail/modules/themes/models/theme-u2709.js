describe('Daria.mThemeU2709', function() {

    beforeEach(function() {
        this.model = ns.Model.get('theme-u2709');
    });

    describe('#getScope', function() {
        it('Должен вернуть scope по умолчанию, если дата меньше 20 декабря', function() {
            this.sinon.useFakeTimers((new Date(2015, 11, 19)).getTime());
            expect(this.model.getScope()).to.be.eql(this.model.DEFAULT_SCOPE);
        });

        it('Должен вернуть scope по умолчанию, если дата больше 10 января', function() {
            this.sinon.useFakeTimers((new Date(2016, 1, 1)).getTime());
            expect(this.model.getScope()).to.be.eql(this.model.DEFAULT_SCOPE);
        });

        it('Должен вернуть новогодний scope, если дата больше 19 декабря, но меньше 11 января', function() {
            this.sinon.useFakeTimers((new Date(2016, 0, 1)).getTime());
            expect(this.model.getScope()).to.be.eql('ny2016');
        });
    });
});
