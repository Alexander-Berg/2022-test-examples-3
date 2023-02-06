const isPriceGreater = jest.fn();
const getLowestPrice = jest.fn();

jest.setMock('../isPriceGreater', isPriceGreater);
jest.setMock('../getLowestPrice', getLowestPrice);

const isUpdateCheaper = require.requireActual('../isUpdateCheaper').default;

const baseSegment = {};
const updateSegment = {};

describe('isUpdateCheaper', () => {
    it('если у сегментов нет цены - возвращаем false', () => {
        getLowestPrice.mockReturnValue(null);

        expect(isUpdateCheaper(baseSegment, updateSegment)).toBe(false);
    });

    it(`если цена определена у базового сегмента,
        а у сегмента для сравнения не определена - возвращаем false`, () => {
        getLowestPrice.mockReturnValueOnce({}).mockReturnValueOnce(null);

        expect(isUpdateCheaper(baseSegment, updateSegment)).toBe(false);
    });

    it(`если цена не определена у базового сегмента,
        а у сегмента для сравнения определена - возвращаем true`, () => {
        getLowestPrice.mockReturnValueOnce(null).mockReturnValueOnce({});

        expect(isUpdateCheaper(baseSegment, updateSegment)).toBe(true);
    });

    it('если цены определены - возвращаем результат сравнения', () => {
        getLowestPrice.mockReturnValue({});
        isPriceGreater.mockReturnValueOnce(false).mockReturnValueOnce(true);

        expect(isUpdateCheaper(baseSegment, updateSegment)).toBe(false);
        expect(isUpdateCheaper(baseSegment, updateSegment)).toBe(true);
    });
});
