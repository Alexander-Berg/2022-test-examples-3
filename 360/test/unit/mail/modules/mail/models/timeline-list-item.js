describe('Daria.mTimelineListItem', function() {
    beforeEach(function() {
        var df = '2016-07-19T00:00:00';
        var dt = '2016-07-19T23:59:59';

        this.model = ns.Model.get('timeline-list-item', {
            df: df,
            dt: dt
        }).setData({
            df: df,
            dt: dt,
            dateFrom: Jane.Date.LocalDate.parseISOTime(df),
            dateTo: Jane.Date.LocalDate.parseISOTime(dt)
        });
    });

    afterEach(function() {
        this.model.destroy();
    });

    describe('#checkDateInRange', function() {
        it('Должен вернуть true, если дата находится в диапазоне', function() {
            expect(this.model.checkDateInRange(Jane.Date.LocalDate.parseISOTime('2016-07-19T12:00:00'))).to.be.equal(true);
        });

        it('Должен вернуть false, если дата находится вне диапазона', function() {
            expect(this.model.checkDateInRange(Jane.Date.LocalDate.parseISOTime('2016-07-18T23:59:59'))).to.be.equal(false);
        });
    });

    describe('#checkDateGtRange', function() {
        it('Должен вернуть true, елси дата больше интервала', function() {
            expect(this.model.checkDateGtRange(Jane.Date.LocalDate.parseISOTime('2016-07-20T00:00:00'))).to.be.equal(true);
        });

        it('Должен вернуть false, елси дата меньше интервала', function() {
            expect(this.model.checkDateGtRange(Jane.Date.LocalDate.parseISOTime('2016-07-18T00:00:00'))).to.be.equal(false);
        });

        it('Должен вернуть false, елси дата в интервале', function() {
            expect(this.model.checkDateGtRange(Jane.Date.LocalDate.parseISOTime('2016-07-19T00:00:00'))).to.be.equal(false);
        });
    });

    describe('#checkDateLtRange', function() {
        it('Должен вернуть true, елси дата меньше интервала', function() {
            expect(this.model.checkDateLtRange(Jane.Date.LocalDate.parseISOTime('2016-07-18T00:00:00'))).to.be.equal(true);
        });

        it('Должен вернуть false, елси дата большее интервала', function() {
            expect(this.model.checkDateLtRange(Jane.Date.LocalDate.parseISOTime('2016-07-20T00:00:00'))).to.be.equal(false);
        });

        it('Должен вернуть false, елси дата в интервале', function() {
            expect(this.model.checkDateLtRange(Jane.Date.LocalDate.parseISOTime('2016-07-19T00:00:00'))).to.be.equal(false);
        });
    });

    describe('#getOffsetRange', function() {
        it('Должен вернуть 0 если дата в интервале', function() {
            expect(this.model.getOffsetRange(Jane.Date.LocalDate.parseISOTime('2016-07-19T00:00:00'))).to.be.equal(0);
        });

        it('Должен вернуть положительное смещение, если дата больше даты окончания интервала', function() {
            expect(this.model.getOffsetRange(Jane.Date.LocalDate.parseISOTime('2016-07-20T00:00:00'))).to.be.equal(1000);
        });

        it('Должен вернуть отрицательное смещение, если дата меньше даты начале интервала', function() {
            expect(this.model.getOffsetRange(Jane.Date.LocalDate.parseISOTime('2016-07-18T23:59:59'))).to.be.equal(-1000);
        });
    });
});

