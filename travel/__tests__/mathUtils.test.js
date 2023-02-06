jest.disableAutomock();

import {ceilByStep} from '../mathUtils';

describe('ceilByStep', () => {
    it('Вернёт число округленное с заданным шагом', () => {
        expect(ceilByStep(9, 2)).toBe(10);
        expect(ceilByStep(10, 5)).toBe(10);
    });
});
