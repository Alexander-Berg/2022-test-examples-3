const filterSegmentsWithIncorrectPrice = jest.fn(segments => segments);

jest.setMock(
    '../filterSegmentsWithIncorrectPrice',
    filterSegmentsWithIncorrectPrice,
);

const joinDynamicPriceAnswers = require.requireActual(
    '../joinDynamicPriceAnswers',
).default;

const busSegment = {transport: {code: 'bus'}};
const trainSegment = {transport: {code: 'train'}};
const planeSegment = {transport: {code: 'plane'}};

const currencies = {
    currencyRates: {},
};

describe('joinDynamicPriceAnswers', () => {
    it('Вернёт сегменты и флаг опроса из ответов ручек динамических цен', () => {
        expect(
            joinDynamicPriceAnswers(
                [
                    {
                        busTariffs: {
                            querying: true,
                            segments: [busSegment],
                        },
                    },
                    {
                        trainTariffs: {
                            querying: false,
                            segments: [trainSegment],
                        },
                    },
                    {
                        planeTariffs: {
                            querying: true,
                            segments: [planeSegment],
                        },
                    },
                ],
                currencies,
            ),
        ).toEqual({
            segments: [busSegment, planeSegment, trainSegment],
            querying: {
                bus: true,
                train: false,
                plane: true,
            },
        });
        expect(filterSegmentsWithIncorrectPrice).toHaveBeenCalledWith(
            [busSegment],
            currencies,
        );
    });

    it('Вернеёт объект с пустыми полями для пустых ответов ручек', () => {
        expect(joinDynamicPriceAnswers([], currencies)).toEqual({
            segments: [],
            querying: {},
        });
    });
});
