jest.disableAutomock();

import {CHAR_THINSP} from '../../stringUtils';

import formatDistance from '../formatDistance';

describe('formatDistance', () => {
    it('Дистанция для ru', () => {
        expect(formatDistance(12234)).toBe(`12${CHAR_THINSP}234 км`);
    });
});
