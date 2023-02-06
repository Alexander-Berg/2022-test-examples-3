import { getScrollTop } from 'shared/helpers/getScrollTop/getScrollTop';

describe('getScrollTop', () => {
    it('default call', () => {
        expect(getScrollTop()).toBe(0);
    });
});
