import { getCourierDelivery } from '../courier';
import { deliveryFree7days, deliveryToday, hasDelivery, regional13days } from './mocs';

describe('getCourierDelivery', () => {
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

    it('Срок и стоимость формата "+№р доставка, № дней"', () => {
        const delivery = getCourierDelivery(hasDelivery);

        expect(delivery).toEqual([
            { isBenefit: false, text: '+1,000\u00A0₽ доставка' },
            { isBenefit: false, text: '5\u00A0дней' },
        ]);
    });

    it('Срок и стоимость формата "+№р доставка, сегодня (завтра)"', () => {
        const delivery = getCourierDelivery(deliveryToday);

        expect(delivery).toEqual([
            { isBenefit: false, text: '+1,000\u00A0₽ доставка' },
            { isBenefit: true, text: 'сегодня' },
        ]);
    });

    it('Срок и стоимость формата "бесплатная доставка № дней"', () => {
        const delivery = getCourierDelivery(deliveryFree7days);

        expect(delivery).toEqual([
            { isBenefit: true, text: 'Бесплатная доставка' },
            { isBenefit: false, text: '7\u00A0дней' },
        ]);
    });

    it('Региональная 13 дней, бесплатно', () => {
        const delivery = getCourierDelivery(regional13days);

        expect(delivery).toEqual([
            { isBenefit: true, text: 'Бесплатная доставка' },
            { text: 'из Москвы' },
            { isBenefit: false, text: '13\u00A0дней' }]);
    });
});
