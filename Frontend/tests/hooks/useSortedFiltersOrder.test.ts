import type { IFilters } from '@src/typings/filters';
import { getSortedFiltersOrder } from '../../hooks/useSortedFiltersOrder';

describe('Filters/hooks/useSortedFiltersOrder', () => {
    it('Возвращает сортированный список идентификаторов фильтров', () => {
        const defaultOrder = ['brand', 'shop', 'size', 'model'];
        const appliedFilters: IFilters = {
            size: {
                type: 'enum',
                title: '',
                id: 'size',
                param: '',
                values: [
                    { value: '1', found: 1, id: '1', checked: true },
                    { value: '2', found: 0, id: '2' },
                    { value: '3', found: 0, id: '3' },
                ],
            },
        };
        const disabledFilters: IFilters = {
            brand: {
                type: 'enum',
                title: '',
                id: 'brand',
                param: '',
                values: [
                    { value: '1', found: 0, id: '1' },
                    { value: '2', found: 0, id: '2' },
                    { value: '3', found: 0, id: '3' },
                ],
            },
        };
        const order = getSortedFiltersOrder(defaultOrder, appliedFilters, disabledFilters);

        expect(order).toStrictEqual(['size', 'shop', 'model', 'brand']);
    });
});
