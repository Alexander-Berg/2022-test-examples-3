import { getFullDeliveryInfo } from '..';
import { optionsCombo } from './pickupMock';

describe('getFullDeliveryInfo', () => {
    beforeEach(() => {
        // Имитируем первоначальное глобальное состояние,
        // которое обычно пушится из common.blocks/root/root.bemhtml.js
        window.Ya.getStore!().dispatch({
            type: '@SYS/UPDATE',
            payload: {
                global: {
                    deviceDetect: {
                        BrowserEngine: 'WebKit',
                        BrowserName: 'MobileSafari',
                        BrowserVersion: '0011',
                        BrowserVersionRaw: '11.0',
                        OSFamily: 'iOS',
                        OSName: '',
                        OSVersion: '11.0',
                    },
                },
            },
        });
    });

    it('Подробная информация про доставку', () => {
        // @ts-ignore
        const delivery = getFullDeliveryInfo(optionsCombo);

        expect(delivery).toEqual({
            courier: {
                rows: [
                    [
                        { isBenefit: false, text: '+249\u00A0₽ доставка' },
                        { isBenefit: true, text: 'завтра' },
                    ],
                ],
            },
            pickup: {
                rows: [
                    [
                        { isBenefit: false, text: '99\u00A0₽' },
                        { isBenefit: true, text: 'завтра' },
                        { text: '1 пункт Пункт выдачи посылок Беру' },
                    ],
                    [
                        { isBenefit: false, text: '99\u00A0₽' },
                        { isBenefit: true, text: 'завтра' },
                        { text: '351 пункт Boxberry' },
                    ],
                    [
                        { isBenefit: false, text: '99\u00A0₽' },
                        { isBenefit: false, text: '3 дня' },
                        { text: '152 пункта СДЭК' },
                    ],
                ],
            },
        });
    });
});
