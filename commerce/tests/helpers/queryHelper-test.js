const { expect } = require('chai');

const queryHelper = require('helpers/queryHelper');
const catchError = require('tests/helpers/catchError').func;

describe('`queryHelper`', () => {
    describe('`getInterval`', () => {
        it('should return `from` and `to` from query', () => {
            const now = '2018-01-31T06:07:51.545Z';
            const query = { from: now, to: now };

            const actual = queryHelper.getInterval(query);

            expect(actual).to.deep.equal({ from: new Date(now), to: new Date(now) });
        });

        it('should throw 400 when `from` date is invalid', () => {
            const query = { from: 'invalid date' };
            const error = catchError(queryHelper.getInterval.bind(null, query));

            expect(error.message).to.equal('From date is invalid');
            expect(error.status).to.equal(400);
            expect(error.options).to.deep.equal({
                from: 'invalid date',
                internalCode: '400_FND'
            });
        });

        it('should throw 400 when `to` date is invalid', () => {
            const query = {
                from: new Date().toISOString(),
                to: 'invalid date'
            };
            const error = catchError(queryHelper.getInterval.bind(null, query));

            expect(error.message).to.equal('To date is invalid');
            expect(error.status).to.equal(400);
            expect(error.options).to.deep.equal({
                to: 'invalid date',
                internalCode: '400_TND'
            });
        });
    });

    describe('`getArray`', () => {
        it('should return array for single value', () => {
            const query = { slug: 'hello' };

            const actual = queryHelper.getArray(query, 'slug');

            expect(actual).to.deep.equal(['hello']);
        });

        it('should not modify array of values', () => {
            const query = { slug: ['hello'] };

            const actual = queryHelper.getArray(query, 'slug');

            expect(actual).to.deep.equal(['hello']);
        });

        it('should return `[]` when field is missing in query', () => {
            const actual = queryHelper.getArray({}, 'slug');

            expect(actual).to.deep.equal([]);
        });
    });

    describe('`getLoginForSearch`', () => {
        it('should replace `.` and `-` on _', () => {
            const query = { login: 'one-two.three' };

            const actual = queryHelper.getLoginForSearch(query);

            expect(actual).to.equal('one_two_three');
        });

        it('should throw 400 when `login` is invalid', () => {
            const query = { login: 4 };
            const error = catchError(queryHelper.getLoginForSearch.bind(null, query));

            expect(error.message).to.equal('Login is invalid');
            expect(error.status).to.equal(400);
            expect(error.options).to.deep.equal({
                internalCode: '400_LII',
                searchedLogin: 4
            });
        });
    });
});
