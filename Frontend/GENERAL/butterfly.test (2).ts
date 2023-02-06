import { showButterfly } from './index';

describe('ButterFly', () => {
    it('Совершенно сторонний человек', () => {
        expect(showButterfly({
            isInternalAccount: false,
            isYandexNet: false,
            forceHide: false,
        })).toBe(false);

        expect(showButterfly({
            isInternalAccount: false,
            isYandexNet: false,
            forceHide: true,
        })).toBe(false);
    });

    it('Сеть яндекса, но не яндексоид', () => {
        expect(showButterfly({
            isInternalAccount: false,
            isYandexNet: true,
            forceHide: false,
        })).toBe(true);
        expect(showButterfly({
            isInternalAccount: false,
            isYandexNet: true,
            forceHide: true,
        })).toBe(false);
    });

    it('Яндексоид, но не в сети яндекса', () => {
        expect(showButterfly({
            isInternalAccount: true,
            isYandexNet: false,
            forceHide: false,
        })).toBe(true);
        expect(showButterfly({
            isInternalAccount: true,
            isYandexNet: false,
            forceHide: true,
        })).toBe(false);
    });

    it('Яндексоид в сети яндекса', () => {
        expect(showButterfly({
            isInternalAccount: true,
            isYandexNet: true,
            forceHide: false,
        })).toBe(true);
        expect(showButterfly({
            isInternalAccount: true,
            isYandexNet: true,
            forceHide: true,
        })).toBe(false);
    });
});
