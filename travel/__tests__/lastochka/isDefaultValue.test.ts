import lastochka from '../../lastochka';

describe('lastochka', () => {
    describe('isDefaultValue', () => {
        it('Показать только ласточки', () => {
            const result = lastochka.isDefaultValue(true);

            expect(result).toBe(false);
        });

        it('Показать все сегменты', () => {
            const result = lastochka.isDefaultValue(false);

            expect(result).toBe(true);
        });
    });
});
