const isCheapest = jest.fn();

jest.setMock('../cheapest/isCheapest', isCheapest);

const isFastestSegment = jest.fn();

jest.setMock('../fastest/isFastestSegment', isFastestSegment);

const getAppropriateBadges = require.requireActual(
    '../getAppropriateBadges',
).default;

const TRAIN_SEGMENT = {
    transport: {
        code: 'train',
    },
};

const BADGES_DATA_CHEAPEST_1000 = {
    cheapestValue: 1000,
    fastestSegmentValue: 2333,
};

describe('getAppropriateBadges', () => {
    beforeEach(() => {
        isCheapest.mockReturnValue(false);
        isFastestSegment.mockReturnValue(false);
    });

    const segment = TRAIN_SEGMENT;
    const badgesData = BADGES_DATA_CHEAPEST_1000;

    it('Вернет `{ cheapest: true }`, если подходит только бейджик `cheapest`', () => {
        isCheapest.mockReturnValue(true);

        expect(getAppropriateBadges(segment, badgesData)).toEqual({
            cheapest: true,
        });
    });

    it('Вернет `{}`, если не подходят бейджики `cheap` и `cheapest`', () => {
        expect(getAppropriateBadges(segment, badgesData)).toEqual({});
    });

    it('Вернет { cheapest: true }, даже если подходят бейджики `cheap` и `cheapest`', () => {
        isCheapest.mockReturnValue(true);

        expect(getAppropriateBadges(segment, badgesData)).toEqual({
            cheapest: true,
        });
    });

    it('Вернет { fastest: true }, если подходит только бейджик `fastest`', () => {
        isFastestSegment.mockReturnValue(true);

        expect(getAppropriateBadges(segment, badgesData)).toEqual({
            fastest: true,
        });
    });

    it('Не вернет { fastest: true }, если бейджиков `fastest` слишком много', () => {
        expect(
            getAppropriateBadges(segment, {...badgesData, isManyFastest: true}),
        ).toEqual({});
    });

    it('Сегмент и самый дешевый и самый быстрый', () => {
        isCheapest.mockReturnValue(true);
        isFastestSegment.mockReturnValue(true);

        expect(getAppropriateBadges(segment, badgesData)).toEqual({
            cheapest: true,
            fastest: true,
        });
    });
});
