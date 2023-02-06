import shouldUnauth from '../../../../src/helpers/should-unauth';
import { HTTP_STATUSES } from '../../../../src/consts';

describe('shouldUnauth', () => {
    it('should return true if fetch-response status is 401 (UNAUTHORIZED)', () => {
        expect(shouldUnauth({ status: HTTP_STATUSES.UNAUTHORIZED })).toBe(true);
    });

    it('should return false if response does not exist or HTTP status is not 401', () => {
        expect(shouldUnauth({ status: HTTP_STATUSES.FORBIDDEN })).toBe(false);
        expect(shouldUnauth(null)).toBe(false);
    });
});
