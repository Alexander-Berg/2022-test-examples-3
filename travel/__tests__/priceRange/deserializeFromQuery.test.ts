import priceRange from '../../priceRange';

describe('priceRange', () => {
    describe('deserializeFromQuery', () => {
        it('Есть отмеченные опции', () => {
            const result = priceRange.deserializeFromQuery({
                priceRange: ['100-200'],
            });

            expect(result).toEqual([{min: 100, max: 200}]);
        });

        it('Есть отмеченные опции. Несколько штук', () => {
            const result = priceRange.deserializeFromQuery({
                priceRange: ['100-200', '200-300'],
            });

            expect(result).toEqual([
                {min: 100, max: 200},
                {min: 200, max: 300},
            ]);
        });

        it('Нет отмеченных опций', () => {
            const result = priceRange.deserializeFromQuery({});

            expect(result).toEqual([]);
        });

        it('Есть отмеченные опции. Поломанные опции', () => {
            const result = priceRange.deserializeFromQuery({
                priceRange: ['100-0', '-300', '300-400'],
            });

            expect(result).toEqual([{min: 300, max: 400}]);
        });
    });
});
