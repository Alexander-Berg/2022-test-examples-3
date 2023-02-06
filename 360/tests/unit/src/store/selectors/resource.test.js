import { shouldShowAntiFileSharingTooltip } from '../../../../../src/store/selectors/resource';

jest.mock('../../../../../src/store/selectors/promo', () => ({
    shouldShowAntiFOPromoMail360: () => false
}));

const getState = ({ isSmartphone, antiFileSharing }) => ({
    ua: { isSmartphone },
    environment: { antiFileSharing }
});

describe('shouldShowAntiFileSharingTooltip -> ', () => {
    it('равен true, если приложение открыто на планшете, и корневой ресурс имеет признак АнтиФО', () => {
        expect(shouldShowAntiFileSharingTooltip(
            getState({ isSmartphone: false, antiFileSharing: true })
        )).toBe(true);
    });

    it('равен false, если приложение открыто на смартфоне', () => {
        expect(shouldShowAntiFileSharingTooltip(
            getState({ isSmartphone: true, antiFileSharing: true })
        )).toBe(false);
    });

    it('равен false, если приложение открыто на планшете или десктопе, но корневой ресурс не имеет признака АнтиФО', () => {
        expect(shouldShowAntiFileSharingTooltip(
            getState({ isSmartphone: false, antiFileSharing: false })
        )).toBe(false);
    });
});
