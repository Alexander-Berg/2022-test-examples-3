import getPickupInfo from '../pickup';
import { freePickup, pickupWithPrice } from './pickupMock';

describe('getPickupInfo', () => {
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

    it('Бесплатный самовывоз', () => {
        // @ts-ignore
        const delivery = getPickupInfo(freePickup);

        expect(delivery).toEqual([
            [
                { text: 'Бесплатная доставка', isBenefit: true },
                { text: '2 дня', isBenefit: false },
                { text: '1\u00A0пункт Пункт выдачи посылок Беру' },
            ],
            [
                { text: 'Бесплатная доставка', isBenefit: true },
                { text: '2 дня', isBenefit: false },
                { text: '647\u00A0пунктов PickPoint' },
            ],
            [
                { text: 'Бесплатная доставка', isBenefit: true },
                { text: '2 дня', isBenefit: false },
                { text: '351\u00A0пункт Boxberry' },
            ],
            [
                { text: 'Бесплатная доставка', isBenefit: true },
                { text: '2 дня', isBenefit: false },
                { text: '30\u00A0пунктов Стриж Почтоматы' },
            ],
            [
                { text: 'Бесплатная доставка', isBenefit: true },
                { text: '3 дня', isBenefit: false },
                { text: '152\u00A0пункта СДЭК' },
            ],
            [
                { text: 'Бесплатная доставка', isBenefit: true },
                { text: '4 дня', isBenefit: false },
                { text: '429\u00A0пунктов DPD' },
            ],
        ]);
    });

    it('Платный самовывоз', () => {
        // @ts-ignore
        const delivery = getPickupInfo(pickupWithPrice);

        expect(delivery).toEqual([
            [
                { text: 'Бесплатная доставка', isBenefit: true },
                { text: 'cрок доставки уточняйте при заказе', isBenefit: false },
                { text: '1\u00A0пункт магазина' },
            ],
            [
                { text: '650\u00A0₽', isBenefit: false },
                { text: 'cрок доставки уточняйте при заказе', isBenefit: false },
                { text: '1\u00A0пункт магазина' },
            ],
        ]);
    });
});
