const {ALL_TYPE, TRAIN_TYPE, SUBURBAN_TYPE} = require.requireActual(
    '../../transportType',
);

const shouldRequestSuburbanTransfer = require.requireActual(
    '../shouldRequestSuburbanTransfer',
).default;

const context = {
    when: {
        date: '22.12.2017',
    },
    sameSuburbanZone: true,
    transportType: ALL_TYPE,
};

describe('shouldRequestSuburbanTransfer', () => {
    it('на запрашиваем пересадки электричками при поиске на все дни', () => {
        expect(
            shouldRequestSuburbanTransfer({
                ...context,
                when: {
                    date: null,
                },
            }),
        ).toBe(false);
    });

    it('не запрашиваем пересадки электричками если поиск не в рамках одной пригородной зоны', () => {
        expect(
            shouldRequestSuburbanTransfer({
                ...context,
                sameSuburbanZone: false,
            }),
        ).toBe(false);
    });

    it('не запрашиваем пересадки электричками при поиске другими типами транспорта', () => {
        expect(
            shouldRequestSuburbanTransfer({
                ...context,
                transportType: TRAIN_TYPE,
            }),
        ).toBe(false);
    });

    it(`запрашиваем пересадки электричками при поиске на дату в рамках одной пригородной зоны
        если искали электричками`, () => {
        expect(shouldRequestSuburbanTransfer(context)).toBe(false);
        expect(
            shouldRequestSuburbanTransfer({
                ...context,
                transportType: SUBURBAN_TYPE,
            }),
        ).toBe(true);
    });
});
