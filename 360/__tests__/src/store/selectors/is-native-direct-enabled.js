import { isNativeDirectEnabled } from '../../../../src/store/selectors/is-native-direct-enabled';
import { hasAds } from '../../../../src/store/selectors/has-ads';

jest.mock('store/selectors/has-ads');

const getStore = (ua, flags = {}) => ({
    cfg: {
        ua,
        experiments: { flags }
    }
});

describe('store/selectors/is-native-direct-enabled', () => {
    beforeEach(() => {
        hasAds.mockReturnValue(true);
    });

    it('should be disabled with exp on mobile', () => {
        const store = getStore({ isMobile: true });
        expect(isNativeDirectEnabled(store)).toEqual(false);
        expect(hasAds).not.toHaveBeenCalled();
    });

    it('should be enabled with exp on desktop', () => {
        const store = getStore({ isMobile: false });
        expect(isNativeDirectEnabled(store)).toEqual(true);
        expect(hasAds).toHaveBeenCalledWith(store);
    });

    it('should be disabled with exp on desktop when user has no ads', () => {
        hasAds.mockReturnValue(false);
        const store = getStore({ isMobile: false });
        expect(isNativeDirectEnabled(store)).toEqual(false);
        expect(hasAds).toHaveBeenCalledWith(store);
    });
});
