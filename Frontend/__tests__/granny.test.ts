import { isGranny } from '../../report-renderer/routes/granny';
import { MessengerRequest } from '../../types';

const createMessengerRequestMock = (device: any) => {
    return {
        device,
        isYandexNet: false,
    } as MessengerRequest;
};

describe('granny', () => {
    describe('isGranny', () => {
        it('IE', () => {
            expect(isGranny(createMessengerRequestMock({
                browser: { name: 'MSIE' },
            }))).toBe(true);
        });

        it('Chrome', () => {
            expect(isGranny(createMessengerRequestMock({
                browser: { name: 'Chrome', version: '31.2.0.999' },
            }))).toBe(true);
            expect(isGranny(createMessengerRequestMock({
                browser: { name: 'Chrome', version: '45' },
            }))).toBe(false);
        });

        it('Yabro', () => {
            expect(isGranny(createMessengerRequestMock({
                browser: { name: 'YandexBrowser', version: '14.8' },
            }))).toBe(true);
            expect(isGranny(createMessengerRequestMock({
                browser: { name: 'YandexBrowser', version: '15.2' },
            }))).toBe(false);
        });
    });
});
