const havePrices = jest.fn();

jest.setMock('../../../havePrices', havePrices);

const filterTariffs = require.requireActual('../filterTariffs').default;

const segments = [{}, {}, {}];

describe('filterTariffs', () => {
    it('Не фильтрует сегменты, если у всех есть цены', () => {
        havePrices.mockReturnValue(true);

        expect(filterTariffs(segments).length).toEqual(3);
    });

    it('Вернет только те сегменты, у которых есть цены', () => {
        havePrices.mockReturnValueOnce(true);
        havePrices.mockReturnValueOnce(false);
        havePrices.mockReturnValueOnce(true);

        expect(filterTariffs(segments).length).toEqual(2);
    });

    it('Вернет пустой массив, если никакой сегмент не содержит цен', () => {
        havePrices.mockReturnValue(false);

        expect(filterTariffs(segments).length).toEqual(0);
    });
});
