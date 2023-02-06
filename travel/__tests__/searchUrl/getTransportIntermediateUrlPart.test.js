jest.disableAutomock();

import {getTransportIntermediateUrlPart} from '../../searchUrl';
import {
    TRAIN_TYPE,
    PLANE_TYPE,
    SUBURBAN_TYPE,
    BUS_TYPE,
    ALL_TYPE,
} from '../../../transportType';

describe('getTransportIntermediateUrlPart', () => {
    it('Возвращает правильный промежуточный урл для нового урла поиска по транспортам', () => {
        expect(getTransportIntermediateUrlPart(ALL_TYPE)).toBe(
            '/all-transport',
        );
        expect(getTransportIntermediateUrlPart(BUS_TYPE)).toBe(`/${BUS_TYPE}`);
        expect(getTransportIntermediateUrlPart(TRAIN_TYPE)).toBe(
            `/${TRAIN_TYPE}`,
        );
        expect(getTransportIntermediateUrlPart(PLANE_TYPE)).toBe(
            `/${PLANE_TYPE}`,
        );
        expect(getTransportIntermediateUrlPart(SUBURBAN_TYPE)).toBe(
            `/${SUBURBAN_TYPE}`,
        );
    });
});
