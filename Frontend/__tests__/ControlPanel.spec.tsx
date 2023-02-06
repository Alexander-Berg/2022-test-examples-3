import { getFiltersCount } from '../utils';

describe('ControlPanel', () => {
    describe('Индикатор фильтров', () => {
        it('Пустые фильтра', () => {
            expect(getFiltersCount({}))
                .toEqual(0);

            expect(getFiltersCount({ filters: 'price:;' }))
                .toEqual(0);
        });
        it('Заполненые фильтра', () => {
            expect(getFiltersCount({ filters: 'price:;p125:66,28;p650:4,10' }))
                .toEqual(2);
            expect(getFiltersCount({ filters: 'price:1,;p125:66,28;p650:4,10' }))
                .toEqual(3);
            expect(getFiltersCount({ filters: 'price:,100;p125:66,28;p650:4,10' }))
                .toEqual(3);
        });
    });
});
