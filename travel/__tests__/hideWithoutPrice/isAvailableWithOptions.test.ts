import pricePresence from '../../hideWithoutPrice';

describe('pricePresence.isAvailableWithOptions', () => {
    it('withPrice: true, withoutPrice: true', () => {
        const available = pricePresence.isAvailableWithOptions({
            withPrice: true,
            withoutPrice: true,
        });

        expect(available).toBe(true);
    });

    it('withPrice: true, withoutPrice: false', () => {
        const available = pricePresence.isAvailableWithOptions({
            withPrice: true,
            withoutPrice: false,
        });

        expect(available).toBe(false);
    });

    it('withPrice: false, withoutPrice: true', () => {
        const available = pricePresence.isAvailableWithOptions({
            withPrice: false,
            withoutPrice: true,
        });

        expect(available).toBe(false);
    });

    it('withPrice: false, withoutPrice: false', () => {
        const available = pricePresence.isAvailableWithOptions({
            withPrice: false,
            withoutPrice: false,
        });

        expect(available).toBe(false);
    });
});
