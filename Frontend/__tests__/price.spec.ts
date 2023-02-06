import { getPrice, formatPrice } from '../price';

describe('getPrice', () => {
    const rawPriceWithDiscount = {
        currency: 'RUB',
        isDeliveryIncluded: Boolean(false),
        value: '150',
        discount: {
            percent: 60,
            oldMin: '250',
        },
    };
    const rawPrice = {
        currency: 'RUB',
        isDeliveryIncluded: Boolean(false),
        value: '250',
        rawValue: '250',
    };

    it('Должен вернуть корректно отформатированную цену', () => {
        expect(getPrice(rawPrice)).toEqual({ price: 250 });
        expect(getPrice(rawPriceWithDiscount)).toEqual({ price: 150, oldPrice: 250, percent: 60 });
    });
});

describe('formatPrice', () => {
    it('возвращает корректно отформатированную цену', () => {
        expect(formatPrice('1')).toEqual('1');
        expect(formatPrice('15')).toEqual('15');
        expect(formatPrice('150')).toEqual('150');
        expect(formatPrice('1550')).toEqual('1 550');
        expect(formatPrice('15500')).toEqual('15 500');
        expect(formatPrice('155000')).toEqual('155 000');
        expect(formatPrice('1550000')).toEqual('1 550 000');
    });

    it('возвращает отформатированную цену с учетом кастомного разделителя', () => {
        expect(formatPrice('1550000', ',')).toEqual('1,550,000');
    });
});
