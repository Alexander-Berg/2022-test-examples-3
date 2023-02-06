import { SEC_IN_HOUR, SEC_IN_MIN } from '../../const';
import { getTimeFromSeconds } from './getTimeFromSeconds';

describe('getTimeFromSeconds', () => {
    it('should return correct value', () => {
        expect(getTimeFromSeconds(5)).toEqual('00:00:05');
        expect(getTimeFromSeconds(7 * SEC_IN_MIN + 15)).toEqual('00:07:15');
        expect(getTimeFromSeconds(2 * SEC_IN_HOUR + 7 * SEC_IN_MIN + 15)).toEqual('02:07:15');
    });
});
