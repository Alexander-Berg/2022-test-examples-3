jest.dontMock('../../transportType');

const isAllDaysSearch = jest.fn().mockReturnValue(false);
const isOutOfSellRange = jest.fn().mockReturnValue(false);
const isOpenedForSaleTrainDirection = jest.fn().mockReturnValue(true);

jest.setMock('../isAllDaysSearch', isAllDaysSearch);
jest.setMock('../isOutOfSellRange', {isOutOfSellRange});
jest.setMock('../isOpenedForSaleTrainDirection', isOpenedForSaleTrainDirection);

const shouldRequestTrainTariffs = require.requireActual(
    '../shouldRequestTrainTariffs',
).default;
const {BUS_TYPE, ALL_TYPE, TRAIN_TYPE, PLANE_TYPE, WATER_TYPE, SUBURBAN_TYPE} =
    require.requireActual('../../transportType');

const context = {
    transportType: ALL_TYPE,
};
const flags = {
    __ufsTesting: true,
};

describe('shouldRequestTrainTariffs', () => {
    it('Должен вернуть false для поиска на все дни', () => {
        isAllDaysSearch.mockReturnValueOnce(true);
        expect(shouldRequestTrainTariffs(context)).toBe(false);
    });

    it(`Должен вернуть false для поиска на дату за пределами диапазона продаж,
        если сервис запущен под флагом тестирования`, () => {
        isOutOfSellRange.mockReturnValueOnce(true);
        expect(shouldRequestTrainTariffs(context, flags)).toBe(false);
    });

    it(`Должен вернуть true для поиска на дату в пределах диапазона продаж,
        если сервис запущен под флагом тестирования`, () => {
        isOutOfSellRange.mockReturnValueOnce(false);
        expect(shouldRequestTrainTariffs(context, flags)).toBe(true);
    });

    [BUS_TYPE, PLANE_TYPE, WATER_TYPE].forEach(transportType =>
        it(`Должен вернуть false для ${transportType} поиска`, () => {
            expect(
                shouldRequestTrainTariffs({
                    transportType,
                }),
            ).toBe(false);
        }),
    );

    it('Если продажа билетов по заданному направлению закрыта - вернём false', () => {
        isOpenedForSaleTrainDirection.mockReturnValueOnce(false);
        expect(shouldRequestTrainTariffs(context)).toBe(false);
    });

    [ALL_TYPE, TRAIN_TYPE, SUBURBAN_TYPE].forEach(transportType =>
        it(`Поиск ${transportType}: Если продажа билетов по заданному направлению открыта - вернём true`, () => {
            expect(
                shouldRequestTrainTariffs({
                    transportType,
                }),
            ).toBe(true);
        }),
    );
});
