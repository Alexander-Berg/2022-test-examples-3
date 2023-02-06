import { PortfolioComparePeriod } from 'entities/Portfolio/consts/PortfolioComparePeriod';
import { getPortfolioComparePeriod } from 'entities/Portfolio/helpers/getPortfolioComparePeriod/getPortfolioComparePeriod';

describe('getPortfolioComparePeriod', function () {
    beforeEach(function () {
        jest.useFakeTimers().setSystemTime(new Date('2022-01-01').getTime());
    });

    afterEach(function () {
        jest.useRealTimers();
    });

    it('works with empty params', function () {
        expect(getPortfolioComparePeriod(null)).toMatchSnapshot();
    });

    it('works with full params', function () {
        Object.values(PortfolioComparePeriod).forEach((period) => {
            expect(getPortfolioComparePeriod(period)).toMatchSnapshot();
        });
    });
});
