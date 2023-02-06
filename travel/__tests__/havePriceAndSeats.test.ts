import ISegmentFromBackend from '../../../../interfaces/segment/ISegmentFromBackend';

import havePriceAndSeats from '../havePriceAndSeats';

describe('havePriceAndSeats', () => {
    it('Вернет true, если есть цены, но нет информации о количестве мест', () => {
        const segment = {
            tariffs: {
                classes: {
                    econom: {
                        price: {
                            value: 100,
                        },
                    },
                },
            },
        } as unknown as ISegmentFromBackend;

        expect(havePriceAndSeats(segment)).toBe(true);
    });

    it('Вернет true, если есть цены и указано положительное количество мест', () => {
        const segment = {
            tariffs: {
                classes: {
                    econom: {
                        price: {
                            value: 100,
                        },
                        seats: 10,
                    },
                },
            },
        } as unknown as ISegmentFromBackend;

        expect(havePriceAndSeats(segment)).toBe(true);
    });

    it('Вернет false, если указаны цены, но количество мест меньше одного', () => {
        const segment = {
            tariffs: {
                classes: {
                    econom: {
                        price: {
                            value: 100,
                        },
                        seats: 0,
                    },
                },
            },
        } as unknown as ISegmentFromBackend;

        expect(havePriceAndSeats(segment)).toBe(false);
    });

    it('Вернет false, если отсутствуют тарифы', () => {
        const segment = {
            tariffs: {
                classes: {},
            },
        } as unknown as ISegmentFromBackend;

        expect(havePriceAndSeats(segment)).toBe(false);
    });
});
