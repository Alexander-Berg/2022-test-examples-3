jest.disableAutomock();

import {
    PLATZKARTE,
    SITTING,
    COMPARTMENT,
} from '../../../../lib/segments/tariffClasses';

const gatherMinPriceData = jest.fn(() => ({
    price: {
        class: SITTING,
    },
}));

jest.setMock('../../../../lib/segments/gatherMinPriceData', gatherMinPriceData);

const {
    getActualBadgesToDisplayAfterFiltering,
} = require('../getActualBadgesToDisplayAfterFiltering');

const segment = {
    badges: {
        cheapest: true,
        fastest: true,
    },
    tariffClassKeys: [COMPARTMENT, PLATZKARTE],
};

describe('getActualBadgesToDisplayAfterFiltering', () => {
    it('Если нет бейджиков - вернет undefined', () => {
        expect(
            getActualBadgesToDisplayAfterFiltering({
                ...segment,
                badges: undefined,
            }),
        ).toBe(undefined);
    });

    it('Если сегмент не самый дешевый - вернем текущие бейджики', () => {
        expect(
            getActualBadgesToDisplayAfterFiltering({
                ...segment,
                badges: {fastest: true},
            }),
        ).toEqual({fastest: true});
    });

    it('Если в отображаемых тарифах нету самого дешевого тарифа сегмента - убираем бейджик "самый дешевый"', () => {
        expect(getActualBadgesToDisplayAfterFiltering(segment)).toEqual({
            fastest: true,
        });
    });

    it('Если в отображаемых тарифов присутствует самый дешевый тариф сегмента - возвращаем не измененные бейджики', () => {
        gatherMinPriceData.mockReturnValueOnce({
            price: {
                class: COMPARTMENT,
            },
        });

        expect(getActualBadgesToDisplayAfterFiltering(segment)).toEqual(
            segment.badges,
        );
    });
});
