describe('Daria.timify', function() {
    it('Должен уметь переводить секунды в миллисекунды', function() {
        expect(Daria.timify({seconds: 60})).to.be.equal(60000);
    });

    it('Должен уметь переводить минуты в миллисекунды', function() {
        expect(Daria.timify({minutes: 10})).to.be.equal(600000);
    });

    it('Должен уметь переводить часы в миллисекунды', function() {
        expect(Daria.timify({hours: 1.6})).to.be.equal(5760000);
    });

    it('Должен уметь переводить дни в миллисекунды', function() {
        expect(Daria.timify({days: 5})).to.be.equal(432000000);
    });

    it('Должен уметь переводить месяцы в миллисекунды', function() {
        expect(Daria.timify({months: 1.5})).to.be.equal(3888000000);
    });

    it('Должен уметь переводить годы в миллисекунды', function() {
        expect(Daria.timify({years: 2})).to.be.equal(62208000000);
    });

    it('Должен уметь переводить несколько величин в миллисекунды', function() {
        var timifyParams = {years: 1, months: 2, days: 3, hours: 3, minutes: 25, seconds: 14};
        expect(Daria.timify(timifyParams)).to.be.equal(36559514000);
    });

    it('Должен уметь переводить несколько величин в таймстамп', function() {
        var timifyParams = {years: 1, months: 2, days: 3, hours: 3, minutes: 25, seconds: 14};
        expect(Daria.timify(timifyParams, 'timestamp')).to.be.equal(36559514);
    });

    it('Должен уметь переводить несколько величин в минуты', function() {
        var timifyParams = {years: 1, months: 2, days: 3, hours: 3, minutes: 25, seconds: 14};
        expect(Math.floor(Daria.timify(timifyParams, 'minutes'))).to.be.equal(609325);
    });

    it('Должен уметь переводить месяцы в секунды', function() {
        expect(Daria.timify({months: 2}, 'seconds')).to.be.equal(5184000);
    });

    it('Должен уметь переводить месяцы в минуты', function() {
        expect(Daria.timify({months: 2}, 'minutes')).to.be.equal(86400);
    });

    it('Должен уметь переводить месяцы в часы', function() {
        expect(Daria.timify({months: 2}, 'hours')).to.be.equal(1440);
    });

    it('Должен уметь переводить месяцы в дни', function() {
        expect(Daria.timify({months: 2}, 'days')).to.be.equal(60);
    });

    it('Должен уметь переводить месяцы в годы', function() {
        expect(Daria.timify({months: 48}, 'years')).to.be.equal(4);
    });
});
