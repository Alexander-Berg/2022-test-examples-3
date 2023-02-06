const findCheapestValue = jest.fn();

jest.setMock('../cheapest/findCheapestValue', findCheapestValue);

const checkIsManyCheapest = jest.fn();

jest.setMock('../cheapest/checkIsManyCheapest', checkIsManyCheapest);

const findFastestSegmentValue = jest.fn();

jest.setMock('../fastest/findFastestSegmentValue', findFastestSegmentValue);

const checkIsManyFastest = jest.fn();

jest.setMock('../fastest/checkIsManyFastest', checkIsManyFastest);

const getBadgesData = require.requireActual('../getBadgesData').default;

const segments = null;

describe('getBadgesData', () => {
    beforeEach(() => {
        findCheapestValue.mockReset();
        checkIsManyCheapest.mockReset();
        findFastestSegmentValue.mockReset();
        checkIsManyFastest.mockReset();
    });

    it('Вернет данные для расчета бейджиков', () => {
        findCheapestValue.mockReturnValue(3000);
        checkIsManyCheapest.mockReturnValue(false);

        findFastestSegmentValue.mockReturnValue(23222);
        checkIsManyFastest.mockReturnValue(false);

        const expectedValue = {
            isManyCheapest: false,
            cheapestValue: 3000,
            fastestSegmentValue: 23222,
            isManyFastest: false,
        };

        expect(getBadgesData(segments)).toEqual(expectedValue);
    });

    it('Не будет записана информация для быстрых сегментов, если не нашелся самый быстрый сегмент', () => {
        findCheapestValue.mockReturnValue(3000);
        checkIsManyCheapest.mockReturnValue(false);

        findFastestSegmentValue.mockReturnValue(undefined);
        checkIsManyFastest.mockReturnValue(false);

        const expectedValue = {
            isManyCheapest: false,
            cheapestValue: 3000,
        };

        expect(getBadgesData(segments)).toEqual(expectedValue);
    });

    it('Вернет null, если не будет информации по бейджикам', () => {
        findCheapestValue.mockReturnValue(Infinity);
        findFastestSegmentValue.mockReturnValue(undefined);

        expect(getBadgesData(segments)).toBe(null);
    });
});
