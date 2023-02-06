const dateHelper = require('helpers/dateHelper');
const { expect } = require('chai');
const moment = require('moment');

describe('Date helper', () => {
    describe('`isValidInterval`', () => {
        it('should return `true` for valid interval', () => {
            const actual = dateHelper.isValidInterval('10m');

            expect(actual).to.be.true;
        });

        it('should return `false` for invalid interval', () => {
            const actual = dateHelper.isValidInterval('abc');

            expect(actual).to.be.false;
        });
    });

    describe('`subtractFromDate`', () => {
        const beginDate = new Date(2016, 5, 6, 19, 53, 0);

        it('should return `beginDate` when string is invalid', () => {
            const actual = dateHelper.subtractFromDate(beginDate, 'abc');

            expect(actual).to.deep.equal(beginDate);
        });

        it('should parse date from day', () => {
            const actual = dateHelper.subtractFromDate(beginDate, '2d');

            expect(actual).to.deep.equal(new Date(2016, 5, 4, 0, 0, 0));
        });

        it('should parse date from week', () => {
            const actual = dateHelper.subtractFromDate(beginDate, '3w');

            expect(actual).to.deep.equal(new Date(2016, 4, 16, 0, 0, 0));
        });

        it('should parse date from month', () => {
            const actual = dateHelper.subtractFromDate(beginDate, '4m');

            expect(actual).to.deep.equal(new Date(2016, 1, 6, 0, 0, 0));
        });

        it('should parse date from year', () => {
            const actual = dateHelper.subtractFromDate(beginDate, '5y');

            expect(actual).to.deep.equal(new Date(2011, 5, 6, 0, 0, 0));
        });
    });

    describe('`addToDate`', () => {
        const beginDate = new Date(2016, 5, 6, 19, 53, 0);

        it('should return `beginDate` when string is invalid', () => {
            const actual = dateHelper.addToDate(beginDate, 'abc');

            expect(actual).to.deep.equal(beginDate);
        });

        it('should parse date from day', () => {
            const actual = dateHelper.addToDate(beginDate, '2d');

            expect(actual).to.deep.equal(new Date(2016, 5, 8, 0, 0, 0));
        });

        it('should parse date from week', () => {
            const actual = dateHelper.addToDate(beginDate, '3w');

            expect(actual).to.deep.equal(new Date(2016, 5, 27, 0, 0, 0));
        });

        it('should parse date from month', () => {
            const actual = dateHelper.addToDate(beginDate, '4m');

            expect(actual).to.deep.equal(new Date(2016, 9, 6, 0, 0, 0));
        });

        it('should parse date from year', () => {
            const actual = dateHelper.addToDate(beginDate, '5y');

            expect(actual).to.deep.equal(new Date(2021, 5, 6, 0, 0, 0));
        });
    });

    describe('`getMeasure`', () => {
        it('should return correct time unit', () => {
            const actual = dateHelper.getMeasure('7m');

            expect(actual).to.equal('months');
        });
    });

    describe('`getPrevMonthInterval`', () => {
        it('should return correct from and to for middle month', () => {
            const date = new Date(2019, 6, 17, 7, 8, 9);
            const actual = dateHelper.getPrevMonthInterval(date);
            const dateAtPrevMonth = new Date(2019, 5, 1);

            expect(actual).to.deep.equal({
                from: moment(dateAtPrevMonth).startOf('month').toDate(),
                to: moment(dateAtPrevMonth).endOf('month').toDate()
            });
        });

        it('should return December when date is January', () => {
            const date = new Date(2019, 0, 17, 7, 8, 9);
            const actual = dateHelper.getPrevMonthInterval(date);
            const dateAtPrevMonth = new Date(2018, 11, 1);

            expect(actual).to.deep.equal({
                from: moment(dateAtPrevMonth).startOf('month').toDate(),
                to: moment(dateAtPrevMonth).endOf('month').toDate()
            });
        });
    });
});
