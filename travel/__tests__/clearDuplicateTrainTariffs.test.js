const {TRAIN_TYPE} = require.requireActual('../../transportType');
const clearDuplicateTrainTariffs = require.requireActual(
    '../clearDuplicateTrainTariffs',
).default;

const today1A = {
    transport: {
        code: TRAIN_TYPE,
    },
    departure: '2017-01-11T00:39:00+00:00',
    number: '1A',
    tariffs: {},
};
const today1ADuplicate = {
    transport: {
        code: TRAIN_TYPE,
    },
    departure: '2017-01-11T09:18:00+00:00',
    number: '1A',
    tariffs: {},
};
const today2A = {
    transport: {
        code: TRAIN_TYPE,
    },
    departure: '2017-01-11T04:00:00+00:00',
    number: '2A',
    tariffs: {},
};
const tomorrowA1 = {
    transport: {
        code: TRAIN_TYPE,
    },
    departure: '2017-01-12T00:39:00+00:00',
    number: '1A',
    tariffs: {},
};

describe('clearDuplicateTrainTariffs', () => {
    it('should return empty array', () => {
        expect(clearDuplicateTrainTariffs([])).toEqual([]);
    });

    it('should not modify segments', () => {
        expect(
            clearDuplicateTrainTariffs([{...today1A}, {...today2A}]),
        ).toEqual([today1A, today2A]);
    });

    it('should not modify segments in different days', () => {
        expect(
            clearDuplicateTrainTariffs([{...today1A}, {...tomorrowA1}]),
        ).toEqual([today1A, tomorrowA1]);
    });

    it('should delete tariffs from duplicates', () => {
        const result = clearDuplicateTrainTariffs([
            {...today1A},
            {...today1ADuplicate},
        ]);
        const withoutTariff = {
            ...today1ADuplicate,
            tariffs: null,
        };

        expect(result[0]).toEqual(today1A);
        expect(result[1]).toEqual(withoutTariff);
    });
});
