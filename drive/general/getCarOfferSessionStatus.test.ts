import { getCarOfferSessionStatus } from 'entities/Car/helpers/getCarOfferSessionStatus/getCarOfferSessionStatus';

describe('getCarOfferSessionStatus', function () {
    beforeEach(function () {
        jest.useFakeTimers().setSystemTime(new Date('2022-05-05').getTime());
    });

    afterEach(function () {
        jest.useRealTimers();
    });

    it('works with empty params', function () {
        expect(
            getCarOfferSessionStatus({
                since: new Date('2022-06-01'),
                until: new Date('2022-06-20'),
            }),
        ).toBeUndefined();
    });

    it('should return "in_progress"', function () {
        expect(
            getCarOfferSessionStatus({
                since: new Date('2022-04-30'),
                until: new Date('2022-05-10'),
                actual_since: new Date('2022-05-01'),
            }),
        ).toMatch('in_progress');
    });

    it('should return "complete"', function () {
        expect(
            getCarOfferSessionStatus({
                since: new Date('2022-01-01'),
                until: new Date('2022-01-20'),
                actual_until: new Date('2022-01-20'),
            }),
        ).toMatch('complete');

        expect(
            getCarOfferSessionStatus({
                since: new Date('2022-01-01'),
                until: new Date('2022-01-20'),
                actual_since: new Date('2022-01-01'),
                actual_until: new Date('2022-01-20'),
            }),
        ).toMatch('complete');
    });

    it('should return "outdated"', function () {
        expect(
            getCarOfferSessionStatus({
                since: new Date('2022-04-01'),
                until: new Date('2022-04-20'),
            }),
        ).toMatch('outdated');
    });
});
