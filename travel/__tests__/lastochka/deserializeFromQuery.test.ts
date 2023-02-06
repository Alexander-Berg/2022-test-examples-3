import lastochka from '../../lastochka';

describe('lastochka', () => {
    describe('deserializeFromQuery', () => {
        it('lastochka: y', () => {
            const value = lastochka.deserializeFromQuery({lastochka: 'y'});

            expect(value).toBe(true);
        });

        it('lastochka: Y', () => {
            const value = lastochka.deserializeFromQuery({lastochka: 'Y'});

            expect(value).toBe(true);
        });

        it('lastochka: unknown', () => {
            const value = lastochka.deserializeFromQuery({
                lastochka: 'unknown',
            });

            expect(value).toBe(false);
        });

        it('no "lastochka" param', () => {
            const value = lastochka.deserializeFromQuery({foo: 'bar'});

            expect(value).toBe(false);
        });
    });
});
