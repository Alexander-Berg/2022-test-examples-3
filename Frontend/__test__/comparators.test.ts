import { isEqual, isGreaterThanOrEqual, isGreaterThan } from '../comparators';

describe('comparators', () => {
    it('#isEqual', () => {
        expect(isEqual(20, 4, 8)({
            year: 20,
            month: 4,
            minor: 8,
        })).toBeTruthy();

        expect(isEqual(20, 4, 8)({
            year: 20,
            month: 4,
            minor: 1,
        })).toBeFalsy();

        expect(isEqual(20, 1, 8)({
            year: 2,
            month: 1,
            minor: 8,
        })).toBeFalsy();
    });

    it('#isGreaterThan', () => {
        expect(isGreaterThan(20, 4, 8)({
            year: 21,
            month: 4,
            minor: 8,
        })).toBeTruthy();

        expect(isGreaterThan(20, 4, 8)({
            year: 20,
            month: 5,
            minor: 1,
        })).toBeTruthy();

        expect(isGreaterThan(20, 1, 8)({
            year: 20,
            month: 1,
            minor: 10,
        })).toBeTruthy();

        expect(isGreaterThan(20, 4, 8)({
            year: 19,
            month: 4,
            minor: 8,
        })).toBeFalsy();

        expect(isGreaterThan(20, 4, 8)({
            year: 20,
            month: 0,
            minor: 8,
        })).toBeFalsy();

        expect(isGreaterThan(20, 1, 8)({
            year: 20,
            month: 1,
            minor: 7,
        })).toBeFalsy();

        expect(isGreaterThan(20, 1, 8)({
            year: 20,
            month: 1,
            minor: 8,
        })).toBeFalsy();
    });

    it('#isGreaterThanOrEqual', () => {
        expect(isGreaterThanOrEqual(20, 4, 8)({
            year: 21,
            month: 4,
            minor: 8,
        })).toBeTruthy();

        expect(isGreaterThanOrEqual(20, 4, 8)({
            year: 20,
            month: 5,
            minor: 1,
        })).toBeTruthy();

        expect(isGreaterThanOrEqual(20, 1, 8)({
            year: 20,
            month: 1,
            minor: 10,
        })).toBeTruthy();

        expect(isGreaterThanOrEqual(20, 4, 8)({
            year: 19,
            month: 4,
            minor: 8,
        })).toBeFalsy();

        expect(isGreaterThanOrEqual(20, 4, 8)({
            year: 20,
            month: 0,
            minor: 8,
        })).toBeFalsy();

        expect(isGreaterThanOrEqual(20, 1, 8)({
            year: 20,
            month: 1,
            minor: 7,
        })).toBeFalsy();

        expect(isGreaterThanOrEqual(20, 1, 8)({
            year: 20,
            month: 1,
            minor: 8,
        })).toBeTruthy();
    });
});
