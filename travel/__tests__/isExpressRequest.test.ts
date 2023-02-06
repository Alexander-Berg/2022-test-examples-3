import IExpressRequest from '../../interfaces/IExpressRequest';

import isExpressRequest from '../isExpressRequest';

describe('isExpressRequest', () => {
    it('return false', () => {
        expect(
            isExpressRequest({isExpressRequest: false} as IExpressRequest),
        ).toBe(false);
        expect(isExpressRequest({} as IExpressRequest)).toBe(false);
    });

    it('return true', () => {
        expect(
            isExpressRequest({isExpressRequest: true} as IExpressRequest),
        ).toBe(true);
    });
});
