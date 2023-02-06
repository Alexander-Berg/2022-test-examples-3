const getToday = jest.fn();
const isOutOfRange = jest.fn();
const getOrderRange = jest.fn();

jest.setMock('../../date/utils', {
    getToday,
    isOutOfRange,
    getOrderRange,
});

const {TRAIN_TYPE, PLANE_TYPE} = require.requireActual('../../transportType');
const {isOutOfSellRange} = require.requireActual('../isOutOfSellRange');

const context = {
    transportTypes: [TRAIN_TYPE],
    time: {},
    when: {},
};

describe('isOutOfSellRange', () => {
    it('если в результатах поиска присутствует несколько типов транспорта - вернёт false', () =>
        expect(
            isOutOfSellRange({
                ...context,
                transportTypes: [TRAIN_TYPE, PLANE_TYPE],
            }),
        ).toBe(false));

    it('если в результатах поиска только один тип транспорта - вернёт вызов функции isOutOfRange', () => {
        isOutOfRange.mockReturnValueOnce(true).mockReturnValueOnce(false);

        expect(isOutOfSellRange(context)).toBe(true);
        expect(isOutOfSellRange(context)).toBe(false);
    });
});
