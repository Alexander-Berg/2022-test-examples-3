import lastochka from '../../lastochka';

describe('lastochka', () => {
    describe('isAvailableWithOptions', () => {
        it('withLastochka: true, withoutLastochka: true', () => {
            const available = lastochka.isAvailableWithOptions({
                withLastochka: true,
                withoutLastochka: true,
            });

            expect(available).toBe(true);
        });

        it('withLastochka: true, withoutLastochka: false', () => {
            const available = lastochka.isAvailableWithOptions({
                withLastochka: true,
                withoutLastochka: false,
            });

            expect(available).toBe(true);
        });

        it('withLastochka: false, withoutLastochka: true', () => {
            const available = lastochka.isAvailableWithOptions({
                withLastochka: false,
                withoutLastochka: true,
            });

            expect(available).toBe(false);
        });

        it('withLastochka: false, withoutLastochka: false', () => {
            const available = lastochka.isAvailableWithOptions({
                withLastochka: false,
                withoutLastochka: false,
            });

            expect(available).toBe(false);
        });
    });
});
